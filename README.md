# S.E.B.O. Encrypt

Eine Android-App zur Ende-zu-Ende-verschlüsselten Kommunikation über beliebige Messenger (z. B. WhatsApp). Nachrichten werden lokal verschlüsselt und können als Text geteilt werden – der Messenger selbst sieht nur unlesbaren Chiffretext.

**NEU:** Integrierte Custom-Tastatur **S.E.B.O. E-Board** – verschlüsseln und entschlüsseln Sie Texte direkt in jeder App, systemweit!

---

## Funktionsweise

Die App nutzt ein **ECDH-Schlüsselaustausch-Protokoll**:

1. Jede Instanz generiert beim ersten Start ein **EC-Schlüsselpaar (secp256r1)** im Android Keystore – der private Schlüssel verlässt das Gerät nie.
2. Der **öffentliche Schlüssel** wird als QR-Code angezeigt und kann mit dem Kommunikationspartner geteilt werden.
3. Durch Scannen des gegenseitigen QR-Codes wird über **ECDH** ein gemeinsames Secret berechnet, aus dem via **HKDF (RFC 5869 / SHA-256)** ein 256-Bit-AES-Schlüssel abgeleitet wird.
4. Nachrichten werden mit **AES-256-GCM** verschlüsselt (zufälliger 12-Byte-IV, 128-Bit-Auth-Tag) und als Base64-String übertragen.

---

## Features

### Haupt-App

| Feature | Beschreibung |
|---|---|
| Verschlüsseln | Text eingeben, mit aktivem Kontakt verschlüsseln, kopieren oder direkt via WhatsApp teilen |
| Entschlüsseln | Verschlüsselten Text einfügen oder aus Zwischenablage lesen, mit aktivem Kontakt entschlüsseln |
| QR-Scan | Öffentlichen Schlüssel des Kontakts per QR-Code scannen (immer im Hochformat) |
| Manuell hinzufügen | Kontakt auch per Base64-Public-Key manuell eintragen |
| Kontaktverwaltung | Kontakte umbenennen, löschen, aktiven Kontakt wechseln |
| Share-Intent | Verschlüsselte Texte direkt aus WhatsApp o. ä. in die App teilen → wird automatisch in den Entschlüsseln-Tab geladen |
| Text Processing Intent | Markieren Sie Text in jeder App und wählen Sie "Entschlüsseln" aus dem Kontextmenü (Android 6.0+) |
| Zwischenablage | Beim Öffnen der App wird die Zwischenablage automatisch auf verschlüsselten Text geprüft |
| Einstellungen | Anpassung von Design, Tastatur-Layout und Feedback-Einstellungen |

### S.E.B.O. E-Board Tastatur

| Feature | Beschreibung |
|---|---|
| 🔒 Verschlüsseln | Tippen Sie Text in **jeder App** und verschlüsseln Sie ihn direkt mit der 🔒-Taste |
| 🔓 Entschlüsseln | Empfangene verschlüsselte Nachrichten direkt in der App entschlüsseln mit der 🔓-Taste |
| 📋 Aus Zwischenablage entschlüsseln | Schnelle Entschlüsselung von kopiertem Text – perfekt für WhatsApp-Nachrichten |
| QWERTZ-Layout | Deutsches Tastaturlayout mit Shift/Caps Lock |
| Text-Auswahl | Markieren Sie Text und die Löschen-Taste entfernt den markierten Text |
| Systemweit | Funktioniert in WhatsApp, Telegram, Signal, SMS, E-Mail, Notizen – überall! |
| Auto-Sync | SessionKeys werden automatisch zwischen App und Tastatur synchronisiert |
| Kein Tippen nötig | Verschlüsseln Sie Nachrichten ohne die App zu öffnen |

**Anwendungsbeispiel für Verschlüsselung:**
1. Öffnen Sie WhatsApp
2. Wählen Sie S.E.B.O. E-Board als Tastatur
3. Tippen Sie Ihre Nachricht
4. Drücken Sie 🔒 → Text wird verschlüsselt
5. Senden Sie die Nachricht wie gewohnt

**Anwendungsbeispiel für Entschlüsselung (Clipboard):**
1. Kopieren Sie eine verschlüsselte Nachricht aus WhatsApp
2. Öffnen Sie eine beliebige App mit Textfeld
3. Aktivieren Sie S.E.B.O. E-Board als Tastatur
4. Drücken Sie 📋 "Entschlüsseln" Button
5. Der entschlüsselte Text wird direkt eingefügt

**Anwendungsbeispiel für Textauswahl-Menü:**
1. Markieren Sie einen verschlüsselten Text in einer beliebigen App
2. Das Kontextmenü erscheint
3. Tippen Sie auf "Entschlüsseln"
4. Der Text wird in der S.E.B.O. Encrypt App entschlüsselt

Der Empfänger kann die Nachricht entweder in der App oder direkt mit der Tastatur entschlüsseln!

---

## Intent-Integrationen

### Text Processing Intent
Die App registriert sich als Handler für **PROCESS_TEXT** Intent. Dies bedeutet:
- Markieren Sie Text in **jeder App** (Browser, Mail, Nachrichten, etc.)
- Das Kontextmenü zeigt einen **"Entschlüsseln"** Punkt
- Tippen Sie darauf → Text wird in S.E.B.O. Encrypt entschlüsselt
- Funktioniert auf Android 6.0+

### Share Intent
Verschlüsselte Texte können direkt aus anderen Apps geteilt werden:
- **ACTION_SEND** für Text
- Text wird automatisch im Entschlüsseln-Tab geladen
- Perfekt für WhatsApp, Telegram und andere Messenger

