# Eclipse

Pelacak siklus menstruasi offline-first, single user, tanpa jaringan. Semua data
terenkripsi di perangkat, tidak pernah dikirim ke mana pun.

## Kenapa Eclipse

- **Nol jaringan** — tidak ada izin INTERNET, akun, cloud, atau telemetri.
- **Terenkripsi penuh** — satu file `data.enc` AES-256-GCM, kunci di Android Keystore.
- **Disamarkan** — tampil seperti jurnal produktivitas; label fase tersembunyi default,
  screenshots/recents diblokir (`FLAG_SECURE`).
- **Data milikmu** — ekspor/impor `.ecl` terenkripsi dengan passphrase buatanmu.

## Fitur

- Catat aliran, 8 gejala, mood, dan catatan harian.
- Prediksi siklus lokal (median 6 siklus terakhir).
- Kalender dengan penanda fakta vs prediksi bisa dibedakan sekilas.
- Wawasan sederhana (panjang siklus, akurasi, gejala tersering).
- Kunci PIN 6 digit + lockout 7 hari, tanpa biometrik.
- Bahasa Indonesia (default) dan Inggris.

Bukan alat kontrasepsi, bukan alat medis, tidak mengklaim kesuburan.

## Teknologi

Kotlin, Android Views + XML, Material 3, RecyclerView. Tanpa Compose, tanpa dependency
jaringan. MinSdk 23. GPL-3.0.

## Build (offline)

Prasyarat:

```
JAVA_HOME=C:\Program Files\Android\openjdk\jdk-21.0.8
```

```
./gradlew --offline assembleDebug    # build cepat untuk pengembangan
./gradlew --offline assembleRelease  # rilis, R8 aktif
```

Gerbang rilis: APK ≤ 1.8 MB, PSS idle ≤ 55 MB, cold start ≤ 350 ms.

## Model ancaman (ringkas)

Melindungi dari: pinjam-dukung device, ekstraksi file saat device terkunci, restore backup
tanpa kunci, brute-force PIN, guardra analisis jaringan (tidak ada trafik).

Tidak melindungi dari: device ter-root, device tidak terkunci yang dipegang orang lain,
keylogger saat mengetik PIN, kehilangan kunci Keystoke permanen (jalan keluar: clear data +
impor backup).

Detail: `design/THREAT-MODEL.md` (lokal, tidak di-commit ke repo publik).

## Rilis & distribusi

1. Buat keystore lokal (jangan di-commit):
   `keytool -genkeypair -v -keystore eclipse-release.jks -alias eclipse -keyalg RSA -keysize 4096 -validity 10000`
2. Bangun rilis: `./gradlew --offline assembleRelease`
3. APK mentah: `app/build/outputs/apk/release/app-release-unsigned.apk`
4. Tandatangani dengan `apksigner` (atau konfigurasikan `signingConfig` di `app/build.gradle.kts`
   menggunakan keystore lokal) sebelum didistribusikan lewat GitHub Releases.

## CI

`.github/workflows/ci.yml` menjalankan `testDebugUnitTest` + `assembleDebug` di GitHub Actions.
CI membutuhkan network untuk mengunduh dependency; build `--offline` hanya untuk mesin lokal
yang cache-nya sudah lengkap.

## Lisensi

GPL-3.0 — lihat `LICENSE`.