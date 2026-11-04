# Alfonso 17

Die sichtbare Versionsnummer lautet ab jetzt nur **17**, danach **18**, **19** usw.
Die interne Android-Buildnummer wird für sichere Updates weiter erhöht, aber
nicht in der Oberfläche angezeigt. APK-Dateiname: `Alfonso-17.apk`.

## App aktualisieren (GitHub Releases)

Im Drei-Punkte-Menü der Smartphone-Übersicht **App aktualisieren** öffnen und
**Nach Updates suchen** drücken. Die Seite zeigt installierte und veröffentlichte
Version als kurze Versionsnummer sowie Release-Änderungen. **Herunterladen und
installieren** lädt die APK mit Fortschritt über Androids DownloadManager; der
Download bleibt bei Bildschirmwechsel und Prozessende erhalten. Abbrechen ist
möglich. Anschließend werden Größe, SHA-256, Paket, höhere Versionsnummer und
kryptografische APK-Signatur geprüft. Nur eine geprüfte private Dateikopie wird
über einen eng begrenzten FileProvider an den Android-Paketinstaller übergeben.
Falls erforderlich **Installation durch Alfonso erlauben** öffnen und Androids
„Aus dieser Quelle zulassen“ aktivieren. Nach Rückkehr wird erneut geprüft und
die Installation fortgesetzt. Bei Abbruch kann sie erneut aufgerufen werden.
Es gibt keine stille Installation, Deinstallation oder Datenrücksetzung.

Quelle ist der neueste reguläre veröffentlichte Release von
`parip69/Haus-Sprachsteuerung` (`GET /repos/parip69/Haus-Sprachsteuerung/releases/latest`).
Entwürfe und Vorabversionen werden ausgeschlossen. Ohne Release erscheint
„Noch kein Update veröffentlicht“. Das API-`latest` ist nach GitHub-Regeln der
aktuelle stabile Release: zukünftige Veröffentlichungen deshalb auch als neuesten
Release kennzeichnen. Tags und Dateinamen werden nicht als Versionszahlen geparst.
Die App verwendet die tatsächliche `browser_download_url` des APK-Assets.
Private Repositories/Assets sind ohne Anmeldung nicht als öffentliche Updatequelle
nutzbar; ein GitHub-404 kann auch ein verborgenes privates Repository bedeuten.
Keine Tokens in die App aufnehmen. Keine GitHub Actions oder zusätzlichen Dienste
sind nötig. Die App benötigt Internetzugriff auf `api.github.com`, `github.com`
und die von GitHub verwendeten Release-Downloadhosts.

### Künftigen Release erstellen

1. In `app/build.gradle.kts` den **versionCode strikt erhöhen** und versionName
   anpassen. Application-ID und `projectKey` unverändert lassen. Der App-Name bleibt
   **Alfonso**, damit „OK Google, öffne Alfonso“ weiterhin derselbe Aufruf bleibt.
   Die Zuordnung durch Google/Gemini muss auf dem Handy getestet werden.
2. Mit JDK 17 und SDK 35 lokal oder in Codex bauen:
   `./gradlew assembleDebug assembleRelease testDebugUnitTest lintDebug signingReport`.
   Die Updater-Tests prüfen auch die echte signierte Release-APK und manipulierte
   bzw. abgeschnittene Kopien. Daher baut die JVM-Testaufgabe ebenfalls Release.
3. `ANDROID_HOME` setzen und mit Python 3.11+ `python3 scripts/prepare-release.py`
   ausführen. Das Skript prüft per `apksigner` und `aapt` Signatur, Paket und Version
   gegen die Build-Konfiguration und erzeugt unter `app/build/outputs/github-release/`
   `Alfonso-<letzte Versionsnummer>.apk` (aktuell `Alfonso-17.apk`) und `alfonso-update.json`.
4. Beide geprüften Dateien zusammen mit Änderungsbeschreibung an einen regulären
   GitHub Release hängen, etwa Tag `v17`, Titel `Alfonso 17`.
   Erst mit beiden Assets veröffentlichen; vorhandene Release-Dateien nicht unter
   derselben Version austauschen. Für spätere Updates immer einen neuen Release
   und höheren versionCode verwenden. SHA-256 ist eine Integritätsprüfung;
   die feste APK-Signatur schützt zusätzlich gegen fremde APKs.
5. Auf einem Handy mit älterer Alfonso-Version Update, Freigaberückkehr,
   Installationsabbruch und Erhalt von Einstellungen/Verlauf prüfen. Die erste
   Installation dieser Updater-Version erfolgt noch durch Öffnen der APK.

Metadatenschema des Release-Assets `alfonso-update.json`:

```json
{
  "applicationId": "de.parip69.homesprachsteuerung",
  "versionCode": 28,
  "versionName": "17",
  "apkFileName": "Alfonso-17.apk",
  "sha256": "<64 Hex-Zeichen, vom Vorbereitungsskript berechnet>"
}
```

Als Vorbilder wurden die zugänglichen nativen Updater in
`parip69/RechenGuruLGI_AndroidAPK` (DownloadManager, Wiederaufnahme, Android-Freigabe)
und `parip69/BarcodeAudi_AndroidAPK` (Installer/FileProvider) geprüft. Deren
versionsabhängig konstruierte Downloadadressen wurden für Alfonso nicht übernommen.
Im geprüften Stand von `parip69/Blitz_lesen_Isabella_AndroidAPK` wurde kein
vergleichbarer nativer Kotlin-Updater gefunden; keine Funktionsgleichheit zugesagt.

## Fester Signierschlüssel – wichtig für VS Code und Codex

**Für alle weiteren APKs denselben vorhandenen Schlüssel verwenden.**
Die verbindlichen KI-Anweisungen stehen direkt in [AGENTS.md](AGENTS.md).

- Keystore: [signing/home-sprachsteuerung.jks](signing/home-sprachsteuerung.jks)
- Signing-Konfiguration: [signing/signing.properties](signing/signing.properties)
- Fingerabdruck und Details: [signing/SIGNATUR.md](signing/SIGNATUR.md)
- Debug und Release sind bereits in `app/build.gradle.kts` an diesen Schlüssel gebunden.
- Application-ID: `de.parip69.homesprachsteuerung`.

Den Repository-Hauptordner in VS Code öffnen. Signatur mit
`./gradlew signingReport` beziehungsweise unter Windows mit
`.\gradlew.bat signingReport` prüfen. Keinen neuen Keystore erzeugen.

Privater Android-Auto-IoT-Prototyp: siehe [ANDROID_AUTO.md](ANDROID_AUTO.md).
Die Smartphone-Funktionen bleiben erhalten. Auto-Spracheingabe hängt vom Host ab;
DHU- und Fahrzeugbetrieb sind bisher nicht nachgewiesen.

Kotlin-App für Android mit deutscher Spracherkennung, dauerhaftem Verlauf,
Fibaro-REST und MQTT. Projekt für Android Studio und VS Code / Gradle.

## Sofort ausprobieren

