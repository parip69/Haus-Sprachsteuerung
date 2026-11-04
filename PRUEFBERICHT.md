# Prüfstand – Home-Sprachsteuerung 1.0.0

Prüfdatum: 1. November 2026. Paket: `de.parip69.homesprachsteuerung`.

## Tatsächlich ausgeführt

- Android-Debug- und Release-APK erfolgreich mit JDK 17, SDK 35 und Gradle 8.11.1 gebaut.
- `testDebugUnitTest`: **9 Tests bestanden, 0 Fehler**.
- `lintDebug`: **0 Fehler, 10 Hinweise**. Verbleibende Hinweise betreffen UI-/Kotlin-Stil,
  keine im Lint-Bericht erkannten blockierenden API- oder Berechtigungsfehler.
- HTTP-Tests mit MockWebServer: PUT-Pfad und URL-Kodierung, Basic Auth,
  unveränderter Text und JSON-Escaping, 401, kein Redirect-Following,
  optionales `invokeScenes` als JSON-Boolean.
- MQTT-Tests über echten lokalen TCP-Testbroker: unveränderte UTF-8-Payload,
  QoS-1-Publish ohne Retain, PUBACK und abgelehnte Anmeldung.
- Testmodus ohne Zielkonfiguration erfolgreich auf Transportebene getestet.
- XML-Dateien und VS-Code-Task-JSON syntaktisch geprüft.
- Release- und Debug-APK mit `apksigner verify` erfolgreich geprüft.
- Zertifikat beider APKs stimmt mit dem festen Projektschlüssel überein.
- Gradle-Distribution mit Hersteller-SHA-256 verglichen und Prüfsumme im Wrapper festgelegt.

## Nicht ausgeführt

Keine Installation auf einem echten Handy und kein Emulatorlauf.
Keine echte Mikrofonaufnahme, keine Laufzeitprüfung der Berechtigungsdialoge,
SQLite-Verlaufansicht oder des Hintergrunddienstes auf einem Samsung-Gerät.
Kein Zugriff auf Gerhards Fibaro, reale MQTT-Broker, Google Assistant oder Gemini.
Kein Test des `invokeScenes`-Verhaltens einer konkreten Fibaro-Firmware.
Das GitHub-Repository wurde nicht angelegt oder hochgeladen: Die verfügbare
Anbindung bietet keine Repository-Erstellung, und für den direkten API-Zugang
steht keine Anmeldung zur Verfügung.

Die Lieferung ist eine kompilierte, signierte erste Testversion. Eine
Produktionsfreigabe erfordert die Geräte- und Anlagentests aus der README.

Release-APK SHA-256:

`bd32062f8d6ce9993bd2150b24d27a94cc403ba614a5a82b7db0a15e70202663`

## Ergänzung: Version 1.0.1, Icon-Varianten

- Mikrofon: APK-Build und Signatur geprüft; das passende Originalbild ist pixelgleich im APK enthalten. SHA-256: `4bf7ef43bd665e51f5a84d91fef1a903e5f3d2362cd52f722ad07269aab6d998`
- Sprachwelle: APK-Build und Signatur geprüft; das passende Originalbild ist pixelgleich im APK enthalten. SHA-256: `8555bf037d7a1e1445320a0a6495d435eafe9c9fbe22852bd2a56f17d6441f5e`

Beide Varianten: versionCode 2, gleicher Paketname und gleicher Signierschlüssel wie 1.0.0.
Adaptive Icon-Ressource mit schwarzem Hintergrund und Abstand zur Maskengrenze.
Android-Lint für die aktualisierten Ressourcen ohne Fehler durchgelaufen.
Die Sprach- und Netzwerklogik wurde für diesen Schritt nicht geändert; die neun
bereits bestandenen Funktionstests wurden deshalb nicht erneut ausgeführt.
Kein Test auf einem echten Launcher; Geräteprüfung bleibt offen.

## Version 1.0.2 – proportionaler Icon-Abstand

Release-Build erfolgreich, Android-Lint ohne Fehler. APK-Signatur entspricht
weiterhin dem festen Schlüssel aus Version 1.0.0. Der gewählte Entwurf 1 wurde
nicht verändert; der adaptive Icon-Vordergrund erhält stattdessen 18 Prozent
Abstand pro Seite. Die frühere absolute 12-dp-Angabe wurde ersetzt.

Manuelle Bedienung, Hintergrundaufnahme eines Satzes, Verlauf und Versandlogik
sind unverändert. Es wurde keine Google-Assistant-/Gemini-Textübergabe eingebaut.
Kein Geräte- oder Launcher-Test dieser Version.

APK SHA-256: `c87799342b791d272128c6d9d879c7dbf3ca97a2be9334597b6f8de128676aeb`

## Version 1.0.3 – LGI Haus

App-Name LGI Haus, versionCode 4, gleicher Paketname und gleiches Zertifikat wie
bisher. Optionaler Schalter „Beim Öffnen sofort zuhören“, standardmäßig aus.
Manuelle Mikrofontaste bleibt verfügbar. Der Start erfolgt nur bei einem neuen
Launcher-Aufruf; Rückkehr aus Einstellungen oder Bildschirmdrehung lösen keine
zusätzliche automatische Aufnahme aus.

Release-Build erfolgreich. 9 Funktionstests bestanden, Android-Lint ohne Fehler.
APK-ZIP-CRC sowie DEX-Dateigrößen und SHA-1-Prüfsummen geprüft; der erste Build
hatte eine unvollständige DEX-Zwischendatei, die beim erneuten Build vollständig
neu erzeugt wurde. Zertifikat, Paketname, Version und App-Label aus der fertigen
APK geprüft.

