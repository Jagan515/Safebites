package com.example.safebite;

public class UserRecipe {
    public String userId;
    public String userName;
    public String normalRecipe;
    public String ingredients;
    public String outputRecipe;

    public UserRecipe() {}

    public UserRecipe(String userId, String userName, String normalRecipe, String ingredients, String outputRecipe) {
        this.userId = userId;
        this.userName = userName;
        this.normalRecipe = normalRecipe;
        this.ingredients = ingredients;
        this.outputRecipe = outputRecipe;
    }
}