1. ZIP entpacken. Den Ordner `Home-Sprachsteuerung` in Android Studio mit **Open** öffnen.
2. Gradle-JDK **17** einstellen; SDK Platform **35** und Build Tools **35.0.0** installieren lassen.
3. Gradle synchronisieren und **Run** mit Deinem Handy als Ziel ausführen.
4. Der **Testmodus · nur Verlauf** ist bereits aktiviert. Es ist kein Server nötig.
5. Die Übersicht „Hauspilot“ bleibt offen. Mikrofon antippen, Berechtigung
   zulassen und bei „Bitte jetzt sprechen“ einen Satz sprechen.
   Für eine einzelne Hintergrundaufnahme den zweiten Eintrag „Hauspilot Sprache“ öffnen.
6. Du kannst nach dem Start zu einer anderen App wechseln. Die laufende einzelne
   Erkennung wird von einem Mikrofon-Foreground-Service mit Benachrichtigung getragen.
7. App wieder öffnen: Der erkannte Rohtext steht mit Uhrzeit und Status im Verlauf,
   auch wenn nichts an Fibaro oder MQTT geschickt wurde.

**Ein Aufruf von Hauspilot Sprache oder ein Tastendruck erfasst genau einen Satz.** Kein Dauerlauschen, keine automatische
neue Aufnahme nach einem Satz, kein Start beim Hochfahren. Nach 45 Sekunden wird
eine hängende Erkennung beendet. In der Benachrichtigung kann die Aufnahme abgebrochen werden.

Die App benötigt Android 8.0 / API 26 oder neuer und einen aktivierten
Android-Spracherkennungsdienst mit Deutsch-Unterstützung. Der Dienst kann
Internet benötigen und Audiodaten zum Anbieter übertragen. Die App speichert
selbst keine Audioaufnahmen und verwendet keine Analyse- oder Werbe-SDKs.
Eine bestehende Unterhaltung von Google Assistant oder Gemini wird nicht mitgelesen.

## Verlauf und Testmodus

Die letzten **100 erkannten Sätze** werden lokal in einer SQLite-Datenbank gespeichert:
Uhrzeit, unveränderter Text, Zielmodus und Versandstatus. Der Verlauf bleibt nach
Neustart erhalten und kann im Menü mit Bestätigung gelöscht werden.

Der Rohtext wird vor einem Versand gespeichert. Schlägt die Speicherung fehl,
wird nichts gesendet. Ein nach Prozessabbruch offener Eintrag wird beim nächsten
App-Start als „Unterbrochen · Zustellung unklar“ gekennzeichnet. Es gibt keine
Warteschlange und keinen automatischen späteren Versand alter Testeinträge.

Im Zahnrad-Menü kannst Du zwischen diesen Modi wählen:

- **Testmodus · nur Verlauf**: Erkennen und speichern, ohne Netzwerkversand.
- **Fibaro REST-API**: Speichern und als JSON an eine globale Variable senden.
- **MQTT**: Speichern und den Rohtext als UTF-8-Payload publizieren.

Einstellungen und Zugangsdaten werden in SharedPreferences gespeichert.
Ein gestarteter Vorgang verwendet einen unveränderlichen Konfigurationsstand.
Bildschirmdrehung startet weder eine zweite Erkennung noch einen zweiten Versand.
Android bzw. der gewählte Sprachdienst können Hintergrundaufnahmen trotzdem
unterbrechen; das konkrete Samsung-Gerät muss damit erprobt werden.

## Fibaro: URL und invokeScenes

Im Home Center eine normale, frei beschreibbare globale Textvariable
**GesprochenerText** anlegen, keine Variable mit festen Auswahlwerten.

Einstellungen:

- Basis-URL beispielsweise `http://192.168.1.10`, ohne `/api` oder Variablenpfad.
- Variablenname exakt wie im Home Center.
- Fibaro-Benutzername und Passwort mit den erforderlichen Rechten.
- Schalter **Fibaro: invokeScenes: true mitsenden** nach Bedarf ein- oder ausschalten.

Die vollständige Zieladresse wäre in diesem Beispiel:

```text
http://192.168.1.10/api/globalVariables/GesprochenerText
```

Die Adresse allein im Browser aufzurufen entspricht nicht dem benötigten PUT:

```http
PUT /api/globalVariables/GesprochenerText
Authorization: Basic <Base64 von Benutzername:Passwort in UTF-8>
Content-Type: application/json; charset=utf-8
Accept: application/json

{"value":"Wohnzimmerlicht einschalten","invokeScenes":true}
```

`invokeScenes` steht im JSON-Body, nicht in der URL; `true` ist ein kleingeschriebener
JSON-Boolescher Wert ohne Anführungszeichen. Bei ausgeschaltetem Schalter lautet
der Body nur `{"value":"Wohnzimmerlicht einschalten"}`.

Die globale Variable und der zusätzliche Szenenaufruf sind verschiedene Aspekte:
`value` enthält den neuen Wert; `invokeScenes` dient in HC2-Integrationsbeispielen
dem Auslösen abhängiger Szenen. Der HTTP-Pfad und der Parameter sind in der
unten verlinkten ursprünglichen HC2/HCL-Implementierung gemeinsam zu sehen.
Das ist ein Implementierungsbeleg, keine Zusicherung für jede Firmware.
Das Verhalten Deiner Szenen und wiederholter identischer Texte muss am eigenen
Home Center geprüft werden. Diese Lieferung hat nicht auf Dein Gerät zugegriffen.

Eine Szene muss den Text noch auswerten. Die App ergänzt keine intelligente
Interpretation und schaltet nicht selbstständig Geräte. HTTP 2xx bedeutet, dass
der Server den Request erfolgreich beantwortet hat. Andere Codes erscheinen
als Fehler, etwa 401, 403, 404 oder 500. Redirects und automatische
HTTP-Verbindungswiederholungen sind deaktiviert.

## MQTT

- URI z. B. `tcp://192.168.1.20:1883` oder `ssl://broker.example:8883`.
- Separates Portfeld passend auf **1883** bzw. **8883** einstellen.
  Steht der Port zusätzlich in der URI, müssen beide übereinstimmen.
- Eine Client-ID wird pro Installation erzeugt; sie muss pro gleichzeitig
  verbundenem Gerät eindeutig bleiben (1–23 Buchstaben/Ziffern).
- Ziel-Topic z. B. `smarthome/sprache`, ohne Wildcards `+` oder `#`.
- Benutzername und Passwort sind optional.

Eclipse-Paho-Java-Client im IO-Thread, MQTT 3.1.1, Clean Session,
**QoS 1**, **Retain aus**, keine automatische Wiederverbindung. Pro Satz wird
verbunden, publiziert, PUBACK abgewartet und die Verbindung geschlossen.
Der veraltete Paho-Android-Hintergrunddienst wird nicht benötigt.

„Erfolgreich“ bestätigt den Empfang durch den Broker, nicht die ausgeführte
Geräteaktion. QoS 1 erlaubt auf Protokollebene doppelte Zustellung. Exakt einmalige
Aktionen erfordern passende Empfängerlogik. Nach Verbindungsfehlern kann die
Zustellung unklar sein: Vor einem manuellen Wiederholen Ziel oder Topic prüfen.

## Fester Signierschlüssel – auch in VS Code