Kein Laufzeittest des automatischen Aufnahmestarts auf einem echten Gerät.
Keine Google-/Gemini-Textübernahme und keine eigene Wakeword-Erkennung eingebaut.
Das gewünschte Folgeprojekt ist in NAECHSTER_SCHRITT.md festgehalten.

APK SHA-256: `da6fdb1a63d36a459974d557a787311fd4612697f81fa71c4a516d5c6609e56f`


## Version 1.0.4 – Hauspilot

App-Label und Aufrufhinweis auf Hauspilot geändert, versionCode 5 und
versionName 1.0.4. Release-Build und lintRelease erfolgreich; Lint:
0 Fehler, 0 schwere Fehler, 12 Hinweise.
APK-Label, Launcher-Activity, Paketname, Version und gültige Signatur geprüft.
Zertifikat SHA-256 unverändert:
`c32561af99bf0593178a31fa6b5fce55a2419ad121184999c6efd2df6918b564`.
ZIP-CRC und Größe/SHA-1 jeder DEX-Datei geprüft. Alle DEX-Dateien und das
Launcher-Originalbild sind bytegleich mit 1.0.3; keine Änderung der App-Logik.
Keine erneuten Funktionstests für die reine Namensänderung.
Kein Geräte-/Installationstest und kein Google-/Gemini-Aufruftest ausgeführt.
Die Erkennung des neuen App-Namens muss am Telefon getestet werden.

APK SHA-256: `ea62e6afcf8756ab1fc6529faba69b284a105ae8c0a9aa86957545073881d64a`


## Version 1.0.5 – automatische Einzelaufnahme und Rückkehr

Frische App-Aufrufe starten standardmäßig eine Aufnahme, auch ohne
CATEGORY_LAUNCHER. Vorhandene Installationen bekommen autoListen einmalig
eingeschaltet; danach werden Nutzeränderungen respektiert. Der neue autoHide-
Schalter ist standardmäßig an. Nach abgeschlossener automatischer Aufnahme
und Verarbeitung geht die Activity per moveTaskToBack in den Hintergrund.
Manuelle Aufnahmen bleiben geöffnet. „Nur Verlauf ansehen“ bricht die Aufnahme
ab und verhindert die automatische Rückkehr; Versand wird nicht abgebrochen.
Die Aufnahme-Benachrichtigung öffnet mit eigenem HISTORY-Intent ohne neue Aufnahme.
Die Wiederaufnahme über letzte Apps, Bildschirmdrehung und Rückkehr aus den
Einstellungen starten keine zusätzliche Aufnahme. Keine eigene Wakeword-Engine.

Clean-Release-Build, testDebugUnitTest und lintRelease erfolgreich.
14 Tests bestanden (9 HTTP/MQTT/Testmodus, 5 Start-/Abschlussregeln).
Die neuen Tests prüfen erlaubte Startaktionen, explizite Verlaufsöffnung,
Historienstart, deaktivierten Autostart, veraltete Statusereignisse, übersprungene
Zwischenzustände, gespeicherten Abschlussmarker und Abbruch der Rückkehr.
Kein Android-Lifecycle-/Mikrofon-/Berechtigungs-/Installations- oder Samsung-
Gerätetest durchgeführt; die Regeln sind JVM-Tests, keine Geräte-Simulation.
Lint: 0 Fehler, 0 schwere Fehler, 16 Hinweise.
APK-ZIP-CRC sowie Größe und SHA-1 aller DEX-Dateien geprüft. Haus-Icon
bytegleich mit 1.0.4. Paketname, Launcher, Label Hauspilot und Version 6 / 1.0.5
aus der fertigen APK geprüft. Gültige Signatur und gleiches Zertifikat:
`c32561af99bf0593178a31fa6b5fce55a2419ad121184999c6efd2df6918b564`.
APK SHA-256: `7cb5f650c33a9d6947ace23c4ece9590d292c35d2689c4e1ea209bb931fb40bc`


## Version 1.0.6 – getrennter Spracheinstieg

Eine APK, zwei exportierte Launcher-Activities: MainActivity (Hauspilot) als
bleibende manuelle Übersicht und VoiceCaptureActivity (Hauspilot Sprache) für
den einzelnen Hintergrundauftrag. Der Spracheinstieg zeigt ein kleines
Startfenster und bleibt sichtbar bis zum gestarteten Mikrofon-Foreground-Service
und Bereitschaft des Sprachdiensts; erst dann wird sein separater Task beendet.
Er liegt nicht in den letzten Apps. Die Aufnahme selbst läuft in CaptureService
weiter, speichert und sendet einen Satz und beendet sich. Die MainActivity wird
dabei nicht geschlossen und startet beim Öffnen keine Aufnahme mehr.

Optionale Start-/Ergebnis-Toasts durch den Service; keine TTS-Sprachausgabe.
Android-Mikrofonanzeige und Foreground-Benachrichtigung bleiben aktiv, solange
aufgenommen wird. Fremde Sprachdienst-Töne werden nicht verändert.
Abschlusszähler getrennt von UI-Toast-Ereignissen: kein erneuter alter Toast beim
späteren Öffnen der Übersicht nach einer Hintergrundaufnahme.

Clean-Build assembleRelease, testDebugUnitTest und lintRelease erfolgreich.
14 JVM-Tests bestanden: 9 Transporttests und 5 Regeln zum Schließen des
Spracheinstiegs (kein eigener Auftrag, Startphase, Bereitschaft, Fehlerabschluss,
veraltete Abschlüsse und Abschluss während Wiederherstellung).
Lint: 0 Fehler, 0 schwere Fehler, 13 Hinweise.
Manifest mit zwei Launcher-Zielen, getrenntem Aufnahme-Task und excludeFromRecents
geprüft. APK-Label/Version 7 / 1.0.6, ZIP-CRC sowie Größen/SHA-1 aller DEX-Dateien
geprüft. Haus-Originalbild identisch mit 1.0.5. Paketname und Zertifikat gleich:
`c32561af99bf0593178a31fa6b5fce55a2419ad121184999c6efd2df6918b564`.

