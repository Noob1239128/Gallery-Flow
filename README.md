# Lumina – Fotogalerie für Android

Eine schnelle, moderne Fotogalerie-App mit Jetpack Compose, Material Design 3, Wasserfall-Raster, Foto-Editor, Kollage-Studio, Kino-Diashow, EXIF-Inspektor, Farbsuche, PIN-Tresor und Papierkorb.

---

## Direkt mit Obtainium installieren (GitHub Releases)

Dieses Repository enthält einen automatischen **GitHub Actions Workflow** (`.github/workflows/release-apk.yml`), der bei jedem Push auf `main`/`master` oder bei einem neuen `v*`-Tag automatisch eine signierte Release-APK erstellt und unter **GitHub Releases** veröffentlicht.

### 1. Projekt auf GitHub hochladen
1. Erstelle ein neues (öffentliches) GitHub-Repository.
2. Lade den gesamten Projektordner hoch (inklusive des versteckten Ordners `.github/`):
   ```bash
   git init
   git add .
   git commit -m "Initial Lumina release"
   git branch -M main
   git remote add origin https://github.com/<DEIN-USER>/<DEIN-REPO>.git
   git push -u origin main
   ```
3. Öffne auf GitHub den Tab **Actions** und warte ca. 2–4 Minuten, bis der Workflow **Build & Release Lumina APK (Obtainium)** grün ist. Danach existiert automatisch ein Eintrag unter **Releases** mit `Lumina-v1.0.x.apk`.

### 2. In Obtainium hinzufügen
1. Öffne **Obtainium** auf deinem Android-Gerät und tippe auf **App hinzufügen**.
2. Füge deine Repository-URL ein:
   ```
   https://github.com/<DEIN-USER>/<DEIN-REPO>
   ```
3. Tippe auf **Hinzufügen** -> **Installieren**. Obtainium lädt automatisch die aktuelle signierte `.apk` aus deinen GitHub Releases herunter und benachrichtigt dich bei neuen Versionen!

---

## Optional: Fester Signatur-Key für nahtlose Obtainium-Updates

Ohne eigene Secrets erzeugt der Workflow automatisch einen temporären Keystore. Damit zukünftige Updates in Obtainium **direkt drüberinstalliert** werden können (ohne vorherige Deinstallation), hinterlege einmalig deinen eigenen Keystore in den GitHub Repository Secrets (**Settings -> Secrets and variables -> Actions**):

1. Keystore lokal erzeugen:
   ```bash
   keytool -genkeypair -v -keystore my-upload-key.jks -storetype PKCS12 -keyalg RSA -keysize 2048 -validity 10000 -alias upload
   base64 -w 0 my-upload-key.jks
   ```
2. Folgende **3 Repository Secrets** auf GitHub anlegen:
   - `KEYSTORE_BASE64`: Der Base64-Text deines Keystores
   - `STORE_PASSWORD`: Das Store-Passwort
   - `KEY_PASSWORD`: Das Key-Passwort
