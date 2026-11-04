# Feste Signatur für Home-Sprachsteuerung

Alias: `homevoice`

SHA-256 des Zertifikats:

`C3:25:61:AF:99:BF:05:93:17:8A:31:FA:6B:5F:CE:55:A2:41:9A:D1:21:18:49:99:C6:EF:D2:DF:69:18:B5:64`

Schlüssel: `home-sprachsteuerung.jks` (RSA 3072 Bit, JKS).
Passwörter: `signing.properties`. Beide Dateien bleiben unverändert im lokalen
Projekt, werden aber nicht öffentlich versioniert. Debug- und
Release-Builds verwenden dieselbe feste Identität, auch in VS Code.

Diese Dateien nicht neu erzeugen oder austauschen. Vor einem Update muss
zusätzlich der applicationId gleich bleiben und der versionCode steigen.
Die Erstversion ist versionCode 1 / versionName 1.0.0.

Für Play-Store-Verteilung muss auch Play App Signing denselben vorhandenen
App-Signierschlüssel verwenden. Keinen neuen Google-Schlüssel erzeugen lassen.
Der App-Signaturfingerabdruck in der Play Console und auf der ausgelieferten APK
muss dem oben festgelegten Wert entsprechen. Ein übereinstimmendes
Upload-Zertifikat allein genügt nicht. Laut Bestätigung des Eigentümers vom
04.11.2026 wurde der verschlüsselte PEPK-Export in Google Play übernommen;
der ursprüngliche App-Signierschlüssel ist dort mit diesem Fingerabdruck
„In Verwendung“. AAB-Upload, interne Veröffentlichung und Play-Installation
sind davon getrennte Prüfungen.

## Gemeinsamer Build für Windows und Codex Cloud

Der manuell gestartete Workflow `.github/workflows/signed-android.yml` baut
und prüft einen ausdrücklich angegebenen, bereits hochgeladenen Quellcommit.
Er stellt denselben Originalschlüssel über die Repository-Secrets
`ANDROID_KEYSTORE_BASE64` und `ANDROID_SIGNING_PROPERTIES` bereit.
Kein neuer Keystore, kein anderer Debug-Key. APK und AAB werden auf dem Runner
geprüft; nur das Prüfprotokoll mit Commit und Prüfsummen wird in der
Build-Zusammenfassung ausgegeben. Keine öffentliche Binärverteilung durch
diesen Workflow, keine Play-Veröffentlichung und keine Änderung der
öffentlichen Repository-Updates.

Die Dateien waren zuvor trotz öffentlichen Repositorys in Git versioniert.
Das Entfernen aus dem aktuellen Stand beseitigt sie nicht aus älteren Commits;
die ursprüngliche Vertraulichkeit des Schlüssels ist dadurch nicht wiederhergestellt.
Keine Historie umschreiben und keinen Schlüssel ohne gesonderte Abstimmung ersetzen.