Kein Android-Geräte-, Lifecycle-, Mikrofon-, Berechtigungs- oder Installationstest.
Insbesondere wurde NICHT geprüft, ob Google/Gemini den zweiten Starteintrag
„Hauspilot Sprache“ auf Gerhards Samsung am Namen findet. Das ist der nächste
Gerätetest; direktes Antippen des zweiten Eintrags testet zuerst den Android-Weg
unabhängig von Googles Namenssuche. Keine garantierte Google-Integration.

APK SHA-256: `5de9669c49fb063fcef5ca876f0d2712a408cee72ebd67be831aaab7058f0279`


## Version 1.0.7 – ein Name, ein Starteintrag: Jarvis

App-Name Jarvis, versionCode 8 / 1.0.7. Nur VoiceCaptureActivity ist als
Launcher-Eintrag registriert. MainActivity bleibt die manuelle Übersicht,
erreichbar per statischer App-Verknüpfung bei langem Drücken auf dasselbe Symbol
sowie über die Aufnahme-Benachrichtigung. Kein zweiter App-Name und keine zweite
APK. Standardaufruf zum Testen: „OK Google, öffne Jarvis“. Normales Antippen
des Symbols startet denselben Einzelaufnahmeweg. Keine behauptete automatische
Unterscheidung identischer manueller und Google-Startintents.

Release-Build und lintRelease erfolgreich. Lint: 0 Fehler,
0 schwere Fehler, 14 Hinweise. Die 14 Funktionstests aus
1.0.6 wurden für Namens-/Manifest-/Verknüpfungsänderungen nicht wiederholt.
Manifest und Shortcut-XML geprüft: genau ein Launcher-Eintrag und explizite
Verknüpfung zur MainActivity. Fertige APK ebenfalls mit genau einem
Launcher-Eintrag, Label Jarvis, richtigem Paket und Version geprüft.
Die durch Ressourcenoptimierung verkürzte XML-Verknüpfung in der APK geprüft.
ZIP-CRC und Größe/SHA-1 aller DEX-Dateien gültig; Haus-Icon unverändert.
Signatur gültig; Zertifikat weiterhin
`c32561af99bf0593178a31fa6b5fce55a2419ad121184999c6efd2df6918b564`.

Kein Geräte-/Google-/Launcher-/Mikrofon-Test ausgeführt. Nächster Test auf dem
Samsung: „OK Google, öffne Jarvis“, Bereitschaft abwarten, Satz sprechen.
Danach Symbolmenü → Verlauf und Einstellungen öffnen zur manuellen Übersicht.
APK SHA-256: `ffa4b55337e305135c72538c068021f831ad306b0d6b537969e527066afdde8b`


## Version 1.0.8 – Versandrückmeldung und optionale Sprachausgabe

Ergebnisfeedback aus typisiertem Transportstatus: bestätigt gesendet, nur lokal
gespeichert, nicht gesendet, unklarer Sendestatus oder bewusst abgebrochen.
Toast vom Aufnahmedienst nach Abschluss für manuelle und Hintergrundaufnahmen.
Textrückmeldungen werden bei diesem Update einmalig eingeschaltet; spätere
Änderungen bleiben gespeichert. Optionaler Schalter „Ergebnis vorlesen“,
standardmäßig aus. Deutsch-TTS erst nach Ende der Aufnahme; eine neue Aufnahme
stoppt vorherige Ansagen. Application-eigene TTS-Verwaltung mit Initialisierungs-
und Sprachprüfung, UtteranceProgressListener und shutdown nach Ende/Fehler bzw.
spätestens nach 15 Sekunden. Bei TTS-Fehler fällt die Rückmeldung auf Text zurück.
Keine Veränderung globaler Lautstärken. Kein erneuter UI-Toast beim späteren
Öffnen der Übersicht. Ausführliche Statusdetails bleiben erhalten.

Clean-Release-Build, testDebugUnitTest und lintRelease erfolgreich.
20 JVM-Tests bestanden (9 Transport, 5 Spracheinstieg, 6 Ergebnisfeedback).
Neue Tests: bestätigter Erfolg, Testmodus ohne falsche Sendebestätigung,
Timeout/MQTT als unklar, HTTP-Ablehnung/DNS-Fehler, Verlaufsstatusfehler nach
bestätigtem Versand sowie Aufnahmeabbruch. Keine erneuten Netzwerkbefehle.
Lint: 0 Fehler, 0 schwere Fehler, 15 Hinweise.
APK-ZIP und Größe/SHA-1 aller DEX-Dateien geprüft. Das Launcher-Bild wurde über
die kompilierten Ressourcenpfade aufgelöst und ist bytegleich mit 1.0.7.
Genau ein Launcher-Eintrag Jarvis, Version 9 / 1.0.8, Verlaufsverknüpfung und
TTS_SERVICE-Paketsichtbarkeit in der fertigen APK geprüft. Signatur gültig und
Zertifikat gleich:
`c32561af99bf0593178a31fa6b5fce55a2419ad121184999c6efd2df6918b564`.

Kein Geräte-/Google-/Mikrofon-/Toast-/TTS-Laufzeittest. Insbesondere tatsächliche
TTS-Engine, deutsche Stimme, Hintergrundausgabe und Lautstärke müssen auf dem
Samsung geprüft werden. Die JVM-Tests prüfen Ergebnisregeln und Transport,
keine Audioausgabe oder Android-Lifecycle-Simulation.
APK SHA-256: `d980a9501d990b4fa4827bea8a94ab017da8d2fd946ebc83e2eaf6bd1c48c5e1`


