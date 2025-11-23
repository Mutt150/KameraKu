# 📌 KameraKu (CameraX)

## 🎯 Deskripsi
**KameraKu** adalah aplikasi sederhana berbasis Android dan Jetpack CameraX yang mampu menampilkan preview kamera secara langsung, mengambil foto, menyimpan ke penyimpanan perangkat menggunakan **MediaStore**, serta menampilkan thumbnail foto terakhir.

Aplikasi ini dirancang untuk memenuhi standar Android modern (Scoped Storage) dan mendukung berbagai fitur kamera esensial.

## 🚀 Fitur Utama

| Fitur | Status |
| :--- | :---: |
| Live Camera Preview | ✔ |
| Ambil Foto | ✔ |
| Simpan Foto ke MediaStore | ✔ |
| Thumbnail Foto Terakhir | ✔ |
| Switch Kamera Depan/Belakang | ✔ |
| Flash Mode (On/Off/Auto) | ✔ |
| Torch (Senter) | ✔ |
| Perekaman Video | ✔ (Opsional) |

---

## ⚙️ Alur Izin Kamera
Saat aplikasi dijalankan, pengguna diminta memberikan izin kamera. Jika pengguna menolak, aplikasi akan menampilkan pesan bahwa kamera tidak dapat digunakan.

**Kode Implementasi:**
```kotlin
val launcher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
) { granted ->
    hasCameraPermission = granted
}
```

**Izin yang digunakan:**
* `android.permission.CAMERA`
* **Note:** Kita **tidak** memerlukan `WRITE_EXTERNAL_STORAGE` karena menggunakan Scoped Storage (MediaStore).

---

## 🖼️ Menampilkan Preview Kamera
Preview kamera ditampilkan menggunakan `PreviewView` yang dipasang melalui `AndroidView` di Jetpack Compose. Ini memastikan kamera otomatis aktif saat Activity berjalan dan mencegah kebocoran memori.

**Kode Implementasi:**
```kotlin
AndroidView(
    factory = { ctx ->
        PreviewView(ctx).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    },
    modifier = Modifier.fillMaxSize(),
    update = { previewView = it }
)

// Binding ke Lifecycle
provider.bindToLifecycle(
    lifecycleOwner,
    selector,
    preview,
    ic,
    vc
)
```

---

## 📷 Penyimpanan Foto ke MediaStore
Hasil foto disimpan secara aman menggunakan **MediaStore** ke folder `Pictures/KameraKu`.

**Keunggulan Scoped Storage:**
* Tidak perlu akses *write storage* global.
* Foto langsung muncul di Galeri bawaan HP.
* Aman dan sesuai standar Android modern (Android 10+).

**Kode Penyimpanan:**
```kotlin
val contentValues = ContentValues().apply {
    put(MediaStore.MediaColumns.DISPLAY_NAME, name)
    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/KameraKu")
}
```

---

## 🔄 Penanganan Rotasi
Aplikasi menangani orientasi perangkat sehingga hasil foto tidak miring atau terbalik saat HP diputar.

```kotlin
imageCapture.targetRotation =
    previewView.display?.rotation ?: Surface.ROTATION_0
```
*Hasil: Orientasi EXIF tersimpan benar.*

---

## 💡 Fitur Opsional

### 🌓 Switch Kamera (Depan/Belakang)
```kotlin
fun toggleCameraSelector(current: CameraSelector): CameraSelector {
    return if (current == CameraSelector.DEFAULT_BACK_CAMERA)
        CameraSelector.DEFAULT_FRONT_CAMERA
    else
        CameraSelector.DEFAULT_BACK_CAMERA
}
```

### ⚡ Flash Mode
Mengatur flash menyala hanya saat tombol shutter ditekan.
```kotlin
ImageCapture.Builder()
    .setFlashMode(flashMode)
    .build()
```

### 🔦 Torch (Senter)
Mengaktifkan lampu flash agar menyala terus menerus.
```kotlin
camera?.cameraControl?.enableTorch(true/false)
```

> **Catatan Penting:** Flash dan Torch biasanya **tidak bekerja di Emulator** karena keterbatasan hardware. Silakan uji coba fitur ini menggunakan perangkat fisik (HP Android asli).

---

## 📱 Contoh Hasil Foto

*(Ganti bagian ini dengan screenshot atau file foto asli kamu)*

| Portrait | Landscape | Opsional |
| :---: | :---: | :---: |
| ![Portrait](hasil_portrait.jpeg) | ![Landscape](hasil_landscape.jpeg) | ![Lainnya](hasil_switch.jpeg) |

---

## 🧪 Troubleshooting

1.  **Preview Hitam?**
    * Pastikan izin kamera sudah diberikan (Allow).
    * Pastikan emulator yang digunakan memiliki konfigurasi kamera yang aktif.

2.  **Flash/Torch Tidak Menyala?**
    * Ini normal jika dijalankan di Emulator. Gunakan perangkat fisik untuk mengetes lampu.

3.  **Foto Tidak Muncul di Galeri?**
    * Tunggu beberapa saat untuk proses *indexing*.
    * Pastikan kode `MediaStore` dan `RELATIVE_PATH` sudah benar sesuai snippet di atas.

---

## 👨‍💻 Teknologi yang Digunakan
* **Android Studio**
* **Kotlin**
* **Jetpack Compose** (UI)
* **CameraX** (Preview + ImageCapture + Lifecycle)

---

## 🏁 Status Tugas
Semua poin tugas praktikum telah terpenuhi:

- [x] Preview Live
- [x] Simpan foto ke MediaStore
- [x] Thumbnail foto terakhir
- [x] Switch kamera
- [x] Flash & Torch
- [x] Laporan & Dokumentasi