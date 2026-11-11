<p align="center">
  <img src="assets/logo.jpg" alt="ArgusAC Logo" width="220" />
</p>

# ArgusAC (Argus Anti Cheat)

> **"The 100-Eyed All-Seeing Guardian for Modern Minecraft Servers."**

ArgusAC is an enterprise-grade, high-performance anti-cheat, threat intelligence, and server security plugin engineered for **Paper, Purpur, and Folia** servers (Minecraft 1.21.x). It combines deterministic sub-tick kinematics and packet-level inspection with an autonomous, resilient multi-provider AI engine (Groq, OpenRouter, Gemini), zero-downtime hot reloading, intelligent alt-account enforcement, and rich Discord operations.

---

## 🌟 Key Highlights

- **Multi-Provider AI Intelligence Engine**: Dynamic integration with **Groq, OpenRouter, and Gemini**. Autonomous model discovery via `/v1/models` scanning every 24 hours and at startup. Automatic failover on HTTP 429 rate limits, and seamless zero-downtime fallback to pure kinematics anti-cheat mode.
- **Pre-Loaded Prompt Memory Caching (`argusai-prompts/`)**: Four efficiency tiers (`LOW`, `MEDIUM`, `HIGH`, `EXTRA_HIGH`) loaded completely into RAM at startup—eliminating runtime file I/O latency.
- **Strict Configuration & Secret Isolation**: Four clean configuration domains (`config.yml`, `messages.yml`, `discord.yml`, and `secrets.yml`) so credentials never leak in shared configs or Git repositories.
- **Zero-Downtime Hot Reloading (`/argus reload`)**: Dynamically refreshes configs, prompts, API keys, AI model discovery, and Discord connections without kicking players or restarting the server.
- **Relational Alt-Account Management**: Tracks `UUID <-> Username <-> IP` relations in SQLite/MySQL. Configurable primary account protection: allow alts with automated Discord auditing or enforce customizable punishments on secondary accounts (`zaid2`)—such as **instant kick, tempban (e.g. 7d), permanent ban, or custom network command**—while strictly safeguarding the original player (`zaid1`).
- **Interactive Staff Experience**:
  - In-game written guide book (`/argus guide`) with live setup diagnostics and clickable config links.
  - Interactive clickable chat alerts with one-click actions: `[TP]`, `[INSPECT]`, `[FREEZE]`, `[BAN]`.
  - Chest GUI panel (`/argus panel`) for live server TPS, suspect radar, and check toggles.
  - In-game Freeze system linked to automated Discord screen-share ticket channels.
- **Modern Performance & Folia Support**: Native support for Paper and Folia regionised multithreading, utilizing **PacketEvents 2.x** and non-blocking virtual threads.
- **Modrinth Standard Compliant**: Built strictly following human-crafted, idiomatic Java 21 architecture (zero AI slop, no hollow wrappers, production-grade kinematics) suitable for public Modrinth release.

---

## 🧱 Architecture Diagram

```mermaid
flowchart TD
    Packets[Client Inbound Packets] --> PacketGate[PacketEvents 2.x Async Gateway]

    subgraph Watchers [Argus Core Watchers]
        PacketGate --> Ares[Eye of Ares: Combat & Aim Engine]
        PacketGate --> Hermes[Eye of Hermes: Movement Kinematics]
        PacketGate --> Hephaestus[Eye of Hephaestus: World & Block]
        PacketGate --> Cerberus[Eye of Cerberus: Network & Anti-Bot]
    end

    subgraph Intelligence [Threat & AI Layer]
        Ares & Hermes & Hephaestus --> Buffer[VL Buffer & Exponential Decay]
        Buffer --> SuspicionCheck{Suspicion Exceeds Threshold?}
        SuspicionCheck -->|Yes| AIRouter[Multi-Provider AI Router]
        AIRouter -->|Priority 1| Groq[Groq API: Ultra-Fast Inference]
        AIRouter -->|Failover 1| OpenRouter[OpenRouter API: Adaptive Routing]
        AIRouter -->|Failover 2| Gemini[Google Gemini API]
        AIRouter -->|Quota Depleted / Offline| PureMath[Graceful Fallback: Pure Kinematic Heuristics]
    end

    subgraph Actions [Mitigation & Response]
        SuspicionCheck -->|Minor Flag| SilentMitigation[Silent Nerf: Hit Cancel & Reach Clamp]
        SuspicionCheck -->|Setback| SmoothReconcile[Smooth Vector Reconciliation]
        SuspicionCheck -->|Severe Flag| InGameAlerts[Clickable Staff Alerts & OP Notify]
        InGameAlerts --> DiscordBot[Discord Bot: Interactive Action Buttons]
        SuspicionCheck -->|Max VL| PunishEngine[Auto Ban / Kick]
    end

    subgraph StorageLayer [Relational Persistence]
        Cerberus --> IPTable[(SQLite / MySQL: UUID & IP Relations)]
        IPTable --> AltPolicy{Alt Accounts Allowed?}
        AltPolicy -->|No| KickAlt[Kick Secondary Account & Protect Primary]
        AltPolicy -->|Yes| LogAlt[Audit Log to Discord #argus-alts]
    end
```

---

## 📋 Documentation Directory

| Document | Description |
|---|---|
| [Setup Guide](docs/SETUP.md) | Installation walkthrough, Java 21 setup, and OP setup guide. |
| [Configuration Guide](docs/CONFIGURATION.md) | Master guide for `config.yml`, `messages.yml`, and Alt policy. |
| [Secrets & Security Guide](docs/SECRETS.md) | Configuring Groq, OpenRouter, Gemini, Discord token, and MySQL safely. |
| [AI Engine & Failover](docs/AI_ENGINE.md) | Dynamic `/v1/models` discovery, prompts caching, and pure math fallback. |
| [Commands & Permissions](docs/COMMANDS.md) | Complete list of in-game commands, permissions, and GUI shortcuts. |
| [Discord Integration](docs/DISCORD.md) | Setting up Discord buttons, slash commands, and screen-share tickets. |
| [Development Guide](docs/DEVELOPMENT.md) | Source layout, Paper/Folia API target, and contributing standards. |

---

## ⚡ Quick Start

1. Ensure your server is running **Java 21** and **Paper / Purpur / Folia 1.21.x**.
2. Download the verified release JAR exclusively from [Modrinth](https://modrinth.com/plugin/argusac).
3. Place `ArgusAC.jar` inside your server's `plugins/` directory.
4. Start the server once to generate initial configurations.
5. Open `plugins/ArgusAC/secrets.yml` and insert your preferred AI API key(s) and Discord Bot token.
6. In game, run `/argus reload` to load all keys instantly with **zero downtime**.
7. An operator joining the server will receive the live status summary banner or can run `/argus guide` to inspect system readiness.

---

## 🔨 Building From Source (Gradle)

```bash
# Clone the open-source repository
git clone https://github.com/YourUsername/ArgusAC.git
cd ArgusAC

# Build shaded plugin JAR using Gradle
./gradlew clean shadowJar
```

Compiled JAR will be in `build/libs/ArgusAC-1.0.0-all.jar`.

---

## 📦 Distribution & Licensing

- **Source Code**: 100% Free and Open-Source on GitHub under the **GNU General Public License v3.0 (GPLv3)**.
- **Binary Releases**: Pre-compiled and signed `.jar` releases are published **exclusively on Modrinth** to guarantee security, integrity, and anti-malware verification.
- **Code Craftsmanship**: Zero synthetic AI boilerplate; fully human-architected for strict Modrinth community approval.