Auf ausdrücklichen Wunsch sind **derselbe projektbezogene Schlüssel und seine
Passwortdatei Bestandteil des Projekts**, für ein privates Repository:

- `signing/home-sprachsteuerung.jks`
- `signing/signing.properties`
- `signing/SIGNATUR.md` mit dem SHA-256-Fingerabdruck

**Debug und Release verwenden beide diesen Schlüssel.** Es wird nicht automatisch
ein gerätespezifischer Android-Debugschlüssel benutzt. Die Dateien werden von
`.gitignore` nicht ausgeschlossen. Kein Schlüssel eines fremden Projekts wurde übernommen.

Diese Dateien unverändert sichern und bei weiteren Checkouts mitnehmen. Gradle
liest die Pfade relativ zum Projekt; Rechner und IDE spielen dafür keine Rolle.
Wer Schlüssel und Passwort hat, kann ebenfalls passende APKs signieren. Das
Repository deshalb privat halten. Für Updates zusätzlich dieselbe applicationId
beibehalten und den `versionCode` erhöhen. Die Erstversion ist 1 / 1.0.0.
Play App Signing kann eine eigene Signatur für Store-Installationen verwenden.

## Bauen und prüfen

Windows:

```powershell
.\gradlew.bat assembleDebug assembleRelease testDebugUnitTest lintDebug
```

Linux / macOS:

```sh
./gradlew assembleDebug assembleRelease testDebugUnitTest lintDebug
```

Die APKs liegen in `app/build/outputs/apk/debug/` bzw. `release/`.
In VS Code: **Terminal → Aufgabe ausführen → Android: Test-APK bauen** oder
**Android: Release-APK bauen**. Die passenden Gradle-Tasks sind mitgeliefert.
Auch **GitHub holen** und **GitHub hochladen** sind als Tasks vorhanden;
sie setzen ein eingerichtetes Git-Repository mit Remote voraus. Der Push-Task
schiebt vorhandene Commits und erstellt keine automatischen Commits.

## Struktur

| Datei | Aufgabe |
|---|---|
| `AndroidManifest.xml` | Activities, Mikrofon-Foreground-Service, Berechtigungen |
| `activity_main.xml` | Mikrofon, Status und Verlauf |
| `settings_activity.xml` | Einstellungen einschließlich Testmodus und invokeScenes |
| `MainActivity.kt` | Berechtigungen, Service-Start und UI |
| `CaptureService.kt` | SpeechRecognizer und ein Satz im Hintergrund, IO-Versand |
| `BridgeApplication.kt` | Gemeinsamer Zustand unabhängig von der Activity |
| `BridgeRepository.kt` | SQLite-Verlauf und beobachtbarer UI-Zustand |
| `SettingsActivity.kt`, `SettingsStore.kt` | Formular und SharedPreferences |
| `AppConfig.kt` | Zielvalidierung und URL-Erzeugung |
| `NetworkSender.kt` | OkHttp-PUT / Paho-MQTT / Testmodus ohne Versand |
| `EdgeInsets.kt` | Systemleisten und Bildschirmtastatur |

Kotlin-Pfad: `app/src/main/java/de/parip69/homesprachsteuerung/`.
Layout-Pfad: `app/src/main/res/layout/`.
Die Trennung von Service, UI und Transport hält Aufnahmen beim Bildschirmwechsel
am Leben und macht den Netzwerkteil unabhängig testbar.

## Daten und Transport

Wie angefordert werden Passwörter im privaten App-Speicher als Klartext gespeichert;
HTTP Basic Auth kodiert erst für den Header in Base64. Das ist keine Verschlüsselung.
Backups und Datentransfer sind deaktiviert; SharedPreferences und Datenbank sind
zusätzlich in den Datenextraktionsregeln ausgeschlossen. OEM-Regeln können die
Wirksamkeit beeinflussen. Zugangsdaten werden nicht protokolliert.

HTTP und `tcp://` erlauben die gewünschte lokale unverschlüsselte Verbindung.
HTTPS und `ssl://` prüfen normale vertrauenswürdige Zertifikate einschließlich
Hostnamen. Es gibt keinen „Trust all“-Modus. HTTP-Cleartext ist appweit aktiviert,
weil die Ziel-IP erst zur Laufzeit eingetragen wird.

## Vor täglichem Einsatz auf Deinem Handy prüfen

1. Im voreingestellten Testmodus ohne URL sprechen und den Verlauf nach Neustart prüfen.
2. Aufnahme starten, zu anderer App wechseln und den erkannten Text danach kontrollieren.
3. Mikrofon ablehnen und Aufnahme über die Benachrichtigung abbrechen.
4. Erst eine Testvariable bzw. ein Testtopic ohne schaltende Szene konfigurieren.
5. Richtiges/falsches Passwort und unerreichbares Ziel testen.
6. Für Fibaro `invokeScenes` gezielt mit einer ungefährlichen Testszene prüfen.
7. Bildschirm während Aufnahme und Versand drehen; kein zweiter App-Versand.
8. Später ein Update mit höherem versionCode und demselben Schlüssel installieren.

Der konkrete Prüfstand steht in `PRUEFBERICHT.md`. Eine pauschale Produktionsfreigabe
setzt zusätzlich Tests auf Deinem Gerät und mit Deiner Anlage voraus.

## Quellen

- SpeechRecognizer: https://developer.android.com/reference/android/speech/SpeechRecognizer
- Mikrofon-Foreground-Service: https://developer.android.com/develop/background-work/services/fgs/service-types
- Gradle-/JDK-Kompatibilität: https://developer.android.com/build/releases/agp-8-9-0-release-notes
- Fibaro REST API (Hersteller): https://manuals.fibaro.com/content/other/FIBARO_System_REST_API.pdf
- Ursprünglicher HC2/HCL-Client mit PUT und invokeScenes (2017): https://gist.github.com/tcastberg/b4ceb4763c2b6c86f8b8d22658281de3
- Eclipse Paho Java: https://eclipse.dev/paho/clients/java/
- OkHttp: https://github.com/square/okhttp

## Icon-Auswahl ab Version 1.0.1

Zwei generierte Entwürfe sind integriert: `mikrofon` (Standard) und `sprachwelle`.
Beide APKs haben Paketname `de.parip69.homesprachsteuerung`, versionCode **2**,
versionName **1.0.1** und denselben festen Signierschlüssel wie Version 1.0.0.
Sie aktualisieren dieselbe App; es werden keine zwei getrennten Apps installiert.
App-Daten und Einstellungen bleiben bei einem normalen Update erhalten.

```powershell
.\gradlew.bat assembleRelease -PiconVariant=mikrofon
.\gradlew.bat assembleRelease -PiconVariant=sprachwelle
```

Ohne Parameter gilt `mikrofon`. Der gewählte Entwurf wird als adaptives Android-
Startsymbol inklusive runder Launcher-Maske eingebunden. Die Originalbilder liegen
unter `app/src/icons/`. Die Benachrichtigung behält ihr geeignetes einfaches Mikrofon-
Symbol. Ein APK-Download öffnet nach Bestätigung den Android-Paketinstaller;
ein Bild selbst kann keine App oder ein Startsymbol installieren.