## Version 1.0.9 – erkannten Satz in Ergebnisanzeige und Ansage aufnehmen

CaptureService hält den ersten erkannten Rohtext zusätzlich für die Rückmeldung
fest. Anzeige und optionale Ansage nennen diesen Satz zuerst und anschließend
das Ergebnis, z. B. „Wohnzimmer Rollo 1 öffnen. Erfolgreich gesendet.“
Auch Fehler, unklarer Versand und Testmodus nennen den Satz. Der an das Ziel
gesendete Rohtext bleibt unverändert. Ohne erkannten Text wird kein Satz erfunden.
Die Statusansicht zeigt vollständigen Text, Ergebnis und Details; der Verlauf
bleibt erhalten. Die kurze System-Texteinblendung unterliegt Androids Zeilenlimit.
Die TTS-Frist berücksichtigt die Textlänge und liegt zwischen 15 und 120 Sekunden.

Clean-Build assembleRelease, testDebugUnitTest und lintRelease erfolgreich.
24 JVM-Tests bestanden: 9 Transport, 5 Spracheinstieg, 10 Ergebnisfeedback.
Vier neue Fälle prüfen Satz-vor-Ergebnis, Fehler/Testmodus/Unklarheit mit Satz,
Satzzeichen/fehlenden Text und vollständige lange Sätze in der Statusansicht.
Lint: 0 Fehler, 0 schwere Fehler, 14 Hinweise.
APK-CRC, Größe/SHA-1 aller DEX-Dateien, Label Jarvis, Version 10 / 1.0.9,
genau ein Launcher-Ziel und unverändertes Haus-Originalbild geprüft.
Signatur gültig; Zertifikat identisch:
`c32561af99bf0593178a31fa6b5fce55a2419ad121184999c6efd2df6918b564`.

Download-Prüfung: Dateiname .apk und MIME-Type der gespeicherten 1.0.8-Datei
application/vnd.android.package-archive sind korrekt. Die konkrete Übergabe beim
direkten Öffnen aus dem Chat konnte hier nicht geprüft werden. Andere dort
funktionierende Projekt-APKs waren nicht zugänglich; keine vermeintliche
Reparatur der direkten Installation behauptet. Ein alternativer Dateizugang
zur neuen APK wird zum Testen angeboten.

Kein Geräte-/Google-/Toast-/TTS-Laufzeittest und kein direkter Chat-Installer-Test.
APK SHA-256: `ec066d7702dcabb0f1ba2edf29ac0894f797aa85eb9611e899437cd882b7349e`


## Version 1.1.0 – zwei Fibaro-Profile und automatische Auswahl

Eigene URLs, Benutzer und Passwörter für Lokal und Öffentlich. Die bisherigen
SharedPreferences-Schlüssel bleiben unverändert das lokale Profil; öffentliche
Zugangsdaten werden separat gespeichert und kein bestehendes Passwort kopiert.
Öffentliche Standardadresse mit dem korrigierten Port 48884, lokale Standard-
adresse 192.168.111.111:80 nur als Default. Verbindungsauswahl AUTO/LOCAL/PUBLIC.
Automatisch wird vor jedem Versand die lokale Variable nur lesend geprüft, mit
1,5 Sekunden Call-Timeout. Passendes JSON oder eine 401/403-Antwort halten die
Auswahl lokal; sonst wird vor dem ersten PUT öffentlich gewählt. Nach einem PUT
gibt es keinerlei Profilwechsel oder Wiederholung. Modus Test und MQTT bleiben
von dieser Auswahl unabhängig. Kein SSID-/Standortzugriff.

Neue UI: zwei beschriftete Profile, gemeinsame Variable/invokeScenes und ein
lesender Verbindungstest mit aktuellen Eingaben. Er erkennt passende Variable,
Authentifizierungsfehler, Weiterleitungen und Web-Anmeldeseiten. Bekannte HTML-
Antworten auf PUT werden nicht als Erfolg gemeldet. TLS-Prüfung und gesperrte
Weiterleitungen bleiben erhalten. Fehlermeldung/Erfolg nennen den Eingang.

Clean-Build, testDebugUnitTest und lintRelease erfolgreich. 35 Tests bestanden.
11 neue Routingtests prüfen explizite Ziel-/Zugangsdaten-Trennung, lokales GET
vor genau einem PUT, öffentlichen Fallback nur vor dem PUT, HTML-Erkennung,
keinen öffentlichen Replay nach PUT-Fehler, lokale 401 ohne Profilwechsel,
rein lesenden Test, abgelehnte HTML-Sendebestätigung, lokale Probe-Zeitgrenze
sowie unabhängige Profilvalidierung und Port 48884.
Lint: 0 Fehler, 0 schwere Fehler, 19 Hinweise.
APK-CRC und Größe/SHA-1 aller DEX-Dateien gültig. Paket/Version 11 / 1.1.0,
Label Jarvis, genau ein Launcher und unverändertes Haus-Originalbild geprüft.
Signatur gültig; Zertifikat weiterhin
`c32561af99bf0593178a31fa6b5fce55a2419ad121184999c6efd2df6918b564`.

Extern je ein nicht authentifizierter GET über HTTP und HTTPS an die MyFRITZ-
Adresse mit Port 48884: beide endeten mit 503 vom vorgelagerten Envoy-Gateway
und Verbindungsfehler, keine bestätigte HC2-Antwort. Kein reales Passwort, kein
Login und kein PUT an Gerhards Gerät ausgeführt. Gerhard bestätigt, dass die
FRITZ!Box-Portweiterleitung zum Fibaro HC2 führt; dafür ist Fibaro-Basic-Auth
vorgesehen, keine FRITZ!Box-Websitzung.

