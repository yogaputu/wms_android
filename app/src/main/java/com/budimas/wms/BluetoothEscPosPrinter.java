package com.budimas.wms;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

public class BluetoothEscPosPrinter {
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final int CHUNK_SIZE = 1024;

    private BluetoothEscPosPrinter() {
    }

    public static void printBitmap(String address, int paperWidthMm, Bitmap bitmap) throws Exception {
        if (address == null || address.trim().isEmpty()) {
            throw new IllegalArgumentException("Printer Bluetooth belum dipilih.");
        }
        if (bitmap == null) {
            throw new IllegalArgumentException("Data cetak kosong.");
        }

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            throw new IllegalStateException("Perangkat ini tidak mendukung Bluetooth.");
        }
        if (!adapter.isEnabled()) {
            throw new IllegalStateException("Bluetooth belum aktif.");
        }
        if (adapter.isDiscovering()) {
            adapter.cancelDiscovery();
        }

        BluetoothDevice device = adapter.getRemoteDevice(address.trim());
        BluetoothSocket socket = device.createRfcommSocketToServiceRecord(SPP_UUID);
        try {
            socket.connect();
            OutputStream output = socket.getOutputStream();
            Bitmap prepared = prepareBitmap(bitmap, paperWidthMm);

            write(output, new byte[]{0x1B, 0x40});
            write(output, new byte[]{0x1B, 0x61, 0x01});
            write(output, new byte[]{0x1B, 0x33, 0x18});
            write(output, rasterize(prepared));
            write(output, new byte[]{0x0A, 0x0A, 0x0A});
            output.flush();
        } catch (IOException e) {
            throw new IOException("Tidak bisa mengirim ke printer. Pastikan printer menyala dan sudah paired.", e);
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    private static Bitmap prepareBitmap(Bitmap source, int paperWidthMm) {
        int targetWidth = paperWidthMm >= 80 ? 576 : 384;
        targetWidth -= targetWidth % 8;
        int targetHeight = Math.max(1, Math.round(source.getHeight() * (targetWidth / (float) source.getWidth())));

        Bitmap scaled = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(scaled);
        canvas.drawColor(Color.WHITE);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
        Rect dst = new Rect(0, 0, targetWidth, targetHeight);
        canvas.drawBitmap(source, null, dst, paint);
        return scaled;
    }

    private static byte[] rasterize(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int widthBytes = width / 8;

        ByteArrayOutputStream out = new ByteArrayOutputStream(widthBytes * height + 8);
        out.write(0x1D);
        out.write(0x76);
        out.write(0x30);
        out.write(0x00);
        out.write(widthBytes & 0xFF);
        out.write((widthBytes >> 8) & 0xFF);
        out.write(height & 0xFF);
        out.write((height >> 8) & 0xFF);

        for (int y = 0; y < height; y++) {
            for (int xByte = 0; xByte < widthBytes; xByte++) {
                int packed = 0;
                for (int bit = 0; bit < 8; bit++) {
                    int x = xByte * 8 + bit;
                    if (isBlack(bitmap.getPixel(x, y))) {
                        packed |= (0x80 >> bit);
                    }
                }
                out.write(packed);
            }
        }
        return out.toByteArray();
    }

    private static boolean isBlack(int color) {
        if (Color.alpha(color) < 128) {
            return false;
        }
        int luminance = (Color.red(color) * 299 + Color.green(color) * 587 + Color.blue(color) * 114) / 1000;
        return luminance < 180;
    }

    private static void write(OutputStream output, byte[] data) throws IOException {
        int offset = 0;
        while (offset < data.length) {
            int length = Math.min(CHUNK_SIZE, data.length - offset);
            output.write(data, offset, length);
            output.flush();
            offset += length;
        }
    }
}
