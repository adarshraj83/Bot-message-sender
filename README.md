# Ghanta's Embed Sender (Android)

A phone-first Android app for sending Discord bot messages, gold embeds, and GIF/file attachments directly to a Discord channel.

## Features
- Bot token field (password-masked).
- Optional local token persistence encrypted with Android Keystore AES/GCM.
- Numeric channel ID field.
- Normal message or rich embed with a custom colour picker (hue slider, shade area, hex input, and RGB readout); defaults to Ghanta’s Gold (`#FFD43B`).
- Multiple GIF attachments; for embeds, the first attachment is used as the embed image.
- Offline check before sending.
- Connect/read timeouts and explicit 429 rate-limit messaging.
- Avoids automatic retries after a request starts because a timeout can mean Discord accepted it; this prevents accidental duplicate messages.
- Token is sent directly from the phone to Discord's API; no intermediary server is used by this app.

## Build from a phone using GitHub Actions
1. Create a private GitHub repository (recommended) and upload the contents of this folder.
2. Open the repository's **Actions** tab and enable Actions if asked.
3. Run **Build Ghanta's Embed Sender APK** (or push to `main`).
4. Open the completed workflow run, scroll to **Artifacts**, and download `Ghantas-Embed-Sender-debug-apk`.
5. Extract the ZIP and install `app-debug.apk` on your Android phone. Android may ask permission to install apps from your browser/file manager.

No bot token is needed to build the APK. Enter the token only inside the installed app. Keep the repository private if you don't want to publish the source.

## Discord permissions
The bot needs View Channel and Send Messages. For GIFs/files it also needs Attach Files; embeds need Embed Links. It must be in the server and have access to the chosen channel.

## Safety notes
- Never share the bot token, APK, or screenshots that expose the token. Revoke/regenerate the token in the Discord Developer Portal if it leaks.
- Local token saving uses Android Keystore-backed AES/GCM encryption. It is not a guarantee against a compromised/rooted device.
- If the request times out after upload begins, check the target channel before manually retrying. The app deliberately does not blindly resend uncertain POST requests, since that can create duplicate messages.
- Offline sends are not queued automatically; reconnect and tap Send. This avoids an unattended message being sent later to the wrong channel.
