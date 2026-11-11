# ArgusAC Setup & Installation Guide

This guide details the complete installation, initial setup, and production verification for **ArgusAC (Argus Anti Cheat)**.

---

## 1. System Requirements

| Component | Minimum Specification | Recommended Specification |
|---|---|---|
| **Server Engine** | Paper 1.21.x / Purpur 1.21.x | Paper / Purpur 1.21.x or **Folia 1.21.x** |
| **Java Runtime** | Java 21 | Eclipse Temurin 21 (LTS) / GraalVM 21 |
| **Memory** | 2 GB dedicated heap | 4 GB+ dedicated heap |
| **Packet Layer** | Internal Packet Gateway | **PacketEvents 2.x** (Shaded/Bundled) |
| **Storage** | Local SQLite (Built-in) | HikariCP MySQL/MariaDB for networks |

---

## 2. Step-by-Step Installation

### Step 1: Install the Plugin JAR
1. Stop your Minecraft server gracefully (`stop`).
2. Place `ArgusAC.jar` into your server's `plugins/` folder.
3. Start the server:
   ```bash
   java -Xms4G -Xmx4G -XX:+UseG1GC -jar paper-1.21.jar --nogui
   ```
4. ArgusAC will initialize, generate default configuration files, and pre-cache system prompts into memory.

### Step 2: Configure Secrets
Open `plugins/ArgusAC/secrets.yml` and add your credentials:
```yaml
ai:
  groq-api-key: "gsk_..."
  openrouter-api-key: "sk-or-..."
  gemini-api-key: "AIzaSy..."

discord:
  bot-token: "MTA3..."
```

### Step 3: Zero-Downtime Hot Reload
Instead of restarting the server, execute in the server console:
```text
argus reload
```
ArgusAC will hot-load your keys, initiate dynamic `/v1/models` discovery, and connect the Discord bot within milliseconds.

---

## 3. Operator Experience & Interactive Setup Book

### Joining as an Operator
When a player with OP or `argus.admin` permission joins the server:
- If setup is **complete**: A diagnostic status banner is displayed showing the active AI model, latency, and Discord connectivity.
- If setup is **incomplete**: An interactive warning notification appears with a clickable `[CLICK HERE]` link that opens the **Interactive Setup Book**.

### Using the Interactive Guide Book (`/argus guide`)
Any admin can run `/argus guide` at any time to receive an interactive in-game written book (`ItemStack`):
- **Page 1: System Readiness Checklist**:
  - `[✔] Core Checks: ACTIVE`
  - `[✔] Database: SQLite Connected`
  - `[✔] Prompts: 4 Profiles Cached in RAM`
  - `[✔/✖] AI Provider Status`
- **Page 2: AI Provider Quick Links**: Direct clickable links to create free API keys on Groq, OpenRouter, and Google AI Studio.
- **Page 3: Discord Bot Setup**: Instructions on creating a bot and pasting channel snowflake IDs.
- **Page 4: Quick Command Reference**: Summary of essential commands (`/argus panel`, `/argus freeze`, `/argus alts`).

---

## 4. Verification Checklist

- [ ] Java 21 confirmed (`java -version`).
- [ ] Paper / Purpur / Folia running 1.21.x.
- [ ] At least one AI API key configured in `secrets.yml`.
- [ ] Prompts loaded (`argusai-prompts/` visible in logs).
- [ ] Tested `/argus reload` without console errors.
- [ ] Confirmed `/argus panel` opens the administrative GUI.
- [ ] Verified Discord bot is online in your staff alert channel.
- [ ] Verified alt-account policy according to your server rules.
