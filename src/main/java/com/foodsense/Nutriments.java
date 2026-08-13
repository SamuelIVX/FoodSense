/*
 * Gson DTO for Open Food Facts nutriment values (typically per 100g).
 * Uses {@link com.google.gson.annotations.SerializedName} for {@code energy} and {@code saturated-fat}.
 */

package com.foodsense;

import com.google.gson.annotations.SerializedName;

/**
 * Nutrition facts fields shown in the Nutrition Facts grid.
 * Values are stored as strings to match the API's mixed numeric/string JSON.
 */
public class Nutriments {
    @SerializedName("energy")
    private String calories;

    private String sugars;
    private String fat;

    @SerializedName("saturated-fat")
    private String saturated_fat;

    private String carbohydrates;
    private String proteins;
    private String salt;
    private String sodium;

    /** @return carbohydrates amount string, or {@code null} */
    public String getCarbohydrates() {
        return carbohydrates;
    }

    /** @param carbohydrates carbohydrates amount from the API */
    public void setCarbohydrates(String carbohydrates) {
        this.carbohydrates = carbohydrates;
    }

    /**
     * Energy mapped from JSON key {@code energy} onto the internal {@code calories} field.
     *
     * @return energy/calories amount string, or {@code null}
     */
    public String getEnergy() {
        return calories;
    }

    /**
     * @param energy energy value; stored under the {@code calories} field ({@code @SerializedName("energy")})
     */
    public void setEnergy(String energy) {
        this.calories = energy;
    }

    /** @return fat amount string, or {@code null} */
    public String getFat() {
        return fat;
    }

    /** @param fat fat amount from the API */
    public void setFat(String fat) {
        this.fat = fat;
    }

    /** @return proteins amount string, or {@code null} */
    public String getProteins() {
        return proteins;
    }

    /** @param proteins proteins amount from the API */
    public void setProteins(String proteins) {
        this.proteins = proteins;
    }

    /** @return salt amount string, or {@code null} */
    public String getSalt() {
        return salt;
    }

    /** @param salt salt amount from the API */
    public void setSalt(String salt) {
        this.salt = salt;
    }

    /**
     * Saturated fat mapped from JSON key {@code saturated-fat}.
     *
     * @return saturated fat amount string, or {@code null}
     */
    public String getSaturated_fat() {
        return saturated_fat;
    }

    /** @param saturated_fat saturated fat amount from the API */
    public void setSaturated_fat(String saturated_fat) {
        this.saturated_fat = saturated_fat;
    }

    /** @return sodium amount string, or {@code null} */
    public String getSodium() {
        return sodium;
    }

    /** @param sodium sodium amount from the API */
    public void setSodium(String sodium) {
        this.sodium = sodium;
    }

    /** @return sugars amount string, or {@code null} */
    public String getSugars() {
        return sugars;
    }

    /** @param sugars sugars amount from the API */
    public void setSugars(String sugars) {
        this.sugars = sugars;
    }

    /**
     * Human-readable dump of all nutriment fields (debug / logging aid).
     *
     * @return multi-line summary; missing fields appear as {@code null} in the text
     */
    @Override
    public String toString() {
        return (
                "Carbohydrates: " + carbohydrates
                + "\nEnergy: " + calories
                + "\nFat: " + fat
                + "\nProteins: " + proteins
                + "\nSalt: " + salt
                + "\nSaturated Fat: " + saturated_fat
                + "\nSodium: " + sodium
                + "\nSugars: " + sugars
        );
    }
}
