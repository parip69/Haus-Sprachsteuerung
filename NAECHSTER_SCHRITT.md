# Gewünschter eigener Hintergrund-Sprachmodus – noch nicht implementiert

Stand 1.0.7: Ein Launcher-Eintrag „Jarvis“ startet die einzelne Hintergrundaufnahme.
Die manuelle Übersicht ist über langes Drücken auf das gleiche Symbol → Verlauf
öffnen erreichbar. Keine getrennten App-Namen mehr. Der Google-Aufruf „öffne
Jarvis“ muss am Samsung getestet werden. Der Fibaro-Versand wurde bereits in
einer früheren Version auf Gerhards Handy erfolgreich getestet.


Der folgende Modus ist gewünscht und erfordert eine zusätzliche Erkennungsengine:

- Eingabefeld für mehrere Aktivierungssätze, ein Satz pro Zeile, beispielsweise
  „Hauspilot“, „Jarvis“ oder weitere selbst gewählte Formulierungen.
- Expliziter Start-/Stoppschalter für das Zuhören im Hintergrund, sichtbare
  Mikrofon-Benachrichtigung und Abbruchmöglichkeit.
- Nach erkanntem Aktivierungssatz den folgenden Befehl erfassen und an den
  bestehenden Verlauf-/Fibaro-/MQTT-Weg übergeben. Aktivierungssatz nicht mitsenden.
- Keine Interpretation von Geräteaktionen in Google/Gemini; keine automatischen
  Wiederholungen fehlgeschlagener Netzwerkbefehle.
- Eigene Erkennung muss zwischen Aktivierungswort im selben Satz und einer
  separaten anschließenden Äußerung unterscheiden. Auslöser dürfen nicht durch
  simple Teilworttreffer mitten in einem anderen Wort ausgelöst werden.
- Manuelle Bedienung bleibt unabhängig verfügbar. Hintergrund-Mikrofon darf den
  manuellen Spracherkennungsdienst nicht gleichzeitig blockieren.
- Erste technische Option: lokale deutsche Spracherkennung mit Vosk und geeignetem
  Modell; Modellgröße, Worterkennung von „LG“, Android-Kompatibilität und Akkubedarf
  vor Einführung auf dem Zielgerät prüfen. Alternativ benötigt sparsame Wakeword-
  Erkennung trainierte Modelle statt beliebig eingetippter Aktivierungswörter.

Eine Integration kann erst nach Implementierung und Gerätetest als funktionsfähig
bezeichnet werden. 1.0.7 enthält diesen dauerhaften Aktivierungswort-Modus und das Eingabefeld noch nicht.
