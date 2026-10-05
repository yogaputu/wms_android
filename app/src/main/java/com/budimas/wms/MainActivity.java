package com.budimas.wms;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;
import androidx.print.PrintHelper;

import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner;
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning;
import com.google.zxing.WriterException;

import java.io.File;
import java.io.FileOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Calendar;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private static final int REQ_ENABLE_BLUETOOTH = 701;
    private static final int REQ_BLUETOOTH_PERMISSIONS = 702;
    private static final String UI_PREF_NAME = "budimas_wms_ui";
    private static final String KEY_THEME_MODE = "theme_mode";
    private static final String THEME_SYSTEM = "Sistem";
    private static final String THEME_LIGHT = "Terang";
    private static final String THEME_DARK = "Gelap";
    private int BLUE = Color.rgb(11, 94, 215);
    private int INK = Color.rgb(16, 42, 86);
    private int MUTED = Color.rgb(93, 107, 128);
    private int BG = Color.rgb(244, 247, 251);
    private int CARD = Color.WHITE;
    private int CARD_BORDER = Color.rgb(220, 228, 240);
    private int SUBTLE = Color.rgb(244, 248, 255);
    private int SUBTLE_BORDER = Color.rgb(198, 217, 250);
    private int ROW_BORDER = Color.rgb(234, 239, 247);
    private int GREEN = Color.rgb(25, 135, 84);
    private int AMBER = Color.rgb(176, 120, 0);
    private static final Pattern WMS_RACK_PATTERN = Pattern.compile("G\\d+R\\d+L\\d+K\\d+N\\d+", Pattern.CASE_INSENSITIVE);
    private static final Pattern WMS_PRODUCT_PATTERN = Pattern.compile("[A-Z0-9][A-Z0-9._-]{3,}", Pattern.CASE_INSENSITIVE);
    private static final Pattern WMS_MANIFEST_PATTERN = Pattern.compile("\\b(?:MNF|RETUR)-[A-Z0-9-]+\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern WMS_RETURN_PATTERN = Pattern.compile("\\bKPR/[A-Z0-9._-]+/[A-Z0-9._-]+\\b", Pattern.CASE_INSENSITIVE);

    private WmsStore store;
    private AuthSession authSession;
    private BudimasApiClient apiClient;
    private PrinterSettings printerSettings;
    private SharedPreferences uiPrefs;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final java.util.ArrayList<String[]> discoveredBluetoothDevices = new java.util.ArrayList<>();
    private LinearLayout content;
    private Pallet selectedPallet;
    private JSONObject selectedApiPickingRow;
    private JSONObject selectedTransferStock;
    private String currentApiPickingNota = "";
    private String currentApiPickerName = "";
    private String scanMode = "";
    private String currentScreen = "dashboard";
    private String warehouseFilter = "ALL";
    private String warehouseQuery = "";
    private String rackQuery = "";
    private String rackTypeFilter = "";
    private String inventoryQuery = "";
    private String inventoryStatusFilter = "";
    private String transactionQuery = "";
    private String transactionTypeFilter = "";
    private String loadingQuery = "";
    private JSONObject loadingSelection;
    private boolean pickingByVariant = true;
    private boolean shipmentPickingEditable = false;
    private String droppingDriverQuery = "";
    private String quarantineQuery = "";
    // QC no longer accepts an arbitrary rack string. This tracks the rack
    // selected from the branch-scoped search picker for the current item.
    private int quarantineRackContextId = 0;
    private String quarantineTargetRack = "";
    private String warehouseReturnReference = "";
    private String stageTwoQuery = "";
    private String transferQuery = "";
    private String transferSourceRack = "";
    private String transferSortMode = "FEFO";
    private String reprintQuery = "";
    private String stockOpnameSoNo = "";
    private String stockOpnamePrinciple = "";
    private String stockOpnamePic = "";
    private String stockOpnameLocation = "";
    private int stockOpnameScheduleId = 0;
    private String stockOpnameScheduleStatus = "";
    private String stockOpnameExecutionDate = "";
    private JSONArray stockOpnameRackRows = new JSONArray();
    private int stockOpnameScannedItems = 0;
    private final java.util.LinkedHashMap<String, String> stockOpnameLocationStatus = new java.util.LinkedHashMap<>();
    private final java.util.LinkedHashMap<String, Boolean> stockOpnameLocationTitipan = new java.util.LinkedHashMap<>();
    private int pendingTransferQty = 0;
    private String pendingTransferTargetRack = "";
    private JSONObject selectedLoadingDriver;
    private JSONObject selectedLoadingVehicle;
    private final LinkedHashMap<String, JSONObject> selectedLoadingHelpers = new LinkedHashMap<>();
    private String selectedLoadingManifest = "";
    private boolean transferManualTargetOpen = false;
    private boolean transferFilterOpen = false;
    private boolean transferTargetPanelOpen = false;
    private boolean incomingManualOpen = false;
    private boolean pickingManualOpen = false;
    private boolean rackFilterOpen = false;
    private boolean inventoryFilterOpen = false;
    private boolean transactionFilterOpen = false;
    private boolean warehouseFilterOpen = false;
    private boolean reprintFilterOpen = false;
    private boolean loadingFilterOpen = false;
    private boolean droppingFilterOpen = false;
    private boolean quarantineFilterOpen = false;
    private boolean stockOpnameScheduleOpen = true;
    private boolean stockOpnameManualOpen = false;
    private boolean settingsAppearanceOpen = false;
    private boolean settingsPrinterOpen = false;
    private boolean settingsPrinterConfigOpen = false;
    private boolean bluetoothScanning = false;
    private boolean bluetoothDiscoveryReceiverRegistered = false;
    private BroadcastReceiver bluetoothDiscoveryReceiver;

    private interface PalletAction {
        void onSelect(Pallet pallet);
    }

    private interface MasterSelectionAction {
        void onSelect(JSONObject item);
    }

    private interface MasterMultiSelectionAction {
        void onSelect(LinkedHashMap<String, JSONObject> items);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new WmsStore(this);
        authSession = new AuthSession(this);
        apiClient = new BudimasApiClient(authSession);
        printerSettings = new PrinterSettings(this);
        uiPrefs = getSharedPreferences(UI_PREF_NAME, Context.MODE_PRIVATE);
        applyThemeColors();
        applySystemBars();
        if (authSession.isLoggedIn()) {
            showDashboard();
        } else {
            showLogin();
        }
    }

    @Override
    public void onBackPressed() {
        if ("login".equals(currentScreen)) {
            super.onBackPressed();
            return;
        }
        if (!"dashboard".equals(currentScreen)) {
            showDashboard();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_ENABLE_BLUETOOTH) {
            if (resultCode == RESULT_OK) {
                toast("Bluetooth aktif");
                showSettingsWithDevices(null, "Bluetooth sudah aktif. Tekan Cari Perangkat Baru untuk scan printer.");
            } else {
                toast("Bluetooth belum diaktifkan");
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQ_BLUETOOTH_PERMISSIONS) {
            return;
        }

        boolean granted = true;
        for (int grantResult : grantResults) {
            if (grantResult != PackageManager.PERMISSION_GRANTED) {
                granted = false;
                break;
            }
        }

        if (granted) {
            toast("Izin Bluetooth diberikan");
            showSettingsWithDevices(null, "Izin Bluetooth aktif. Tekan Cari Perangkat Baru untuk scan.");
        } else {
            showSettingsWithDevices(null, "Izin Bluetooth ditolak. Scan perangkat tidak bisa dijalankan.");
        }
    }

    @Override
    protected void onDestroy() {
        stopBluetoothDiscovery();
        super.onDestroy();
    }

    private void showLogin() {
        currentScreen = "login";
        LinearLayout root = vertical();
        root.setBackgroundColor(BG);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(18), dp(18), dp(18), dp(18));

        ScrollView scroll = new ScrollView(this);
        LinearLayout wrapper = vertical();
        wrapper.setGravity(Gravity.CENTER_HORIZONTAL);
        wrapper.setPadding(0, dp(22), 0, dp(22));
        scroll.addView(wrapper, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView brand = title("Budimas WMS", 28);
        brand.setGravity(Gravity.CENTER);
        wrapper.addView(brand);

        TextView endpoint = small(AuthSession.API_BASE_URL);
        endpoint.setGravity(Gravity.CENTER);
        endpoint.setTextColor(BLUE);
        wrapper.addView(endpoint);
        wrapper.addView(space(18));

        LinearLayout form = card();
        form.addView(sectionTitle("Login"));
        form.addView(body("Masuk memakai akun Budimas seperti di aplikasi web."));

        EditText username = input("", "Username / email", InputType.TYPE_CLASS_TEXT);
        EditText password = input("", "Password", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        form.addView(username);
        form.addView(password);

        TextView error = body("");
        error.setTextColor(Color.rgb(190, 18, 60));
        error.setVisibility(View.GONE);
        form.addView(error);

        Button loginButton = primaryButton("Login", v -> {
        });
        loginButton.setOnClickListener(v -> {
            String identity = value(username);
            String secret = value(password);
            if (identity.isEmpty() || secret.isEmpty()) {
                error.setText("Username dan password wajib diisi.");
                error.setVisibility(View.VISIBLE);
                return;
            }
            loginButtonState(loginButton, false, "Memproses...");
            error.setVisibility(View.GONE);
            performLogin(identity, secret, loginButton, error);
        });
        form.addView(loginButton);

        wrapper.addView(form);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(root);
    }

    private void performLogin(String identity, String password, Button loginButton, TextView errorView) {
        executor.execute(() -> {
            try {
                JSONObject loginData = apiClient.login(identity, password);
                String token = AuthSession.firstString(loginData, "access_token", "token", "accessToken");
                if (token.isEmpty()) {
                    throw new IllegalStateException("Login berhasil, tetapi token tidak tersedia dari API.");
                }

                authSession.save(loginData);
                try {
                    JSONObject profile = apiClient.fetchProfile(token);
                    authSession.updateProfile(profile);
                } catch (Exception ignored) {
                    // Login tetap valid bila profil sudah ikut response login API lama.
                }

                uiHandler.post(() -> {
                    toast("Login berhasil");
                    showDashboard();
                });
            } catch (Exception e) {
                uiHandler.post(() -> {
                    errorView.setText(resolveLoginError(e));
                    errorView.setVisibility(View.VISIBLE);
                    loginButtonState(loginButton, true, "Login");
                });
            }
        });
    }

    private void loginButtonState(Button button, boolean enabled, String text) {
        button.setEnabled(enabled);
        button.setText(text);
        button.setAlpha(enabled ? 1f : 0.7f);
    }

    private String resolveLoginError(Exception error) {
        String message = error.getMessage() == null ? "" : error.getMessage();
        String lower = message.toLowerCase(Locale.US);
        if (lower.contains("failed to connect") || lower.contains("timeout") || lower.contains("timed out")) {
            return "Tidak bisa terhubung ke API. Periksa internet atau server API.";
        }
        if (error instanceof BudimasApiClient.ApiException) {
            int status = ((BudimasApiClient.ApiException) error).statusCode;
            if (status == 401 || status == 403) {
                return "Username/password tidak sesuai atau akses WMS belum diberikan.";
            }
            if (status == 404) {
                return "Endpoint login belum ditemukan. Hubungi admin untuk cek konfigurasi API.";
            }
            if (status >= 500) {
                return "Server WMS sedang bermasalah. Tunggu sebentar, lalu coba login lagi.";
            }
        }
        if (lower.contains("password") || lower.contains("user") || lower.contains("credential") || lower.contains("401") || lower.contains("403")) {
            return "Username atau password tidak sesuai.";
        }
        return "Login gagal. Periksa username/password, lalu coba lagi.";
    }

    private void showDashboard() {
        currentScreen = "dashboard";
        setShell("Budimas WMS", "Scan gudang, incoming, transfer, picking", "dashboard", false);

        content.addView(dashboardUserCard());

        content.addView(primaryActionPanel());
        content.addView(secondaryActionPanel());
        content.addView(extraActionPanel());
    }

    private LinearLayout wmsFlowCard() {
        LinearLayout flow = card();
        flow.addView(sectionTitle("Flow WMS Mobile"));
        flow.addView(body("Barang datang sudah cetak QR dari admin dan semua masuk Rak Titipan. Transfer memindahkan ke Rak Tetap; jika penuh, sistem memakai Rak Lorong kolom yang sama."));

        LinearLayout firstRow = horizontal();
        firstRow.setPadding(0, dp(10), 0, 0);
        firstRow.addView(flowChip("1 Barang Datang", BLUE));
        firstRow.addView(flowChip("2 QR Admin", Color.rgb(111, 66, 193)));
        firstRow.addView(flowChip("3 Rak Titipan", GREEN));
        flow.addView(firstRow);

        LinearLayout secondRow = horizontal();
        secondRow.setPadding(0, dp(6), 0, 0);
        secondRow.addView(flowChip("4 Scan Rak Tetap", AMBER));
        secondRow.addView(flowChip("5 Jika Penuh: Lorong", Color.rgb(14, 116, 144)));
        secondRow.addView(flowChip("6 Scan Barang", Color.rgb(220, 53, 69)));
        flow.addView(secondRow);

        LinearLayout thirdRow = horizontal();
        thirdRow.setPadding(0, dp(6), 0, 0);
        thirdRow.addView(flowChip("7 Update Rak", BLUE));
        thirdRow.addView(flowChip("8 Inventory Scan", INK));
        thirdRow.addView(flowChip("Web Monitor", GREEN));
        flow.addView(thirdRow);
        return flow;
    }

    private TextView flowChip(String text, int color) {
        TextView chip = small(text);
        chip.setTextColor(color);
        chip.setTypeface(Typeface.DEFAULT_BOLD);
        chip.setGravity(Gravity.CENTER);
        chip.setSingleLine(true);
        chip.setEllipsize(TextUtils.TruncateAt.END);
        chip.setBackground(round(colorWithAlpha(color, 24), colorWithAlpha(color, 80), 100));
        chip.setPadding(dp(8), dp(8), dp(8), dp(8));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(38), 1f);
        lp.setMargins(0, 0, dp(6), 0);
        chip.setLayoutParams(lp);
        return chip;
    }

    private void showReceivingForm() {
        currentScreen = "receiving";
        setShell("Receiving Barang", "Input barang datang sebelum dibuat pallet", "receiving", true);
        content.addView(stepper(1));

        LinearLayout form = card();
        form.addView(sectionTitle("Detail Barang"));

        EditText po = input("PO-240601", "No. PO / SJ", InputType.TYPE_CLASS_TEXT);
        EditText supplier = input("PT. Tirta Sumber Abadi", "Supplier", InputType.TYPE_CLASS_TEXT);
        EditText date = input(todayString(), "Tanggal", InputType.TYPE_CLASS_TEXT);
        EditText sku = input("AQUA 600ml", "SKU / Barang", InputType.TYPE_CLASS_TEXT);
        EditText lot = input("AQ240601", "Lot / Batch", InputType.TYPE_CLASS_TEXT);
        EditText exp = input("01-06-2027", "Exp Date", InputType.TYPE_CLASS_TEXT);
        EditText total = input("240", "Total Datang (Karton)", InputType.TYPE_CLASS_NUMBER);
        EditText capacity = input("40", "Kapasitas Pallet (Maks)", InputType.TYPE_CLASS_NUMBER);

        form.addView(po);
        form.addView(supplier);
        form.addView(date);
        form.addView(sku);
        form.addView(lot);
        form.addView(exp);
        form.addView(total);
        form.addView(capacity);
        form.addView(space(10));
        form.addView(primaryButton("Lanjut Buat Palet", v -> {
            IncomingRecord record = new IncomingRecord();
            record.poNumber = value(po);
            record.supplier = value(supplier);
            record.date = value(date);
            record.skuName = value(sku);
            record.lotBatch = value(lot);
            record.expDate = value(exp);
            record.totalCartons = parsePositiveInt(value(total), 0);
            record.capacityPerPallet = parsePositiveInt(value(capacity), 40);

            if (record.skuName.isEmpty() || record.lotBatch.isEmpty() || record.totalCartons <= 0) {
                toast("SKU, lot, dan total datang wajib diisi");
                return;
            }
            showPalletPlan(record);
        }));
        content.addView(form);
    }

    private void showPalletPlan(IncomingRecord record) {
        currentScreen = "pallet_plan";
        setShell("Buat Palet", "Sistem menghitung pallet dari total karton", "receiving", true);
        content.addView(stepper(2));

        LinearLayout info = card();
        info.addView(sectionTitle("Perhitungan Palet"));
        info.addView(keyValue("Total Karton", record.totalCartons + " Karton"));
        info.addView(keyValue("Kapasitas / Pallet", record.capacityPerPallet + " Karton"));
        info.addView(keyValue("Akan Dibuat", record.palletCount() + " Pallet"));
        TextView note = small("1 pallet = 1 QR code. Pallet terakhir otomatis mengikuti sisa karton.");
        note.setTextColor(GREEN);
        info.addView(note);
        content.addView(info);

        LinearLayout detail = card();
        detail.addView(sectionTitle("Barang Datang"));
        detail.addView(keyValue("No. PO / SJ", safe(record.poNumber)));
        detail.addView(keyValue("Supplier", safe(record.supplier)));
        detail.addView(keyValue("Tanggal", safe(record.date)));
        detail.addView(keyValue("SKU / Barang", safe(record.skuName)));
        detail.addView(keyValue("Lot / Batch", safe(record.lotBatch)));
        detail.addView(keyValue("Exp Date", safe(record.expDate)));
        content.addView(detail);

        content.addView(primaryButton("Buat Palet & Cetak QR", v -> {
            if (!validateReceivingRecord(record)) {
                return;
            }
            List<Pallet> created = store.createPallets(record, currentUserName());
            if (created.isEmpty()) {
                toast("Tidak ada pallet yang dibuat");
                return;
            }
            showPalletResult(created);
        }));
        content.addView(space(8));
        content.addView(secondaryButton("Edit Data Receiving", v -> showReceivingForm()));
    }

    private void showPalletResult(List<Pallet> pallets) {
        currentScreen = "pallet_result";
        setShell("Hasil Pallet & QR", "QR siap dicetak atau dibagikan", "receiving", true);
        content.addView(stepper(3));

        LinearLayout success = card();
        success.setBackground(successBackground());
        success.addView(title("Berhasil membuat " + pallets.size() + " pallet", 18));
        int total = 0;
        for (Pallet pallet : pallets) {
            total += pallet.cartonCount;
        }
        success.addView(body("Total karton: " + total));
        content.addView(success);

        content.addView(primaryButton("Cetak Semua QR", v -> printPallets(pallets)));
        content.addView(space(8));
        content.addView(secondaryButton("Lanjut Putaway", v -> showPutawayScanPallet()));

        LinearLayout list = card();
        list.addView(sectionTitle("Daftar QR Pallet"));
        for (Pallet pallet : pallets) {
            list.addView(palletQrRow(pallet));
        }
        content.addView(list);

        if (shouldAutoPrintBluetooth()) {
            content.postDelayed(() -> printPallets(pallets), 450);
        }
    }

    private void showApiIncoming() {
        currentScreen = "api_incoming";
        setShell("Incoming", "Scan nota barang datang", "receiving", true);

        LinearLayout scan = card();
        scan.addView(sectionTitle("Scan Nota"));
        scan.addView(body("Alur utama: scan QR/barcode nota incoming dari admin gudang."));
        scan.addView(primaryButton("Scan QR / Barcode", v -> startScanner("API_INCOMING_NOTA")));
        scan.addView(dropdownHeader("Input Manual Nota", incomingManualOpen, v -> {
            incomingManualOpen = !incomingManualOpen;
            showApiIncoming();
        }));
        if (incomingManualOpen) {
            EditText nota = input("", "Nomor nota incoming", InputType.TYPE_CLASS_TEXT);
            scan.addView(nota);
            scan.addView(secondaryButton("Muat Manual", v -> loadApiIncoming(value(nota))));
        } else {
            scan.addView(small("Input manual disembunyikan. Tap bar ini jika QR tidak bisa discan."));
        }
        content.addView(scan);
    }

    private void loadApiIncoming(String nota) {
        if (nota.isEmpty()) {
            toast("Nomor nota wajib diisi");
            return;
        }
        showLoadingScreen("Incoming", "Memuat nota " + nota + "...");
        executor.execute(() -> {
            try {
                JSONObject response = apiClient.getWmsIncomingNoteDetails(nota);
                uiHandler.post(() -> renderApiIncoming(nota, response));
            } catch (Exception e) {
                uiHandler.post(() -> showApiError("Incoming", "Incoming note belum bisa dimuat.", e, () -> showApiIncoming()));
            }
        });
    }

    private void renderApiIncoming(String nota, JSONObject response) {
        currentScreen = "api_incoming_result";
        setShell("Barang Datang", "Nota " + nota, "receiving", true);

        JSONArray rows = normalizeRows(response);
        LinearLayout list = card();
        list.addView(sectionTitle("Barang Datang"));
        if (rows.length() > 0) {
            list.addView(small(rows.length() + " item siap dicek"));
        }
        if (rows.length() == 0) {
            list.addView(space(8));
            list.addView(body("Item incoming belum tersedia."));
        }
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.optJSONObject(i);
            if (row != null) {
                list.addView(apiIncomingRow(row, nota));
            }
        }
        content.addView(list);
        content.addView(secondaryButton("Muat Nota Lain", v -> showApiIncoming()));
    }

    private LinearLayout apiIncomingRow(JSONObject row, String nota) {
        LinearLayout wrapper = vertical();
        wrapper.setPadding(0, dp(12), 0, dp(12));
        wrapper.setBackground(separatorBackground());

        String code = first(row, "KodeBarang", "KodeStok", "kode_barang", "product_code");
        String name = first(row, "NamaBarang", "nama_barang", "product_name");
        String rawStatus = first(row, "StatusGudang", "status_gudang", "status");
        boolean processed = row.optBoolean("SudahProses")
                || "DONE".equalsIgnoreCase(rawStatus)
                || "READY".equalsIgnoreCase(rawStatus);
        String statusLabel = processed ? "Sudah Konfirmasi Gudang" : "Belum Konfirmasi Gudang";
        LinearLayout top = horizontal();
        LinearLayout text = vertical();
        TextView codeText = title(fallback(code, "-"), 15);
        codeText.setSingleLine(true);
        codeText.setEllipsize(TextUtils.TruncateAt.END);
        text.addView(codeText);
        TextView nameText = small(fallback(name, "-"));
        nameText.setSingleLine(true);
        nameText.setEllipsize(TextUtils.TruncateAt.END);
        text.addView(nameText);
        top.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        top.addView(badge(processed ? "OK" : "Belum", processed ? GREEN : AMBER));
        wrapper.addView(top);
        wrapper.addView(small(incomingQtyText(row)));
        String titipanRack = first(row, "KodeRakTitipan", "kode_rak_titipan", "kode_rak_asal");
        if (!titipanRack.isEmpty()) {
            wrapper.addView(small("Titipan: " + titipanRack));
        }
        String zona = first(row, "ZonaPenyimpanan", "zona_penyimpanan");
        String gudang = first(row, "GudangRekomendasi", "gudang_rekomendasi");
        if (!zona.isEmpty() || !gudang.isEmpty()) {
            String labelZona = "NON_FOOD".equalsIgnoreCase(zona)
                    ? "Non Food"
                    : "FOOD".equalsIgnoreCase(zona)
                    ? "Food"
                    : fallback(zona, "Belum dipetakan");
            wrapper.addView(small("Zona otomatis: " + labelZona + " · Gedung G" + fallback(gudang, "1")));
        }
        if (!processed) {
            wrapper.addView(tinyButton("Konfirmasi", v -> processApiIncoming(row, nota)));
        }
        return wrapper;
    }

    private void processApiIncoming(JSONObject row, String nota) {
        String code = first(row, "KodeBarang", "KodeStok", "kode_barang", "product_code");
        String name = first(row, "NamaBarang", "nama_barang", "product_name");
        int perUnit = incomingPerUnit(row);
        int totalQty = incomingTotalQty(row);
        int qtyCt = totalQty / perUnit;
        int qtyPc = totalQty % perUnit;
        if (code.isEmpty()) {
            toast("Barang incoming belum jelas. Muat ulang nota atau pilih item lain.");
            return;
        }
        if (totalQty <= 0) {
            toast("Qty incoming harus lebih dari 0 sebelum proses QR.");
            return;
        }
        showLoadingScreen("Konfirmasi Gudang", "Memproses barang " + fallback(code, nota) + " ke Rak Titipan...");
        executor.execute(() -> {
            try {
                JSONObject payload = new JSONObject(row.toString());
                payload.put("qty_ct_palet", qtyCt);
                payload.put("qty_pc_palet", qtyPc);
                payload.put("qty_pcs", totalQty);
                // Do not replace the PO receipt's batch/expiry with a fake
                // fallback.  The incoming endpoint stores these values per
                // pallet and uses them for FIFO/FEFO later on.
                String batch = first(row, "batch", "Batch", "batch_number");
                String expired = apiDateOnly(first(row, "expired", "Expired", "expired_date", "expiry_date"));
                payload.remove("batch");
                payload.remove("Batch");
                payload.remove("batch_number");
                payload.remove("expired");
                payload.remove("Expired");
                payload.remove("expired_date");
                payload.remove("expiry_date");
                putIfNotEmpty(payload, "batch", batch);
                putIfNotEmpty(payload, "batch_number", batch);
                putIfNotEmpty(payload, "expired", expired);
                putIfNotEmpty(payload, "expired_date", expired);
                payload.put("reference_no", nota);
                putIfNotEmpty(payload, "id_cabang", currentBranchId());
                putIfNotEmpty(payload, "user_id", currentUserId());

                JSONObject result = apiClient.processWmsIncomingPallet(payload);
                uiHandler.post(() -> {
                    String rack = "";
                    JSONObject dataRack = result.optJSONObject("data_rak");
                    if (dataRack != null) {
                        rack = first(dataRack, "KodeRak", "kode_rak");
                    }
                    recordLocalTransaction("Barang Datang", nota, "", code, name, "-", rack, totalQty, "PCS", "Barang dikonfirmasi masuk ke Rak Titipan dan menunggu transfer rak.");
                    showApiIncomingProcessed(code, result, () -> loadApiIncoming(nota));
                });
            } catch (Exception e) {
                uiHandler.post(() -> showApiError("Konfirmasi Gudang", "Konfirmasi Rak Titipan belum berhasil diproses.", e, () -> loadApiIncoming(nota)));
            }
        });
    }

    private void showApiIncomingProcessed(String code, JSONObject result, Runnable reload) {
        currentScreen = "api_incoming_done";
        setShell("Konfirmasi Gudang", fallback(code, "Incoming berhasil diproses"), "receiving", true);

        LinearLayout success = card();
        success.setBackground(successBackground());
        success.addView(title("Konfirmasi Berhasil", 20));
        success.addView(body(first(result, "msg", "message")));
        JSONObject rack = result.optJSONObject("data_rak");
        if (rack != null) {
            success.addView(keyValue("Rak Titipan", first(rack, "KodeRak", "kode_rak")));
            success.addView(keyValue("Produk", first(rack, "NamaBarang", "nama_barang", "KodeBarang", "kode_barang")));
        }
        String zona = first(result, "ZonaPenyimpanan", "zona_penyimpanan");
        String gudang = first(result, "GudangRekomendasi", "gudang_rekomendasi");
        if (!zona.isEmpty() || !gudang.isEmpty()) {
            String labelZona = "NON_FOOD".equalsIgnoreCase(zona)
                    ? "Non Food"
                    : "FOOD".equalsIgnoreCase(zona)
                    ? "Food"
                    : fallback(zona, "Belum dipetakan");
            success.addView(keyValue("Zona penyimpanan", labelZona + " · Gedung G" + fallback(gudang, "1")));
        }
        content.addView(success);

        String qrData = first(result, "qr_data", "qr", "barcode");
        if (!qrData.isEmpty()) {
            LinearLayout qrCard = card();
            qrCard.addView(sectionTitle("QR Palet dari API"));
            ImageView qr = new ImageView(this);
            try {
                qr.setImageBitmap(QrUtils.createQrBitmap(qrData, 520));
            } catch (WriterException e) {
                qr.setBackgroundColor(Color.LTGRAY);
            }
            qrCard.addView(qr, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(280)));
            qrCard.addView(primaryButton("Cetak QR Palet", v -> printApiQr(fallback(code, "QR Palet"), qrData)));
            content.addView(qrCard);
            if (shouldAutoPrintBluetooth()) {
                content.postDelayed(() -> printApiQr(fallback(code, "QR Palet"), qrData), 450);
            }
        }
        content.addView(secondaryButton("Kembali ke Incoming", v -> reload.run()));
    }

    private void showApiPicking() {
        currentScreen = "api_picking";
        selectedApiPickingRow = null;
        setShell("Picking", "Ambil barang dari Rak Tetap/Lorong", "putaway", true);

        LinearLayout form = card();
        form.addView(sectionTitle("Draf Kiriman"));
        form.addView(body("Scan QR draf kiriman, lalu pilih item yang sudah berada di Rak Tetap atau Rak Lorong. Stok Rak Titipan harus ditransfer dulu."));
        form.addView(primaryButton("Scan QR Draf Kiriman", v -> startScanner("API_PICKING_NOTA")));
        form.addView(dropdownHeader("Input Referensi Draf Kiriman", pickingManualOpen, v -> {
            pickingManualOpen = !pickingManualOpen;
            showApiPicking();
        }));
        if (pickingManualOpen) {
            EditText nota = input("", "Referensi DRF-cabang-ID", InputType.TYPE_CLASS_TEXT);
            form.addView(nota);
            form.addView(secondaryButton("Muat Draft", v -> loadApiPicking(value(nota))));
        } else {
            form.addView(small("Input manual disembunyikan agar layar picking lebih lega."));
        }
        content.addView(form);
    }

    private void loadApiPicking(String nota) {
        if (ShipmentReference.parse(nota).isEmpty()) {
            toast("Scan QR draf kiriman atau isi DRF-cabang-ID, bukan nomor nota.");
            return;
        }
        currentApiPickingNota = nota;
        showLoadingScreen("Picking", "Memuat draft " + nota + "...");
        executor.execute(() -> {
            try {
                JSONObject response = apiClient.getWmsPickingDraftDetail(nota);
                uiHandler.post(() -> renderApiPicking(nota, response));
            } catch (Exception e) {
                uiHandler.post(() -> showApiError("Picking", "Draft picking belum bisa dimuat.", e, () -> showApiPicking()));
            }
        });
    }

    private void renderApiPicking(String nota, JSONObject response) {
        currentScreen = "api_picking_result";
        JSONObject draftDocument = response.optJSONObject("document");
        currentApiPickingNota = draftDocument == null ? nota : draftDocument.optString("shipment_reference", nota);
        shipmentPickingEditable = true;
        JSONArray states = draftDocument == null ? null : draftDocument.optJSONArray("task_statuses");
        if (states != null) for (int i = 0; i < states.length(); i++) {
            if (!java.util.Arrays.asList("DRAFT", "PICKING", "PICKED", "PICKING_VOID").contains(states.optString(i))) shipmentPickingEditable = false;
        }
        setShell("Picking", "Draft " + nota, "putaway", true);

        JSONArray rows = normalizeRows(response);
        LinearLayout list = card();
        list.addView(sectionTitle("Item Picking"));
        list.addView(body(rows.length() + " item dari API"));
        EditText picker = input(currentApiPickerName, "Nama picker", InputType.TYPE_CLASS_TEXT);
        list.addView(picker);
        if (rows.length() == 0) {
            list.addView(space(8));
            list.addView(body("Draft picking belum tersedia."));
        }
        list.addView(secondaryButton(pickingByVariant ? "By variant ✓ / Tampilkan by nota" : "By nota ✓ / Tampilkan by variant", v -> {
            currentApiPickerName = value(picker);
            pickingByVariant = !pickingByVariant;
            renderApiPicking(nota, response);
        }));
        LinkedHashMap<String, JSONArray> groups = new LinkedHashMap<>();
        boolean hasShortage = false;
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.optJSONObject(i);
            if (row == null) continue;
            String key = pickingByVariant ? first(row, "product_code", "kode_barang") : first(row, "nota", "no_order", "wms_task_id", "id_picking");
            if (!groups.containsKey(key)) groups.put(key, new JSONArray());
            groups.get(key).put(row);
            if (row.optInt("picked_quantity") < row.optInt("required_quantity")) hasShortage = true;
        }
        for (JSONArray items : groups.values()) {
            JSONObject firstRow = items.optJSONObject(0);
            int required = 0, pickedQty = 0;
            for (int i = 0; i < items.length(); i++) { required += items.optJSONObject(i).optInt("required_quantity"); pickedQty += items.optJSONObject(i).optInt("picked_quantity"); }
            list.addView(sectionTitle(pickingByVariant ? first(firstRow, "product_name", "product_code") : first(firstRow, "nota", "no_order")));
            list.addView(body("Order " + required + " PCS · Picked " + pickedQty + " PCS · Kurang " + Math.max(0, required-pickedQty)));
            for (int i = 0; i < items.length(); i++) list.addView(apiPickingRow(items.optJSONObject(i)));
        }
        if (!shipmentPickingEditable) list.addView(body("Draf ditahan untuk revisi atau sudah masuk proses berikutnya. Jika REVISION_REQUIRED, selesaikan Revisi Faktur ERP lalu muat ulang draf."));
        if (shipmentPickingEditable && hasShortage) list.addView(secondaryButton("Ajukan Revisi Faktur Kurang Picked", v -> requestShipmentRevision()));
        if (shipmentPickingEditable && !hasShortage) list.addView(primaryButton("Selesai Picking → Checker", v -> {
            currentApiPickerName = value(picker);
            finalizeApiPicking(response);
        }));
        content.addView(list);
        content.addView(secondaryButton("Muat Draf Lain", v -> showApiPicking()));
    }

    private LinearLayout apiPickingRow(JSONObject row) {
        LinearLayout wrapper = vertical();
        wrapper.setPadding(0, dp(12), 0, dp(12));
        wrapper.setBackground(separatorBackground());

        String code = first(row, "product_code", "kode_barang", "KodeBarang");
        String name = first(row, "product_name", "nama_barang", "NamaBarang");
        String rack = first(row, "rak_tetap", "kode_rak", "KodeRak");
        String status = first(row, "status_draft", "status");
        int requiredQty = parseOptionalInt(row, 0, "required_quantity", "qty_pcs");
        int pickedQty = parseOptionalInt(row, 0, "picked_quantity");
        int availableQty = parseOptionalInt(row, -1, "available_quantity", "available_qty", "stock_qty", "qty_available");
        // `available_quantity` is the total ready stock for this product across
        // eligible picking racks.  `rack_available_quantity` is the amount on
        // the rack currently suggested by the API.  A product may legitimately
        // need to be picked from more than one rack.
        int rackAvailableQty = parseOptionalInt(row, -1,
                "rack_available_quantity", "rack_available_qty",
                "rak_available_quantity", "rak_available_qty");
        int remainingQty = PickingAllocation.remainingQuantity(requiredQty, pickedQty);
        int suggestedScanQty = PickingAllocation.suggestedRackScanQuantity(remainingQty, rackAvailableQty);
        boolean scanned = "PICKED".equalsIgnoreCase(status)
                || (requiredQty > 0 && remainingQty == 0);
        boolean statusReady = "READY".equalsIgnoreCase(status)
                || "DRAFT".equalsIgnoreCase(status)
                || "PARTIAL".equalsIgnoreCase(status)
                || status.isEmpty();
        boolean globalStockEnough = PickingAllocation.totalStockCoversRemaining(remainingQty, availableQty);
        boolean rackHasStock = rackAvailableQty < 0 || rackAvailableQty > 0;
        // Older drafts may still say LOW because their original rack could not
        // hold the whole line.  It is safe to scan in that case only when the
        // API says the aggregate ready stock covers the remaining quantity and
        // the currently suggested rack has stock.  The server remains the final
        // authority for rack, batch, and physical availability.
        boolean splitRackReady = "LOW".equalsIgnoreCase(status)
                && availableQty >= remainingQty
                && rackAvailableQty > 0;
        boolean ready = !scanned
                && remainingQty > 0
                && rackHasStock
                && shipmentPickingEditable
                && (statusReady || splitRackReady || "LOW".equalsIgnoreCase(status));
        String statusLabel = scanned
                ? "Sudah Scan"
                : (ready
                ? (suggestedScanQty < remainingQty ? "Siap Scan Bertahap" : "Siap Scan")
                : (("LOW".equalsIgnoreCase(status) || (availableQty >= 0 && availableQty < remainingQty))
                ? "Stok Kurang"
                : "Belum Siap Picking"));
        int statusColor = scanned || ready ? GREEN : AMBER;

        wrapper.addView(title(fallback(code, "-"), 15));
        wrapper.addView(small("Alokasi nota: " + first(row, "nota", "no_order")));
        wrapper.addView(small(fallback(name, "-")));
        String stockLine = "Rak picking: " + fallback(rack, "-")
                + " | Sisa pick: " + (remainingQty > 0 ? remainingQty : requiredQty) + " PCS";
        if (rackAvailableQty >= 0) {
            stockLine += " | Stok rak ini: " + rackAvailableQty + " PCS";
        }
        if (availableQty >= 0) {
            stockLine += " | Saldo rak Tetap/Lorong: " + availableQty + " PCS";
        }
        wrapper.addView(small(stockLine));
        if (row.has("stok_ready")) wrapper.addView(small("Stok Ready Gudang: " + first(row, "stok_ready") + " PCS"));
        if (ready && suggestedScanQty > 0 && suggestedScanQty < remainingQty) {
            wrapper.addView(small("Rak ini akan dipindai " + suggestedScanQty
                    + " PCS dahulu; lanjutkan dari rak berikutnya setelah memuat ulang draft."));
        }
        String readyNote = first(row, "ready_picking_note", "wms_ready_status");
        if (!readyNote.isEmpty()) {
            wrapper.addView(small(readyNote));
        }
        wrapper.addView(small("Batch: " + fallback(first(row, "batch", "batch_number"), "-")));
        wrapper.addView(badge(statusLabel, statusColor));
        if (!scanned && ready && !rack.isEmpty()) {
            wrapper.addView(tinyButton("Scan QR", v -> {
                selectedApiPickingRow = row;
                startScanner("API_PICKING_RACK");
            }));
        }
        return wrapper;
    }

    private void processApiPickingScan(String scanPayload) {
        if (selectedApiPickingRow == null) {
            toast("Item picking belum dipilih");
            showApiPicking();
            return;
        }

        JSONObject row = selectedApiPickingRow;
        String expectedRack = first(row, "rak_tetap", "kode_rak", "KodeRak");
        String code = first(row, "product_code", "kode_barang", "KodeBarang");
        String name = first(row, "product_name", "nama_barang", "NamaBarang");
        String rawScanPayload = safe(scanPayload).trim();
        String[] scanParts = rawScanPayload.split(";", -1);
        String semicolonPalletCode = extractSemicolonPart(rawScanPayload, 2);
        boolean semicolonPalletScan = scanParts.length >= 9 && !semicolonPalletCode.isEmpty();
        String extractedPalletCode = QrUtils.extractPalletCode(rawScanPayload);
        boolean compactPalletScan = (rawScanPayload.toUpperCase(Locale.US).startsWith("BUDIMAS-WMS|PALLET|")
                && extractedPalletCode != null
                && !rawScanPayload.equalsIgnoreCase(extractedPalletCode))
                || (extractedPalletCode != null && extractedPalletCode.matches("PLT-\\d{6,}")
                && rawScanPayload.equalsIgnoreCase(extractedPalletCode));
        boolean palletScan = semicolonPalletScan || compactPalletScan;
        String palletCode = semicolonPalletScan
                ? semicolonPalletCode
                : (compactPalletScan ? extractedPalletCode.toUpperCase(Locale.US) : "");
        String scannedCode = compactPalletScan ? "" : extractSemicolonPart(rawScanPayload, 0);
        String scannedRack = semicolonPalletScan
                ? extractSemicolonPart(rawScanPayload, 1)
                : (compactPalletScan ? "" : QrUtils.extractLocationCode(rawScanPayload));
        if (scannedRack.isEmpty() && !compactPalletScan) {
            scannedRack = extractSemicolonPart(rawScanPayload, 1);
        }
        int scannedBatchIndex = palletScan ? 7 : 6;
        String scannedBatch = scanParts.length > scannedBatchIndex
                ? extractSemicolonPart(rawScanPayload, scannedBatchIndex)
                : "";
        String expectedBatch = first(row, "batch", "batch_number");
        if ("-".equals(expectedBatch)) {
            expectedBatch = "";
        }
        if ("-".equals(scannedBatch)) {
            scannedBatch = "";
        }
        int requiredQty = parseOptionalInt(row, 0, "required_quantity", "qty_pcs");
        int pickedQty = parseOptionalInt(row, 0, "picked_quantity");
        int remainingQty = PickingAllocation.remainingQuantity(requiredQty, pickedQty);
        int availableQty = parseOptionalInt(row, -1, "available_quantity", "available_qty", "stock_qty", "qty_available");
        int rackAvailableQty = parseOptionalInt(row, -1,
                "rack_available_quantity", "rack_available_qty",
                "rak_available_quantity", "rak_available_qty");
        if (code.isEmpty()) {
            toast("Barang picking belum jelas. Muat ulang draft picking.");
            loadApiPicking(currentApiPickingNota);
            return;
        }
        if (expectedRack.isEmpty()) {
            toast("Rak picking belum ada di draft. Cek master rak/stok di WMS.");
            loadApiPicking(currentApiPickingNota);
            return;
        }
        if (!scannedCode.isEmpty() && !code.equalsIgnoreCase(scannedCode)) {
            toast("Barang scan " + scannedCode + " berbeda dari item " + code);
            loadApiPicking(currentApiPickingNota);
            return;
        }
        if (!expectedBatch.isEmpty() && !scannedBatch.isEmpty() && !expectedBatch.equalsIgnoreCase(scannedBatch)) {
            toast("Batch scan " + scannedBatch + " berbeda dari batch draft " + expectedBatch);
            loadApiPicking(currentApiPickingNota);
            return;
        }
        if (!palletScan && scannedRack.isEmpty()) {
            toast("QR rak/barang tidak memuat kode rak.");
            loadApiPicking(currentApiPickingNota);
            return;
        }
        if (remainingQty <= 0) {
            toast("Item ini sudah selesai dipicking. Muat ulang draft untuk melanjutkan.");
            loadApiPicking(currentApiPickingNota);
            return;
        }
        // Partial picks are allowed; shortage must be revised before finalization.
        if (rackAvailableQty == 0) {
            toast("Stok pada rak " + expectedRack + " sudah tidak tersedia. Muat ulang draft untuk memilih rak berikutnya.");
            loadApiPicking(currentApiPickingNota);
            return;
        }
        // Pick only what is physically available from this suggested rack.  A
        // later draft refresh will ask the picker to scan the next rack for the
        // still-unpicked balance.  Do not use the aggregate stock as the scan
        // quantity: that would make a valid split-rack pick look insufficient.
        int qtyToPick = PickingAllocation.suggestedRackScanQuantity(remainingQty, rackAvailableQty);
        if (qtyToPick <= 0) {
            toast("Qty picking harus lebih dari 0.");
            loadApiPicking(currentApiPickingNota);
            return;
        }
        if (!palletScan && !expectedRack.isEmpty() && !expectedRack.equalsIgnoreCase(scannedRack)) {
            toast("Rak scan " + scannedRack + " berbeda dari rak draft " + expectedRack);
            loadApiPicking(currentApiPickingNota);
            return;
        }

        // A pallet label can still print its previous rack after a transfer.
        // The server receives the pallet identity/raw scan and validates its live
        // location; submit the draft rack as the requested picking location.
        showLoadingScreen("Picking", "Submit scan rak " + (palletScan ? expectedRack : fallback(scannedRack, expectedRack)) + "...");
        final int finalQtyToPick = qtyToPick;
        final String finalScannedRack = palletScan ? expectedRack : fallback(scannedRack, expectedRack);
        final boolean finalPalletScan = palletScan;
        final String finalPalletCode = palletCode;
        final String finalScanPayload = rawScanPayload;
        executor.execute(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("wms_task_detail_id", row.opt("wms_task_detail_id"));
                payload.put("nota", currentApiPickingNota);
                payload.put("shipment_reference", currentApiPickingNota);
                payload.put("reference_no", fallback(first(row, "nota"), currentApiPickingNota));
                payload.put("kode_barang", code);
                payload.put("kode_rak", finalScannedRack);
                payload.put("batch", first(row, "batch", "batch_number"));
                putIfNotEmpty(payload, "expired_date", first(row, "expired_date", "expired", "expiry_date"));
                payload.put("qty_pcs", finalQtyToPick);
                if (finalPalletScan) {
                    putIfNotEmpty(payload, "pallet_code", finalPalletCode);
                    putIfNotEmpty(payload, "scan_payload", finalScanPayload);
                }
                putIfNotEmpty(payload, "user_id", currentUserId());
                JSONObject result = apiClient.scanWmsPickingRack(payload);
                String resultStatus = first(result, "status");
                if (!"OK".equalsIgnoreCase(resultStatus)) {
                    String rejectionMessage = fallback(
                            first(result, "msg", "message"),
                            "Server WMS belum mengonfirmasi scan picking. Muat ulang draft sebelum mencoba kembali."
                    );
                    uiHandler.post(() -> showApiError(
                            "Picking",
                            "Scan picking belum berhasil.",
                            new BudimasApiClient.ApiException(409, rejectionMessage),
                            () -> loadApiPicking(currentApiPickingNota)
                    ));
                    return;
                }
                uiHandler.post(() -> {
                    recordLocalTransaction("Picking", fallback(first(row, "nota"), currentApiPickingNota), "", code, name, finalScannedRack, "Area Loading", finalQtyToPick, "PCS", "Barang diambil dari Rak Tetap dan siap loading.");
                    showApiPickingScanned(code, result, finalQtyToPick,
                            Math.max(remainingQty - finalQtyToPick, 0));
                });
            } catch (Exception e) {
                uiHandler.post(() -> showApiError("Picking", "Scan picking belum berhasil.", e, () -> loadApiPicking(currentApiPickingNota)));
            }
        });
    }

    private void showApiPickingScanned(String code, JSONObject result, int scannedQty, int remainingQty) {
        currentScreen = "api_picking_done";
        setShell("Scan Rak Berhasil", fallback(code, "Picking"), "putaway", true);
        LinearLayout success = card();
        success.setBackground(successBackground());
        success.addView(title("Picking Berhasil", 20));
        success.addView(body(fallback(first(result, "msg", "message"), fallback(code, "Item") + " selesai discan.")));
        if (remainingQty > 0) {
            success.addView(small(scannedQty + " PCS telah dipicking. Masih ada "
                    + remainingQty + " PCS yang perlu diambil dari rak berikutnya."));
        }
        content.addView(success);
        content.addView(primaryButton(remainingQty > 0 ? "Lanjutkan Picking" : "Muat Ulang Draft",
                v -> loadApiPicking(currentApiPickingNota)));
        content.addView(secondaryButton("Draf Kiriman Lain", v -> showApiPicking()));
    }

    private void requestShipmentRevision() {
        EditText note = input("", "Catatan barang / nota kurang picked", InputType.TYPE_CLASS_TEXT);
        new android.app.AlertDialog.Builder(this).setTitle("Ajukan Revisi Faktur")
                .setMessage("Nota kurang picked akan ditahan sampai revisi diselesaikan di ERP.")
                .setView(note).setNegativeButton("Batal", null).setPositiveButton("Ajukan", (dialog, which) -> {
                    if (value(note).isEmpty()) { toast("Catatan wajib diisi"); return; }
                    final String draft = currentApiPickingNota;
                    showLoadingScreen("Revisi Picking", "Mengirim kekurangan picked...");
                    executor.execute(() -> {
                        try {
                            JSONObject payload = new JSONObject(); payload.put("shipment_reference", draft); payload.put("note", value(note));
                            JSONObject result = apiClient.requestPickingRevision(payload);
                            uiHandler.post(() -> { toast(result.optString("message")); loadApiPicking(draft); });
                        } catch (Exception e) { uiHandler.post(() -> showApiError("Revisi Picking", "Pengajuan belum berhasil.", e, () -> loadApiPicking(draft))); }
                    });
                }).show();
    }

    private void finalizeApiPicking(JSONObject response) {
        JSONObject document = response == null ? null : response.optJSONObject("document");
        if (document == null && response != null) {
            document = response.optJSONObject("task");
        }
        JSONArray taskIds = document == null ? null : document.optJSONArray("task_ids");
        int taskId = parseOptionalInt(document, 0, "id_picking", "id");
        if (taskId <= 0 && taskIds != null && taskIds.length() > 0) {
            try {
                taskId = taskIds.optInt(0, 0);
            } catch (Exception ignored) {
            }
        }
        if (taskId <= 0) {
            toast("ID draf belum tersedia. Muat ulang QR draf kiriman.");
            return;
        }
        final int finalTaskId = taskId;
        final JSONArray finalTaskIds = taskIds;
        showLoadingScreen("Finalisasi Picking", "Menyiapkan dokumen untuk Loading...");
        executor.execute(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("task_id", finalTaskId);
                if (finalTaskIds != null && finalTaskIds.length() > 0) {
                    payload.put("task_ids", finalTaskIds);
                }
                payload.put("nota", currentApiPickingNota);
                payload.put("picker_name", currentApiPickerName);
                putIfNotEmpty(payload, "user_id", currentUserId());
                JSONObject result = apiClient.finalizeWmsPicking(payload);
                uiHandler.post(() -> {
                    boolean needsChecker = "CHECKER".equalsIgnoreCase(first(result, "next_step"));
                    setShell(needsChecker ? "Menunggu Checker" : "Picking Final",
                            needsChecker ? "Semua barang wajib dicek ulang" : "Siap diproses ke Loading",
                            "putaway", true);
                    LinearLayout success = card();
                    success.setBackground(successBackground());
                    success.addView(title(needsChecker ? "Picking Masuk Checker" : "Picking Siap Loading", 20));
                    success.addView(body(fallback(first(result, "msg", "message"), "Picking sudah difinalisasi.")));
                    content.addView(success);
                    content.addView(primaryButton(needsChecker ? "Buka Pemeriksaan Checker" : "Buka Loading",
                            v -> {
                                if (needsChecker) {
                                    showCheckerList();
                                } else {
                                    loadLoadingList();
                                }
                            }));
                    content.addView(secondaryButton("Kembali ke Picking", v -> showApiPicking()));
                });
            } catch (Exception e) {
                uiHandler.post(() -> showApiError("Finalisasi Picking", "Pastikan semua item sudah discan sebelum finalisasi.", e, () -> loadApiPicking(currentApiPickingNota)));
            }
        });
    }

    private void showCheckerList() {
        renderCheckerList(null, true, "");
        executor.execute(() -> {
            try {
                JSONObject response = apiClient.getWmsCheckerPending();
                JSONArray rows = normalizeRows(response);
                uiHandler.post(() -> renderCheckerList(rows, false, ""));
            } catch (Exception e) {
                uiHandler.post(() -> renderCheckerList(new JSONArray(), false,
                        operatorErrorMessage(e, "Daftar pemeriksaan checker belum bisa dimuat.")));
            }
        });
    }

    private void renderCheckerList(JSONArray rows, boolean loading, String error) {
        currentScreen = "checker";
        setShell("Checker Barang", "Cek semua barang per nota pada draf kiriman", "putaway", true);

        LinearLayout info = card();
        info.addView(sectionTitle("Antrian Checker"));
        info.addView(body("Semua barang termasuk karton wajib dihitung dan diperiksa sebelum loading. Nota yang kurang picked harus selesai revisi."));
        content.addView(info);

        LinearLayout list = card();
        if (loading) {
            list.addView(body("Memuat antrian checker..."));
        } else if (!error.isEmpty()) {
            TextView err = body(error);
            err.setTextColor(AMBER);
            list.addView(err);
            list.addView(primaryButton("Coba Lagi", v -> showCheckerList()));
        } else if (rows == null || rows.length() == 0) {
            list.addView(body("Tidak ada picking yang menunggu pemeriksaan."));
            list.addView(secondaryButton("Buka Loading", v -> loadLoadingList()));
        } else {
            list.addView(body(rows.length() + " draft menunggu pemeriksaan checker."));
            for (int i = 0; i < rows.length(); i++) {
                JSONObject task = rows.optJSONObject(i);
                if (task != null) {
                    list.addView(checkerTaskRow(task));
                }
            }
        }
        content.addView(list);
    }

    private LinearLayout checkerTaskRow(JSONObject task) {
        LinearLayout row = vertical();
        row.setPadding(0, dp(12), 0, dp(12));
        row.setBackground(separatorBackground());
        String nota = fallback(first(task, "nota", "reference_no"), "Draft Picking");
        row.addView(title(first(task, "shipment_reference") + " · " + nota, 17));
        row.addView(small("Picker: " + fallback(first(task, "picker_name"), "-")
                + " | Item: " + fallback(first(task, "small_item"), "0")));
        row.addView(small("Qty perlu dicek: " + fallback(first(task, "qty_perlu_cek"), "0") + " PCS"));
        row.addView(compactAction("Periksa Barang", v -> showCheckerConfirm(task)));
        return row;
    }

    private void showCheckerConfirm(JSONObject task) {
        currentScreen = "checker_confirm";
        String nota = fallback(first(task, "nota", "reference_no"), "Draft Picking");
        setShell("Pemeriksaan Checker", nota, "putaway", true);

        LinearLayout summary = card();
        summary.addView(sectionTitle("Hasil Picking"));
        summary.addView(keyValue("Picker", fallback(first(task, "picker_name"), "-")));
        summary.addView(keyValue("Status", fallback(first(task, "status"), "CHECKER_PENDING")));
        content.addView(summary);

        JSONArray details = task.optJSONArray("details");
        java.util.ArrayList<JSONObject> detailRows = new java.util.ArrayList<>();
        java.util.ArrayList<EditText> quantityInputs = new java.util.ArrayList<>();
        java.util.ArrayList<android.widget.CheckBox> damagedInputs = new java.util.ArrayList<>();
        java.util.ArrayList<EditText> checkerNotes = new java.util.ArrayList<>();
        LinearLayout items = card();
        items.addView(sectionTitle("Cek Jumlah Fisik"));
        if (details == null || details.length() == 0) {
            items.addView(body("Detail item belum tersedia."));
        } else {
            for (int i = 0; i < details.length(); i++) {
                JSONObject detail = details.optJSONObject(i);
                if (detail == null) {
                    continue;
                }
                int picked = parseOptionalInt(detail, 0, "picked_quantity", "qty_picked", "qty");
                items.addView(title(fallback(first(detail, "nama_barang", "product_name"),
                        fallback(first(detail, "kode_barang", "product_code"), "Produk")), 15));
                items.addView(small("Kode: " + fallback(first(detail, "kode_barang", "product_code"), "-")
                        + " | Tipe: " + fallback(first(detail, "handling_class"), "SMALL")
                        + " | Picked: " + configuredQtyText(picked, detail)));
                boolean carton = false; // Every shipment line requires an actual checker count.
                EditText qty = input(carton ? String.valueOf(picked) : "", "Qty hasil hitung aktual (PCS)", InputType.TYPE_CLASS_NUMBER);
                qty.setEnabled(!carton);
                items.addView(qty);
                android.widget.CheckBox damaged = new android.widget.CheckBox(this);
                damaged.setText("Kondisi rusak (NG)");
                damaged.setEnabled(!carton);
                items.addView(damaged);
                EditText note = input("", "Catatan wajib jika selisih / rusak", InputType.TYPE_CLASS_TEXT);
                note.setEnabled(!carton);
                items.addView(note);
                damagedInputs.add(damaged);
                checkerNotes.add(note);
                items.addView(sectionDivider());
                detailRows.add(detail);
                quantityInputs.add(qty);
            }
        }
        content.addView(items);

        LinearLayout form = card();
        form.addView(sectionTitle("Konfirmasi Checker"));
        EditText checker = input("", "Nama checker", InputType.TYPE_CLASS_TEXT);
        EditText dock = input(first(task, "loading_dock", "dock_code", "DockRencana"), "Kode loading dock / area kendaraan", InputType.TYPE_CLASS_TEXT);
        form.addView(checker);
        form.addView(dock);
        form.addView(primaryButton("Simpan Checker OK / NG", v -> submitChecker(task, value(checker), value(dock), detailRows, quantityInputs, damagedInputs, checkerNotes)));
        form.addView(secondaryButton("Kembali", v -> showCheckerList()));
        content.addView(form);
    }

    private void submitChecker(JSONObject task, String checkerName, String dockCode,
                               java.util.ArrayList<JSONObject> details,
                               java.util.ArrayList<EditText> quantityInputs,
                               java.util.ArrayList<android.widget.CheckBox> damagedInputs,
                               java.util.ArrayList<EditText> checkerNotes) {
        if (checkerName.isEmpty() || dockCode.isEmpty()) {
            toast("Nama checker dan loading dock wajib diisi.");
            return;
        }
        JSONArray checkedItems = new JSONArray();
        for (int i = 0; i < details.size(); i++) {
            JSONObject detail = details.get(i);
            String rawQuantity = value(quantityInputs.get(i));
            int quantity = rawQuantity.matches("\\d+") ? parsePositiveInt(rawQuantity, -1) : -1;
            if (quantity < 0) {
                toast("Qty hasil checker harus diisi dengan benar.");
                return;
            }
            JSONObject checked = new JSONObject();
            try {
                checked.put("detail_id", parseOptionalInt(detail, 0, "id", "detail_id", "wms_task_detail_id"));
                checked.put("checked_quantity", quantity);
                checked.put("condition", damagedInputs.get(i).isChecked() ? "DAMAGED" : "GOOD");
                checked.put("note", value(checkerNotes.get(i)));
                checkedItems.put(checked);
            } catch (Exception ignored) {
                toast("Detail checker tidak valid.");
                return;
            }
        }

        showLoadingScreen("Checker", "Menyimpan hasil pemeriksaan...");
        executor.execute(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("task_id", parseOptionalInt(task, 0, "id", "id_picking", "task_id"));
                payload.put("checker_name", checkerName);
                payload.put("dock_code", dockCode);
                payload.put("items", checkedItems);
                putIfNotEmpty(payload, "user_id", currentUserId());
                JSONObject result = apiClient.confirmWmsChecker(payload);
                uiHandler.post(() -> {
                    boolean held = "CHECKER_HOLD".equals(first(result, "next_step"));
                    setShell(held ? "Checker NG" : "Checker OK", held ? "Ditahan, perbaiki dan cek ulang" : "Barang siap ke loading dock", "putaway", true);
                    LinearLayout success = card();
                    success.setBackground(successBackground());
                    success.addView(title(held ? "NG — perlu perbaikan" : "Pemeriksaan OK", 20));
                    success.addView(body(fallback(first(result, "message", "msg"), "Barang sudah siap dimuat.")));
                    success.addView(keyValue("Checker", checkerName));
                    success.addView(keyValue("Loading Dock", dockCode));
                    content.addView(success);
                    if (!held) content.addView(primaryButton("Buka Loading", v -> loadLoadingList()));
                    content.addView(secondaryButton("Antrian Checker", v -> showCheckerList()));
                });
            } catch (Exception e) {
                uiHandler.post(() -> showApiError("Checker", "Hasil pemeriksaan belum berhasil disimpan.", e,
                        () -> showCheckerConfirm(task)));
            }
        });
    }

    private void showPutawayScanPallet() {
        currentScreen = "putaway_pallet";
        selectedPallet = null;
        setShell("Putaway", "Scan QR barang/rak dari API", "putaway", true);
        content.addView(stepper(1));

        LinearLayout scanner = card();
        scanner.addView(sectionTitle("Scan QR Barang / Rak"));
        scanner.addView(body("Arahkan kamera ke QR label gudang. Data tidak dicari dari pallet lokal HP."));
        scanner.addView(primaryButton("Scan QR Barang / Rak", v -> startScanner("PALLET")));

        EditText manual = input("", "Input payload QR barang/rak", InputType.TYPE_CLASS_TEXT);
        scanner.addView(manual);
        scanner.addView(secondaryButton("Proses QR", v -> {
            Pallet pallet = palletFromPickingQr(value(manual));
            if (pallet == null) {
                toast("QR tidak berisi kode barang dan rak.");
                return;
            }
            showScanLocation(pallet);
        }));
        content.addView(scanner);
    }

    private void showScanLocation(Pallet pallet) {
        renderScanLocation(pallet, null, true, "");
        executor.execute(() -> {
            try {
                JSONObject response = apiClient.getWmsRacks("", "", "");
                JSONArray rows = normalizeRows(response);
                uiHandler.post(() -> renderScanLocation(pallet, rows, false, ""));
            } catch (Exception e) {
                uiHandler.post(() -> renderScanLocation(pallet, new JSONArray(), false, operatorErrorMessage(e, "Lokasi rak belum bisa dimuat dari API.")));
            }
        });
    }

    private void renderScanLocation(Pallet pallet, JSONArray racks, boolean loading, String error) {
        currentScreen = "putaway_location";
        selectedPallet = pallet;
        setShell("Scan Lokasi Rak", "Scan QR lokasi atau input kode rak", "putaway", true);
        content.addView(stepper(2));

        LinearLayout palletCard = card();
        palletCard.addView(sectionTitle("Pallet Dipilih"));
        palletCard.addView(keyValue("Kode Pallet", pallet.code));
        palletCard.addView(keyValue("SKU / Barang", safe(pallet.skuName)));
        palletCard.addView(keyValue("Isi", pallet.cartonCount + " Karton"));
        palletCard.addView(keyValue("Lot / Exp", safe(pallet.lotBatch) + " / " + safe(pallet.expDate)));
        content.addView(palletCard);

        LinearLayout locationCard = card();
        locationCard.addView(sectionTitle("Lokasi Rak"));
        locationCard.addView(body("Lokasi diambil dari master rak database WMS."));
        locationCard.addView(primaryButton("Scan QR Lokasi Rak", v -> startScanner("LOCATION")));

        EditText location = input("", "Input kode rak database", InputType.TYPE_CLASS_TEXT);
        locationCard.addView(location);
        locationCard.addView(secondaryButton("Cek & Gunakan Lokasi", v -> {
            String code = QrUtils.extractLocationCode(value(location));
            if (code.isEmpty()) {
                toast("Lokasi rak wajib diisi");
                return;
            }
            verifyRackAndConfirm(pallet, code);
        }));
        content.addView(locationCard);

        LinearLayout list = card();
        list.addView(sectionTitle("Rak Database"));
        if (loading) {
            list.addView(body("Memuat lokasi rak dari database..."));
        } else if (!error.isEmpty()) {
            TextView err = body(error);
            err.setTextColor(AMBER);
            list.addView(err);
            list.addView(secondaryButton("Coba Muat Lagi", v -> showScanLocation(pallet)));
        } else if (racks == null || racks.length() == 0) {
            list.addView(body("Lokasi rak belum tersedia dari database."));
        } else {
            int shown = Math.min(12, racks.length());
            list.addView(body("Pilih salah satu rak aktif dari database. Menampilkan " + shown + " dari " + racks.length() + " rak."));
            for (int i = 0; i < shown; i++) {
                JSONObject row = racks.optJSONObject(i);
                if (row != null) {
                    String rackCode = first(row, "kode_rak", "KodeRak");
                    list.addView(rackLocationRow(row, v -> {
                        String problem = rackValidationProblem(row, rackCode, "");
                        if (!problem.isEmpty()) {
                            toast(problem);
                            return;
                        }
                        showPutawayConfirm(pallet, rackCode);
                    }));
                }
            }
            list.addView(secondaryButton("Lihat Semua Lokasi Rak", v -> {
                rackQuery = "";
                rackTypeFilter = "";
                showRackLocations();
            }));
        }
        content.addView(list);
    }

    private void verifyRackAndConfirm(Pallet pallet, String location) {
        showLoadingScreen("Cek Lokasi Rak", "Mengecek " + location + " ke database WMS...");
        executor.execute(() -> {
            try {
                JSONObject response = apiClient.getWmsRacks(location, "", "");
                JSONArray rows = normalizeRows(response);
                JSONObject rack = findRackByCode(rows, location);
                String problem = rackValidationProblem(rack, location, "");
                if (problem.isEmpty()) {
                    uiHandler.post(() -> showPutawayConfirm(pallet, location));
                } else {
                    uiHandler.post(() -> {
                        toast(problem);
                        renderScanLocation(pallet, rows, false, "");
                    });
                }
            } catch (Exception e) {
                uiHandler.post(() -> showApiError("Cek Lokasi Rak", "Lokasi rak belum bisa diverifikasi.", e, () -> showScanLocation(pallet)));
            }
        });
    }

    private void showRackLocations() {
        loadRackLocations();
    }

    private void loadRackLocations() {
        renderRackLocations(null, true, "");
        executor.execute(() -> {
            try {
                JSONObject response = apiClient.getWmsRacks(rackQuery, rackTypeFilter, "");
                JSONArray rows = normalizeRows(response);
                uiHandler.post(() -> renderRackLocations(rows, false, ""));
            } catch (Exception e) {
                uiHandler.post(() -> renderRackLocations(new JSONArray(), false, operatorErrorMessage(e, "Daftar lokasi rak belum bisa dimuat.")));
            }
        });
    }

    private void renderRackLocations(JSONArray rows, boolean loading, String error) {
        currentScreen = "rack_locations";
        setShell("Lokasi Rak", "Data master rak dari database WMS", "warehouse", true);

        LinearLayout search = card();
        search.addView(dropdownHeader(filterSummary("Filter Rak", rackQuery, rackTypeFilter), rackFilterOpen, v -> {
            rackFilterOpen = !rackFilterOpen;
            renderRackLocations(rows, loading, error);
        }));
        if (rackFilterOpen) {
            EditText query = input(rackQuery, "Cari kode / tipe / status rak", InputType.TYPE_CLASS_TEXT);
            search.addView(query);

            LinearLayout filters = horizontal();
            filters.setPadding(0, dp(6), 0, dp(10));
            filters.addView(filterChip("Semua", rackTypeFilter.isEmpty(), v -> {
                rackQuery = value(query);
                rackTypeFilter = "";
                loadRackLocations();
            }));
            filters.addView(filterChip("Tetap", "Tetap".equalsIgnoreCase(rackTypeFilter), v -> {
                rackQuery = value(query);
                rackTypeFilter = "Tetap";
                loadRackLocations();
            }));
            filters.addView(filterChip("Titipan", "Titipan".equalsIgnoreCase(rackTypeFilter), v -> {
                rackQuery = value(query);
                rackTypeFilter = "Titipan";
                loadRackLocations();
            }));
            filters.addView(filterChip("Lorong", "Lorong".equalsIgnoreCase(rackTypeFilter), v -> {
                rackQuery = value(query);
                rackTypeFilter = "Lorong";
                loadRackLocations();
            }));
            search.addView(filters);
            search.addView(secondaryButton("Cari Rak", v -> {
                rackQuery = value(query);
                loadRackLocations();
            }));
        } else {
            search.addView(small(activeFilterText(rackQuery, rackTypeFilter, "Tidak ada filter aktif. Tap untuk cari rak atau pilih tipe rak.")));
        }
        content.addView(search);

        LinearLayout list = card();
        list.addView(sectionTitle("Daftar Lokasi Rak"));
        if (loading) {
            list.addView(body("Memuat master rak dari database..."));
        } else if (!error.isEmpty()) {
            TextView err = body(error);
            err.setTextColor(AMBER);
            list.addView(err);
            list.addView(primaryButton("Coba Lagi", v -> loadRackLocations()));
        } else if (rows == null || rows.length() == 0) {
            list.addView(body("Data rak tidak ditemukan."));
        } else {
            int limit = Math.min(rows.length(), 80);
            list.addView(body("Menampilkan " + limit + " dari " + rows.length() + " lokasi rak aktif."));
            for (int i = 0; i < limit; i++) {
                JSONObject row = rows.optJSONObject(i);
                if (row != null) {
                    list.addView(rackLocationRow(row, null));
                }
            }
        }
        content.addView(list);
        content.addView(secondaryButton("Muat Inventory API", v -> showInventory()));
    }

    private void showInventory() {
        loadInventory();
    }

    private void loadInventory() {
        renderInventory(null, true, "");
        executor.execute(() -> {
            try {
                JSONObject response = apiClient.getWmsInventory(inventoryQuery, inventoryStatusFilter);
                JSONArray rows = normalizeRows(response);
                uiHandler.post(() -> renderInventory(rows, false, ""));
            } catch (Exception e) {
                uiHandler.post(() -> renderInventory(new JSONArray(), false, operatorErrorMessage(e, "Inventory belum bisa dimuat.")));
            }
        });
    }

    private void loadInventoryBarcode(String barcode) {
        String scannedRack = QrUtils.extractLocationCode(barcode);
        String firstPart = extractSemicolonPart(barcode, 0);
        String cleanPayload = safe(barcode).trim();
        if (firstPart.isEmpty()) {
            firstPart = cleanPayload;
        }
        if (!scannedRack.isEmpty()
                && (isRackCode(firstPart) || firstPart.equalsIgnoreCase(scannedRack) || cleanPayload.toUpperCase(Locale.US).contains("LOCATION"))) {
            inventoryQuery = scannedRack;
            inventoryStatusFilter = "";
            loadInventory();
            return;
        }

        String productCode = firstPart.trim();
        if (isRackCode(productCode)) {
            inventoryQuery = productCode;
            inventoryStatusFilter = "";
            loadInventory();
            return;
        }
        if (productCode.isEmpty()) {
            toast("Barcode produk/rak tidak terbaca");
            showInventory();
            return;
        }
        inventoryQuery = productCode;
        renderInventory(null, true, "");
        executor.execute(() -> {
            try {
                JSONObject response = apiClient.getWmsInventoryBarcode(productCode);
                JSONArray rows = normalizeRows(response);
                uiHandler.post(() -> renderInventory(rows, false, ""));
            } catch (Exception e) {
                uiHandler.post(() -> renderInventory(new JSONArray(), false, operatorErrorMessage(e, "Barcode produk tidak ditemukan.")));
            }
        });
    }

    private void renderInventory(JSONArray rows, boolean loading, String error) {
        currentScreen = "inventory";
        setShell("Inventory", "Scan barang untuk lokasi, scan rak untuk isi rak", "warehouse", true);

        LinearLayout search = card();
        search.addView(secondaryButton("Scan Barang / Rak", v -> startScanner("INVENTORY_BARCODE")));
        search.addView(dropdownHeader(filterSummary("Filter Inventory", inventoryQuery, inventoryStatusFilter), inventoryFilterOpen, v -> {
            inventoryFilterOpen = !inventoryFilterOpen;
            renderInventory(rows, loading, error);
        }));
        if (inventoryFilterOpen) {
            EditText query = input(inventoryQuery, "Cari kode barang, nama barang, atau kode rak", InputType.TYPE_CLASS_TEXT);
            search.addView(query);
            LinearLayout filters = horizontal();
            filters.setPadding(0, dp(6), 0, dp(10));
            filters.addView(filterChip("Semua", inventoryStatusFilter.isEmpty(), v -> {
                inventoryQuery = value(query);
                inventoryStatusFilter = "";
                loadInventory();
            }));
            filters.addView(filterChip("Tetap", "Tetap".equalsIgnoreCase(inventoryStatusFilter), v -> {
                inventoryQuery = value(query);
                inventoryStatusFilter = "Tetap";
                loadInventory();
            }));
            filters.addView(filterChip("Titipan", "Titipan".equalsIgnoreCase(inventoryStatusFilter), v -> {
                inventoryQuery = value(query);
                inventoryStatusFilter = "Titipan";
                loadInventory();
            }));
            filters.addView(filterChip("Lorong", "Lorong".equalsIgnoreCase(inventoryStatusFilter), v -> {
                inventoryQuery = value(query);
                inventoryStatusFilter = "Lorong";
                loadInventory();
            }));
            search.addView(filters);
            search.addView(primaryButton("Cari Inventory", v -> {
                inventoryQuery = value(query);
                loadInventory();
            }));
        } else {
            search.addView(small(activeFilterText(inventoryQuery, inventoryStatusFilter, "Filter disembunyikan. Tap untuk cari barang atau rak tertentu.")));
        }
        content.addView(search);

        LinearLayout list = card();
        list.addView(sectionTitle("Stok Barang"));
        if (loading) {
            list.addView(body("Memuat inventory dari database..."));
        } else if (!error.isEmpty()) {
            TextView err = body(error);
            err.setTextColor(AMBER);
            list.addView(err);
            list.addView(primaryButton("Coba Lagi", v -> loadInventory()));
        } else if (rows == null || rows.length() == 0) {
            list.addView(body("Data inventory tidak ditemukan."));
        } else {
            JSONArray displayRows = sortInventoryRows(rows);
            int limit = Math.min(displayRows.length(), 80);
            list.addView(body("Menampilkan " + limit + " dari " + displayRows.length()
                    + " baris stok. Rak Tetap/Lorong = siap picking, Rak Titipan = ready gudang belum transfer."));
            for (int i = 0; i < limit; i++) {
                JSONObject row = displayRows.optJSONObject(i);
                if (row != null) {
                    list.addView(inventoryRow(row));
                }
            }
        }
        content.addView(list);
    }

    private void showTransactions() {
        loadTransactions();
    }

    private void loadTransactions() {
        renderTransactions(null, true, "");
        executor.execute(() -> {
            try {
                JSONObject response = apiClient.getWmsTransactions(transactionQuery, transactionTypeFilter);
                JSONArray rows = normalizeRows(response);
                uiHandler.post(() -> renderTransactions(rows, false, ""));
            } catch (Exception e) {
                uiHandler.post(() -> renderTransactions(new JSONArray(), false, operatorErrorMessage(e, "History transaksi belum bisa dimuat.")));
            }
        });
    }

    private void renderTransactions(JSONArray rows, boolean loading, String error) {
        currentScreen = "transactions";
        setShell("History Transaksi", "Riwayat masuk, picking, transfer, dan adjustment", "dashboard", true);

        LinearLayout search = card();
        search.addView(dropdownHeader(filterSummary("Filter History", transactionQuery, transactionTypeFilter), transactionFilterOpen, v -> {
            transactionFilterOpen = !transactionFilterOpen;
            renderTransactions(rows, loading, error);
        }));
        if (transactionFilterOpen) {
            EditText query = input(transactionQuery, "Cari nota / barang / kode", InputType.TYPE_CLASS_TEXT);
            search.addView(query);
            LinearLayout filters = horizontal();
            filters.setPadding(0, dp(6), 0, dp(10));
            filters.addView(filterChip("Semua", transactionTypeFilter.isEmpty(), v -> {
                transactionQuery = value(query);
                transactionTypeFilter = "";
                loadTransactions();
            }));
            filters.addView(filterChip("Incoming", "BD".equalsIgnoreCase(transactionTypeFilter), v -> {
                transactionQuery = value(query);
                transactionTypeFilter = "BD";
                loadTransactions();
            }));
            filters.addView(filterChip("Picking", "PICK".equalsIgnoreCase(transactionTypeFilter), v -> {
                transactionQuery = value(query);
                transactionTypeFilter = "PICK";
                loadTransactions();
            }));
            search.addView(filters);

            LinearLayout filters2 = horizontal();
            filters2.setPadding(0, 0, 0, dp(10));
            filters2.addView(filterChip("Transfer", "TR".equalsIgnoreCase(transactionTypeFilter), v -> {
                transactionQuery = value(query);
                transactionTypeFilter = "TR";
                loadTransactions();
            }));
            filters2.addView(filterChip("Adjustment", "ADJ".equalsIgnoreCase(transactionTypeFilter), v -> {
                transactionQuery = value(query);
                transactionTypeFilter = "ADJ";
                loadTransactions();
            }));
            search.addView(filters2);
            search.addView(primaryButton("Cari History", v -> {
                transactionQuery = value(query);
                loadTransactions();
            }));
        } else {
            search.addView(small(activeFilterText(transactionQuery, transactionTypeFilter, "Filter disembunyikan. Tap untuk cari nota atau tipe transaksi.")));
        }
        content.addView(search);

        LinearLayout list = card();
        list.addView(sectionTitle("Transaksi Terakhir"));
        if (loading) {
            list.addView(body("Memuat riwayat transaksi..."));
        } else if (!error.isEmpty()) {
            TextView err = body(error);
            err.setTextColor(AMBER);
            list.addView(err);
            list.addView(primaryButton("Coba Lagi", v -> loadTransactions()));
        } else if (rows == null || rows.length() == 0) {
            list.addView(body("Belum ada transaksi sesuai filter."));
        } else {
            int limit = Math.min(rows.length(), 80);
            list.addView(body("Menampilkan " + limit + " dari " + rows.length() + " transaksi."));
            for (int i = 0; i < limit; i++) {
                JSONObject row = rows.optJSONObject(i);
                if (row != null) {
                    list.addView(transactionRow(row));
                }
            }
        }
        content.addView(list);
        content.addView(body("Riwayat diambil dari API server. Riwayat lokal HP dinonaktifkan."));
    }

    private void showWmsAlerts() {
        renderWmsAlerts(null, true, "");
        executor.execute(() -> {
            try {
                JSONObject response = apiClient.getWmsLowStockAlerts();
                JSONArray rows = normalizeRows(response);
                uiHandler.post(() -> renderWmsAlerts(rows, false, ""));
            } catch (Exception e) {
                uiHandler.post(() -> renderWmsAlerts(new JSONArray(), false, operatorErrorMessage(e, "Alert stok rendah belum bisa dimuat.")));
            }
        });
    }

    private void renderWmsAlerts(JSONArray rows, boolean loading, String error) {
        currentScreen = "wms_alerts";
        setShell("Alert WMS", "Stok rendah dan rak perlu tindak lanjut", "warehouse", true);

        LinearLayout info = card();
        info.addView(sectionTitle("Low Stock Alert"));
        info.addView(body("Mengikuti tab Alert di web WMS. Gunakan untuk melihat stok/rak yang perlu dipindah, diisi, atau dicek ulang."));
        info.addView(primaryButton("Refresh Alert", v -> showWmsAlerts()));
        content.addView(info);

        LinearLayout list = card();
        list.addView(sectionTitle("Daftar Alert"));
        if (loading) {
            list.addView(body("Memuat alert stok rendah..."));
        } else if (!error.isEmpty()) {
            TextView err = body(error);
            err.setTextColor(AMBER);
            list.addView(err);
        } else if (rows == null || rows.length() == 0) {
            list.addView(body("Tidak ada alert stok rendah."));
        } else {
            int limit = Math.min(rows.length(), 80);
            list.addView(body("Menampilkan " + limit + " dari " + rows.length() + " alert."));
            for (int i = 0; i < limit; i++) {
                JSONObject row = rows.optJSONObject(i);
                if (row != null) {
                    list.addView(wmsAlertRow(row));
                }
            }
        }
        content.addView(list);
    }

    private LinearLayout wmsAlertRow(JSONObject item) {
        LinearLayout row = vertical();
        row.setPadding(0, dp(12), 0, dp(12));
        row.setBackground(separatorBackground());

        String rack = fallback(first(item, "kode_rak", "KodeRak", "location"), "-");
        String code = fallback(first(item, "kode_barang", "KodeBarang", "product_code"), "-");
        String name = fallback(first(item, "nama_barang", "NamaBarang", "product_name"), "-");
        String qty = fallback(first(item, "qty", "qty_pcs", "SaldoRak", "stock_qty"), "0");

        LinearLayout top = horizontal();
        LinearLayout text = vertical();
        text.addView(title(code, 15));
        text.addView(small(name));
        text.addView(small("Rak: " + rack + " | Qty: " + qty));
        top.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        top.addView(badge("LOW", AMBER));
        row.addView(top);

        row.addView(compactAction("Buka Transfer", v -> {
            transferQuery = "-".equals(code) ? "" : code;
            transferSourceRack = "";
            pendingTransferTargetRack = "";
            loadTransferStocks();
        }));
        return row;
    }

    private void showLoadingList() {
        showLoadingAssignmentForm();
    }

    private void showLoadingAssignmentForm() {
        final JSONObject previous = loadingSelection;
        loadingSelection = null;
        currentScreen = "loading_selection";
        showLoadingScreen("Loading Armada", "Memuat master armada dan driver...");
        final LinearLayout pendingContent = content;
        executor.execute(() -> {
            try {
                JSONObject options = apiClient.getLoadingOptions();
                // A missing array is an API contract error, not an empty master.
                JSONArray vehicles = options.getJSONArray("vehicles");
                JSONArray drivers = options.getJSONArray("drivers");
                uiHandler.post(() -> {
                    if (isFinishing() || content != pendingContent) return;
                    renderLoadingAssignmentForm(vehicles, drivers, previous);
                });
            } catch (Exception e) {
                uiHandler.post(() -> {
                    if (isFinishing() || content != pendingContent) return;
                    loadingSelection = previous;
                    showApiError("Loading Armada", "Master armada dan driver belum bisa dimuat.", e, () -> showLoadingAssignmentForm());
                });
            }
        });
    }

    private void renderLoadingAssignmentForm(JSONArray vehicles, JSONArray drivers, JSONObject previous) {
        setShell("Loading Armada", "Pilih kendaraan, driver, dan tanggal pengiriman.", "dashboard", true);
        final JSONObject[] vehicle = {findMasterById(vehicles, previous == null ? "" : previous.optString("id_armada"))};
        final JSONObject[] driver = {findMasterById(drivers, previous == null ? "" : previous.optString("id_driver"))};
        final JSONObject[] replacementDriver = {findMasterById(drivers, previous == null ? "" : previous.optString("replacement_driver_id"))};
        final String[] date = {previous == null ? "" : previous.optString("delivery_date")};
        LinearLayout form = card();
        form.addView(sectionTitle("Jadwal Loading"));
        form.addView(small(vehicles.length() + " armada · " + drivers.length() + " driver dari master sesuai akses akun."));
        form.addView(space(16));
        form.addView(title("Armada", 16));
        TextView vehicleField = loadingChoiceField("Pilih armada", R.drawable.ic_expand_more);
        form.addView(vehicleField);
        form.addView(space(12));
        form.addView(title("Driver Jadwal", 16));
        TextView driverField = loadingChoiceField("Pilih driver sesuai jadwal", R.drawable.ic_expand_more);
        form.addView(driverField);
        form.addView(space(12));
        form.addView(title("Driver Pengganti (opsional)", 16));
        TextView replacementDriverField = loadingChoiceField("Gunakan driver jadwal", R.drawable.ic_expand_more);
        form.addView(replacementDriverField);
        form.addView(small("Pilih pengganti jika driver jadwal berhalangan. Driver jadwal tetap dipakai untuk mencari barang."));
        form.addView(space(12));
        form.addView(title("Tanggal Pengiriman", 16));
        TextView dateField = loadingChoiceField("Pilih tanggal dari kalender", R.drawable.ic_calendar);
        form.addView(dateField);
        form.addView(space(12));
        form.addView(small("Driver pengganti dicatat untuk manifest Loading. Armada dan tanggal tetap mengikuti jadwal."));
        Button submit = primaryButton("Tampilkan Barang Lolos Checker", v -> {
            if (vehicle[0] == null || driver[0] == null || date[0].isEmpty()) return;
            try {
                JSONObject selection = new JSONObject();
                selection.put("id_armada", vehicle[0].getInt("id_armada"));
                selection.put("id_driver", driver[0].getInt("id_driver"));
                selection.put("vehicle", masterRowLabel(vehicle[0], true));
                selection.put("driver", masterRowLabel(driver[0], false));
                if (replacementDriver[0] != null
                        && replacementDriver[0].optInt("id_driver") != driver[0].optInt("id_driver")) {
                    selection.put("replacement_driver_id", replacementDriver[0].getInt("id_driver"));
                    selection.put("replacement_driver", masterRowLabel(replacementDriver[0], false));
                }
                selection.put("delivery_date", date[0]);
                loadingQuery = ""; // An old search must not hide goods for the new selection.
                loadingSelection = selection;
                loadLoadingList();
            } catch (Exception error) { toast("Pilihan master tidak valid. Muat ulang daftar armada dan driver."); }
        });
        Runnable update = () -> {
            updateLoadingChoice(vehicleField, "Armada", vehicle[0] == null ? "Pilih armada" : masterRowLabel(vehicle[0], true), vehicle[0] != null);
            updateLoadingChoice(driverField, "Driver", driver[0] == null ? "Pilih driver" : masterRowLabel(driver[0], false), driver[0] != null);
                    updateLoadingChoice(replacementDriverField, "Driver pengganti",
                    replacementDriver[0] == null ? "Gunakan driver jadwal" : masterRowLabel(replacementDriver[0], false),
                    replacementDriver[0] != null);
            String displayDate = date[0].isEmpty() ? "Pilih tanggal dari kalender" : date[0].substring(8, 10) + "/" + date[0].substring(5, 7) + "/" + date[0].substring(0, 4);
            updateLoadingChoice(dateField, "Tanggal pengiriman", displayDate, !date[0].isEmpty());
            boolean ready = vehicle[0] != null && driver[0] != null && !date[0].isEmpty();
            submit.setEnabled(ready);
            submit.setAlpha(ready ? 1f : 0.5f);
        };
        vehicleField.setOnClickListener(v -> showLoadingMasterPicker("Pilih Armada", vehicles, true, picked -> { vehicle[0] = picked; update.run(); }));
        driverField.setOnClickListener(v -> showLoadingMasterPicker("Pilih Driver sesuai Jadwal", drivers, false, picked -> { driver[0] = picked; update.run(); }));
        replacementDriverField.setOnClickListener(v -> showLoadingMasterPicker("Pilih Driver Pengganti", drivers, false, picked -> {
            replacementDriver[0] = picked;
            update.run();
        }));
        dateField.setOnClickListener(v -> {
            Calendar initial = Calendar.getInstance();
            if (!date[0].isEmpty()) initial.set(Integer.parseInt(date[0].substring(0, 4)), Integer.parseInt(date[0].substring(5, 7)) - 1, Integer.parseInt(date[0].substring(8, 10)));
            DatePickerDialog picker = new DatePickerDialog(this,
                    isDarkMode() ? R.style.LoadingCalendarDark : R.style.LoadingCalendarLight,
                    (view, year, month, day) -> { date[0] = LoadingFormSupport.isoDate(year, month, day); update.run(); },
                    initial.get(Calendar.YEAR), initial.get(Calendar.MONTH), initial.get(Calendar.DAY_OF_MONTH));
            picker.setTitle("Tanggal Pengiriman");
            picker.setButton(android.content.DialogInterface.BUTTON_POSITIVE, "Pilih", picker);
            picker.setButton(android.content.DialogInterface.BUTTON_NEGATIVE, "Batal", picker);
            picker.show();
        });
        if (vehicles.length() == 0 || drivers.length() == 0) form.addView(body("Master armada atau driver belum tersedia untuk akses akun ini. Periksa data master dan hak cabang/perusahaan."));
        form.addView(submit);
        content.addView(form);
        update.run();
    }

    private TextView loadingChoiceField(String hint, int icon) {
        TextView field = masterSelectionField(hint);
        field.setSingleLine(false);
        field.setMaxLines(3);
        field.setMinHeight(dp(56));
        field.setPadding(dp(14), dp(12), dp(14), dp(12));
        field.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
        field.setFocusable(true);
        android.graphics.drawable.Drawable arrow = getDrawable(icon).mutate();
        arrow.setTint(MUTED);
        field.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, arrow, null);
        field.setCompoundDrawablePadding(dp(12));
        return field;
    }

    private void updateLoadingChoice(TextView field, String label, String value, boolean selected) {
        field.setText(value);
        field.setTextColor(selected ? INK : MUTED);
        field.setContentDescription(label + ": " + value + ". Ketuk untuk memilih.");
    }

    private void showLoadingMasterPicker(String label, JSONArray rows, boolean vehicle, MasterSelectionAction action) {
        LinearLayout pickerBody = vertical();
        pickerBody.setPadding(dp(16), dp(8), dp(16), dp(8));
        pickerBody.setBackgroundColor(CARD);
        EditText query = input("", vehicle ? "Cari plat, kode, nama, atau cabang" : "Cari nama driver atau cabang", InputType.TYPE_CLASS_TEXT);
        query.setContentDescription(vehicle ? "Cari armada" : "Cari driver");
        pickerBody.addView(query);
        TextView count = small("");
        pickerBody.addView(count);
        ArrayList<JSONObject> filtered = new ArrayList<>();
        android.widget.ListView list = new android.widget.ListView(this);
        list.setDivider(null);
        list.setDividerHeight(dp(6));
        android.widget.ArrayAdapter<JSONObject> adapter = new android.widget.ArrayAdapter<JSONObject>(this, android.R.layout.simple_list_item_1, filtered) {
            @Override public View getView(int position, View recycled, ViewGroup parent) {
                JSONObject row = getItem(position);
                String detail = masterRowDetail(row, vehicle);
                if (vehicle && !row.optString("status_operasional", "AVAILABLE").equalsIgnoreCase("AVAILABLE")) detail += " · " + row.optString("status_operasional");
                TextView option = recycled instanceof TextView ? (TextView) recycled : masterOptionRow("", "");
                option.setText(masterRowLabel(row, vehicle) + "\n" + detail + " · ID " + masterRowId(row));
                option.setMinHeight(dp(56));
                option.setLayoutParams(new android.widget.AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                return option;
            }
        };
        list.setAdapter(adapter);
        int listHeight = Math.min(dp(520), getResources().getDisplayMetrics().heightPixels * 55 / 100);
        pickerBody.addView(list, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, listHeight));
        AlertDialog dialog = new AlertDialog.Builder(this, isDarkMode() ? android.R.style.Theme_Material_Dialog_Alert : android.R.style.Theme_Material_Light_Dialog_Alert)
                .setTitle(label).setView(pickerBody).setNegativeButton("Batal", null).create();
        Runnable filter = () -> {
            filtered.clear();
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.optJSONObject(i);
                if (row != null && LoadingFormSupport.matches(value(query), masterRowLabel(row, vehicle), masterRowDetail(row, vehicle), row.optString("kode"), masterRowId(row))) filtered.add(row);
            }
            count.setText(filtered.isEmpty() ? "Tidak ada hasil. Coba kata pencarian lain." : filtered.size() + " dari " + rows.length() + " pilihan");
            adapter.notifyDataSetChanged();
            list.setSelection(0);
        };
        query.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filter.run(); }
            @Override public void afterTextChanged(Editable s) { }
        });
        list.setOnItemClickListener((parent, view, position, id) -> { action.onSelect(adapter.getItem(position)); dialog.dismiss(); });
        filter.run();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(BLUE);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                Math.min(dp(680), getResources().getDisplayMetrics().heightPixels * 88 / 100));
            dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE | android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        }
    }

    private void loadLoadingList() {
        if (loadingSelection == null) { showLoadingAssignmentForm(); return; }
        final JSONObject selectedAssignment = loadingSelection;
        renderLoadingList(null, true, "");
        final LinearLayout pendingContent = content;
        executor.execute(() -> {
            try {
                JSONObject response = apiClient.getWmsReadyToLoad(loadingQuery, selectedAssignment);
                JSONArray rows = normalizeRows(response);
                uiHandler.post(() -> { if (!isFinishing() && content == pendingContent && loadingSelection == selectedAssignment) renderLoadingList(rows, false, ""); });
            } catch (Exception e) {
                uiHandler.post(() -> { if (!isFinishing() && content == pendingContent && loadingSelection == selectedAssignment) renderLoadingList(new JSONArray(), false, operatorErrorMessage(e, "Data ready-to-load belum bisa dimuat.")); });
            }
        });
    }

    private void renderLoadingList(JSONArray rows, boolean loading, String error) {
        currentScreen = "loading";
        setShell("Loading / Shipping", "Daftar picking siap dimuat", "dashboard", true);
        if (loadingSelection != null) {
            String driverLabel = first(loadingSelection, "driver");
            String replacementLabel = first(loadingSelection, "replacement_driver");
            String driverSummary = replacementLabel.isEmpty()
                ? driverLabel
                : driverLabel + " → Pengganti: " + replacementLabel;
            content.addView(body(first(loadingSelection, "vehicle") + " · " + driverSummary
                + " · " + first(loadingSelection, "delivery_date")));
        }
        content.addView(secondaryButton("Ganti Armada / Driver / Tanggal", v -> showLoadingAssignmentForm()));

        LinearLayout search = card();
        search.addView(dropdownHeader(filterSummary("Cari Loading", loadingQuery, ""), loadingFilterOpen, v -> {
            loadingFilterOpen = !loadingFilterOpen;
            renderLoadingList(rows, loading, error);
        }));
        if (loadingFilterOpen) {
            EditText query = input(loadingQuery, "Cari nota / produk / manifest", InputType.TYPE_CLASS_TEXT);
            search.addView(query);
            search.addView(primaryButton("Muat Ready To Load", v -> {
                loadingQuery = value(query);
                loadLoadingList();
            }));
        } else {
            search.addView(small(activeFilterText(loadingQuery, "", "Filter loading disembunyikan. Tap untuk cari nota atau produk.")));
        }
        content.addView(search);

        LinearLayout list = card();
        list.addView(sectionTitle("Ready To Load / Shipping"));
        if (loading) {
            list.addView(body("Memuat daftar barang siap loading..."));
        } else if (!error.isEmpty()) {
            TextView err = body(error);
            err.setTextColor(AMBER);
            list.addView(err);
            list.addView(primaryButton("Coba Lagi", v -> loadLoadingList()));
        } else if (rows == null || rows.length() == 0) {
            list.addView(body("Belum ada barang lolos checker untuk armada, driver, dan tanggal ini. Periksa jadwal kiriman serta hasil checker, atau ganti pilihan."));
        } else {
            LinkedHashMap<String, JSONArray> scheduleGroups = groupLoadingItemsBySchedule(rows);
            list.addView(body("Menampilkan " + rows.length() + " item dalam " + scheduleGroups.size()
                    + " jadwal kendaraan. Loading harus diproses per jadwal."));
            int shownGroups = 0;
            for (String scheduleKey : scheduleGroups.keySet()) {
                if (shownGroups >= 100) {
                    break;
                }
                JSONArray scheduleItems = scheduleGroups.get(scheduleKey);
                if (scheduleItems != null && scheduleItems.length() > 0) {
                    list.addView(loadingScheduleRow(scheduleItems));
                    shownGroups++;
                }
            }
        }
        content.addView(list);
    }

    private LinkedHashMap<String, JSONArray> groupLoadingItemsBySchedule(JSONArray rows) {
        LinkedHashMap<String, JSONArray> groups = new LinkedHashMap<>();
        if (rows == null) {
            return groups;
        }
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.optJSONObject(i);
            if (row == null) {
                continue;
            }
            String scheduleKey = loadingScheduleKey(row);
            JSONArray group = groups.get(scheduleKey);
            if (group == null) {
                group = new JSONArray();
                groups.put(scheduleKey, group);
            }
            try {
                group.put(new JSONObject(row.toString()));
            } catch (Exception ignored) {
                group.put(row);
            }
        }
        return groups;
    }

    private String loadingScheduleKey(JSONObject item) {
        String key = safe(first(item, "ScheduleKey", "schedule_key")).trim();
        if (!key.isEmpty()) {
            return key;
        }
        return "UNSCHEDULED|" + fallback(first(item, "wms_task_detail_id", "Nota", "NoFaktur"), "unknown");
    }

    private boolean isScheduledLoadingItem(JSONObject item) {
        return "SCHEDULED".equalsIgnoreCase(first(item, "ScheduleStatus", "schedule_status"));
    }

    private void showLoadingConfirm(JSONArray scheduleItems) {
        currentScreen = "loading_confirm";
        setShell("Konfirmasi Loading", "Gunakan penugasan armada yang sudah dijadwalkan", "dashboard", true);
        renderLoadingConfirm(scheduleItems);
    }

    private void renderLoadingConfirm(JSONArray scheduleItems) {
        currentScreen = "loading_confirm";
        setShell("Konfirmasi Loading", "Driver, armada, dan helper mengikuti jadwal", "dashboard", true);
        String validationError = loadingScheduleValidationProblem(scheduleItems);
        if (!validationError.isEmpty()) {
            LinearLayout problem = card();
            TextView message = body(validationError);
            message.setTextColor(AMBER);
            problem.addView(sectionTitle("Jadwal Belum Siap"));
            problem.addView(message);
            problem.addView(primaryButton("Muat Ulang Loading", v -> loadLoadingList()));
            content.addView(problem);
            return;
        }

        JSONObject item = scheduleItems.optJSONObject(0);
        int totalQty = loadingGroupQty(scheduleItems);

        LinearLayout detail = card();
        detail.addView(sectionTitle("Penugasan Armada (read-only)"));
        detail.addView(keyValue("Tanggal Kirim", fallback(first(item, "ScheduledDeliveryDate", "scheduled_delivery_date"), "-")));
        detail.addView(keyValue("Driver", fallback(first(item, "DriverRencana", "driver_name"), "-")));
        detail.addView(keyValue("Armada", fallback(first(item, "ArmadaRencana", "armada_name", "vehicle_no"), "-")));
        detail.addView(keyValue("Helper", fallback(first(item, "HelperRencana", "helper_name"), "-")));
        detail.addView(keyValue("Referensi Draft", fallback(first(item, "CalonNoManifest"), "Dibuat oleh server")));
        detail.addView(keyValue("Loading Dock", fallback(first(item, "DockRencana"), "Belum diisi")));
        detail.addView(keyValue("Zona Muatan", fallback(first(item, "ZonaMuatan"), "Belum diisi")));
        detail.addView(keyValue("Catatan", fallback(first(item, "CatatanPengiriman"), "-")));
        content.addView(detail);

        LinearLayout form = card();
        form.addView(sectionTitle("Item dalam Jadwal Ini"));
        form.addView(body(scheduleItems.length() + " item / " + totalQty + " PCS akan dimuat dengan penugasan di atas."));
        int limit = Math.min(scheduleItems.length(), 40);
        for (int i = 0; i < limit; i++) {
            JSONObject scheduledItem = scheduleItems.optJSONObject(i);
            if (scheduledItem == null) {
                continue;
            }
            form.addView(small((i + 1) + ". " + fallback(first(scheduledItem, "Kode", "KodeBarang"), "-")
                    + " — " + fallback(first(scheduledItem, "Nama", "NamaBarang", "product_name"), "-")
                    + " (" + loadingQty(scheduledItem) + " PCS)"));
        }
        if (scheduleItems.length() > limit) {
            form.addView(small("+ " + (scheduleItems.length() - limit) + " item lainnya"));
        }
        boolean allLoading = true;
        for (int i = 0; i < scheduleItems.length(); i++) {
            JSONObject row = scheduleItems.optJSONObject(i);
            if (row == null || !"LOADING".equals(first(row, "loading_status"))) allLoading = false;
        }
        String loadingAction = allLoading ? "complete" : "start";
        form.addView(primaryButton(allLoading ? "Selesaikan Loading" : "Mulai Loading", v -> submitLoading(scheduleItems, loadingAction)));
        form.addView(space(6));
        form.addView(secondaryButton("Kembali ke Loading", v -> loadLoadingList()));
        content.addView(form);
    }

    private int loadingGroupQty(JSONArray items) {
        int total = 0;
        if (items == null) {
            return total;
        }
        for (int i = 0; i < items.length(); i++) {
            total += loadingQty(items.optJSONObject(i));
        }
        return total;
    }

    private String loadingScheduleValidationProblem(JSONArray items) {
        if (items == null || items.length() == 0) {
            return "Pilih jadwal loading terlebih dahulu.";
        }
        String expectedKey = "";
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.optJSONObject(i);
            if (item == null) {
                return "Data item loading tidak valid. Muat ulang daftar loading.";
            }
            if (!isScheduledLoadingItem(item)) {
                return "Jadwal armada belum lengkap. Lengkapi driver, armada, dan helper pada Penjadwalan Armada terlebih dahulu.";
            }
            String scheduleKey = loadingScheduleKey(item);
            if (expectedKey.isEmpty()) {
                expectedKey = scheduleKey;
            } else if (!expectedKey.equals(scheduleKey)) {
                return "Item berasal dari jadwal kendaraan yang berbeda. Proses loading satu jadwal setiap kali.";
            }
            String itemProblem = loadingItemValidationProblem(item);
            if (!itemProblem.isEmpty()) {
                return itemProblem;
            }
        }
        return "";
    }

    private String loadingItemValidationProblem(JSONObject item) {
        if (item == null) {
            return "Pilih item loading terlebih dahulu.";
        }
        if (first(item, "Kode", "KodeBarang").isEmpty()) {
            return "Kode barang loading belum jelas. Muat ulang daftar loading.";
        }
        if (first(item, "Nama", "NamaBarang", "product_name").isEmpty()) {
            return "Nama barang loading belum jelas. Muat ulang daftar loading.";
        }
        if (loadingQty(item) <= 0) {
            return "Qty loading harus lebih dari 0.";
        }
        return "";
    }

    private void submitLoading(JSONArray scheduleItems, String action) {
        String problem = loadingScheduleValidationProblem(scheduleItems);
        if (!problem.isEmpty()) {
            toast(problem);
            return;
        }
        JSONObject firstItem = scheduleItems.optJSONObject(0);
        showLoadingScreen("Loading", "Menyimpan " + scheduleItems.length() + " item sesuai jadwal armada...");
        executor.execute(() -> {
            try {
                JSONArray items = new JSONArray();
                for (int i = 0; i < scheduleItems.length(); i++) {
                    JSONObject item = scheduleItems.optJSONObject(i);
                    if (item == null) {
                        continue;
                    }
                    items.put(new JSONObject(item.toString()));
                }

                JSONObject payload = new JSONObject();
                payload.put("items", items);
                payload.put("action", action);
                payload.put("id_armada", firstItem.optInt("ScheduledArmadaId"));
                payload.put("id_driver", firstItem.optInt("ScheduledDriverId"));
                payload.put("delivery_date", firstItem.optString("ScheduledDeliveryDate"));
                if (loadingSelection != null) {
                    int replacementDriverId = loadingSelection.optInt("replacement_driver_id", 0);
                    if (replacementDriverId > 0) {
                        payload.put("replacement_driver_id", replacementDriverId);
                    }
                }

                JSONObject result = apiClient.processWmsLoading(payload);
                uiHandler.post(() -> {
                    if ("start".equals(action)) {
                        toast("Loading dimulai. Muat seluruh barang, lalu pilih Selesaikan Loading.");
                        loadLoadingList();
                        return;
                    }
                    String noManifest = fallback(first(result, "no_manifest"), fallback(first(firstItem, "CalonNoManifest"), "Manifest"));
                    recordLocalTransaction("Loading", noManifest, "", first(firstItem, "Kode", "KodeBarang"), first(firstItem, "Nama", "NamaBarang", "product_name"), fallback(first(firstItem, "KodeRak", "kode_rak", "location"), "-"), "Manifest Loading", loadingGroupQty(scheduleItems), "PCS", "Item masuk manifest sesuai jadwal armada.");
                    showLoadingSuccess(result, noManifest);
                });
            } catch (Exception e) {
                uiHandler.post(() -> showApiError("Loading", "Loading belum berhasil disimpan.", e, () -> showLoadingConfirm(scheduleItems)));
            }
        });
    }

    private void showLoadingSuccess(JSONObject result, String manifest) {
        currentScreen = "loading_success";
        setShell("Loading Berhasil", "Manifest siap shipping", "dashboard", true);

        LinearLayout success = card();
        success.setGravity(Gravity.CENTER_HORIZONTAL);
        success.setBackground(successBackground());
        TextView check = title("OK", 36);
        check.setTextColor(GREEN);
        success.addView(check);
        success.addView(title("Loading Berhasil", 20));
        success.addView(body("Manifest " + fallback(first(result, "no_manifest"), manifest) + " sudah tersimpan."));
        JSONObject actualDriver = result.optJSONObject("driver");
        String driverName = first(actualDriver, "nama");
        if (driverName.isEmpty()) {
            driverName = fallback(first(loadingSelection, "replacement_driver"), first(loadingSelection, "driver"));
        }
        success.addView(keyValue("Driver Aktual", fallback(driverName, "-")));
        success.addView(keyValue("Total Item", fallback(first(result, "total_item"), "1")));
        content.addView(success);

        content.addView(primaryButton("Refresh Loading", v -> loadLoadingList()));
        content.addView(space(8));
        content.addView(secondaryButton("Lihat History", v -> {
            transactionQuery = manifest;
            showTransactions();
        }));
    }

    private void showTransferStocks() {
        currentScreen = "transfer_scan";
        selectedTransferStock = null;
        pendingTransferTargetRack = "";
        transferSourceRack = "";
        transferFilterOpen = false;
        transferTargetPanelOpen = true;
        setShell("Transfer Rak", "Scan rak tujuan dulu", "warehouse", true);

        LinearLayout scanner = card();
        scanner.addView(sectionTitle("Rak Tujuan"));
        scanner.addView(body("Scan rak tetap tujuan. Setelah rak terbaca, sistem akan menampilkan stok titipan yang bisa dipindahkan ke rak tersebut."));
        scanner.addView(primaryButton("Scan Rak Tujuan", v -> startScanner("TRANSFER_TARGET_ITEM")));
        content.addView(scanner);

        LinearLayout manual = card();
        manual.addView(dropdownHeader("Input Manual Rak", transferManualTargetOpen, v -> {
            transferManualTargetOpen = !transferManualTargetOpen;
            showTransferStocks();
        }));
        if (transferManualTargetOpen) {
            EditText payload = input("", "Kode rak tujuan", InputType.TYPE_CLASS_TEXT);
            manual.addView(payload);
            manual.addView(secondaryButton("Pakai Rak Ini", v -> processTransferTargetItem(value(payload))));
        } else {
            manual.addView(small("Dibuka hanya jika QR rak tidak bisa discan."));
        }
        content.addView(manual);
    }

    private void loadTransferStocks() {
        renderTransferStocks(null, true, "");
        executor.execute(() -> {
            try {
                JSONObject response = apiClient.getWmsTemporaryStocks(transferQuery, transferSourceRack, pendingTransferTargetRack);
                JSONArray rows = normalizeTransferSourceRows(normalizeRows(response));
                uiHandler.post(() -> renderTransferStocks(rows, false, ""));
            } catch (Exception e) {
                uiHandler.post(() -> renderTransferStocks(new JSONArray(), false, operatorErrorMessage(e, "Stok transfer belum bisa dimuat.")));
            }
        });
    }

    private void loadTransferStocksForTarget(String productCode, String targetRack) {
        transferQuery = productCode;
        pendingTransferTargetRack = targetRack;
        renderTransferStocks(null, true, "");
        executor.execute(() -> {
            try {
                JSONObject response = apiClient.getWmsTemporaryStocks(transferQuery, transferSourceRack, pendingTransferTargetRack);
                JSONArray rows = normalizeTransferSourceRows(normalizeRows(response));
                uiHandler.post(() -> renderTransferStocks(rows, false, ""));
            } catch (Exception e) {
                uiHandler.post(() -> renderTransferStocks(new JSONArray(), false, operatorErrorMessage(e, "Stok transfer belum bisa dimuat.")));
            }
        });
    }

    private String transferInventorySearch() {
        String productOrName = safe(transferQuery).trim();
        if (!productOrName.isEmpty()) {
            return productOrName;
        }
        return safe(transferSourceRack).trim();
    }

    private JSONArray normalizeTransferSourceRows(JSONArray sourceRows) {
        JSONArray rows = new JSONArray();
        if (sourceRows == null) {
            return rows;
        }
        String rackFilter = safe(transferSourceRack).trim().toUpperCase(Locale.US);
        String queryFilter = safe(transferQuery).trim().toUpperCase(Locale.US);
        for (int i = 0; i < sourceRows.length(); i++) {
            JSONObject raw = sourceRows.optJSONObject(i);
            if (raw == null || isFixedRackStock(raw)) {
                continue;
            }
            JSONObject row = normalizeTransferSourceRow(raw);
            if (!rackFilter.isEmpty() && !transferStockRack(row).toUpperCase(Locale.US).contains(rackFilter)) {
                continue;
            }
            if (!queryFilter.isEmpty() && !transferStockSearchText(row).contains(queryFilter)) {
                continue;
            }
            rows.put(row);
        }
        return rows;
    }

    private JSONObject normalizeTransferSourceRow(JSONObject raw) {
        JSONObject row = new JSONObject();
        copyJson(row, raw);
        putNormalized(row, "wms_stock_rak_id", first(raw, "wms_stock_rak_id", "id_stock_rak", "stock_rak_id", "id"));
        putNormalized(row, "kode_barang", first(raw, "kode_barang", "KodeBarang", "product_code", "kode_sku", "sku"));
        putNormalized(row, "nama_barang", first(raw, "nama_barang", "NamaBarang", "product_name", "nama_produk", "Nama"));
        putNormalized(row, "kode_rak_asal", transferStockRack(raw));
        putNormalized(row, "type_rak", transferStockRackType(raw));
        int perUnit = transferStockPerUnit(raw);
        int qtyPcs = transferStockQty(raw);
        int qtyCt = parseOptionalInt(raw, -1, "qty_karton", "quantity_ct", "qty_ct", "QtyCT");
        int qtyPc = parseOptionalInt(raw, -1, "quantity_pc", "qty_pc", "QtyPC");
        if (qtyCt < 0) {
            qtyCt = qtyPcs / perUnit;
        }
        if (qtyPc < 0) {
            qtyPc = qtyPcs % perUnit;
        }
        putNormalized(row, "per_unit", String.valueOf(perUnit));
        putNormalized(row, "qty", String.valueOf(qtyPcs));
        putNormalized(row, "qty_pcs", String.valueOf(qtyPcs));
        putNormalized(row, "qty_karton", String.valueOf(qtyCt));
        putNormalized(row, "quantity_ct", String.valueOf(qtyCt));
        putNormalized(row, "quantity_pc", String.valueOf(qtyPc));
        putNormalized(row, "batch", first(raw, "batch", "batch_number", "Batch"));
        putNormalized(row, "expired_date", first(raw, "expired_date", "expiry_date", "expired", "Expired"));
        putNormalized(row, "tanggal_masuk_gudang", first(raw, "tanggal_masuk_gudang", "received_at", "incoming_at", "created_at"));
        putNormalized(row, "umur_barang_hari", first(raw, "umur_barang_hari", "age_days", "umur_hari"));
        putNormalized(row, "fifo_rank", first(raw, "fifo_rank", "fifoRank", "FIFO"));
        putNormalized(row, "fefo_rank", first(raw, "fefo_rank", "fefoRank", "FEFO"));
        putNormalized(row, "fifo_label", first(raw, "fifo_label", "fifoLabel"));
        putNormalized(row, "fefo_label", first(raw, "fefo_label", "fefoLabel"));
        putNormalized(row, "status", fallback(first(raw, "status", "status_stock", "Status"), "READY"));
        return row;
    }

    private void copyJson(JSONObject target, JSONObject source) {
        if (target == null || source == null) {
            return;
        }
        JSONArray names = source.names();
        if (names == null) {
            return;
        }
        for (int i = 0; i < names.length(); i++) {
            String key = names.optString(i);
            if (key == null || key.isEmpty()) {
                continue;
            }
            try {
                target.put(key, source.opt(key));
            } catch (Exception ignored) {
            }
        }
    }

    private void putNormalized(JSONObject row, String key, String value) {
        if (row == null || key == null || value == null || value.trim().isEmpty()) {
            return;
        }
        try {
            row.put(key, value.trim());
        } catch (Exception ignored) {
        }
    }

    private boolean isFixedRackStock(JSONObject row) {
        String type = transferStockRackType(row);
        return "TETAP".equalsIgnoreCase(type) || type.toUpperCase(Locale.US).contains("TETAP");
    }

    private String transferStockRackType(JSONObject row) {
        return fallback(first(row, "type_rak", "shelf_type", "tipe_rak", "jenis_rak", "rack_type", "StatusRak"), "");
    }

    private String transferStockRack(JSONObject row) {
        return fallback(first(row, "kode_rak_asal", "KodeRakAsal", "location", "kode_rak", "KodeRak", "rack_code"), "");
    }

    private int transferStockQty(JSONObject row) {
        return parseOptionalInt(row, 0, "qty", "qty_pcs", "quantity", "quantity_pc", "qty_pieces", "SaldoRak", "stock_qty", "available_qty");
    }

    private String configuredUomName(JSONObject row, int level, String fallbackValue) {
        String value = fallback(first(row,
                "uom" + level + "_nama",
                "uom_" + level + "_nama",
                "nama_uom_" + level,
                "uom" + level + "Nama",
                "uom_" + level + "_name"), "");
        return value.isEmpty() ? fallbackValue : value;
    }

    private int configuredUomFactor(JSONObject row, int level, int fallbackValue) {
        return Math.max(1, parseOptionalInt(row, fallbackValue,
                "uom" + level + "_factor",
                "uom_" + level + "_factor",
                "faktor_uom_" + level,
                "uom" + level + "Factor",
                "uom_" + level + "_faktor_konversi"));
    }

    private String configuredQtyText(int totalQty, JSONObject row) {
        int total = Math.max(totalQty, 0);
        String primary = configuredUomName(row, 1, "PCS");
        String secondary = configuredUomName(row, 2, "");
        int legacyFactor = Math.max(1, parseOptionalInt(row, 1,
                "per_unit", "PerUnit", "IsiKarton", "isi_per_karton", "isiperkarton", "isiperbox"));
        int secondaryFactor = configuredUomFactor(row, 2, legacyFactor);
        if (!secondary.isEmpty() && secondaryFactor > 1) {
            return (total / secondaryFactor) + " " + secondary
                    + " | " + (total % secondaryFactor) + " " + primary
                    + " (" + total + " " + primary + ")";
        }
        return total + " " + primary;
    }

    private int transferStockPerUnit(JSONObject row) {
        return configuredUomFactor(row, 2, Math.max(1, parseOptionalInt(row, 1,
                "per_unit", "PerUnit", "IsiKarton", "isi_per_karton", "isiperkarton", "isiperbox")));
    }

    private int incomingPerUnit(JSONObject row) {
        return configuredUomFactor(row, 2, Math.max(1, parseOptionalInt(row, 1,
                "PerUnit", "per_unit", "IsiKarton", "isi_per_karton", "isiperkarton", "isiperbox")));
    }

    private int incomingTotalQty(JSONObject row) {
        int direct = parseOptionalInt(row, -1, "qty_pcs", "QtyPCS", "qty_total_pcs", "SaldoRak");
        if (direct >= 0) {
            return direct;
        }
        int qtyCt = parseOptionalInt(row, 0, "QtyCT", "qty_ct", "qty_ct_palet");
        int qtyPc = parseOptionalInt(row, 0, "QtyPC", "qty_pc", "qty_pc_palet");
        return qtyCt * incomingPerUnit(row) + qtyPc;
    }

    private String incomingQtyText(JSONObject row) {
        return configuredQtyText(incomingTotalQty(row), row);
    }

    private int transferStockQtyCt(JSONObject row) {
        int direct = parseOptionalInt(row, -1, "qty_karton", "quantity_ct", "qty_ct", "QtyCT");
        if (direct >= 0) {
            return direct;
        }
        return transferStockQty(row) / transferStockPerUnit(row);
    }

    private int transferStockQtyPc(JSONObject row) {
        int direct = parseOptionalInt(row, -1, "quantity_pc", "qty_pc", "QtyPC");
        if (direct >= 0) {
            return direct;
        }
        return transferStockQty(row) % transferStockPerUnit(row);
    }

    private String transferQtyText(JSONObject row) {
        return configuredQtyText(transferStockQty(row), row);
    }

    private String transferStockBatch(JSONObject row) {
        return fallback(first(row, "batch", "batch_number", "Batch"), "-");
    }

    private String transferStockExpired(JSONObject row) {
        return readableDate(fallback(first(row, "expired_date", "expiry_date", "expired", "Expired"), "-"));
    }

    private String transferStockIncomingDate(JSONObject row) {
        return readableDate(fallback(first(row, "tanggal_masuk_gudang", "received_at", "incoming_at", "created_at"), "-"));
    }

    private String transferStockAgeText(JSONObject row) {
        String direct = fallback(first(row, "umur_barang_hari", "age_days", "umur_hari"), "");
        if (!direct.isEmpty()) {
            int days = parsePositiveInt(direct, -1);
            if (days >= 0) {
                return days == 0 ? "Hari ini" : days + " hari";
            }
        }
        long millis = parseDateMillis(first(row, "tanggal_masuk_gudang", "received_at", "incoming_at", "created_at"));
        if (millis <= 0) {
            return "-";
        }
        long diff = Math.max(0L, System.currentTimeMillis() - millis);
        long days = diff / (24L * 60L * 60L * 1000L);
        return days == 0 ? "Hari ini" : days + " hari";
    }

    private String transferStockFifoLabel(JSONObject row) {
        String label = fallback(first(row, "fifo_label", "fifoLabel"), "");
        if (!label.isEmpty()) {
            return label;
        }
        int rank = parsePositiveInt(fallback(first(row, "fifo_rank", "fifoRank"), "0"), 0);
        if (rank >= 999999) {
            return "FIFO -";
        }
        return rank <= 1 ? "FIFO utama" : "FIFO #" + rank;
    }

    private String transferStockFefoLabel(JSONObject row) {
        String label = fallback(first(row, "fefo_label", "fefoLabel"), "");
        if (!label.isEmpty()) {
            return label;
        }
        int rank = parsePositiveInt(fallback(first(row, "fefo_rank", "fefoRank"), "0"), 0);
        if ("-".equals(transferStockExpired(row))) {
            return "FEFO tanpa exp";
        }
        if (rank >= 999999) {
            return "FEFO -";
        }
        return rank <= 1 ? "FEFO utama" : "FEFO #" + rank;
    }

    private int transferStockRank(JSONObject row, String primaryKey, String alternateKey) {
        int rank = parsePositiveInt(fallback(first(row, primaryKey, alternateKey), "999999"), 999999);
        return rank <= 0 ? 999999 : rank;
    }

    private JSONArray sortTransferRows(JSONArray rows) {
        JSONArray sortedRows = new JSONArray();
        if (rows == null) {
            return sortedRows;
        }
        java.util.ArrayList<JSONObject> list = new java.util.ArrayList<>();
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.optJSONObject(i);
            if (row != null) {
                list.add(row);
            }
        }
        java.util.Collections.sort(list, (left, right) -> {
            String mode = safe(transferSortMode).toUpperCase(Locale.US);
            int primary;
            if ("FIFO".equals(mode)) {
                primary = Integer.compare(
                        transferStockRank(left, "fifo_rank", "fifoRank"),
                        transferStockRank(right, "fifo_rank", "fifoRank"));
                if (primary != 0) {
                    return primary;
                }
                primary = Integer.compare(
                        transferStockRank(left, "fefo_rank", "fefoRank"),
                        transferStockRank(right, "fefo_rank", "fefoRank"));
            } else if ("SEMUA".equals(mode)) {
                primary = transferStockRack(left).compareToIgnoreCase(transferStockRack(right));
            } else {
                primary = Integer.compare(
                        transferStockRank(left, "fefo_rank", "fefoRank"),
                        transferStockRank(right, "fefo_rank", "fefoRank"));
                if (primary != 0) {
                    return primary;
                }
                primary = Integer.compare(
                        transferStockRank(left, "fifo_rank", "fifoRank"),
                        transferStockRank(right, "fifo_rank", "fifoRank"));
            }
            if (primary != 0) {
                return primary;
            }
            primary = transferStockRack(left).compareToIgnoreCase(transferStockRack(right));
            if (primary != 0) {
                return primary;
            }
            return safe(first(left, "kode_barang", "KodeBarang")).compareToIgnoreCase(safe(first(right, "kode_barang", "KodeBarang")));
        });
        for (JSONObject row : list) {
            sortedRows.put(row);
        }
        return sortedRows;
    }

    private String transferSortDescription() {
        String mode = safe(transferSortMode).toUpperCase(Locale.US);
        if ("FIFO".equals(mode)) {
            return "FIFO: barang masuk paling awal tampil paling atas.";
        }
        if ("SEMUA".equals(mode)) {
            return "Semua: urutan rak titipan dan kode barang.";
        }
        return "FEFO: expired paling dekat tampil paling atas.";
    }

    private String transferQtyTextFromPcs(int qtyPcs, int perUnit) {
        int unit = Math.max(1, perUnit);
        return (Math.max(qtyPcs, 0) / unit) + " CT / " + (Math.max(qtyPcs, 0) % unit) + " PC";
    }

    private int transferQtyFromInputs(EditText qtyCt, EditText qtyPc, int perUnit) {
        int ct = parsePositiveInt(value(qtyCt), 0);
        int pc = parsePositiveInt(value(qtyPc), 0);
        return ct * Math.max(1, perUnit) + pc;
    }

    private String transferStockSearchText(JSONObject row) {
        return (safe(first(row, "kode_barang", "KodeBarang"))
                + " " + safe(first(row, "nama_barang", "NamaBarang"))
                + " " + safe(transferStockRack(row))
                + " " + safe(first(row, "batch")))
                .toUpperCase(Locale.US);
    }

    private void processTransferTargetItem(String payload) {
        String targetRack = QrUtils.extractLocationCode(payload);
        if (targetRack.isEmpty()) {
            String cleanPayload = safe(payload).trim();
            if (isRackCode(cleanPayload)) {
                targetRack = cleanPayload;
            }
        }
        if (targetRack.isEmpty()) {
            targetRack = extractSemicolonPart(payload, 1);
        }
        if (targetRack.isEmpty()) {
            toast("QR target tidak memuat rak tujuan.");
            showTransferStocks();
            return;
        }
        transferSourceRack = "";
        transferQuery = "";
        pendingTransferTargetRack = targetRack;
        transferTargetPanelOpen = false;
        transferFilterOpen = false;
        loadTransferStocksForTarget("", targetRack);
        toast("Rak tujuan " + targetRack + " diset. Menampilkan stok titipan dengan produk yang sesuai.");
    }

    private void processTransferVerifyItem(String payload) {
        if (selectedTransferStock == null) {
            toast("Pilih stok titipan dulu");
            showTransferStocks();
            return;
        }
        String scannedPalletCode = extractSemicolonPart(payload, 2);
        if (!scannedPalletCode.startsWith("PLT-")) {
            String detectedPalletCode = QrUtils.extractPalletCode(payload);
            scannedPalletCode = detectedPalletCode != null && detectedPalletCode.toUpperCase(Locale.US).startsWith("PLT-")
                    ? detectedPalletCode.toUpperCase(Locale.US)
                    : "";
        }
        String rawPayload = safe(payload).trim();
        boolean palletOnlyScan = !scannedPalletCode.isEmpty()
                && (rawPayload.equalsIgnoreCase(scannedPalletCode)
                || rawPayload.toUpperCase(Locale.US).startsWith("BUDIMAS-WMS|PALLET|"));

        String scannedCode = palletOnlyScan ? "" : extractSemicolonPart(payload, 0);
        String expectedCode = first(selectedTransferStock, "kode_barang", "KodeBarang");
        if (scannedCode.isEmpty() && scannedPalletCode.isEmpty()) {
            scannedCode = QrUtils.extractPalletCode(payload);
        }
        if (!expectedCode.isEmpty() && !scannedCode.isEmpty() && !expectedCode.equalsIgnoreCase(scannedCode)) {
            toast("Barang scan " + scannedCode + " berbeda dari stok " + expectedCode);
            renderTransferStocks(rowsAsResponse(selectedTransferStock).optJSONArray("items"), false, "");
            return;
        }
        String scannedRack = palletOnlyScan ? "" : QrUtils.extractLocationCode(payload);
        if (scannedRack.isEmpty()) {
            scannedRack = extractSemicolonPart(payload, 1);
        }
        String expectedRack = first(selectedTransferStock, "kode_rak_asal", "KodeRakAsal");
        if (!expectedRack.isEmpty() && !scannedRack.isEmpty() && !expectedRack.equalsIgnoreCase(scannedRack)) {
            toast("Rak asal scan " + scannedRack + " berbeda dari stok " + expectedRack);
            renderTransferStocks(rowsAsResponse(selectedTransferStock).optJSONArray("items"), false, "");
            return;
        }
        if (!scannedRack.isEmpty()) {
            transferSourceRack = scannedRack;
        }

        // Pallet QR WMS uses product;rak;pallet;... while the compact pallet QR
        // uses the PLT- code directly. Keep the scanned identity for the API so
        // it never has to guess between multiple pallets in one rack/product lot.
        if (!scannedPalletCode.isEmpty()) {
            try {
                selectedTransferStock.put("pallet_code", scannedPalletCode);
            } catch (Exception ignored) {
            }
        }
        int qty = pendingTransferQty > 0 ? pendingTransferQty : transferStockQty(selectedTransferStock);
        if (pendingTransferTargetRack.isEmpty()) {
            toast("Rak tujuan belum ada. Scan ulang target transfer.");
            showTransferStocks();
            return;
        }
        verifyTransferTarget(selectedTransferStock, pendingTransferTargetRack, qty);
    }

    private void processTransferFilterItem(String payload) {
        String productCode = extractSemicolonPart(payload, 0);
        String rackCode = QrUtils.extractLocationCode(payload);
        String cleanPayload = safe(payload).trim();
        if (rackCode.isEmpty()) {
            rackCode = extractSemicolonPart(payload, 1);
        }
        if (productCode.isEmpty()) {
            productCode = QrUtils.extractPalletCode(payload);
        }
        if (isRackCode(cleanPayload)) {
            rackCode = cleanPayload;
        }
        if (!productCode.isEmpty() && isRackCode(productCode)) {
            rackCode = productCode;
            productCode = "";
        }
        if (productCode.isEmpty() && rackCode.isEmpty()) {
            toast("QR/barcode belum memuat kode barang atau rak titipan.");
            if (pendingTransferTargetRack.isEmpty()) {
                loadTransferStocks();
            } else {
                loadTransferStocksForTarget(transferQuery, pendingTransferTargetRack);
            }
            return;
        }
        if (!productCode.isEmpty()) {
            transferQuery = productCode;
        }
        if (!rackCode.isEmpty()) {
            transferSourceRack = rackCode;
        }
        if (pendingTransferTargetRack.isEmpty()) {
            loadTransferStocks();
        } else {
            loadTransferStocksForTarget(transferQuery, pendingTransferTargetRack);
        }
    }

    private void renderTransferStocks(JSONArray rows, boolean loading, String error) {
        currentScreen = "transfer_stocks";
        setShell("Transfer Rak", "Tujuan " + fallback(pendingTransferTargetRack, "-"), "warehouse", true);
        JSONArray displayRows = sortTransferRows(rows);

        LinearLayout search = card();
        String targetSummary = pendingTransferTargetRack.isEmpty() ? "Rak Tujuan" : "Rak Tujuan: " + pendingTransferTargetRack;
        search.addView(dropdownHeader(targetSummary, transferTargetPanelOpen, v -> {
            transferTargetPanelOpen = !transferTargetPanelOpen;
            renderTransferStocks(rows, loading, error);
        }));
        if (transferTargetPanelOpen) {
            if (!pendingTransferTargetRack.isEmpty()) {
                search.addView(largeKeyValue("Tujuan", pendingTransferTargetRack));
            }
            if (!transferSourceRack.isEmpty()) {
                search.addView(largeKeyValue("Rak Titipan", transferSourceRack));
            }
            search.addView(primaryButton("Scan Barang / Rak Titipan", v -> startScanner("TRANSFER_FILTER_ITEM")));
            search.addView(dropdownHeader("Filter Manual", transferFilterOpen, v -> {
                transferFilterOpen = !transferFilterOpen;
                renderTransferStocks(rows, loading, error);
            }));
            if (transferFilterOpen) {
                EditText query = input(transferQuery, "Cari barang", InputType.TYPE_CLASS_TEXT);
                EditText sourceRack = input(transferSourceRack, "Cari rak titipan", InputType.TYPE_CLASS_TEXT);
                search.addView(query);
                search.addView(sourceRack);
                search.addView(secondaryButton("Terapkan Filter", v -> {
                    transferQuery = value(query);
                    transferSourceRack = value(sourceRack);
                    if (pendingTransferTargetRack.isEmpty()) {
                        loadTransferStocks();
                    } else {
                        loadTransferStocksForTarget(transferQuery, pendingTransferTargetRack);
                    }
                }));
            } else {
                search.addView(small("Filter manual disembunyikan agar layar lebih lega. Gunakan scan untuk alur utama."));
            }
            search.addView(space(6));
            search.addView(secondaryButton("Scan Ulang Rak Tujuan", v -> showTransferStocks()));
        } else {
            String helper = "Panel rak disembunyikan. Tap bar ini jika ingin scan ulang atau memakai filter manual.";
            if (!transferSourceRack.isEmpty()) {
                helper = "Filter rak titipan: " + transferSourceRack + ". Tap untuk ubah filter atau scan ulang.";
            } else if (!transferQuery.isEmpty()) {
                helper = "Filter barang: " + transferQuery + ". Tap untuk ubah filter atau scan ulang.";
            }
            search.addView(small(helper));
        }
        content.addView(search);

        LinearLayout list = card();
        list.addView(sectionTitle("Stok Transfer"));
        LinearLayout sortTabs = horizontal();
        sortTabs.addView(filterChip("FEFO", "FEFO".equalsIgnoreCase(transferSortMode), v -> {
            transferSortMode = "FEFO";
            renderTransferStocks(rows, loading, error);
        }));
        sortTabs.addView(filterChip("FIFO", "FIFO".equalsIgnoreCase(transferSortMode), v -> {
            transferSortMode = "FIFO";
            renderTransferStocks(rows, loading, error);
        }));
        sortTabs.addView(filterChip("Semua", "SEMUA".equalsIgnoreCase(transferSortMode), v -> {
            transferSortMode = "SEMUA";
            renderTransferStocks(rows, loading, error);
        }));
        list.addView(sortTabs);
        list.addView(small(transferSortDescription()));
        list.addView(space(8));
        if (loading) {
            list.addView(body("Memuat stok titipan..."));
        } else if (!error.isEmpty()) {
            TextView err = body(error);
            err.setTextColor(AMBER);
            list.addView(err);
            list.addView(primaryButton("Coba Lagi", v -> loadTransferStocks()));
        } else if (displayRows.length() == 0) {
            list.addView(body("Tidak ada stok transfer sesuai filter."));
        } else {
            int limit = Math.min(displayRows.length(), 80);
            list.addView(small(limit + " dari " + displayRows.length() + " stok titipan"));
            for (int i = 0; i < limit; i++) {
                JSONObject row = displayRows.optJSONObject(i);
                if (row != null) {
                    list.addView(transferStockRow(row));
                }
            }
        }
        content.addView(list);
    }

    private void showTransferForm(JSONObject stock) {
        selectedTransferStock = stock;
        pendingTransferQty = transferStockQty(stock);
        renderTransferForm(stock, null, true, "");
        executor.execute(() -> {
            try {
                JSONObject response = apiClient.getWmsRacks("", "Tetap", "");
                JSONArray rows = normalizeRows(response);
                uiHandler.post(() -> renderTransferForm(stock, rows, false, ""));
            } catch (Exception e) {
                uiHandler.post(() -> renderTransferForm(stock, new JSONArray(), false, operatorErrorMessage(e, "Rak tetap tujuan belum bisa dimuat.")));
            }
        });
    }

    private void renderTransferForm(JSONObject stock, JSONArray targetRacks, boolean loading, String error) {
        currentScreen = "transfer_form";
        selectedTransferStock = stock;
        setShell("Transfer Rak", "Rak Tetap tujuan, Lorong jika penuh", "warehouse", true);

        int availableQty = transferStockQty(stock);
        int perUnit = transferStockPerUnit(stock);
        int availableCt = transferStockQtyCt(stock);
        int availablePc = transferStockQtyPc(stock);

        content.addView(transferDetailCard(stock, "Stok Asal", 0, ""));

        LinearLayout form = card();
        form.addView(sectionTitle("Rak Tujuan"));
        form.addView(body("Rak tujuan utama harus Rak Tetap. Jika kapasitas Rak Tetap penuh, API WMS akan memakai Rak Lorong dengan kolom yang sama."));
        EditText targetRack = input("", "Input kode rak tujuan Tetap", InputType.TYPE_CLASS_TEXT);
        EditText qtyCt = input(String.valueOf(Math.max(availableCt, 0)), "Qty pindah CT", InputType.TYPE_CLASS_NUMBER);
        EditText qtyPc = input(String.valueOf(Math.max(availablePc, 0)), "Qty pindah PC", InputType.TYPE_CLASS_NUMBER);
        form.addView(targetRack);
        form.addView(qtyCt);
        form.addView(qtyPc);
        form.addView(primaryButton("Cek & Lanjut Transfer", v -> {
            String target = QrUtils.extractLocationCode(value(targetRack));
            if (target.isEmpty() && isRackCode(value(targetRack))) {
                target = value(targetRack).trim();
            }
            int transferQty = transferQtyFromInputs(qtyCt, qtyPc, perUnit);
            if (target.isEmpty()) {
                toast("Rak tujuan wajib diisi");
                return;
            }
            if (!isValidTransferQty(transferQty, availableQty)) {
                return;
            }
            verifyTransferTarget(stock, target, transferQty);
        }));
        form.addView(space(6));
        form.addView(secondaryButton("Scan QR Rak Tujuan", v -> {
            int transferQty = transferQtyFromInputs(qtyCt, qtyPc, perUnit);
            if (!isValidTransferQty(transferQty, availableQty)) {
                return;
            }
            pendingTransferQty = transferQty;
            selectedTransferStock = stock;
            startScanner("TRANSFER_TARGET_RACK");
        }));
        content.addView(form);

        LinearLayout targets = card();
        targets.addView(sectionTitle("Rekomendasi Rak Tetap"));
        if (loading) {
            targets.addView(body("Memuat rak tetap dari database..."));
        } else if (!error.isEmpty()) {
            TextView err = body(error);
            err.setTextColor(AMBER);
            targets.addView(err);
            targets.addView(secondaryButton("Coba Lagi", v -> showTransferForm(stock)));
        } else if (targetRacks == null || targetRacks.length() == 0) {
            targets.addView(body("Rak tetap tujuan belum tersedia."));
        } else {
            int limit = Math.min(targetRacks.length(), 10);
            targets.addView(body("Pilih cepat Rak Tetap aktif. Jika penuh, sistem memakai Rak Lorong pada kolom yang sama."));
            for (int i = 0; i < limit; i++) {
                JSONObject rack = targetRacks.optJSONObject(i);
                if (rack != null) {
                    String rackCode = first(rack, "kode_rak", "KodeRak");
                    targets.addView(rackLocationRow(rack, v -> {
                        int transferQty = transferQtyFromInputs(qtyCt, qtyPc, perUnit);
                        if (isValidTransferQty(transferQty, availableQty)) {
                            String problem = rackValidationProblem(rack, rackCode, "Tetap");
                            if (!problem.isEmpty()) {
                                toast(problem);
                                return;
                            }
                            showTransferConfirm(stock, rackCode, transferQty);
                        }
                    }));
                }
            }
        }
        content.addView(targets);
    }

    private boolean isValidTransferQty(int transferQty, int availableQty) {
        if (availableQty <= 0) {
            toast("Stok asal tidak tersedia untuk ditransfer.");
            return false;
        }
        if (transferQty <= 0) {
            toast("Qty transfer wajib lebih dari 0");
            return false;
        }
        if (transferQty > availableQty) {
            toast("Qty transfer melebihi saldo asal");
            return false;
        }
        return true;
    }

    private void verifyTransferTarget(JSONObject stock, String targetRack, int qty) {
        String stockProblem = transferStockValidationProblem(stock, qty);
        if (!stockProblem.isEmpty()) {
            toast(stockProblem);
            showTransferForm(stock);
            return;
        }
        String sourceRack = first(stock, "kode_rak_asal", "KodeRakAsal");
        if (targetRack.equalsIgnoreCase(sourceRack)) {
            toast("Rak tujuan tidak boleh sama dengan rak asal");
            return;
        }
        showLoadingScreen("Cek Rak Tujuan", "Mengecek " + targetRack + " sebagai rak Tetap...");
        executor.execute(() -> {
            try {
                JSONObject response = apiClient.getWmsRacks(targetRack, "Tetap", "");
                JSONArray rows = normalizeRows(response);
                JSONObject rack = findRackByCode(rows, targetRack);
                String problem = rackValidationProblem(rack, targetRack, "Tetap");
                if (problem.isEmpty()) {
                    uiHandler.post(() -> showTransferConfirm(stock, targetRack, qty));
                } else {
                    uiHandler.post(() -> {
                        toast(problem);
                        renderTransferForm(stock, rows, false, "");
                    });
                }
            } catch (Exception e) {
                uiHandler.post(() -> showApiError("Cek Rak Tujuan", "Rak tujuan belum bisa diverifikasi.", e, () -> showTransferForm(stock)));
            }
        });
    }

    private void showTransferConfirm(JSONObject stock, String targetRack, int qty) {
        String stockProblem = transferStockValidationProblem(stock, qty);
        if (!stockProblem.isEmpty()) {
            toast(stockProblem);
            showTransferForm(stock);
            return;
        }
        currentScreen = "transfer_confirm";
        selectedTransferStock = stock;
        pendingTransferQty = qty;
        setShell("Konfirmasi Transfer", "Pastikan barang, rak, dan qty sudah benar", "warehouse", true);

        content.addView(transferDetailCard(stock, "Detail Transfer", qty, targetRack));

        content.addView(primaryButton("Konfirmasi Transfer", v -> submitTransferStock(stock, targetRack, qty)));
        content.addView(space(8));
        content.addView(secondaryButton("Ganti Rak / Qty", v -> showTransferForm(stock)));
    }

    private void submitTransferStock(JSONObject stock, String targetRack, int qty) {
        String stockProblem = transferStockValidationProblem(stock, qty);
        if (!stockProblem.isEmpty()) {
            toast(stockProblem);
            showTransferForm(stock);
            return;
        }
        if (safe(targetRack).trim().isEmpty()) {
            toast("Rak tujuan wajib diisi sebelum transfer.");
            showTransferForm(stock);
            return;
        }
        String referenceNo = "TRM-" + new SimpleDateFormat("yyyyMMddHHmmss", Locale.US).format(new Date());
        showLoadingScreen("Transfer Rak", "Mengirim transfer ke database WMS...");
        executor.execute(() -> {
            try {
                JSONObject item = new JSONObject();
                item.put("wms_stock_rak_id", first(stock, "wms_stock_rak_id"));
                item.put("kode_barang", first(stock, "kode_barang", "KodeBarang"));
                item.put("nama_barang", first(stock, "nama_barang", "NamaBarang"));
                item.put("kode_rak_asal", first(stock, "kode_rak_asal", "KodeRakAsal"));
                item.put("qty", qty);
                item.put("batch", first(stock, "batch"));
                item.put("expired_date", first(stock, "expired_date"));
                putIfNotEmpty(item, "pallet_code", first(stock, "pallet_code", "kode_pallet", "KodePallet"));

                JSONArray items = new JSONArray();
                items.put(item);

                JSONObject payload = new JSONObject();
                payload.put("target_rak", targetRack);
                payload.put("reference_no", referenceNo);
                putIfNotEmpty(payload, "user_id", currentUserId());
                payload.put("items", items);

                JSONObject result = apiClient.confirmWmsTransfer(payload);
                uiHandler.post(() -> {
                    recordLocalTransaction("Transfer Rak", referenceNo, "", first(stock, "kode_barang", "KodeBarang"), first(stock, "nama_barang", "NamaBarang"), first(stock, "kode_rak_asal", "KodeRakAsal"), targetRack, qty, "PCS", "Stok dipindah dari rak non Tetap ke Rak Tetap; jika penuh diarahkan ke Rak Lorong kolom sama.");
                    showTransferSuccess(result);
                });
            } catch (Exception e) {
                uiHandler.post(() -> showApiError("Transfer Rak", "Transfer rak belum berhasil.", e, () -> showTransferForm(stock)));
            }
        });
    }

    private void showTransferSuccess(JSONObject result) {
        currentScreen = "transfer_success";
        boolean partial = "partial".equalsIgnoreCase(first(result, "status"));
        setShell(partial ? "Transfer Sebagian" : "Transfer Berhasil",
                partial ? "Sebagian item memerlukan tindak lanjut" : "Stok sudah update ke rak tujuan",
                "warehouse", true);

        LinearLayout success = card();
        success.setGravity(Gravity.CENTER_HORIZONTAL);
        success.setBackground(successBackground());
        TextView check = title("OK", 36);
        check.setTextColor(GREEN);
        success.addView(check);
        success.addView(title(partial ? "Transfer Sebagian Berhasil" : "Transfer Berhasil", 20));
        success.addView(body(fallback(first(result, "message", "msg"), "Transfer rak berhasil dikonfirmasi.")));
        success.addView(keyValue("Referensi", fallback(first(result, "reference_no"), "-")));
        success.addView(keyValue("Total Item", fallback(first(result, "total_item"), "1")));
        content.addView(success);

        content.addView(primaryButton("Refresh Transfer Rak", v -> loadTransferStocks()));
        content.addView(space(8));
        content.addView(secondaryButton("Lihat History", v -> {
            transactionTypeFilter = "TR";
            showTransactions();
        }));
    }

    private void showPutawayConfirm(Pallet pallet, String location) {
        currentScreen = "putaway_confirm";
        selectedPallet = pallet;
        setShell("Konfirmasi Putaway", "Pastikan pallet dan lokasi sudah benar", "putaway", true);
        content.addView(stepper(3));

        LinearLayout confirm = card();
        confirm.addView(sectionTitle("Pallet"));
        confirm.addView(keyValue("Kode Pallet", pallet.code));
        confirm.addView(keyValue("SKU / Barang", safe(pallet.skuName)));
        confirm.addView(keyValue("Isi", pallet.cartonCount + " Karton"));
        confirm.addView(keyValue("Lot / Exp", safe(pallet.lotBatch) + " / " + safe(pallet.expDate)));
        confirm.addView(space(8));
        confirm.addView(sectionTitle("Lokasi"));
        confirm.addView(keyValue("Kode Rak", location));
        content.addView(confirm);

        content.addView(primaryButton("Konfirmasi Putaway", v -> {
            if (Pallet.STATUS_STORED.equals(pallet.status)) {
                toast("Pallet sudah tersimpan. Gunakan Transfer Rak untuk perpindahan antar rak.");
                return;
            }
            if (safe(pallet.skuName).isEmpty()) {
                toast("Nama barang pallet belum jelas. Cek data receiving sebelum putaway.");
                return;
            }
            if (pallet.cartonCount <= 0) {
                toast("Qty pallet harus lebih dari 0 sebelum putaway.");
                return;
            }
            if (safe(location).trim().isEmpty()) {
                toast("Rak tujuan putaway wajib diisi.");
                return;
            }
            pallet.location = location;
            pallet.status = Pallet.STATUS_STORED;
            pallet.storedAt = System.currentTimeMillis();
            showPutawaySuccess(pallet);
        }));
        content.addView(space(8));
        content.addView(secondaryButton("Ganti Lokasi", v -> showScanLocation(pallet)));
    }

    private void showPutawaySuccess(Pallet pallet) {
        currentScreen = "putaway_success";
        setShell("Putaway Berhasil", "Pallet sudah masuk lokasi gudang", "putaway", true);

        LinearLayout success = card();
        success.setGravity(Gravity.CENTER_HORIZONTAL);
        success.setBackground(successBackground());
        TextView check = title("OK", 36);
        check.setTextColor(GREEN);
        success.addView(check);
        success.addView(title("Putaway Berhasil", 20));
        success.addView(body(pallet.code + " tersimpan di " + pallet.location));
        content.addView(success);

        content.addView(primaryButton("Lanjut Pallet Berikutnya", v -> showPutawayScanPallet()));
        content.addView(space(8));
        content.addView(secondaryButton("Lihat Lokasi Rak", v -> showRackLocations()));
    }

    private void showWarehouseList() {
        currentScreen = "warehouse";
        setShell("Daftar Pallet di Gudang", "Cari pallet, lokasi, lot, atau SKU", "warehouse", true);

        List<Pallet> pallets = store.getPallets();
        LinearLayout search = card();
        search.addView(dropdownHeader(filterSummary("Filter Pallet", warehouseQuery, warehouseFilter), warehouseFilterOpen, v -> {
            warehouseFilterOpen = !warehouseFilterOpen;
            showWarehouseList();
        }));
        if (warehouseFilterOpen) {
            EditText query = input(warehouseQuery, "Cari pallet / SKU / lokasi", InputType.TYPE_CLASS_TEXT);
            search.addView(query);
            LinearLayout filters = horizontal();
            filters.setPadding(0, dp(6), 0, dp(10));
            filters.addView(filterChip(filterLabel("ALL", "Semua"), "ALL".equals(warehouseFilter), v -> {
                warehouseFilter = "ALL";
                warehouseQuery = value(query);
                showWarehouseList();
            }));
            filters.addView(filterChip(filterLabel("STORED", "Tersimpan"), "STORED".equals(warehouseFilter), v -> {
                warehouseFilter = "STORED";
                warehouseQuery = value(query);
                showWarehouseList();
            }));
            filters.addView(filterChip(filterLabel("WAITING", "Menunggu"), "WAITING".equals(warehouseFilter), v -> {
                warehouseFilter = "WAITING";
                warehouseQuery = value(query);
                showWarehouseList();
            }));
            search.addView(filters);
            search.addView(secondaryButton("Cari", v -> {
                warehouseQuery = value(query);
                showWarehouseList();
            }));
        } else {
            search.addView(small(activeFilterText(warehouseQuery, warehouseFilter, "Filter pallet disembunyikan. Tap untuk cari pallet, SKU, atau lokasi.")));
        }
        content.addView(search);

        LinearLayout list = card();
        list.addView(sectionTitle("Pallet"));
        int shown = 0;
        for (Pallet pallet : pallets) {
            if (matchesWarehouseFilter(pallet)) {
                list.addView(warehouseRow(pallet));
                shown++;
            }
        }
        if (shown == 0) {
            list.addView(body("Tidak ada pallet sesuai filter."));
        }
        content.addView(list);
    }

    private void showReprintQr() {
        currentScreen = "reprint_qr";
        setShell("Reprint QR", "Cetak ulang label pallet lokal", "settings", true);

        LinearLayout search = card();
        search.addView(secondaryButton("Scan QR Pallet", v -> startScanner("REPRINT_PALLET")));
        search.addView(dropdownHeader(filterSummary("Cari Label Pallet", reprintQuery, ""), reprintFilterOpen, v -> {
            reprintFilterOpen = !reprintFilterOpen;
            showReprintQr();
        }));
        if (reprintFilterOpen) {
            EditText query = input(reprintQuery, "Kode pallet / SKU / lot / lokasi", InputType.TYPE_CLASS_TEXT);
            search.addView(query);
            search.addView(primaryButton("Cari Label", v -> {
                reprintQuery = value(query);
                showReprintQr();
            }));
        } else {
            search.addView(small(activeFilterText(reprintQuery, "", "Pencarian manual disembunyikan. Tap jika ingin cari label lama.")));
        }
        content.addView(search);

        LinearLayout list = card();
        list.addView(sectionTitle("Label Pallet"));
        int shown = 0;
        for (Pallet pallet : store.getPallets()) {
            if (matchesReprintFilter(pallet)) {
                list.addView(palletQrRow(pallet));
                shown++;
            }
            if (shown >= 80) {
                break;
            }
        }
        if (shown == 0) {
            list.addView(body("Pallet tidak ditemukan. Cek kode pallet atau buat pallet dari Receiving dulu."));
        } else {
            list.addView(body("Gunakan tombol Cetak untuk reprint label pallet yang dipilih."));
        }
        content.addView(list);
    }

    private void showStockOpname() {
        showStockOpnameSchedules(null, true, "");
        loadStockOpnameSchedules();
    }

    private void showStockOpnameSchedules(JSONArray rows, boolean loading, String error) {
        currentScreen = "stock_opname";
        setShell("Stock Opname", "Jadwal dari ERP", "warehouse", true);
        content.addView(stockOpnameStepCard(1));

        LinearLayout info = card();
        info.addView(sectionTitle("Jadwal Pelaksanaan"));
        info.addView(body("Jadwal dibuat melalui ERP. Pilih jadwal untuk mulai menghitung stok per lokasi rak."));
        info.addView(keyValue("Cabang", fallback(currentBranchName(), "Cabang login")));
        info.addView(keyValue("Petugas", currentUserName()));
        content.addView(info);

        LinearLayout list = card();
        list.addView(sectionTitle("Jadwal Aktif"));
        if (loading) {
            list.addView(body("Memuat jadwal stok opname dari ERP..."));
        } else if (!safe(error).isEmpty()) {
            TextView message = body(error);
            message.setTextColor(AMBER);
            list.addView(message);
            list.addView(primaryButton("Coba Lagi", v -> loadStockOpnameSchedules()));
        } else if (rows == null || rows.length() == 0) {
            list.addView(body("Belum ada jadwal stok opname untuk cabang ini. Buat jadwal melalui website ERP."));
            list.addView(secondaryButton("Muat Ulang", v -> loadStockOpnameSchedules()));
        } else {
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.optJSONObject(i);
                if (row != null) {
                    list.addView(stockOpnameScheduleRow(row));
                }
            }
        }
        content.addView(list);
    }

    private void loadStockOpnameSchedules() {
        executor.execute(() -> {
            try {
                JSONObject response = apiClient.getWmsStockOpnameSchedules(currentBranchId());
                JSONArray rows = normalizeRows(response);
                uiHandler.post(() -> showStockOpnameSchedules(rows, false, ""));
            } catch (Exception e) {
                uiHandler.post(() -> showStockOpnameSchedules(null, false,
                        operatorErrorMessage(e, "Jadwal stok opname belum bisa dimuat dari ERP.")));
            }
        });
    }

    private LinearLayout stockOpnameScheduleRow(JSONObject row) {
        int scheduleId = row.optInt("id", row.optInt("id_stock_opname", 0));
        String code = fallback(first(row, "kode_so", "code"), "SO #" + scheduleId);
        String principal = fallback(first(row, "nama_principal", "principal"), "Principal");
        String executionDate = readableDate(first(row, "tanggal_pelaksanaan", "tanggal_so"));
        String status = fallback(first(row, "status_so", "status"), "scheduled");
        int totalRack = row.optInt("total_rak", 0);
        int finishedRack = row.optInt("selesai_rak", 0);

        LinearLayout panel = vertical();
        panel.setPadding(dp(16), dp(14), dp(16), dp(14));
        panel.setBackground(round(SUBTLE, SUBTLE_BORDER, 8));
        LinearLayout.LayoutParams panelParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        panelParams.setMargins(0, dp(6), 0, dp(6));
        panel.setLayoutParams(panelParams);
        panel.addView(title(code, 18));
        panel.addView(body(principal));
        panel.addView(small("Pelaksanaan: " + fallback(executionDate, "-") + " | Status: " + status.replace('_', ' ')));
        if (totalRack > 0) {
            panel.addView(small("Progress rak: " + finishedRack + " / " + totalRack));
        }
        panel.addView(primaryButton("in_progress".equalsIgnoreCase(status) ? "Lanjut Hitung" : "Mulai Stok Opname", v -> {
            stockOpnameScheduleId = scheduleId;
            stockOpnameSoNo = code;
            stockOpnamePrinciple = principal;
            stockOpnamePic = currentUserName();
            stockOpnameScheduleStatus = status;
            stockOpnameExecutionDate = first(row, "tanggal_pelaksanaan", "tanggal_so");
            stockOpnameLocationStatus.clear();
            stockOpnameLocationTitipan.clear();
            stockOpnameScannedItems = 0;
            if ("in_progress".equalsIgnoreCase(status)) {
                loadStockOpnameLocations();
            } else {
                startStockOpnameSchedule();
            }
        }));
        return panel;
    }

    private void startStockOpnameSchedule() {
        if (stockOpnameScheduleId <= 0) {
            toast("Jadwal stok opname belum dipilih.");
            return;
        }
        showLoadingScreen("Stock Opname", "Membuat snapshot rak dan mengunci transaksi barang keluar...");
        executor.execute(() -> {
            try {
                JSONObject payload = new JSONObject();
                putIfNotEmpty(payload, "user_id", currentUserId());
                apiClient.startWmsStockOpname(stockOpnameScheduleId, payload);
                stockOpnameScheduleStatus = "in_progress";
                uiHandler.post(() -> {
                    toast("Stok opname dimulai. Barang keluar dikunci.");
                    loadStockOpnameLocations();
                });
            } catch (Exception e) {
                uiHandler.post(() -> showApiError("Stock Opname", "Jadwal belum dapat dimulai.", e, this::showStockOpname));
            }
        });
    }

    private void showStockOpnameLocations(JSONArray rackRows, boolean loading, String error) {
        currentScreen = "stock_opname_locations";
        setShell("SO per Principle", fallback(stockOpnamePrinciple, "Principle"), "warehouse", true);
        content.addView(stockOpnameStepCard(2));

        LinearLayout header = card();
        header.addView(sectionTitle("Detail SO"));
        header.addView(keyValue("Jadwal SO", fallback(stockOpnameSoNo, "-")));
        header.addView(keyValue("Principle", fallback(stockOpnamePrinciple, "-")));
        header.addView(keyValue("PIC", fallback(stockOpnamePic, "-")));
        header.addView(keyValue("Pelaksanaan", readableDate(stockOpnameExecutionDate)));
        header.addView(keyValue("Status", "Sedang berjalan - barang keluar terkunci"));
        content.addView(header);

        LinearLayout locations = card();
        locations.addView(sectionTitle("Pilih Lokasi Rak"));
        locations.addView(body("Pilih lokasi rak untuk mulai scan. Lokasi rak yang belum discan akan terlihat jelas."));
        if (loading) {
            locations.addView(body("Memuat lokasi rak dari API..."));
        } else if (!safe(error).isEmpty()) {
            TextView err = body(error);
            err.setTextColor(AMBER);
            locations.addView(err);
            locations.addView(primaryButton("Coba Lagi", v -> loadStockOpnameLocations()));
        } else {
            if (rackRows != null) mergeStockOpnameLocations(rackRows);
            addStockOpnameLocationSection(locations, "Rak Tetap & Lorong", false);
            addStockOpnameLocationSection(locations, "Rak Titipan", true);
        }
        content.addView(locations);

        LinearLayout summary = card();
        summary.addView(sectionTitle("Ringkasan SO"));
        summary.addView(body("Stok sistem tidak ditampilkan. Petugas hanya menghitung fisik barang untuk menjaga akurasi."));
        summary.addView(keyValue("Lokasi Rak", String.valueOf(stockOpnameLocationStatus.size())));
        summary.addView(keyValue("Selesai", String.valueOf(countStockOpnameStatus("Selesai"))));
        summary.addView(keyValue("Belum Scan", String.valueOf(countStockOpnameStatus("Belum Scan"))));
        summary.addView(keyValue("Item Sudah Scan", String.valueOf(stockOpnameScannedItems)));
        summary.addView(primaryButton("Periksa & Finish", v -> showStockOpnameResult()));
        content.addView(summary);
    }

    private void loadStockOpnameLocations() {
        showStockOpnameLocations(null, true, "");
        executor.execute(() -> {
            try {
                JSONObject response = apiClient.getWmsStockOpnameRacks(stockOpnameScheduleId);
                JSONArray rows = normalizeRows(response);
                uiHandler.post(() -> showStockOpnameLocations(rows, false, ""));
            } catch (Exception e) {
                uiHandler.post(() -> showStockOpnameLocations(null, false, operatorErrorMessage(e, "Lokasi rak belum bisa dimuat dari API.")));
            }
        });
    }

    private void showStockOpnameScanLocation(String location) {
        stockOpnameLocation = location;
        currentScreen = "stock_opname_scan";
        setShell("Scan Barang", location, "warehouse", true);
        content.addView(stockOpnameStepCard(3));

        LinearLayout scan = card();
        scan.addView(sectionTitle("Scan Item"));
        scan.addView(keyValue("SO", fallback(stockOpnameSoNo, "-")));
        scan.addView(keyValue("Principle", fallback(stockOpnamePrinciple, "-")));
        scan.addView(keyValue("Lokasi Rak", fallback(location, "-")));
        scan.addView(body("Produk, batch, expired, dan saldo WMS pada rak ditampilkan di bawah. Scan barang untuk mulai menghitung."));
        scan.addView(primaryButton("Scan Item", v -> startScanner("STOCK_OPNAME_PALLET")));
        scan.addView(dropdownHeader("Input Manual Barcode", stockOpnameManualOpen, v -> {
            stockOpnameManualOpen = !stockOpnameManualOpen;
            showStockOpnameScanLocation(location);
        }));
        if (stockOpnameManualOpen) {
            EditText manual = input("", "Input payload QR / barcode", InputType.TYPE_CLASS_TEXT);
            scan.addView(manual);
            scan.addView(secondaryButton("Proses Manual", v -> {
                Pallet pallet = stockOpnamePalletFromScan(value(manual));
                if (pallet == null) {
                    toast("QR/barcode tidak terbaca untuk lokasi ini.");
                    return;
                }
                resolveStockOpnameLot(pallet);
            }));
        } else {
            scan.addView(small("Input manual disembunyikan. Gunakan hanya jika scan kamera gagal."));
        }
        content.addView(scan);

        LinearLayout rackContents = card();
        rackContents.addView(sectionTitle("Produk di Rak"));
        LinearLayout rackItems = vertical();
        rackItems.addView(body("Memuat produk dan saldo WMS..."));
        rackContents.addView(rackItems);
        content.addView(rackContents);
        loadStockOpnameRackContents(location, rackItems);

        LinearLayout progress = card();
        progress.addView(sectionTitle("Progress Lokasi"));
        progress.addView(keyValue("Status", fallback(stockOpnameLocationStatus.get(location), "Belum Scan")));
        progress.addView(keyValue("Item Sudah Scan", String.valueOf(stockOpnameScannedItems)));
        progress.addView(secondaryButton("Kembali ke Daftar Lokasi", v -> showStockOpnameLocations(null, false, "")));
        content.addView(progress);
    }

    private void loadStockOpnameRackContents(String location, LinearLayout rowsContainer) {
        executor.execute(() -> {
            try {
                JSONObject response = apiClient.getWmsStockOpnameRackItems(
                        stockOpnameScheduleId, location, "");
                JSONArray rows = normalizeRows(response);
                uiHandler.post(() -> {
                    rowsContainer.removeAllViews();
                    if (rows.length() == 0) {
                        rowsContainer.addView(body("Tidak ada saldo produk pada rak ini."));
                        return;
                    }
                    for (int i = 0; i < rows.length(); i++) {
                        JSONObject row = rows.optJSONObject(i);
                        if (row != null) {
                            rowsContainer.addView(stockOpnameRackItemRow(row));
                        }
                    }
                });
            } catch (Exception e) {
                uiHandler.post(() -> {
                    rowsContainer.removeAllViews();
                    TextView error = body(operatorErrorMessage(e, "Daftar produk rak belum bisa dimuat."));
                    error.setTextColor(AMBER);
                    rowsContainer.addView(error);
                    rowsContainer.addView(secondaryButton("Muat Ulang Produk Rak",
                            v -> loadStockOpnameRackContents(location, rowsContainer)));
                });
            }
        });
    }

    private LinearLayout stockOpnameRackItemRow(JSONObject item) {
        LinearLayout row = vertical();
        row.setPadding(0, dp(10), 0, dp(10));
        row.setBackground(separatorBackground());
        row.addView(title(fallback(first(item, "nama_barang", "product_name"), "Produk"), 16));
        row.addView(small("Kode: " + fallback(first(item, "kode_barang", "product_code"), "-")));
        row.addView(small("Batch " + fallback(first(item, "batch_number", "lot_batch"), "-")
            + " | Expired " + readableDate(fallback(first(item, "expired_date", "exp_date"), "-"))));
        row.addView(small("Saldo WMS: " + parseOptionalInt(item, 0, "qty_pcs") + " PCS"));
        return row;
    }

    private void showStockOpnameResult() {
        currentScreen = "stock_opname_result";
        setShell("Rekap SO", fallback(stockOpnameSoNo, "Stock Opname"), "warehouse", true);
        content.addView(stockOpnameStepCard(4));

        LinearLayout done = card();
        done.setBackground(successBackground());
        done.addView(title("Konfirmasi Finish", 20));
        done.addView(keyValue("SO", fallback(stockOpnameSoNo, "-")));
        done.addView(keyValue("Principle", fallback(stockOpnamePrinciple, "-")));
        done.addView(keyValue("PIC", fallback(stockOpnamePic, "-")));
        done.addView(keyValue("Lokasi Selesai", String.valueOf(countStockOpnameStatus("Selesai"))));
        done.addView(keyValue("Belum Scan", String.valueOf(countStockOpnameStatus("Belum Scan"))));
        done.addView(keyValue("Item Sudah Scan", String.valueOf(stockOpnameScannedItems)));
        done.addView(body("Finish hanya dapat diproses jika seluruh isi rak pada snapshot jadwal sudah dihitung."));
        content.addView(done);

        LinearLayout detail = card();
        detail.addView(sectionTitle("Status per Lokasi"));
        for (String code : stockOpnameLocationStatus.keySet()) {
            detail.addView(keyValue(code, fallback(stockOpnameLocationStatus.get(code), "-")));
        }
        content.addView(detail);
        content.addView(primaryButton("Finish & Kirim ke ERP", v -> finishStockOpnameSchedule()));
        content.addView(secondaryButton("Lanjut Scan Lokasi", v -> showStockOpnameLocations(null, false, "")));
    }

    /**
     * A bare SKU barcode is ambiguous when the same product occupies a rack
     * in multiple batches or expired dates. Resolve that ambiguity before the
     * count form is opened so one physical lot is never overwritten by another.
     */
    private void resolveStockOpnameLot(Pallet scannedPallet) {
        if (scannedPallet == null || safe(scannedPallet.code).isEmpty()) {
            toast("Kode barang hasil scan belum terbaca.");
            showStockOpnameScanLocation(stockOpnameLocation);
            return;
        }
        final String rackCode = fallback(scannedPallet.location, stockOpnameLocation);
        if (stockOpnameScheduleId <= 0 || safe(rackCode).isEmpty()) {
            toast("Pilih jadwal dan lokasi rak stok opname terlebih dahulu.");
            showStockOpnameLocations(null, false, "");
            return;
        }

        showLoadingScreen("Stock Opname", "Memeriksa batch dan expired pada rak...");
        executor.execute(() -> {
            try {
                JSONObject response = apiClient.getWmsStockOpnameRackItems(
                        stockOpnameScheduleId, rackCode, scannedPallet.code);
                JSONArray candidates = stockOpnameMatchingLotRows(normalizeRows(response), scannedPallet);
                uiHandler.post(() -> {
                    if (candidates.length() == 0) {
                        showApiError("Stock Opname",
                                "Produk atau batch/expired hasil scan tidak tersedia pada rak stok opname ini.",
                                new BudimasApiClient.ApiException(409,
                                        "Pilih barang yang benar pada rak " + rackCode + ", lalu scan ulang."),
                                () -> showStockOpnameScanLocation(rackCode));
                        return;
                    }
                    if (candidates.length() == 1) {
                        applyStockOpnameLot(scannedPallet, candidates.optJSONObject(0));
                        showStockOpnameForm(scannedPallet);
                        return;
                    }
                    showStockOpnameLotPicker(scannedPallet, candidates);
                });
            } catch (Exception e) {
                uiHandler.post(() -> showApiError("Stock Opname",
                        "Batch dan expired pada rak belum bisa dimuat.", e,
                        () -> resolveStockOpnameLot(scannedPallet)));
            }
        });
    }

    private JSONArray stockOpnameMatchingLotRows(JSONArray rows, Pallet scannedPallet) {
        JSONArray matches = new JSONArray();
        boolean scanHasBatch = stockOpnameHasLotValue(scannedPallet == null ? "" : scannedPallet.lotBatch);
        boolean scanHasExpired = stockOpnameHasLotValue(scannedPallet == null ? "" : scannedPallet.expDate);
        String expectedBatch = stockOpnameLotText(scannedPallet == null ? "" : scannedPallet.lotBatch);
        String expectedExpired = stockOpnameLotDate(scannedPallet == null ? "" : scannedPallet.expDate);

        for (int i = 0; rows != null && i < rows.length(); i++) {
            JSONObject row = rows.optJSONObject(i);
            if (row == null) {
                continue;
            }
            String rowBatch = stockOpnameLotText(first(row, "batch_number", "lot_batch"));
            String rowExpired = stockOpnameLotDate(first(row, "expired_date", "exp_date"));
            if (scanHasBatch && !expectedBatch.equalsIgnoreCase(rowBatch)) {
                continue;
            }
            if (scanHasExpired && !expectedExpired.equals(rowExpired)) {
                continue;
            }
            matches.put(row);
        }
        return matches;
    }

    private boolean stockOpnameHasLotValue(String value) {
        String clean = safe(value).trim();
        return !clean.isEmpty() && !"-".equals(clean) && !"null".equalsIgnoreCase(clean);
    }

    private String stockOpnameLotText(String value) {
        return stockOpnameHasLotValue(value) ? safe(value).trim() : "";
    }

    private String stockOpnameLotDate(String value) {
        String clean = stockOpnameLotText(value);
        if (clean.matches("^\\d{2}/\\d{2}/\\d{4}$")) {
            try {
                Date date = new SimpleDateFormat("dd/MM/yyyy", Locale.US).parse(clean);
                if (date != null) {
                    return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date);
                }
            } catch (ParseException ignored) {
                // The server remains authoritative and will reject an invalid
                // scanner date. Keep the raw value to avoid selecting a wrong lot.
            }
        }
        return clean.length() >= 10 && clean.matches("^\\d{4}-\\d{2}-\\d{2}.*")
                ? clean.substring(0, 10) : clean;
    }

    private void applyStockOpnameLot(Pallet pallet, JSONObject lot) {
        if (pallet == null || lot == null) {
            return;
        }
        pallet.code = fallback(first(lot, "kode_barang", "product_code"), pallet.code);
        pallet.skuName = fallback(first(lot, "nama_barang", "product_name"), pallet.skuName);
        String batch = stockOpnameLotText(first(lot, "batch_number", "lot_batch"));
        String expired = stockOpnameLotDate(first(lot, "expired_date", "exp_date"));
        pallet.lotBatch = batch.isEmpty() ? "-" : batch;
        pallet.expDate = expired.isEmpty() ? "-" : expired;
        pallet.stockOpnameQtyPcs = parseOptionalInt(lot, 0, "qty_pcs", "system_qty_pcs");
        pallet.location = fallback(pallet.location, stockOpnameLocation);
    }

    private void showStockOpnameLotPicker(Pallet pallet, JSONArray candidates) {
        currentScreen = "stock_opname_lot_picker";
        setShell("Pilih Batch & Expired", fallback(pallet.code, "Stock Opname"), "warehouse", true);
        content.addView(stockOpnameStepCard(3));

        LinearLayout explanation = card();
        explanation.addView(sectionTitle("Produk memiliki beberapa lot"));
        explanation.addView(body("Pilih Batch dan Expired yang sedang dihitung. Saldo WMS ditampilkan per lot."));
        explanation.addView(keyValue("Lokasi Rak", fallback(pallet.location, stockOpnameLocation)));
        content.addView(explanation);

        LinearLayout choices = card();
        choices.addView(sectionTitle("Pilihan Batch / Expired"));
        for (int i = 0; i < candidates.length(); i++) {
            final JSONObject lot = candidates.optJSONObject(i);
            if (lot == null) {
                continue;
            }
            String batch = stockOpnameLotText(first(lot, "batch_number", "lot_batch"));
            String expired = stockOpnameLotDate(first(lot, "expired_date", "exp_date"));
            String label = "Batch " + (batch.isEmpty() ? "-" : batch);
                String detail = "Expired " + (expired.isEmpty() ? "-" : readableDate(expired))
                    + " | Saldo WMS " + parseOptionalInt(lot, 0, "qty_pcs") + " PCS";
            TextView option = masterOptionRow(label, detail);
            option.setOnClickListener(v -> {
                applyStockOpnameLot(pallet, lot);
                showStockOpnameForm(pallet);
            });
            choices.addView(option);
        }
        choices.addView(secondaryButton("Scan Barang Lain", v -> showStockOpnameScanLocation(
                fallback(pallet.location, stockOpnameLocation))));
        content.addView(choices);
    }

    private void showStockOpnameForm(Pallet pallet) {
        showLoadingScreen("Stock Opname", "Memuat konfigurasi UOM produk...");
        executor.execute(() -> {
            try {
                JSONObject response = apiClient.getWmsStockOpnameProductUoms(pallet.code);
                JSONArray uoms = normalizeRows(response);
                uiHandler.post(() -> showStockOpnameForm(pallet, uoms, ""));
            } catch (Exception e) {
                // Never guess PCS when the configured master UOM is unavailable:
                // the server must remain the source of truth for conversions.
                uiHandler.post(() -> showApiError("Stock Opname",
                        "Konfigurasi UOM produk belum bisa dimuat. Coba lagi saat koneksi tersedia.",
                        e, () -> showStockOpnameForm(pallet)));
            }
        });
    }

    private void showStockOpnameForm(Pallet pallet, JSONArray uomRows, String warning) {
        selectedPallet = pallet;
        currentScreen = "stock_opname_form";
        setShell("Input Qty Hitung", pallet.code, "warehouse", true);
        content.addView(stockOpnameStepCard(3));

        LinearLayout detail = card();
        detail.addView(sectionTitle("Item"));
        detail.addView(keyValue("Kode Item", fallback(pallet.code, "-")));
        detail.addView(keyValue("Nama Item", fallback(pallet.skuName, "-")));
        detail.addView(keyValue("Lokasi Rak", fallback(pallet.location, stockOpnameLocation)));
        detail.addView(keyValue("Batch", fallback(pallet.lotBatch, "-")));
        detail.addView(keyValue("Expired", readableDate(fallback(pallet.expDate, "-"))));
        detail.addView(keyValue("Saldo WMS", pallet.stockOpnameQtyPcs + " PCS"));
        content.addView(detail);

        LinearLayout form = card();
        form.addView(sectionTitle("Hasil Hitung Fisik"));
        if (!safe(warning).isEmpty()) {
            TextView warningView = small(warning);
            warningView.setTextColor(AMBER);
            form.addView(warningView);
        }
        form.addView(body("Saldo WMS menjadi nilai awal GOOD. BAD dimulai dari 0; ubah qty jika hasil fisik berbeda."));

        final JSONArray configuredUoms = stockOpnameEffectiveUomRows(uomRows);
        final JSONObject baseUom = stockOpnameBaseUom(configuredUoms);
        final String baseLabel = stockOpnameUomLabel(baseUom);
        final ArrayList<Integer> defaultGoodCounts = stockOpnameDefaultGoodCounts(
            configuredUoms, pallet.stockOpnameQtyPcs);
        final ArrayList<EditText> goodInputs = new ArrayList<>();
        final ArrayList<EditText> badInputs = new ArrayList<>();

        for (int i = 0; i < configuredUoms.length(); i++) {
            JSONObject uom = configuredUoms.optJSONObject(i);
            if (uom == null) {
                continue;
            }
            String uomLabel = stockOpnameUomLabel(uom);
            double factor = stockOpnameUomFactor(uom);

            LinearLayout uomPanel = vertical();
            uomPanel.setPadding(dp(12), dp(10), dp(12), dp(10));
            uomPanel.setBackground(round(inputFill(), CARD_BORDER, 10));
            LinearLayout.LayoutParams uomPanelParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            uomPanelParams.setMargins(0, dp(6), 0, dp(6));
            uomPanel.setLayoutParams(uomPanelParams);
            uomPanel.addView(title(uomLabel, 17));
            if (factor != 1d) {
                uomPanel.addView(small("1 " + uomLabel + " = " + stockOpnameNumber(factor) + " " + baseLabel));
            }

            LinearLayout quantities = horizontal();
            LinearLayout goodColumn = vertical();
            LinearLayout badColumn = vertical();
            TextView goodLabel = small("GOOD (" + uomLabel + ")");
            goodLabel.setTypeface(Typeface.DEFAULT_BOLD);
            goodLabel.setTextColor(GREEN);
            TextView badLabel = small("BAD (" + uomLabel + ")");
            badLabel.setTypeface(Typeface.DEFAULT_BOLD);
            badLabel.setTextColor(AMBER);
            EditText good = input(String.valueOf(defaultGoodCounts.get(i)), "Jumlah", InputType.TYPE_CLASS_NUMBER);
            EditText bad = input("0", "Jumlah", InputType.TYPE_CLASS_NUMBER);
            good.setTextSize(15);
            bad.setTextSize(15);
            goodColumn.addView(goodLabel);
            goodColumn.addView(good);
            badColumn.addView(badLabel);
            badColumn.addView(bad);
            LinearLayout.LayoutParams goodParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            goodParams.setMargins(0, dp(4), dp(4), 0);
            LinearLayout.LayoutParams badParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            badParams.setMargins(dp(4), dp(4), 0, 0);
            quantities.addView(goodColumn, goodParams);
            quantities.addView(badColumn, badParams);
            uomPanel.addView(quantities);
            form.addView(uomPanel);
            goodInputs.add(good);
            badInputs.add(bad);
        }

        TextView physicalTotal = small("");
        physicalTotal.setTextColor(INK);
        physicalTotal.setTypeface(Typeface.DEFAULT_BOLD);
        physicalTotal.setPadding(0, dp(8), 0, dp(8));
        form.addView(physicalTotal);
        final Runnable refreshPhysicalTotal = () -> physicalTotal.setText(stockOpnamePhysicalTotalText(
                configuredUoms, goodInputs, badInputs, baseLabel));
        for (EditText field : goodInputs) {
            addStockOpnameTotalWatcher(field, refreshPhysicalTotal);
        }
        for (EditText field : badInputs) {
            addStockOpnameTotalWatcher(field, refreshPhysicalTotal);
        }
        refreshPhysicalTotal.run();

        EditText note = input("", "Catatan kondisi / selisih fisik", InputType.TYPE_CLASS_TEXT);
        form.addView(note);
        form.addView(primaryButton("Simpan & Lanjut Scan", v -> {
            if (!stockOpnameUomInputsValid(goodInputs) || !stockOpnameUomInputsValid(badInputs)) {
                toast("Qty GOOD/BAD harus berupa angka nol atau lebih.");
                return;
            }
            JSONArray uomCounts = stockOpnameUomCounts(configuredUoms, goodInputs, badInputs);
            double totalGood = stockOpnameUomInputTotal(configuredUoms, goodInputs);
            double totalBad = stockOpnameUomInputTotal(configuredUoms, badInputs);
            submitStockOpnameApi(pallet, uomCounts, baseUom, totalGood, totalBad, value(note));
        }));
        form.addView(secondaryButton("Kembali ke Daftar Lokasi", v -> showStockOpnameLocations(null, false, "")));
        content.addView(form);
    }

    private ArrayList<Integer> stockOpnameDefaultGoodCounts(JSONArray uoms, int totalPcs) {
        ArrayList<Integer> counts = new ArrayList<>();
        int count = uoms == null ? 0 : uoms.length();
        double[] factors = new double[count];
        for (int i = 0; i < count; i++) {
            factors[i] = stockOpnameUomFactor(uoms.optJSONObject(i));
        }
        int[] allocated = StockOpnameQuantityDefaults.distributeGoodQty(totalPcs, factors);
        for (int quantity : allocated) {
            counts.add(quantity);
        }
        return counts;
    }

    private JSONArray stockOpnameEffectiveUomRows(JSONArray uomRows) {
        JSONArray result = new JSONArray();
        if (uomRows != null) {
            for (int i = 0; i < uomRows.length(); i++) {
                JSONObject row = uomRows.optJSONObject(i);
                if (row != null) {
                    result.put(row);
                }
            }
        }
        if (result.length() > 0) {
            return result;
        }
        try {
            result.put(new JSONObject()
                    .put("id", JSONObject.NULL)
                    .put("nama", "PCS")
                    .put("faktor_konversi", 1));
        } catch (Exception ignored) {
        }
        return result;
    }

    private String stockOpnameUomLabel(JSONObject uom) {
        return fallback(first(uom, "nama", "uom_label", "nama_uom", "label", "uom", "name"), "PCS");
    }

    private int stockOpnameUomId(JSONObject uom) {
        return parseOptionalInt(uom, 0, "id", "uom_id", "id_uom");
    }

    private double stockOpnameUomFactor(JSONObject uom) {
        String raw = first(uom, "faktor_konversi", "uom_factor", "factor", "conversion_factor", "konversi");
        try {
            double factor = Double.parseDouble(raw.replace(',', '.'));
            return factor > 0d ? factor : 1d;
        } catch (Exception ignored) {
            return 1d;
        }
    }

    private JSONObject stockOpnameBaseUom(JSONArray uoms) {
        JSONObject base = null;
        double smallestFactor = Double.MAX_VALUE;
        if (uoms != null) {
            for (int i = 0; i < uoms.length(); i++) {
                JSONObject candidate = uoms.optJSONObject(i);
                if (candidate == null) {
                    continue;
                }
                double factor = stockOpnameUomFactor(candidate);
                if (base == null || factor < smallestFactor) {
                    base = candidate;
                    smallestFactor = factor;
                }
            }
        }
        return base;
    }

    private void addStockOpnameTotalWatcher(EditText field, Runnable updateTotal) {
        field.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { updateTotal.run(); }
            @Override public void afterTextChanged(Editable s) { }
        });
    }

    private boolean stockOpnameUomInputsValid(List<EditText> fields) {
        for (EditText field : fields) {
            String raw = value(field).replace(',', '.');
            if (raw.isEmpty()) {
                continue;
            }
            try {
                double number = Double.parseDouble(raw);
                if (Double.isNaN(number) || Double.isInfinite(number) || number < 0d
                        || Math.abs(number - Math.rint(number)) > 0.0000001d) {
                    return false;
                }
            } catch (Exception ignored) {
                return false;
            }
        }
        return true;
    }

    private double stockOpnameUomInputValue(EditText field) {
        try {
            String raw = value(field).replace(',', '.');
            if (raw.isEmpty()) {
                return 0d;
            }
            double number = Double.parseDouble(raw);
            return Double.isNaN(number) || Double.isInfinite(number) || number < 0d ? 0d : number;
        } catch (Exception ignored) {
            return 0d;
        }
    }

    private double stockOpnameUomInputTotal(JSONArray uoms, List<EditText> inputs) {
        double total = 0d;
        int inputCount = inputs == null ? 0 : inputs.size();
        int uomCount = uoms == null ? 0 : uoms.length();
        int count = Math.min(inputCount, uomCount);
        for (int i = 0; i < count; i++) {
            total += stockOpnameUomInputValue(inputs.get(i)) * stockOpnameUomFactor(uoms.optJSONObject(i));
        }
        return total;
    }

    private JSONArray stockOpnameUomCounts(JSONArray uoms, List<EditText> goodInputs, List<EditText> badInputs) {
        JSONArray counts = new JSONArray();
        int uomCount = uoms == null ? 0 : uoms.length();
        for (int i = 0; i < uomCount; i++) {
            JSONObject uom = uoms.optJSONObject(i);
            if (uom == null) {
                continue;
            }
            double good = i < goodInputs.size() ? stockOpnameUomInputValue(goodInputs.get(i)) : 0d;
            double bad = i < badInputs.size() ? stockOpnameUomInputValue(badInputs.get(i)) : 0d;
            try {
                JSONObject count = new JSONObject();
                int uomId = stockOpnameUomId(uom);
                if (uomId > 0) {
                    count.put("uom_id", uomId);
                }
                count.put("uom_label", stockOpnameUomLabel(uom));
                count.put("faktor_konversi", stockOpnameUomFactor(uom));
                count.put("qty_good", good);
                count.put("qty_bad", bad);
                count.put("qty_total", good + bad);
                counts.put(count);
            } catch (Exception ignored) {
            }
        }
        return counts;
    }

    private String stockOpnamePhysicalTotalText(JSONArray uoms, List<EditText> goodInputs,
                                                  List<EditText> badInputs, String baseLabel) {
        double good = stockOpnameUomInputTotal(uoms, goodInputs);
        double bad = stockOpnameUomInputTotal(uoms, badInputs);
        return "Total fisik: " + stockOpnameNumber(good + bad) + " " + baseLabel
                + "  |  GOOD " + stockOpnameNumber(good) + " " + baseLabel
                + "  |  BAD " + stockOpnameNumber(bad) + " " + baseLabel;
    }

    private String stockOpnameNumber(double value) {
        double normalized = Math.abs(value) < 0.0000001d ? 0d : value;
        if (Math.abs(normalized - Math.rint(normalized)) < 0.0000001d) {
            return String.valueOf((long) Math.rint(normalized));
        }
        String text = String.format(Locale.US, "%.4f", normalized);
        text = text.replaceAll("0+$", "");
        return text.endsWith(".") ? text.substring(0, text.length() - 1) : text;
    }

    private LinearLayout stockOpnameStepCard(int activeStep) {
        LinearLayout steps = card();
        steps.addView(sectionTitle("Alur Stok Opname Gudang"));
        steps.addView(stockOpnameStepRow(1, "Pilih Jadwal ERP", activeStep));
        steps.addView(stockOpnameStepRow(2, "Start & Pilih Rak", activeStep));
        steps.addView(stockOpnameStepRow(3, "Hitung Fisik per UOM", activeStep));
        steps.addView(stockOpnameStepRow(4, "Finish & Review ERP", activeStep));
        return steps;
    }

    private TextView stockOpnameStepRow(int number, String label, int activeStep) {
        String marker = number < activeStep ? "OK" : String.valueOf(number);
        TextView row = body(marker + ". " + label + (number == activeStep ? " - Aktif" : ""));
        row.setTextColor(number == activeStep ? BLUE : number < activeStep ? GREEN : MUTED);
        row.setTypeface(Typeface.DEFAULT_BOLD);
        return row;
    }

    private void mergeStockOpnameLocations(JSONArray rows) {
        stockOpnameRackRows = rows == null ? new JSONArray() : rows;
        stockOpnameLocationStatus.clear();
        stockOpnameLocationTitipan.clear();
        stockOpnameScannedItems = 0;
        if (rows == null) return;
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.optJSONObject(i);
            if (row == null) continue;
            String code = first(row, "kode_rak", "KodeRak", "location");
            if (code.isEmpty()) continue;
            int itemCount = row.optInt("item_count", 0);
            int countedItems = row.optInt("counted_items", 0);
            String apiStatus = first(row, "status");
            String status = countedItems >= itemCount && itemCount > 0
                    ? "Selesai"
                    : countedItems > 0 || "COUNTING".equalsIgnoreCase(apiStatus) ? "Menghitung" : "Belum Scan";
            stockOpnameLocationStatus.put(code, status);
            stockOpnameScannedItems += countedItems;
            String type = first(row, "type_rak", "shelf_type", "TipeRak");
            stockOpnameLocationTitipan.put(code, "titipan".equalsIgnoreCase(type));
        }
    }

    private void addStockOpnameLocationSection(LinearLayout parent, String titleText, boolean titipan) {
        parent.addView(space(8));
        parent.addView(sectionTitle(titleText));
        int shown = 0;
        for (String code : stockOpnameLocationStatus.keySet()) {
            boolean isTitipan = Boolean.TRUE.equals(stockOpnameLocationTitipan.get(code));
            if (isTitipan != titipan) {
                continue;
            }
            parent.addView(stockOpnameLocationButton(code));
            shown++;
        }
        if (shown == 0) {
            parent.addView(body("Lokasi " + titleText + " belum tersedia dari API."));
        }
    }

    private LinearLayout stockOpnameLocationButton(String code) {
        String status = fallback(stockOpnameLocationStatus.get(code), "Belum Scan");
        int color = "Selesai".equalsIgnoreCase(status) ? GREEN : "Ada Selisih".equalsIgnoreCase(status) ? Color.rgb(220, 53, 69) : AMBER;
        JSONObject rack = findStockOpnameRack(code);
        String type = rack == null ? (Boolean.TRUE.equals(stockOpnameLocationTitipan.get(code)) ? "Titipan" : "Tetap / Lorong")
                : fallback(first(rack, "type_rak", "shelf_type"), "Rak");
        int itemCount = rack == null ? 0 : rack.optInt("item_count", 0);
        int countedItems = rack == null ? 0 : rack.optInt("counted_items", 0);
        LinearLayout panel = vertical();
        panel.setPadding(dp(16), dp(16), dp(16), dp(16));
        panel.setBackground(round(SUBTLE, color, 8));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(6), 0, dp(6));
        panel.setLayoutParams(params);
        TextView codeView = title(code, 21);
        codeView.setTextColor(INK);
        panel.addView(codeView);
        TextView statusView = body(type + " | " + status);
        statusView.setTextColor(color);
        statusView.setTypeface(Typeface.DEFAULT_BOLD);
        panel.addView(statusView);
        panel.addView(small("Produk dihitung: " + countedItems + " / " + itemCount));
        panel.setOnClickListener(v -> showStockOpnameScanLocation(code));
        return panel;
    }

    private JSONObject findStockOpnameRack(String code) {
        for (int i = 0; i < stockOpnameRackRows.length(); i++) {
            JSONObject row = stockOpnameRackRows.optJSONObject(i);
            if (row != null && code.equalsIgnoreCase(first(row, "kode_rak", "location"))) return row;
        }
        return null;
    }

    private void finishStockOpnameSchedule() {
        showLoadingScreen("Stock Opname", "Memvalidasi seluruh rak dan mengirim hasil ke ERP...");
        executor.execute(() -> {
            try {
                JSONObject payload = new JSONObject();
                putIfNotEmpty(payload, "user_id", currentUserId());
                apiClient.finishWmsStockOpname(stockOpnameScheduleId, payload);
                uiHandler.post(() -> {
                    toast("Stok opname selesai dan menunggu review ERP.");
                    stockOpnameScheduleId = 0;
                    stockOpnameLocationStatus.clear();
                    stockOpnameLocationTitipan.clear();
                    showStockOpname();
                });
            } catch (Exception e) {
                uiHandler.post(() -> showApiError("Finish Stock Opname", "Stok opname belum dapat diselesaikan.", e, this::showStockOpnameResult));
            }
        });
    }

    private int countStockOpnameStatus(String status) {
        int total = 0;
        for (String value : stockOpnameLocationStatus.values()) {
            if (status.equalsIgnoreCase(value)) {
                total++;
            }
        }
        return total;
    }

    private Pallet stockOpnamePalletFromScan(String payload) {
        Pallet pallet = palletFromPickingQr(payload);
        if (pallet == null && !safe(stockOpnameLocation).isEmpty()) {
            String productCode = extractProductCodeFromPayload(payload);
            if (!productCode.isEmpty()) {
                pallet = new Pallet();
                pallet.code = productCode;
                pallet.skuName = productCode;
                pallet.lotBatch = "-";
                pallet.expDate = "-";
                pallet.cartonCount = 0;
                pallet.location = stockOpnameLocation;
                pallet.status = Pallet.STATUS_STORED;
                pallet.storedAt = System.currentTimeMillis();
            }
        }
        if (pallet == null) {
            return null;
        }
        if (!safe(stockOpnameLocation).isEmpty()) {
            pallet.location = stockOpnameLocation;
        }
        pallet.incomingId = fallback(stockOpnameSoNo, "SO-STOCK-OPNAME");
        return pallet;
    }

    private String extractProductCodeFromPayload(String payload) {
        if (payload == null) {
            return "";
        }
        Matcher productMatcher = WMS_PRODUCT_PATTERN.matcher(payload.trim());
        while (productMatcher.find()) {
            String candidate = productMatcher.group().toUpperCase(Locale.US);
            if (!isRackCode(candidate)
                    && !"BUDIMAS-WMS".equals(candidate)
                    && !"LOCATION".equals(candidate)
                    && !"PALLET".equals(candidate)) {
                return candidate;
            }
        }
        return "";
    }

    private void showPickingSalesOrder() {
        currentScreen = "picking_so";
        setShell("Picking Sales Order", "Ambil pallet berdasarkan SO", "putaway", true);

        LinearLayout form = card();
        form.addView(sectionTitle("Sales Order"));
        EditText so = input(stageTwoQuery, "No Sales Order / Faktur", InputType.TYPE_CLASS_TEXT);
        form.addView(so);
        form.addView(primaryButton("Lanjut Pilih Pallet", v -> {
            stageTwoQuery = value(so);
            showPickingSalesOrderList();
        }));
        content.addView(form);
    }

    private void showPickingSalesOrderList() {
        currentScreen = "picking_so_list";
        setShell("Picking SO", fallback(stageTwoQuery, "Sales Order"), "putaway", true);

        LinearLayout action = card();
        action.addView(sectionTitle("Scan Picking"));
        action.addView(body("Scan QR barang/rak dari label gudang, lalu konfirmasi qty yang diambil untuk Sales Order."));
        action.addView(primaryButton("Scan QR Barang / Rak", v -> startScanner("SO_PICK_PALLET")));
        content.addView(action);
    }

    private void showPickingSalesOrderForm(Pallet pallet) {
        selectedPallet = pallet;
        currentScreen = "picking_so_form";
        setShell("Konfirmasi Picking", fallback(stageTwoQuery, "Sales Order"), "putaway", true);
        content.addView(palletDetailCard(pallet, "Pallet Dipilih"));

        LinearLayout form = card();
        form.addView(sectionTitle("Qty Picking"));
        EditText qty = input(String.valueOf(Math.max(pallet.cartonCount, 0)), "Qty ambil karton", InputType.TYPE_CLASS_NUMBER);
        EditText picker = input(currentUserName(), "Picker", InputType.TYPE_CLASS_TEXT);
        form.addView(qty);
        form.addView(picker);
        form.addView(primaryButton("Simpan Picking", v -> {
            int pickedQty = parsePositiveInt(value(qty), 0);
            if (pickedQty <= 0 || pickedQty > pallet.cartonCount) {
                toast("Qty picking harus 1 sampai " + pallet.cartonCount + " karton");
                return;
            }
            submitPickingSalesOrderApi(pallet, fallback(stageTwoQuery, "SO-" + todayString()), pickedQty, fallback(value(picker), currentUserName()));
        }));
        content.addView(form);
    }

    private void showVehicleLoading() {
        // Loading now follows the authoritative Penjadwalan Armada.  Keep
        // this legacy entry point as a redirect so an operator cannot select
        // a different driver, vehicle, or helper from the mobile app.
        showLoadingList();
    }

    private void renderVehicleLoading(JSONObject resources) {
        currentScreen = "vehicle_loading";
        setShell("Loading Kendaraan", "Pilih petugas dan kendaraan dari master", "dashboard", true);
        JSONArray drivers = resources.optJSONArray("drivers");
        JSONArray vehicles = resources.optJSONArray("vehicles");
        JSONArray helpers = resources.optJSONArray("helpers");
        if (drivers == null) drivers = new JSONArray();
        if (vehicles == null) vehicles = new JSONArray();
        if (helpers == null) helpers = new JSONArray();
        final JSONArray driverRows = drivers;
        final JSONArray vehicleRows = vehicles;
        final JSONArray helperRows = helpers;

        LinearLayout form = card();
        form.addView(sectionTitle("Data Kendaraan"));
        EditText manifest = input(stageTwoQuery, "No manifest / surat jalan", InputType.TYPE_CLASS_TEXT);
        TextView driver = masterSelectionField("Pilih driver dari master");
        TextView vehicle = masterSelectionField("Pilih kendaraan dari master");
        TextView helper = masterSelectionField("Pilih helper (opsional)");
        form.addView(manifest);
        form.addView(driver);
        form.addView(vehicle);
        form.addView(helper);
        driver.setOnClickListener(v -> showMasterSinglePicker("Pilih Driver", driverRows, false, picked -> {
            selectedLoadingDriver = picked;
            driver.setText("Driver: " + masterRowLabel(picked, false));
            JSONObject suggestedVehicle = findMasterById(vehicleRows, first(picked, "id_armada", "linked_armada_id"));
            if (suggestedVehicle != null) {
                selectedLoadingVehicle = suggestedVehicle;
                vehicle.setText("Kendaraan: " + masterRowLabel(suggestedVehicle, true));
            }
        }));
        vehicle.setOnClickListener(v -> showMasterSinglePicker("Pilih Kendaraan", vehicleRows, true, picked -> {
            selectedLoadingVehicle = picked;
            vehicle.setText("Kendaraan: " + masterRowLabel(picked, true));
        }));
        helper.setOnClickListener(v -> showMasterMultiPicker("Pilih Helper", helperRows, selectedLoadingHelpers, picked -> {
            selectedLoadingHelpers.clear();
            selectedLoadingHelpers.putAll(picked);
            helper.setText("Helper: " + selectedMasterNames(selectedLoadingHelpers));
        }));
        form.addView(primaryButton("Scan QR Barang / Rak", v -> {
            String manifestNo = value(manifest);
            if (manifestNo.isEmpty()) {
                toast("No manifest wajib diisi");
                return;
            }
            if (selectedLoadingDriver == null || selectedLoadingVehicle == null) {
                toast("Pilih driver dan kendaraan dari master.");
                return;
            }
            selectedLoadingManifest = manifestNo;
            stageTwoQuery = manifestNo;
            startScanner("LOADING_PALLET");
        }));
        content.addView(form);
    }

    private void showVehicleLoadingForm(Pallet pallet) {
        selectedPallet = pallet;
        currentScreen = "vehicle_loading_form";
        setShell("Konfirmasi Muat", pallet.code, "dashboard", true);
        content.addView(palletDetailCard(pallet, "Pallet Dimuat"));

        String manifestNo = fallback(selectedLoadingManifest, stageTwoQuery);

        LinearLayout form = card();
        form.addView(sectionTitle("Kendaraan"));
        EditText manifest = input(manifestNo, "No manifest / surat jalan", InputType.TYPE_CLASS_TEXT);
        TextView driver = masterSelectionField(selectedLoadingDriver == null
                ? "Driver belum dipilih" : "Driver: " + masterRowLabel(selectedLoadingDriver, false));
        TextView vehicle = masterSelectionField(selectedLoadingVehicle == null
                ? "Kendaraan belum dipilih" : "Kendaraan: " + masterRowLabel(selectedLoadingVehicle, true));
        TextView helper = masterSelectionField("Helper: " + selectedMasterNames(selectedLoadingHelpers));
        driver.setEnabled(false);
        vehicle.setEnabled(false);
        helper.setEnabled(false);
        EditText qty = input(String.valueOf(Math.max(pallet.cartonCount, 0)), "Qty muat karton", InputType.TYPE_CLASS_NUMBER);
        form.addView(manifest);
        form.addView(driver);
        form.addView(vehicle);
        form.addView(helper);
        form.addView(qty);
        form.addView(primaryButton("Simpan Loading Kendaraan", v -> {
            int loadQty = parsePositiveInt(value(qty), 0);
            if (value(manifest).isEmpty() || loadQty <= 0 || selectedLoadingDriver == null || selectedLoadingVehicle == null) {
                toast("Manifest, driver, kendaraan, dan qty wajib lengkap");
                return;
            }
            submitVehicleLoadingApi(pallet, value(manifest), selectedLoadingVehicle, selectedLoadingDriver, selectedLoadingHelpers, loadQty);
        }));
        form.addView(secondaryButton("Ubah Driver / Kendaraan", v -> showVehicleLoading()));
        content.addView(form);
    }

    private void showDriverManifest() {
        currentScreen = "driver_manifest";
        setShell("Manifest Driver", "Tanda tangan serah terima driver", "dashboard", true);

        LinearLayout form = card();
        form.addView(sectionTitle("Serah Terima"));
        EditText manifest = input(stageTwoQuery, "No manifest", InputType.TYPE_CLASS_TEXT);
        EditText driver = input("", "Nama driver", InputType.TYPE_CLASS_TEXT);
        EditText plate = input("", "No polisi", InputType.TYPE_CLASS_TEXT);
        EditText signature = input("", "Nama penanda tangan", InputType.TYPE_CLASS_TEXT);
        form.addView(manifest);
        form.addView(driver);
        form.addView(plate);
        form.addView(signature);
        form.addView(primaryButton("Simpan Tanda Tangan Driver", v -> {
            if (value(manifest).isEmpty() || value(driver).isEmpty() || value(signature).isEmpty()) {
                toast("Manifest, driver, dan tanda tangan wajib diisi");
                return;
            }
            submitManifestSignatureApi(value(manifest), value(driver), value(plate), value(signature));
        }));
        content.addView(form);
    }

    private void showDroppingTransfer() {
        currentScreen = "dropping";
        setShell("Dropping / Karantina", "Cari manifest lalu drop ke karantina", "warehouse", true);

        LinearLayout form = card();
        form.addView(sectionTitle("Cari Manifest"));
        form.addView(body("Cari manifest berdasarkan kode atau nama driver, lalu kirim manifest bermasalah ke Rak Karantina."));
        EditText driver = input(droppingDriverQuery, "Kode atau nama driver (kosongkan untuk semua)", InputType.TYPE_CLASS_TEXT);
        form.addView(driver);
        form.addView(primaryButton("Muat Manifest", v -> {
            droppingDriverQuery = value(driver);
            loadDroppingManifests();
        }));
        content.addView(form);

        if (!droppingDriverQuery.isEmpty()) {
            loadDroppingManifests();
        }
    }

    private void loadDroppingManifests() {
        renderDroppingManifests(null, true, "");
        executor.execute(() -> {
            try {
                JSONObject response = apiClient.searchWmsManifest(droppingDriverQuery);
                JSONArray rows = normalizeRows(response);
                uiHandler.post(() -> renderDroppingManifests(rows, false, ""));
            } catch (Exception e) {
                uiHandler.post(() -> renderDroppingManifests(new JSONArray(), false, operatorErrorMessage(e, "Manifest dropping belum bisa dimuat.")));
            }
        });
    }

    private void renderDroppingManifests(JSONArray rows, boolean loading, String error) {
        currentScreen = "dropping";
        setShell("Dropping / Karantina", "Manifest driver dari WMS", "warehouse", true);

        LinearLayout search = card();
        search.addView(dropdownHeader(filterSummary("Filter Manifest", droppingDriverQuery, ""), droppingFilterOpen, v -> {
            droppingFilterOpen = !droppingFilterOpen;
            renderDroppingManifests(rows, loading, error);
        }));
        if (droppingFilterOpen) {
            EditText driver = input(droppingDriverQuery, "Kode atau nama driver", InputType.TYPE_CLASS_TEXT);
            search.addView(driver);
            search.addView(primaryButton("Cari Manifest", v -> {
                droppingDriverQuery = value(driver);
                loadDroppingManifests();
            }));
        } else {
            search.addView(small(activeFilterText(droppingDriverQuery, "", "Filter manifest disembunyikan. Tap untuk cari driver.")));
        }
        content.addView(search);

        LinearLayout list = card();
        list.addView(sectionTitle("Manifest"));
        if (loading) {
            list.addView(body("Memuat manifest dari WMS..."));
        } else if (!error.isEmpty()) {
            TextView err = body(error);
            err.setTextColor(AMBER);
            list.addView(err);
            list.addView(primaryButton("Coba Lagi", v -> loadDroppingManifests()));
        } else if (rows == null || rows.length() == 0) {
            list.addView(body("Manifest belum tersedia untuk filter ini."));
        } else {
            int limit = Math.min(rows.length(), 80);
            list.addView(body("Menampilkan " + limit + " dari " + rows.length() + " manifest."));
            for (int i = 0; i < limit; i++) {
                JSONObject row = rows.optJSONObject(i);
                if (row != null) {
                    list.addView(droppingManifestRow(row));
                }
            }
        }
        content.addView(list);
    }

    private LinearLayout droppingManifestRow(JSONObject manifest) {
        LinearLayout row = vertical();
        row.setPadding(0, dp(12), 0, dp(12));
        row.setBackground(separatorBackground());

        String noManifest = first(manifest, "NoManifest", "no_manifest", "manifest_no", "no_manifest");
        String driver = fallback(first(manifest, "KodeDriver", "kode_driver", "Driver", "driver"), "-");
        JSONArray details = manifest.optJSONArray("details");
        int detailCount = details == null ? parseOptionalInt(manifest, 0, "total_item", "item_count") : details.length();

        LinearLayout top = horizontal();
        LinearLayout text = vertical();
        text.addView(title(fallback(noManifest, "-"), 15));
        text.addView(small("Driver: " + driver + " | Item: " + detailCount));
        text.addView(small("Status: " + fallback(first(manifest, "status", "Status"), "Manifest")));
        top.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        top.addView(badge("Manifest", Color.rgb(111, 66, 193)));
        row.addView(top);

        String status = fallback(first(manifest, "status", "Status"), "").toUpperCase(Locale.ROOT);
        if ("DELIVERY_ISSUE".equals(status)) {
            row.addView(body("Retur/selisih per item sudah dicatat oleh Driver/Helper. Jangan karantina ulang seluruh manifest."));
            row.addView(compactAction("Buka QC Karantina", v -> showQuarantineQcForSearch(noManifest)));
        } else if (!"DELIVERED".equals(status) && !"QUARANTINE".equals(status) && !"QC_COMPLETE".equals(status)) {
            row.addView(compactAction("Pengiriman Selesai", v -> showDeliveryConfirm(manifest)));
            row.addView(compactAction("Gagal Kirim / Retur", v -> showQuarantineConfirm(manifest)));
        } else if ("QUARANTINE".equals(status)) {
            row.addView(compactAction("Buka QC Karantina", v -> showQuarantineQcForSearch(noManifest)));
        }
        return row;
    }

    private void showDeliveryConfirm(JSONObject manifest) {
        String noManifest = first(manifest, "NoManifest", "no_manifest", "manifest_no");
        currentScreen = "delivery_confirm";
        setShell("Konfirmasi Pengiriman", fallback(noManifest, "Manifest"), "warehouse", true);

        LinearLayout detail = card();
        detail.addView(sectionTitle("Manifest Terkirim"));
        detail.addView(keyValue("Driver", fallback(first(manifest, "driver", "Driver"), "-")));
        detail.addView(keyValue("Kendaraan", fallback(first(manifest, "vehicle_no", "NoKendaraan"), "-")));
        content.addView(detail);

        LinearLayout form = card();
        EditText receiver = input("", "Nama penerima di toko", InputType.TYPE_CLASS_TEXT);
        EditText notes = input("", "Catatan pengiriman", InputType.TYPE_CLASS_TEXT);
        form.addView(receiver);
        form.addView(notes);
        form.addView(primaryButton("Selesaikan Pengiriman", v -> completeManifestDelivery(manifest, value(receiver), value(notes))));
        form.addView(secondaryButton("Kembali", v -> loadDroppingManifests()));
        content.addView(form);
    }

    private void completeManifestDelivery(JSONObject manifest, String receiver, String notes) {
        String noManifest = first(manifest, "NoManifest", "no_manifest", "manifest_no");
        if (noManifest.isEmpty() || receiver.isEmpty()) {
            toast("Manifest dan nama penerima wajib tersedia.");
            return;
        }
        showLoadingScreen("Pengiriman", "Menyelesaikan manifest " + noManifest + "...");
        executor.execute(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("no_manifest", noManifest);
                payload.put("received_by", receiver);
                payload.put("notes", notes);
                putIfNotEmpty(payload, "user_id", currentUserId());
                JSONObject result = apiClient.completeWmsDelivery(payload);
                uiHandler.post(() -> {
                    setShell("Pengiriman Selesai", noManifest, "warehouse", true);
                    LinearLayout success = card();
                    success.setBackground(successBackground());
                    success.addView(title("Barang Diterima Toko", 20));
                    success.addView(body(fallback(first(result, "message", "msg"), "Manifest selesai dikirim.")));
                    success.addView(keyValue("Penerima", receiver));
                    content.addView(success);
                    content.addView(primaryButton("Daftar Manifest", v -> loadDroppingManifests()));
                });
            } catch (Exception e) {
                uiHandler.post(() -> showApiError("Pengiriman", "Manifest belum berhasil diselesaikan.", e,
                        () -> showDeliveryConfirm(manifest)));
            }
        });
    }

    private void showQuarantineConfirm(JSONObject manifest) {
        String noManifest = first(manifest, "NoManifest", "no_manifest", "manifest_no");
        currentScreen = "quarantine_confirm";
        setShell("Gagal Kirim / Retur", fallback(noManifest, "Manifest"), "warehouse", true);
        LinearLayout form = card();
        form.addView(sectionTitle("Alasan Karantina"));
        form.addView(body("Seluruh item manifest akan ditahan di area karantina untuk pemeriksaan GOOD/BAD."));
        EditText reason = input("Gagal kirim / retur toko", "Alasan gagal kirim", InputType.TYPE_CLASS_TEXT);
        form.addView(reason);
        form.addView(primaryButton("Kirim ke Karantina", v -> dropManifestToQuarantine(manifest, value(reason))));
        form.addView(secondaryButton("Batal", v -> loadDroppingManifests()));
        content.addView(form);
    }

    private void dropManifestToQuarantine(JSONObject manifest, String reason) {
        String noManifest = first(manifest, "NoManifest", "no_manifest", "manifest_no");
        if (noManifest.isEmpty()) {
            toast("No manifest tidak terbaca.");
            return;
        }
        showLoadingScreen("Dropping / Karantina", "Mengirim " + noManifest + " ke karantina...");
        executor.execute(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("no_manifest", noManifest);
                payload.put("reason", fallback(reason, "Gagal kirim / retur toko"));
                JSONArray details = manifest.optJSONArray("details");
                if (details == null) {
                    details = new JSONArray();
                    details.put(new JSONObject(manifest.toString()));
                }
                payload.put("items", details);
                putIfNotEmpty(payload, "user_id", currentUserId());
                final int quarantineItemCount = details.length();

                JSONObject result = apiClient.dropWmsManifestToQuarantine(payload);
                uiHandler.post(() -> {
                    recordLocalTransaction("Dropping / Karantina", noManifest, "", "", "Manifest karantina", "Manifest", "Karantina", quarantineItemCount, "Item", fallback(first(result, "message", "msg"), "Manifest dikirim ke karantina."));
                    showDroppingQuarantineSuccess(noManifest, result);
                });
            } catch (Exception e) {
                uiHandler.post(() -> showApiError("Dropping / Karantina", "Manifest belum berhasil dikirim ke karantina.", e, () -> renderDroppingManifests(rowsAsResponse(manifest).optJSONArray("items"), false, "")));
            }
        });
    }

    private void showDroppingQuarantineSuccess(String noManifest, JSONObject result) {
        currentScreen = "dropping_success";
        setShell("Karantina Berhasil", noManifest, "warehouse", true);
        LinearLayout success = card();
        success.setGravity(Gravity.CENTER_HORIZONTAL);
        success.setBackground(successBackground());
        TextView check = title("OK", 36);
        check.setTextColor(GREEN);
        success.addView(check);
        success.addView(title("Manifest Masuk Karantina", 20));
        success.addView(body(fallback(first(result, "message", "msg"), "Manifest " + noManifest + " sudah diproses ke karantina.")));
        content.addView(success);
        content.addView(primaryButton("Refresh Manifest", v -> loadDroppingManifests()));
        content.addView(space(8));
        content.addView(secondaryButton("Lihat Transaksi", v -> {
            transactionQuery = noManifest;
            showTransactions();
        }));
        content.addView(secondaryButton("Proses QC Karantina", v -> showQuarantineQcForSearch(noManifest)));
    }

    private void showQuarantineQc() {
        quarantineQuery = "";
        quarantineFilterOpen = true;
        loadQuarantineQc();
    }

    private void showQuarantineQcForSearch(String query) {
        quarantineQuery = safe(query).trim();
        quarantineFilterOpen = !quarantineQuery.isEmpty();
        loadQuarantineQc();
    }

    private void loadQuarantineQc() {
        renderQuarantineQc(null, true, "");
        executor.execute(() -> {
            try {
                JSONObject response = apiClient.getWmsQuarantineOpen(quarantineQuery);
                JSONArray rows = normalizeRows(response);
                uiHandler.post(() -> renderQuarantineQc(rows, false, ""));
            } catch (Exception e) {
                uiHandler.post(() -> renderQuarantineQc(new JSONArray(), false,
                        operatorErrorMessage(e, "Data QC karantina belum bisa dimuat.")));
            }
        });
    }

    private void renderQuarantineQc(JSONArray rows, boolean loading, String error) {
        currentScreen = "quarantine_qc";
        setShell("QC Karantina", "Pisahkan barang GOOD dan BAD", "warehouse", true);
        LinearLayout info = card();
        info.addView(sectionTitle("Pemeriksaan Barang Gagal Kirim"));
        info.addView(body("Barang GOOD dikembalikan ke rak Tetap/Lorong. Barang BAD dicatat sebagai stok rusak."));
        content.addView(info);

        LinearLayout search = card();
        search.addView(dropdownHeader(filterSummary("Cari / Scan Manifest", quarantineQuery, ""), quarantineFilterOpen, v -> {
            quarantineFilterOpen = !quarantineFilterOpen;
            renderQuarantineQc(rows, loading, error);
        }));
        if (quarantineFilterOpen) {
            search.addView(body("Scan atau masukkan nomor manifest, nota/faktur, kode KPR, SKU, atau nama produk. Filter ini memakai sumber antrean QC yang sama dengan WMS website."));
            search.addView(secondaryButton("Scan QR / Barcode Manifest", v -> startScanner("QUARANTINE_MANIFEST")));
            EditText query = input(quarantineQuery, "Manifest, nota, KPR, SKU, atau produk", InputType.TYPE_CLASS_TEXT);
            search.addView(query);
            search.addView(primaryButton("Muat QC Karantina", v -> {
                quarantineQuery = value(query);
                loadQuarantineQc();
            }));
            if (!quarantineQuery.isEmpty()) {
                search.addView(secondaryButton("Tampilkan Semua Antrean", v -> {
                    quarantineQuery = "";
                    loadQuarantineQc();
                }));
            }
        } else {
            search.addView(small(activeFilterText(quarantineQuery, "", "Filter disembunyikan. Tap untuk mencari atau scan manifest tertentu.")));
        }
        content.addView(search);

        LinearLayout list = card();
        if (loading) {
            list.addView(body("Memuat barang karantina..."));
        } else if (!error.isEmpty()) {
            TextView err = body(error);
            err.setTextColor(AMBER);
            list.addView(err);
            list.addView(primaryButton("Coba Lagi", v -> loadQuarantineQc()));
        } else if (rows == null || rows.length() == 0) {
            list.addView(body(quarantineQuery.isEmpty()
                    ? "Tidak ada barang karantina yang menunggu QC."
                    : "Tidak ada barang karantina yang menunggu QC untuk filter ini."));
        } else {
            list.addView(body(rows.length() + " item menunggu QC."));
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.optJSONObject(i);
                if (row != null) {
                    list.addView(quarantineQcRow(row));
                }
            }
        }
        content.addView(list);
    }

    private LinearLayout quarantineQcRow(JSONObject item) {
        LinearLayout row = vertical();
        row.setPadding(0, dp(12), 0, dp(12));
        row.setBackground(separatorBackground());
        row.addView(title(fallback(first(item, "nama_barang"), fallback(first(item, "kode_barang"), "Produk")), 17));
        row.addView(small("Manifest: " + fallback(first(item, "no_manifest"), "-")
                + " | Nota: " + fallback(first(item, "reference_no"), "-")));
        row.addView(small("Qty: " + fallback(first(item, "qty_ct"), "0") + " CT + "
                + fallback(first(item, "qty_pc"), "0") + " PC ("
                + fallback(first(item, "qty_pcs"), "0") + " PCS)"));
        row.addView(small("Batch: " + transferStockBatch(item) + " | Expired: " + transferStockExpired(item)));
        row.addView(small("Alasan: " + fallback(first(item, "reason"), "-")));
        row.addView(compactAction("Proses QC", v -> showQuarantineQcForm(item)));
        return row;
    }

    /**
     * Searchable picker for the QC destination. The backend supplies only
     * active Tetap/Lorong racks in this quarantine item's branch, which avoids
     * manual typos and prevents choosing a rack from a different branch.
     */
    private void showQuarantineRackPicker(int quarantineId, TextView selectedField) {
        if (quarantineId <= 0) {
            toast("Data karantina tidak valid. Muat ulang daftar QC.");
            return;
        }

        LinearLayout pickerBody = new LinearLayout(this);
        pickerBody.setOrientation(LinearLayout.VERTICAL);
        pickerBody.setPadding(dp(20), dp(6), dp(20), 0);

        EditText search = input("", "Cari kode rak Tetap atau Lorong", InputType.TYPE_CLASS_TEXT);
        pickerBody.addView(search);
        TextView hint = small("Daftar dibatasi pada rak aktif di cabang barang karantina.");
        hint.setPadding(0, 0, 0, dp(6));
        pickerBody.addView(hint);

        ScrollView scroll = new ScrollView(this);
        LinearLayout choices = new LinearLayout(this);
        choices.setOrientation(LinearLayout.VERTICAL);
        choices.setPadding(0, dp(4), 0, dp(4));
        scroll.addView(choices);
        pickerBody.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(380)));

        final AlertDialog[] dialog = new AlertDialog[1];
        Runnable loadChoices = () -> {
            choices.removeAllViews();
            choices.addView(body("Memuat rak..."));
            final String query = value(search);
            executor.execute(() -> {
                try {
                    JSONObject response = apiClient.getWmsQuarantineTargetRacks(quarantineId, query);
                    JSONArray rows = normalizeRows(response);
                    uiHandler.post(() -> {
                        choices.removeAllViews();
                        if (rows.length() == 0) {
                            choices.addView(body("Rak Tetap/Lorong aktif tidak ditemukan untuk pencarian ini."));
                            return;
                        }
                        for (int i = 0; i < rows.length(); i++) {
                            final JSONObject rack = rows.optJSONObject(i);
                            if (rack == null) {
                                continue;
                            }
                            final String code = first(rack, "kode_rak", "KodeRak");
                            if (code.isEmpty()) {
                                continue;
                            }
                            String type = fallback(first(rack, "type_rak", "tipe_rak"), "Rak");
                            String label = fallback(first(rack, "label"), code + " · " + type);
                            TextView option = masterOptionRow(code, label);
                            option.setOnClickListener(v -> {
                                quarantineTargetRack = code;
                                selectedField.setText("Rak tujuan GOOD: " + code + "   Ubah");
                                if (dialog[0] != null) {
                                    dialog[0].dismiss();
                                }
                            });
                            choices.addView(option);
                        }
                    });
                } catch (Exception e) {
                    uiHandler.post(() -> {
                        choices.removeAllViews();
                        TextView error = body(operatorErrorMessage(e, "Rak tujuan belum bisa dimuat."));
                        error.setTextColor(Color.rgb(190, 18, 60));
                        choices.addView(error);
                    });
                }
            });
        };

        Button searchButton = secondaryButton("Cari Rak", v -> loadChoices.run());
        pickerBody.addView(searchButton);
        dialog[0] = new AlertDialog.Builder(this)
                .setTitle("Pilih Rak Tetap / Lorong")
                .setView(pickerBody)
                .setNegativeButton("Batal", null)
                .create();
        dialog[0].setOnShowListener(ignored -> loadChoices.run());
        dialog[0].show();
    }

    private void showQuarantineQcForm(JSONObject item) {
        currentScreen = "quarantine_qc_form";
        int total = parseOptionalInt(item, 0, "qty_pcs");
        int quarantineId = parseOptionalInt(item, 0, "id", "quarantine_id");
        if (quarantineId != quarantineRackContextId) {
            quarantineRackContextId = quarantineId;
            quarantineTargetRack = "";
        }
        setShell("Proses QC", fallback(first(item, "kode_barang"), "Karantina"), "warehouse", true);

        LinearLayout detail = card();
        detail.addView(title(fallback(first(item, "nama_barang"), "Produk"), 18));
        detail.addView(keyValue("Total diperiksa", total + " PCS"));
        detail.addView(keyValue("Batch", transferStockBatch(item)));
        detail.addView(keyValue("Expired", transferStockExpired(item)));
        content.addView(detail);

        LinearLayout form = card();
        form.addView(sectionTitle("Hasil QC"));
        EditText good = input(String.valueOf(total), "Qty GOOD (PCS)", InputType.TYPE_CLASS_NUMBER);
        EditText bad = input("0", "Qty BAD (PCS)", InputType.TYPE_CLASS_NUMBER);
        TextView rack = masterSelectionField(quarantineTargetRack.isEmpty()
                ? "Pilih Rak Tetap / Lorong untuk GOOD"
                : "Rak tujuan GOOD: " + quarantineTargetRack + "   Ubah");
        rack.setOnClickListener(v -> showQuarantineRackPicker(quarantineId, rack));
        EditText qcName = input("", "Nama checker / QC", InputType.TYPE_CLASS_TEXT);
        form.addView(good);
        form.addView(bad);
        form.addView(rack);
        form.addView(qcName);
        form.addView(primaryButton("Simpan Hasil QC", v -> submitQuarantineQc(item,
                value(good), value(bad), quarantineTargetRack, value(qcName))));
        form.addView(secondaryButton("Kembali", v -> loadQuarantineQc()));
        content.addView(form);
    }

    private void submitQuarantineQc(JSONObject item, String goodText, String badText, String rack, String qcName) {
        int total = parseOptionalInt(item, 0, "qty_pcs");
        int good = parsePositiveInt(goodText, -1);
        int bad = parsePositiveInt(badText, -1);
        if (good < 0 || bad < 0 || good + bad != total) {
            toast("Total GOOD + BAD harus sama dengan " + total + " PCS.");
            return;
        }
        if (good > 0 && rack.isEmpty()) {
            toast("Rak Tetap/Lorong wajib dipilih untuk barang GOOD.");
            return;
        }
        if (qcName.isEmpty()) {
            toast("Nama checker / QC wajib diisi.");
            return;
        }
        showLoadingScreen("QC Karantina", "Menyimpan hasil pemeriksaan...");
        executor.execute(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("quarantine_id", parseOptionalInt(item, 0, "id", "quarantine_id"));
                payload.put("qty_good", good);
                payload.put("qty_bad", bad);
                payload.put("target_rack", rack);
                payload.put("qc_name", qcName);
                putIfNotEmpty(payload, "user_id", currentUserId());
                JSONObject result = apiClient.processWmsQuarantineQc(payload);
                uiHandler.post(() -> {
                    quarantineTargetRack = "";
                    quarantineRackContextId = 0;
                    setShell("QC Selesai", fallback(first(item, "kode_barang"), "Karantina"), "warehouse", true);
                    LinearLayout success = card();
                    success.setBackground(successBackground());
                    success.addView(title("Hasil QC Tersimpan", 20));
                    success.addView(body(fallback(first(result, "message", "msg"), "Barang karantina sudah diproses.")));
                    success.addView(keyValue("GOOD", good + " PCS"));
                    success.addView(keyValue("BAD", bad + " PCS"));
                    content.addView(success);
                    content.addView(primaryButton("QC Berikutnya", v -> loadQuarantineQc()));
                });
            } catch (Exception e) {
                uiHandler.post(() -> showApiError("QC Karantina", "Hasil QC belum berhasil disimpan.", e,
                        () -> showQuarantineQcForm(item)));
            }
        });
    }

    private void showDroppingForm(Pallet pallet) {
        selectedPallet = pallet;
        currentScreen = "dropping_form";
        setShell("Konfirmasi Dropping", pallet.code, "warehouse", true);
        content.addView(palletDetailCard(pallet, "Pallet Transfer"));

        LinearLayout form = card();
        form.addView(sectionTitle("Tujuan Gudang"));
        EditText doc = input("DROP-" + new SimpleDateFormat("yyyyMMddHHmm", Locale.US).format(new Date()), "No dokumen dropping", InputType.TYPE_CLASS_TEXT);
        EditText warehouse = input("", "Gudang tujuan", InputType.TYPE_CLASS_TEXT);
        EditText receiver = input("", "Penerima / checker", InputType.TYPE_CLASS_TEXT);
        form.addView(doc);
        form.addView(warehouse);
        form.addView(receiver);
        form.addView(primaryButton("Simpan Dropping", v -> {
            if (value(doc).isEmpty() || value(warehouse).isEmpty()) {
                toast("No dokumen dan gudang tujuan wajib diisi");
                return;
            }
            String from = fallback(pallet.location, "Gudang asal");
            submitDroppingTransferApi(pallet, value(doc), from, value(warehouse), value(receiver));
        }));
        content.addView(form);
    }

    private void showWarehouseReturn() {
        currentScreen = "warehouse_return";
        setShell("Retur Barang Masuk", "Scan KPR lalu terima ke antrean QC Karantina", "warehouse", true);

        LinearLayout form = card();
        form.addView(sectionTitle("Dokumen Retur"));
        form.addView(body("Scan QR KPR atau masukkan kode request retur yang sudah diapprove. Seluruh detail request akan dibuatkan antrean QC; stok tidak bertambah sebelum petugas QC menentukan GOOD/BAD."));
        form.addView(primaryButton("Scan QR KPR / Request Retur", v -> startScanner("RETURN_INBOUND")));
        EditText doc = input(warehouseReturnReference, "Kode KPR / kode request retur", InputType.TYPE_CLASS_TEXT);
        form.addView(doc);
        form.addView(primaryButton("Terima ke QC Karantina", v -> {
            String reference = extractReturnReference(value(doc));
            if (reference.isEmpty()) {
                toast("Scan atau isi Kode KPR / kode request retur terlebih dahulu");
                return;
            }
            warehouseReturnReference = reference;
            submitReturnInboundApi(reference);
        }));
        form.addView(secondaryButton("Buka QC Karantina", v -> showQuarantineQc()));
        content.addView(form);
    }

    private void submitStockOpnameApi(Pallet pallet, JSONArray uomCounts, JSONObject baseUom,
                                      double totalGood, double totalBad, String note) {
        showLoadingScreen("Stock Opname", "Mengirim hasil cycle count ke API...");
        executor.execute(() -> {
            try {
                JSONObject payload = palletApiPayload(pallet, stockOpnameSoNo, 0);
                double totalPhysical = totalGood + totalBad;
                String baseLabel = stockOpnameUomLabel(baseUom);
                payload.put("id_stock_opname", stockOpnameScheduleId);
                // The lot picker always resolves a single batch/expired identity.
                // Tell the API not to aggregate another lot with the same SKU.
                payload.put("lot_selected", true);
                // Field lama tetap dikirim dalam satuan terkecil agar server lama tetap menyimpan total fisik.
                payload.put("qty_uom", stockOpnameNumber(totalPhysical));
                int baseUomId = stockOpnameUomId(baseUom);
                if (baseUomId > 0) payload.put("uom_id", baseUomId);
                payload.put("uom_label", baseLabel);
                payload.put("qty_good", totalGood);
                payload.put("qty_bad", totalBad);
                payload.put("qty_physical", totalPhysical);
                payload.put("uom_counts", uomCounts == null ? new JSONArray() : uomCounts);
                payload.put("principle", stockOpnamePrinciple);
                payload.put("pic", stockOpnamePic);
                payload.put("lokasi_rak", fallback(pallet.location, stockOpnameLocation));
                payload.put("note", "SO per Principle: " + fallback(stockOpnamePrinciple, "-")
                        + ". Lokasi " + fallback(pallet.location, stockOpnameLocation)
                        + ". Total fisik " + stockOpnameNumber(totalPhysical) + " " + baseLabel
                        + " (GOOD " + stockOpnameNumber(totalGood) + " " + baseLabel
                        + ", BAD " + stockOpnameNumber(totalBad) + " " + baseLabel + "). "
                        + fallback(stockOpnameUomCountsSummary(uomCounts), "")
                        + (safe(note).isEmpty() ? "" : " " + note));
                apiClient.submitWmsStockOpname(payload);
                uiHandler.post(() -> {
                    toast("Stock opname tersimpan di API");
                    loadStockOpnameLocations();
                });
            } catch (Exception e) {
                uiHandler.post(() -> showApiError("Stock Opname", "Stock opname belum berhasil dikirim.", e, () -> showStockOpnameForm(pallet)));
            }
        });
    }

    private String stockOpnameUomCountsSummary(JSONArray counts) {
        ArrayList<String> rows = new ArrayList<>();
        if (counts != null) {
            for (int i = 0; i < counts.length(); i++) {
                JSONObject count = counts.optJSONObject(i);
                if (count == null) {
                    continue;
                }
                String label = fallback(first(count, "uom_label"), "PCS");
                rows.add(label + " GOOD " + stockOpnameNumber(count.optDouble("qty_good", 0d))
                        + ", BAD " + stockOpnameNumber(count.optDouble("qty_bad", 0d)));
            }
        }
        return TextUtils.join("; ", rows);
    }

    private void submitPickingSalesOrderApi(Pallet pallet, String salesOrder, int qty, String picker) {
        showLoadingScreen("Picking SO", "Mengirim picking Sales Order ke API...");
        executor.execute(() -> {
            try {
                JSONObject payload = palletApiPayload(pallet, salesOrder, qty);
                payload.put("sales_order", salesOrder);
                payload.put("picker", picker);
                payload.put("to_rack", "Area Loading");
                payload.put("note", "Picker: " + picker);
                apiClient.submitWmsPickingSalesOrder(payload);
                uiHandler.post(() -> {
                    recordLocalTransaction("Picking SO", salesOrder, pallet.code, "", pallet.skuName,
                            fallback(pallet.location, "-"), "Area Loading", qty, "Karton", "Picker: " + picker);
                    toast("Picking SO tersimpan di API");
                    transactionQuery = salesOrder;
                    showTransactions();
                });
            } catch (Exception e) {
                uiHandler.post(() -> showApiError("Picking SO", "Picking SO belum berhasil dikirim.", e, () -> showPickingSalesOrderForm(pallet)));
            }
        });
    }

    private void submitVehicleLoadingApi(Pallet pallet, String manifest, JSONObject vehicle, JSONObject driver,
                                         LinkedHashMap<String, JSONObject> helpers, int qty) {
        showLoadingScreen("Loading Kendaraan", "Mengirim loading kendaraan ke API...");
        executor.execute(() -> {
            try {
                String vehicleName = masterRowLabel(vehicle, true);
                String driverName = masterRowLabel(driver, false);
                JSONObject payload = palletApiPayload(pallet, manifest, qty);
                payload.put("no_manifest", manifest);
                payload.put("id_driver", masterRowId(driver));
                payload.put("driver_name", driverName);
                payload.put("driver", driverName);
                payload.put("id_armada", masterRowId(vehicle));
                payload.put("armada_name", first(vehicle, "nama", "name", "nama_armada"));
                payload.put("vehicle_no", first(vehicle, "no_pelat", "vehicle_no", "no_polisi", "plat_nomor", "kode", "name", "nama"));
                JSONArray helperIds = new JSONArray();
                for (JSONObject helper : helpers.values()) {
                    helperIds.put(masterRowId(helper));
                }
                payload.put("helper_ids", helperIds);
                payload.put("to_rack", fallback(vehicleName, "Kendaraan"));
                apiClient.submitWmsVehicleLoading(payload);
                uiHandler.post(() -> {
                    recordLocalTransaction("Loading Kendaraan", manifest, pallet.code, "", pallet.skuName,
                            fallback(pallet.location, "-"), fallback(vehicleName, "Kendaraan"), qty, "Karton",
                            "Driver: " + fallback(driverName, "-") + ". Helper: " + selectedMasterNames(helpers));
                    toast("Loading kendaraan tersimpan di API");
                    transactionQuery = manifest;
                    showTransactions();
                });
            } catch (Exception e) {
                uiHandler.post(() -> showApiError("Loading Kendaraan", "Loading kendaraan belum berhasil dikirim.", e, () -> showVehicleLoadingForm(pallet)));
            }
        });
    }

    private void submitManifestSignatureApi(String manifest, String driver, String plate, String signature) {
        showLoadingScreen("Manifest Driver", "Mengirim tanda tangan driver ke API...");
        executor.execute(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("reference_no", manifest);
                payload.put("no_manifest", manifest);
                payload.put("driver", driver);
                payload.put("vehicle_no", plate);
                payload.put("signature_name", signature);
                payload.put("qty", 1);
                putIfNotEmpty(payload, "user_id", currentUserId());
                apiClient.submitWmsManifestSignature(payload);
                uiHandler.post(() -> {
                    recordLocalTransaction("Manifest Driver", manifest, "", "", "Serah terima driver",
                            "Area Loading", fallback(plate, "Kendaraan"), 1, "Dokumen",
                            "Driver: " + driver + ". Ditandatangani: " + signature);
                    toast("Manifest driver tersimpan di API");
                    transactionQuery = manifest;
                    showTransactions();
                });
            } catch (Exception e) {
                uiHandler.post(() -> showApiError("Manifest Driver", "Tanda tangan driver belum berhasil dikirim.", e, () -> showDriverManifest()));
            }
        });
    }

    private void submitDroppingTransferApi(Pallet pallet, String documentNo, String from, String warehouse, String receiver) {
        showLoadingScreen("Dropping", "Mengirim transfer antar gudang ke API...");
        executor.execute(() -> {
            try {
                JSONObject payload = palletApiPayload(pallet, documentNo, pallet.cartonCount);
                payload.put("from_rack", from);
                payload.put("to_warehouse", warehouse);
                payload.put("to_rack", warehouse);
                payload.put("receiver", receiver);
                apiClient.submitWmsDroppingTransfer(payload);
                uiHandler.post(() -> {
                    toast("Dropping antar gudang tersimpan di API");
                    transactionQuery = documentNo;
                    showTransactions();
                });
            } catch (Exception e) {
                uiHandler.post(() -> showApiError("Dropping", "Dropping antar gudang belum berhasil dikirim.", e, () -> showDroppingForm(pallet)));
            }
        });
    }

    private void submitReturnInboundApi(String documentNo) {
        final String returnReference = extractReturnReference(documentNo);
        if (returnReference.isEmpty()) {
            toast("Kode KPR atau request retur belum valid.");
            return;
        }
        showLoadingScreen("Retur Masuk", "Membuat antrean QC Karantina dari dokumen retur...");
        executor.execute(() -> {
            try {
                JSONObject payload = new JSONObject();
                // Kirim alias eksplisit agar API lama maupun API WMS terbaru
                // menerima kode KPR yang sama. Satu kode tetap menjadi sumber
                // tunggal; tidak ada qty/produk bebas yang dapat menaikkan stok.
                payload.put("reference_no", returnReference);
                payload.put("kode_kpr", returnReference);
                payload.put("kode_request", returnReference);
                payload.put("request_reference", returnReference);
                putIfNotEmpty(payload, "user_id", currentUserId());
                putIfNotEmpty(payload, "actor_name", currentUserName());
                JSONObject result = apiClient.submitWmsReturnInbound(payload);
                uiHandler.post(() -> {
                    int qtyPcs = result.optInt("qty_pcs", 0);
                    int created = result.optInt("created", 0);
                    int reused = result.optInt("reused", 0);
                    String resolvedReference = fallback(first(result, "reference_no", "kode_kpr", "kode_request"), returnReference);
                    recordLocalTransaction("Retur Masuk QC", resolvedReference, "", "", "Retur sumber KPR/request",
                            "Outlet", "Karantina", qtyPcs, "PCS",
                            "Antrean QC dibuat " + created + ", digunakan ulang " + reused + ". Stok belum berubah.");
                    toast(fallback(first(result, "message", "msg"), "Retur masuk antrean QC Karantina."));
                    warehouseReturnReference = "";
                    transactionQuery = resolvedReference;
                    showQuarantineQcForSearch(fallback(first(result, "no_manifest", "reference_no"), resolvedReference));
                });
            } catch (Exception e) {
                uiHandler.post(() -> showApiError("Retur Masuk", "Retur belum berhasil dikaitkan ke QC Karantina.", e, () -> showWarehouseReturn()));
            }
        });
    }

    private JSONObject palletApiPayload(Pallet pallet, String reference, int qty) throws Exception {
        JSONObject payload = new JSONObject();

        payload.put("reference_no", reference);
        payload.put("pallet_code", pallet.code);
        payload.put("kode_barang", pallet.code);
        payload.put("product_name", pallet.skuName);
        payload.put("nama_barang", pallet.skuName);

        String batch = pallet.lotBatch == null ? "" : pallet.lotBatch.trim();
        if (!batch.isEmpty() && !"-".equals(batch)) {
            payload.put("lot_batch", batch);
        }

        String expDate = pallet.expDate == null ? "" : pallet.expDate.trim();
        if (!expDate.isEmpty() && !"-".equals(expDate)) {
            payload.put("exp_date", expDate);
        }

        payload.put("location", pallet.location);
        payload.put("from_rack", fallback(pallet.location, "-"));
        payload.put("kode_rak", pallet.location);
        payload.put("qty", qty);
        payload.put("qty_karton", qty);

        putIfNotEmpty(payload, "user_id", currentUserId());

        return payload;
    }

    private void showSettings() {
        showSettingsWithDevices(null, "");
    }

    private void showSettingsWithDevices(List<String[]> devices, String note) {
        currentScreen = "settings";
        setShell("Settings", "Printer Bluetooth, tampilan, dan preferensi cetak", "settings", true);

        LinearLayout appearance = card();
        appearance.addView(dropdownHeader("Tampilan", settingsAppearanceOpen, v -> {
            settingsAppearanceOpen = !settingsAppearanceOpen;
            showSettingsWithDevices(devices, note);
        }));
        String savedTheme = themeMode();
        appearance.addView(keyValue("Mode Tampilan", savedTheme));
        if (settingsAppearanceOpen) {
            appearance.addView(body("Pilih Gelap untuk penggunaan gudang malam hari, atau Sistem agar mengikuti setting Android."));
            LinearLayout themeRow = horizontal();
            themeRow.setPadding(0, dp(10), 0, 0);
            final String[] selectedTheme = {savedTheme};
            TextView systemTheme = settingChip(THEME_SYSTEM, THEME_SYSTEM.equals(selectedTheme[0]));
            TextView lightTheme = settingChip(THEME_LIGHT, THEME_LIGHT.equals(selectedTheme[0]));
            TextView darkTheme = settingChip(THEME_DARK, THEME_DARK.equals(selectedTheme[0]));
            View.OnClickListener themeListener = v -> {
                selectedTheme[0] = ((TextView) v).getText().toString();
                uiPrefs.edit().putString(KEY_THEME_MODE, selectedTheme[0]).apply();
                toast("Mode tampilan: " + selectedTheme[0]);
                showSettings();
            };
            systemTheme.setOnClickListener(themeListener);
            lightTheme.setOnClickListener(themeListener);
            darkTheme.setOnClickListener(themeListener);
            themeRow.addView(systemTheme);
            themeRow.addView(lightTheme);
            themeRow.addView(darkTheme);
            appearance.addView(themeRow);
        } else {
            appearance.addView(small("Pengaturan tampilan disembunyikan."));
        }
        content.addView(appearance);

        LinearLayout printer = card();
        printer.addView(dropdownHeader("Printer Bluetooth", settingsPrinterOpen, v -> {
            settingsPrinterOpen = !settingsPrinterOpen;
            showSettingsWithDevices(devices, note);
        }));
        printer.addView(keyValue("Mode Aktif", printerSettings.getMode()));
        printer.addView(keyValue("Printer", printerSettings.summary()));
        printer.addView(keyValue("Lebar Kertas", printerSettings.getPaperWidth() + " mm"));
        printer.addView(keyValue("Auto Print QR", printerSettings.isAutoPrint() ? "Aktif" : "Nonaktif"));
        if (!note.isEmpty()) {
            TextView noteView = small(note);
            noteView.setTextColor(BLUE);
            printer.addView(noteView);
        }
        if (!settingsPrinterOpen) {
            printer.addView(small("Detail koneksi printer disembunyikan."));
        }
        content.addView(printer);

        LinearLayout form = card();
        form.addView(dropdownHeader("Konfigurasi Printer", settingsPrinterConfigOpen, v -> {
            settingsPrinterConfigOpen = !settingsPrinterConfigOpen;
            showSettingsWithDevices(devices, note);
        }));
        final String[] selectedMode = {printerSettings.getMode()};
        TextView modeLabel = body("Mode: " + selectedMode[0]);
        if (settingsPrinterConfigOpen) {
            form.addView(modeLabel);

            LinearLayout modeRow = horizontal();
            modeRow.setPadding(0, dp(8), 0, dp(12));
            TextView androidPrintMode = settingChip("Android Print", "Android Print".equals(selectedMode[0]));
            TextView bluetoothMode = settingChip("Bluetooth ESC/POS", "Bluetooth ESC/POS".equals(selectedMode[0]));
            androidPrintMode.setOnClickListener(v -> {
                selectedMode[0] = "Android Print";
                modeLabel.setText("Mode: " + selectedMode[0]);
                styleSettingChip(androidPrintMode, true);
                styleSettingChip(bluetoothMode, false);
            });
            bluetoothMode.setOnClickListener(v -> {
                selectedMode[0] = "Bluetooth ESC/POS";
                modeLabel.setText("Mode: " + selectedMode[0]);
                styleSettingChip(androidPrintMode, false);
                styleSettingChip(bluetoothMode, true);
            });
            modeRow.addView(androidPrintMode);
            modeRow.addView(bluetoothMode);
            form.addView(modeRow);

            EditText printerName = input(printerSettings.getName(), "Nama printer", InputType.TYPE_CLASS_TEXT);
            EditText printerAddress = input(printerSettings.getAddress(), "MAC address printer", InputType.TYPE_CLASS_TEXT);
            EditText paperWidth = input(String.valueOf(printerSettings.getPaperWidth()), "Lebar kertas (58 / 80)", InputType.TYPE_CLASS_NUMBER);
            CheckBox autoPrint = new CheckBox(this);
            autoPrint.setText("Auto print Bluetooth setelah QR dibuat");
            autoPrint.setTextColor(INK);
            autoPrint.setButtonTintList(android.content.res.ColorStateList.valueOf(BLUE));
            autoPrint.setTextSize(14);
            autoPrint.setChecked(printerSettings.isAutoPrint());
            autoPrint.setPadding(0, dp(8), 0, dp(8));

            form.addView(printerName);
            form.addView(printerAddress);
            form.addView(paperWidth);
            form.addView(autoPrint);
            form.addView(space(8));
            form.addView(primaryButton("Simpan Setting Printer", v -> {
                int width = parsePaperWidth(value(paperWidth));
                printerSettings.save(selectedMode[0], value(printerName), value(printerAddress), width, autoPrint.isChecked());
                toast("Setting printer disimpan");
                showSettings();
            }));
            form.addView(space(6));
            form.addView(secondaryButton("Aktifkan Bluetooth", v -> requestEnableBluetooth()));
            form.addView(space(6));
            form.addView(secondaryButton(bluetoothScanning ? "Mencari Perangkat..." : "Cari Perangkat Baru", v -> startBluetoothDiscovery()));
            form.addView(space(6));
            form.addView(secondaryButton("Cek Printer Bluetooth Paired", v -> showPairedBluetoothDevices()));
            form.addView(space(6));
            form.addView(secondaryButton("Tes Cetak Bluetooth", v -> printBluetoothTest()));
        } else {
            form.addView(small("Konfigurasi printer disembunyikan. Tap jika ingin ubah mode, MAC address, atau tes cetak."));
        }
        content.addView(form);

        if (devices != null) {
            LinearLayout list = card();
            list.addView(sectionTitle("Perangkat Paired"));
            if (devices.isEmpty()) {
                list.addView(body("Belum ada perangkat Bluetooth paired yang terbaca."));
            } else {
                for (String[] device : devices) {
                    list.addView(pairedDeviceRow(device[0], device[1]));
                }
            }
            content.addView(list);
        }

        if (bluetoothScanning || !discoveredBluetoothDevices.isEmpty()) {
            LinearLayout foundList = card();
            foundList.addView(sectionTitle("Perangkat Ditemukan"));
            if (bluetoothScanning) {
                foundList.addView(body("Sedang mencari printer/perangkat Bluetooth sekitar..."));
            }
            if (discoveredBluetoothDevices.isEmpty()) {
                foundList.addView(body("Belum ada perangkat baru yang ditemukan."));
            } else {
                for (String[] device : discoveredBluetoothDevices) {
                    foundList.addView(discoveredDeviceRow(device[0], device[1]));
                }
            }
            content.addView(foundList);
        }
    }

    private LinearLayout pairedDeviceRow(String name, String address) {
        LinearLayout row = horizontal();
        row.setPadding(0, dp(10), 0, dp(10));
        row.setBackground(separatorBackground());

        LinearLayout text = vertical();
        text.addView(title(fallback(name, "Bluetooth Device"), 15));
        text.addView(small(address));
        row.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(tinyButton("Pilih", v -> {
            printerSettings.save("Bluetooth ESC/POS", name, address, printerSettings.getPaperWidth(), printerSettings.isAutoPrint());
            toast("Printer dipilih: " + fallback(name, address));
            showSettings();
        }));
        return row;
    }

    private void showPairedBluetoothDevices() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            showSettingsWithDevices(null, "Perangkat ini tidak mendukung Bluetooth.");
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && checkSelfPermission("android.permission.BLUETOOTH_CONNECT") != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{"android.permission.BLUETOOTH_CONNECT"}, REQ_BLUETOOTH_PERMISSIONS);
            toast("Izinkan Bluetooth, lalu tekan cek lagi.");
            return;
        }

        try {
            Set<BluetoothDevice> bondedDevices = adapter.getBondedDevices();
            java.util.ArrayList<String[]> devices = new java.util.ArrayList<>();
            for (BluetoothDevice device : bondedDevices) {
                devices.add(new String[]{
                        fallback(device.getName(), "Bluetooth Device"),
                        fallback(device.getAddress(), "")
                });
            }
            showSettingsWithDevices(devices, "Pilih printer dari perangkat yang sudah paired di Android.");
        } catch (SecurityException e) {
            showSettingsWithDevices(null, "Akses Bluetooth belum diizinkan.");
        }
    }

    private LinearLayout discoveredDeviceRow(String name, String address) {
        LinearLayout row = horizontal();
        row.setPadding(0, dp(10), 0, dp(10));
        row.setBackground(separatorBackground());

        LinearLayout text = vertical();
        text.addView(title(fallback(name, "Bluetooth Device"), 15));
        text.addView(small(address));
        row.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(tinyButton("Tautkan", v -> pairBluetoothDevice(name, address)));
        return row;
    }

    private void requestEnableBluetooth() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            showSettingsWithDevices(null, "Perangkat ini tidak mendukung Bluetooth.");
            return;
        }

        if (!ensureBluetoothPermissions(false)) {
            return;
        }

        try {
            if (adapter.isEnabled()) {
                showSettingsWithDevices(null, "Bluetooth sudah aktif.");
                return;
            }
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQ_ENABLE_BLUETOOTH);
        } catch (SecurityException e) {
            showSettingsWithDevices(null, "Akses untuk mengaktifkan Bluetooth belum diizinkan.");
        }
    }

    private void startBluetoothDiscovery() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            showSettingsWithDevices(null, "Perangkat ini tidak mendukung Bluetooth.");
            return;
        }

        if (!ensureBluetoothPermissions(true)) {
            return;
        }

        try {
            if (!adapter.isEnabled()) {
                requestEnableBluetooth();
                return;
            }

            registerBluetoothDiscoveryReceiver();
            discoveredBluetoothDevices.clear();

            if (adapter.isDiscovering()) {
                adapter.cancelDiscovery();
            }

            bluetoothScanning = adapter.startDiscovery();
            showSettingsWithDevices(null, bluetoothScanning
                    ? "Mencari perangkat Bluetooth sekitar..."
                    : "Pencarian Bluetooth gagal dimulai.");
        } catch (SecurityException e) {
            bluetoothScanning = false;
            showSettingsWithDevices(null, "Akses scan Bluetooth belum diizinkan.");
        }
    }

    private void pairBluetoothDevice(String name, String address) {
        if (address == null || address.trim().isEmpty()) {
            toast("Alamat Bluetooth tidak tersedia");
            return;
        }

        if (!ensureBluetoothPermissions(true)) {
            return;
        }

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            showSettingsWithDevices(null, "Perangkat ini tidak mendukung Bluetooth.");
            return;
        }

        try {
            if (adapter.isDiscovering()) {
                adapter.cancelDiscovery();
            }
            bluetoothScanning = false;
            registerBluetoothDiscoveryReceiver();

            BluetoothDevice device = adapter.getRemoteDevice(address);
            if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                printerSettings.save("Bluetooth ESC/POS", fallback(name, device.getName()), address, printerSettings.getPaperWidth(), printerSettings.isAutoPrint());
                showSettingsWithDevices(null, "Printer sudah tertaut dan disimpan.");
                return;
            }

            boolean started = device.createBond();
            showSettingsWithDevices(null, started
                    ? "Dialog tautkan Bluetooth dibuka. Selesaikan pairing di Android."
                    : "Gagal memulai tautkan Bluetooth.");
        } catch (IllegalArgumentException e) {
            showSettingsWithDevices(null, "MAC address Bluetooth tidak valid.");
        } catch (SecurityException e) {
            showSettingsWithDevices(null, "Akses tautkan Bluetooth belum diizinkan.");
        }
    }

    private boolean ensureBluetoothPermissions(boolean includeScan) {
        java.util.ArrayList<String> missing = new java.util.ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checkSelfPermission("android.permission.BLUETOOTH_CONNECT") != PackageManager.PERMISSION_GRANTED) {
                missing.add("android.permission.BLUETOOTH_CONNECT");
            }
            if (includeScan && checkSelfPermission("android.permission.BLUETOOTH_SCAN") != PackageManager.PERMISSION_GRANTED) {
                missing.add("android.permission.BLUETOOTH_SCAN");
            }
        } else if (includeScan && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission("android.permission.ACCESS_FINE_LOCATION") != PackageManager.PERMISSION_GRANTED) {
                missing.add("android.permission.ACCESS_FINE_LOCATION");
            }
        }

        if (!missing.isEmpty()) {
            requestPermissions(missing.toArray(new String[0]), REQ_BLUETOOTH_PERMISSIONS);
            toast("Izinkan Bluetooth agar printer bisa dicari/ditautkan.");
            return false;
        }

        return true;
    }

    private void registerBluetoothDiscoveryReceiver() {
        if (bluetoothDiscoveryReceiverRegistered) {
            return;
        }

        if (bluetoothDiscoveryReceiver == null) {
            bluetoothDiscoveryReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        if (device == null) {
                            return;
                        }
                        try {
                            addDiscoveredBluetoothDevice(fallback(device.getName(), "Bluetooth Device"), fallback(device.getAddress(), ""));
                            uiHandler.post(() -> showSettingsWithDevices(null, "Perangkat ditemukan. Pilih Tautkan untuk pairing."));
                        } catch (SecurityException ignored) {
                        }
                        return;
                    }

                    if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                        bluetoothScanning = false;
                        uiHandler.post(() -> showSettingsWithDevices(null, "Pencarian perangkat selesai."));
                        return;
                    }

                    if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        if (device == null) {
                            return;
                        }
                        try {
                            if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                                printerSettings.save("Bluetooth ESC/POS", fallback(device.getName(), "Bluetooth Device"), fallback(device.getAddress(), ""), printerSettings.getPaperWidth(), printerSettings.isAutoPrint());
                                uiHandler.post(() -> showSettingsWithDevices(null, "Printer berhasil ditautkan dan disimpan."));
                            }
                        } catch (SecurityException ignored) {
                        }
                    }
                }
            };
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(bluetoothDiscoveryReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(bluetoothDiscoveryReceiver, filter);
        }
        bluetoothDiscoveryReceiverRegistered = true;
    }

    private void addDiscoveredBluetoothDevice(String name, String address) {
        if (address == null || address.trim().isEmpty()) {
            return;
        }
        for (String[] item : discoveredBluetoothDevices) {
            if (address.equalsIgnoreCase(item[1])) {
                item[0] = fallback(name, item[0]);
                return;
            }
        }
        discoveredBluetoothDevices.add(new String[]{fallback(name, "Bluetooth Device"), address});
    }

    private void stopBluetoothDiscovery() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        try {
            if (adapter != null && adapter.isDiscovering()) {
                adapter.cancelDiscovery();
            }
        } catch (SecurityException ignored) {
        }
        bluetoothScanning = false;

        if (bluetoothDiscoveryReceiverRegistered && bluetoothDiscoveryReceiver != null) {
            try {
                unregisterReceiver(bluetoothDiscoveryReceiver);
            } catch (IllegalArgumentException ignored) {
            }
            bluetoothDiscoveryReceiverRegistered = false;
        }
    }

    private void handleScanResult(String payload) {
        if ("PALLET".equals(scanMode)) {
            Pallet pallet = palletFromPickingQr(payload);
            if (pallet == null) {
                toast("QR tidak berisi kode barang dan rak.");
                showPutawayScanPallet();
                return;
            }
            showScanLocation(pallet);
            return;
        }

        if ("REPRINT_PALLET".equals(scanMode)) {
            toast("Reprint lokal dinonaktifkan. Cetak QR dari WMS web/API.");
            showReprintQr();
            return;
        }

        if ("LOCATION".equals(scanMode)) {
            String location = QrUtils.extractLocationCode(payload);
            if (selectedPallet == null) {
                toast("Pilih pallet dulu sebelum scan lokasi");
                showPutawayScanPallet();
                return;
            }
            if (location.isEmpty()) {
                toast("QR rak tidak terbaca");
                showScanLocation(selectedPallet);
                return;
            }
            verifyRackAndConfirm(selectedPallet, location);
            return;
        }

        if ("API_PICKING_RACK".equals(scanMode)) {
            processApiPickingScan(payload);
            return;
        }

        if ("API_INCOMING_NOTA".equals(scanMode)) {
            String nota = extractIncomingNota(payload);
            if (nota.isEmpty()) {
                toast("Nomor nota incoming tidak terbaca.");
                showApiIncoming();
                return;
            }
            loadApiIncoming(nota);
            return;
        }

        if ("API_PICKING_NOTA".equals(scanMode)) {
            String nota = extractPickingNota(payload);
            loadApiPicking(nota);
            return;
        }

        if ("RETURN_INBOUND".equals(scanMode)) {
            String reference = extractReturnReference(payload);
            if (reference.isEmpty()) {
                toast("Kode KPR atau request retur tidak terbaca dari QR.");
                showWarehouseReturn();
                return;
            }
            warehouseReturnReference = reference;
            showWarehouseReturn();
            return;
        }

        if ("QUARANTINE_MANIFEST".equals(scanMode)) {
            String query = extractQuarantineQuery(payload);
            if (query.isEmpty()) {
                toast("Nomor manifest atau referensi QC tidak terbaca.");
                showQuarantineQc();
                return;
            }
            quarantineQuery = query;
            quarantineFilterOpen = true;
            loadQuarantineQc();
            return;
        }

        if ("INVENTORY_BARCODE".equals(scanMode)) {
            loadInventoryBarcode(payload);
            return;
        }

        if ("TRANSFER_TARGET_RACK".equals(scanMode)) {
            String location = QrUtils.extractLocationCode(payload);
            if (selectedTransferStock == null) {
                toast("Pilih stok transfer terlebih dahulu");
                showTransferStocks();
                return;
            }
            if (location.isEmpty()) {
                toast("QR rak tujuan tidak terbaca");
                showTransferForm(selectedTransferStock);
                return;
            }
            int qty = pendingTransferQty > 0 ? pendingTransferQty : parsePositiveInt(first(selectedTransferStock, "qty"), 0);
            verifyTransferTarget(selectedTransferStock, location, qty);
            return;
        }

        if ("TRANSFER_TARGET_ITEM".equals(scanMode)) {
            processTransferTargetItem(payload);
            return;
        }

        if ("TRANSFER_FILTER_ITEM".equals(scanMode)) {
            processTransferFilterItem(payload);
            return;
        }

        if ("TRANSFER_VERIFY_ITEM".equals(scanMode)) {
            processTransferVerifyItem(payload);
            return;
        }

        if ("STOCK_OPNAME_PALLET".equals(scanMode)) {
            Pallet pallet = stockOpnamePalletFromScan(payload);
            if (pallet != null) {
                resolveStockOpnameLot(pallet);
            } else {
                toast("QR/barcode item tidak terbaca untuk stock opname.");
                showStockOpnameScanLocation(stockOpnameLocation);
            }
            return;
        }

        if ("SO_PICK_PALLET".equals(scanMode)) {
            Pallet pallet = scanPickingSoItemOrPallet(payload);
            if (pallet == null) {
                toast("QR picking tidak terbaca. Pastikan QR berisi kode barang dan rak.");
                showPickingSalesOrderList();
                return;
            }
            if (pallet != null) {
                showPickingSalesOrderForm(pallet);
            }
            return;
        }

        if ("LOADING_PALLET".equals(scanMode)) {
            Pallet pallet = scanPalletOrReturn(payload, () -> showVehicleLoading());
            if (pallet != null) {
                showVehicleLoadingForm(pallet);
            }
            return;
        }

        if ("DROPPING_PALLET".equals(scanMode)) {
            Pallet pallet = scanPalletOrReturn(payload, () -> showDroppingTransfer());
            if (pallet != null) {
                showDroppingForm(pallet);
            }
            return;
        }

        toast("Hasil scan: " + payload);
    }

    private void startScanner(String mode) {
        scanMode = mode;
        String prompt = "Scan QR " + ("PALLET".equals(mode) ? "Pallet" : "Lokasi Rak");
        if ("INVENTORY_BARCODE".equals(mode)) {
            prompt = "Scan QR/barcode barang atau QR rak";
        }
        if ("API_PICKING_RACK".equals(mode)) {
            prompt = "Scan QR barang/rak picking";
        }
        if ("API_INCOMING_NOTA".equals(mode)) {
            prompt = "Scan QR/barcode nota incoming";
        }
        if ("TRANSFER_TARGET_RACK".equals(mode)) {
            prompt = "Scan QR rak tujuan Tetap";
        }
        if ("TRANSFER_TARGET_ITEM".equals(mode)) {
            prompt = "Scan QR rak tujuan";
        }
        if ("TRANSFER_FILTER_ITEM".equals(mode)) {
            prompt = "Scan QR barang atau rak titipan asal";
        }
        if ("TRANSFER_VERIFY_ITEM".equals(mode)) {
            prompt = "Scan QR barang yang sudah dipindah";
        }
        if ("API_PICKING_NOTA".equals(mode)) {
            prompt = "Scan QR draf kiriman (DRF)";
        }
        if ("RETURN_INBOUND".equals(mode)) {
            prompt = "Scan QR KPR atau request retur";
        }
        if ("QUARANTINE_MANIFEST".equals(mode)) {
            prompt = "Scan QR/barcode manifest atau nota QC";
        }
        if ("REPRINT_PALLET".equals(mode)) {
            prompt = "Scan QR pallet untuk reprint";
        }
        if ("STOCK_OPNAME_PALLET".equals(mode)) {
            prompt = "Scan QR pallet untuk stock opname";
        }
        if ("SO_PICK_PALLET".equals(mode)) {
            prompt = "Scan QR barang/rak untuk picking SO";
        }
        if ("LOADING_PALLET".equals(mode)) {
            prompt = "Scan QR pallet masuk kendaraan";
        }
        if ("DROPPING_PALLET".equals(mode)) {
            prompt = "Scan QR pallet dropping gudang";
        }
        GmsBarcodeScannerOptions options = new GmsBarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                        Barcode.FORMAT_QR_CODE,
                        Barcode.FORMAT_CODE_128,
                        Barcode.FORMAT_CODE_39,
                        Barcode.FORMAT_CODE_93,
                        Barcode.FORMAT_EAN_13,
                        Barcode.FORMAT_EAN_8,
                        Barcode.FORMAT_UPC_A,
                        Barcode.FORMAT_UPC_E)
                .enableAutoZoom()
                .build();
        GmsBarcodeScanner scanner = GmsBarcodeScanning.getClient(this, options);
        scanner.startScan()
                .addOnSuccessListener(barcode -> {
                    String payload = barcode == null ? "" : barcode.getRawValue();
                    if (payload == null || payload.trim().isEmpty()) {
                        toast("Hasil scan kosong");
                        return;
                    }
                    handleScanResult(payload.trim());
                })
                .addOnCanceledListener(() -> toast("Scan dibatalkan"))
                .addOnFailureListener(e -> {
                    String message = e == null || e.getMessage() == null ? "coba lagi" : e.getMessage();
                    toast("Scanner Google belum bisa dibuka: " + message);
                });
    }

    private Pallet scanPalletOrReturn(String payload, Runnable fallbackScreen) {
        Pallet pallet = palletFromPickingQr(payload);
        if (pallet == null) {
            toast("QR tidak berisi data barang/rak yang lengkap.");
            if (fallbackScreen != null) {
                fallbackScreen.run();
            }
            return null;
        }
        return pallet;
    }

    private Pallet scanPickingSoItemOrPallet(String payload) {
        return palletFromPickingQr(payload);
    }

    private Pallet palletFromPickingQr(String payload) {
        if (payload == null || payload.trim().isEmpty()) {
            return null;
        }

        String text = payload.trim();
        String[] semicolon = text.split(";");
        String productCode = "";
        String rackCode = "";
        String qtyText = "";
        String productName = "";
        String batch = "";
        String expired = "";

        if (semicolon.length >= 2) {
            String firstPart = cleanQrPart(semicolon[0]);
            String secondPart = cleanQrPart(semicolon[1]);
            if (isRackCode(firstPart)) {
                rackCode = firstPart;
                productCode = secondPart;
            } else {
                productCode = firstPart;
                rackCode = secondPart;
            }
            qtyText = semicolon.length > 2 ? cleanQrPart(semicolon[2]) : "";
            productName = semicolon.length > 5 ? cleanQrPart(semicolon[5]) : "";
            batch = semicolon.length > 6 ? cleanQrPart(semicolon[6]) : "";
            expired = semicolon.length > 7 ? cleanQrPart(semicolon[7]) : "";
        }

        if (rackCode.isEmpty()) {
            Matcher rackMatcher = WMS_RACK_PATTERN.matcher(text);
            if (rackMatcher.find()) {
                rackCode = rackMatcher.group().toUpperCase(Locale.US);
            }
        }

        if (productCode.isEmpty()) {
            Matcher productMatcher = WMS_PRODUCT_PATTERN.matcher(text);
            while (productMatcher.find()) {
                String candidate = productMatcher.group().toUpperCase(Locale.US);
                if (!isRackCode(candidate)
                        && !"BUDIMAS-WMS".equals(candidate)
                        && !"LOCATION".equals(candidate)
                        && !"PALLET".equals(candidate)) {
                    productCode = candidate;
                    break;
                }
            }
        }

        if (productCode.isEmpty() || rackCode.isEmpty()) {
            return null;
        }

        Pallet pallet = new Pallet();
        pallet.code = productCode;
        pallet.incomingId = fallback(stageTwoQuery, "SO-PICK");
        pallet.skuName = fallback(productName, productCode);
        pallet.lotBatch = fallback(batch, "-");
        pallet.expDate = fallback(expired, "-");
        pallet.cartonCount = Math.max(1, parsePositiveInt(qtyText, 1));
        pallet.location = rackCode;
        pallet.status = Pallet.STATUS_STORED;
        pallet.storedAt = System.currentTimeMillis();
        return pallet;
    }

    private boolean isRackCode(String value) {
        return value != null && WMS_RACK_PATTERN.matcher(value.trim()).matches();
    }

    private String cleanQrPart(String value) {
        return value == null ? "" : value.trim();
    }

    private void addPalletActionList(String titleText, String action, PalletAction actionHandler, boolean storedOnly) {
        LinearLayout list = card();
        list.addView(sectionTitle(titleText));
        int shown = 0;
        for (Pallet pallet : store.getPallets()) {
            if (storedOnly && !Pallet.STATUS_STORED.equals(pallet.status)) {
                continue;
            }
            if (!matchesStageTwoPallet(pallet)) {
                continue;
            }
            list.addView(palletSmallRow(pallet, action, v -> actionHandler.onSelect(pallet)));
            shown++;
            if (shown >= 80) {
                break;
            }
        }
        if (shown == 0) {
            list.addView(body(storedOnly
                    ? "Belum ada pallet tersimpan sesuai filter. Putaway pallet dulu atau ubah pencarian."
                    : "Belum ada pallet sesuai filter."));
        }
        content.addView(list);
    }

    private LinearLayout palletDetailCard(Pallet pallet, String titleText) {
        LinearLayout detail = card();
        detail.addView(sectionTitle(titleText));
        detail.addView(keyValue("Kode Pallet", fallback(pallet.code, "-")));
        detail.addView(keyValue("SKU / Barang", fallback(pallet.skuName, "-")));
        detail.addView(keyValue("Qty Sistem", pallet.cartonCount + " Karton"));
        detail.addView(keyValue("Lot / Exp", fallback(pallet.lotBatch, "-") + " / " + fallback(pallet.expDate, "-")));
        detail.addView(keyValue("Lokasi", fallback(pallet.location, "-")));
        detail.addView(keyValue("Status", fallback(pallet.status, "-")));
        return detail;
    }

    private LinearLayout palletQrRow(Pallet pallet) {
        LinearLayout row = vertical();
        row.setPadding(dp(12), dp(12), dp(12), dp(12));
        row.setBackground(round(Color.rgb(248, 251, 255), Color.rgb(218, 228, 244), 10));
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowLp.setMargins(0, 0, 0, dp(10));
        row.setLayoutParams(rowLp);

        LinearLayout top = horizontal();
        ImageView qr = new ImageView(this);
        try {
            qr.setImageBitmap(QrUtils.createQrBitmap(pallet.toQrPayload(), 180));
        } catch (WriterException e) {
            qr.setBackgroundColor(Color.LTGRAY);
        }
        qr.setPadding(dp(5), dp(5), dp(5), dp(5));
        qr.setBackground(round(Color.WHITE, CARD_BORDER, 8));
        LinearLayout.LayoutParams qrLp = new LinearLayout.LayoutParams(dp(78), dp(78));
        qrLp.setMargins(0, 0, dp(12), 0);
        top.addView(qr, qrLp);

        LinearLayout text = vertical();
        TextView code = title(pallet.code, 16);
        code.setSingleLine(true);
        code.setEllipsize(TextUtils.TruncateAt.END);
        text.addView(code);
        TextView sku = small(safe(pallet.skuName));
        sku.setSingleLine(true);
        sku.setEllipsize(TextUtils.TruncateAt.END);
        text.addView(sku);

        LinearLayout meta = horizontal();
        meta.setPadding(0, dp(7), 0, 0);
        meta.addView(palletMetaChip(pallet.cartonCount + " Karton", GREEN));
        meta.addView(palletMetaChip("Lot " + fallback(pallet.lotBatch, "-"), INK));
        text.addView(meta);
        TextView exp = small("Exp: " + fallback(pallet.expDate, "-"));
        exp.setTextSize(11);
        text.addView(exp);
        top.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(top);

        LinearLayout buttons = horizontal();
        buttons.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        buttons.addView(compactAction("Cetak", v -> printPallet(pallet)));
        buttons.addView(compactAction("Bagikan", v -> sharePallet(pallet)));
        LinearLayout.LayoutParams buttonsLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        buttonsLp.setMargins(0, dp(10), 0, 0);
        row.addView(buttons, buttonsLp);
        return row;
    }

    private LinearLayout palletSmallRow(Pallet pallet, String action, View.OnClickListener listener) {
        LinearLayout row = horizontal();
        row.setPadding(0, dp(10), 0, dp(10));
        row.setBackground(separatorBackground());

        LinearLayout text = vertical();
        text.addView(title(pallet.code, 15));
        text.addView(small(safe(pallet.skuName) + " | " + pallet.cartonCount + " Karton"));
        text.addView(small("Lokasi: " + (pallet.location.isEmpty() ? "-" : pallet.location)));
        row.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(tinyButton(action, listener));
        return row;
    }

    private LinearLayout warehouseRow(Pallet pallet) {
        LinearLayout row = vertical();
        row.setPadding(0, dp(12), 0, dp(12));
        row.setBackground(separatorBackground());
        LinearLayout top = horizontal();

        LinearLayout text = vertical();
        text.addView(title(pallet.code, 15));
        text.addView(small(safe(pallet.skuName) + " | " + pallet.cartonCount + " Karton"));
        text.addView(small("Lot: " + safe(pallet.lotBatch) + " | Exp: " + safe(pallet.expDate)));
        text.addView(small("Lokasi: " + (pallet.location.isEmpty() ? "-" : pallet.location)));
        top.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView status = badge(pallet.status, Pallet.STATUS_STORED.equals(pallet.status) ? GREEN : AMBER);
        top.addView(status);
        row.addView(top);

        LinearLayout actions = horizontal();
        if (!Pallet.STATUS_STORED.equals(pallet.status)) {
            actions.addView(tinyButton("Putaway", v -> showScanLocation(pallet)));
        }
        actions.addView(tinyButton("Reprint QR", v -> {
            reprintQuery = pallet.code;
            showReprintQr();
        }));
        actions.addView(tinyButton("History", v -> {
            transactionQuery = pallet.code;
            transactionTypeFilter = "";
            showTransactions();
        }));
        row.addView(actions);
        return row;
    }

    private LinearLayout rackLocationRow(JSONObject rack, View.OnClickListener action) {
        LinearLayout row = vertical();
        row.setPadding(dp(12), dp(12), dp(12), dp(12));

        String code = first(rack, "kode_rak", "KodeRak");
        String type = fallback(first(rack, "type_rak", "shelf_type", "TipeRak"), "-");
        String status = fallback(first(rack, "status_rak", "status", "StatusRak"), "-");
        int qty = parseOptionalInt(rack, 0, "qty_pcs", "QtyPcs", "SaldoRak", "quantity", "stock_qty");
        String placements = fallback(first(rack, "placement_count", "PlacementCount"), "0");
        int rackColor = rackTypeColor(type);
        row.setBackground(round(colorWithAlpha(rackColor, 8), colorWithAlpha(rackColor, 74), 14));
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowLp.setMargins(0, 0, 0, dp(10));
        row.setLayoutParams(rowLp);

        LinearLayout top = horizontal();
        LinearLayout text = vertical();
        TextView title = title(fallback(code, "-"), 18);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        text.addView(title);
        text.addView(body("G" + fallback(first(rack, "gudang"), "-")
                + " | R" + fallback(first(rack, "rak"), "-")
                + " | L" + fallback(first(rack, "level"), "-")
                + " | K" + fallback(first(rack, "kolom"), "-")
                + " | N" + fallback(first(rack, "nomor_urut"), "-")));
        text.addView(small("Placement: " + placements));
        top.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        top.addView(stockQtyPanel(qty, -1, -1, rackColor));
        row.addView(top);

        LinearLayout meta = horizontal();
        meta.setPadding(0, dp(7), 0, 0);
        meta.addView(rackTypePill(type));
        int statusColor = "Nonaktif".equalsIgnoreCase(status) ? MUTED : ("Kosong".equalsIgnoreCase(status) ? AMBER : GREEN);
        meta.addView(priorityPill(status, statusColor));
        if (action != null) {
            meta.addView(compactAction("Pakai", action));
        } else if (!code.isEmpty()) {
            meta.addView(compactAction("Isi Rak", v -> {
                inventoryQuery = code;
                inventoryStatusFilter = "";
                showInventory();
            }));
            if ("Tetap".equalsIgnoreCase(type)) {
                meta.addView(compactAction("Tujuan", v -> loadTransferStocksForTarget("", code)));
            }
        }
        row.addView(meta);
        return row;
    }

    private LinearLayout inventoryRow(JSONObject item) {
        LinearLayout row = vertical();
        String rackType = inventoryRackType(item);
        int rackColor = rackTypeColor(rackType);
        row.setPadding(dp(12), dp(12), dp(12), dp(12));
        row.setBackground(round(colorWithAlpha(rackColor, 8), colorWithAlpha(rackColor, 74), 14));
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowLp.setMargins(0, 0, 0, dp(10));
        row.setLayoutParams(rowLp);

        LinearLayout top = horizontal();
        LinearLayout text = vertical();
        TextView code = title(fallback(first(item, "product_code", "KodeBarang", "kode_barang"), "-"), 18);
        code.setSingleLine(true);
        code.setEllipsize(TextUtils.TruncateAt.END);
        text.addView(code);
        TextView name = body(fallback(first(item, "product_name", "NamaBarang", "nama_barang"), "-"));
        name.setSingleLine(false);
        name.setMaxLines(2);
        name.setEllipsize(TextUtils.TruncateAt.END);
        text.addView(name);
        text.addView(body("Rak: " + fallback(first(item, "location", "kode_rak", "KodeRak"), "-")));
        String readyStatus = first(item, "wms_ready_status", "ready_picking_note");
        if (!readyStatus.isEmpty()) {
            text.addView(body(readyStatus));
        }
        text.addView(small("Batch " + inventoryBatch(item) + " | Exp " + inventoryExpired(item)));
        text.addView(small("Masuk " + inventoryIncomingDate(item) + " | Umur " + inventoryAgeText(item)));
        int mergedCount = parseOptionalInt(item, 1, "merged_count");
        if (mergedCount > 1) {
            text.addView(small("Total gabungan dari " + mergedCount + " baris rak yang sama."));
        }
        top.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        top.addView(stockQtyPanel(inventoryQtyPcs(item), item, rackColor));
        row.addView(top);

        row.addView(stockStatusFooter(inventoryFefoLabel(item), inventoryFifoLabel(item), rackType));
        return row;
    }

    private void addLocalTransactionsCard() {
        LinearLayout local = card();
        local.addView(sectionTitle("Riwayat Lokal Pallet / Stok"));
        local.addView(body("Catatan dari proses mobile: siapa, kapan, rak asal, rak tujuan, dan qty."));

        int shown = 0;
        for (WmsTransaction tx : store.getTransactions()) {
            if (matchesLocalTransaction(tx)) {
                local.addView(localTransactionRow(tx));
                shown++;
            }
            if (shown >= 80) {
                break;
            }
        }
        if (shown == 0) {
            local.addView(space(8));
            local.addView(body("Belum ada riwayat lokal sesuai filter."));
        }
        content.addView(local);
    }

    private LinearLayout localTransactionRow(WmsTransaction tx) {
        LinearLayout row = vertical();
        row.setPadding(0, dp(12), 0, dp(12));
        row.setBackground(separatorBackground());

        LinearLayout top = horizontal();
        LinearLayout text = vertical();
        String code = !safe(tx.palletCode).isEmpty() ? tx.palletCode : fallback(tx.stockCode, "-");
        TextView title = title(fallback(tx.process, "Transaksi") + " | " + code, 15);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        text.addView(title);
        TextView product = small(fallback(tx.productName, "-"));
        product.setSingleLine(true);
        product.setEllipsize(TextUtils.TruncateAt.END);
        text.addView(product);
        text.addView(small("Dari: " + fallback(tx.fromRack, "-") + " | Ke: " + fallback(tx.toRack, "-")));
        top.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        top.addView(badge(fallback(tx.process, "-"), transactionColorForProcess(tx.process)));
        row.addView(top);

        LinearLayout meta = horizontal();
        meta.setPadding(0, dp(8), 0, 0);
        meta.addView(palletMetaChip(tx.qty + " " + fallback(tx.unit, "PCS"), tx.qty > 0 ? GREEN : AMBER));
        meta.addView(palletMetaChip(fallback(tx.userName, "User"), BLUE));
        row.addView(meta);
        row.addView(small("Ref: " + fallback(tx.referenceNo, "-") + " | Waktu: " + formatDateTime(tx.createdAt)));
        if (!safe(tx.note).isEmpty()) {
            row.addView(small(tx.note));
        }
        return row;
    }

    private LinearLayout transactionRow(JSONObject tx) {
        LinearLayout row = vertical();
        row.setPadding(0, dp(12), 0, dp(12));
        row.setBackground(separatorBackground());

        String type = fallback(first(tx, "JenisTransaksi", "jenis_transaksi"), "-").toUpperCase(Locale.US);
        String label = transactionTypeLabel(type);
        int color = transactionTypeColor(type);
        int masuk = parsePositiveInt(first(tx, "Masuk", "masuk"), 0);
        int keluar = parsePositiveInt(first(tx, "Keluar", "keluar"), 0);

        LinearLayout top = horizontal();
        LinearLayout text = vertical();
        TextView nota = title("#" + fallback(first(tx, "Nota", "reference_no"), "-"), 15);
        nota.setSingleLine(true);
        nota.setEllipsize(TextUtils.TruncateAt.END);
        text.addView(nota);
        TextView name = small(fallback(first(tx, "NamaBarang", "nama_barang"), "-"));
        name.setSingleLine(true);
        name.setEllipsize(TextUtils.TruncateAt.END);
        text.addView(name);
        text.addView(small("Rak: " + fallback(first(tx, "KodeRak", "kode_rak"), "-")
                + " | Kode: " + fallback(first(tx, "KodeBarang", "kode_barang"), "-")));
        top.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        top.addView(badge(label, color));
        row.addView(top);

        LinearLayout meta = horizontal();
        meta.setPadding(0, dp(8), 0, 0);
        meta.addView(palletMetaChip((masuk > 0 ? "+" + masuk : "-" + keluar) + " PCS", masuk > 0 ? GREEN : AMBER));
        meta.addView(palletMetaChip("User " + fallback(first(tx, "UserAdd", "user_id"), "-"), BLUE));
        row.addView(meta);
        row.addView(small("Waktu: " + fallback(first(tx, "UrutTanggal", "created_at"), "-")));
        return row;
    }

    private LinearLayout loadingScheduleRow(JSONArray scheduleItems) {
        LinearLayout row = vertical();
        row.setPadding(dp(12), dp(12), dp(12), dp(12));
        row.setBackground(round(colorWithAlpha(BLUE, 8), colorWithAlpha(BLUE, 64), 14));
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowLp.setMargins(0, 0, 0, dp(10));
        row.setLayoutParams(rowLp);

        JSONObject item = scheduleItems == null ? null : scheduleItems.optJSONObject(0);
        if (item == null) {
            row.addView(body("Data jadwal loading tidak tersedia."));
            return row;
        }
        boolean scheduled = isScheduledLoadingItem(item);
        int totalQty = loadingGroupQty(scheduleItems);

        LinearLayout top = horizontal();
        LinearLayout text = vertical();
        TextView nota = title(scheduled ? "Jadwal Armada" : "Jadwal Belum Lengkap", 16);
        nota.setSingleLine(true);
        nota.setEllipsize(TextUtils.TruncateAt.END);
        text.addView(nota);
        TextView name = small("Tanggal: " + fallback(first(item, "ScheduledDeliveryDate", "scheduled_delivery_date"), "-"));
        name.setSingleLine(true);
        name.setEllipsize(TextUtils.TruncateAt.END);
        text.addView(name);
        text.addView(small("Driver: " + fallback(first(item, "DriverRencana", "driver_name"), "-")
                + " | Armada: " + fallback(first(item, "ArmadaRencana", "armada_name", "vehicle_no"), "-")));
        text.addView(small("Helper: " + fallback(first(item, "HelperRencana", "helper_name"), "-")));
        top.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        top.addView(badge(scheduled ? "TERJADWAL" : "LENGKAPI", scheduled ? GREEN : AMBER));
        row.addView(top);

        LinearLayout meta = horizontal();
        meta.setPadding(0, dp(8), 0, 0);
        meta.addView(palletMetaChip(scheduleItems.length() + " Item", BLUE));
        meta.addView(palletMetaChip(totalQty + " PCS", GREEN));
        row.addView(meta);
        row.addView(small("Contoh nota: " + fallback(first(item, "Nota", "NoFaktur"), "-")
                + " | Referensi: " + fallback(first(item, "CalonNoManifest"), "Dibuat oleh server")));
        if (scheduled) {
            row.addView(compactAction("Buka Jadwal Loading", v -> showLoadingConfirm(scheduleItems)));
        } else {
            TextView warning = small("Driver, armada, atau helper belum ditetapkan pada Penjadwalan Armada.");
            warning.setTextColor(AMBER);
            row.addView(warning);
        }
        return row;
    }

    private LinearLayout transferStockRow(JSONObject item) {
        LinearLayout row = vertical();
        String rackType = fallback(first(item, "type_rak", "shelf_type"), "Titipan");
        int rackColor = rackTypeColor(rackType);
        row.setPadding(dp(12), dp(12), dp(12), dp(12));
        row.setBackground(round(colorWithAlpha(rackColor, 8), colorWithAlpha(rackColor, 74), 14));
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowLp.setMargins(0, 0, 0, dp(10));
        row.setLayoutParams(rowLp);

        int qty = transferStockQty(item);

        LinearLayout top = horizontal();
        LinearLayout text = vertical();
        TextView name = title(fallback(first(item, "nama_barang", "NamaBarang"), "-"), 18);
        name.setSingleLine(false);
        name.setMaxLines(2);
        name.setEllipsize(TextUtils.TruncateAt.END);
        text.addView(name);
        TextView code = body(fallback(first(item, "kode_barang", "KodeBarang"), "-"));
        code.setTextColor(BLUE);
        code.setTypeface(Typeface.DEFAULT_BOLD);
        code.setSingleLine(true);
        code.setEllipsize(TextUtils.TruncateAt.END);
        text.addView(code);
        text.addView(body("Rak titipan: " + fallback(first(item, "kode_rak_asal", "KodeRakAsal"), "-")));
        text.addView(small("Batch " + transferStockBatch(item) + " | Exp " + transferStockExpired(item)));
        text.addView(small("Masuk " + transferStockIncomingDate(item) + " | Umur " + transferStockAgeText(item)));
        top.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        top.addView(stockQtyPanel(qty, item, rackColor));
        row.addView(top);

        row.addView(stockStatusFooter(transferStockFefoLabel(item), transferStockFifoLabel(item), rackType));

        Button action = primaryButton(pendingTransferTargetRack.isEmpty() ? "Pilih Barang Ini" : "Scan Barang Dipindah", v -> {
            if (pendingTransferTargetRack.isEmpty()) {
                showTransferForm(item);
            } else {
                selectedTransferStock = item;
                pendingTransferQty = transferStockQty(item);
                startScanner("TRANSFER_VERIFY_ITEM");
            }
        });
        row.addView(action);
        return row;
    }

    private boolean matchesWarehouseFilter(Pallet pallet) {
        if ("STORED".equals(warehouseFilter) && !Pallet.STATUS_STORED.equals(pallet.status)) {
            return false;
        }
        if ("WAITING".equals(warehouseFilter) && Pallet.STATUS_STORED.equals(pallet.status)) {
            return false;
        }
        String query = warehouseQuery == null ? "" : warehouseQuery.trim().toUpperCase(Locale.US);
        if (query.isEmpty()) {
            return true;
        }
        String haystack = (safe(pallet.code) + " " + safe(pallet.skuName) + " " + safe(pallet.lotBatch) + " " + safe(pallet.location))
                .toUpperCase(Locale.US);
        return haystack.contains(query);
    }

    private boolean matchesStageTwoPallet(Pallet pallet) {
        String query = safe(stageTwoQuery).trim().toUpperCase(Locale.US);
        if (query.contains("|")) {
            query = "";
        }
        if (query.isEmpty()) {
            return true;
        }
        String haystack = (safe(pallet.code) + " " + safe(pallet.skuName) + " " + safe(pallet.lotBatch) + " " + safe(pallet.location))
                .toUpperCase(Locale.US);
        return haystack.contains(query);
    }

    private void addRecentPallets(LinearLayout parent, List<Pallet> pallets) {
        int limit = Math.min(3, pallets.size());
        if (limit == 0) {
            return;
        }
        parent.addView(space(8));
        parent.addView(sectionTitle("Pallet Terbaru"));
        for (int i = 0; i < limit; i++) {
            Pallet pallet = pallets.get(i);
            parent.addView(palletSmallRow(pallet, Pallet.STATUS_STORED.equals(pallet.status) ? "Lihat" : "Putaway", v -> {
                if (Pallet.STATUS_STORED.equals(pallet.status)) {
                    warehouseQuery = pallet.code;
                    showWarehouseList();
                } else {
                    showScanLocation(pallet);
                }
            }));
        }
    }

    private void printPallet(Pallet pallet) {
        try {
            Bitmap bitmap = isBluetoothPrintMode()
                    ? QrUtils.createPalletThermalLabel(pallet, printerSettings.getPaperWidth())
                    : QrUtils.createPalletLabel(pallet);
            printBitmap("QR " + pallet.code, bitmap);
        } catch (WriterException e) {
            toast("Gagal membuat QR");
        }
    }

    private void printPallets(List<Pallet> pallets) {
        try {
            Bitmap bitmap = isBluetoothPrintMode()
                    ? QrUtils.createPalletThermalSheet(pallets, printerSettings.getPaperWidth())
                    : QrUtils.createPalletSheet(pallets);
            printBitmap("QR Pallet Budimas WMS", bitmap);
        } catch (WriterException e) {
            toast("Gagal membuat QR");
        }
    }

    private void printApiQr(String title, String qrData) {
        try {
            Bitmap bitmap = isBluetoothPrintMode()
                    ? QrUtils.createApiQrThermalLabel(title, qrData, printerSettings.getPaperWidth())
                    : QrUtils.createQrBitmap(qrData, 900);
            printBitmap(title, bitmap);
        } catch (WriterException e) {
            toast("Gagal membuat QR dari API");
        }
    }

    private void printBitmap(String jobName, Bitmap bitmap) {
        if (isBluetoothPrintMode()) {
            printBluetoothBitmap(jobName, bitmap);
            return;
        }

        PrintHelper helper = new PrintHelper(this);
        helper.setScaleMode(PrintHelper.SCALE_MODE_FIT);
        helper.printBitmap(jobName, bitmap);
    }

    private void printBluetoothTest() {
        if (!isBluetoothPrintMode()) {
            showSettingsWithDevices(null, "Pilih mode Bluetooth ESC/POS dulu untuk tes cetak langsung.");
            return;
        }

        try {
            printBitmap("Tes Printer Budimas WMS", QrUtils.createPrinterTestLabel(printerSettings.getPaperWidth(), printerSettings.summary()));
        } catch (WriterException e) {
            toast("Gagal membuat label test");
        }
    }

    private boolean isBluetoothPrintMode() {
        return "Bluetooth ESC/POS".equals(printerSettings.getMode());
    }

    private boolean shouldAutoPrintBluetooth() {
        return printerSettings.isAutoPrint() && isBluetoothPrintMode();
    }

    private void printBluetoothBitmap(String jobName, Bitmap bitmap) {
        String address = printerSettings.getAddress().trim();
        if (address.isEmpty()) {
            showSettingsWithDevices(null, "Pilih atau tautkan printer Bluetooth dulu sebelum cetak.");
            return;
        }

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            showSettingsWithDevices(null, "Perangkat ini tidak mendukung Bluetooth.");
            return;
        }

        if (!ensureBluetoothPermissions(false)) {
            return;
        }

        try {
            if (!adapter.isEnabled()) {
                requestEnableBluetooth();
                return;
            }
        } catch (SecurityException e) {
            showSettingsWithDevices(null, "Akses Bluetooth belum diizinkan.");
            return;
        }

        String printerName = printerSettings.summary();
        toast("Mengirim " + jobName + " ke printer...");
        executor.execute(() -> {
            try {
                BluetoothEscPosPrinter.printBitmap(address, printerSettings.getPaperWidth(), bitmap);
                uiHandler.post(() -> toast("Cetak dikirim ke " + printerName));
            } catch (Exception e) {
                uiHandler.post(() -> showSettingsWithDevices(null, "Cetak gagal: " + fallback(e.getMessage(), "printer tidak merespons.")));
            }
        });
    }

    private void sharePallet(Pallet pallet) {
        try {
            Bitmap bitmap = QrUtils.createPalletLabel(pallet);
            File dir = new File(getCacheDir(), "shared_qr");
            if (!dir.exists() && !dir.mkdirs()) {
                toast("Gagal menyiapkan file QR");
                return;
            }
            File file = new File(dir, pallet.code + ".png");
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();

            Uri uri = FileProvider.getUriForFile(this, getString(R.string.file_provider_authority), file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("image/png");
            intent.putExtra(Intent.EXTRA_SUBJECT, "QR Pallet " + pallet.code);
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Bagikan QR Pallet"));
        } catch (Exception e) {
            toast("Gagal membagikan QR");
        }
    }

    private void showLoadingScreen(String title, String message) {
        setShell(title, message, "dashboard", true);
        LinearLayout loading = card();
        loading.setGravity(Gravity.CENTER_HORIZONTAL);
        loading.addView(sectionTitle("Memproses"));
        loading.addView(body(message));
        content.addView(loading);
    }

    private void showApiError(String title, String fallbackMessage, Exception error, Runnable retry) {
        setShell(title, fallbackMessage, "dashboard", true);
        LinearLayout card = card();
        card.setBackground(dangerBackground());
        card.addView(sectionTitle("Gagal"));
        card.addView(body(operatorErrorMessage(error, fallbackMessage)));
        card.addView(primaryButton("Coba Lagi", v -> retry.run()));
        content.addView(card);
    }

    private String operatorErrorMessage(Exception error, String fallbackMessage) {
        String message = error == null || error.getMessage() == null ? "" : error.getMessage().trim();
        String lower = message.toLowerCase(Locale.US);

        if (lower.contains("failed to connect") || lower.contains("timeout") || lower.contains("timed out")
                || lower.contains("connection refused") || lower.contains("unable to resolve host")) {
            return "Koneksi ke server WMS terputus. Periksa internet/perangkat, lalu tekan Coba Lagi.";
        }

        if (error instanceof BudimasApiClient.ApiException) {
            int status = ((BudimasApiClient.ApiException) error).statusCode;
            String clean = cleanApiMessage(message);
            if (status == 401 || status == 403) {
                return "Sesi login tidak valid atau akses belum diberikan. Logout, login ulang, lalu coba proses lagi.";
            }
            if (status == 404) {
                return "Data tidak ditemukan di WMS. Cek nomor nota, kode barang, atau kode rak lalu muat ulang.";
            }
            if (status == 409) {
                return "Data belum bisa disimpan karena aturan WMS. " + clean;
            }
            if (status == 423) {
                // A 423 response is an intentional operational lock (for
                // example an active stock opname), not a stock shortage.
                // Preserve the safe server explanation so the operator knows
                // which workflow must be completed before trying again.
                return clean;
            }
            if (status == 400 || status == 422) {
                return "Data belum lengkap atau tidak valid. " + clean;
            }
            if (status >= 500) {
                // API only uses this prefix for a vetted, operator-safe
                // message (for example an unapplied WMS schema update). Do
                // not surface arbitrary server exception content to a device.
                if (clean.startsWith("WMS-OPERASIONAL:")) {
                    return clean.substring("WMS-OPERASIONAL:".length()).trim();
                }
                return "Server WMS sedang bermasalah. Jangan ulang input berkali-kali; tunggu sebentar lalu coba lagi.";
            }
        }

        if (lower.contains("stok") || lower.contains("stock")) {
            return "Stok tidak cukup atau stok belum tersedia di rak. Cek Inventory/History lalu ulangi proses.";
        }
        if (lower.contains("rak")) {
            return "Rak belum sesuai. Pastikan rak aktif, tipe rak benar, lalu scan/input ulang.";
        }
        if (lower.contains("qty") || lower.contains("quantity")) {
            return "Qty belum valid. Isi qty lebih dari 0 dan pastikan tidak melebihi stok.";
        }

        return fallback(cleanApiMessage(message), fallbackMessage);
    }

    private String cleanApiMessage(String message) {
        String clean = fallback(message, "Cek data input, lalu coba lagi.").trim();
        if (clean.matches("(?i)^HTTP\\s+\\d+$")) {
            return "Cek data input, lalu coba lagi.";
        }
        return clean;
    }

    private JSONArray normalizeRows(JSONObject response) {
        if (response == null) {
            return new JSONArray();
        }
        JSONArray items = response.optJSONArray("items");
        if (items != null) {
            return items;
        }
        JSONArray rows = response.optJSONArray("rows");
        if (rows != null) {
            return rows;
        }
        JSONArray data = response.optJSONArray("data");
        if (data != null) {
            return data;
        }
        JSONArray value = response.optJSONArray("value");
        if (value != null) {
            return value;
        }
        JSONObject pages = response.optJSONObject("pages");
        if (pages != null) {
            JSONArray result = pages.optJSONArray("result");
            if (result != null) {
                return result;
            }
        }
        JSONArray result = response.optJSONArray("result");
        return result == null ? new JSONArray() : result;
    }

    private boolean containsRackCode(JSONArray rows, String location) {
        String expected = safe(location).trim().toUpperCase(Locale.US);
        if (expected.isEmpty() || rows == null) {
            return false;
        }
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.optJSONObject(i);
            String code = first(row, "kode_rak", "KodeRak").toUpperCase(Locale.US);
            if (expected.equals(code)) {
                return true;
            }
        }
        return false;
    }

    private JSONObject rowsAsResponse(JSONObject row) {
        JSONObject response = new JSONObject();
        JSONArray rows = new JSONArray();
        rows.put(row);
        try {
            response.put("items", rows);
        } catch (Exception ignored) {
        }
        return response;
    }

    private String first(JSONObject row, String... keys) {
        return AuthSession.firstString(row, keys);
    }

    private String extractSemicolonPart(String payload, int index) {
        if (payload == null || index < 0) {
            return "";
        }
        String[] parts = payload.split(";");
        if (index >= parts.length) {
            return "";
        }
        return parts[index] == null ? "" : parts[index].trim().toUpperCase(Locale.US);
    }

    private String extractIncomingNota(String payload) {
        if (payload == null) {
            return "";
        }
        String text = payload.trim();
        if (text.isEmpty()) {
            return "";
        }

        String[] pipeParts = text.split("\\|");
        if (pipeParts.length >= 3 && "BUDIMAS-WMS".equalsIgnoreCase(pipeParts[0])) {
            text = pipeParts[pipeParts.length - 1].trim();
        }

        int queryIndex = text.indexOf('?');
        if (queryIndex >= 0 && queryIndex + 1 < text.length()) {
            String query = text.substring(queryIndex + 1);
            for (String part : query.split("&")) {
                String[] pair = part.split("=", 2);
                if (pair.length == 2 && (
                        "nota".equalsIgnoreCase(pair[0])
                                || "no_transaksi".equalsIgnoreCase(pair[0])
                                || "reference_no".equalsIgnoreCase(pair[0])
                                || "ref".equalsIgnoreCase(pair[0])
                )) {
                    return pair[1].trim();
                }
            }
        }

        String normalized = text
                .replace("Nota Incoming:", "")
                .replace("NOTA INCOMING:", "")
                .replace("No Transaksi:", "")
                .replace("NO TRANSAKSI:", "")
                .replace("Nota:", "")
                .replace("NOTA:", "")
                .trim();
        if (normalized.contains(";")) {
            normalized = normalized.split(";")[0].trim();
        }
        return normalized;
    }

    private String extractPickingNota(String payload) {
        return ShipmentReference.parse(payload);
    }

    private String extractQuarantineQuery(String payload) {
        if (payload == null) {
            return "";
        }
        String text = payload.trim();
        if (text.isEmpty()) {
            return "";
        }

        // Labels printed by WMS can carry the manifest inside a richer QR
        // payload. Prefer the explicit manifest token so the API receives the
        // same value used by the ERP QC search field.
        Matcher manifestMatcher = WMS_MANIFEST_PATTERN.matcher(text);
        if (manifestMatcher.find()) {
            return manifestMatcher.group().trim();
        }

        int queryIndex = text.indexOf('?');
        if (queryIndex >= 0 && queryIndex + 1 < text.length()) {
            String query = text.substring(queryIndex + 1);
            for (String part : query.split("&")) {
                String[] pair = part.split("=", 2);
                if (pair.length != 2) {
                    continue;
                }
                String key = pair[0].trim();
                if ("manifest".equalsIgnoreCase(key)
                        || "no_manifest".equalsIgnoreCase(key)
                        || "reference_no".equalsIgnoreCase(key)
                        || "no_faktur".equalsIgnoreCase(key)
                        || "nota".equalsIgnoreCase(key)
                        || "kode_kpr".equalsIgnoreCase(key)
                        || "ref".equalsIgnoreCase(key)) {
                    try {
                        return java.net.URLDecoder.decode(pair[1], "UTF-8").trim();
                    } catch (Exception ignored) {
                        return pair[1].trim();
                    }
                }
            }
        }

        String[] pipeParts = text.split("\\|");
        if (pipeParts.length >= 3 && "BUDIMAS-WMS".equalsIgnoreCase(pipeParts[0])) {
            String lastPart = pipeParts[pipeParts.length - 1].trim();
            if (!lastPart.isEmpty()) {
                return lastPart;
            }
        }

        String normalized = text.replaceFirst("(?i)^(?:no\\s*manifest|manifest|nota|faktur|kpr|referensi)\\s*[:=-]?\\s*", "").trim();
        if (normalized.contains(";")) {
            normalized = normalized.split(";", 2)[0].trim();
        }
        return normalized;
    }

    private String extractReturnReference(String payload) {
        if (payload == null) {
            return "";
        }
        String text = payload.trim();
        if (text.isEmpty()) {
            return "";
        }

        // A KPR label may contain product text below its QR. Prefer the exact
        // KPR token instead of posting the complete printed label to the API.
        Matcher kprMatcher = WMS_RETURN_PATTERN.matcher(text);
        if (kprMatcher.find()) {
            return kprMatcher.group().trim();
        }

        String[] pipeParts = text.split("\\|");
        if (pipeParts.length >= 3 && "BUDIMAS-WMS".equalsIgnoreCase(pipeParts[0])) {
            String candidate = decodeQrValue(pipeParts[pipeParts.length - 1]);
            Matcher candidateMatcher = WMS_RETURN_PATTERN.matcher(candidate);
            if (candidateMatcher.find()) {
                return candidateMatcher.group().trim();
            }
            if (!candidate.trim().isEmpty()) {
                return candidate.trim();
            }
        }

        int queryIndex = text.indexOf('?');
        if (queryIndex >= 0 && queryIndex + 1 < text.length()) {
            String query = text.substring(queryIndex + 1);
            for (String part : query.split("&")) {
                String[] pair = part.split("=", 2);
                if (pair.length != 2) {
                    continue;
                }
                String key = pair[0].trim();
                if ("kode_kpr".equalsIgnoreCase(key)
                        || "kpr".equalsIgnoreCase(key)
                        || "kode_request".equalsIgnoreCase(key)
                        || "request".equalsIgnoreCase(key)
                        || "id_request".equalsIgnoreCase(key)
                        || "reference_no".equalsIgnoreCase(key)
                        || "ref".equalsIgnoreCase(key)) {
                    String candidate = decodeQrValue(pair[1]);
                    Matcher candidateMatcher = WMS_RETURN_PATTERN.matcher(candidate);
                    if (candidateMatcher.find()) {
                        return candidateMatcher.group().trim();
                    }
                    if (!candidate.isEmpty()) {
                        return candidate;
                    }
                }
            }
        }

        String normalized = text
                .replaceFirst("(?i)^(?:kode\\s*)?(?:kpr|request\\s*retur|retur|referensi)\\s*[:=-]?\\s*", "")
                .trim();
        if (normalized.contains(";")) {
            normalized = normalized.split(";", 2)[0].trim();
        }
        Matcher normalizedMatcher = WMS_RETURN_PATTERN.matcher(normalized);
        if (normalizedMatcher.find()) {
            return normalizedMatcher.group().trim();
        }
        return normalized;
    }

    private String decodeQrValue(String value) {
        try {
            return java.net.URLDecoder.decode(value == null ? "" : value, "UTF-8").trim();
        } catch (Exception ignored) {
            return value == null ? "" : value.trim();
        }
    }

    private boolean validateReceivingRecord(IncomingRecord record) {
        if (safe(record.poNumber).isEmpty()) {
            toast("No. PO / SJ wajib diisi sebelum pallet dibuat.");
            return false;
        }
        if (safe(record.supplier).isEmpty()) {
            toast("Supplier wajib diisi sebelum pallet dibuat.");
            return false;
        }
        if (safe(record.skuName).isEmpty() || safe(record.lotBatch).isEmpty()) {
            toast("SKU/barang dan lot wajib diisi sebelum pallet dibuat.");
            return false;
        }
        if (record.totalCartons <= 0) {
            toast("Total datang harus lebih dari 0 karton.");
            return false;
        }
        if (record.capacityPerPallet <= 0) {
            toast("Kapasitas pallet harus lebih dari 0 karton.");
            return false;
        }
        return true;
    }

    private void recordLocalTransaction(String process, String referenceNo, String palletCode, String stockCode, String productName, String fromRack, String toRack, int qty, String unit, String note) {
        // Riwayat lokal dinonaktifkan; semua transaksi dibaca ulang dari API server.
    }

    private String currentUserName() {
        return fallback(authSession.getUserName(), "User WMS");
    }

    private String currentUserId() {
        JSONObject user = authSession.getUser();
        return first(user, "id_user", "id", "user_id", "uid");
    }

    private String currentBranchId() {
        JSONObject user = authSession.getUser();
        return first(user, "id_cabang", "cabang_id", "branch_id");
    }

    private String currentCompanyId() {
        JSONObject user = authSession.getUser();
        return first(user, "id_perusahaan", "perusahaan_id", "company_id");
    }

    private String currentBranchName() {
        JSONObject user = authSession.getUser();
        return first(user, "nama_cabang", "cabang", "branch_name", "kode_cabang");
    }

    private void putIfNotEmpty(JSONObject object, String key, String value) {
        if (object == null || key == null || value == null || value.trim().isEmpty()) {
            return;
        }
        try {
            object.put(key, value.trim());
        } catch (Exception ignored) {
        }
    }

    private int parseOptionalInt(JSONObject row, int fallback, String... keys) {
        if (row == null) {
            return fallback;
        }
        for (String key : keys) {
            if (!row.has(key)) {
                continue;
            }
            Object raw = row.opt(key);
            if (raw instanceof Number) {
                return Math.max(0, ((Number) raw).intValue());
            }
            String text = String.valueOf(raw == null ? "" : raw).trim();
            if (text.isEmpty() || "null".equalsIgnoreCase(text)) {
                continue;
            }
            try {
                return Math.max(0, (int) Math.round(Double.parseDouble(text)));
            } catch (Exception ignored) {
            }
        }
        return fallback;
    }

    private JSONObject findRackByCode(JSONArray rows, String location) {
        String expected = safe(location).trim().toUpperCase(Locale.US);
        if (expected.isEmpty() || rows == null) {
            return null;
        }
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.optJSONObject(i);
            String code = first(row, "kode_rak", "KodeRak").toUpperCase(Locale.US);
            if (expected.equals(code)) {
                return row;
            }
        }
        return null;
    }

    private String rackValidationProblem(JSONObject rack, String rackCode, String requiredType) {
        String code = fallback(rackCode, "-");
        if (rack == null) {
            return "Rak " + code + " tidak ditemukan di master rak. Cek kode rak atau muat ulang data rak.";
        }
        String status = first(rack, "status_rak", "status", "StatusRak");
        String active = first(rack, "active", "Active", "is_active");
        if ("Nonaktif".equalsIgnoreCase(status) || "inactive".equalsIgnoreCase(status)
                || "false".equalsIgnoreCase(active) || "0".equals(active) || "no".equalsIgnoreCase(active)) {
            return "Rak " + code + " tidak aktif. Pilih rak aktif dari master rak.";
        }
        if (!safe(requiredType).isEmpty()) {
            String type = first(rack, "type_rak", "shelf_type", "TipeRak");
            if (!requiredType.equalsIgnoreCase(type)) {
                return "Rak " + code + " harus tipe " + requiredType + ". Pilih Rak Tetap aktif; Rak Lorong dipakai otomatis oleh sistem jika Rak Tetap penuh.";
            }
        }
        return "";
    }

    private String transferStockValidationProblem(JSONObject stock, int qty) {
        if (stock == null) {
            return "Pilih stok transfer terlebih dahulu.";
        }
        String code = first(stock, "kode_barang", "KodeBarang");
        String name = first(stock, "nama_barang", "NamaBarang");
        String sourceRack = first(stock, "kode_rak_asal", "KodeRakAsal");
        int availableQty = parseOptionalInt(stock, 0, "qty", "qty_pcs");
        if (code.isEmpty() || name.isEmpty()) {
            return "Barang transfer belum jelas. Muat ulang daftar stok transfer.";
        }
        if (sourceRack.isEmpty()) {
            return "Rak asal transfer belum jelas. Muat ulang stok transfer.";
        }
        String sourceType = first(stock, "type_rak", "shelf_type");
        if (!sourceType.isEmpty() && "Tetap".equalsIgnoreCase(sourceType)) {
            return "Rak asal tidak boleh Rak Tetap. Pilih stok dari Rak Titipan atau Rak Lorong.";
        }
        if (availableQty <= 0) {
            return "Stok asal tidak tersedia untuk ditransfer.";
        }
        if (qty <= 0) {
            return "Qty transfer wajib lebih dari 0.";
        }
        if (qty > availableQty) {
            return "Qty transfer melebihi saldo asal. Saldo tersedia " + availableQty + " PCS.";
        }
        return "";
    }

    private int loadingQty(JSONObject item) {
        int direct = Math.max(parseOptionalInt(item, 0, "required_quantity"), parseOptionalInt(item, 0, "picked_quantity"));
        if (direct > 0) {
            return direct;
        }
        int perUnit = Math.max(1, parseOptionalInt(item, 1, "PerUnit", "per_unit"));
        int unit = parseOptionalInt(item, 0, "Unit");
        int pieces = parseOptionalInt(item, 0, "Satuan");
        return unit * perUnit + pieces;
    }

    private String loadingValidationProblem(JSONObject item, String manifest) {
        if (item == null) {
            return "Pilih item loading terlebih dahulu.";
        }
        if (safe(manifest).isEmpty()) {
            return "No manifest loading wajib diisi.";
        }
        if (first(item, "Kode", "KodeBarang").isEmpty()) {
            return "Kode barang loading belum jelas. Muat ulang daftar loading.";
        }
        if (first(item, "Nama", "NamaBarang", "product_name").isEmpty()) {
            return "Nama barang loading belum jelas. Muat ulang daftar loading.";
        }
        if (loadingQty(item) <= 0) {
            return "Qty loading harus lebih dari 0.";
        }
        return "";
    }

    private boolean matchesReprintFilter(Pallet pallet) {
        String query = safe(reprintQuery).trim().toUpperCase(Locale.US);
        if (query.isEmpty()) {
            return true;
        }
        String haystack = (safe(pallet.code) + " " + safe(pallet.skuName) + " " + safe(pallet.lotBatch) + " " + safe(pallet.location))
                .toUpperCase(Locale.US);
        return haystack.contains(query);
    }

    private boolean matchesLocalTransaction(WmsTransaction tx) {
        if (tx == null) {
            return false;
        }
        if (!localProcessMatchesFilter(tx.process, transactionTypeFilter)) {
            return false;
        }
        String query = safe(transactionQuery).trim().toUpperCase(Locale.US);
        if (query.isEmpty()) {
            return true;
        }
        String haystack = (safe(tx.process) + " " + safe(tx.referenceNo) + " " + safe(tx.palletCode) + " "
                + safe(tx.stockCode) + " " + safe(tx.productName) + " " + safe(tx.fromRack) + " " + safe(tx.toRack) + " " + safe(tx.userName))
                .toUpperCase(Locale.US);
        return haystack.contains(query);
    }

    private boolean localProcessMatchesFilter(String process, String filter) {
        String cleanFilter = safe(filter).toUpperCase(Locale.US);
        if (cleanFilter.isEmpty()) {
            return true;
        }
        String cleanProcess = safe(process).toUpperCase(Locale.US);
        if ("BD".equals(cleanFilter)) {
            return cleanProcess.contains("RECEIVING") || cleanProcess.contains("PUTAWAY") || cleanProcess.contains("RETUR");
        }
        if ("PICK".equals(cleanFilter)) {
            return cleanProcess.contains("PICKING");
        }
        if ("TR".equals(cleanFilter)) {
            return cleanProcess.contains("TRANSFER") || cleanProcess.contains("DROPPING");
        }
        if ("ADJ".equals(cleanFilter)) {
            return cleanProcess.contains("ADJUST") || cleanProcess.contains("OPNAME");
        }
        return cleanProcess.contains(cleanFilter);
    }

    private int transactionColorForProcess(String process) {
        String clean = safe(process).toUpperCase(Locale.US);
        if (clean.contains("RECEIVING") || clean.contains("PUTAWAY") || clean.contains("RETUR")) {
            return GREEN;
        }
        if (clean.contains("PICKING") || clean.contains("LOADING") || clean.contains("MANIFEST")) {
            return AMBER;
        }
        if (clean.contains("TRANSFER") || clean.contains("DROPPING")) {
            return Color.rgb(111, 66, 193);
        }
        return BLUE;
    }

    private String formatDateTime(long timestamp) {
        return new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("id", "ID")).format(new Date(timestamp));
    }

    private String readableDate(String raw) {
        String clean = fallback(raw, "-").trim();
        if ("-".equals(clean) || clean.startsWith("1900-01-01")) {
            return "-";
        }
        long millis = parseDateMillis(clean);
        if (millis <= 0) {
            return clean.length() >= 10 ? clean.substring(0, 10) : clean;
        }
        return new SimpleDateFormat("dd/MM/yyyy", new Locale("id", "ID")).format(new Date(millis));
    }

    private String apiDateOnly(String raw) {
        String clean = fallback(raw, "").trim();
        if (clean.isEmpty() || "-".equals(clean) || clean.startsWith("1900-01-01")) {
            return "";
        }
        if (clean.matches("^\\d{4}-\\d{2}-\\d{2}.*")) {
            return clean.substring(0, 10);
        }
        long millis = parseDateMillis(clean);
        if (millis <= 0) {
            return "";
        }
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        formatter.setTimeZone(TimeZone.getTimeZone("Asia/Jakarta"));
        return formatter.format(new Date(millis));
    }

    private long parseDateMillis(String raw) {
        String clean = fallback(raw, "").trim();
        if (clean.isEmpty() || "-".equals(clean)) {
            return 0L;
        }
        String normalized = clean.replace("T", " ");
        if (normalized.endsWith("Z")) {
            normalized = normalized.substring(0, normalized.length() - 1) + " +0000";
        }
        if (normalized.matches(".*[+-]\\d{2}:\\d{2}$")) {
            normalized = normalized.substring(0, normalized.length() - 3) + normalized.substring(normalized.length() - 2);
        }
        String[] patterns = {
                "EEE, dd MMM yyyy HH:mm:ss zzz",
                "yyyy-MM-dd HH:mm:ss.SSS Z",
                "yyyy-MM-dd HH:mm:ss Z",
                "yyyy-MM-dd HH:mm:ss.SSS",
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd"
        };
        for (String pattern : patterns) {
            try {
                SimpleDateFormat formatter = new SimpleDateFormat(pattern, Locale.US);
                if (!pattern.contains("Z")) {
                    formatter.setTimeZone(TimeZone.getTimeZone("Asia/Jakarta"));
                }
                Date parsed = formatter.parse(normalized);
                if (parsed != null) {
                    return parsed.getTime();
                }
            } catch (ParseException ignored) {
            }
        }
        return 0L;
    }

    private void setShell(String title, String subtitle, String activeTab, boolean showBack) {
        applyThemeColors();
        applySystemBars();
        LinearLayout root = vertical();
        root.setBackgroundColor(BG);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        content = vertical();
        content.setPadding(dp(16), dp(14), dp(16), dp(118));
        scroll.setClipToPadding(false);
        scroll.addView(content, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        content.addView(header(title, subtitle, showBack));

        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        LinearLayout.LayoutParams navLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(92));
        root.addView(bottomNav(activeTab), navLp);
        setContentView(root);
    }

    private LinearLayout header(String title, String subtitle, boolean showBack) {
        LinearLayout wrapper = vertical();
        wrapper.setPadding(0, 0, 0, dp(12));
        LinearLayout row = horizontal();
        row.setGravity(Gravity.CENTER_VERTICAL);

        if (showBack) {
            TextView back = navIcon("<");
            back.setOnClickListener(v -> showDashboard());
            LinearLayout.LayoutParams backLp = new LinearLayout.LayoutParams(dp(40), dp(40));
            backLp.setMargins(0, 0, dp(8), 0);
            row.addView(back, backLp);
        }

        LinearLayout text = vertical();
        text.addView(title(title, 22));
        text.addView(small(subtitle));
        row.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        wrapper.addView(row);
        return wrapper;
    }

    private LinearLayout bottomNav(String activeTab) {
        LinearLayout nav = horizontal();
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(8), dp(8), dp(8), dp(10));
        nav.setBackgroundColor(CARD);
        nav.addView(navItem("Home", "dashboard", activeTab, R.drawable.ic_home, v -> showDashboard()));
        nav.addView(navItem("Incoming", "receiving", activeTab, R.drawable.ic_box, v -> showApiIncoming()));
        nav.addView(navItem("Picking", "putaway", activeTab, R.drawable.ic_scan, v -> showApiPicking()));
        nav.addView(navItem("Rak", "warehouse", activeTab, R.drawable.ic_warehouse, v -> showRackLocations()));
        nav.addView(navItem("Setting", "settings", activeTab, R.drawable.ic_settings, v -> showSettings()));
        return nav;
    }

    private LinearLayout navItem(String label, String key, String active, int iconRes, View.OnClickListener listener) {
        boolean isActive = key.equals(active);
        LinearLayout item = vertical();
        item.setGravity(Gravity.CENTER);
        item.setOnClickListener(listener);
        item.setPadding(dp(3), dp(4), dp(3), dp(3));
        item.setBackground(isActive ? round(activeSoftFill(), activeSoftStroke(), 8) : null);
        item.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setColorFilter(isActive ? BLUE : MUTED);
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(24), dp(24));
        iconLp.setMargins(0, 0, 0, dp(2));
        item.addView(icon, iconLp);

        TextView text = new TextView(this);
        text.setText(label);
        text.setGravity(Gravity.CENTER);
        text.setTextSize(10);
        text.setTypeface(Typeface.DEFAULT, isActive ? Typeface.BOLD : Typeface.NORMAL);
        text.setTextColor(isActive ? BLUE : MUTED);
        text.setSingleLine(true);
        text.setIncludeFontPadding(false);
        item.addView(text, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(18)));
        return item;
    }

    private LinearLayout stepper(int activeStep) {
        LinearLayout stepper = card();
        stepper.addView(sectionTitle("Alur"));
        LinearLayout row = horizontal();
        row.addView(stepCircle("1", "Receiving", activeStep == 1));
        row.addView(stepCircle("2", "Buat Palet", activeStep == 2));
        row.addView(stepCircle("3", "QR / Putaway", activeStep == 3));
        stepper.addView(row);
        return stepper;
    }

    private LinearLayout stepCircle(String number, String label, boolean active) {
        LinearLayout box = vertical();
        box.setGravity(Gravity.CENTER);
        TextView circle = new TextView(this);
        circle.setText(number);
        circle.setGravity(Gravity.CENTER);
        circle.setTextColor(active ? Color.WHITE : MUTED);
        circle.setTypeface(Typeface.DEFAULT_BOLD);
        circle.setBackground(round(active ? BLUE : mutedFill(), active ? BLUE : mutedFill(), 100));
        box.addView(circle, new LinearLayout.LayoutParams(dp(30), dp(30)));
        TextView text = small(label);
        text.setGravity(Gravity.CENTER);
        box.addView(text);
        box.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return box;
    }

    private LinearLayout horizontalSummaryRow(View left, View right) {
        LinearLayout row = horizontal();
        LinearLayout.LayoutParams leftLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        leftLp.setMargins(0, 0, dp(6), dp(10));
        LinearLayout.LayoutParams rightLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        rightLp.setMargins(dp(6), 0, 0, dp(10));
        row.addView(left, leftLp);
        row.addView(right, rightLp);
        return row;
    }

    private LinearLayout dashboardTileRow(View left, View right) {
        LinearLayout row = horizontal();
        LinearLayout.LayoutParams leftLp = new LinearLayout.LayoutParams(0, dp(122), 1f);
        leftLp.setMargins(0, dp(1), dp(6), dp(12));
        LinearLayout.LayoutParams rightLp = new LinearLayout.LayoutParams(0, dp(122), 1f);
        rightLp.setMargins(dp(6), dp(1), 0, dp(12));
        row.addView(left, leftLp);
        row.addView(right, rightLp);
        return row;
    }

    private LinearLayout dashboardTile(String label, String helper, int iconRes, int accent, View.OnClickListener listener) {
        LinearLayout tile = vertical();
        tile.setPadding(dp(14), dp(14), dp(14), dp(12));
        tile.setGravity(Gravity.CENTER_VERTICAL);
        tile.setBackground(cardBackground(CARD, CARD_BORDER));
        tile.setOnClickListener(listener);

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setColorFilter(accent);
        GradientDrawable iconBg = round(colorWithAlpha(accent, 28), colorWithAlpha(accent, 60), 10);
        icon.setBackground(iconBg);
        icon.setPadding(dp(8), dp(8), dp(8), dp(8));
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(42), dp(42));
        iconLp.setMargins(0, 0, 0, dp(10));
        tile.addView(icon, iconLp);

        TextView title = title(label, 15);
        title.setTextColor(INK);
        tile.addView(title);

        TextView sub = small(helper);
        sub.setTextSize(11);
        tile.addView(sub);
        return tile;
    }

    private LinearLayout primaryActionPanel() {
        LinearLayout panel = card();
        panel.setPadding(dp(12), dp(12), dp(12), dp(12));
        panel.addView(sectionTitle("01 · Penerimaan & Penempatan"));
        panel.addView(sectionDivider());
        panel.addView(dashboardListTile("Incoming", "Scan nota barang datang", R.drawable.ic_api, Color.rgb(111, 66, 193), v -> showApiIncoming()));
        panel.addView(dashboardListTile("Transfer Rak", "Scan rak tujuan lalu pilih barang", R.drawable.ic_scan, GREEN, v -> showTransferStocks()));
        panel.addView(dashboardListTile("Inventory", "Cek stok dan lokasi setelah penempatan", R.drawable.ic_warehouse, Color.rgb(220, 53, 69), v -> showInventory()));
        return panel;
    }

    private LinearLayout secondaryActionPanel() {
        LinearLayout panel = card();
        panel.setPadding(dp(12), dp(12), dp(12), dp(12));
        panel.addView(sectionTitle("02 · Picking & Pengiriman"));
        panel.addView(sectionDivider());
        panel.addView(dashboardListTile("Picking", "Ambil barang dari rak tetap/lorong", R.drawable.ic_scan, Color.rgb(14, 116, 144), v -> showApiPicking()));
        panel.addView(dashboardListTile("Checker", "Cek barang kecil hasil picking", R.drawable.ic_scan, GREEN, v -> showCheckerList()));
        panel.addView(dashboardListTile("Loading", "Muat sesuai jadwal armada", R.drawable.ic_box, BLUE, v -> showLoadingList()));
        panel.addView(dashboardListTile("Pengiriman", "Pantau manifest dan gagal kirim", R.drawable.ic_box, Color.rgb(111, 66, 193), v -> showDroppingTransfer()));
        return panel;
    }

    private LinearLayout extraActionPanel() {
        LinearLayout panel = card();
        panel.setPadding(dp(12), dp(12), dp(12), dp(12));
        panel.addView(sectionTitle("03 · Retur & Pengendalian"));
        panel.addView(sectionDivider());
        panel.addView(dashboardListTile("Retur", "Terima KPR ke antrean QC", R.drawable.ic_box, GREEN, v -> showWarehouseReturn()));
        panel.addView(dashboardListTile("QC Karantina", "Pisahkan barang GOOD / BAD", R.drawable.ic_warehouse, Color.rgb(220, 53, 69), v -> showQuarantineQc()));
        panel.addView(dashboardListTile("Stock Opname", "Hitung stok fisik", R.drawable.ic_warehouse, AMBER, v -> showStockOpname()));
        panel.addView(dashboardListTile("Transaksi", "Riwayat proses WMS", R.drawable.ic_api, Color.rgb(82, 96, 255), v -> showTransactions()));
        return panel;
    }

    private LinearLayout dashboardActionRow(View left, View right) {
        LinearLayout row = horizontal();
        LinearLayout.LayoutParams leftLp = new LinearLayout.LayoutParams(0, dp(86), 1f);
        leftLp.setMargins(0, dp(1), dp(5), dp(8));
        LinearLayout.LayoutParams rightLp = new LinearLayout.LayoutParams(0, dp(86), 1f);
        rightLp.setMargins(dp(5), dp(1), 0, dp(8));
        row.addView(left, leftLp);
        row.addView(right, rightLp);
        return row;
    }

    private LinearLayout dashboardActionTile(String label, String helper, int iconRes, int accent, View.OnClickListener listener) {
        LinearLayout tile = horizontal();
        tile.setPadding(dp(10), dp(10), dp(10), dp(10));
        tile.setGravity(Gravity.CENTER_VERTICAL);
        tile.setBackground(cardBackground(CARD, SUBTLE_BORDER));
        tile.setOnClickListener(listener);

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setColorFilter(accent);
        icon.setBackground(round(colorWithAlpha(accent, 28), colorWithAlpha(accent, 60), 10));
        icon.setPadding(dp(8), dp(8), dp(8), dp(8));
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(40), dp(40));
        iconLp.setMargins(0, 0, dp(10), 0);
        tile.addView(icon, iconLp);

        LinearLayout text = vertical();
        TextView title = title(label, 14);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        text.addView(title);
        TextView sub = small(helper);
        sub.setTextSize(10);
        sub.setSingleLine(true);
        sub.setEllipsize(TextUtils.TruncateAt.END);
        text.addView(sub);
        tile.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return tile;
    }

    private LinearLayout compactMenuRow(View left, View right) {
        LinearLayout row = horizontal();
        LinearLayout.LayoutParams leftLp = new LinearLayout.LayoutParams(0, dp(58), 1f);
        leftLp.setMargins(0, dp(1), dp(5), dp(8));
        LinearLayout.LayoutParams rightLp = new LinearLayout.LayoutParams(0, dp(58), 1f);
        rightLp.setMargins(dp(5), dp(1), 0, dp(8));
        row.addView(left, leftLp);
        row.addView(right, rightLp);
        return row;
    }

    private LinearLayout compactMenuTile(String label, String helper, int iconRes, int accent, View.OnClickListener listener) {
        LinearLayout tile = horizontal();
        tile.setGravity(Gravity.CENTER_VERTICAL);
        tile.setPadding(dp(10), dp(8), dp(10), dp(8));
        tile.setBackground(round(SUBTLE, SUBTLE_BORDER, 10));
        tile.setOnClickListener(listener);

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setColorFilter(accent);
        icon.setBackground(round(colorWithAlpha(accent, 22), colorWithAlpha(accent, 50), 8));
        icon.setPadding(dp(6), dp(6), dp(6), dp(6));
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(32), dp(32));
        iconLp.setMargins(0, 0, dp(8), 0);
        tile.addView(icon, iconLp);

        LinearLayout text = vertical();
        TextView title = title(label, 12);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        text.addView(title);
        TextView sub = small(helper);
        sub.setTextSize(9);
        sub.setSingleLine(true);
        sub.setEllipsize(TextUtils.TruncateAt.END);
        text.addView(sub);
        tile.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return tile;
    }

    private LinearLayout dashboardListTile(String label, String helper, int iconRes, int accent, View.OnClickListener listener) {
        LinearLayout tile = horizontal();
        tile.setGravity(Gravity.CENTER_VERTICAL);
        tile.setPadding(dp(12), dp(10), dp(12), dp(10));
        tile.setBackground(round(SUBTLE, colorWithAlpha(accent, 62), 10));
        tile.setOnClickListener(listener);
        LinearLayout.LayoutParams tileLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(76));
        tileLp.setMargins(0, 0, 0, dp(10));
        tile.setLayoutParams(tileLp);

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setColorFilter(accent);
        icon.setBackground(round(colorWithAlpha(accent, 24), colorWithAlpha(accent, 58), 10));
        icon.setPadding(dp(8), dp(8), dp(8), dp(8));
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(42), dp(42));
        iconLp.setMargins(0, 0, dp(12), 0);
        tile.addView(icon, iconLp);

        LinearLayout text = vertical();
        TextView title = title(label, 15);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        text.addView(title);
        TextView sub = small(helper);
        sub.setTextSize(11);
        sub.setSingleLine(true);
        sub.setEllipsize(TextUtils.TruncateAt.END);
        text.addView(sub);
        tile.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView arrow = small(">");
        arrow.setTextSize(18);
        arrow.setTextColor(accent);
        arrow.setTypeface(Typeface.DEFAULT_BOLD);
        arrow.setGravity(Gravity.CENTER);
        tile.addView(arrow, new LinearLayout.LayoutParams(dp(24), ViewGroup.LayoutParams.MATCH_PARENT));
        return tile;
    }

    private LinearLayout summaryCard(String label, String value, String helper, int accent) {
        LinearLayout card = vertical();
        card.setPadding(dp(12), dp(12), dp(12), dp(12));
        card.setBackground(cardBackground(CARD, CARD_BORDER));
        TextView title = small(label);
        title.setTextColor(MUTED);
        card.addView(title);
        TextView number = title(value, 20);
        number.setTextColor(accent);
        card.addView(number);
        TextView help = small(helper);
        help.setTextSize(11);
        card.addView(help);
        return card;
    }

    private LinearLayout dashboardUserCard() {
        LinearLayout userCard = userLoginCard();
        LinearLayout userTop = horizontal();

        TextView avatar = new TextView(this);
        avatar.setText(initials(fallback(authSession.getUserName(), "User")));
        avatar.setTextColor(Color.WHITE);
        avatar.setTextSize(13);
        avatar.setTypeface(Typeface.DEFAULT_BOLD);
        avatar.setGravity(Gravity.CENTER);
        avatar.setIncludeFontPadding(false);
        avatar.setBackground(round(BLUE, BLUE, 100));
        LinearLayout.LayoutParams avatarLp = new LinearLayout.LayoutParams(dp(36), dp(36));
        avatarLp.setMargins(0, 0, dp(10), 0);
        userTop.addView(avatar, avatarLp);

        LinearLayout identity = vertical();
        TextView userName = title(fallback(authSession.getUserName(), "User"), 15);
        userName.setSingleLine(true);
        userName.setEllipsize(TextUtils.TruncateAt.END);
        identity.addView(userName);
        TextView role = small(fallback(authSession.getRoleLabel(), "User Login"));
        role.setTextSize(11);
        role.setSingleLine(true);
        role.setEllipsize(TextUtils.TruncateAt.END);
        identity.addView(role);
        userTop.addView(identity, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        userTop.addView(compactAction("Logout", v -> {
            authSession.clear();
            showLogin();
        }));
        userCard.addView(userTop);

        LinearLayout userInfo = horizontal();
        userInfo.setPadding(0, dp(8), 0, 0);
        userInfo.addView(userInfoChip(fallback(authSession.getBranchName(), "Cabang -"), GREEN));
        userInfo.addView(userInfoChip("API aktif", BLUE));
        userCard.addView(userInfo);
        return userCard;
    }

    private LinearLayout keyValue(String key, String value) {
        LinearLayout row = horizontal();
        row.setPadding(0, dp(4), 0, dp(4));
        TextView k = small(key);
        TextView v = small(value);
        v.setTextColor(INK);
        v.setTypeface(Typeface.DEFAULT_BOLD);
        row.addView(k, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(v, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return row;
    }

    private LinearLayout largeKeyValue(String key, String value) {
        LinearLayout row = vertical();
        row.setPadding(dp(14), dp(12), dp(14), dp(12));
        row.setBackground(round(inputFill(), CARD_BORDER, 12));
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowLp.setMargins(0, 0, 0, dp(10));
        row.setLayoutParams(rowLp);

        TextView k = small(key);
        k.setTypeface(Typeface.DEFAULT_BOLD);
        k.setTextColor(MUTED);
        TextView v = title(value, 21);
        v.setSingleLine(false);
        v.setMaxLines(3);
        v.setEllipsize(TextUtils.TruncateAt.END);
        row.addView(k);
        row.addView(v);
        return row;
    }

    private LinearLayout transferDetailCard(JSONObject stock, String titleText, int qtyTransferPcs, String targetRack) {
        int availableQty = transferStockQty(stock);
        int perUnit = transferStockPerUnit(stock);
        int qtyPcs = qtyTransferPcs > 0 ? qtyTransferPcs : availableQty;

        LinearLayout detail = card();
        detail.setPadding(dp(18), dp(18), dp(18), dp(18));
        detail.addView(sectionTitle(titleText));

        TextView productName = title(fallback(first(stock, "nama_barang", "NamaBarang"), "-"), 24);
        productName.setSingleLine(false);
        productName.setMaxLines(3);
        productName.setEllipsize(TextUtils.TruncateAt.END);
        detail.addView(productName);

        TextView productCode = body(fallback(first(stock, "kode_barang", "KodeBarang"), "-"));
        productCode.setTextColor(BLUE);
        productCode.setTypeface(Typeface.DEFAULT_BOLD);
        detail.addView(productCode);
        detail.addView(space(10));

        detail.addView(largeKeyValue("Rak Asal", fallback(first(stock, "kode_rak_asal", "KodeRakAsal"), "-")));
        if (targetRack != null && !targetRack.trim().isEmpty()) {
            detail.addView(largeKeyValue("Rak Tujuan", targetRack.trim()));
        }

        LinearLayout qtyRow = horizontal();
        qtyRow.setPadding(0, dp(2), 0, dp(8));
        qtyRow.addView(detailMetric("CT", String.valueOf(qtyPcs / perUnit), BLUE), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        qtyRow.addView(detailMetric("PC", String.valueOf(qtyPcs % perUnit), GREEN), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        qtyRow.addView(detailMetric("Total", qtyPcs + " PCS", AMBER), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        detail.addView(qtyRow);

        detail.addView(largeKeyValue("Batch / Expired", transferStockBatch(stock) + " / " + transferStockExpired(stock)));
        detail.addView(largeKeyValue("Masuk Gudang", transferStockIncomingDate(stock)));
        detail.addView(largeKeyValue("Umur Barang", transferStockAgeText(stock)));

        String rackType = fallback(first(stock, "type_rak", "shelf_type"), "-");
        detail.addView(stockStatusFooter(transferStockFefoLabel(stock), transferStockFifoLabel(stock), rackType));
        return detail;
    }

    private LinearLayout detailMetric(String label, String value, int color) {
        LinearLayout box = vertical();
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(8), dp(12), dp(8), dp(12));
        box.setBackground(round(colorWithAlpha(color, 14), colorWithAlpha(color, 58), 12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(0, 0, dp(8), 0);
        box.setLayoutParams(lp);

        TextView labelView = small(label);
        labelView.setGravity(Gravity.CENTER);
        labelView.setTypeface(Typeface.DEFAULT_BOLD);
        TextView valueView = title(value, 22);
        valueView.setGravity(Gravity.CENTER);
        valueView.setTextColor(color);
        box.addView(labelView);
        box.addView(valueView);
        return box;
    }

    private TextView detailChip(String text, int color) {
        TextView chip = new TextView(this);
        chip.setText(text);
        chip.setTextSize(14);
        chip.setTextColor(color);
        chip.setTypeface(Typeface.DEFAULT_BOLD);
        chip.setGravity(Gravity.CENTER);
        chip.setSingleLine(true);
        chip.setEllipsize(TextUtils.TruncateAt.END);
        chip.setMaxWidth(dp(176));
        chip.setPadding(dp(12), 0, dp(12), 0);
        chip.setBackground(round(colorWithAlpha(color, 18), colorWithAlpha(color, 62), 99));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(34));
        lp.setMargins(0, 0, dp(7), 0);
        chip.setLayoutParams(lp);
        return chip;
    }

    private int rackTypeColor(String rackType) {
        String type = safe(rackType).toUpperCase(Locale.US);
        if (type.contains("TETAP")) {
            return GREEN;
        }
        if (type.contains("TITIPAN")) {
            return Color.rgb(111, 66, 193);
        }
        if (type.contains("LORONG")) {
            return BLUE;
        }
        return AMBER;
    }

    private int rackTypeSortOrder(String rackType) {
        String type = safe(rackType).toUpperCase(Locale.US);
        if (type.contains("TETAP")) {
            return 0;
        }
        if (type.contains("LORONG")) {
            return 1;
        }
        if (type.contains("TITIPAN")) {
            return 2;
        }
        return 3;
    }

    private TextView rackTypePill(String text) {
        String label = fallback(text, "-");
        int color = rackTypeColor(label);
        TextView pill = new TextView(this);
        pill.setText(label);
        pill.setTextSize(15);
        pill.setTextColor(color);
        pill.setTypeface(Typeface.DEFAULT_BOLD);
        pill.setGravity(Gravity.CENTER);
        pill.setSingleLine(true);
        pill.setEllipsize(TextUtils.TruncateAt.END);
        pill.setPadding(dp(12), 0, dp(12), 0);
        pill.setBackground(round(colorWithAlpha(color, 20), colorWithAlpha(color, 95), 99));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(38));
        lp.setMargins(dp(6), 0, 0, 0);
        pill.setLayoutParams(lp);
        return pill;
    }

    private TextView priorityPill(String text, int color) {
        boolean main = isPriorityLabel(text);
        TextView pill = new TextView(this);
        pill.setText(fallback(text, "-"));
        pill.setTextSize(main ? 14 : 13);
        pill.setTextColor(main ? Color.WHITE : color);
        pill.setTypeface(Typeface.DEFAULT_BOLD);
        pill.setGravity(Gravity.CENTER);
        pill.setSingleLine(true);
        pill.setEllipsize(TextUtils.TruncateAt.END);
        pill.setMaxWidth(dp(150));
        pill.setPadding(dp(10), 0, dp(10), 0);
        pill.setBackground(round(main ? color : colorWithAlpha(color, 18), colorWithAlpha(color, 95), 99));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(34));
        lp.setMargins(0, 0, dp(6), 0);
        pill.setLayoutParams(lp);
        return pill;
    }

    private boolean isPriorityLabel(String label) {
        String text = safe(label).toLowerCase(Locale.US);
        return text.contains("utama") || text.endsWith("#1");
    }

    private LinearLayout stockQtyPanel(int qtyPcs, int qtyCt, int qtyPc, int accent) {
        return stockQtyPanel(qtyPcs, qtyCt, qtyPc, accent, "CT", "PCS");
    }

    private LinearLayout stockQtyPanel(int qtyPcs, JSONObject row, int accent) {
        String primary = configuredUomName(row, 1, "PCS");
        String secondary = configuredUomName(row, 2, "");
        int factor = configuredUomFactor(row, 2, transferStockPerUnit(row));
        if (!secondary.isEmpty() && factor > 1) {
            return stockQtyPanel(qtyPcs, qtyPcs / factor, qtyPcs % factor, accent, secondary, primary);
        }
        return stockQtyPanel(qtyPcs, -1, 0, accent, "", primary);
    }

    private LinearLayout stockQtyPanel(int qtyPcs, int qtyCt, int qtyPc, int accent,
                                       String secondaryUnit, String primaryUnit) {
        LinearLayout box = vertical();
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(10), dp(10), dp(10), dp(10));
        box.setBackground(round(colorWithAlpha(accent, 14), colorWithAlpha(accent, 70), 12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(110), ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(10), 0, 0, 0);
        box.setLayoutParams(lp);

        boolean hasCartonBreakdown = !safe(secondaryUnit).isEmpty() && (qtyCt >= 0 || qtyPc >= 0);
        int mainQty = hasCartonBreakdown ? Math.max(qtyCt, 0) : Math.max(qtyPcs, 0);
        TextView qty = title(String.valueOf(mainQty), 28);
        qty.setTextColor(accent);
        qty.setGravity(Gravity.CENTER);
        qty.setSingleLine(true);
        TextView unit = small(hasCartonBreakdown ? secondaryUnit : primaryUnit);
        unit.setGravity(Gravity.CENTER);
        unit.setTypeface(Typeface.DEFAULT_BOLD);
        box.addView(qty);
        box.addView(unit);

        if (hasCartonBreakdown) {
            TextView detail = small(Math.max(qtyPc, 0) + " " + primaryUnit);
            detail.setGravity(Gravity.CENTER);
            detail.setSingleLine(true);
            detail.setEllipsize(TextUtils.TruncateAt.END);
            box.addView(detail);
            TextView total = small(Math.max(qtyPcs, 0) + " total " + primaryUnit);
            total.setGravity(Gravity.CENTER);
            total.setSingleLine(true);
            total.setEllipsize(TextUtils.TruncateAt.END);
            box.addView(total);
        }
        return box;
    }

    private LinearLayout stockStatusFooter(String fefo, String fifo, String rackType) {
        LinearLayout footer = horizontal();
        footer.setGravity(Gravity.CENTER_VERTICAL);
        footer.setPadding(0, dp(10), 0, 0);
        footer.addView(priorityPill(fefo, GREEN));
        footer.addView(priorityPill(fifo, BLUE));
        TextView spacer = new TextView(this);
        footer.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1f));
        footer.addView(rackTypePill(rackType));
        return footer;
    }

    private String inventoryRackType(JSONObject item) {
        return fallback(first(item, "shelf_type", "type_rak", "tipe_rak", "jenis_rak", "rack_type", "StatusRak"), "-");
    }

    private int inventoryPerUnit(JSONObject item) {
        return Math.max(1, parseOptionalInt(item, 1, "per_unit", "PerUnit", "IsiKarton", "isi_per_karton", "isiperkarton", "isiperbox"));
    }

    private int inventoryQtyPcs(JSONObject item) {
        int direct = parseOptionalInt(item, -1, "qty", "qty_pcs", "quantity", "stock_qty", "available_qty", "SaldoRak");
        if (direct >= 0) {
            return direct;
        }
        int ct = parseOptionalInt(item, 0, "quantity_ct", "qty_karton", "qty_ct", "QtyCT");
        int pc = parseOptionalInt(item, 0, "quantity_pc", "qty_pieces", "qty_pc", "QtyPC");
        return (ct * inventoryPerUnit(item)) + pc;
    }

    private int inventoryQtyCt(JSONObject item) {
        int direct = parseOptionalInt(item, -1, "quantity_ct", "qty_karton", "qty_ct", "QtyCT");
        return direct >= 0 ? direct : inventoryQtyPcs(item) / inventoryPerUnit(item);
    }

    private int inventoryQtyPc(JSONObject item) {
        int direct = parseOptionalInt(item, -1, "quantity_pc", "qty_pieces", "qty_pc", "QtyPC");
        return direct >= 0 ? direct : inventoryQtyPcs(item) % inventoryPerUnit(item);
    }

    private String inventoryBatch(JSONObject item) {
        return fallback(first(item, "batch_number", "batch", "Batch"), "-");
    }

    private String inventoryExpired(JSONObject item) {
        return readableDate(fallback(first(item, "expiry_date", "expired_date", "expired", "Expired"), "-"));
    }

    private String inventoryIncomingDate(JSONObject item) {
        return readableDate(fallback(first(item, "tanggal_masuk_gudang", "received_at", "incoming_at", "created_at"), "-"));
    }

    private String inventoryAgeText(JSONObject item) {
        String direct = fallback(first(item, "umur_barang_hari", "age_days", "umur_hari"), "");
        if (!direct.isEmpty()) {
            int days = parsePositiveInt(direct, -1);
            if (days >= 0) {
                return days == 0 ? "Hari ini" : days + " hari";
            }
        }
        long millis = parseDateMillis(first(item, "tanggal_masuk_gudang", "received_at", "incoming_at", "created_at"));
        if (millis <= 0) {
            return "-";
        }
        long diff = Math.max(0L, System.currentTimeMillis() - millis);
        long days = diff / (24L * 60L * 60L * 1000L);
        return days == 0 ? "Hari ini" : days + " hari";
    }

    private int inventoryRank(JSONObject item, String primaryKey, String alternateKey) {
        int rank = parsePositiveInt(fallback(first(item, primaryKey, alternateKey), "999999"), 999999);
        return rank <= 0 ? 999999 : rank;
    }

    private String inventoryFifoLabel(JSONObject item) {
        String label = fallback(first(item, "fifo_label", "fifoLabel"), "");
        if (!label.isEmpty()) {
            return label;
        }
        int rank = inventoryRank(item, "fifo_rank", "fifoRank");
        if (rank >= 999999) {
            return "FIFO -";
        }
        return rank <= 1 ? "FIFO utama" : "FIFO #" + rank;
    }

    private String inventoryFefoLabel(JSONObject item) {
        String label = fallback(first(item, "fefo_label", "fefoLabel"), "");
        if (!label.isEmpty()) {
            return label;
        }
        int rank = inventoryRank(item, "fefo_rank", "fefoRank");
        if ("-".equals(inventoryExpired(item))) {
            return "FEFO tanpa exp";
        }
        if (rank >= 999999) {
            return "FEFO -";
        }
        return rank <= 1 ? "FEFO utama" : "FEFO #" + rank;
    }

    private String inventoryGroupKey(JSONObject item) {
        String product = fallback(first(item, "product_code", "KodeBarang", "kode_barang"), "-").trim().toUpperCase(Locale.US);
        String rack = fallback(first(item, "location", "kode_rak", "KodeRak"), "-").trim().toUpperCase(Locale.US);
        String type = inventoryRackType(item).trim().toUpperCase(Locale.US);
        return product + "|" + rack + "|" + type;
    }

    private java.util.ArrayList<JSONObject> mergeInventoryRows(JSONArray rows) {
        java.util.LinkedHashMap<String, JSONObject> mergedRows = new java.util.LinkedHashMap<>();
        if (rows == null) {
            return new java.util.ArrayList<>();
        }
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.optJSONObject(i);
            if (row == null) {
                continue;
            }
            String key = inventoryGroupKey(row);
            JSONObject existing = mergedRows.get(key);
            if (existing == null) {
                try {
                    JSONObject copy = new JSONObject(row.toString());
                    copy.put("qty_pcs", inventoryQtyPcs(row));
                    copy.put("quantity_ct", inventoryQtyCt(row));
                    copy.put("quantity_pc", inventoryQtyPc(row));
                    copy.put("merged_count", 1);
                    mergedRows.put(key, copy);
                } catch (Exception ignored) {
                    mergedRows.put(key, row);
                }
                continue;
            }
            mergeInventoryQuantity(existing, row);
        }
        return new java.util.ArrayList<>(mergedRows.values());
    }

    private void mergeInventoryQuantity(JSONObject target, JSONObject source) {
        try {
            int totalPcs = inventoryQtyPcs(target) + inventoryQtyPcs(source);
            int perUnit = Math.max(1, inventoryPerUnit(target));
            target.put("qty_pcs", totalPcs);
            target.put("quantity_ct", totalPcs / perUnit);
            target.put("quantity_pc", totalPcs % perUnit);
            target.put("merged_count", parseOptionalInt(target, 1, "merged_count") + 1);

            int fifoRank = Math.min(inventoryRank(target, "fifo_rank", "fifoRank"), inventoryRank(source, "fifo_rank", "fifoRank"));
            int fefoRank = Math.min(inventoryRank(target, "fefo_rank", "fefoRank"), inventoryRank(source, "fefo_rank", "fefoRank"));
            if (fifoRank < 999999) {
                target.put("fifo_rank", fifoRank);
                target.remove("fifo_label");
                target.remove("fifoLabel");
            }
            if (fefoRank < 999999) {
                target.put("fefo_rank", fefoRank);
                target.remove("fefo_label");
                target.remove("fefoLabel");
            }

            String currentExp = inventoryExpired(target);
            String sourceExp = inventoryExpired(source);
            long currentMillis = parseDateMillis(currentExp);
            long sourceMillis = parseDateMillis(sourceExp);
            if (sourceMillis > 0 && (currentMillis <= 0 || sourceMillis < currentMillis)) {
                target.put("expiry_date", sourceExp);
                target.put("expired_date", sourceExp);
            }

            String currentBatch = inventoryBatch(target);
            String sourceBatch = inventoryBatch(source);
            if (!"-".equals(currentBatch) && !"-".equals(sourceBatch) && !currentBatch.equalsIgnoreCase(sourceBatch)) {
                target.put("batch_number", "Multi batch");
            }
        } catch (Exception ignored) {
        }
    }

    private JSONArray sortInventoryRows(JSONArray rows) {
        JSONArray sortedRows = new JSONArray();
        if (rows == null) {
            return sortedRows;
        }
        java.util.ArrayList<JSONObject> list = mergeInventoryRows(rows);
        java.util.Collections.sort(list, (left, right) -> {
            int primary = Integer.compare(rackTypeSortOrder(inventoryRackType(left)), rackTypeSortOrder(inventoryRackType(right)));
            if (primary != 0) {
                return primary;
            }
            primary = Integer.compare(inventoryRank(left, "fefo_rank", "fefoRank"), inventoryRank(right, "fefo_rank", "fefoRank"));
            if (primary != 0) {
                return primary;
            }
            primary = Integer.compare(inventoryRank(left, "fifo_rank", "fifoRank"), inventoryRank(right, "fifo_rank", "fifoRank"));
            if (primary != 0) {
                return primary;
            }
            primary = safe(first(left, "location", "kode_rak", "KodeRak")).compareToIgnoreCase(safe(first(right, "location", "kode_rak", "KodeRak")));
            if (primary != 0) {
                return primary;
            }
            return safe(first(left, "product_code", "KodeBarang", "kode_barang")).compareToIgnoreCase(safe(first(right, "product_code", "KodeBarang", "kode_barang")));
        });
        for (JSONObject row : list) {
            sortedRows.put(row);
        }
        return sortedRows;
    }

    private String filterSummary(String title, String query, String type) {
        String q = safe(query).trim();
        String t = safe(type).trim();
        if ("ALL".equalsIgnoreCase(t)) {
            t = "";
        }
        if (q.isEmpty() && t.isEmpty()) {
            return title;
        }
        if (!q.isEmpty() && !t.isEmpty()) {
            return title + ": " + t + " / " + q;
        }
        return title + ": " + fallback(t, q);
    }

    private String activeFilterText(String query, String type, String emptyText) {
        String q = safe(query).trim();
        String t = safe(type).trim();
        if ("ALL".equalsIgnoreCase(t)) {
            t = "";
        }
        if (q.isEmpty() && t.isEmpty()) {
            return emptyText;
        }
        if (!q.isEmpty() && !t.isEmpty()) {
            return "Filter aktif: " + t + " / " + q + ". Tap untuk ubah.";
        }
        return "Filter aktif: " + fallback(t, q) + ". Tap untuk ubah.";
    }

    private TextView dropdownHeader(String text, boolean open, View.OnClickListener listener) {
        TextView header = new TextView(this);
        header.setText((open ? "v  " : ">  ") + text);
        header.setTextSize(17);
        header.setTextColor(INK);
        header.setTypeface(Typeface.DEFAULT_BOLD);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setSingleLine(true);
        header.setPadding(dp(14), 0, dp(14), 0);
        header.setBackground(round(SUBTLE, SUBTLE_BORDER, 12));
        header.setOnClickListener(listener);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52));
        lp.setMargins(0, dp(8), 0, dp(8));
        header.setLayoutParams(lp);
        return header;
    }

    private TextView masterSelectionField(String hint) {
        TextView field = new TextView(this);
        field.setText(hint + "   Pilih");
        field.setTextSize(16);
        field.setTextColor(INK);
        field.setTypeface(Typeface.DEFAULT_BOLD);
        field.setGravity(Gravity.CENTER_VERTICAL);
        field.setSingleLine(true);
        field.setEllipsize(TextUtils.TruncateAt.END);
        field.setPadding(dp(14), 0, dp(14), 0);
        field.setBackground(round(inputFill(), CARD_BORDER, 10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56));
        lp.setMargins(0, dp(6), 0, dp(6));
        field.setLayoutParams(lp);
        return field;
    }

    private void showMasterSinglePicker(String title, JSONArray rows, boolean vehicle, MasterSelectionAction action) {
        LinearLayout pickerBody = new LinearLayout(this);
        pickerBody.setOrientation(LinearLayout.VERTICAL);
        pickerBody.setPadding(dp(20), dp(6), dp(20), 0);
        EditText query = input("", vehicle ? "Cari plat atau nama kendaraan" : "Cari nama petugas", InputType.TYPE_CLASS_TEXT);
        pickerBody.addView(query);
        ScrollView scroll = new ScrollView(this);
        LinearLayout choices = new LinearLayout(this);
        choices.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(choices);
        pickerBody.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(390)));

        final AlertDialog[] dialog = new AlertDialog[1];
        Runnable render = new Runnable() {
            @Override
            public void run() {
                choices.removeAllViews();
                String needle = value(query).toLowerCase(Locale.ROOT);
                int count = 0;
                for (int i = 0; i < rows.length(); i++) {
                    JSONObject row = rows.optJSONObject(i);
                    if (row == null || !masterRowLabel(row, vehicle).toLowerCase(Locale.ROOT).contains(needle)) {
                        continue;
                    }
                    TextView option = masterOptionRow(masterRowLabel(row, vehicle), masterRowDetail(row, vehicle));
                    option.setOnClickListener(v -> {
                        action.onSelect(row);
                        if (dialog[0] != null) dialog[0].dismiss();
                    });
                    choices.addView(option);
                    count++;
                }
                if (count == 0) {
                    choices.addView(body("Data master tidak ditemukan."));
                }
            }
        };
        query.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { render.run(); }
            @Override public void afterTextChanged(Editable s) { }
        });
        dialog[0] = new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(pickerBody)
                .setNegativeButton("Batal", null)
                .create();
        render.run();
        dialog[0].show();
    }

    private void showMasterMultiPicker(String title, JSONArray rows, LinkedHashMap<String, JSONObject> selected, MasterMultiSelectionAction action) {
        LinkedHashMap<String, JSONObject> working = new LinkedHashMap<>(selected);
        LinearLayout pickerBody = new LinearLayout(this);
        pickerBody.setOrientation(LinearLayout.VERTICAL);
        pickerBody.setPadding(dp(20), dp(6), dp(20), 0);
        EditText query = input("", "Cari nama helper", InputType.TYPE_CLASS_TEXT);
        pickerBody.addView(query);
        ScrollView scroll = new ScrollView(this);
        LinearLayout choices = new LinearLayout(this);
        choices.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(choices);
        pickerBody.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(390)));

        Runnable render = new Runnable() {
            @Override
            public void run() {
                choices.removeAllViews();
                String needle = value(query).toLowerCase(Locale.ROOT);
                int count = 0;
                for (int i = 0; i < rows.length(); i++) {
                    JSONObject row = rows.optJSONObject(i);
                    String id = masterRowId(row);
                    if (row == null || id.isEmpty() || !masterRowLabel(row, false).toLowerCase(Locale.ROOT).contains(needle)) {
                        continue;
                    }
                    CheckBox option = new CheckBox(MainActivity.this);
                    option.setText(masterRowLabel(row, false) + "\n" + masterRowDetail(row, false));
                    option.setTextSize(16);
                    option.setTextColor(INK);
                    option.setPadding(dp(4), dp(8), dp(4), dp(8));
                    option.setChecked(working.containsKey(id));
                    option.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        if (isChecked) {
                            working.put(id, row);
                        } else {
                            working.remove(id);
                        }
                    });
                    choices.addView(option);
                    count++;
                }
                if (count == 0) {
                    choices.addView(body("Data helper tidak ditemukan."));
                }
            }
        };
        query.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { render.run(); }
            @Override public void afterTextChanged(Editable s) { }
        });
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(pickerBody)
                .setNegativeButton("Batal", null)
                .setPositiveButton("Simpan", (ignored, ignoredWhich) -> action.onSelect(working))
                .create();
        render.run();
        dialog.show();
    }

    private TextView masterOptionRow(String label, String detail) {
        TextView option = new TextView(this);
        option.setText(label + (detail.isEmpty() ? "" : "\n" + detail));
        option.setTextSize(16);
        option.setTextColor(INK);
        option.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        option.setGravity(Gravity.CENTER_VERTICAL);
        option.setPadding(dp(14), dp(10), dp(14), dp(10));
        option.setBackground(round(inputFill(), CARD_BORDER, 8));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(4), 0, dp(4));
        option.setLayoutParams(lp);
        return option;
    }

    private String masterRowId(JSONObject row) {
        return first(row, "id", "id_driver", "id_armada", "id_helper");
    }

    private JSONObject findMasterById(JSONArray rows, String id) {
        if (rows == null || id == null || id.trim().isEmpty()) return null;
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.optJSONObject(i);
            if (row != null && id.equals(first(row, "id", "id_armada", "id_driver", "id_helper"))) {
                return row;
            }
        }
        return null;
    }

    private String masterRowLabel(JSONObject row, boolean vehicle) {
        if (row == null) return "";
        if (vehicle) {
            String plate = first(row, "no_pelat", "vehicle_no", "no_polisi");
            String name = first(row, "nama", "armada_name");
            return plate.isEmpty() ? fallback(name, "Kendaraan") : plate + (name.isEmpty() ? "" : " - " + name);
        }
        return fallback(first(row, "nama", "driver_name", "helper_name"), "Petugas");
    }

    private String masterRowDetail(JSONObject row, boolean vehicle) {
        if (row == null) return "";
        String scope = first(row, "nama_cabang", "cabang", "branch_name");
        if (vehicle) {
            String company = first(row, "nama_perusahaan", "perusahaan", "company_name");
            return scope + (scope.isEmpty() || company.isEmpty() ? "" : " | ") + company;
        }
        String email = first(row, "email", "username");
        return scope + (scope.isEmpty() || email.isEmpty() ? "" : " | ") + email;
    }

    private String selectedMasterNames(LinkedHashMap<String, JSONObject> values) {
        if (values == null || values.isEmpty()) return "Tidak ada helper dipilih (opsional)";
        ArrayList<String> names = new ArrayList<>();
        for (JSONObject value : values.values()) names.add(masterRowLabel(value, false));
        return TextUtils.join(", ", names);
    }

    private EditText input(String defaultValue, String hint, int inputType) {
        EditText editText = new EditText(this);
        editText.setText(defaultValue);
        editText.setHint(hint);
        editText.setSingleLine(true);
        editText.setInputType(inputType);
        editText.setTextSize(17);
        editText.setTextColor(INK);
        editText.setHintTextColor(MUTED);
        editText.setPadding(dp(14), 0, dp(14), 0);
        editText.setBackground(round(inputFill(), CARD_BORDER, 10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56));
        lp.setMargins(0, dp(6), 0, dp(6));
        editText.setLayoutParams(lp);
        return editText;
    }

    private Button primaryButton(String text, View.OnClickListener listener) {
        Button button = button(text, Color.WHITE, BLUE, BLUE);
        button.setOnClickListener(listener);
        return button;
    }

    private Button secondaryButton(String text, View.OnClickListener listener) {
        Button button = button(text, BLUE, CARD, SUBTLE_BORDER);
        button.setOnClickListener(listener);
        return button;
    }

    private Button tinyButton(String text, View.OnClickListener listener) {
        Button button = button(text, BLUE, SUBTLE, SUBTLE_BORDER);
        button.setTextSize(14);
        normalizeButtonChrome(button);
        button.setMinHeight(dp(46));
        button.setMinimumHeight(dp(46));
        button.setPadding(dp(12), dp(9), dp(12), dp(9));
        button.setOnClickListener(listener);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(10), dp(8), dp(2));
        button.setLayoutParams(lp);
        return button;
    }

    private TextView compactAction(String text, View.OnClickListener listener) {
        TextView action = new TextView(this);
        action.setText(text);
        action.setTextSize(14);
        action.setTextColor(BLUE);
        action.setTypeface(Typeface.DEFAULT_BOLD);
        action.setGravity(Gravity.CENTER);
        action.setIncludeFontPadding(false);
        action.setSingleLine(true);
        action.setPadding(dp(12), 0, dp(12), 0);
        action.setBackground(round(SUBTLE, SUBTLE_BORDER, 8));
        action.setOnClickListener(listener);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(42));
        lp.setMargins(dp(10), 0, 0, 0);
        action.setLayoutParams(lp);
        return action;
    }

    private TextView userInfoChip(String text, int color) {
        TextView chip = new TextView(this);
        chip.setText(text);
        chip.setTextSize(12);
        chip.setTextColor(color);
        chip.setTypeface(Typeface.DEFAULT_BOLD);
        chip.setGravity(Gravity.CENTER);
        chip.setIncludeFontPadding(false);
        chip.setSingleLine(true);
        chip.setEllipsize(TextUtils.TruncateAt.END);
        chip.setMaxWidth(dp(168));
        chip.setPadding(dp(10), 0, dp(10), 0);
        chip.setBackground(round(colorWithAlpha(color, 18), colorWithAlpha(color, 58), 99));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(30));
        lp.setMargins(0, 0, dp(6), 0);
        chip.setLayoutParams(lp);
        return chip;
    }

    private TextView palletMetaChip(String text, int color) {
        TextView chip = new TextView(this);
        chip.setText(text);
        chip.setTextSize(12);
        chip.setTextColor(color);
        chip.setTypeface(Typeface.DEFAULT_BOLD);
        chip.setGravity(Gravity.CENTER);
        chip.setIncludeFontPadding(false);
        chip.setSingleLine(true);
        chip.setEllipsize(TextUtils.TruncateAt.END);
        chip.setMaxWidth(dp(154));
        chip.setPadding(dp(10), 0, dp(10), 0);
        chip.setBackground(round(colorWithAlpha(color, 16), colorWithAlpha(color, 52), 99));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(30));
        lp.setMargins(0, 0, dp(6), 0);
        chip.setLayoutParams(lp);
        return chip;
    }

    private TextView filterChip(String text, boolean active, View.OnClickListener listener) {
        TextView chip = new TextView(this);
        chip.setText(text);
        chip.setTextSize(14);
        chip.setGravity(Gravity.CENTER);
        chip.setSingleLine(true);
        chip.setTypeface(Typeface.DEFAULT, active ? Typeface.BOLD : Typeface.NORMAL);
        chip.setTextColor(active ? Color.WHITE : BLUE);
        chip.setPadding(dp(8), 0, dp(8), 0);
        chip.setBackground(round(active ? BLUE : SUBTLE, active ? BLUE : SUBTLE_BORDER, 8));
        chip.setOnClickListener(listener);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(46), 1f);
        lp.setMargins(0, 0, dp(8), 0);
        chip.setLayoutParams(lp);
        return chip;
    }

    private TextView settingChip(String text, boolean active) {
        TextView chip = new TextView(this);
        chip.setText(text);
        chip.setTextSize(14);
        chip.setGravity(Gravity.CENTER);
        chip.setSingleLine(true);
        chip.setIncludeFontPadding(false);
        chip.setPadding(dp(8), 0, dp(8), 0);
        styleSettingChip(chip, active);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(50), 1f);
        lp.setMargins(0, 0, dp(8), 0);
        chip.setLayoutParams(lp);
        return chip;
    }

    private void styleSettingChip(TextView chip, boolean active) {
        chip.setTypeface(Typeface.DEFAULT, active ? Typeface.BOLD : Typeface.NORMAL);
        chip.setTextColor(active ? Color.WHITE : BLUE);
        chip.setBackground(round(active ? BLUE : SUBTLE, active ? BLUE : SUBTLE_BORDER, 8));
    }

    private Button button(String text, int textColor, int bgColor, int strokeColor) {
        Button button = new Button(this);
        normalizeButtonChrome(button);
        button.setAllCaps(false);
        button.setText(text);
        button.setTextColor(textColor);
        button.setTextSize(16);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setGravity(Gravity.CENTER);
        button.setIncludeFontPadding(false);
        button.setSingleLine(false);
        button.setMinLines(1);
        button.setMaxLines(2);
        button.setMinHeight(dp(56));
        button.setMinimumHeight(dp(56));
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setPadding(dp(12), dp(10), dp(12), dp(10));
        button.setBackground(round(bgColor, strokeColor, 8));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(7), 0, dp(7));
        button.setLayoutParams(lp);
        return button;
    }

    private void normalizeButtonChrome(Button button) {
        button.setAllCaps(false);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setIncludeFontPadding(false);
        button.setStateListAnimator(null);
        button.setElevation(0);
        button.setTranslationZ(0);
    }

    private TextView badge(String text, int color) {
        TextView badge = new TextView(this);
        badge.setText(text);
        badge.setTextSize(13);
        badge.setTextColor(color);
        badge.setTypeface(Typeface.DEFAULT_BOLD);
        badge.setGravity(Gravity.CENTER);
        badge.setPadding(dp(8), dp(5), dp(8), dp(5));
        badge.setBackground(round(CARD, color, 99));
        return badge;
    }

    private LinearLayout userLoginCard() {
        LinearLayout card = card();
        card.setPadding(dp(12), dp(10), dp(12), dp(10));
        card.setBackground(cardBackground(CARD, CARD_BORDER));
        return card;
    }

    private LinearLayout card() {
        LinearLayout card = vertical();
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackground(cardBackground(CARD, CARD_BORDER));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(lp);
        return card;
    }

    private TextView title(String text, int sp) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(sp);
        view.setTextColor(INK);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setIncludeFontPadding(true);
        return view;
    }

    private TextView sectionTitle(String text) {
        TextView view = title(text, 19);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(6));
        view.setLayoutParams(lp);
        return view;
    }

    private View sectionDivider() {
        View divider = new View(this);
        divider.setBackgroundColor(CARD_BORDER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        lp.setMargins(0, 0, 0, dp(12));
        divider.setLayoutParams(lp);
        return divider;
    }

    private TextView body(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(16);
        view.setTextColor(MUTED);
        view.setLineSpacing(0, 1.15f);
        return view;
    }

    private TextView small(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(14);
        view.setTextColor(MUTED);
        view.setLineSpacing(0, 1.1f);
        return view;
    }

    private TextView navIcon(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(24);
        view.setTextColor(INK);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setGravity(Gravity.CENTER);
        view.setBackground(round(CARD, CARD_BORDER, 8));
        return view;
    }

    private View space(int dp) {
        View view = new View(this);
        view.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(dp)));
        return view;
    }

    private LinearLayout vertical() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private LinearLayout horizontal() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        return layout;
    }

    private GradientDrawable cardBackground(int color, int strokeColor) {
        return round(color, strokeColor, 8);
    }

    private GradientDrawable successBackground() {
        return isDarkMode()
                ? cardBackground(Color.rgb(13, 50, 35), Color.rgb(32, 108, 74))
                : cardBackground(Color.rgb(232, 246, 238), Color.rgb(186, 230, 202));
    }

    private GradientDrawable dangerBackground() {
        return isDarkMode()
                ? cardBackground(Color.rgb(64, 24, 32), Color.rgb(145, 55, 72))
                : cardBackground(Color.rgb(255, 241, 242), Color.rgb(254, 205, 211));
    }

    private GradientDrawable separatorBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.TRANSPARENT);
        drawable.setStroke(dp(1), ROW_BORDER);
        return drawable;
    }

    private GradientDrawable round(int color, int strokeColor, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        drawable.setStroke(dp(1), strokeColor);
        return drawable;
    }

    private String filterLabel(String key, String label) {
        return label;
    }

    private String transactionTypeLabel(String type) {
        String clean = safe(type).toUpperCase(Locale.US);
        if ("BD".equals(clean)) {
            return "IN";
        }
        if ("PICK".equals(clean)) {
            return "PICK";
        }
        if ("TR".equals(clean) || "TRI".equals(clean) || "TRO".equals(clean) || "TR_IN".equals(clean) || "TR_OUT".equals(clean)) {
            return "TR";
        }
        if ("ADJ".equals(clean)) {
            return "ADJ";
        }
        return clean.isEmpty() ? "-" : clean;
    }

    private String wmsStockStatusLabel(String status) {
        String clean = safe(status).trim().toUpperCase(Locale.US);
        if (clean.isEmpty()) {
            return "READY";
        }
        if ("PENDING_WAREHOUSE".equals(clean) || "BELUM_KONFIRMASI_GUDANG".equals(clean)) {
            return "Belum Konfirmasi Gudang";
        }
        if ("READY".equals(clean) || "DONE".equals(clean)) {
            return "Ready";
        }
        return status;
    }

    private int transactionTypeColor(String type) {
        String clean = safe(type).toUpperCase(Locale.US);
        if ("BD".equals(clean) || "TRI".equals(clean) || "TR_IN".equals(clean)) {
            return GREEN;
        }
        if ("PICK".equals(clean) || "TRO".equals(clean) || "TR_OUT".equals(clean)) {
            return AMBER;
        }
        if ("ADJ".equals(clean)) {
            return Color.rgb(111, 66, 193);
        }
        return BLUE;
    }

    private String value(EditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    private int parsePositiveInt(String text, int fallback) {
        try {
            int value = Integer.parseInt(text.trim());
            return Math.max(0, value);
        } catch (Exception e) {
            return fallback;
        }
    }

    private int parsePaperWidth(String text) {
        int width = parsePositiveInt(text, 58);
        return width <= 60 ? 58 : 80;
    }

    private String todayString() {
        return new SimpleDateFormat("dd/MM/yyyy", new Locale("id", "ID")).format(new Date());
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String fallback(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private String initials(String name) {
        String clean = fallback(name, "User").trim();
        String[] parts = clean.split("\\s+");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                result.append(part.substring(0, 1).toUpperCase(Locale.US));
            }
            if (result.length() == 2) {
                break;
            }
        }
        return result.length() == 0 ? "U" : result.toString();
    }

    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }

    private String themeMode() {
        if (uiPrefs == null) {
            return THEME_SYSTEM;
        }
        String mode = uiPrefs.getString(KEY_THEME_MODE, THEME_SYSTEM);
        if (THEME_LIGHT.equals(mode) || THEME_DARK.equals(mode)) {
            return mode;
        }
        return THEME_SYSTEM;
    }

    private boolean isDarkMode() {
        String mode = themeMode();
        if (THEME_DARK.equals(mode)) {
            return true;
        }
        if (THEME_LIGHT.equals(mode)) {
            return false;
        }
        int nightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightMode == Configuration.UI_MODE_NIGHT_YES;
    }

    private void applyThemeColors() {
        if (isDarkMode()) {
            BLUE = Color.rgb(96, 165, 250);
            INK = Color.rgb(229, 237, 248);
            MUTED = Color.rgb(156, 170, 192);
            BG = Color.rgb(10, 15, 25);
            CARD = Color.rgb(17, 24, 39);
            CARD_BORDER = Color.rgb(51, 65, 85);
            SUBTLE = Color.rgb(20, 39, 67);
            SUBTLE_BORDER = Color.rgb(50, 91, 142);
            ROW_BORDER = Color.rgb(45, 55, 72);
            GREEN = Color.rgb(74, 222, 128);
            AMBER = Color.rgb(251, 191, 36);
            return;
        }
        BLUE = Color.rgb(11, 94, 215);
        INK = Color.rgb(16, 42, 86);
        MUTED = Color.rgb(93, 107, 128);
        BG = Color.rgb(244, 247, 251);
        CARD = Color.WHITE;
        CARD_BORDER = Color.rgb(220, 228, 240);
        SUBTLE = Color.rgb(244, 248, 255);
        SUBTLE_BORDER = Color.rgb(198, 217, 250);
        ROW_BORDER = Color.rgb(234, 239, 247);
        GREEN = Color.rgb(25, 135, 84);
        AMBER = Color.rgb(176, 120, 0);
    }

    private void applySystemBars() {
        Window window = getWindow();
        window.setStatusBarColor(BG);
        window.setNavigationBarColor(CARD);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int flags = window.getDecorView().getSystemUiVisibility();
            if (isDarkMode()) {
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                }
            } else {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                }
            }
            window.getDecorView().setSystemUiVisibility(flags);
        }
    }

    private int activeSoftFill() {
        return isDarkMode() ? Color.rgb(18, 45, 82) : Color.rgb(239, 246, 255);
    }

    private int activeSoftStroke() {
        return isDarkMode() ? Color.rgb(43, 91, 150) : Color.rgb(219, 234, 254);
    }

    private int mutedFill() {
        return isDarkMode() ? Color.rgb(38, 50, 70) : Color.rgb(233, 237, 245);
    }

    private int inputFill() {
        return isDarkMode() ? Color.rgb(11, 18, 32) : Color.WHITE;
    }

    private int colorWithAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }
}
