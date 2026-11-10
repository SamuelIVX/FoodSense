# FoodSense

FoodSense is a modern Java desktop application that scans grocery barcodes to display nutrition data, ingredients, allergen warnings, and official Nutri-Score ratings via the Open Food Facts API.

## Features

- **In-App Live Webcam Scanner:** Built-in modal dialog with illuminated corner reticles, animated scan guide, and automatic barcode detection.
- **Modern Adaptive Theme:** Sleek desktop interface built with FlatLaf, supporting instant Light and Dark mode toggling via an icon button.
- **Nutri-Score Spectrum Bar:** Displays the recognized European 5-pill indicator (A–E) with the active grade elevated and highlighted.
- **Nutrition Facts & Macro Visualizer:** Hero calorie callout, visual macronutrient distribution bar (Carbs, Fats, Protein), and per-100g nutrient table.
- **Allergen Alert Banners:** Highlighted alert cards parsing allergens with clear warning iconography.
- **Hardened Security & Input Validation:** Strict barcode sanitization, Swing HTML injection defenses, bounded async image loading, and SSRF prevention.
- **Headless CLI Mode:** Fast barcode query via `--barcode <code>` / `-b <code>` for headless terminal use without initializing GUI components.

## Tech Stack

- **Java 25 / Swing:** Core desktop application and GUI
- **FlatLaf:** Modern cross-platform Look and Feel with Retina HiDPI scaling
- **JavaCV (OpenCV):** Webcam capture and computer vision
- **ZXing:** Barcode decoding
- **Gson:** JSON deserialization

## Deployment & Execution

Prebuilt native binaries are published on the [Releases](https://github.com/SamuelIVX/FoodSense/releases) page for macOS (Apple Silicon ARM64), Windows (x64), and Linux (x64).

Requires **Java 25+** installed on your system.

```bash
java -jar FoodSense-macOS-ARM64.jar
# or FoodSense-Windows-X64.jar / FoodSense-Linux-X64.jar
```

### Release Verification

Every release publishes cryptographic SHA-256 checksums and GitHub build provenance attestations. To verify your downloaded artifact integrity:

**macOS / Linux:**
```bash
shasum -a 256 -c FoodSense-macOS-ARM64.jar.sha256
```

**Windows (PowerShell):**
```powershell
Get-FileHash .\FoodSense-Windows-X64.jar -Algorithm SHA256
```

*Note for macOS users:* When launching via terminal, ensure your terminal application (Terminal.app, iTerm) has Camera permissions granted in `System Settings > Privacy & Security > Camera` for live barcode scanning.

## Building from Source

```bash
git clone https://github.com/SamuelIVX/FoodSense.git
cd FoodSense
mvn clean package
```

The build produces a self-contained fat JAR in `target/` using `maven-shade-plugin`.

### Running Locally

Interactive GUI:
```bash
mvn compile exec:java -Dexec.mainClass="com.foodsense.FoodSense"
```

Headless CLI search:
```bash
mvn compile exec:java -Dexec.mainClass="com.foodsense.FoodSense" -Dexec.args="-b 0049000006346"
```

Override the default API host (`world.openfoodfacts.org`):
```bash
mvn compile exec:java -Dexec.mainClass="com.foodsense.FoodSense" -Dfoodsense.host="staging.openfoodfacts.org" -Dexec.args="-b 0049000006346"
```

## Project Structure

```text
src/main/java/com/foodsense/
├── FoodSense.java                  # Main entry point (CLI springboard + GUI launcher)
├── ProductApiClient.java           # API client and host resolver
├── FoodSenseGUI.java               # Main desktop window & search orchestrator
├── VideoProcessor.java             # Webcam capture & barcode detection pipeline
├── Product.java                    # Product data model
├── Nutriments.java                 # Nutrition data model
├── ApiResponse.java                # API response wrapper
├── security/
│   ├── SecurityValidator.java      # Barcode regex, HTML escaping, and HTTPS URL checks
│   └── SafeImageLoader.java        # Bounded async image fetcher (5MB cap, timeouts)
└── ui/
    ├── FoodSenseTheme.java         # Design tokens, palette scales, and Light/Dark toggle
    ├── CameraScanDialog.java       # In-app modal camera scanner with viewfinder reticles
    ├── NutriScoreSpectrumView.java # 5-pill Nutri-Score spectrum (A-B-C-D-E)
    ├── NutritionLabelPanel.java    # Nutrition card with hero calories & macro proportion bar
    └── AllergenBanner.java         # Danger alert card for allergen warnings
```

## Troubleshooting

- **Camera Permissions:** Ensure your OS camera permissions allow the terminal or Java process to access video device 0.
- **Barcode Validation:** Barcodes must be 3–48 alphanumeric characters. Special characters and spaces are rejected.
- **Product Not Found:** Products must be cataloged in Open Food Facts. Check the barcode on `world.openfoodfacts.org`.
