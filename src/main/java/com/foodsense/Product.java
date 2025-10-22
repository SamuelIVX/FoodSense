package com.foodsense;

public class Product {
    private String product_name;
    private String brands;
    private String nutriscore_grade;
    private String ingredients_text;
    private Nutriments nutriments;
    private String allergens_from_ingredients;
    private String image_front_url;
    private String image_ingredients_url;

    public String getImage_front_url() {
        return image_front_url;
    }

    public void setImage_front_url(String image_front_url) {
        this.image_front_url = image_front_url;
    }

    public String getImage_ingredients_url() {
        return image_ingredients_url;
    }

    public void setImage_ingredients_url(String image_ingredients_url) {
        this.image_ingredients_url = image_ingredients_url;
    }

    public String getImage_nutrition_url() {
        return image_nutrition_url;
    }

    public void setImage_nutrition_url(String image_nutrition_url) {
        this.image_nutrition_url = image_nutrition_url;
    }

    private String image_nutrition_url;


    public String getBrands() {
        return brands;
    }

    public void setBrands(String brands) {
        this.brands = brands;
    }

    public String getProduct_name() {
        return product_name;
    }

    public void setProduct_name(String product_name) {
        this.product_name = product_name;
    }

    public String getNutriscore_grade() {
        return nutriscore_grade;
    }

    public void setNutriscore_grade(String nutriscore_grade) {
        this.nutriscore_grade = nutriscore_grade;
    }

    public Nutriments getNutriments() {
        return nutriments;
    }

    public void setNutriments(Nutriments nutriments) {
        this.nutriments = nutriments;
    }

    public String getAllergens_from_ingredients() {
        return allergens_from_ingredients;
    }

    public void setAllergens_from_ingredients(String allergens_from_ingredients) {
        this.allergens_from_ingredients = allergens_from_ingredients;
    }

    public String getIngredients_text() {
        return ingredients_text;
    }

    public void setIngredients_text(String ingredients_text) {
        this.ingredients_text = ingredients_text;
    }
}
