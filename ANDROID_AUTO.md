# Alfonso – Android-Auto-Prototyp und Gerätediagnose

**Aktueller Nachweis vom 04.11.2026:** Die unveränderte Alfonso 18 (29)
erscheint im offiziellen DHU im Launcher und öffnet ihre Startseite. Android
Auto bindet ihren CarAppService erfolgreich. Nach Ende des DHU-Tests und einem
frischen normalen Host-Start fehlt Alfonso wieder unter „Launcher anpassen“;
`CAR.VALIDATOR` protokolliert `Package DENIED; failed all other checks`.
Die Host-Vertrauens-/Zulassungsprüfung ist damit als Ausblendungsgrund belegt.
Die genaue interne Teilprüfung nennt die Meldung nicht. Einzelheiten und
verbleibende Voraussetzungen stehen im Diagnoseabschnitt am Ende.

Die folgenden Prototyp-Angaben und die ursprüngliche Testtabelle dokumentieren
den früheren Stand 1.1.14-auto-test. Der aktuelle DHU-Nachweis umfasst weder
Spracheingabe/Befehlsversand noch einen Fahrzeugtest.

## Ergebnis und Grenze

Die offizielle IoT-Kategorie ist für Haussteuerung geeignet. Diese Erweiterung ergänzt die bestehende Smartphone-App um CarAppService und Session, eine PaneTemplate-Startseite mit Mikrofonaktion sowie eine SearchTemplate-Eingabe. Der Host zeichnet die Oberfläche. Es gibt keine zweite App und keinen eigenen Zeichenbereich.

**Dies ist ein vorbereiteter DHU-Prototyp, kein nachgewiesen funktionierender Ein-Tipp-Sprachablauf.** Die Mikrofonaktion öffnet die offizielle Auto-Eingabe. Ob dort ein Mikrofon angeboten wird, ob dessen Erkennung direkt ins Eingabefeld schreibt und ob ein fertiger Satz automatisch als Submit ankommt, hängt vom Host ab und ist hier nicht getestet. Gegebenenfalls muss „Befehl senden“ zusätzlich gedrückt werden. Die App ruft weder Gemini für beliebige Hausaktionen auf noch legt sie eine globale Assistant-Automatisierung an.

Die vorhandene Smartphone-Spracherkennung wird nicht aus dem Auto heraus gestartet: SpeechRecognizer öffnet sonst sein eigenes Mikrofon. CarAudioRecord liefert Fahrzeug-Audiodaten, aber keinen Text. Eine direkte PCM-Übergabe über RecognizerIntent.EXTRA_AUDIO_SOURCE ist erst ab Android 13 verfügbar und wird nicht von jedem Erkennungsdienst unterstützt; bei Nichtunterstützung kann der Dienst laut Dokumentation auf sein eigenes Mikrofon zurückfallen. Deshalb ist dieser unbestätigte Pfad nicht eingebaut. Es wird damit **keine generelle Unmöglichkeit** einer CarAudioRecord-/SpeechRecognizer-Integration behauptet. Ein zuverlässiger direkter Ein-Tipp-Pfad erfordert einen konkret geprüften Erkennungsdienst oder einen weiteren Speech-to-Text-Baustein.

Die vorhandenen Sprechpausen und Zeitlimits gelten am Smartphone weiter. Für die Host-Eingabe steuert der Host Aufnahme, Abbruch, Freigabe und Sprechpausen. Diese Parameter sind aus diesem Prototyp nicht steuerbar. Für eigene Fahrzeug-Audioaufnahme wäre RECORD_AUDIO einschließlich Laufzeitfreigabe über CarContext.requestPermissions nötig. Die Erweiterung nimmt selbst kein Audio auf und benötigt daher keine zusätzliche Mikrofonfreigabe; vorhandene Handy-Berechtigungen bleiben erhalten.

## Ausgangsstand und tatsächlich vorhandene Funktionen

