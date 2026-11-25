# Courier's Roost

No message arrives instantly anymore. Every chat message is carried by a courier chicken that takes off from the sender, flies across the world and lands at the recipient's feet. Distance is latency.

**Server-side only.** Players join with a vanilla client. No client mod, no resource pack.

## How it works

- Global chat, `/msg` `/w` `/tell` and `/teammsg` `/tm` are all delivered by bird.
- Each bird spawns at the sender, climbs, flies toward the recipient's current position and descends to them. When it lands, the message appears with a small burst of colored sparks and a chime.
- The recipient keeps moving? The bird follows them.
- Delivery time = base delay + distance / speed. With the defaults (8 s base, 25 blocks/s) a player 500 blocks away waits about 28 seconds.
- Birds fly slower in the Nether, in rain and in thunderstorms.
- Cross-dimension messages: the bird appears at the destination dimension's origin and flies from there, or, with coordinate conversion on, at the scaled Nether/Overworld position.
- Birds can occasionally die on the way. The sender is told the message was lost. An optional silent-loss chance is also available.
- Birds in flight survive server restarts.
- Couriers are invulnerable, never lay eggs and give up after their lifetime runs out.

## Whispering to several players

`/msg` accepts a comma-separated list of names, with tab completion for each name:

```
/msg Alex,Steve,Notch Meet at the village
```

Each recipient gets their own bird.

## Commands

| Command | Permission | Description |
| --- | --- | --- |
| `/birdconfig` | 4 | Opens a settings screen (vanilla dialog on 1.21.6+, clickable chat menu on older versions) |
| `/birdconfig get <key>` | 4 | Read a value |
| `/birdconfig set <key> <value>` | 4 | Change a value, applied immediately |
| `/birdconfig reload` | 4 | Reload `config/courierpost.json` |
| `/birds` | 4 | List every bird in flight |
| `/birdtrack` | 4 | List the birds you sent |

## Configuration

`config/courierpost.json`

| Key | Default | Meaning |
| --- | --- | --- |
| `base_dimension_delay` | 8 | Seconds added to every message |
| `bird_speed` | 25.0 | Blocks per second |
| `nether_speed` | 16.0 | Blocks per second in the Nether |
| `speed_variation` | 0.2 | Random speed spread per bird (0.2 = ±20%) |
| `rain_multiplier` | 0.75 | Speed multiplier in rain |
| `thunder_multiplier` | 0.5 | Speed multiplier in thunderstorms |
| `bird_death_chance` | 0.002 | Chance per flight that the bird dies and the sender is notified |
| `lost_chance` | 0.0 | Chance the message disappears silently |
| `use_coordinate_conversion` | false | Scale coordinates between Nether and Overworld instead of starting at the origin |
| `deliver_offline_messages` | false | Keep birds for recipients who log out instead of discarding them |
| `save_birds` | true | Save birds in flight across restarts |
| `bird_max_lifetime` | 4800 | Ticks before an undelivered bird gives up |
| `show_bird` | true | Spawn visible chicken entities |
| `global_chat_cooldown` | 10 | Seconds between global messages (operators exempt) |

### Large servers

Simulating the birds costs almost nothing. What costs TPS is vanilla entity tracking when hundreds of visible chickens are near many players. On a busy server, run `/birdconfig set show_bird false`. Delivery keeps working exactly the same, only the entities are hidden.

## Languages

English, Korean and Japanese, chosen per player from their client language. The mod's advancement is also localized per player.

## Supported versions

| Minecraft | Java | Settings screen | Chicken variants |
| --- | --- | --- | --- |
| 26.1.2, 26.2, 26.3 | 25 | Dialog | Temperate, warm, cold |
| 1.21.11 | 21 | Dialog | Temperate, warm, cold |
| 1.21.1 | 21 | Chat menu | Standard chicken |
| 1.20.1 | 17 | Chat menu | Standard chicken |

Requires Fabric API. Download the file that matches your server's Minecraft version.

---

The slow-messaging idea is inspired by the Roost app. This project is unofficial and is not affiliated with or endorsed by Roost.
