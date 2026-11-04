# Alfonso – privater Android-Auto-Prototyp 1.1.14-auto-test

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