Bildgestaltung: zwei generierte Entwürfe, schwarzes Haus, Chromkanten, cyanblaue
Akzente; Variante 1 mit Mikrofon, Variante 2 mit Sprachwelle. Für die Erzeugung wurde
die integrierte Bildgenerierung verwendet. Die PNG-Originale sind unverändert eingebunden.

## Version 1.0.2 – kleineres Haus-Icon

Das gewählte Mikrofon-Haus bleibt unverändert. Der Abstand wird relativ zur
Icon-Fläche auf 18 Prozent je Seite eingestellt; absolute dp-Abstände werden
nicht mehr verwendet. Dadurch bleibt der Abstand auch bei unterschiedlichen
Launcher-Größen proportional. versionCode 3, fester Schlüssel unverändert.
Diese Version ergänzt keine Google-Assistant- oder Gemini-Abfangfunktion.

## Version 1.0.3 – App-Name „LGI Haus“ und optionaler Aufnahmestart

Der sichtbare App-Name ist jetzt **LGI Haus**. Paketname, Signierschlüssel,
SharedPreferences und SQLite-Verlauf bleiben gleich; normales Update möglich.
VersionCode 4, versionName 1.0.3. Das gewählte Mikrofon-Haus bleibt mit 18 Prozent Rand.

Neuer optionaler Schalter: **Beim Öffnen sofort zuhören** (Standard: aus).
Zum Freihand-Test einschalten und speichern, App verlassen, dann auf dem Handy:

1. „OK Google, öffne LGI Haus“.
2. Warten, bis „Ich höre zu“ erscheint bzw. der Erkennungsdienst bereit ist.
3. „Wohnzimmerlicht einschalten“ sprechen.

Das sind **zwei getrennte Spracheingaben**. Die App übernimmt keinen vorher bei
Google oder Gemini erkannten Satz. Ob Google diesen kurzen App-Namen zuverlässig
öffnet und das Mikrofon rechtzeitig freigibt, muss auf dem Handy geprüft werden.
„OK Google, Haus“ allein ist nicht als zuverlässiger Aufruf zugesichert.

Bei ausgeschaltetem Schalter bleibt die bisherige Bedienung mit Mikrofontaste.
Bei eingeschaltetem Schalter kann auch ein manuelles neues Öffnen eine Aufnahme
starten. Rückkehr aus Einstellungen oder Bildschirmdrehung startet nicht erneut.
Pro Erkennung wird weiterhin nur ein Satz erfasst, kein Dauerlauschen.


## Version 1.0.4 – Hauspilot

Der sichtbare App-Name in Launcher, Titelleiste und Aufnahme-Benachrichtigung
ist **Hauspilot**. Aufruf zum Testen: „OK Google, öffne Hauspilot“.
Die Einstellung „Beim Öffnen sofort zuhören“ bleibt optional. Nach dem Öffnen
auf die aktive Aufnahme warten und den eigentlichen Befehl getrennt sprechen.
Der Name allein registriert keine Google-Home-Automatisierung und ermöglicht
keine Übergabe bereits von Gemini erkannter Sätze.

VersionCode 5. Paketname und fester Signierschlüssel bleiben gleich; dadurch
ist ein Update der bisherigen App vorgesehen. Einstellungen, Verlauf, Icon,
manuelle Aufnahme und Netzwerkfunktionen werden nicht verändert.


## Version 1.0.5 – öffnen, zuhören, zurück

Automatisches Zuhören wird bei diesem Update einmalig eingeschaltet. Danach
bleibt eine Änderung des Schalters durch den Nutzer gespeichert. Der neue
Schalter „Nach automatischer Aufnahme in den Hintergrund“ ist ebenfalls an.
Die Fibaro-/MQTT-Konfiguration und der Verlauf bleiben unverändert.

1. „OK Google, öffne Hauspilot“. Berechtigungen gegebenenfalls zuerst erteilen.
2. Auf „Bitte jetzt sprechen …“ warten, dann den eigentlichen Befehl sagen.
3. Hauspilot erkennt genau einen Satz, speichert ihn und sendet ihn entsprechend
   dem Zielmodus. Im Testmodus wird ausschließlich gespeichert.
4. Nach dem Abschluss oder einem Erkennungsfehler geht die App in den Hintergrund.
5. Zum Nachsehen über Androids **letzte Apps** zurückkehren: keine neue Aufnahme.
   Während einer Aufnahme öffnet „Nur Verlauf ansehen“ die Ansicht und bricht
   die Aufnahme ab. Ein bereits laufender Versand wird nicht abgebrochen.
6. Eine Aufnahme über den manuellen Mikrofon-Button lässt die Ansicht geöffnet.

Ein neuer Aufruf über das App-Icon kann wie der Google-Aufruf eine neue Aufnahme
starten; beide werden vom System häufig auf demselben Weg zugestellt. Zum reinen
Nachsehen die letzten Apps oder die laufende Aufnahme-Benachrichtigung verwenden.

Google-Startaufrufe benötigen nicht mehr zwingend CATEGORY_LAUNCHER. Rückkehr
aus den Einstellungen, Benachrichtigungsöffnung, Bildschirmdrehung und doppelte
Aufrufe während einer Aufnahme lösen keine zusätzliche Aufnahme aus. Die App
bleibt in der Übersicht der letzten Apps sichtbar; kein versteckter Dauerbetrieb.
Kein Abfangen oder Weiterreichen des bereits bei Gemini gesprochenen Textes.
VersionCode 6; Paketname, Icon und fester Signierschlüssel unverändert.


## Version 1.0.6 – Übersicht und Spracheinstieg getrennt

Eine APK, zwei Android-Starteinträge:

- **Hauspilot**: öffnet die Übersicht und bleibt sichtbar. Nur der Mikrofon-Button
  startet hier eine Aufnahme. Auch beim Öffnen während einer Hintergrundaufnahme
  bleiben Status und Verlauf sichtbar; nach deren Abschluss wird die Übersicht
  nicht geschlossen.
- **Hauspilot Sprache**: öffnet kurz ein kleines Startfenster. Es fordert bei Bedarf
  Berechtigungen an, startet den Mikrofon-Foreground-Service und verschwindet,
  sobald der Sprachdienst bereit ist. Der Service erkennt einen Satz, speichert
  und sendet ihn und beendet sich. Der eigene Aufnahme-Task wird aus den letzten
  Apps entfernt. Die Übersicht bleibt davon unabhängig.

Google-Aufruf zum Testen: **„OK Google, öffne Hauspilot Sprache“**. Die Auflösung
mehrerer Starteinträge desselben Pakets durch Google/Gemini ist geräteabhängig
und wurde hier nicht getestet. Falls Google den zweiten Namen nicht findet,
zunächst das Symbol „Hauspilot Sprache“ direkt antippen, um den Aufnahmeweg
unabhängig von Googles Namenssuche zu prüfen. Keine Behauptung einer garantierten
Google-Integration; kein Abfangen des vorher bei Google gesprochenen Satzes.

