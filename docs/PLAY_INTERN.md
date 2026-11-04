# Alfonso – ausschließlich interner Google-Play-Test

## Gewünschte Sichtbarkeit

Nur interner Test für ausdrücklich freigegebene Google-Konten, zunächst den
Kontoinhaber. Keine Produktion, kein offener Test, keine öffentliche Freigabe.
Diese Vorgabe gilt auch für später einzeln hinzukommende Apps. Ein persönliches
Entwicklerkonto allein macht App-Releases nicht privat; entscheidend sind der
interne Track und die eingeschränkte Tester-Liste.

## Lokaler Kandidat 20 / 31

- Application-ID `de.parip69.homesprachsteuerung`, ursprünglicher projectKey und
  Alias `homevoice`; Identität gemäß `AGENTS.md`.
- compileSdk/targetSdk 36 für die aktuellen Einreichungsanforderungen,
  minSdk unverändert 26. App-Code und Berechtigungen unverändert.
- `assembleDebug bundleRelease testDebugUnitTest lintDebug signingReport`
  erfolgreich mit JDK 17, SDK Platform 36 und dem vorhandenen Gradle 8.11.1.
  Die bestehenden Nutzeränderungen am Gradle-Wrapper bleiben unberührt.
- 92 JVM-Tests bestanden; Lint 66 Warnungen, keine Fehler. Beide APK-Signaturen
  stimmen; AAB-Zertifikat und sämtliche 914 Nutzdateien des signierten Bundles
  kryptografisch geprüft; `bundletool validate` bestanden.
- APK und AAB werden lokal unter `app/build/outputs/play-internal/Alfonso-20/`
  mit den Namen `Alfonso-20.apk` und `Alfonso-20.aab` bereitgehalten.
  `PRUEFUNG.txt` enthält Versionen und SHA-256 der Dateien, keine Zugangsdaten.
- Keine Installation von 20 am Handy, kein Play-Upload, keine Veröffentlichung,
  kein Git-Push. Die öffentlichen Metadaten und APKs unter `downloads/` bleiben
  unverändert. Dort bleibt Alfonso 19 das bisherige Updateziel.

Der vorhandene lokale Kandidat stammt aus der Quellbasis `a359478` mit den
dokumentierten Änderungen. Er wird bei erfolgreicher erneuter Prüfung
unverändert verwendet. Ein gemeinsamer, manuell gestarteter Signierungsworkflow
unter `.github/workflows/signed-android.yml` baut denselben explizit angegebenen
hochgeladenen Quellcommit mit dem vorhandenen Schlüssel aus geschützten Secrets.
Die Prüfschritte lassen sich unter Windows ebenfalls ausführen:
`python scripts/verify-play-artifacts.py <bundletool.jar> <release.aab> <release.apk>`.
Ein erfolgreicher Cloudbuild wird erst nach seinem tatsächlichen Abschluss bestätigt.

## Bestätigter Schlüsselimport

Laut Übergabe des Eigentümers vom 04.11.2026 ist der ursprüngliche
App-Signierschlüssel in Google Play nach dem PEPK-Import „In Verwendung“.
Der dort angezeigte SHA-256-Fingerabdruck stimmt mit `signing/SIGNATUR.md`
überein. Damit ist die zuvor festgestellte Signaturabweichung behoben.
Upload und interne Veröffentlichung des AAB sowie das Update über Play
sind weiterhin gesondert nachzuweisen. Tester bleibt ausschließlich das
vom Eigentümer freigegebene Konto; keine Produktion und kein offener Test.

## Vor der ersten Play-Freigabe

1. App-Entwurf Alfonso, Standardsprache Deutsch, Typ App, kostenlos anlegen.
2. In Play App Signing den **vorhandenen App-Signierschlüssel** sicher übernehmen.
   Die automatische Erzeugung eines anderen App-Schlüssels nicht bestätigen.
   App-Signatur und Upload-Signatur sind verschiedene Eigenschaften; die
   ausgelieferte APK muss weiterhin das Originalzertifikat aus AGENTS.md tragen.
   Schlüsselübertragung nur im geschützten Google-Import, niemals als Chat-Anhang,
   öffentliche Repository-Datei oder Bestandteil der APK/AAB.
3. Android-Auto-Unterstützung und internen Testtrack konfigurieren, AAB hochladen,
   tatsächliche Rückmeldungen der Play Console prüfen. Play-Annahme ist noch offen.
   Vorhandene Updatefunktion oder Berechtigungen nicht vorsorglich entfernen.
4. Tester-Liste auf die gewünschten Konten beschränken; keine Produktion oder
   offenen Tests aktivieren. Testversion nur in diesem internen Track freigeben.
5. Play-Testlink am Handy mit dem freigegebenen Konto öffnen und als Update
   installieren. Vorher Originalsignatur und höheren versionCode prüfen;
   keine Deinstallation oder Datenlöschung.
6. Installationsweg, Version und Android-Auto-Launcher erneut per adb prüfen.
   Anschließend echten Fahrzeugtest durchführen. Keine Hausbefehle für den
   reinen Sichtbarkeits-/Starttest; Sprache und Versand bleiben separat zu prüfen.

Internal App Sharing ersetzt diesen Weg nicht: Google signiert diese Downloads
mit einem eigenen Sharing-Schlüssel. Das passt nicht zur hier vorgeschriebenen
ursprünglichen Update-Signatur.

## Konto und Bedienung

Am 04.11.2026 wurden Identitäts- und Android-Geräteaufgabe als abgeschlossen
beobachtet; Händlerstatus „Kein Händler“ und „Telefonnummer bestätigt“ gelesen.
Die vollständige Freigabe von „App erstellen“ auf einer frisch geladenen
Startseite ist noch zu prüfen. Personenbezogene Daten und Bestätigungscodes
werden hier nicht dokumentiert.

Ein direkter kombinierter adb-Navigations-/Projektprüfaufruf wurde mit
`blocked by policy` abgewiesen. Danach wurden reine Projektprüfungen getrennt
ausgeführt und die vorhandene Oberfläche über die sichtbare App-Navigation
geprüft. Bei nicht zuverlässig erreichbarer Weboberfläche ist der nächste
manuelle Schritt, in Chrome die Play-Console-Startseite zu öffnen.

## Offizielle Quellen

- https://support.google.com/googleplay/android-developer/answer/9845334?hl=de
- https://support.google.com/googleplay/android-developer/answer/11926878?hl=de
- https://developer.android.com/about/versions/16/behavior-changes-16
- https://developer.android.com/studio/publish/app-signing
- https://developer.android.com/training/cars/testing
- https://support.google.com/googleplay/android-developer/answer/9844679