Nicht ausgeführt: Android-UI-/SharedPreferences-Upgrade-/Wechsel-WLAN-Mobilfunk-/
reale Portweiterleitungs-/Toast-/TTS-Gerätetests. Neue Routingtests laufen über
MockWebServer, keine echte Haussteuerung. Netzwerkzugang vom Testtelefon muss
mit „Verbindung prüfen“ lokal und über Mobilfunk getestet werden.
APK SHA-256: `d39bd39d3639d5d6613ece8b73f145cdc0e5776c83d6ad228a6195876a577238`


## Version 1.1.1 – konfigurierbare Wartezeit und zusätzliche Sprechpause

Neue SharedPreferences für Anfangswartezeit 5–60 s (Standard 20), zusätzliche
Pause nach einem Erkennungsergebnis 1–15 s (Standard 5) und maximales
Aufnahmefenster 15–180 s (Standard 90, mindestens Anfangswartezeit). Die
Anfangswartezeit zählt ab erster nativer Bereitschaft; native stille Enden
innerhalb der Frist werden neu gestartet, ohne die Anfangsfrist zurückzusetzen.
Nach einem bestätigten Satz wird automatisch auf eine Fortsetzung gewartet.
Neue Sprache hebt die alte Pause auf; nach deren Ergebnis beginnt die neue
Pausenfrist. Mehrere Endergebnisse werden mit Leerzeichen verbunden, ein
einzelnes bleibt unverändert. Genau eine finale Speicherung/Netzwerksendung.
Kein Drücken von „Jetzt senden“ erforderlich. Dieser zusätzliche Knopf in
Übersicht/Benachrichtigung schließt lediglich früher ab; laufende native
Sprache wird mit stopListening abgeschlossen, mit bis zu 8 s Ergebniswartezeit.

Abbruch/Limit/Erkennungsfehler bei unbestätigter Fortsetzung senden keinen
Teilbefehl. Bereits bestätigte Teile werden ungesendet im Verlauf gespeichert.
Ein Generationenzähler verwirft alte Callbacks zerstörter Erkenner; alle
Handler-Timer werden bei Ende entfernt. Die App schreibt keine Google-/Gemini-
Einstellungen um und verändert keine globalen Lautstärken.

Clean-Build assembleRelease, testDebugUnitTest, lintRelease erfolgreich.
47 JVM-Tests bestanden, davon 12 neue Zeit-/Puffer-Tests. Geprüft sind Startfrist
ab Bereitschaft, kein Neustart der Frist durch stille native Wiederholung,
Sprechbeginn vor der Frist, automatische Pause, Verlängerung bei Fortsetzung,
kein Teilversand während ausstehender Fortsetzung, absolute Begrenzung,
Bestätigung vor dem Limit, unveränderter Rohtext/Einmal-Submission, fehlender
Text, Zeitwertvalidierung und genau ein echter MockWebServer-PUT mit
zusammengefügten Erkennungsteilen.
Lint: 0 Fehler, 0 schwere Fehler, 23 Hinweise.
APK-CRC und Größe/SHA-1 aller DEX-Dateien gültig. Paket/Version 12 / 1.1.1,
Label Jarvis, genau ein Launcher und unverändertes Haus-Originalbild geprüft.
Signatur gültig; gleiches Zertifikat wie alle Vorversionen:
`c32561af99bf0593178a31fa6b5fce55a2419ad121184999c6efd2df6918b564`.

Nicht geprüft: tatsächliche Android-Mikrofonfolge, Runtime-Berechtigungen,
Callback-Reihenfolgen verschiedener Sprachdienste, lückenlose Weiteraufnahme,
Signaltöne, Screenshot-/UI-/TTS-Geräteprüfung, Google-Öffnungsansage. Beim
Neustart einer nativen Erkennung sind kurze Lücken, Signaltöne oder Busy-Fehler
möglich. Das muss auf Gerhards Samsung getestet werden. Die Tests bestätigen
die Zeit-/Übertragungsregeln, keine fehlerfreie kontinuierliche Audioaufnahme.
Lokale/öffentliche Fibaro-Profile und Port 48884 wurden nicht verändert.
APK SHA-256: `d09a4fd12fee2aa7acbcb8d353673b14d4e7ba3c7a239c3fe10dc1402c188709`


## Version 1.1.2 – Feinjustierung kurzer Zeiten

Anfangswartezeit 0–60 s, Zusatzpause 0–15 s, Aufnahmezeit 2–180 s.
Aufnahmezeit darf nicht kleiner als die Anfangswartezeit sein.
Neue Standardwerte: 1 / 1 / 10 Sekunden; gespeicherte Werte bleiben erhalten.
Anfangswartezeit 0 deaktiviert die Anfangsfrist, nicht das absolute Limit.
Pause 0 sendet ohne Zusatzpause direkt nach dem bestätigten Erkennungsergebnis.
Native Ergebnislatenz kann die App nicht auf null verkürzen.

Clean-Build assembleRelease, testDebugUnitTest und lintRelease erfolgreich.
50 JVM-Tests bestanden. Neue Tests prüfen 0-Anfangsfrist mit weiterhin
wirksamer maximaler Dauer, sofortigen einmaligen Versand bei 0-Pause und
Standardwerte sowie Untergrenzen/Gleichheit der Zeitfelder.
Lint: 0 Fehler, 0 schwere Fehler, 22 Hinweise.
Paket unverändert, VersionCode 13 / 1.1.2, Label Jarvis und ein Launcher.
APK-Signatur gültig und unverändert; DEX/ZIP-Integrität sowie gleiches Icon geprüft.
Keine Prüfung auf echtem Android-Gerät oder echter Fibaro-Hardware.
Die Einschränkungen der nativen Aufnahme aus 1.1.1 gelten weiterhin.
APK SHA-256: `db4ce17d2c34d9cf531ad3586a23638cae714464c6a89f3fc4232470f1762288`