In den Einstellungen: „Spracheinstieg aktivieren“ und „Kurze Textrückmeldung bei
Sprachaufruf“. Beide sind zunächst an. Die Rückmeldung zeigt „Bitte jetzt sprechen“
und anschließend den Versand-/Fehlerstatus als kurze Textmeldung. Hauspilot
verwendet keine TTS-Sprachausgabe. Töne des gewählten Sprachdiensts werden nicht
unterdrückt; es werden keine globalen Lautstärken oder Stummschalter verändert.
Androids Mikrofonanzeige und Aufnahme-Benachrichtigung bleiben während der
Aufnahme sichtbar. Nach dem Ende wird die Service-Benachrichtigung entfernt.

Zum ersten Test beide Einträge einmal bei entsperrtem Bildschirm öffnen und
gegebenenfalls Mikrofon/Benachrichtigungen erlauben. Fibaro-/MQTT-Zugangsdaten,
Verlauf, manuelle Aufnahme, Paketname, Haus-Icon und fester Schlüssel bleiben
erhalten. VersionCode 7 / 1.0.6. Die früheren Autostart-/AutoHide-Schalter werden
nicht mehr verwendet: Die Übersicht startet nun grundsätzlich manuell.


## Version 1.0.7 – nur Jarvis

Ein einziger App-Name und ein Launcher-Eintrag: **Jarvis**.
Testaufruf: **„OK Google, öffne Jarvis“**. Der Aufruf startet eine einzelne
Hintergrundaufnahme. Das kleine Startfenster verschwindet, sobald der
Sprachdienst bereit ist; Erkennung und Versand laufen bis zum Abschluss.
Kein Zusatz „Sprache“, keine zweite APK und kein zweiter Launcher-Eintrag.

Zum manuellen Nachsehen das Jarvis-Symbol lange drücken und die Verknüpfung
„Verlauf öffnen“ bzw. „Verlauf und Einstellungen öffnen“ auswählen. Diese öffnet
die bestehende Übersicht mit Einstellungen und Mikrofon-Button und bleibt offen,
auch wenn eine Hintergrundaufnahme endet. Während einer Aufnahme öffnet auch
deren Benachrichtigung die Übersicht.

Ein normales Antippen des Jarvis-Symbols löst ebenso wie der Google-App-Aufruf
die Einzelaufnahme aus. Die App behauptet nicht, diese identischen Startaufrufe
automatisch anhand ihres Ursprungs unterscheiden zu können. Für die manuelle
Übersicht wird deshalb die Funktion im selben Symbolmenü verwendet. Der Google-
Aufruf und die Darstellung der Verknüpfung müssen auf dem Samsung getestet werden.

VersionCode 8 / 1.0.7. Paketname und fester Signierschlüssel bleiben unverändert.
Einstellungen und Verlauf bleiben beim Update erhalten.


## Version 1.0.8 – eindeutige Ergebnisrückmeldung und optionales Vorlesen

Nach jeder abgeschlossenen Aufnahme erscheint auf Wunsch eine kurze
Standard-Android-Texteinblendung – bei Hintergrund- und manuellen Aufnahmen:

- Bestätigter HTTP-/MQTT-Versand: **Befehl gesendet**.
- Ablehnung (z. B. HTTP 401) oder kein erfasster Text: **Befehl nicht gesendet**.
- Timeout/MQTT-Verbindungsabbruch/unklarer Übertragungsfehler: **Sendestatus unklar**.
- Testmodus: **Nur lokal gespeichert**.
- Bewusster Abbruch: **Aufnahme abgebrochen**.

Die ausführlichen Statusdetails bleiben erhalten. Eine Speicherung des
Verlaufsstatus kann nach bestätigtem Versand fehlschlagen; daraus wird keine
Behauptung eines fehlgeschlagenen Versands. Die Rückmeldung wird aus dem
tatsächlichen Ablauf abgeleitet, nicht anhand von Wörtern im Statustext.

Die Texteinblendung wird für dieses Update einmalig eingeschaltet; danach
werden eigene Änderungen gespeichert. In den Einstellungen lässt sich zusätzlich
**Ergebnis vorlesen** aktivieren (standardmäßig aus). Jarvis spricht nur das
kurze Ergebnis auf Deutsch, zum Beispiel „Befehl gesendet“. Es wird nichts
während der Aufnahme vorgelesen. Ein neuer Aufnahmebeginn stoppt eine noch
laufende Ergebnisansage. TTS benötigt einen verfügbaren deutschen Sprachdienst
und hörbare Ausgabelautstärke. Falls TTS fehlschlägt, gibt es eine Textmeldung.
Die TTS-Engine wird nach Abschluss/Fehler bzw. spätestens nach 15 Sekunden
freigegeben. Sie ist unabhängig vom bereits beendeten Aufnahmedienst verwaltet.

Manuelle Einstellungen: Jarvis-Symbol lange drücken → Verlauf und Einstellungen
öffnen → Zahnrad → Ergebnis vorlesen → Einstellungen speichern.
Die Google-Startphrase „OK Google, öffne Jarvis“, der einzige Starteintrag und
der feste Schlüssel bleiben unverändert. VersionCode 9 / 1.0.8.

Gerätetest: real senden (Erfolg), mit absichtlich falschen Zugangsdaten eine
Ablehnung prüfen und den Testmodus prüfen. Anschließend die echten Zugangsdaten
wiederherstellen. Jede Meldung auch mit aktivierter TTS-Einstellung testen;
bei einem Timeout nicht automatisch erneut senden, sondern zuerst das Ziel
prüfen. Keine Wiederholung von Netzwerkbefehlen durch diese App.


## Version 1.0.9 – erst den Satz, dann das Ergebnis

Anzeige und optionale Sprachausgabe nennen zuerst den unveränderten erkannten
Satz und danach das Ergebnis. Beispiel: „Wohnzimmer Rollo 1 öffnen. Erfolgreich
gesendet.“ Fehler, unklarer Versand und Testmodus nennen ebenfalls zuerst den
Satz. Es werden keine Wörter des Befehls interpretiert oder verändert und keine
Netzwerkbefehle erneut ausgelöst. Der an Fibaro/MQTT gesendete Rohtext bleibt
unverändert; der Zusatz wird nur für die Rückmeldung verwendet.

Die kurze Texteinblendung kann durch Android auf zwei Zeilen begrenzt werden.
Die Statusansicht enthält den vollständigen Satz, das Ergebnis und technische
Details. Im gespeicherten Verlauf steht der Satz weiterhin separat neben seinem
Versandstatus. Bei nicht erkannter Sprache wird kein Befehl erfunden.

Die TTS-Frist berücksichtigt nun die Textlänge (15 bis maximal 120 Sekunden),
damit längere erkannte Sätze vor der Ergebnisansage nicht vorzeitig abgeschnitten
werden. Ein neuer Aufnahmebeginn beendet weiterhin eine ältere Ansage.

Download-Prüfung: Die bisherige APK-Datei ist beim Speichern mit
application/vnd.android.package-archive gekennzeichnet; sie lässt sich laut
Gerhard über Eigene Dateien installieren. Der Abbruch beim direkten Öffnen aus
dem Chat wurde hier nicht reproduziert und ist nicht als behoben bestätigt.
Ein alternativer Dateizugang zur neuen APK dient zum Vergleich. Andere, beim
Nutzer direkt installierbare Projekt-APKs lagen für einen Vergleich nicht vor.

