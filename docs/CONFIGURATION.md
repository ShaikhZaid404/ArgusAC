# ArgusAC Configuration Guide

ArgusAC divides configuration into dedicated modular files to maximize clarity and prevent accidental credential leaks.

---

## 1. Configuration Files Overview

```text
plugins/ArgusAC/
├── config.yml           # Core checks, physics, thresholds, alt policy
├── messages.yml         # In-game chat formats, alerts, titles, prefixes
├── discord.yml          # Discord bot, channels, roles, buttons
├── secrets.yml          # Sensitive API keys, tokens, DB passwords
└── argusai-prompts/     # Prompt definitions cached into RAM at startup
```

---

## 2. Alt-Account Enforcement Policy (`alts` block)

ArgusAC maintains an internal relational ledger mapping every player's `UUID <-> Username <-> IP Address <-> First Seen Timestamp`.

```yaml
alts:
  allowed: false
  max-alts-per-ip: 3
  punishment-action: "KICK"            # 'KICK', 'TEMPBAN', 'BAN', or 'COMMAND'
  tempban-duration: "7d"              # Applied when action is 'TEMPBAN'
  custom-command: "tempban {alt_player} 7d Alt Accounts Prohibited | ArgusAC"
  disconnect-message: "&c&lArgusAC &8» &7Alt accounts are not permitted on this server!\n&ePlease join using your primary registered account: &a{primary_account}"
  notify-primary-in-game: true
  primary-notify-message: "&e&lArgusAC &8» &7Notice: A secondary account (&c{alt_player}&7) attempted to connect from your IP and was rejected."
  discord-audit: true
```

### Behavioral Workflow:
1. **Primary Account Identification**:
   The first account to ever connect to your server from an IP address is permanently tagged as the **Primary Account** (e.g. `zaid1`).
2. **When `allowed: false`**:
   - If `zaid1` (Primary) connects: Access is granted immediately.
   - If a new account from the same IP connects (e.g. `zaid2`):
     - The configured `punishment-action` is executed against `zaid2`:
       - `KICK`: Instantly kicked with `disconnect-message`.
       - `TEMPBAN`: Banned for `tempban-duration` (e.g. `7d`, `14d`).
       - `BAN`: Permanently banned from the server.
       - `COMMAND`: Runs your network ban manager command (e.g. LiteBans, AdvancedBan).
     - `zaid1` is strictly protected and never kicked or punished.
     - If `notify-primary-in-game: true` and `zaid1` is currently online, they receive an immediate in-game alert informing them of the rejected connection attempt.
     - An audit record is logged to the Discord `#argus-alts` audit channel.
3. **When `allowed: true`**:
   - Secondary accounts are allowed up to `max-alts-per-ip`.
   - Relationships are charted in SQLite/MySQL.
   - Discord logs audit connections: `[Audit] Player zaid2 connected from IP linked to primary account zaid1`.

---

## 3. Silent Combat Mitigation (`mitigation` block)

Rather than abruptly banning suspected players on transient lag spikes, ArgusAC neutralizes their combat advantages in real-time:

```yaml
mitigation:
  silent-nerf:
    enabled: true
    cancel-hits: true             # Flagged KillAura/Reach hits do not register damage
    clamp-reach: 2.2              # Shrinks attacker reach from 3.0 to 2.2 blocks
    cancel-scaffold-blocks: true  # Drops illegal scaffold block placement packets
```

### Movement Setback Modes:
* `SMOOTH`: Predicts the player's last legitimate physical vector and reconciles client position smoothly without jerky rubberbanding.
* `INSTANT`: Hard teleport back to the last validated server location.

---

## 4. Honeypot Traps (`honeypot` block)

```yaml
honeypot:
  aura-bot:
    enabled: true
    trigger-suspicion-vl: 12
    ticks-to-live: 40
    instant-ban: true
```

When a player triggers suspicious combat patterns, ArgusAC spawns a silent, packet-level invisible entity outside their field of view. 
- Legit players never see or interact with the entity.
- Automated KillAura and AimAssist modules snap towards the entity and register an attack packet.
- Upon receiving a hit packet for this honeypot entity, ArgusAC executes an **instant 100% confirmed ban** with zero false positives.

---

## 5. Checks & Detection Thresholds

Each check under `checks:` exposes:
- `enabled`: Toggle module on or off.
- `vl-threshold`: Violation level buffer before punishment triggers.
- `punishment`: Action to execute (`setback`, `warn`, `kick`, `ban`, or custom command).

Specialized checks include:
* `combat.killaura.gcd-angle-check`: Sensitivity rotation verification against mouse GCD steps.
* `combat.autoclicker.entropy-analysis`: Statistical normal distribution modeling of click cadences.
* `movement.fly.prediction-model`: Sub-tick kinematic physics simulation.
* `movement.timer.max-clock-drift-ms`: Client clock acceleration detection.