- Lokaler Ausgangscommit: `5de7eec6594e64ef83d65dc10a1981ea2937f1b7`, Alfonso 1.1.13. Git-Status vor Änderungen sauber. Separater Worktree/Zweig `feature/android-auto-iot`; `main` bleibt unverändert. Noch kein GitHub-Remote eingerichtet und kein Upload durchgeführt.
- Ein Android-App-Modul `app`, Kotlin 2.1.20, Android Gradle Plugin 8.9.2, Gradle 8.11.1, Java 17.
- compileSdk/targetSdk 35, minSdk 26. Application-ID `de.parip69.homesprachsteuerung`; fester Projektschlüssel für Debug und Release. Testversion: versionCode 25.
- CaptureService nutzt Android SpeechRecognizer mit begrenzten Aufnahmeabschnitten und SpeechTurn zur Zeit-/Pausensteuerung. Manueller Mikrofonstart und automatischer Einstieg sind vorhanden; Widget vorhanden, statischer Shortcut öffnet den Verlauf. Kein permanentes Aktivierungswort.
- NetworkSender implementiert Fibaro-PUT mit optionalem invokeScenes sowie MQTT 3.1.1, QoS 1, nicht retained. Fibaro: lokal/öffentlich/automatisch, IPv4 bevorzugt, GET-Proben vor Auswahl, kein automatischer PUT-Wiederholungsversuch. SettingsStore und SQLite-Verlauf vorhanden.
- Bisherige Berechtigungen: RECORD_AUDIO, INTERNET, FOREGROUND_SERVICE, FOREGROUND_SERVICE_MICROPHONE, POST_NOTIFICATIONS. Keine zusätzlichen Berechtigungen ergänzt.

## Geänderte und neue Dateien

| Datei | Zweck |
|---|---|
| `app/build.gradle.kts` | Testversion, offizielle Car-App-Abhängigkeiten `androidx.car.app:app:1.7.0` und `app-projected:1.7.0` |
| `app/src/main/AndroidManifest.xml` | CarAppService mit IOT-Kategorie, Template-Metadaten, Car API mindestens 6 |
| `app/src/main/res/xml/automotive_app_desc.xml` | Offizielle `template`-Fähigkeit |
| `app/src/main/java/de/parip69/homesprachsteuerung/BridgeApplication.kt` | Lazily erzeugter Dispatcher für Auto-Befehle |
| `app/src/main/java/de/parip69/homesprachsteuerung/car/AlfonsoCarAppService.kt` | Service und Session; im Debug-Build offizielle Entwicklungs-Hostvalidierung, Release mit Signaturliste |
| `app/src/main/java/de/parip69/homesprachsteuerung/car/AlfonsoCarScreen.kt` | Startseite und Host-Eingabe; keine Aufnahme beim Öffnen, kein Versand bei Textzwischenständen |
| `app/src/main/java/de/parip69/homesprachsteuerung/car/CarCommandDispatcher.kt` | Vorhandene Einstellungen lesen, Verlauf und NetworkSender verwenden, kurze Statusmeldung |
| `app/src/main/java/de/parip69/homesprachsteuerung/car/CarSubmissionGate.kt` | Eine Sendungsabsicht pro Eingabesitzung, blockiert parallele/doppelte Callbacks auch nach Fehlern |
| `app/src/test/java/de/parip69/homesprachsteuerung/car/CarSubmissionGateTest.kt` | Tests der Duplikat-/Parallelitätsgrenze und Eingabegrenzen |
| `README.md`, `ANDROID_AUTO.md` | Einstieg, Einschränkungen und Testbericht |

