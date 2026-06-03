# ShortDrama

Kumpulan provider **drama pendek** (Short Drama) untuk CloudStream 3 — via [Anichin API](https://api.anichin.bio).

## Daftar Provider

| Provider | Source |
|----------|--------|
| **DramaBox** | `dramabox` |
| **ReelShort** | `reelshort` |
| **FlickReels** | `flickreels` |
| **DramaWave** | `dramawave` |
| **GoodShort** | `goodshort` |
| **NetShort** | `netshort` |
| **iDrama** | `idrama` |
| **StardustTV** | `stardusttv` |
| **DramaBite** | `dramabite` |
| **ShortMax** | `shortmax` |

## Cara Install

### Via Repository (rekomendasi)

Tambah URL berikut di **Settings → Extensions → Add Repository**:

```
https://raw.githubusercontent.com/Gintama4141/ShortDrama/master/repo.json
```

### Via APK

Build sendiri:

```bash
git clone https://github.com/Gintama4141/ShortDrama.git
cd ShortDrama
./gradlew assembleRelease
```

APK output: `DracinProvider/build/outputs/apk/release/DracinProvider-release.apk`

Install di CloudStream via **Settings → Extensions → Install from APK**.

## Struktur Proyek

```
ShortDrama/
├── build.gradle.kts            # Root build config
├── settings.gradle.kts         # Subproject auto-include
├── gradle.properties           # Build properties
├── gradlew / gradlew.bat       # Gradle wrapper
├── gradle/wrapper/             # Wrapper JAR + properties
└── DracinProvider/             # Provider module (10 kelas)
    └── src/main/kotlin/com/shortdrama/dracin/
        └── DracinProvider.kt   # Semua provider dalam 1 file
```

## API

- **Base URL:** `https://api.anichin.bio`
- **Auth Header:** `X-API-Key: TRIAL-ANICHIN-2026`
- **Endpoints:**
  - `GET /{source}/trending` — trending
  - `GET /{source}/foryou?page={page}` — for you (paginated)
  - `GET /{source}/search?query={keyword}` — pencarian
  - `GET /{source}/detail?id={id}` — detail + daftar episode
  - `GET /{source}/episode?id={id}&ep={ep}` — stream URL

## Lisensi

MIT