## Version 1.1.3 – optionaler Befehl in der Sprachausgabe

Neuer dauerhaft gespeicherter Schalter „Befehl mit vorlesen“, Standard aus.
Bei aktivierter Sprachausgabe nur „Erfolgreich gesendet“ oder Fehlerdetails.
Mit eingeschaltetem Schalter weiterhin Befehl + Ergebnis. Keine Änderung an
Textanzeige, Verlauf, Rohtext-Versand oder Zeitwerten.
Clean-Build assembleRelease, testDebugUnitTest und lintRelease erfolgreich.
53 JVM-Tests bestanden. Neue Prüfungen: kurzer Erfolg ohne Befehl, vollständiger
Text im Überblick, explizite Langform, Fehlerdetails ohne Befehl, Testmodus und
Abbruch ohne falsche Erfolgsbehauptung.
Lint: 0 Fehler, 0 schwere Fehler, 22 Hinweise.
APK-Signatur unverändert und gültig; ZIP/DEX-Integrität und Icon geprüft.
Paket unverändert, Version 14 / 1.1.3, Label Jarvis, ein Launcher.
Keine TTS-/UI-Prüfung auf echtem Android-Gerät; kein echter Fibaro-Versand.
APK SHA-256: `2882e403fe4c23a57e50313b905e2c0378c7d25e327d37942ffbfd2f0cf91e20`


## Version 1.1.4 – ausgeschalteter Spracheinstieg öffnet die Übersicht

Der Launcher-Zweig bei ausgeschaltetem Spracheinstieg öffnet jetzt explizit
MainActivity mit ACTION_HISTORY, NEW_TASK und CLEAR_TOP. Danach wird die
kurzlebige Spracheinstiegs-Task entfernt. Kein Start der Aufnahme in diesem
Zweig. Die früher abgeschnittene Toast-Meldung wird hier nicht mehr verwendet.

Clean-Build assembleRelease, testDebugUnitTest und lintRelease erfolgreich.
53 bestehende JVM-Tests bestanden; Lint ohne Error/Fatal. Statische Prüfung des
Launcher-Zweiges und des expliziten Übersichtsziels bestanden. APK-ZIP gültig,
Paket unverändert, Version 15 / 1.1.4, ein Launcher, Signatur unverändert.
Keine Geräteprüfung der Task-Navigation, Google-Ansage oder TTS.
Kein Schlüssel-/Paketwechsel; keine Änderung an Zielprofilen und Zeitwerten.
SHA-256: `30546790b8bfc6ce3f1428d2b7e04f1133c58b1e74ac4b11447f7f76cc7137ac`


## Version 1.1.5 – lokale und öffentliche Prüfung getrennt

Je ein Prüfbutton mit eigenem Ergebnisfeld beim lokalen bzw. öffentlichen
Profil. Beide setzen die Verbindung für den Prüfaufruf explizit auf LOCAL bzw.
PUBLIC; die Auswahl für späteren Versand wird dadurch nicht gespeichert oder
verändert. Prüfwerte kommen aus der Maske einschließlich ungespeicherter Werte.
Die vorhandene GET-Prüfung wird unverändert genutzt. Beide Knöpfe werden für
die Dauer einer Prüfung gesperrt.
Clean-Build assembleRelease, testDebugUnitTest, lintRelease erfolgreich.
53 bestehende JVM-Tests bestanden, inklusive öffentlichem GET ohne lokalen
Aufruf, ohne Body und ohne Schreibzugriff. Statische UI-Verknüpfung beider
Buttons und expliziter Profilwahl geprüft. Lint ohne Error/Fatal.
Signatur und Paket unverändert, ZIP gültig, Version 16 / 1.1.5, ein Launcher.
Nicht auf echtem Android-Gerät oder echtem öffentlichen HC2 getestet.
SHA-256: `1ec08688d315ae0e8861946ac90ef38bbf033ca26e0dc9313c8393326be012f9`


## Version 1.1.6 – Fehlerdiagnose beim Verbindungstest

Der Check zeigt GET-Ziel und Dauer. Ausnahmen werden als DNS, Timeout, TCP,
fehlende Route, TLS-Handshake/TLS, Android-Cleartext-Richtlinie, verweigerter
Netzwerkzugriff oder vorzeitiges Ende eingeordnet. Fehlertypen und bekannte
Netzwerkcodes bleiben sichtbar. Keine rohen Ausnahme-/Antworttexte oder
Zugangsdaten in der Diagnose; die URL ist vor Darstellung auf fehlende
Zugangsdaten, Query und Fragment validiert. Validierungsfehler beim Profil.
Keine Änderung am eigentlichen Versand oder den gespeicherten Profilen.
Clean-Build mit rerun-tasks assembleRelease, testDebugUnitTest, lintRelease
erfolgreich. 58 JVM-Tests bestanden, inklusive 5 neuer Diagnose-Tests für
verschachtelte DNS-Fehler, Timeout/TLS, TCP-Codes, Android-Richtlinie und
Berechtigungen sowie Unterdrückung von sensiblen Ausnahmetexten.
Lint ohne Error/Fatal. Signatur und Paket unverändert, APK-ZIP geprüft.
Version 17 / 1.1.6, genau ein Launcher. Kein Geräte-/echter HC2-Test.
Die Ursache des gemeldeten Handy-Verbindungsfehlers ist noch nicht bestimmt.
SHA-256: `ac67562a076154e1de2d1ec8ed687c8195ace979a5a140bb005b726bdfe6dc09`