Ein fertiges Host-Submit oder der ausdrückliche Klick auf die Ergebniszeile übergibt den unveränderten Text an den vorhandenen NetworkSender. Keine heimliche Protokollumschaltung; keine Änderungen an Profilen, Passwort, Topic oder Fibaro-Variable. Der Auto-Dispatcher schreibt keine Einstellungen. Der Versand überlebt das Schließen der Auto-Ansicht, damit der Verlaufsstatus noch abgeschlossen werden kann. Ein fehlgeschlagener/unklarer Versand wird nicht automatisch erneut versucht. MQTT QoS 1 garantiert keine Ende-zu-Ende-Einmaligkeit; die neue Sperre verhindert doppelte App-Aufrufe derselben Eingabesitzung.

## Tatsächlich ausgeführte Tests

| Test | Ergebnis | Beleg / Grenze |
|---|---|---|
| A: Unveränderter Ausgangsbuild | **Bestanden** | assembleDebug + 77 bestehende JVM-Tests erfolgreich |
| A: Neue Smartphone-/Auto-APK | **Bestanden** | assembleDebug erfolgreich; vorhandene Smartphone-Komponenten enthalten |
| A: Bestandsschutz | **Bestanden** | 62 ursprüngliche Dateien bytegleich; insbesondere CaptureService, SettingsStore, NetworkSender, Widget, Schlüssel und Signing-Konfiguration |
| A: Smartphone-Laufzeittest / tatsächlicher Einstellungserhalt nach Installation | **Nicht durchgeführt** | Kein Smartphone angeschlossen; UI und installierte Benutzerdaten nicht getestet |
| B: Service, IOT-Kategorie, Template-Metadaten, Car API 6 | **Bestanden** | Kompiliertes APK-Manifest und zusammengeführtes Manifest geprüft; genau ein regulärer Smartphone-Starteintrag |
| B: DHU-App-Liste und Öffnen der Oberfläche | **Nicht durchgeführt** | Kein Smartphone/Android-Auto-Host angeschlossen |
| B: Installation und Öffnen im echten Fahrzeug | **Nicht durchgeführt** | Keine Fahrzeuganbindung; ohne vertrauenswürdige Quelle nicht zugesichert |
| C: Duplikat-/Parallelitätssperre | **Bestanden** | 5 neue JVM-Tests: leere/zu lange Eingaben, doppelte Callback-ID auch nach Abschluss, bewusste Wiederholung mit neuer ID, überlappende Vorgänge, 8 gleichzeitige Aufrufe mit genau einer Annahme |
| C: Gesamte lokale Testsuite | **Bestanden** | 82 Tests, 0 Fehler, 0 fehlgeschlagene Tests |
| C: Start nur per Aktion / keine partielle Übertragung | **Bestanden (Codeprüfung)** | Keine CaptureService-/SpeechRecognizer-Starts im car-Paket; Submit nur im expliziten Host-Submit bzw. Zeilenklick, Textänderung aktualisiert nur die Vorlage |
| C: Auto-Mikrofon, automatische Aufnahmebeendigung, Freigabe, Host-Diktat und Ein-Tipp-Ablauf | **Nicht durchgeführt** | Host übernimmt Erkennung/Lebenszyklus; weder PCM-Sprachpfad noch Smartphone-Zeitkonfiguration im Auto implementiert |
| D: Bestehende Fibaro-/MQTT-Schnittstelle | **Bestanden (lokale Tests und Codeprüfung)** | Vorhandene Transporttests mit Loopback-/Mockservern bestehen; Auto-Dispatcher verwendet unveränderten NetworkSender und Verlauf |
| D: End-to-End vom Host-Submit zum Zielgerät | **Nicht durchgeführt** | Keine produktive Verbindung getestet oder Hausaktion ausgelöst |
| D: Automatische Fehlertests | **Bestanden (vorhandene Transporttests)** | u. a. HTTP-Fehler, Redirects und Wiederholungsschutz; kein Geräte-End-to-End-Test der neuen Auto-Fehleranzeige |
| Signatur, Application-ID und Berechtigungen | **Bestanden** | apksigner verify erfolgreich, Zertifikat identisch zu 1.1.13, ID unverändert, versionCode 25; Berechtigungsmenge identisch zum Ausgangs-APK |
| Lint / APK-Datei | **Bestanden** | lintDebug ohne Error/Fatal; APK-ZIP intakt. Kompatible ältere PaneTemplate-Header-Methoden erzeugen lediglich Deprecation-Warnungen. |

