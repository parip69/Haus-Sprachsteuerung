# Feste Signatur für Home-Sprachsteuerung

Alias: `homevoice`

SHA-256 des Zertifikats:

`C3:25:61:AF:99:BF:05:93:17:8A:31:FA:6B:5F:CE:55:A2:41:9A:D1:21:18:49:99:C6:EF:D2:DF:69:18:B5:64`

Schlüssel: `home-sprachsteuerung.jks` (RSA 3072 Bit, JKS).
Passwörter: `signing.properties`. Beide Dateien sind auf ausdrücklichen Wunsch
Teil des Projekts und für ein privates Repository vorgesehen. Debug- und
Release-Builds verwenden dieselbe feste Identität, auch in VS Code.

Diese Dateien nicht neu erzeugen oder austauschen. Vor einem Update muss
zusätzlich der applicationId gleich bleiben und der versionCode steigen.
Die Erstversion ist versionCode 1 / versionName 1.0.0.

Für Play-Store-Verteilung gelten zusätzlich die Regeln von Play App Signing;
dessen App-Signierschlüssel kann von diesem lokalen Schlüssel abweichen.
