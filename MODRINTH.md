# Courier's Roost

Chat messages on your server get delivered by chickens. When someone sends a message, a courier chicken takes off from them and flies to the recipient, and the message shows up when it lands. Players far away from each other wait longer.

Only the server needs the mod. Players can join with an unmodded client.

## Features

- Works for global chat, `/msg`, `/w`, `/tell`, `/teammsg` and `/tm`
- The chicken follows the recipient if they move, and lands at their feet with a few sparks and a chime
- Delivery time is a base delay plus distance divided by speed. With default settings, a player 500 blocks away gets the message in about 28 seconds
- Chickens fly slower in the Nether and in bad weather
- Messages between dimensions work too
- A chicken can occasionally die on the way, and the sender gets told the message was lost (configurable, can be turned off)
- Chickens in flight are saved when the server restarts
- Couriers can't be killed and don't lay eggs

You can whisper to several players at once by separating names with commas. Tab completion works for each name:

```
/msg Alex,Steve,Notch meet at the village
```

## Commands

All commands need permission level 4.

| Command | What it does |
| --- | --- |
| `/birdconfig` | Opens the settings menu (a dialog window on 1.21.6 and newer, clickable chat on older versions) |
| `/birdconfig get <key>` | Shows a setting |
| `/birdconfig set <key> <value>` | Changes a setting right away |
| `/birdconfig reload` | Reloads `config/courierpost.json` |
| `/birds` | Lists every chicken currently flying |
| `/birdtrack` | Lists the chickens you sent |

## Config

Stored in `config/courierpost.json`.

| Key | Default | Description |
| --- | --- | --- |
| `base_dimension_delay` | 8 | Seconds added to every message |
| `bird_speed` | 25.0 | Flight speed in blocks per second |
| `nether_speed` | 16.0 | Flight speed in the Nether |
| `speed_variation` | 0.2 | Random speed difference between chickens (0.2 means up to 20%) |
| `rain_multiplier` | 0.75 | Speed multiplier while raining |
| `thunder_multiplier` | 0.5 | Speed multiplier during thunderstorms |
| `bird_death_chance` | 0.002 | Chance a chicken dies on the way (sender is notified) |
| `lost_chance` | 0.0 | Chance a message silently disappears |
| `use_coordinate_conversion` | false | Scale coordinates between the Nether and the Overworld. When off, chickens entering another dimension start from 0, 0 |
| `deliver_offline_messages` | false | Keep chickens flying to players who logged out |
| `save_birds` | true | Save chickens in flight across restarts |
| `bird_max_lifetime` | 4800 | Ticks before an undelivered chicken gives up |
| `show_bird` | true | Show the chicken entities |
| `global_chat_cooldown` | 10 | Seconds between global messages (operators are exempt) |

If you run a big server and see TPS drop with lots of messages in the air, set `show_bird` to false. Messages are still delivered with the same timing, only the chickens are hidden. Most of the cost comes from sending hundreds of chicken entities to nearby players, not from the mod itself.

## Languages

English, Korean and Japanese. Each player sees text in their own client language.

## Versions

Download the file that matches your server's Minecraft version and loader. The Fabric version needs Fabric API. The Forge version needs nothing else.

| Minecraft | Fabric | Forge |
| --- | --- | --- |
| 26.3 | yes | yes |
| 26.2 | yes | yes |
| 26.1, 26.1.1 | yes | |
| 26.1.2 | yes | yes |
| 1.21.11 | yes | yes |
| 1.21 | yes | |
| 1.21.1 | yes | yes |
| 1.20 | yes | |
| 1.20.1 | yes | yes |

Every version listed above was tested on a real server. 1.20.2 to 1.20.6 and 1.21.2 to 1.21.10 are not supported yet.

On versions older than 1.21.6 the settings menu is clickable chat instead of a dialog window, and on versions older than 1.21.5 couriers are regular chickens instead of the temperate, warm and cold variants.

---

This mod was vibe coded: written with an AI coding assistant, then tested on real servers for every supported version.

The slow-message idea was inspired by the Roost app. Courier's Roost is an independent fan project and is not affiliated with, endorsed by or connected to Roost or its developers.
