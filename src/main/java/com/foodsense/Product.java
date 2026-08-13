/**
 * Gson DTO for an Open Food Facts product record shown in the UI.
 * Field names match API snake_case keys (e.g. {@code product_name}, {@code nutriscore_grade}).
 */
package com.foodsense;

/**
 * Product identity, Nutri-Score, ingredients/allergens, nutriments, and image URLs.
 */
public class Product {
    private String product_name;
    private String brands;
    private String nutriscore_grade;
    private String ingredients_text;
    private Nutriments nutriments;
    private String allergens_from_ingredients;
    private String image_front_url;
    private String image_ingredients_url;
    private String image_nutrition_url;

    /** @return front-of-pack image URL, or {@code null} */
    public String getImage_front_url() {
        return image_front_url;
    }

    /** @param image_front_url front-of-pack image URL from the API */
    public void setImage_front_url(String image_front_url) {
        this.image_front_url = image_front_url;
    }

    /** @return ingredients-label image URL, or {@code null} */
    public String getImage_ingredients_url() {
        return image_ingredients_url;
    }

    /** @param image_ingredients_url ingredients-label image URL from the API */
    public void setImage_ingredients_url(String image_ingredients_url) {
        this.image_ingredients_url = image_ingredients_url;
    }

    /** @return nutrition-label image URL, or {@code null} */
    public String getImage_nutrition_url() {
        return image_nutrition_url;
    }

    /** @param image_nutrition_url nutrition-label image URL from the API */
    public void setImage_nutrition_url(String image_nutrition_url) {
        this.image_nutrition_url = image_nutrition_url;
    }

    /** @return brand string(s), or {@code null} */
    public String getBrands() {
        return brands;
    }

    /** @param brands brand string(s) from the API */
    public void setBrands(String brands) {
        this.brands = brands;
    }

    /** @return display name of the product, or {@code null} */
    public String getProduct_name() {
        return product_name;
    }

    /** @param product_name display name from the API */
    public void setProduct_name(String product_name) {
        this.product_name = product_name;
    }

    /**
     * @return Nutri-Score letter (A–E), or {@code null} when unknown
     */
    public String getNutriscore_grade() {
        return nutriscore_grade;
    }

    /** @param nutriscore_grade Nutri-Score letter from the API */
    public void setNutriscore_grade(String nutriscore_grade) {
        this.nutriscore_grade = nutriscore_grade;
    }

    /** @return per-100g nutriments block, or {@code null} */
    public Nutriments getNutriments() {
        return nutriments;
    }

    /** @param nutriments nested nutriments DTO */
    public void setNutriments(Nutriments nutriments) {
        this.nutriments = nutriments;
    }

    /** @return allergen text derived from ingredients, or {@code null} */
    public String getAllergens_from_ingredients() {
        return allergens_from_ingredients;
    }

    /** @param allergens_from_ingredients allergen text from the API */
    public void setAllergens_from_ingredients(String allergens_from_ingredients) {
        this.allergens_from_ingredients = allergens_from_ingredients;
    }

    /** @return free-text ingredients list, or {@code null} */
    public String getIngredients_text() {
        return ingredients_text;
    }

    /** @param ingredients_text free-text ingredients from the API */
    public void setIngredients_text(String ingredients_text) {
        this.ingredients_text = ingredients_text;
    }
}