Keine abschließende lokale Prüfung ist fehlgeschlagen. Die markierten Laufzeittests wurden nicht simuliert oder als bestanden ausgegeben.

Build-Befehle: `./gradlew assembleDebug testDebugUnitTest lintDebug`. Originaler APK-Ausgabepfad: `app/build/outputs/apk/debug/app-debug.apk` im Projekt. Bereitgestellte Datei: `/workspace/scratch/3e170866d40c/deliverables/Alfonso-1.1.14-AndroidAuto-debug.apk`.

APK SHA-256: `009eff861dafdfbb157bfffbbf37fb5575899429b31110d792f1818ecad9f1a0`.

Die APK bleibt für das Smartphone gebaut; Paketkennung, Signatur und bisherige Einstiegspunkte sind enthalten. Der tatsächliche Start auf einem Telefon wurde nicht geprüft. Es wurde nichts auf GitHub hochgeladen.

## Kleinster nächster Schritt: DHU mit dem Samsung-Handy

1. Debug-APK über die bestehende Alfonso-App installieren, nicht deinstallieren. Einstellungen vorher am Handy prüfen. Für einen Test ohne Hausaktion bewusst den bereits vorhandenen TEST-Modus wählen; anschließend bei Bedarf zurückstellen.
2. Android Auto am Handy aktualisieren. Auf dem PC im Android-SDK-Manager „Android Auto Desktop Head Unit Emulator“ sowie Platform-Tools installieren. Dafür ist kein Play-Entwicklerkonto nötig.
3. Android-Auto-Entwicklermodus aktivieren und im Entwicklermenü „Head-Unit-Server starten“ wählen. Handy per USB anschließen, USB-Debugging freigeben, Bildschirm entsperren.
4. `adb forward tcp:5277 tcp:5277` ausführen. Unter Windows im SDK-Verzeichnis `extras/google/auto` die Datei `desktop-head-unit.exe` starten.
5. In der DHU-App-Liste Alfonso suchen und öffnen. Fehlt Alfonso, ist die Priorität „DHU-Erkennung“ noch nicht erreicht; keine Fahrzeug-Kompatibilität daraus ableiten. Service-/Host-Logs prüfen.
6. „Spracheingabe“ wählen. Prüfen, ob der Host Spracheingabe anbietet und den gesprochenen Satz in die Alfonso-Eingabe zurückliefert. Wenn nicht, zuerst geparkt per Texteingabe den Versandpfad im TEST-Modus prüfen. Die Lenkrad-/DHU-Assistant-Taste ist kein Nachweis, dass Sprache direkt an Alfonso geht.
7. Genau einen Testbefehl eingeben/sprechen, abschicken und prüfen: eine Verlaufszeile, Testmodus-Rückmeldung, wieder bereit. Keine doppelten Befehle beim Doppelklick. Abbruch, erneutes Öffnen und Host-Trennung testen. Eingabe auch mit DHU-Fahrbeschränkungen prüfen; keine Umgehung der Host-Sperren.

**Echtes Fahrzeug:** Eine Installation ohne Play-Registrierung ist hier nicht nachgewiesen. Googles Dokumentation verlangt für reale Fahrzeuge eine vertrauenswürdige Installationsquelle; der Entwickler-Schalter „Unbekannte Quellen“ gilt ausdrücklich nicht für Car-App-Library-Apps. Ein DHU-Erfolg ist kein Fahrzeug-Nachweis. Ohne Entwicklerkonto bleiben wir zunächst beim offiziellen DHU-Test. Kein Fake-Medienservice, kein Installer-Spoofing und keine Änderung der Android-Auto-Vertrauensprüfung.

## Offizielle Grundlagen (geprüft am 04.11.2026)

