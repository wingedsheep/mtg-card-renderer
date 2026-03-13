# MTG Card Renderer

# Default: list available commands
default:
    @just --list

# ── Development ──────────────────────────────────────────────

# Run the application (starts on port 8080)
run:
    ./gradlew app:bootRun

# Build the project
build:
    ./gradlew app:bootJar

# Compile only (fast check)
compile:
    ./gradlew app:compileKotlin

# Clean all build outputs
clean:
    ./gradlew clean

# Run all checks and tests
check:
    ./gradlew check

# ── Playwright ───────────────────────────────────────────────

# Install Playwright Chromium browser
install-browser:
    npx playwright install chromium

# ── Testing ──────────────────────────────────────────────────

# Render example card as PNG (server must be running)
render-example format="PNG" scale="3":
    curl -s -X POST 'http://localhost:8080/api/render?format={{format}}&scale={{scale}}' \
      -H "Content-Type: application/json" \
      -d '{"name":"Lightning Bolt","mana_cost":"{R}","type_line":"Instant","oracle_text":"Lightning Bolt deals 3 damage to any target.","colors":["R"],"set":"lea","collector_number":"161","rarity":"common","artist":"Christopher Rush","lang":"en"}' \
      -o card.{{lowercase(format)}} && echo "Saved card.{{lowercase(format)}}"

# Render a card from a JSON file (server must be running)
render-file file format="PNG" scale="3":
    curl -s -X POST 'http://localhost:8080/api/render?format={{format}}&scale={{scale}}' \
      -H "Content-Type: application/json" \
      -d @{{file}} \
      -o card.{{lowercase(format)}} && echo "Saved card.{{lowercase(format)}}"

# Open Swagger UI in browser (server must be running)
swagger:
    open http://localhost:8080/swagger-ui.html