## Version 1.1.7 – vollständige Browser-Links übernehmen

Neues maskiertes optionales Linkfeld mit Übernehmen-Knopf. Einfügen eines
Links mit Benutzername/Passwort und optionalem Variablen-API-Pfad füllt
öffentliche Basis-URL, Zugangsdaten und gemeinsame Variable. URL-kodierte
Bestandteile werden einmal dekodiert, Pluszeichen bleiben erhalten. Danach
wird das Linkfeld geleert; Prüfen/Speichern übernimmt ebenfalls vor dem Lesen
der Konfiguration. Übernehmen allein speichert keine Einstellungen.
Versand weiterhin mit explizitem HTTP Basic Auth. Keine echten Zugangsdaten
verwendet oder eingebettet. Die alte Fehlerursache ist nicht nachgewiesen.
Clean-Build assembleRelease, testDebugUnitTest und lintRelease erfolgreich.
65 JVM-Tests bestanden; 7 neue Tests: URL-Zerlegung/Port/geschütztes Passwort,
einmalige Dekodierung und Pluszeichen, IPv6-Basislink mit bestehender Variable, /invoke-Übernahme als JSON-Option,
Ablehnung ungeeigneter Links ohne Passwort im Fehlertext, tatsächlicher
MockWebServer-GET und genau ein PUT mit korrektem Auth-Header und API-Pfad.
Lint ohne Error/Fatal. APK-Signatur und Paket unverändert, ZIP gültig.
Version 18 / 1.1.7, ein Launcher. Keine Geräteprüfung oder authentifizierte
Verbindung zur echten HC2.
SHA-256: `c2d78553d8a86b15559e213be482b7ffdf6075c317448845b9932a3905754d05`


## Version 1.1.8 – IPv4-Priorität bei HTTP und tatsächliches Verbindungsziel

HTTP-DNS sortiert IPv4 vor IPv6, ohne Adressen zu entfernen; reine IPv6-Ziele
bleiben verfügbar. Kein fest eingebauter Host oder IP. HTTP-Wiederholungen und
Weiterleitungen bleiben abgeschaltet. Diagnose-GET nutzt einen EventListener
und zeigt tatsächliche TCP-Ziele mit Adressfamilie und Port auch bei Fehlern.
Keine Zugangsdaten in Diagnoseausgaben. Die IPv6-Hypothese zum gemeldeten
ECONNREFUSED ist auf dem Handy noch zu bestätigen.
Clean-Build assembleRelease, testDebugUnitTest, lintRelease erfolgreich.
70 JVM-Tests bestanden. Fünf neue Tests: stabile IPv4-Priorität, IPv6-only und
DNS-Fehlerweitergabe, echte Dual-Stack-Testauflösung mit erfolgreichem GET auf
IPv4 und Zielprotokoll, kein doppelter PUT bei Verbindungsschluss nach Empfang,
fehlgeschlagener GET mit erhaltenem TCP-Ziel in der Diagnose.
Lint ohne Error/Fatal. Signatur und Paket unverändert; APK-ZIP gültig.
Version 19 / 1.1.8, ein Launcher. Kein echter Handy-/Fibaro-Versand getestet.
SHA-256: `a4cd5ce14820069f12d5a1392dc4ade77b691d76c179e07fcb9fe72eb0031fb4`


## Version 1.1.9 – redundantes öffentliches Linkfeld entfernt

Nur ein öffentliches Adressfeld. Zusätzliche Linkeingabe und Übernehmen-Knopf
entfernt. Prüfen/Speichern normalisiert volle Browser-Links aus dem Adressfeld
in bestehende URL-/Zugangs-/Variablenfelder. Reine Basisadressen überschreiben
keine Zugangsdaten. API-Links ohne Userinfo verwenden die separaten Felder.
Bei Neuinstallationen korrigierte öffentliche Vorgabe, kein vorbelegter
öffentlicher Benutzername. Gespeicherte Werte bleiben erhalten.
Ein Starteintrag, keine neuen Namen oder zusätzlichen Speicherorte.
Clean-Build assembleRelease, testDebugUnitTest, lintRelease erfolgreich.
73 JVM-Tests bestanden, davon drei neue Prüfungen für unveränderte Basis-URL-
Eingabe, API-Link mit vorhandenen Zugangsdaten und vollen Link im einen Feld.
Lint ohne Error/Fatal; APK-Signatur/Paket unverändert, ZIP geprüft.
Version 20 / 1.1.9. Keine echte Handy-UI-/Neuinstallationsprüfung.
SHA-256: `88390df2ced54fee05759fa0da5c7f1235d16ab30e820a98bcb2530125d3078d`


## Version 1.1.10 – ein Starteintrag Hauszentrale, volle Übersicht

Der einzige LAUNCHER-Eintrag ist MainActivity mit Anzeigename Hauszentrale.
VoiceCaptureActivity bleibt ohne Launcherfilter ausschließlich zur Weiterleitung
alter expliziter Verknüpfungen auf MainActivity erhalten. Es gibt keinen
regulären separaten Schnellstarter mehr. Frischer MAIN-Aufruf startet bei
aktiviertem Schalter eine Aufnahme in sichtbarer Übersicht; die Ansicht bleibt
nach Ende offen. History-Aufruf, Rotation/Restoration und Android-Historie
starten keine neue Aufnahme. Laufende Vorgänge werden nicht vorgemerkt.

