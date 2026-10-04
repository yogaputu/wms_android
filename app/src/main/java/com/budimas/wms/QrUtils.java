package com.budimas.wms;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QrUtils {
    private static final Pattern PALLET_PATTERN = Pattern.compile("PLT-\\d{6,}", Pattern.CASE_INSENSITIVE);
    private static final Pattern LOCATION_PATTERN = Pattern.compile("([A-Z]-\\d{2}-\\d{2}|G\\d+R\\d+L\\d+K\\d+N\\d+)", Pattern.CASE_INSENSITIVE);

    private QrUtils() {
    }

    public static String extractPalletCode(String payload) {
        if (payload == null) {
            return "";
        }
        String text = payload.trim();
        String[] parts = text.split("\\|");
        if (parts.length >= 3 && "BUDIMAS-WMS".equalsIgnoreCase(parts[0]) && "PALLET".equalsIgnoreCase(parts[1])) {
            return parts[2].trim().toUpperCase(Locale.US);
        }
        Matcher matcher = PALLET_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group().toUpperCase(Locale.US);
        }
        return text.toUpperCase(Locale.US);
    }

    public static String extractLocationCode(String payload) {
        if (payload == null) {
            return "";
        }
        String text = payload.trim();
        String[] parts = text.split("\\|");
        if (parts.length >= 3 && "BUDIMAS-WMS".equalsIgnoreCase(parts[0]) && "LOCATION".equalsIgnoreCase(parts[1])) {
            return parts[2].trim().toUpperCase(Locale.US);
        }
        String[] semicolonParts = text.split(";");
        if (semicolonParts.length >= 2 && !semicolonParts[1].trim().isEmpty()) {
            return semicolonParts[1].trim().toUpperCase(Locale.US);
        }
        Matcher matcher = LOCATION_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group().toUpperCase(Locale.US);
        }
        return text.toUpperCase(Locale.US);
    }

    public static Bitmap createQrBitmap(String payload, int size) throws WriterException {
        BitMatrix matrix = new MultiFormatWriter().encode(payload, BarcodeFormat.QR_CODE, size, size);
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                bitmap.setPixel(x, y, matrix.get(x, y) ? Color.BLACK : Color.WHITE);
            }
        }
        return bitmap;
    }

    public static Bitmap createPalletLabel(Pallet pallet) throws WriterException {
        int width = 900;
        int height = 420;
        int padding = 34;
        Bitmap label = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(label);
        canvas.drawColor(Color.WHITE);

        Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
        border.setColor(Color.rgb(214, 222, 234));
        border.setStyle(Paint.Style.STROKE);
        border.setStrokeWidth(3);
        canvas.drawRect(2, 2, width - 2, height - 2, border);

        Bitmap qr = createQrBitmap(pallet.toQrPayload(), 280);
        canvas.drawBitmap(qr, padding, 70, null);

        Paint title = new Paint(Paint.ANTI_ALIAS_FLAG);
        title.setColor(Color.rgb(16, 42, 86));
        title.setTextSize(44);
        title.setFakeBoldText(true);

        Paint body = new Paint(Paint.ANTI_ALIAS_FLAG);
        body.setColor(Color.rgb(39, 52, 73));
        body.setTextSize(28);

        int textX = 360;
        canvas.drawText(pallet.code, textX, 86, title);
        drawWrapped(canvas, body, safe(pallet.skuName) + " | " + pallet.cartonCount + " Karton", textX, 138, 480, 34);
        canvas.drawText("Lot: " + safe(pallet.lotBatch), textX, 230, body);
        canvas.drawText("Exp: " + safe(pallet.expDate), textX, 274, body);
        canvas.drawText("QR Pallet - Budimas WMS", textX, 350, body);
        return label;
    }

    public static Bitmap createPalletSheet(List<Pallet> pallets) throws WriterException {
        int labelWidth = 900;
        int labelHeight = 420;
        int gap = 30;
        int columns = 1;
        int rows = Math.max(1, pallets.size());
        int width = labelWidth + gap * 2;
        int height = rows * labelHeight + (rows + 1) * gap;
        Bitmap sheet = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(sheet);
        canvas.drawColor(Color.WHITE);
        for (int i = 0; i < pallets.size(); i++) {
            int row = i / columns;
            Bitmap label = createPalletLabel(pallets.get(i));
            canvas.drawBitmap(label, gap, gap + row * (labelHeight + gap), null);
        }
        return sheet;
    }

    public static Bitmap createPalletThermalLabel(Pallet pallet, int paperWidthMm) throws WriterException {
        int width = thermalPaperDots(paperWidthMm);
        int padding = paperWidthMm >= 80 ? 24 : 16;
        int qrSize = paperWidthMm >= 80 ? 300 : 224;
        int height = paperWidthMm >= 80 ? 700 : 560;

        Bitmap label = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(label);
        canvas.drawColor(Color.WHITE);

        Paint title = new Paint(Paint.ANTI_ALIAS_FLAG);
        title.setColor(Color.BLACK);
        title.setTextSize(paperWidthMm >= 80 ? 38 : 30);
        title.setFakeBoldText(true);

        Paint body = new Paint(Paint.ANTI_ALIAS_FLAG);
        body.setColor(Color.BLACK);
        body.setTextSize(paperWidthMm >= 80 ? 28 : 22);

        Paint small = new Paint(Paint.ANTI_ALIAS_FLAG);
        small.setColor(Color.BLACK);
        small.setTextSize(paperWidthMm >= 80 ? 24 : 19);

        int y = paperWidthMm >= 80 ? 44 : 36;
        drawCentered(canvas, title, safe(pallet.code), width, y);

        Bitmap qr = createQrBitmap(pallet.toQrPayload(), qrSize);
        int qrX = (width - qrSize) / 2;
        int qrY = y + (paperWidthMm >= 80 ? 24 : 18);
        canvas.drawBitmap(qr, qrX, qrY, null);

        y = qrY + qrSize + (paperWidthMm >= 80 ? 42 : 34);
        y = drawCenteredWrapped(canvas, body, safe(pallet.skuName), y, width, width - padding * 2, paperWidthMm >= 80 ? 34 : 28);
        drawCentered(canvas, body, pallet.cartonCount + " Karton", width, y + 8);
        drawCentered(canvas, small, "Lot: " + safe(pallet.lotBatch), width, y + (paperWidthMm >= 80 ? 46 : 38));
        drawCentered(canvas, small, "Exp: " + safe(pallet.expDate), width, y + (paperWidthMm >= 80 ? 78 : 66));
        if (!safe(pallet.location).isEmpty()) {
            drawCentered(canvas, small, "Rak: " + safe(pallet.location), width, y + (paperWidthMm >= 80 ? 110 : 94));
        }
        drawCentered(canvas, small, "Budimas WMS", width, height - (paperWidthMm >= 80 ? 28 : 24));
        return label;
    }

    public static Bitmap createPalletThermalSheet(List<Pallet> pallets, int paperWidthMm) throws WriterException {
        int width = thermalPaperDots(paperWidthMm);
        int gap = paperWidthMm >= 80 ? 40 : 30;
        List<Bitmap> labels = new ArrayList<>();
        int height = gap;
        for (Pallet pallet : pallets) {
            Bitmap label = createPalletThermalLabel(pallet, paperWidthMm);
            labels.add(label);
            height += label.getHeight() + gap;
        }

        Bitmap sheet = Bitmap.createBitmap(width, Math.max(height, gap * 2), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(sheet);
        canvas.drawColor(Color.WHITE);
        int y = gap;
        for (Bitmap label : labels) {
            canvas.drawBitmap(label, 0, y, null);
            y += label.getHeight() + gap;
        }
        return sheet;
    }

    public static Bitmap createApiQrThermalLabel(String titleText, String qrData, int paperWidthMm) throws WriterException {
        int width = thermalPaperDots(paperWidthMm);
        int padding = paperWidthMm >= 80 ? 24 : 16;
        int qrSize = width - padding * 2;
        int height = qrSize + (paperWidthMm >= 80 ? 170 : 138);

        Bitmap label = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(label);
        canvas.drawColor(Color.WHITE);

        Paint title = new Paint(Paint.ANTI_ALIAS_FLAG);
        title.setColor(Color.BLACK);
        title.setTextSize(paperWidthMm >= 80 ? 32 : 26);
        title.setFakeBoldText(true);

        Paint small = new Paint(Paint.ANTI_ALIAS_FLAG);
        small.setColor(Color.BLACK);
        small.setTextSize(paperWidthMm >= 80 ? 24 : 19);

        int y = paperWidthMm >= 80 ? 42 : 34;
        drawCentered(canvas, title, safe(titleText), width, y);
        Bitmap qr = createQrBitmap(qrData, qrSize);
        canvas.drawBitmap(qr, padding, y + (paperWidthMm >= 80 ? 24 : 18), null);
        drawCentered(canvas, small, "Budimas WMS", width, height - (paperWidthMm >= 80 ? 28 : 24));
        return label;
    }

    public static Bitmap createPrinterTestLabel(int paperWidthMm, String printerName) throws WriterException {
        int width = thermalPaperDots(paperWidthMm);
        int padding = paperWidthMm >= 80 ? 24 : 16;
        int qrSize = paperWidthMm >= 80 ? 240 : 190;
        int height = paperWidthMm >= 80 ? 520 : 430;

        Bitmap label = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(label);
        canvas.drawColor(Color.WHITE);

        Paint title = new Paint(Paint.ANTI_ALIAS_FLAG);
        title.setColor(Color.BLACK);
        title.setTextSize(paperWidthMm >= 80 ? 36 : 28);
        title.setFakeBoldText(true);

        Paint body = new Paint(Paint.ANTI_ALIAS_FLAG);
        body.setColor(Color.BLACK);
        body.setTextSize(paperWidthMm >= 80 ? 26 : 21);

        int y = paperWidthMm >= 80 ? 44 : 36;
        drawCentered(canvas, title, "Budimas WMS", width, y);
        drawCentered(canvas, body, "Tes Printer Bluetooth", width, y + (paperWidthMm >= 80 ? 42 : 34));
        y += paperWidthMm >= 80 ? 78 : 64;
        y = drawCenteredWrapped(canvas, body, safe(printerName), y, width, width - padding * 2, paperWidthMm >= 80 ? 34 : 28);

        Bitmap qr = createQrBitmap("BUDIMAS-WMS|PRINTER-TEST", qrSize);
        canvas.drawBitmap(qr, (width - qrSize) / 2, y + 18, null);
        drawCentered(canvas, body, paperWidthMm + " mm ESC/POS", width, height - (paperWidthMm >= 80 ? 34 : 28));
        return label;
    }

    private static void drawWrapped(Canvas canvas, Paint paint, String text, int x, int y, int maxWidth, int lineHeight) {
        for (String line : wrapText(text, paint, maxWidth)) {
            canvas.drawText(line, x, y, paint);
            y += lineHeight;
        }
    }

    private static void drawCentered(Canvas canvas, Paint paint, String text, int width, int y) {
        Paint.Align previous = paint.getTextAlign();
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(safe(text), width / 2f, y, paint);
        paint.setTextAlign(previous);
    }

    private static int drawCenteredWrapped(Canvas canvas, Paint paint, String text, int y, int width, int maxWidth, int lineHeight) {
        Paint.Align previous = paint.getTextAlign();
        paint.setTextAlign(Paint.Align.CENTER);
        for (String line : wrapText(safe(text), paint, maxWidth)) {
            canvas.drawText(line, width / 2f, y, paint);
            y += lineHeight;
        }
        paint.setTextAlign(previous);
        return y;
    }

    private static int thermalPaperDots(int paperWidthMm) {
        return paperWidthMm >= 80 ? 576 : 384;
    }

    private static List<String> wrapText(String text, Paint paint, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder current = new StringBuilder();
        Rect bounds = new Rect();
        for (String word : words) {
            String candidate = current.length() == 0 ? word : current + " " + word;
            paint.getTextBounds(candidate, 0, candidate.length(), bounds);
            if (bounds.width() > maxWidth && current.length() > 0) {
                lines.add(current.toString());
                current = new StringBuilder(word);
            } else {
                current = new StringBuilder(candidate);
            }
        }
        if (current.length() > 0) {
            lines.add(current.toString());
        }
        return lines;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
