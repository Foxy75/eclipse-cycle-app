# Eclipse

> An offline-first, zero-telemetry menstrual cycle tracker for Android, engineered for individuals seeking complete data sovereignty without corporate surveillance, cloud leaks, or third-party tracking.

[![License: GPL-3.0](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Android_6.0+_(API_23+)-success.svg)](https://developer.android.com)
[![Language](https://img.shields.io/badge/Language-Kotlin-purple.svg)](https://kotlinlang.org)
[![Network](https://img.shields.io/badge/Network-Zero--Permission-red.svg)](#-features)
[![UI](https://img.shields.io/badge/UI-Android_Views_%2B_XML-orange.svg)](#-tech-stack)
[![Security](https://img.shields.io/badge/Security-AES--256--GCM-green.svg)](#-features)

---

## 🚀 Features

- **Zero-Network Footprint**: Operating without the `android.permission.INTERNET` manifest declaration. Zero telemetry, zero analytics SDKs, zero cloud dependencies, and zero outbound network traffic.
- **Hardware-Backed Encryption**: Application data is isolated in a single local encrypted container (`data.enc`) using AES-256-GCM with non-exportable cryptographic keys secured by the Android KeyStore.
- **PBKDF2-Hardened PIN Gate**: 6-digit access PIN derived via PBKDF2-HmacSHA256 (120,000 iterations) with progressive 7-day lockout mechanisms to defeat physical brute-force attempts.
- **Discreet Camouflage & Anti-Snooping**: Disguised within a warm, tactile productivity-journal interface. Automatically applies `FLAG_SECURE` to block OS-level screenshots and task switcher previews.
- **Transparent Local Analytics**: Cycle projections, cycle-length variations, and phase estimations computed client-side using statistical median calculations over past cycles without speculative AI or fertility claims.
- **Sovereign Backup & Migration**: Password-protected `.ecl` portable encrypted archives allow full data backup and migration without exposing cleartext records.
- **Deterministic Resource Profile**: Highly tuned footprint strictly targeting an APK size ≤ 1.8 MB, idle PSS memory ≤ 55 MB, and cold start latency under 350 ms.

---

## 🛠️ Tech Stack

| Component / Layer | Technology |
|---|---|
| **Language** | Kotlin 1.9+ / Java 11 |
| **UI Framework** | Android Views + XML Layouts (ViewBinding), Material Components 3 |
| **Architecture** | Clean Architecture (MVVM / Unidirectional Data Flow) |
| **Local Storage** | Single-file encrypted storage (`data.enc`), atomic temp-to-fsync write with `.bak` rollback |
| **Security & Cryptography** | AES-256-GCM (Android KeyStore), PBKDF2-HmacSHA256 (120,000 rounds) |
| **Serialization** | `org.json` (zero external dependencies, zero reflective overhead) |
| **Build & Tooling** | Gradle 8.x (Kotlin DSL), Android Gradle Plugin 9.3+ |
| **Compatibility** | Minimum SDK 23 (Android 6.0) · Target SDK 36 (Android 15) |

---

## 📦 Installation

Ensure you have the Android SDK and a compatible JDK (Java 17 or Java 21) configured.

```bash
# Clone the repository
git clone https://github.com/foxy75/eclipse-cycle-app.git

# Navigate into the project directory
cd eclipse-cycle-app

# Build the debug APK (supports 100% offline Gradle build)
./gradlew assembleDebug
```

To install directly onto an adb-connected physical device or emulator:

```bash
./gradlew installDebug
```

---

## 💡 Usage

### Typical Application Workflow

1. **Initial Vault Initialization**:
   - Launch Eclipse and configure your 6-digit master PIN.
   - Eclipse provisions a cryptographic key in Android KeyStore and creates your encrypted data vault (`data.enc`).

2. **Log Daily Cycle Signals**:
   - Access the daily entry sheet to record flow intensity, physical symptoms, emotional states, and optional notes.
   - Updates are flushed atomically to disk via an fsync rename sequence to prevent file corruption.

3. **Explore Cycle Projections**:
   - Inspect the monthly calendar view displaying past logs, active phases, and projected upcoming dates.
   - Review the **Insights** dashboard for cycle statistics, median duration, and frequency distributions.

4. **Export Encrypted Backups**:
   - Navigate to **Settings** > **Data & Security** > **Export Backup**.
   - Enter an encryption passphrase to generate a portable `.ecl` archive for safe offline backup.

### Verification & Testing Commands

```bash
# Execute local unit test suite
./gradlew testDebugUnitTest

# Assemble production-ready release APK with R8 code shrinking
./gradlew assembleRelease

# Verify zero internet permission is present in compiled binary
aapt dump permissions app/build/outputs/apk/debug/app-debug.apk
```

---

## 🤝 Contributing & License

### Contributing

Contributions that uphold Eclipse's core principles of privacy and minimal footprint are welcome.

1. **Fork the Repository** and create a descriptive feature branch:
   ```bash
   git checkout -b feature/improvement-name
   ```
2. **Respect Architectural Constraints**:
   - **Zero Network**: Do not include networking libraries, remote analytics, or internet permissions.
   - **Android Views Only**: Strictly no Jetpack Compose dependencies to preserve offline build reproducibility and lean APK size.
   - **Privacy Compliance**: All new data fields must integrate with `EncryptedStore` and honor `FLAG_SECURE`.
3. **Run Unit Tests**:
   ```bash
   ./gradlew testDebugUnitTest
   ```
4. **Submit a Pull Request** detailing changes, motivation, and verification steps.

### License

This project is licensed under the **GNU General Public License v3.0 (GPL-3.0)**. See the [LICENSE](LICENSE) file for complete terms and copyright details.

---

> **Medical Disclaimer**: Eclipse is an offline health tracking and statistical estimation utility. It is not a medical device, diagnostic instrument, or contraceptive aid.