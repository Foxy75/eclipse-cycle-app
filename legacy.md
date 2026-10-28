# legacy.md — Handoff untuk agent berikutnya

Baca ini dulu sebelum menyentuh kode. Ini catatan migrasi dari app resep "Bites & Bowls"
ke "Eclipse", bukan dokumentasi desain (itu ada di `design/`).

## 1. Kondisi repo saat ini

- Ini repo Android **Bites & Bowls** (app resep), belum `git init`.
- Package: `com.example.stasiscahmber`. Label: "Bites & Bowls".
- Stack existing: XML + ViewBinding + Fragment + Material 1.14 + ConstraintLayout + RecyclerView.
- File lama yang masih ada dan HARUS dihapus:
  - `app/src/main/java/com/example/stasiscahmber/ui/{home,saved,grocery,detail}/`
  - `app/src/main/java/com/example/stasiscahmber/data/{DbHelper,Models}.kt`
  - `app/src/main/java/com/example/stasiscahmber/ui/log/...` (tidak ada, hanya 4 fragment)
  - `app/src/main/res/layout/` (semua: fragment_*, item_*, activity_main)
  - `app/src/main/res/menu/bottom_nav_menu.xml`
  - `app/src/main/res/raw/recipes.json`
  - `app/src/main/res/drawable/{ic_*, bg_*, cb_*, ph_food_*}`
  - `app/src/main/res/mipmap-*/ic_launcher*` (ganti dengan vector/adaptive ikon Eclipse)
- Yang DIPERTAHANKAN: `res/font/outfit_{regular,medium,semibold}.ttf`, `res/values/colors.xml`
  (diganti token baru), struktur gradle (AGP 9.3.3, built-in Kotlin).

## 2. Keputusan yang sudah dikunci (jangan dinegosiasi ulang)

- Nama **Eclipse**. Package `io.github.foxy75.eclipse`. `rootProject.name = "Eclipse"`.
  Lisensi GPL-3.0. Repo di-upload user sendiri (tanpa remote).
- **Views/XML, bukan Compose.** Ini override eksplisit atas aturan Compose di AGENTS.md.
  Alasan: mutlak offline, semua dependency Views sudah di cache.
- **Tanpa Room/SQLCipher/SQLite.** Data = satu file `data.enc` AES-256-GCM, kunci 32B di
  AndroidKeyStore, tulis atomik temp→fsync→rename + `.bak`, `Dispatchers.IO` + Mutex,
  debounce 300ms, flush saat `onStop`. Serialisasi `org.json`.
- **Tanpa constraintlayout** (hapus dependency; layout pakai LinearLayout/FrameLayout).
- **Tanpa biometrik.** PIN 6 digit PBKDF2-HmacSHA256 120k. 5x salah → 7 hari, lalu 1
  percobaan/7 hari. Tanpa reset, tanpa wipe.
- Prediksi: median 6 siklus, luteal 14 hari, min 2 siklus, MAD >9 → "belum stabil".
- Siklus diturunkan dari hari aliran (aliran pertama setelah ≥10 hari kosong).
- Bahasa: `values/` ID (default) + `values-en/` EN.
- minSdk 23. Release: `optimization { enable = true }` + `isShrinkResources = true`,
  `androidResources { localeFilters += listOf("id","en") }`.

Sisa keputusan lengkap + token warna terakhir: baca `design/DESIGN.md`, `PRODUCT.md`,
`THREAT-MODEL.md`.

## 3. Urutan kerja agent berikutnya

1. **Update AGENTS.md** — tulis override "Views/XML menggantikan Compose" + alasannya,
   supaya agent berikutnya tidak diam-diam menulis Compose.
2. **M0 (selesai sebagian)** — `design/*.md` dan `README.md` sudah ada. Tinggal:
   `git init` + `.gitignore` (exclude `design/`, `local.properties`, `build/`) + `LICENSE`
   (GPL-3.0, teks lengkap).
3. **M1** — rename package/namespace/label/rootProject; hapus file lama (liste di atas);
   tema + token (lihat `DESIGN.md`) + ikon stroke; `MainActivity` 3 tab + FAB + skeleton.
   Gerbang: `gradlew --offline assembleDebug`.
4. **M2** — `EncryptedStore` + PIN/lockout + onboarding 2 langkah + layar Hari ini.
5. **M3** — log sheet + kalender grid 7 kolom + `CycleBuilder`/`CyclePredictor` + unit test.
6. **M4** — wawasan + pengaturan + `FLAG_SECURE` + ekspor/impor `.ecl`.
7. **M5** — README final + CI (unit test + `assembleDebug`) + instruksi rilis.

Commit di akhir tiap milestone.

## 4. Gotchas environment

- Tidak ada `java` di PATH. `JAVA_HOME=C:\Program Files\Android\openjdk\jdk-21.0.8` (JDK 21).
- Build harus `--offline`. Dependency yang tersedia di cache Gradle: Material 1.14,
  AppCompat 1.8, RecyclerView 1.4, constraintlayout 2.2.2 (ini yang akan DIHAPUS).
  TIDAK tersedia: Room, KSP, SQLCipher, Compose runtime plugins yang koheren untuk Views —
  jangan coba menambahnya.
- SDK terpasang: `android-36.1`, `android-37.0`.
- CLI Gradle di `gradlew.bat` (Windows PowerShell).
- Baseline saat ini: `app-debug.apk` 6.93 MB (belum R8), dex ~12.9 MB, res 229 KB.

## 5. Target rilis (gate)

APK ≤1.8 MB · PSS idle ≤55 MB · cold start ≤350 ms · nol frame >32ms (scroll kalender) ·
nol wakeup saat idle (dumpsys alarm) · `adb shell am start -W` + `dumpsys meminfo`.