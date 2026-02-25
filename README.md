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
| 🔓 **Entschlüsseln** | Verschlüsselten Text einfügen oder aus Zwischenablage lesen, mit aktivem Kontakt entschlüsseln |
| 📷 **QR-Scan** | Öffentlichen Schlüssel des Kontakts per QR-Code scannen (immer im Hochformat) |
| ✏️ **Manuell hinzufügen** | Kontakt auch per Base64-Public-Key manuell eintragen |
| 👥 **Kontaktverwaltung** | Kontakte umbenennen, löschen, aktiven Kontakt wechseln |
| 📥 **Share-Intent** | Verschlüsselte Texte direkt aus WhatsApp o. ä. in die App teilen → wird automatisch in den Entschlüsseln-Tab geladen |
| 📋 **Zwischenablage** | Beim Öffnen der App wird die Zwischenablage automatisch auf verschlüsselten Text geprüft |

---

## Technologie-Stack

| Komponente | Technologie |
|---|---|
| Sprache | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architektur | MVVM (`AndroidViewModel`) |
| Verschlüsselung | AES-256-GCM (`javax.crypto`) |
| Schlüsselaustausch | ECDH secp256r1 (Android Keystore) |
| Schlüsselableitung | HKDF-SHA256 (Bouncy Castle) |
| QR-Code | ZXing Android Embedded |
| Kontaktspeicherung | `EncryptedSharedPreferences` (Jetpack Security Crypto) |
| Min. Android-Version | Android 7.0 (API 24) |
| Target SDK | Android 16 (API 36) |

---

## Projektstruktur

```
app/src/main/java/com/sebo/seboencrypt/
├── MainActivity.kt                  # Entry Point, Tab-Navigation, QR-Scanner-Start
├── PortraitCaptureActivity.kt       # Scanner immer im Hochformat
├── KeyDerivation.kt                 # HKDF – leitet AES-Key aus ECDH-Secret ab
├── QRHelper.kt                      # QR-Code erzeugen & Public Key dekodieren
├── ClipboardHelper.kt               # Zwischenablage lesen/schreiben
├── ShareHelper.kt                   # Text via WhatsApp teilen
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
- Kontaktdaten (inkl. Session Keys) werden in `EncryptedSharedPreferences` gespeichert.

---

## Build & Installation

Voraussetzungen:
- Android Studio Ladybug oder neuer
- JDK 11+
- Android SDK 36

---

## Lizenz

Dieses Projekt ist privat und nicht zur öffentlichen Verbreitung freigegeben.

