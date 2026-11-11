# ArgusAC Development Guide

This guide details the technical specifications, build requirements, source code conventions, and threading rules for contributing to **ArgusAC**.

---

## 1. Technical Baseline

| Component | Target Version |
|---|---|
| **Java Platform** | Java 21 (Record classes, pattern matching, virtual threads) |
| **Server API** | Paper API 1.21 (with Folia Regionised Scheduler compatibility) |
| **Packet Interception** | **PacketEvents 2.x** (High-throughput async packet pipeline) |
| **Build Automation** | **Gradle 8.x** (`build.gradle.kts` / Kotlin DSL) + Shadow Plugin |
| **Database Pool** | HikariCP 5.x |
| **Discord Library** | JDA 5.x (Java Discord API) |
| **Source Hosting** | **GitHub** (Public Open-Source) |
| **Binary Releases** | **Modrinth** (Exclusive verified JAR distribution) |

---

## 2. Source Layout

```text
src/main/java/dev/argus/
├── ArgusPlugin.java             # Main lifecycle, scheduler abstraction & bootstrap
├── api/                         # Public API, custom events (ArgusFlagEvent, ArgusPunishEvent)
├── checks/                      # Core detection modules
│   ├── Check.java               # Abstract base check with VL buffer & decay
│   ├── CheckCategory.java       # COMBAT, MOVEMENT, WORLD, PACKET
│   ├── combat/                  # KillAura (GCD), Reach, AutoClicker, Velocity
│   ├── movement/                # Flight, Speed, Timer, Scaffold, Step, Jesus
│   ├── world/                   # FastBreak, FastPlace, Nuker, AirPlace
│   └── packet/                  # BadPackets, NettyCrashShield, WindowClick
├── engine/                      # Mathematical simulation
│   ├── kinematics/              # Client velocity, friction & acceleration prediction
│   ├── math/                    # GCD mouse sensitivity, vector dot products, raycasts
│   └── tracker/                 # Sub-tick target history buffer for lag compensation
├── intelligence/                # AI and network profiling
│   ├── ai/                      # Provider routers (Groq, OpenRouter, Gemini)
│   ├── discovery/               # Asynchronous /v1/models dynamic scanner
│   ├── prompts/                 # In-memory prompt cache loader
│   ├── antibot/                 # Handshake rate limiter & burst protection
│   └── alts/                    # Relational IP ledger & primary account enforcement
├── mitigation/                  # Silent combat nerf, hit cancellation & smooth setback
├── storage/                     # SQLite and HikariCP MySQL repositories
├── ui/                          # Written book generator, Chest GUI & clickable chat
└── discord/                     # JDA bot lifecycle, slash commands & button listeners
```

---

## 3. Concurrency & Folia Threading Rules

ArgusAC is designed to run seamlessly on modern multi-threaded Folia environments as well as traditional Paper servers:

1. **Never Block the Main or Region Thread**:
   - HTTP requests to AI providers (Groq, OpenRouter, Gemini) MUST execute via Java 21 Virtual Threads (`Executors.newVirtualThreadPerTaskExecutor()`).
   - Database operations (SQLite/MySQL queries) MUST run asynchronously.
2. **Folia Scheduler Abstraction**:
   - Do not call `Bukkit.getScheduler()` directly.
   - Use the internal `ArgusScheduler` abstraction which maps to `RegionScheduler` on Folia and standard `BukkitScheduler` on Paper.
3. **Thread-Safe State Access**:
   - Player packet histories and kinematic vectors are stored in concurrent or thread-confined ring buffers.
   - Hot-reload of configuration maps uses atomic reference swaps (`AtomicReference<ArgusConfig>`).

---

## 4. Building the Project (Gradle Kotlin DSL)

ArgusAC uses **Gradle (Kotlin DSL)** with the `com.gradleup.shadow` plugin to produce a self-contained, optimized shaded JAR:

### Prerequisites:
- Java 21 JDK (`java -version`)
- Git

### Build Commands:
```bash
# Grant execution permissions to Gradle wrapper (Linux / macOS)
chmod +x gradlew

# Build clean and package shaded plugin JAR
./gradlew clean shadowJar
```

On Windows:
```cmd
gradlew.bat clean shadowJar
```

The shaded production plugin JAR will be generated under:
```text
build/libs/ArgusAC-1.0.0-all.jar
```

---

## 5. Open-Source & Distribution Pipeline

ArgusAC follows a dual-channel distribution model:

1. **GitHub (Open-Source Repository)**:
   - Contains the complete transparent human-written source code, issue tracker, pull requests, and documentation.
   - Licensed under the open-source **GNU General Public License v3.0 (GPLv3)**.
2. **Modrinth (Exclusive Binary Distribution)**:
   - Official compiled and signed `.jar` releases are published **exclusively on Modrinth**.
   - Ensures authenticity, checksum verification, and adherence to Modrinth's strict anti-malware and human-crafted quality standards.

---

## 5. Contribution Guidelines

1. **Zero False Positives**: Every kinematic or combat check must incorporate server TPS, client ping variance, and environmental modifiers (ice, potion effects, knockback).
2. **Preserve Documentation**: Any modification to checks, configuration keys, or commands must be accompanied by updates to the relevant files in `docs/`.
3. **Security Standards**: Never commit real API keys, bot tokens, or test database passwords into test classes or codebases.

---

## 6. Modrinth Compliance & Human-Crafted Code Standards

Modrinth enforces strict quality standards and actively rejects projects exhibiting synthetic "AI slop" hallmarks (such as excessive generic boilerplate, repetitive placeholder classes, hollow docstrings, and unidiomatic architectures).

When authoring code for ArgusAC, the following standards are **strictly required**:

### A. Zero AI Hallmarks
- **NO Generic Placeholder Comments**: Never leave `// TODO: Implement later`, `// Helper method`, or hollow restatements of method names.
- **NO Useless Abstraction Layers**: Avoid creating artificial utility classes (e.g. `StringUtilHelper`, `CommonUtils`) that merely wrap standard Java or Paper methods without adding genuine domain value.
- **NO Synthetic Repetition**: Detection routines must use real mathematical formulations (GCD, Euler angles, vector kinematics) rather than synthetic copy-pasted blocks.

### B. Modern & Idiomatic Java 21 Architecture
- **Records for Data Carriers**: Use Java 21 `record` classes for immutable packet payloads, telemetry snapshots, and violation events.
- **Pattern Matching**: Utilize modern pattern matching for `switch` and `instanceof` to maintain clean, readable dispatch routines.
- **Sealed Hierarchies**: Model finite outcomes (such as `CheckResult.Legit`, `CheckResult.Flag`, `CheckResult.Setback`) using sealed interfaces.
- **Primitive Optimization on Hot Paths**: In tick-by-tick packet listeners (Aura, Reach, Fly), prefer primitive arrays or bounded ring buffers over boxing objects to avoid GC pressure.

### C. Realistic Minecraft Domain Modeling
- Code must reflect deep, real-world understanding of the Minecraft network protocol:
  - Exact tick phases (Pre-Move, Post-Move, Attack Packet Ordering).
  - Bounding box expansions and server-side raytracing taking player eye-height and sneak state into account.
  - Latency compensation using sub-tick time stamps rather than simplistic fixed distance checks.
