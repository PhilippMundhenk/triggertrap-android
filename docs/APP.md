# The app

## The dongle

Triggertrap Mobile is a passive dongle: the app plays a 17 kHz tone on the headphone output,
the dongle rectifies it and closes the shutter (and focus) contacts of the camera cable for as
long as the tone plays. That is why the volume has to be at maximum, and why a phone without a
headphone jack needs a USB-C to 3.5 mm adapter with a real DAC.

The two stereo channels are driven in anti-phase for a stronger signal; the left (shutter)
channel has a short silence at the start of every 50 ms block, which Samsung NX cameras need.

Settings, *Trigger pulse length*: how long the shutter contact is closed for a single shot.
150 ms suits most cameras; raise it if the camera misses shots.

## Modes

**Cable release.** *Simple*: tap, one shot. *Quick release*: the shot fires when you let go
(steadier). *Press and hold*: the shutter stays pressed while you hold the button, for bulb
exposures. *Press and lock*: tap to press, tap to release, with a stopwatch. *Timed release*:
set a duration, the shutter stays pressed that long. *Self timer*: a countdown, then one shot.

**Timelapse.** *Timelapse*: one shot every interval, until stopped. *TimeWarp*: a timelapse
whose interval follows an easing curve you draw, so the result speeds up or slows down.
*Distance lapse*: one shot every n metres, from GPS. *Star trail*: n long exposures with a gap.
*Bramping*: n shots with the exposure ramping linearly from a start to an end value, for
sunsets and sunrises in bulb mode.

**Sound sensor.** The microphone level is shown on the dial; set the threshold (the dial) and
the sensitivity (the slider), arm with the red button. A sound above the threshold fires one
shot; *Settings, Sensor delay* delays it, *Sensor reset delay* is the minimum time between two
shots.

**HDR.** *LE HDR*: an odd number of exposures around a middle exposure, n EV apart, in bulb
mode. *LE HDR timelapse*: the same bracket repeated every interval.

**Remote.** One phone as *Wi-Fi master* (its trigger output stays unchanged) and others as
*Wi-Fi slaves* on the same network; the master finds the slaves via Bonjour and every shot it
takes is repeated on the slaves. No authentication: use on networks you trust.

**Calculators.** *Solar*: sunrise, sunset and civil twilight for the current location, with
a countdown. *ND*: the exposure a base shutter speed becomes behind an n-stop ND filter.

## Running in the background

A running mode continues when you switch to another screen (a bar at the bottom shows it, with
a Stop button) and when you leave the app (a notification with a Stop button). Android asks
for the notification permission on first start for that.

## Permissions

| Permission | Used by | Asked |
|---|---|---|
| Notifications | the running-mode notification | at first start |
| Microphone | sound sensor | when the screen opens |
| Location | distance lapse, solar calculator | when the screen opens |
| Local network | Wi-Fi master / slave | no prompt (multicast) |

## Troubleshooting

* The camera does not fire: volume at maximum? Cable fully plugged in? Camera in manual
  focus? Try a longer pulse length in the settings. Some phones apply an audio effect or
  "volume limiter" to the headphone output; disable it.
* Shots are missed in a fast timelapse: the interval must be longer than the pulse length
  (the screen shows a warning icon).
* The solar calculator shows no location: location is off, or the permission was declined
  (*Android settings, Apps, Triggertrap, Permissions*).
