# S.E.B.O. Encrypt

Eine Android-App zur Ende-zu-Ende-verschlüsselten Kommunikation über beliebige Messenger (z. B. WhatsApp). Nachrichten werden lokal verschlüsselt und können als Text geteilt werden – der Messenger selbst sieht nur unlesbaren Chiffretext.

---

## Funktionsweise

Die App nutzt ein **ECDH-Schlüsselaustausch-Protokoll**:

1. Jede Instanz generiert beim ersten Start ein **EC-Schlüsselpaar (secp256r1)** im Android Keystore – der private Schlüssel verlässt das Gerät nie.
2. Der **öffentliche Schlüssel** wird als QR-Code angezeigt und kann mit dem Kommunikationspartner geteilt werden.
3. Durch Scannen des gegenseitigen QR-Codes wird über **ECDH** ein gemeinsames Secret berechnet, aus dem via **HKDF (RFC 5869 / SHA-256)** ein 256-Bit-AES-Schlüssel abgeleitet wird.
4. Nachrichten werden mit **AES-256-GCM** verschlüsselt (zufälliger 12-Byte-IV, 128-Bit-Auth-Tag) und als Base64-String übertragen.

---

## Features

| Feature | Beschreibung |
|---|---|
| 🔒 **Verschlüsseln** | Text eingeben, mit aktivem Kontakt verschlüsseln, kopieren oder direkt via WhatsApp teilen |
| 🔓 **Entschlüsseln** | Verschlüsselten Text einfügen (per Button) oder via Share-Intent empfangen, mit aktivem Kontakt entschlüsseln |
| 📷 **QR-Scan** | Öffentlichen Schlüssel des Kontakts per QR-Code scannen (immer im Hochformat) |
| 🔏 **TOFU-Fingerprint** | Nach dem QR-Scan wird ein SHA-256-Fingerprint des Schlüssels angezeigt – Nutzer muss ihn mit dem Kontakt abgleichen (Man-in-the-Middle-Schutz) |
| ✏️ **Manuell hinzufügen** | Kontakt auch per Base64-Public-Key manuell eintragen |
| 👥 **Kontaktverwaltung** | Kontakte umbenennen, löschen, aktiven Kontakt wechseln, Fingerprint in Details einsehen |
| 📥 **Share-Intent** | Verschlüsselte Texte direkt aus WhatsApp o. ä. in die App teilen → wird automatisch in den Entschlüsseln-Tab geladen |
| 🔐 **App-Sperre** | Biometrie oder Geräte-PIN wird beim Start und bei jedem Zurückkehren zur App angefordert |
| 🛡️ **Screenshot-Schutz** | `FLAG_SECURE` verhindert Screenshots und App-Switcher-Vorschau |

---

## Technologie-Stack

| Komponente | Technologie | Version |
|---|---|---|
| Sprache | Kotlin | 2.3.10 |
| UI | Jetpack Compose + Material 3 | BOM 2026.02.00 |
| Architektur | MVVM (`AndroidViewModel`) | – |
| Verschlüsselung | AES-256-GCM (`javax.crypto`) | – |
| Schlüsselaustausch | ECDH secp256r1 (Android Keystore) | – |
| Schlüsselableitung | HKDF-SHA256 (Bouncy Castle) | 1.83 |
| QR-Code | ZXing Android Embedded | 4.3.0 |
| Kontaktspeicherung | `EncryptedSharedPreferences` (Jetpack Security Crypto) | 1.1.0 |
| Biometrie / PIN | AndroidX Biometric | 1.1.0 |
| Activity-Basis | `FragmentActivity` (AndroidX Fragment) | – |
| Build-System | Gradle (Kotlin DSL) | AGP 9.0.1 |
| Min. Android-Version | Android 7.0 (API 24) | – |
| Target SDK | Android 16 (API 36) | – |

---

## Projektstruktur