Clean-Build assembleRelease, testDebugUnitTest und lintRelease erfolgreich.
74 JVM-Tests bestanden. Die sechs überarbeiteten Startregeln prüfen frischen
MAIN-Aufruf, deaktivierten Einstieg, explizite History, System-Historie,
Restoration und unbekannte Aktionen. Kompiliertes APK-Manifest mit genau einer
LAUNCHER-Kategorie und MainActivity als Ziel geprüft; Label Hauszentrale.
Statisch geprüft: alter Einstieg leitet weiter und startet selbst keinen
CaptureService. Lint ohne Error/Fatal. Signatur/Paket unverändert, APK-ZIP gültig.
VersionCode 21 / 1.1.10. Kein echter Android-Lifecycle-/Launcher-/Google-Test.
Alte Startbildschirm-Verknüpfungen fremder Launcher können nicht automatisch
gelöscht oder umbenannt werden. Bei Bedarf manuell nur die Symbole entfernen
und Hauszentrale aus der App-Liste neu hinzufügen. Nicht deinstallieren.
SHA-256: `9a99968b5b02cd343604e39f92a14f4ff486e426b33989d0c4d7952b465f7366`


## Version 1.1.11 – Jetzt-sprechen-Shortcut und bedienbares Zahnrad

Ein normaler LAUNCHER-Eintrag Hauszentrale bleibt erhalten. Neuer statischer
Shortcut „Jetzt sprechen“ startet die Aufnahme in voller Übersicht auch bei
ausgeschaltetem automatischem Zuhören für normale Appstarts. Über das Menü
kann der Nutzer das Anheften beim Launcher anfordern und im Systemdialog
bestätigen. Kein automatisches zusätzliches Symbol. Das Zahnrad bleibt während
der Aufnahme aktiv; öffnet die Einstellungen und fordert Aufnahmeabbruch an.
Ein laufender Versand wird weiter abgeschlossen. Keine neue Autoaufnahme
beim Zurückkehren aus Einstellungen oder Historie/Restoration.
Clean-Build assembleRelease, testDebugUnitTest, lintRelease erfolgreich.
76 JVM-Tests bestanden, einschließlich zweier zusätzlicher Regeln für expliziten
Sprech-Shortcut bei deaktivierter normaler Automatik und keine erneute Aufnahme
aus Restoration/Systemhistorie. Shortcut-XML, Ziel, Aktion, Anheft-Anfrage und
aktives Zahnrad statisch geprüft. Kompiliertes Manifest: genau ein Launcher,
MainActivity, Label Hauszentrale. APK-Signatur/Paket unverändert, ZIP gültig.
VersionCode 22 / 1.1.11. Keine echte Launcher-/Pinning-/Hardwaretasten-/
Lifecycle-/Mikrofon-Geräteprüfung. Tastenbelegung hängt vom Launcher/Tool ab.
SHA-256: `26a9f44ac42563cd586fbd4eedbf287fe4af5f80a81c03daf1dc4419a1b2ed8f`


## Version 1.1.12 – Widget mit Logo, Aufnahmestart und Zahnrad

Neuer RemoteViews-AppWidgetProvider mit Layout und Provider-Metadaten.
Hauslogo/Titel öffnen die passive Übersicht, „Aufnahme starten“ öffnet die
sichtbare MainActivity mit START_CAPTURE. Zahnrad verwendet OPEN_SETTINGS,
startet keine Autoaufnahme und öffnet die Einstellungen über die vorhandene
Abbruchlogik. Alle PendingIntents explizit und immutable. Provider startet
keinen Dienst; keine periodischen Updates. Menü fordert optional das Anheften
des Widgets beim Launcher an. Der zusätzliche Sprech-Shortcut und dessen
Anheft-Menü entfallen. Der normale App-Starteintrag bleibt einmalig.

Clean-Build assembleRelease, testDebugUnitTest, lintRelease erfolgreich.
77 JVM-Tests bestanden, inklusive Startpolitik für Aufnahme und passiven
Widget-Einstellungsaufruf. Widget-XML/Metadaten, Aktionen, PendingIntents,
fehlender Service-Aufruf im Provider sowie updatePeriodMillis=0 statisch
geprüft. Kompiliertes APK-Manifest mit einem Launcher und Widget-Receiver
geprüft. Lint ohne Error/Fatal. Signatur/Paket unverändert, APK-ZIP gültig.
VersionCode 23 / 1.1.12. Keine Geräteprüfung von Widget-Darstellung, Größe,
Anheften, Android-Lifecycle, Mikrofon oder tatsächlicher Launcher-Kompatibilität.
SHA-256: `39f554c8122a2347e11c42a6cb49c0ad0fa4d0dcc936aba6a881a479a4a1d8b4`


## Version 1.1.13 – Alfonso und dezentes 1×1-Widget

App-Anzeigename und aktuelle Bedienhinweise auf Alfonso geändert. Ein Launcher-Eintrag MainActivity. Widget auf 1×1 Zelle verkleinert; genau ein runder 56-dp-Button mit Haus-/Mikrofonsymbol. Kein Titel, kein Zahnrad, kein Text. Außenfläche transparent; Button-Hintergrund #141C2933 (ca. 92 % transparent), dezente Kontur, sichtbare Druckrückmeldung. Ein Tipp startet weiterhin die Aufnahme über ACTION_CAPTURE. Einstellungen in der vollständigen App erreichbar.

assembleRelease, testDebugUnitTest und lintRelease erfolgreich. 77 JVM-Tests, keine Fehler. Lint ohne Error/Fatal. Kompilierte Widget-Metadaten, Ein-Button-Layout, transparente Hintergrundfarbe, App-Name Alfonso, VersionCode 24 und Paketkennung geprüft. APK-Signatur gültig und identisch mit 1.1.12. ZIP gültig. Keine Geräteprüfung der Widget-Größe/Darstellung, Aufnahme oder Google/Gemini-Namenserkennung.

SHA-256: `ea8eabfd74bc42b45fc5f460b1c755ea4f169fcdc7fb314b773649091f480517`
