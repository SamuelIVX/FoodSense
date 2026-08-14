# FoodSense

**Barcode Scanner & Nutrition Analyzer**

> "Know what's in your food, instantly."

A Java desktop application that scans product barcodes and instantly displays comprehensive nutrition information, ingredients, and Nutri-Score ratings to help you make informed food choices.

## Features

- 📷 **Live Webcam Scanning** - Real-time barcode detection using your camera
- ⌨️ **Manual Barcode Entry** - Type or paste barcodes for quick searches
- 📊 **Nutrition Facts Display** - Auto-generated grid showing all nutritional values
- 🏷️ **Nutri-Score Visualization** - Color-coded health ratings (A-E)
- 🖼️ **Product Images** - Visual product identification
- 📝 **Ingredients List** - Full ingredient breakdown for each product

## Tech Stack

### Core
- **Java 25** - Programming language
- **Java Swing** - Desktop GUI framework
- **Maven** - Build automation

### Libraries
- **JavaCV (OpenCV)** - Computer vision and webcam access
- **ZXing** - Barcode decoding library
- **Gson** - JSON parsing

### APIs & Services
- **Open Food Facts API** - Product database and nutrition information

## Prerequisites

- JDK 25 or higher
- Maven 3.9+
- Internet connection (for API access)
- Webcam (optional, for live scanning)

## Getting Started

### 1. Clone the Repository

```bash
git clone https://github.com/<your-username>/FoodSense.git
cd FoodSense
```

### 2. Build the Project

```bash
mvn clean package
```

This will download all dependencies including:
- JavaCV platform libraries (OpenCV bindings)
- ZXing barcode reader
- Gson JSON parser

### 3. Run the Application

**Option A: Interactive Swing GUI**

```bash
mvn compile exec:java -Dexec.mainClass="com.foodsense.FoodSense"
```

**Option B: Headless CLI Barcode Search**

```bash
mvn compile exec:java -Dexec.mainClass="com.foodsense.FoodSense" -Dexec.args="-b 0049000006346"
```

**Option C: Custom Host Configuration**

Default host is `world.openfoodfacts.org`. Override host via System Property or Environment Variable:

```bash
# System Property override:
mvn compile exec:java -Dexec.mainClass="com.foodsense.FoodSense" -Dfoodsense.host="staging.openfoodfacts.org" -Dexec.args="-b 0049000006346"
# Environment Variable override:
FOODSENSE_HOST=world.openfoodfacts.org java -cp target/classes:$(mvn dependency:build-classpath | grep -v '\[INFO\]' | tr '\n' ':') com.foodsense.FoodSense -b 0049000006346
```

## Usage

1. **Scan with Camera**: Click "Scan w/ Camera" button to open webcam window
   - Point your camera at a product barcode
   - The app will automatically detect and search when found

2. **Manual Search**: Enter a barcode number in the text field and click "Search"

3. **CLI Mode**: Run `com.foodsense.FoodSense -b <barcode>` for instant terminal output without opening a GUI window.

## Project Structure

```text
src/main/java/com/foodsense/
├── FoodSense.java          # Main entry point (CLI springboard + GUI launcher)
├── ProductApiClient.java   # Open Food Facts API client & host resolver (.org default)
├── FoodSenseGUI.java       # Swing desktop GUI renderer
├── VideoProcessor.java     # Webcam capture and barcode detection
├── Product.java            # Product data model
├── Nutriments.java         # Nutrition data model
└── ApiResponse.java        # API response wrapper
```

## Troubleshooting

**Camera Issues**
- Ensure no other applications are using the camera
- Check system permissions for camera access
- Verify JavaCV native libraries are compatible with your OS

**Barcode Not Detected**
- Ensure good lighting and the barcode is in focus
- Try adjusting distance from camera
- Make sure barcode is fully visible in frame

**Product Not Found**
- Verify internet connection
- Product may not exist in Open Food Facts database
- Try a different barcode

**Build Errors**
- Confirm JDK 25 is installed: `java -version`
- Update IDE project SDK settings
- Run `mvn clean` and rebuild

## Security & Privacy

- All barcode scanning happens locally on your device
- Only barcode numbers are sent to the Open Food Facts API
- No personal data or video footage is stored or transmitted
- Webcam access requires explicit user activation
