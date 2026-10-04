# Data Test API WMS

Data berikut dicek langsung ke API `http://58.147.185.142:9092/api/`. Ini bukan data demo internal aplikasi Android; semua nomor dipanggil lewat endpoint API WMS.

## Incoming API

Gunakan di menu `Incoming API`.

| Nota | Status Saat Dicek | Isi |
| --- | --- | --- |
| KBD26060001 | READY | 1 item, EN RECH PWR+ 2000 AA |
| KBD26060002 | READY | 7 item, E91 AA-MAX 4+2 |
| KBD26060003 | READY | 6 item, CHO CHOC JOY SUPER |

Endpoint:

```text
GET /api/incoming/incoming/note-details/{nota}
POST /api/incoming/incoming/process-single-pallet
```

## Picking API

Gunakan di menu `Picking API`.

| Nota | Status Saat Dicek | Isi |
| --- | --- | --- |
| SNP26060046 | READY | 3 item, 1012 BP20 AAA MERAH |
| SNP26060050 | READY | 3 item, 1015 AA BP20 MERAH |
| SNP26060069 | READY | 1 item, TS SWT DIABTX PLS |

Endpoint:

```text
GET /api/picking/picking/draft-detail/{nota}
POST /api/picking/picking/scan-rak
```

Catatan: status data bisa berubah setelah tombol proses QR atau scan rak dijalankan, karena request tersebut menulis ke API.