VersionCode 10 / 1.0.9, gleicher Paketname und Signierschlüssel.


## Version 1.1.0 – lokaler und öffentlicher Fibaro-Zugang

Die vorhandenen Felder fibaroUrl, fibaroUser und fibaroPassword bleiben das
lokale Profil. Alte gespeicherte Werte werden weder überschrieben noch als
öffentliche Zugangsdaten kopiert. Ohne bisher gespeicherte lokale Adresse ist
http://192.168.111.111:80 vorbelegt. Das neue öffentliche Profil hat eigene
Schlüssel publicFibaroUrl, publicFibaroUser und publicFibaroPassword. Adresse:
http://w2ntn84uuipl7xnk.myfritz.net:48884. Benutzername Agnita69 ist anhand der
vorgegebenen Adresse vorbelegt, das Passwort bleibt leer und muss am Handy
eingetragen werden. Beide Profile benutzen dieselbe globale Variable und den
invokeScenes-Schalter. Die vorhandene Variable wird unverändert übernommen.

Verbindungsauswahl in den Einstellungen: Automatisch, Lokal, Öffentlich. Die
App-Einstellungen beginnen ohne früher gespeicherte Auswahl mit Automatisch.

- Lokal: genau ein PUT an die lokale Adresse mit lokalen Zugangsdaten.
- Öffentlich: genau ein PUT an die öffentliche Adresse mit deren Zugangsdaten.
- Automatisch: zunächst ein GET der lokalen Variable, maximal 1,5 Sekunden.
  Eine passende JSON-Variable bestätigt die lokale API. Antwortet der lokale
  Server mit 401/403, bleibt die Auswahl lokal und zeigt einen Zugangsdaten-/
  Rechtefehler. Sonst wird bei fehlender Erreichbarkeit oder unpassendem Inhalt
  das öffentliche Profil gewählt, bevor irgendein PUT gesendet wird. Nach einem
  PUT-Fehler gibt es keinen zweiten PUT über den anderen Eingang.

Nicht jede WLAN-Verbindung wird pauschal als Heimnetz angenommen. Es werden
keine SSID- oder Standortberechtigungen benötigt; entscheidend ist der lesende
API-Test. Zwischen Probe und Versand kann die Verbindung wechseln; dann wird
ein Fehler gemeldet und kein automatischer zweiter Versand versucht.

„Verbindung prüfen · nur Lesen“ arbeitet mit den aktuellen Eingaben, auch vor
dem Speichern. Der Button liest die Variable, zeigt den ausgewählten Eingang
und unterscheidet passende API-Antworten, Anmeldungsfehler, Weiterleitungen und
Webseiten. Es wird kein Variablenwert verändert. Der Ergebnistext meldet bei
erfolgreichem Versand ebenfalls Lokal oder Öffentlich. HTTP-200-HTML gilt nicht
als erfolgreicher Fibaro-Versand. Weiterleitungen und TLS-Zertifikatsprüfungen
werden nicht umgangen.

Gerhards bestätigter Aufbau ist MyFRITZ-Adresse:48884 → Portweiterleitung der
FRITZ!Box → Fibaro HC2. Dafür braucht die App Fibaro-Zugangsdaten und keine
FRITZ!Box-Webanmeldung. MyFRITZ ist hier die Adresse, kein zusätzlicher
Anmeldeschritt. Zeigt der Test eine FRITZ!Box-Webseite, muss die Freigabe auf
die HC2-API geprüft werden. HTTP überträgt die Basic-Auth-Zugangsdaten
unverschlüsselt; HTTPS oder ein VPN ist für geschützten Fernzugriff erforderlich.

Externer Test aus dieser Arbeitsumgebung: je ein unauthentifizierter GET per
HTTP und HTTPS an Port 48884. Beide endeten mit HTTP 503 des vorgelagerten
Gateways und einem Verbindungsfehler, nicht mit einer bestätigten HC2-Antwort.
Es wurden keine Anmeldedaten ausprobiert und keine Werte geschrieben. Daraus
folgt nicht, dass Gerhards Handy die Weiterleitung nicht erreichen kann. Der
Verbindungsbutton am Handy ist der nächste reale Test.

VersionCode 11 / 1.1.0. Fester Signierschlüssel, Paketname, Eintrag „Jarvis“,
manuelle Übersicht, Verlauf und Ergebnisansagen bleiben erhalten. MQTT und
Testmodus verwenden keine Fibaro-Verbindungsprüfung.


## Version 1.1.1 – mehr Zeit zum Sprechen

Drei neue gespeicherte Einstellungen, gültig für manuellen und Hintergrundstart:

- Wartezeit zum Beginnen: 5–60 Sekunden, Standard 20. Sie beginnt beim ersten
  Bereitschafts-Callback des Sprachdiensts. Frühe stille Erkennungsenden starten
  innerhalb dieser Frist einen neuen nativen Hörversuch, ohne die Frist zurückzusetzen.
- Zusätzliche Pause bis zum Senden: 1–15 Sekunden, Standard 5. Nach einem
  bestätigten Erkennungsergebnis hört Jarvis auf eine Fortsetzung. Beginnt neue
  Sprache, wird die alte Pausenfrist aufgehoben. Nach dem nächsten Ergebnis
  beginnt eine neue Frist. Die native Erkennungslatenz kommt zu dieser Zusatzpause
  hinzu; es wird keine sekundengenaue Pause ab dem letzten gesprochenen Laut behauptet.
- Maximale Aufnahmezeit: 15–180 Sekunden, Standard 90, mindestens so lang wie
  die Anfangswartezeit. Der gesamte Auftrag bleibt dadurch zeitlich begrenzt.

Mehrere native Endergebnisse werden in Reihenfolge mit einem Leerzeichen
verbunden und genau einmal gespeichert/gesendet. Ein einzelnes Ergebnis bleibt
unverändert. Da eine native Erkennung bei Sprechpausen schon früh enden kann,
setzt Jarvis den Hörversuch innerhalb derselben begrenzten Sitzung neu auf. Es
kann dabei kurze Aufnahmelücken, anbieterabhängige Signaltöne oder einen
Sprachdienst-beschäftigt-Fehler geben. Diese neue Audiosequenz muss auf dem
Samsung getestet werden; sie ist keine garantierte unterbrechungsfreie Aufnahme.
Die Android-Dokumentation weist darauf hin, dass die Silence-Length-Extras von
RecognizerIntent je nach Anbieter wirkungslos sein können. Deshalb stützt sich
die App auf eigene Fristen und nicht allein auf solche Intent-Zusatzwerte.

Die Übersicht und die Aufnahme-Benachrichtigung zeigen die verbleibende Zeit.
„Jetzt senden“ beendet die Pause sofort oder fordert beim laufenden Sprechen
das Endergebnis an (bis zu 8 Sekunden zum Abschluss). Abbruch ist weiter
verfügbar. Bei unverständlicher Fortsetzung oder Zeitlimit während noch
unbestätigter Sprache wird kein Teilbefehl gesendet. Bereits bestätigte Teile
werden in diesem Fall als ungesendet im Verlauf gespeichert. Alte Callbacks
zerstörter Erkenner werden per Generation verworfen. Keine zweite Netzwerksendung.

