package com.foodsense;

import java.util.Scanner;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.swing.*;
import java.awt.*;

public class FoodSense {
    static void main(String[] args){
        Product product = new Product();
        ApiResponse apiResponse = new ApiResponse();
        Gson gson = new GsonBuilder()
                    .setPrettyPrinting()
                    .create();

//        Scanner input = new Scanner(System.in);
//        System.out.print("Enter Your Barcode: ");
//        String barcode = input.nextLine();

//        try (HttpClient client = HttpClient.newHttpClient()) {
//            HttpRequest request = HttpRequest.newBuilder()
//                    .uri(URI.create("https://world.openfoodfacts.net/api/v2/product/" + barcode))
//                    .GET()
//                    .build();
//
//            try {
//                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
//
//                apiResponse = gson.fromJson(response.body(), ApiResponse.class);
//                product = apiResponse.getProduct();
//                System.out.println(product.getNutriments());
//
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            }
//        }

        FoodSenseGUI foodSenseGUI = new FoodSenseGUI();

    }
}

//3017624010701