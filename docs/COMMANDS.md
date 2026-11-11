# ArgusAC Commands & Permissions Reference

ArgusAC provides a comprehensive administrative and diagnostic command suite accessible via `/argus` (aliases: `/argusac`, `/ac`, `/guardian`).

---

## 1. Command Table

| Command | Arguments | Permission | Description |
|---|---|---|---|
| `/argus panel` | None | `argus.admin` | Opens the administrative Chest GUI dashboard. |
| `/argus guide` | None | `argus.admin` | Gives an interactive in-game written setup book. |
| `/argus reload` | `[all \| ai \| discord]` | `argus.admin` | Performs zero-downtime hot reloading of configs & keys. |
| `/argus freeze` | `<player>` | `argus.mod` | Freezes a player and starts a screen-share timer. |
| `/argus unfreeze` | `<player>` | `argus.mod` | Unfreezes a frozen player. |
| `/argus alts` | `<player \| ip>` | `argus.mod` | Displays all registered accounts linked to an IP or player. |
| `/argus efficiency` | `<low \| medium \| high \| extra_high>` | `argus.admin` | Hot-swaps the active AI prompt in memory. |
| `/argus inspect` | `<player>` | `argus.mod` | Opens live floating action-bar and raycast metrics on a suspect. |
| `/argus dump` | `<player>` | `argus.mod` | Generates a formatted forensic incident report of recent flags. |
| `/argus bypass` | `<player> [duration]` | `argus.admin` | Grants temporary exemption from detection checks. |
| `/argus alerts` | None | `argus.alerts` | Toggles in-game cheat alert notifications for staff. |
| `/argus vl` | `<player>` | `argus.mod` | Views the current violation levels and buffers of a player. |
| `/argus reset` | `<player>` | `argus.admin` | Resets a player's violation levels and suspicion score to 0. |

---

## 2. Permissions Hierarchy

| Permission Node | Description | Default Role |
|---|---|---|
| `argus.admin` | Unrestricted access to all configuration, reload, and panel tools. | OP |
| `argus.mod` | Access to inspection, freeze, alts lookup, and violation reset. | OP |
| `argus.alerts` | Permission to view live in-game cheat alerts and action-bar metrics. | OP |
| `argus.bypass` | Permanent bypass from all detection checks (assign with caution). | false |
| `argus.alts.bypass` | Exempt from the primary-account alt enforcement policy. | false |

---

## 3. Interactive In-Game Shortcuts

When a cheat alert appears in staff chat:
```text
[Argus] PlayerX flagged KillAura (Type A: GCD Mismatch) [VL: 14] | [TP] [INSPECT] [FREEZE] [BAN]
```
Staff members can click directly on the chat brackets:
* `[TP]` -> Teleports staff member behind the suspect.
* `[INSPECT]` -> Engages first-person mirror and action-bar metrics.
* `[FREEZE]` -> Locks the suspect's movement and triggers Discord verification.
* `[BAN]` -> Suggests the configured ban command into the staff member's chat input.