```
app/src/main/java/com/sebo/seboencrypt/
├── MainActivity.kt                  # Entry Point, Tab-Navigation, QR-Scanner-Start
│                                    # Erbt von FragmentActivity (für BiometricPrompt)
│                                    # QR-Scan via startActivityForResult (Request-Code 42)
├── PortraitCaptureActivity.kt       # Scanner immer im Hochformat
├── helper/
│   ├── BiometricAuthHelper.kt       # Biometrie/PIN-Authentifizierung
│   ├── ClipboardHelper.kt           # Zwischenablage lesen/schreiben
│   ├── KeyDerivation.kt             # HKDF – leitet AES-Key aus ECDH-Secret ab
│   ├── QRHelper.kt                  # QR-Code erzeugen & Public Key dekodieren
│   └── ShareHelper.kt               # Text via WhatsApp teilen
├── engine/
│   └── CryptoEngine.kt              # AES-256-GCM Verschlüsselung & Entschlüsselung
├── manager/
│   └── KeystoreManager.kt           # EC-Schlüsselpaar im Android Keystore, ECDH
├── model/
│   └── Contact.kt                   # Kontakt-Datenmodell
├── repository/
│   └── ContactRepository.kt         # Kontakte speichern/laden (EncryptedSharedPreferences)
├── viewmodel/
│   └── E2EEViewModel.kt             # Gesamte Business-Logik
└── ui/
    ├── screens/
    │   ├── EncryptTab.kt            # Verschlüsseln-Tab
    │   ├── DecryptTab.kt            # Entschlüsseln-Tab
    │   └── KeyTab.kt                # Schlüssel & Kontakte-Tab
    └── components/
        ├── StatusBanner.kt          # Globales Status-Banner
        └── keytab/
            ├── MyKeySection.kt      # Eigener QR-Code anzeigen
            ├── ContactsSection.kt   # Kontaktliste
            └── ContactListItem.kt   # Einzelner Kontakt-Eintrag
```

---

## Sicherheitshinweise

- **Der private Schlüssel verlässt das Gerät niemals** – er wird im Android Keystore gespeichert und ist nicht exportierbar. Auf unterstützten Geräten wird StrongBox (Hardware-Security-Modul) verwendet.
- Verschlüsselte Nachrichten sind **nur mit dem richtigen Kontakt** entschlüsselbar – jeder Kontakt hat ein eigenes ECDH-Session-Secret.
- **TOFU-Fingerprint-Verifikation**: Beim Hinzufügen eines Kontakts wird ein SHA-256-Fingerprint des öffentlichen Schlüssels angezeigt, der mit dem Kontakt abgeglichen werden muss – schützt vor Man-in-the-Middle beim Schlüsselaustausch.
- **Kein automatisches Clipboard-Lesen**: Die Zwischenablage wird nur auf expliziten Nutzerklick ("Einfügen"-Button) ausgelesen – verhindert ungewolltes Auslesen durch andere Apps.
- **Clipboard-Sensitive-Flag (API 33+)**: Kopierte verschlüsselte Texte werden als sensitiv markiert – Android unterdrückt die Clipboard-Preview-Benachrichtigung.
- **App-Sperre per Biometrie/PIN**: Bei jedem App-Start und jeder Rückkehr zur App wird eine Authentifizierung angefordert.
- **Screenshot-Schutz**: `FLAG_SECURE` verhindert Screenshots und verbirgt den App-Inhalt in der App-Switcher-Vorschau.
- **Session Keys werden sicher gelöscht**: Beim Beenden der App (`onCleared()`) werden alle Session Keys im RAM mit Nullen überschrieben.
- **EncryptedSharedPreferences**: Kontaktdaten (Name, Public Key, Fingerprint) werden AES-256-GCM verschlüsselt auf dem Gerät gespeichert.

---

## Technische Hinweise

### QR-Scanner (ZXing)
Der QR-Scanner wird über `startActivityForResult` mit dem festen Request-Code `42` gestartet – **nicht** über den `ScanContract` der Activity Result API. Hintergrund: `FragmentActivity` begrenzt Request-Codes auf 16 Bit (max. 65535); der `ScanContract` generiert intern größere Codes, was zu einem `IllegalArgumentException`-Crash führen würde.

### Biometrie
`BiometricAuthHelper` nutzt `androidx.biometric:1.1.0` mit dem `FragmentActivity`-Konstruktor von `BiometricPrompt`. Daher erbt `MainActivity` von `FragmentActivity` statt von `ComponentActivity`.

---

## Build & Installation

Voraussetzungen:
- Android Studio Meerkat (2024.3.1) oder neuer
- JDK 11+
- Android SDK 36


---

## Lizenz

Dieses Projekt ist privat und nicht zur öffentlichen Verbreitung freigegeben.