Jarvis erzeugt keine eigene gesprochene Öffnungsansage. Googles oder Geminis
„Ich öffne Jarvis“ wird von dieser App nicht stummgeschaltet; es werden keine
globalen Lautstärken verändert. Für klassischen Google Assistant dokumentiert
Google eine Sprachausgabe-Einstellung „Keine“; eine gleichwertige aktuelle
Gemini-Einstellung konnte nicht sicher bestätigt werden. Die Ergebnisansage
in Jarvis bleibt wie bisher separat einstellbar.

Quellen zur Einordnung: Android RecognizerIntent API und Google Assistant
Sprachausgabe-Hilfe. Diese belegen API-/Einstellungshinweise, keine Prüfung
des Verhaltens auf Gerhards Samsung.
https://developer.android.com/reference/android/speech/RecognizerIntent
https://support.google.com/assistant/answer/7544506

VersionCode 12 / 1.1.1. Paketname, Schlüssel, Jarvis-Start, lokale/öffentliche
Profile und Port 48884 bleiben unverändert.


## Version 1.1.2 – kurze Zeiten zur Feinjustierung

Anfangswartezeit 0–60 s, zusätzliche Pause 0–15 s, maximale Aufnahmezeit
2–180 s und mindestens so lang wie die Anfangswartezeit. Neue Standardwerte
1 / 1 / 10 Sekunden entsprechend dem gezeigten Handy-Setup. Bereits
gespeicherte Werte bleiben erhalten und können geändert werden.
Anfangswartezeit 0 deaktiviert nur die separate Anfangsfrist; das absolute
Aufnahmelimit bleibt aktiv. Pause 0 sendet direkt nach einem bestätigten
Erkennungsergebnis, ohne zusätzliche Pause oder notwendigen Knopfdruck.
Die Zeit des nativen Sprachdienstes bis zum Ergebnis bleibt davon unabhängig.
VersionCode 13; Paket, Signatur und Netzwerkprofile unverändert.


## Version 1.1.3 – kurze Sprachausgabe

Neuer Schalter „Befehl mit vorlesen“ in den Einstellungen, standardmäßig aus.
Bei aktiviertem „Ergebnis vorlesen“ spricht Jarvis ohne diesen Schalter nur
„Erfolgreich gesendet“ oder die Fehlermeldung. Mit dem Schalter bleibt die
bisherige Ausgabe Befehl + Ergebnis verfügbar. Testmodus und Abbruch werden
weiterhin korrekt benannt; kein falscher Sendungserfolg. Textanzeige, Verlauf,
Netzwerk-Payload und Zeitwerte bleiben unverändert. SharedPreferences speichern
den Schalter dauerhaft. VersionCode 14, unverändertes Update-Zertifikat.


## Version 1.1.4 – Übersicht bei ausgeschaltetem Spracheinstieg

Bei deaktiviertem Spracheinstieg öffnet das normale App-Symbol jetzt direkt
die Übersicht mit Verlauf, Einstellungen und manuellem Mikrofon. Die frühere
abgeschnittene Toast-Meldung mit anschließender Rückkehr zur vorherigen App
entfällt in diesem Fall. Kein automatischer Mikrofonstart auf diesem Weg.
Die Übersicht wird explizit als eigene Task geöffnet; erst dann wird die
kurzlebige Spracheinstiegs-Task entfernt. Bei aktiviertem Spracheinstieg bleibt
der bisherige Aufnahmeablauf bestehen. Paket, Signatur und Einstellungen
bleiben erhalten. VersionCode 15.


## Version 1.1.5 – separate Verbindungsprüfungen

Je ein Prüfbutton im lokalen und öffentlichen Profil. Die Prüfung verwendet
ausschließlich das angeklickte Profil mit den aktuellen Eingabewerten,
unabhängig von der gewählten Automatik. Ergebnisse stehen direkt beim Profil.
Beide Prüfbuttons sind während einer laufenden Prüfung gesperrt. Nur GET, keine
Änderung der Variable, keine Änderung des gespeicherten Versandmodus.
Öffentlicher Port muss ausdrücklich in der URL stehen, z. B. :48884.
VersionCode 16; Paket und Signatur bleiben unverändert.


## Version 1.1.6 – konkrete Fehler bei der Verbindungsprüfung

Lokaler und öffentlicher Check zeigen nun die tatsächlich geprüfte GET-Adresse
und Dauer. Bei Ausnahmen werden DNS, Timeout, TLS, TCP, fehlende Route,
verweigerter Netzwerkzugriff und vorzeitig geschlossene Verbindung unterschieden.
Fehlertypen und bekannte Netzwerkcodes werden angezeigt, keine rohen
Ausnahmetexte oder Zugangsdaten. Validierungsfehler erscheinen beim jeweiligen
Prüfbutton. Der Versand und die gespeicherten Zugangsdaten bleiben unverändert.
VersionCode 17; Paket und Signatur unverändert.


## Version 1.1.7 – vollständigen Browser-Link übernehmen

Neues optionales maskiertes Eingabefeld und „Kompletten Link übernehmen“.
Links mit Benutzername:Passwort@Host:Port und optionalem
/api/globalVariables/Variablenname werden einmal URL-dekodiert. Die Basis-URL
wird ohne Zugangsdaten und API-Pfad eingetragen; Benutzername/Passwort/Variable
kommen in ihre vorhandenen Felder. Das Link-Eingabefeld wird danach geleert.
Auch Prüfen/Speichern übernimmt einen noch eingefügten Link vor der Validierung.
Eingaben prüfen und anschließend speichern; Übernehmen allein speichert nicht.
Der Netzwerkaufruf verwendet wie bisher HTTP Basic Auth, keine Browsercookies.
Query/Fragment und fremde Pfade werden abgelehnt. Keine echten Zugangsdaten
im Projekt oder in Diagnoseausgaben. VersionCode 18, gleicher Schlüssel.

Ein optionales /invoke am importierten Link wird als invokeScenes-Schalter
übernommen, nicht als zusätzliche REST-Route gesendet. Der funktionierende
PUT bleibt /api/globalVariables/Name mit invokeScenes:true im JSON.


## Version 1.1.8 – IPv4 bei HTTP bevorzugen und Verbindungsziel anzeigen

Die HTTP-DNS-Auflösung ordnet IPv4-Adressen vor IPv6-Adressen, ohne andere
Adressen zu entfernen. Ohne IPv4 bleibt IPv6 nutzbar. Grund: MyFRITZ-Hostnamen
können beide Adresstypen liefern, während eine IPv4-Portweiterleitung nicht
zwangsläufig auch auf IPv6 existiert. Es werden keine IP-Adressen fest kodiert.
Verbindungsprüfungen zeigen nun zusätzlich die tatsächlichen TCP-Zieladressen
mit Adressfamilie und Port, auch bei Verbindungsfehlern. Die Vermutung IPv6
als Ursache des gemeldeten Handy-Fehlers muss auf dem Gerät bestätigt werden.
Automatische HTTP-Wiederholungen bleiben aus; kein erneuter Schaltbefehl nach
möglichem Versand. Authentifizierung und API-Pfad unverändert. VersionCode 19.


