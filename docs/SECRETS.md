# Secrets & Security Guide

This document describes how to configure, protect, and rotate credentials stored in `plugins/ArgusAC/secrets.yml`.

---

## 1. Security Philosophy

Never place API keys, Discord bot tokens, or database passwords in `config.yml`.
By isolating secrets into `secrets.yml`:
- You can safely commit or share your `config.yml`, `messages.yml`, and `discord.yml` with staff or community forums without exposing sensitive tokens.
- Add `secrets.yml` to your server's `.gitignore` if managing server directories under version control.

---

## 2. Acquiring AI Provider API Keys

ArgusAC supports multiple AI providers with automatic failover:

### A. Groq Cloud (Recommended for Ultra-Fast Inference)
1. Navigate to [Groq Console](https://console.groq.com).
2. Create an account and click **API Keys**.
3. Generate a new key and copy the `gsk_...` string.
4. Paste it into `secrets.yml`:
   ```yaml
   ai:
     groq-api-key: "gsk_your_groq_key_here"
   ```

### B. OpenRouter (Adaptive Model Hub)
1. Navigate to [OpenRouter Keys](https://openrouter.ai/keys).
2. Create an account and generate an API key (`sk-or-...`).
3. OpenRouter allows accessing dozens of models with flexible credits.
4. Paste it into `secrets.yml`:
   ```yaml
   ai:
     openrouter-api-key: "sk-or-your_openrouter_key_here"
   ```

### C. Google Gemini (Google AI Studio)
1. Navigate to [Google AI Studio](https://aistudio.google.com/app/apikey).
2. Create a free Gemini API key (`AIzaSy...`).
3. Paste it into `secrets.yml`:
   ```yaml
   ai:
     gemini-api-key: "AIzaSy_your_gemini_key_here"
   ```

---

## 3. Discord Bot Token Setup

1. Open the [Discord Developer Portal](https://discord.com/developers/applications).
2. Click **New Application** and give it a name (e.g. `ArgusAC Sentinel`).
3. Go to the **Bot** tab and click **Reset Token** to generate a bot token.
4. Enable **Privileged Gateway Intents**:
   - `Server Members Intent` -> Enabled
   - `Message Content Intent` -> Enabled
5. Paste the token into `secrets.yml`:
   ```yaml
   discord:
     bot-token: "MTA3..."
   ```

---

## 4. Credential Rotation & Zero-Downtime Hot Reload

If an API key expires or is rotated:
1. Edit `plugins/ArgusAC/secrets.yml` with the new key.
2. In server console or game chat (with `argus.admin` permission), run:
   ```text
   argus reload
   ```
3. ArgusAC hot-swaps the memory credential reference instantly without disconnecting active players.