- IoT-Kategorie: https://developer.android.com/training/cars/apps/iot
- Car App Library / stabile Version: https://developer.android.com/jetpack/androidx/releases/car-app
- Manifest und Auto-Unterstützung: https://developer.android.com/training/cars/apps/auto
- Service, Session, Hostprüfung: https://developer.android.com/codelabs/car-app-library-fundamentals
- Zulässige IoT-Templates: https://developers.google.com/cars/design/create-apps/apps-for-drivers/templates/overview
- SearchTemplate/Host-Eingabe: https://developers.google.com/cars/design/create-apps/apps-for-drivers/templates/search-template
- Fahrzeugmikrofon: https://developer.android.com/training/cars/apps/library/car-microphone
- Erkenner-Audioquelle: https://developer.android.com/reference/android/speech/RecognizerIntent#EXTRA_AUDIO_SOURCE
- DHU: https://developer.android.com/training/cars/testing/dhu
- Fahrzeug und vertrauenswürdige Installation: https://developer.android.com/training/cars/testing

## Gerätediagnose vom 04.11.2026

Dieser Abschnitt beschreibt den aktuellen Prüfstand; die älteren Angaben oben
beziehen sich auf den ursprünglichen Prototyp und dessen damalige Umgebung.

- Ausgangscommit `a359478`, `main` nach `git fetch origin` und
  `git merge --ff-only origin/main` aktuell. Bestehende lokale Änderungen an
  `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar` und
  `gradle/wrapper/gradle-wrapper.properties` bleiben erhalten.
- Aktueller Quellstand: Alfonso 19 / versionCode 30, Application-ID unverändert.
  Beide Build-Typen verwenden weiterhin `projectKey` / Alias `homevoice`.
  Die vorhandene `downloads/Alfonso-19.apk` besteht `apksigner verify`; ihr
  Zertifikat entspricht dem in AGENTS.md festgelegten SHA-256-Fingerabdruck.
  Die Update-Metadaten unter `downloads/alfonso-update.json` bleiben unverändert.
- Manifest, Service, Session und Screens geprüft: exportierter CarAppService,
  offizielle Action und IOT-Kategorie, `template`-Beschreibung, Mindest-Car-API 6,
  `app` und `app-projected` jeweils 1.7.0. PaneTemplate und SearchTemplate sind
  laut offizieller Template-Tabelle für IoT zulässig. Kein bisher nachgewiesener
  Konfigurationsfehler erklärt das fehlende Launcher-Symbol.
- Release prüft Host-Signaturen mit der Bibliotheks-Allowlist; Debug verwendet
  die offizielle Entwicklungsvalidierung. Die installierte Alfonso 18 ist kein
  Debug-Build. Ihr Service akzeptiert den echten Android-Auto-Host im DHU-Test.
  `HostConfig` meldet Bibliothek 1.7.0, App-Minimum 6, App-Maximum 8 sowie
  Host-Minimum 1 und Host-Maximum 8. Keine API-Unverträglichkeit im geprüften
  Startablauf festgestellt.
- Vorhandenes SDK: `C:\AndroidSDK`, JDK 17, SDK Platform 35, Build-Tools 35.0.0,
  adb 37.0.0. Offizielles Paket `extras;google;auto` ergänzt; DHU 2.0 für Windows
  lässt sich mit `--version` aufrufen. DHU-Verbindung und App-Start anschließend
  erfolgreich durchgeführt, siehe Vergleich unten.
