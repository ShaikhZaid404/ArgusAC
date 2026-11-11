# Contributing to ArgusAC

Thank you for your interest in contributing to **ArgusAC (Argus Anti Cheat)**! 

As an open-source project distributed on GitHub and published on Modrinth, we hold our codebase to rigorous engineering standards.

---

## 🛡️ Modrinth Quality & Human-Crafted Architecture Standard

Modrinth actively rejects projects exhibiting synthetic "AI slop" hallmarks (such as hollow docstrings, empty TODOs, unnecessary wrapper utilities, and generic copy-pasted code).

Every contribution must adhere to these rules:
1. **Idiomatic Java 21**:
   - Use `record` classes for immutable data carriers (packet wrappers, telemetry snapshots).
   - Use pattern matching for `instanceof` and `switch`.
   - Use sealed interfaces for finite domain states (e.g. `CheckResult`).
2. **Zero Synthetic Boilerplate**:
   - Do not submit artificial helper classes that wrap existing Paper API calls without domain value.
   - All comments must be meaningful and provide actual technical context.
3. **Packet & Kinematic Accuracy**:
   - Detection routines must account for sub-tick player latency, server TPS variance, and block friction modifiers.

---

## 🛠️ Development Workflow

1. Fork the repository and create your feature branch:
   ```bash
   git checkout -b feature/my-new-check
   ```
2. Build and verify locally with Gradle:
   ```bash
   ./gradlew clean shadowJar
   ```
3. Commit your changes with concise, descriptive commit messages:
   ```bash
   git commit -m "feat(combat): implement rotation GCD step sensitivity check"
   ```
4. Push to your branch and open a Pull Request.

---

## 🔒 Security Standards

**Never commit real API keys, bot tokens, or test passwords** into test fixtures or pull requests. Use mock objects or environment variables.
