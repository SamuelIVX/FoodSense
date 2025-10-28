package com.foodsense;
import com.google.gson.annotations.SerializedName;

public class Nutriments {
    private String energy;

    private String sugars;
    private String fat;

    @SerializedName("saturated-fat")
    private String saturated_fat;

    private String carbohydrates;
    private String proteins;
    private String salt;
    private String sodium;

    public String getCarbohydrates() {
        return carbohydrates;
    }

    public void setCarbohydrates(String carbohydrates) {
        this.carbohydrates = carbohydrates;
    }

    public String getEnergy() {
        return energy;
    }

    public void setEnergy(String energy) {
        this.energy = energy;
    }

    public String getFat() {
        return fat;
    }

    public void setFat(String fat) {
        this.fat = fat;
    }

    public String getProteins() {
        return proteins;
    }

    public void setProteins(String proteins) {
        this.proteins = proteins;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public String getSaturated_fat() {
        return saturated_fat;
    }

    public void setSaturated_fat(String saturated_fat) {
        this.saturated_fat = saturated_fat;
    }

    public String getSodium() {
        return sodium;
    }

    public void setSodium(String sodium) {
        this.sodium = sodium;
    }

    public String getSugars() {
        return sugars;
    }

    public void setSugars(String sugars) {
        this.sugars = sugars;
    }

    @Override
    public String toString(){
        return (
                "Carbohydrates: " + carbohydrates
                + "\nEnergy: " + energy
                + "\nFat: " + fat
                + "\nProteins: " + proteins
                + "\nSalt: " + salt
                + "\nSaturated Fat: " + saturated_fat
                + "\nSodium: " + sodium
                + "\nSugars: " + sugars
        );
    }
}