- Lokale Prüfung des unveränderten App-Codes mit dem vorhandenen Gradle 8.11.1
  und `ANDROID_HOME=C:\AndroidSDK`: `assembleDebug testDebugUnitTest lintDebug
  signingReport` erfolgreich; der Testtask baut zusätzlich die Release-APK.
  92 JVM-Tests, 0 Fehler, 0 übersprungen. Lint: 66 Warnungen, keine Fehler.
  `signingReport` sowie `apksigner verify --print-certs` für beide neu gebauten
  APKs bestätigen die vorgegebene Identität. Debug-APK-Paket und Version mit
  aapt bestätigt: `de.parip69.homesprachsteuerung`, 19 / 30. Keine neue Version
  ausgegeben, keine APK installiert oder veröffentlicht. Der bestehend geänderte
  Wrapper wird für diese Prüfung nicht benutzt: Seine Distributionsadresse nennt
  9.7.1, während die eingetragene Prüfsumme gegenüber dem 8.11.1-Ausgangsstand
  unverändert ist. Diese lokale Build-Konfiguration erklärt keinen Gerätefehler.
- adb zunächst `unauthorized`, nach Bestätigung am Handy `device`.
  Gerät SM-S928B, Android 17 / API 37, aktiver Benutzer 0.
- Installiert: Alfonso 18 / 29, Android Auto 18.0.664244-release / 180664244.
  Alfonso ist für Benutzer 0 installiert, nicht verborgen, suspendiert,
  angehalten oder deaktiviert; keine deaktivierten Alfonso-Komponenten gefunden.
  Die normale PackageManager-Abfrage nach Action und IOT-Kategorie liefert
  `.car.AlfonsoCarAppService`.
- Installationsquelle: `com.google.android.packageinstaller`, auslösender
  Paketinstaller ebenfalls dieser, Ursprung `com.android.chrome`.
  Die installierte APK wurde nur zum lokalen Signaturvergleich ausgelesen:
  versionName 18, versionCode 29, Originalzertifikat gemäß AGENTS.md;
  SHA-256 der Datei `0abb349417ac381f8e0cac0c5956fa122fc39a5c3acb75ea46bd467f5d3b5af7`.
  Sie ist bytegleich mit `downloads/Alfonso-18.apk`.
- Android-Auto-Entwicklermodus aktiv. „Unbekannte Quellen“ im aktuellen UI
  **ausgeschaltet**; kein Umschalten vorgenommen. Play-Protect-Prüfung ist
  keine Android-Auto-Freigabe.

### Kontrollierter Vergleich am unveränderten Handy

| Zustand | Konkreter Befund |
|---|---|
| Normaler Host, vor DHU | PackageManager findet den Service, aber `CAR.VALIDATOR` meldet für Alfonso `Package DENIED; failed all other checks`. Direkt davor: `Finsky / PlayGearheadService de.parip69.homesprachsteuerung, app owners empty`. |
| Erste DHU-Verbindungsversuche | Zunächst Protokollabbruch; beim Wiederverbinden Absturz von `com.google.android.projection.gearhead:projection` mit `IllegalStateException: Already connected` in `DeveloperHeadUnitNetworkService`. Kein Alfonso-Absturz. |
| Frischer Host, offizielle DHU-Verbindung | Android Auto für Benutzer 0 ohne Datenlöschung beendet und neu geöffnet; Head-Unit-Server über sein offizielles Menü gestartet, adb-Tunnel 5277, eine DHU-Instanz. Protokoll 1.7 und TLS erfolgreich. Android-Auto-Oberfläche angezeigt. |
| DHU-Launcher und Smartphone-Liste | Meldung „Alfonso verfügbar“, Alfonso im Auto-Launcher und in „Launcher anpassen“ sichtbar; dort Häkchen gesetzt. Keine Häkchen geändert. |
| Alfonso im DHU geöffnet | Sichtbare Startseite „Alfonso“, „Bereit“, „Spracheingabe über die Auto-Eingabe“, Aktion „Spracheingabe“. `dumpsys activity services` bestätigt Binder-Verbindung von `com.google.android.projection.gearhead:projection` zum AlfonsoCarAppService. Host-API 1–8 und App-API 6–8 kompatibel. |
| Zurück zum normalen Host | DHU beendet, Head-Unit-Server gestoppt, Tunnel entfernt; Android Auto ohne Datenlöschung frisch geöffnet. In alphabetischer Liste folgt auf „Beenden“ direkt „Einstellungen“: Alfonso fehlt wieder. Erneut mehrere `CAR.VALIDATOR: Package DENIED` für Alfonso. Kein aktiver Alfonso-Service mehr. |

