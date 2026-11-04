# Haus-Sprachsteuerung / Alfonso – verbindliche Projektanweisungen

## Fester Signierschlüssel: zuerst prüfen

Dieses Projekt hat bereits einen festen Android-Signierschlüssel. Alle Arbeiten
in Visual Studio Code, Codex und Android Studio müssen diese Identität erhalten.

| Eigenschaft | Verbindlicher Wert / Datei |
|---|---|
| Application-ID | `de.parip69.homesprachsteuerung` |
| Keystore | `signing/home-sprachsteuerung.jks` |
| Schlüsselalias | `homevoice` |
| Zugangsdaten zum Keystore | `signing/signing.properties` |
| Gradle-Anbindung | `app/build.gradle.kts`, Signing-Konfiguration `projectKey` |
| Build-Typen | **Debug und Release verwenden beide `projectKey`** |
| Zertifikat SHA-256 | `C3:25:61:AF:99:BF:05:93:17:8A:31:FA:6B:5F:CE:55:A2:41:9A:D1:21:18:49:99:C6:EF:D2:DF:69:18:B5:64` |

1. Keystore, Alias, Signing-Einstellungen und Application-ID nicht neu erzeugen,
   austauschen oder auf einen automatisch erzeugten Debug-Key umstellen.
2. Fehlt eine dieser Dateien, zuerst den vorhandenen Originalschlüssel aus dem
   Projekt wiederherstellen. Keinen Ersatzschlüssel erzeugen und keine APK mit
   abweichender Signatur als Update ausgeben.
3. Keystore und `signing.properties` lokal bei vollständiger Sicherung und
   Migration erhalten. Im öffentlichen Repository nicht versionieren; der
   gemeinsame GitHub-Build verwendet die vorhandenen Dateien aus geschützten
   Secrets `ANDROID_KEYSTORE_BASE64` und `ANDROID_SIGNING_PROPERTIES`.
4. Passwörter und Schlüsselinhalt nicht in Antworten, Logs oder Dokumentation
   ausgeben. Dokumentation verweist nur auf die Dateien.
5. Vor Ausgabe einer APK mit `./gradlew signingReport` die Build-Konfiguration
   und mit `apksigner verify --print-certs <APK>` die fertige APK prüfen.
   Der SHA-256-Zertifikatsfingerabdruck muss dem oben genannten Wert entsprechen.
6. Für ein installierbares Update zusätzlich die Application-ID beibehalten und
   den `versionCode` gegenüber der zuvor ausgegebenen APK erhöhen.

Weitere Details: [signing/SIGNATUR.md](signing/SIGNATUR.md).

## Entwicklung und Prüfung

- Das Repository-Hauptverzeichnis direkt in VS Code oder Android Studio öffnen.
- macOS/Linux: `./gradlew assembleDebug testDebugUnitTest lintDebug`.
- Windows/PowerShell: `.\gradlew.bat assembleDebug testDebugUnitTest lintDebug`.
- Android-SDK lokal über `local.properties` einrichten; diese Datei nicht committen.
- Smartphone-Aufnahme, Widget und vorhandene Profile erhalten.
- Keine Passwörter, MQTT-Topics oder Fibaro-Variablennamen ungefragt verändern.
- Automatisierte Tests dürfen keine produktiven Haussteuerungsbefehle auslösen.
- Android-Auto-Grenzen und nicht durchgeführte Geräteprüfungen stehen in
  [ANDROID_AUTO.md](ANDROID_AUTO.md). DHU- oder Fahrzeugerfolg nicht erfinden.
