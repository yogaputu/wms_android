# Budimas WMS Android

Aplikasi WMS Android native untuk alur receiving barang, pembuatan pallet, cetak/bagikan QR pallet, putaway dengan scan QR, dan daftar pallet gudang.

## Teknologi

- Android native Java + XML/resource standar
- Minimum Android 5.0 / API 21
- QR scan: ZXing Android Embedded
- QR generate: ZXing Core
- Cetak QR: Android Print Framework
- Penyimpanan lokal: SharedPreferences JSON

## Alur Utama

0. Login API
   Aplikasi wajib login ke `http://58.147.185.142:9092/api/auth/login`. Token disimpan lokal dan dipakai sebagai `Authorization: Bearer <token>` untuk request API berikutnya.

1. Barang Datang
   Barang datang sudah membawa QR yang dicetak oleh admin.

2. Rak Titipan
   Semua barang datang langsung masuk ke Rak Titipan.

3. Transfer Rak
   Operator scan Rak Tetap tujuan terlebih dahulu, pindahkan barang secara fisik, lalu scan QR barang untuk update lokasi rak. Jika Rak Tetap penuh, sistem memakai Rak Lorong dengan kolom yang sama.

4. Inventory Scan
   Operator bisa scan barang untuk melihat barang berada di rak mana saja, atau scan rak untuk melihat isi rak tersebut.

5. Picking
   Draft picking dimuat dari server, lalu operator scan QR barang/rak tetap.

6. Loading / Shipping
   Barang yang sudah ready-to-load dikonfirmasi ke manifest.

7. Monitoring
   Inventory, master rak, transaksi, dropping/karantina, dan alert low stock mengikuti modul WMS di `new_budimas`.

## Alur API WMS

- Lokasi Rak
  Muat master rak dari `GET /api/wms/racks?active=true`. Data ini dipakai untuk daftar lokasi rak dan validasi scan/input putaway.

- Incoming API
  Muat nota dari `GET /api/incoming/incoming/note-details/{nota}`, lalu proses QR pallet lewat `POST /api/incoming/incoming/process-single-pallet`.

- Picking API
  Muat draft dari `GET /api/picking/picking/draft-detail/{nota}`, scan QR rak, lalu submit ke `POST /api/picking/picking/scan-rak`.

- Inventory
  Muat stok dari `GET /api/inventory/inventory/inventory`, cari berdasarkan kode/nama/rak, filter tipe rak, dan scan barcode produk lewat `GET /api/inventory/inventory/barcode/{kode}`.

- History Transaksi
  Muat riwayat dari `GET /api/transactions/transactions` dengan filter Incoming, Picking, Transfer, dan Adjustment.

- Loading
  Muat daftar barang siap dimuat dari `GET /api/loading/loading/ready-to-load`.

- Transfer Stock
  Muat stok titipan dari `GET /api/transfer/transfer/temporary-stocks`, pilih rak tujuan tipe Tetap, lalu konfirmasi transfer lewat `POST /api/transfer/transfer/confirm`. Jika rak tetap penuh, API WMS mengarahkan ke rak Lorong pada kolom yang sama.

Data test API real yang sudah dicek tersedia di `docs/api-test-data.md` dan juga muncul sebagai tombol cepat di menu Data Test, Incoming API, dan Picking API.

## Printer Bluetooth

Menu Settings menyimpan konfigurasi printer untuk pengembangan cetak Bluetooth berikutnya:

- Mode cetak: Android Print atau Bluetooth ESC/POS
- Nama dan MAC address printer
- Lebar kertas 58 mm atau 80 mm
- Opsi auto print setelah QR dibuat
- Cek perangkat Bluetooth yang sudah paired di Android
- Minta aktifkan Bluetooth dari aplikasi
- Cari perangkat Bluetooth baru dan tautkan/pair printer dari hasil scan

## Build APK Development

Pastikan Android SDK tersedia, lalu jalankan:

```powershell
.\gradlew.bat :app:assembleDebug
```

APK debug akan dibuat di:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Build APK Production

Endpoint production dikonfigurasi di `app/build.gradle`:

```text
http://58.147.185.142:9092/api/
```

Untuk APK release yang siap install, siapkan file lokal `keystore.properties` di root project:

```properties
storeFile=/absolute/path/budimas-wms.jks
storePassword=isi_password_keystore
keyAlias=budimas-wms
keyPassword=isi_password_key
```

Lalu build:

```powershell
.\gradlew.bat :app:assembleRelease
```

Jika `keystore.properties` tersedia, APK release siap install akan dibuat di:

```text
app/build/outputs/apk/release/app-release.apk
```

Jika file signing belum tersedia, Gradle tetap bisa membuat APK unsigned untuk verifikasi build:

```text
app/build/outputs/apk/release/app-release-unsigned.apk
```

Project ini juga bisa langsung dibuka lewat Android Studio dari folder ini.

## Auto Reload Saat Development

Native Android Java tidak punya hot reload penuh seperti Flutter/React Native. Untuk menghindari install manual dari Android Studio, gunakan script watcher:

```powershell
.\dev-live-reload.ps1
```

Script akan:

- Mendeteksi perubahan `.java`, `.xml`, dan file Gradle penting
- Menjalankan `:app:installDebug`
- Membuka ulang aplikasi di device/emulator via ADB

Pastikan HP/emulator sudah terdeteksi:

```powershell
C:\Users\user\AppData\Local\Android\Sdk\platform-tools\adb.exe devices
```