---

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
SEBOEncrypt/
├── app/                             # Haupt-Anwendung
│   └── src/main/java/com/sebo/seboencrypt/
│       ├── MainActivity.kt                  # Entry Point, Tab-Navigation, QR-Scanner-Start
│       ├── PortraitCaptureActivity.kt       # Scanner immer im Hochformat
│       ├── TextProcessingActivity.kt        # PROCESS_TEXT Intent Handler – Textauswahl-Menü
│       ├── engine/
│       │   └── CryptoEngine.kt              # AES-256-GCM Verschlüsselung & Entschlüsselung
│       ├── helper/
│       │   ├── ClipboardHelper.kt           # Zwischenablage lesen/schreiben
│       │   ├── KeyDerivation.kt             # HKDF – leitet AES-Key aus ECDH-Secret ab
│       │   ├── KeyboardSyncHelper.kt        # Synchronisiert SessionKeys mit Tastatur
│       │   ├── QRHelper.kt                  # QR-Code erzeugen & Public Key dekodieren
│       │   └── ShareHelper.kt               # Text via WhatsApp teilen
│       ├── manager/
│       │   └── KeystoreManager.kt           # EC-Schlüsselpaar im Android Keystore, ECDH
│       ├── model/
│       │   └── Contact.kt                   # Kontakt-Datenmodell
│       ├── repository/
│       │   └── ContactRepository.kt         # Kontakte speichern/laden (EncryptedSharedPreferences)
│       ├── viewmodel/
│       │   └── E2EEViewModel.kt             # Gesamte Business-Logik
│       └── ui/
│           ├── components/
│           │   ├── StatusBanner.kt          # Globales Status-Banner
│           │   └── keytab/
│           │       ├── MyKeySection.kt      # Eigener QR-Code anzeigen
│           │       ├── ContactsSection.kt   # Kontaktliste
│           │       └── ContactListItem.kt   # Einzelner Kontakt-Eintrag
│           ├── screens/
│           │   ├── EncryptTab.kt            # Verschlüsseln-Tab
│           │   ├── DecryptTab.kt            # Entschlüsseln-Tab
│           │   └── KeyTab.kt                # Schlüssel & Kontakte-Tab
│           └── theme/
│               └── Theme.kt                 # Material Theme
│
└── sebo-eboard/                     # Custom Keyboard Modul
    └── src/main/java/com/sebo/eboard/
        ├── CustomKeyboardService.kt         # InputMethodService – Tastatur-Logik
        ├── crypto/
        │   └── CryptoEngine.kt              # AES-GCM Verschlüsselung (identisch zur App)
        ├── manager/
        │   └── ContactManager.kt            # Lädt Kontakte & SessionKeys aus SharedPreferences
        └── model/
            └── KeyboardContact.kt           # Vereinfachtes Contact-Modell
```

---

## Sicherheitshinweise

- **Der private Schlüssel verlässt das Gerät niemals** – er wird im Android Keystore gespeichert und ist nicht exportierbar. Auf unterstützten Geräten wird StrongBox (Hardware-Security-Modul) verwendet.
- Verschlüsselte Nachrichten sind **nur mit dem richtigen Kontakt** entschlüsselbar – jeder Kontakt hat ein eigenes ECDH-Session-Secret.
- Kontaktdaten (inkl. Session Keys) werden in `EncryptedSharedPreferences` gespeichert.
- **Custom Keyboard Sicherheit**: Die S.E.B.O. E-Board Tastatur greift nur auf SessionKeys zu, die von der Haupt-App via SharedPreferences bereitgestellt werden. Der private ECDH-Schlüssel bleibt im Android Keystore und ist für die Tastatur nicht zugänglich.
- **Kein Internet-Zugriff**: Die Tastatur benötigt keine Netzwerkberechtigung und sendet keine Daten.

### ⚠️ Wichtig bei Custom Keyboards:
Android zeigt beim Aktivieren einer Custom Keyboard eine Sicherheitswarnung, da Tastaturen theoretisch alle Eingaben mitlesen können. S.E.B.O. E-Board ist Open Source – Sie können den Code überprüfen und selbst kompilieren.

---

## Build & Installation

### Voraussetzungen:
- Android Studio Ladybug oder neuer
- JDK 11+
- Android SDK 36

### Installation:

**S.E.B.O. E-Board Tastatur aktivieren**
   - App öffnen
   - Button "S.E.B.O. E-Board aktivieren" drücken
   - In den Android-Einstellungen "S.E.B.O. E-Board" aktivieren
   - Sicherheitswarnung bestätigen

**Tastatur verwenden**
   - In beliebiger App ein Textfeld antippen
   - Leertaste gedrückt halten oder Tastatur-Symbol (🌐) drücken
   - "S.E.B.O. E-Board" auswählen
   - Text tippen und mit 🔒 verschlüsseln oder mit 🔓 entschlüsseln

### Tastatur-Workflow:

```
┌─────────────────────────────────────────────────────────┐
│              1. Kontakt in App hinzufügen               │
│              (QR-Code scannen)                          │
└─────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────┐
│         2. SessionKey wird automatisch mit              │
│            Tastatur synchronisiert                      │
└─────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────┐
│      3. In beliebiger App S.E.B.O. E-Board wählen      │
└─────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────┐
│    4. Text tippen → 🔒 drücken → Verschlüsselt!        │
│       Verschlüsselt empfangen → 🔓 → Entschlüsselt!    │
└─────────────────────────────────────────────────────────┘
```

---

## Lizenz

Dieses Projekt ist privat und nicht zur öffentlichen Verbreitung freigegeben.
