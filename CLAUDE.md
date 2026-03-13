# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this project does

Kotlin/Spring Boot 4 service that renders Magic: The Gathering cards from Scryfall-format JSON into PNG/JPG images. It uses Playwright (headless Chromium) to render HTML/CSS/JS card templates server-side and screenshots the result.

## Build & run commands

```bash
./gradlew app:bootRun          # Run the app (port 8080)
./gradlew app:bootJar          # Build executable JAR
./gradlew app:compileKotlin    # Compile only (fast check)
./gradlew check                # Run all checks/tests
./gradlew clean                # Clean build outputs
```

A `justfile` is available with shortcuts (`just run`, `just build`, `just compile`, etc.).

Swagger UI is at http://localhost:8080/swagger-ui.html when running.

## Architecture

**Rendering pipeline:**
1. `CardRenderController` receives Scryfall JSON via `POST /api/render`
2. `CardRenderService` opens a Playwright browser page, navigates to the static `render.html`
3. The JS function `renderCardFromJson(card)` builds the card DOM (frames, mana symbols, text)
4. After 800ms (for ResizeObserver-based text fitting), Playwright screenshots the `.mtg-card` element
5. The PNG bytes are returned directly, or converted to JPG via ImageIO

**Key modules:**
- `app/` — Spring Boot application (controller, service, model)
- `buildSrc/` — Gradle convention plugin (JDK 21, JUnit config)
- `app/src/main/resources/static/card-rendering/` — The HTML/CSS/JS renderer and all card frame/symbol assets (3000+ image files)

**Card rendering JS (`static/card-rendering/`):**
- `render.html` — Minimal page that loads the renderer, exposes `renderCardFromJson()`
- `script.js` — Card computation (`computeCardProps`), HTML builders per card type (default, planeswalker, saga), text resizing logic
- `helpers.js` — `IconsHelper` (mana symbol lookup/parsing) and `Sets` (set icon resolution)
- `style.css` — Card layout using CSS custom properties set by JS. Card size is 63.5mm × 88.9mm with `transform: scale(var(--render-scale, 2))`

**The render page overrides** `transform: none !important` so that output size is controlled purely by Playwright's `deviceScaleFactor` (default scale=3 → ~720px wide).

## Dependencies

Managed in `gradle/libs.versions.toml`. Spring Boot BOM is applied via `platform(SpringBootPlugin.BOM_COORDINATES)` in app's build.gradle.kts (not the dependency-management plugin).

## Notes

- Playwright Chromium must be installed (`npx playwright install chromium`)
- The `utils/` module directory exists but is not included in the build
- No tests exist yet
- Card assets were sourced from `/Users/vincent/Git/mtg-card-generator/card-rendering/`