**Einordnung:** Die normale Android-Auto-Hostprüfung lehnt das Paket ab, während
der offizielle DHU-Entwicklungstest dieselbe unveränderte, ursprünglich signierte
Installation erfolgreich anzeigt und verbindet. Damit sind ein fehlender
Manifest-Eintrag, eine grundsätzliche Host-/API-Unverträglichkeit oder ein
Startseitenfehler als Erklärung dieses Unterschieds nicht belegt. Ein
Vertrauens-/Verteilungsproblem ist durch den Vergleich und `app owners empty`
stark gestützt. Die genaue interne Ablehnungsregel bleibt hinter der generischen
Host-Meldung verborgen; eine fehlende Play-Eigentümerzuordnung allein wird
deshalb nicht als vollständig bewiesene Einzelursache behauptet.

### Verbleibende Voraussetzung und Grenzen

Google verlangt für reale Fahrzeuge eine vertrauenswürdige Installationsquelle;
„Unbekannte Quellen“ gilt ausdrücklich nicht für Car-App-Library-Apps. Der
offizielle weitere Weg ist eine passende vertrauenswürdige Verteilung, etwa ein
Google-Play-Testtrack mit Android-Auto-Unterstützung, und anschließend ein echter
Fahrzeugtest. Ein Play-Console-Entwicklerkonto und die dortigen Voraussetzungen
wären dafür nötig. Für diese Verteilung muss der vorhandene Schlüssel auch als
Play-App-Signierschlüssel erhalten bleiben; Google unterstützt die sichere
Übernahme eines vorhandenen Schlüssels. Keine automatische neue Signatur wählen.

**Internal App Sharing ist für ein Update der vorhandenen Installation nicht
gleichwertig:** Google signiert diese Downloads automatisch mit einem separaten
Internal-App-Sharing-Schlüssel. Deshalb diesen Weg nicht ungeprüft als
datenbewahrendes Update der hier fest vorgeschriebenen Identität verwenden.

Kein Codefehler nachgewiesen, daher keine Codeänderung und keine neue APK-Version
ausgegeben. Eine reine Versionsnummernerhöhung beseitigt die beobachtete
Host-Ablehnung nicht nachweislich. Die bestehenden Repository-Updates bleiben
unverändert; sie sind für sich keine Android-Auto-Vertrauensfreigabe.

Spracheingabe, Mikrofonangebot, Ein-Tipp-Ablauf, Versand und Fahrzeugbetrieb
bleiben ungetestet. Es wurde keine Eingabeaktion in Alfonso gewählt und kein
Hausbefehl ausgelöst. Keine Installation, Deinstallation, Datenlöschung,
Profiländerung, gefälschte Installationsquelle oder Umgehung von Host-Prüfungen.
Kein Upload oder Push. Einstellungen, Verlauf und Smartphone-Funktionen wurden
nicht verändert; ein eigener vollständiger Regressionstest am Handy ist nicht
Bestandteil dieses reinen Launcher-/Host-Tests.

Aktuell erneut abgeglichene offizielle Quellen:

- https://developer.android.com/training/cars/apps/auto
- https://developer.android.com/training/cars/apps/iot
- https://developer.android.com/jetpack/androidx/releases/car-app
- https://developer.android.com/reference/androidx/car/app/validation/HostValidator
- https://developers.google.com/cars/design/create-apps/apps-for-drivers/templates/overview
- https://developer.android.com/training/cars/testing
- https://developer.android.com/training/cars/testing/dhu
- https://developer.android.com/training/cars/distribute
- https://developer.android.com/studio/publish/app-signing
- https://support.google.com/googleplay/android-developer/answer/9844679
