# Security

## Reporting a vulnerability

Please do not open a public issue for security problems. Use GitHub's private vulnerability
reporting on this repository ("Security" tab, "Report a vulnerability"). You should get a
first response within a week.

## Notes

- The app talks to nothing on the internet. Analytics (Mixpanel, Flurry) and the Pebble and
  Google Play Services integrations of the original app were removed.
- The only network feature is Wi-Fi master / slave: a Bonjour service (`_triggertrap._tcp`)
  and a plain TCP socket on the local network that carries the trigger command. There is no
  authentication; anyone on the same LAN can trigger a master. Use it on networks you trust.
- Permissions are requested when a mode needs them: microphone (sound sensor), location
  (distance lapse, solar calculator), notifications (the running-mode notification).
- The app stores only its settings (SharedPreferences); they are included in Android backups.
