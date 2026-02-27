# Installation & Test-Anleitung

## APK installieren
```powershell
# APK auf dem Gerät installieren
adb install -r C:\Users\sdend\Android\SEBOEncrypt\app\release\app-release.apk
```

## Tastatur aktivieren

### 1. In Android-Einstellungen
1. Öffne **Einstellungen** → **System** → **Sprachen & Eingabe**
2. Tippe auf **Bildschirmtastatur**
3. Tippe auf **Tastaturen verwalten**
4. Aktiviere **S.E.B.O. E-Board**

### 2. Tastatur verwenden
1. Öffne eine beliebige App mit Textfeld (z.B. Notizen, WhatsApp)
2. Tippe in ein Textfeld
3. Tippe auf das Tastatur-Symbol in der Navigationsleiste
4. Wähle **S.E.B.O. E-Board**

## Features testen

### Kontakt-Auswahl testen
1. **Vorbereitung in der Haupt-App:**
   - Öffne die S.E.B.O. Encrypt App
   - Füge mindestens einen Kontakt hinzu
   - Stelle sicher, dass ein SessionKey generiert wurde

2. **In der Tastatur:**
   - Öffne ein Textfeld
   - Wechsle zur S.E.B.O. E-Board Tastatur
   - Prüfe die Kontakt-Leiste oberhalb der Tastatur
   - Tippe auf "👤 Kontakt wählen"
   - Der PopupWindow sollte erscheinen (kein Crash!)
   - Wähle einen Kontakt aus
   - Der Dialog schließt sich
   - Die Kontakt-Leiste zeigt den gewählten Kontakt

### Ver-/Entschlüsselung testen
1. Gib einen Text ein: "Hallo Welt"
2. Tippe auf 🔒 (Verschlüsseln)
   - Der Text wird verschlüsselt
   - Format: `{SEBO}base64...`
3. Tippe auf 🔓 (Entschlüsseln)
   - Der verschlüsselte Text wird wieder lesbar

### Layout testen
1. Prüfe, dass die Tastatur **NICHT** die Navigations-Buttons überlappt
2. Die Zurück-Taste links unten sollte vollständig sichtbar sein
3. Die Kontakt-Leiste sollte oben klar sichtbar sein
4. Das Spacing sollte angenehm sein

## Bei Problemen

### Logcat ansehen
```powershell
# Echtzeit-Logs ansehen
adb logcat | Select-String "sebo"

# Crash-Logs filtern
adb logcat | Select-String -Pattern "(FATAL|ERROR|WindowManager)"
```

### Tastatur neu laden
1. Deaktiviere die Tastatur in den Einstellungen
2. Aktiviere sie erneut
3. Oder: Neustart des Geräts

### App neu installieren
```powershell
# App deinstallieren
adb uninstall com.sebo.seboencrypt

# Neu installieren
adb install C:\Users\sdend\Android\SEBOEncrypt\app\release\app-release.apk
```

## Bekannte Einschränkungen
- Deprecated APIs (Keyboard/KeyboardView) werden verwendet
  - Funktional stabil, aber veraltet
  - Für moderne Implementierung: MaterialKeyboard erwägen
- PopupWindow zeigt sich zentral (nicht optimal, aber funktional)
- Kontakt-Synchronisation nur über SharedPreferences

## Erfolgreiche Tests
✅ Build erfolgreich ohne Fehler
✅ PopupWindow statt AlertDialog (kein BadTokenException)
✅ Layout-Überlappung behoben
✅ Kontakt-Auswahl funktioniert
✅ Ver-/Entschlüsselung funktioniert
✅ Persistente Kontakt-Speicherung

