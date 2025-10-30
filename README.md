# FoodSense

FoodSense is a Java Swing desktop app for scanning grocery barcodes and instantly viewing product nutrition. It supports live barcode scanning via your webcam or manual barcode entry, fetches product data from the Open Food Facts API, and displays product name, brand, Nutri-Score, ingredients, and a scrollable grid of nutrition facts. Responses are parsed with Gson into POJOs and rendered in a clean, card-based UI, including product imagery when available.

## Features

- **Live webcam barcode scanning**: Uses JavaCV/OpenCV to access your camera and ZXing to decode barcodes, with a visible bounding box overlay when a code is detected.
- **Manual barcode search**: Type or paste a barcode and search without the camera.
- **Open Food Facts integration**: Pulls product info, nutrition, ingredients, and images from the public API.
- **Nutri-Score highlighting**: Prominent grade display with color-coding (A–E).
- **Auto-generated nutrition grid**: Reflectively renders available `Nutriments` fields into cards.

## Tech Stack

- **Language/Runtime**: Java 25
- **UI**: Swing
- **Computer Vision / Camera**: JavaCV (OpenCV)
- **Barcode decoding**: ZXing
- **JSON**: Gson
- **Build**: Maven
- **HTTP**: Java HttpClient (JDK)

## Architecture Overview

- `FoodSense` — App entrypoint; boots the GUI.
- `FoodSenseGUI` — Swing UI: search bar, buttons, result panels, API requests, and rendering.
- `VideoProcessor` — Camera producer/consumer pipeline using JavaCV; decodes frames via ZXing and calls back with detected barcode text.
- `Product`, `Nutriments`, `ApiResponse` — Data models mapped from the Open Food Facts API via Gson.

## Requirements

- JDK 25
- Maven 3.9+
- Internet connectivity (for the Open Food Facts API)
- A working webcam (for live scanning)

## Setup

```bash
git clone https://github.com/<your-username>/FoodSense.git
cd FoodSense
mvn clean package
```

This resolves dependencies:

- `org.bytedeco:javacv-platform` (OpenCV bindings for camera access)
- `com.google.zxing:javase` (barcode decoding)
- `com.google.code.gson:gson` (JSON parsing)

## Running

### Option 1: Run from your IDE (recommended)

1. Open the project as a Maven project.
2. Ensure the project SDK is set to JDK 25.
3. Run the `FoodSense` class.

### Option 2: Run from the command line

After `mvn package`, you can run compiled classes directly:

```bash
java --class-path target/classes com.foodsense.FoodSense
```

Note: The standard Java entrypoint requires `public static void main(String[] args)`. If you encounter issues launching via CLI or a non-executable JAR, prefer running via your IDE, or configure the Maven Shade/Assembly plugin with a `Main-Class` manifest to produce an executable JAR.

## Usage

- Click "Scan w/ Camera" to open the webcam window and point at a barcode. When detected, the app fills the barcode field and searches automatically.
- Or enter a numeric barcode into the text field and click "Search".
- The result view shows product image, brand, Nutri-Score (A–E), a nutrition facts grid, and ingredients.

## API

- Data is fetched from Open Food Facts: [`https://world.openfoodfacts.net/api/v2/product/{barcode}`](https://world.openfoodfacts.net/api/v2/product/)

## Troubleshooting

- **Camera not found or black window**
  - Close other apps using the camera.
  - Check OS permissions to allow Java to access the camera.
  - Ensure JavaCV/OpenCV native libs are compatible with your OS/CPU.

- **No barcode detected**
  - Ensure the code is within the frame and well-lit.
  - Try moving the barcode closer/farther to improve focus.

- **Blank UI or no results**
  - Verify internet connectivity.
  - The product may not exist in the database; try another barcode.

- **Build or runtime errors related to Java version**
  - Confirm you’re using JDK 25 (`java -version`). Update IDE project SDK.

## Security & Privacy

- The app uses your webcam for local barcode scanning; frames are processed on-device. Only the barcode value is used for an API request.
