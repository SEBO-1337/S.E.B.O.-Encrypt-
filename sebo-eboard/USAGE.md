# S.E.B.O. E-Board - Verwendung

## Kontakt-Auswahl in der Tastatur

Die S.E.B.O. E-Board Tastatur verfügt jetzt über eine integrierte Kontakt-Auswahl-Funktion:

### Features
- **Kontakt-Leiste**: Oberhalb der Tastatur wird der aktuell aktive Kontakt angezeigt
- **Kontakt-Auswahl-Button**: Mit dem Button "👤 Kontakt wählen" kannst du zwischen verschiedenen Kontakten wechseln
- **Status-Anzeige**: 
  - 🔑 [Name] - Kontakt aktiv mit SessionKey
  - ⚠️ [Name] (kein Key) - Kontakt ohne SessionKey
  - ⚠️ Kein Kontakt - Keine Kontakte verfügbar

### Verwendung
1. Öffne ein Textfeld (z.B. in einer Messaging-App)
2. Tippe auf das Tastatur-Icon unten in der Navigationsleiste
3. Wähle "S.E.B.O. E-Board" als Tastatur
4. Tippe auf "👤 Kontakt wählen" in der Tastatur
5. Wähle einen Kontakt aus der Liste (PopupWindow wird angezeigt)
6. Nutze die 🔒 und 🔓 Tasten zum Ver- und Entschlüsseln

### Kontakt-Auswahl-Dialog
Der Dialog (PopupWindow) zeigt:
- **Kontaktname** in Fettschrift
- **Status** des SessionKeys
  - "🔑 SessionKey verfügbar" - Bereit für Ver-/Entschlüsselung
  - "⚠️ Kein SessionKey - bitte App öffnen" - SessionKey muss erst generiert werden
- **Aktiver Kontakt** wird mit einem grünen Häkchen (✓) markiert
- **Schließen-Button** (✕) zum manuellen Schließen
- Der Dialog kann auch durch Tippen außerhalb geschlossen werden

### Layout-Verbesserungen
- Erhöhter Abstand am unteren Rand der Tastatur (16dp)
- Verhindert Überlappung mit der Zurück-Taste und Navigations-Buttons
- Kontakt-Leiste mit Elevation für bessere Sichtbarkeit
- PopupWindow-basierter Dialog für bessere Kompatibilität mit InputMethodService

### Technische Details
- **PopupWindow statt AlertDialog**: Da InputMethodService keinen Activity-Context hat, wird ein PopupWindow verwendet
- **Automatisches Neuladen**: Kontakte werden bei jedem Öffnen neu geladen
- **SessionKey-Synchronisation**: Lädt SessionKeys aus SharedPreferences, die von der Haupt-App geschrieben werden

### Hinweise
- Kontakte müssen in der Haupt-App (S.E.B.O. Encrypt) angelegt werden
- SessionKeys werden automatisch synchronisiert über SharedPreferences
- Der zuletzt ausgewählte Kontakt wird gespeichert und beim nächsten Start wiederhergestellt
- Bei Problemen mit der Kontakt-Anzeige die Haupt-App öffnen und prüfen

### Fehlerbehebung
**Problem**: Dialog wird nicht angezeigt oder stürzt ab
- **Lösung**: PopupWindow-Implementierung wurde angepasst, um mit InputMethodService zu funktionieren
- Der Dialog benötigt keinen Activity-Context mehr

**Problem**: Tastatur überlappt mit Navigations-Buttons
- **Lösung**: Padding am unteren Rand wurde erhöht (16dp)
- Zusätzliches Padding für die KeyboardView hinzugefügt

