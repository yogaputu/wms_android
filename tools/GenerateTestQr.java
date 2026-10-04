import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.EnumMap;
import java.util.Map;

public class GenerateTestQr {
    private static final int QR_SIZE = 620;
    private static final int WIDTH = 780;
    private static final int HEIGHT = 840;

    public static void main(String[] args) throws Exception {
        File outDir = new File("test_qr");
        if (!outDir.exists() && !outDir.mkdirs()) {
            throw new IllegalStateException("Cannot create test_qr directory");
        }

        writeQr("pallet_PLT-000001.png", "BUDIMAS-WMS|PALLET|PLT-000001", "PALLET", "PLT-000001");
        writeQr("pallet_PLT-000002.png", "BUDIMAS-WMS|PALLET|PLT-000002", "PALLET", "PLT-000002");
        writeQr("lokasi_G1R01L0K01N01.png", "BUDIMAS-WMS|LOCATION|G1R01L0K01N01", "LOKASI RAK", "G1R01L0K01N01");
        writeQr("lokasi_G1R01L0K01N02.png", "BUDIMAS-WMS|LOCATION|G1R01L0K01N02", "LOKASI RAK", "G1R01L0K01N02");
        writeQr("lokasi_G1R03L2K01N01.png", "BUDIMAS-WMS|LOCATION|G1R03L2K01N01", "LOKASI RAK", "G1R03L2K01N01");
        writeQr("incoming_note_PTR-20260626-0013-132538.png", "PTR-20260626-0013-132538", "NOTA INCOMING", "PTR-20260626-0013-132538");
        writeQr("rak_titipan_G1R03L2K01N01.png", "BUDIMAS-WMS|LOCATION|G1R03L2K01N01", "RAK TITIPAN", "G1R03L2K01N01");
        writeQr("rak_titipan_G1R03L2K02N01.png", "BUDIMAS-WMS|LOCATION|G1R03L2K02N01", "RAK TITIPAN", "G1R03L2K02N01");
        writeQr("rak_tetap_G1R01L0K05N01.png", "BUDIMAS-WMS|LOCATION|G1R01L0K05N01", "RAK TETAP", "G1R01L0K05N01");
        writeQr("rak_lorong_G1L01L0K05N01.png", "BUDIMAS-WMS|LOCATION|G1L01L0K05N01", "RAK LORONG", "G1L01L0K05N01");
        writeQr("barang_CA2A069_test.png", "CA2A069;G1R03L2K01N01;PLT-TEST001;500;0;0;915 AA-4 PERAK;BATCH;2026-12-31", "BARANG TEST", "CA2A069");
    }

    private static void writeQr(String fileName, String payload, String title, String label) throws Exception {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.MARGIN, 2);
        BitMatrix matrix = new MultiFormatWriter().encode(payload, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE, hints);

        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        int xOffset = (WIDTH - QR_SIZE) / 2;
        int yOffset = 42;
        for (int x = 0; x < QR_SIZE; x++) {
            for (int y = 0; y < QR_SIZE; y++) {
                image.setRGB(x + xOffset, y + yOffset, matrix.get(x, y) ? Color.BLACK.getRGB() : Color.WHITE.getRGB());
            }
        }

        g.setColor(new Color(16, 42, 86));
        g.setFont(new Font("SansSerif", Font.BOLD, 36));
        drawCentered(g, title, 705);
        g.setFont(new Font("SansSerif", Font.BOLD, 54));
        drawCentered(g, label, 765);
        g.setFont(new Font("SansSerif", Font.PLAIN, 18));
        g.setColor(new Color(93, 107, 128));
        drawCentered(g, payload, 810);
        g.dispose();

        ImageIO.write(image, "png", new File("test_qr", fileName));
    }

    private static void drawCentered(Graphics2D g, String text, int y) {
        FontMetrics fm = g.getFontMetrics();
        int x = (WIDTH - fm.stringWidth(text)) / 2;
        g.drawString(text, x, y);
    }
}
