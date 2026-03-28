# Native Packaging

This project can be packaged into native desktop app images and native installers with `jpackage`.

## Windows

Run on Windows:

```powershell
powershell -ExecutionPolicy Bypass -File .\launcher\build-windows-exe.ps1
```

Output:

- `dist/ODME/ODME.exe`

Installer note:

- On Windows you can also switch the script to use `jpackage --type msi` later if you want an installer instead of an app folder.

## Linux

Run on Linux:

```bash
chmod +x launcher/build-linux-app.sh
./launcher/build-linux-app.sh
```

Output:

- `dist/ODME/bin/ODME`

Linux installers:

```bash
chmod +x launcher/build-linux-installer.sh
./launcher/build-linux-installer.sh deb
```

or

```bash
./launcher/build-linux-installer.sh rpm
```

Outputs:

- `dist/*.deb`
- `dist/*.rpm`

Common prerequisites:

- For `deb`: `dpkg-deb` and `fakeroot`
- For `rpm`: `rpmbuild`

## macOS

Run on macOS:

```bash
chmod +x launcher/build-macos-app.sh
./launcher/build-macos-app.sh
```

Output:

- `dist/ODME.app`

macOS installer:

```bash
chmod +x launcher/build-macos-dmg.sh
./launcher/build-macos-dmg.sh
```

Output:

- `dist/*.dmg`

## Notes

- Each script builds the shaded jar first with Maven.
- Each script uses a repo-local Maven cache under `build/.m2/repository`.
- `jpackage` must run on the target OS. You cannot build the macOS bundle on Windows or the Windows `.exe` on Linux/macOS.
- Keep the generated app image folder or `.app` bundle intact when distributing it.
- Unsigned macOS `.app` and `.dmg` outputs may trigger Gatekeeper warnings until you sign and notarize them.
- Linux installer naming is controlled by `jpackage` and may vary slightly by distribution and architecture.

## CI/CD

GitHub Actions now provides:

- CI on `push`/`pull_request` to `main`
- native app-image builds for Windows, Linux, and macOS as workflow artifacts
- tagged releases (`v*`) that publish Windows, Linux, and macOS release assets
- CycloneDX SBOM generation and SHA-256 checksum publishing for release assets

Create a release by pushing a tag such as:

```bash
git tag v2.0.0
git push origin v2.0.0
```

## Optional Signing Secrets

The release workflow builds unsigned artifacts by default. To enable signing and notarization, configure these GitHub repository secrets:

### Windows

- `WIN_CODESIGN_CERT_BASE64`: base64-encoded `.pfx`
- `WIN_CODESIGN_CERT_PASSWORD`: password for the `.pfx`

### macOS signing

- `MACOS_CERTIFICATE_P12`: base64-encoded Developer ID Application certificate (`.p12`)
- `MACOS_CERTIFICATE_PASSWORD`: password for the `.p12`
- `MACOS_SIGNING_IDENTITY`: full Developer ID Application identity name
- `MACOS_PACKAGE_IDENTIFIER`: bundle identifier passed to `jpackage`

### macOS notarization

- `APPLE_ID`: Apple ID email used for notarization
- `APPLE_APP_SPECIFIC_PASSWORD`: app-specific password for notary submission
- `APPLE_TEAM_ID`: Apple Developer team identifier