## Version 1.1.9 – ein öffentliches Adressfeld

Die zusätzliche Link-Eingabe und ihr Übernehmen-Knopf entfallen. Im einzigen
öffentlichen Adressfeld kann wahlweise die Basis-URL oder ein vollständiger
Browser-Link stehen. Prüfen/Speichern übernimmt Benutzername, Passwort,
Variablenpfad und optionales /invoke in die vorhandenen Felder. Eine reine
Basis-URL ändert die Zugangsdaten nicht. API-Links ohne Zugangsdaten verwenden
die separat eingetragenen Zugangsdaten. Der Versand nutzt dasselbe Profil.
Kein zweiter Speicher und keine neuen Launcher-Einträge.
Neue Installationen erhalten die zuletzt bestätigte öffentliche Basis-URL
n514fueyqz08svn0.myfritz.net:48884 und keinen vorbelegten öffentlichen
Benutzernamen. Vorhandene gespeicherte Werte bleiben unverändert.
VersionCode 20; gleicher Paketname und Schlüssel.


## Version 1.1.10 – einheitlicher sichtbarer App-Start

MainActivity ist jetzt der einzige normale LAUNCHER-Eintrag, mit Label Jarvis.
Die bisherige VoiceCaptureActivity bleibt ohne Launcherfilter als Weiterleitung
für vorhandene Verknüpfungen erhalten; sie öffnet ebenfalls MainActivity.
Es gibt keinen normalen Startweg mehr, der nur ein kleines Startfenster zeigt
und die Oberfläche wieder schließt. Die Übersicht bleibt nach der Aufnahme
sichtbar. Bei aktiviertem „Beim Öffnen automatisch zuhören“ startet ein frischer
MAIN-Aufruf die Aufnahme; Verlaufseinstieg, Rotation/Restoration und Rückkehr
aus der Android-Historie starten keine neue Aufnahme. Ein laufender Vorgang
wird durch erneute App-Aufrufe nicht doppelt gestartet oder vorgemerkt.

Alte vom Launcher gespeicherte Startbildschirm-Symbole können außerhalb der
App bestehen bleiben. Diese gegebenenfalls nur vom Startbildschirm entfernen
und den aktuellen Jarvis-Eintrag aus der App-Liste neu hinzufügen. Die App
kann fremde Launcher-Verknüpfungen nicht automatisch identifizieren/löschen.
Kein zweiter Anzeigename in dieser Version. Paket/Signatur bleiben gleich.
VersionCode 21. Google/Geminis Öffnungsansage wird nicht stummgeschaltet.


Der endgültige Anzeigename für Version 1.1.10 ist Hauszentrale. Genau ein
regulärer Starteintrag mit diesem Namen. Der bisherige Name Jarvis ist kein
zweiter Launcher-Eintrag. Vorhandene Verknüpfungen werden weiterhin explizit
auf die volle Übersicht weitergeleitet. Sprachaufruf zum Testen: „OK Google,
öffne Hauszentrale“. Die Erkennung/Ansagen von Google/Gemini wurden nicht
auf einem Gerät getestet und können nicht garantiert werden.


## Version 1.1.11 – optionale Sprech-Verknüpfung

Ein normaler App-Starteintrag Hauszentrale bleibt erhalten. Zusätzlich gibt es
den statischen Shortcut „Jetzt sprechen“ im Langdruck-Menü, der die volle
Übersicht öffnet und eine Aufnahme anfordert, unabhängig vom Schalter für
automatisches Zuhören bei normalen Starts. Der vorhandene Shortcut „Verlauf
öffnen“ startet weiterhin keine Aufnahme. Über das Drei-Punkte-Menü →
„Sprech-Verknüpfung hinzufügen“ lässt sich beim Launcher das Anheften
anfordern; der Nutzer bestätigt es im Systemdialog. Kein automatisches
zweites App-Symbol. Anheften hängt vom verwendeten Launcher ab.

Das Zahnrad ist auch während laufender Aufnahme bedienbar. Es beendet eine
Aufnahme vor dem Öffnen der Einstellungen; ein bereits gestarteter Versand
wird nicht wiederholt/neu gestartet. Automatisches Zuhören wird nach Rückkehr
aus den Einstellungen nicht neu ausgelöst.

Für Launcher/Tasker mit explizitem Activity-Start:
Komponente de.parip69.homesprachsteuerung/.MainActivity, Action
de.parip69.homesprachsteuerung.START_CAPTURE. Keine allgemeine Hardwaretaste
wird abgefangen; deren Belegung ist Aufgabe des jeweiligen Launchers/Tools.
Sichtbare Aktivität startet den Mikrofon-Foreground-Service, keine versteckte
Daueraufnahme. Neue VersionCode 22; Paket und Schlüssel unverändert.


## Version 1.1.12 – Startbildschirm-Widget

Neues resizables Hauszentrale-Widget, zunächst 3 × 2 Zellen, mit Original-Logo,
Button „Aufnahme starten“ und Zahnrad. Start öffnet MainActivity mit expliziter
START_CAPTURE-Action und beginnt nach Berechtigungsprüfung die Aufnahme.
Logo/Titel öffnen die passive Übersicht; Zahnrad öffnet über MainActivity die
Einstellungen und beendet eine laufende Aufnahme. Keine Aufnahme vom Widget-
Provider/onUpdate und keine periodischen Updates (updatePeriodMillis=0).
Alle PendingIntents sind explizit und immutable. Keine Zugangsdaten im Widget.
Die genaue Zellgröße und das Anheften hängen vom Launcher ab.

Menü → „Widget hinzufügen“ fragt das Anheften über AppWidgetManager an.
Alternativ: Startbildschirm lange drücken → Widgets → Hauszentrale.
Der zusätzliche Sprech-Shortcut samt Anheft-Menü aus 1.1.11 entfällt; der
vorhandene Verlauf-Shortcut bleibt. Alte explizite START_CAPTURE-Links bleiben
kompatibel, werden aber nicht mehr neu als Sprech-Shortcut angeboten.
Ein App-Launcher-Eintrag Hauszentrale bleibt. VersionCode 23, gleicher Schlüssel.


## Version 1.1.13 – App-Name Alfonso

Anzeigename in App-Liste, Widget und Bedienhinweisen auf Alfonso geändert. Sprachaufruf: „OK Google, öffne Alfonso“. Paketkennung und Signierschlüssel bleiben erhalten. Google/Gemini-Namenszuordnung muss auf dem Handy geprüft werden. VersionCode 24.

Widget 1.1.13: 1 × 1 Zelle, transparenter Außenbereich und dezenter runder 56-dp-Aufnahmebutton mit Haus-/Mikrofonsymbol. Hintergrund ca. 8 % deckend. Keine Titelzeile, kein Zahnrad und kein Text im Widget. Einstellungen bleiben in der vollständigen App erreichbar.
