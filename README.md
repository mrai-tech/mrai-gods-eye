# God's Eye — Mrai

A light Android heart for **God's Eye View** (Bilawal Sidhu's open-source 3D globe).
The heavy body lives elsewhere — on a server / GitHub Pages — the phone app is just
a clean, glassy shell around it. No 3D engine is bundled in the APK.

- **Web body:** God's Eye View v0.3.0, built from upstream on GitHub Actions and
  deployed to GitHub Pages by `.github/workflows/deploy-web.yml`.
- **Android heart:** native Kotlin + WebView, iOS-feel glass UI, motion, splash.
  Built by `.github/workflows/build-apk.yml` — no local Android SDK needed.
- **Default server:** `https://mrai-tech.github.io/mrai-gods-eye/` — change it
  in-app (gear icon) to any other server URL, e.g. a tunnel to a local dev
  server at `http://127.0.0.1:4173` while testing on the same network.

## Build the APK (through git)

1. Push this repo to GitHub (owner `mrai-tech` by default in the workflows).
2. Go to **Actions → Build APK → Run workflow** (or just push — it runs on push).
3. Download the `gods-eye-debug-apk` artifact. Install it on Android.
   Debug APK is installable as-is, no signing key needed for testing.

To make a signed release later, add a keystore as GitHub secrets and switch
the workflow to `assembleRelease`.

## Deploy the web body

**Actions → Deploy web to Pages → Run workflow.** It clones upstream
`bilawalsidhu/gods-eye-view` at v0.3.0, runs `npm ci` + `npm run build`,
and publishes `dist/` to GitHub Pages. Enable Pages in repo Settings
(Source: GitHub Actions) once.

## The app

- Splash: animated orb, glass loading pill, fades into the globe.
- Top glass pill: GOD'S EYE title, live UTC clock, live dot.
- Bottom glass dock: Home (clean embed globe), Layers (full web UI with its
  own layer panel), Fullscreen (hide the glass chrome), Share, Settings.
- Settings: change the server URL any time. It is saved on the phone.
- WebView runs in `?embed=1` mode for a clean globe, full-screen in the app.

## Credits

God's Eye View is MIT-licensed, by Bilawal Sidhu / Halfpixel. This app is a
thin client around it — it does not bundle or re-license the upstream code.
Live data comes from public feeds (OpenSky, USGS, CelesTrak, public cameras).
Not for navigation or safety-critical use.
