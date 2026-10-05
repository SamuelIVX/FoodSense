# FoodSense

FoodSense is a Java desktop application that scans barcodes to display nutrition data, ingredients, and Nutri-Score ratings via the Open Food Facts API. 

## Features

- Live webcam scanning for real-time barcode detection
- Manual barcode entry for quick lookups
- Auto-generated nutrition facts grid
- Nutri-Score and product image display
- Full ingredient breakdown

## Tech Stack

- **Java 25 / Swing:** Core application and GUI
- **JavaCV (OpenCV):** Webcam capture and computer vision
- **ZXing:** Barcode decoding
- **Gson:** JSON parsing

## Deployment & Execution

You can run FoodSense directly without building from source. Download the `.jar` file for your operating system from the [Releases](https://github.com/SamuelIVX/FoodSense/releases) page. 

Requires **Java 25+** installed on your system.

```bash
java -jar FoodSense-macOS-ARM64.jar
# or FoodSense-Windows-X64.jar / FoodSense-Linux-X64.jar
```

*Note for macOS users:* The latest releases are built for Apple Silicon (ARM64). When launching via terminal, ensure your terminal application (Terminal.app, iTerm) has Camera permissions granted in `System Settings > Privacy & Security > Camera` for live barcode scanning to function.

## Building from Source

If you want to build the project yourself, clone the repository and use Maven.

```bash
git clone https://github.com/SamuelIVX/FoodSense.git
cd FoodSense
mvn clean package
```

The Maven build uses the `maven-shade-plugin` and `javacpp.platform.host` to pull native dependencies for your specific operating system.

### Running locally

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
├── FoodSense.java          # Main entry point (CLI springboard + GUI launcher)
├── ProductApiClient.java   # API client and host resolver
├── FoodSenseGUI.java       # Swing desktop renderer
├── VideoProcessor.java     # Webcam capture and barcode detection
├── Product.java            # Product data model
├── Nutriments.java         # Nutrition data model
└── ApiResponse.java        # API response wrapper
```

## Troubleshooting

**Camera Issues:**
Ensure no other applications are using the camera and check system permissions. Verify JavaCV native libraries match your OS.

**Barcode Not Detected:**
Check lighting, ensure the barcode is in focus, and make sure it is fully visible in the frame.

**Product Not Found:**
The product may not exist in the Open Food Facts database.

**Build Errors:**
Confirm JDK 25 is installed (`java -version`). Run `mvn clean` and rebuild.
