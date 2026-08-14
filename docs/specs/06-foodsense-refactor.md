# Spec: FoodSense Refactor — API Client Extraction, Host Default (.org), & CLI Springboard

## Objective

Refactor `FoodSenseGUI.java` by extracting Open Food Facts API network operations into a standalone, testable `ProductApiClient.java`, expanding `FoodSense.java` into a CLI springboard supporting both headless CLI barcode lookup and GUI launch, and updating the default Open Food Facts API host from `.net` staging to `world.openfoodfacts.org` with configurable overrides.

## Scope

- Package: FoodSense (checkout `~/Documents/projects/FoodSense`)
- Modifies:
  - `src/main/java/com/foodsense/ProductApiClient.java` (new class for Open Food Facts API fetching & deserialization)
  - `src/main/java/com/foodsense/FoodSense.java` (CLI springboard entry point supporting `--barcode <code` / `-b <code>` or default GUI launch)
  - `src/main/java/com/foodsense/FoodSenseGUI.java` (Swing renderer using `ProductApiClient`)
  - `src/test/java/com/foodsense/ProductApiClientTest.java` (new unit test suite for host resolution and API client logic)
  - `README.md`
- Off-limits:
  - `Product.java`, `Nutriments.java`, `ApiResponse.java`, `VideoProcessor.java`
  - Existing test suite (`ProductTest.java`, `NutrimentsTest.java`, `JsonDeserializationTest.java`, `ApiResponseTest.java`)

## Non-Goals

- Do not alter `Product`, `Nutriments`, or `ApiResponse` data model schemas.
- Do not modify webcam/ZXing video barcode processing in `VideoProcessor.java`.
- Do not break existing Swing GUI layout or EDT event marshalling.

## Invariants

- **Host Resolution Order:** `foodsense.host` System property $\rightarrow$ `FOODSENSE_HOST` Env Var $\rightarrow$ default `world.openfoodfacts.org`.
- **CLI Mode Headless Operation:** Running `FoodSense.main(new String[]{"--barcode", "0049000006346"})` prints product details to stdout without initializing or displaying any Swing GUI components.
- **Existing Test Integrity:** All 31 existing JUnit tests must remain green.

## Requirements

1. THE SYSTEM SHALL extract Open Food Facts API HTTP GET requests and JSON deserialization from `FoodSenseGUI.java` into `ProductApiClient.java`. (R1)
2. WHEN `ProductApiClient` constructs API request URIs, THE SYSTEM SHALL default to host `world.openfoodfacts.org` while honoring `-Dfoodsense.host=...` and `FOODSENSE_HOST` overrides. (R2)
3. WHEN `FoodSense.main` is invoked with `--barcode <code` or `-b <code>`, THE SYSTEM SHALL execute a CLI barcode lookup and print product details to standard output without launching the GUI. (R3)
4. WHEN `FoodSense.main` is invoked with no arguments, THE SYSTEM SHALL launch `FoodSenseGUI`. (R4)
5. WHEN `mvn test` is executed, THE SYSTEM SHALL compile cleanly and pass all unit tests. (R5)

## Acceptance Criteria

1. `FoodSenseGUI.java` uses `ProductApiClient` for network lookups. (R1)
2. `ProductApiClient` defaults to host `world.openfoodfacts.org` and respects `foodsense.host` property / `FOODSENSE_HOST` env overrides. (R2)
3. Running `FoodSense.main(new String[]{"-b", "0049000006346"})` outputs product name, brand, Nutri-Score, and calories to stdout. (R3, R4)
4. `ProductApiClientTest.java` verifies host resolution, URL building, successful JSON deserialization, and 404/failure handling. (R5)
5. `mvn test` passes 100% of unit tests with 0 failures or errors. (R5)

## Design

`ProductApiClient.java` structure:

```java
public class ProductApiClient {
    private final String host;
    private final HttpSender httpSender;
    private final Gson gson;

    public ProductApiClient() {
        this(resolveDefaultHost());
    }

    public ProductApiClient(String host, HttpSender httpSender) {
        this.host = host != null && !host.isBlank() ? host : resolveDefaultHost();
        this.httpSender = httpSender;
        this.gson = new Gson();
    }

    public static String resolveDefaultHost() {
        String sysProp = System.getProperty("foodsense.host");
        if (sysProp != null && !sysProp.isBlank()) return sysProp;
        String envVar = System.getenv("FOODSENSE_HOST");
        if (envVar != null && !envVar.isBlank()) return envVar;
        return "world.openfoodfacts.org";
    }

    public Product fetchProduct(String barcode) { ... }
}
```

`FoodSense.java` CLI springboard:

```java
public class FoodSense {
    public static void main(String[] args) {
        if (args != null && args.length >= 2 && ("--barcode".equals(args[0]) || "-b".equals(args[0]))) {
            String barcode = args[1];
            ProductApiClient client = new ProductApiClient();
            Product product = client.fetchProduct(barcode);
            if (product != null) {
                System.out.println("Product: " + product.getProduct_name());
                System.out.println("Brand: " + product.getBrands());
                System.out.println("Nutri-Score: " + product.getNutriscore_grade());
            } else {
                System.out.println("Product not found for barcode: " + barcode);
            }
            return;
        }
        FoodSenseGUI gui = new FoodSenseGUI();
        gui.start();
    }
}
```

## Current State

- `FoodSenseGUI.java` (668 lines) included embedded `fetchProductFromAPI` hitting `world.openfoodfacts.net`. [verified]
- `FoodSense.java` was 28 lines and unconditionally launched GUI. [verified]
- Baseline: 31 JUnit 5 tests pass (`mvn test`). [verified 2026-08-13]

## Tests

- `ProductApiClientTest.java`: verifies host resolution hierarchy, request URI format, product JSON parsing, and null on failure.
- `ProductTest`, `NutrimentsTest`, `JsonDeserializationTest`, `ApiResponseTest` preserved.

## Constraints

- Follow `docs/specs` in-repo convention (D6/D7 of `master-refactor-v3.md`).
