package com.example.safebite;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class RecipeRewriteActivity extends AppCompatActivity {

    private EditText etInputRecipe, etAllergies;
    private Button btnSubmit;
    private ImageButton btnProfile;
    private TextView tvResponse;
    private ProgressBar loadingBar;

    private static final String TAG = "SafeBite";

    private static final String OPENROUTER_URL = "https://openrouter.ai/api/v1/chat/completions";
    private static final String MODEL_ID = "moonshotai/kimi-k2:free";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard); // Make sure your XML layout is named activity_dashboard

        // Initialize views
        etInputRecipe = findViewById(R.id.etInputRecipe);
        etAllergies = findViewById(R.id.etAllergies);
        btnSubmit = findViewById(R.id.btnSubmit);
        btnProfile = findViewById(R.id.btnProfile);
        tvResponse = findViewById(R.id.tvResponse);
        loadingBar = findViewById(R.id.loadingBar);

        // Profile button navigation
        btnProfile.setOnClickListener(v ->
                startActivity(new Intent(RecipeRewriteActivity.this, ProfileActivity.class)));

        // Submit button logic
        btnSubmit.setOnClickListener(view -> {
            String recipe = etInputRecipe.getText().toString().trim();
            String allergens = etAllergies.getText().toString().trim();

            if (recipe.isEmpty() || allergens.isEmpty()) {
                Toast.makeText(this, "Please fill both recipe and preferences", Toast.LENGTH_SHORT).show();
            } else {
                loadingBar.setVisibility(View.VISIBLE);
                callOpenRouterAPI(recipe, allergens);
            }
        });
    }

    private void callOpenRouterAPI(String recipe, String allergens) {
        OkHttpClient client = new OkHttpClient.Builder()
                .protocols(Collections.singletonList(Protocol.HTTP_1_1))
                .build();

        String prompt =
                "You are a helpful chef who rewrites recipes to make them allergy-safe. " +
                        "Take the following recipe and replace any allergens with safe alternatives. " +
                        "Clearly explain each replacement and why it is used.\n\n" +
                        "IMPORTANT: " +
                        "1. Keep the response in clean plain text without any special characters like *, _, or markdown formatting. " +
                        "2. Do NOT add extra symbols or emojis. " +
                        "3. Use this exact format:\n\n" +
                        "Title: [Allergy-Safe Recipe Name]\n\n" +
                        "Ingredients:\n" +
                        "- List each ingredient on a new line.\n\n" +
                        "Replacements:\n" +
                        "- Original ingredient → Replacement (short reason)\n\n" +
                        "Instructions:\n" +
                        "Step 1: ...\n" +
                        "Step 2: ...\n\n" +
                        "Allergens/Dietary Restrictions: " + allergens + "\n\n" +
                        "Original Recipe:\n" + recipe;


        try {
            JSONObject requestBodyJson = new JSONObject();
            requestBodyJson.put("model", MODEL_ID);

            JSONArray messages = new JSONArray();
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            messages.put(userMessage);

            requestBodyJson.put("messages", messages);

            RequestBody requestBody = RequestBody.create(
                    requestBodyJson.toString(),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(OPENROUTER_URL)
                    .post(requestBody)
                    .addHeader("Authorization", "Bearer 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") // ⚠️ Replace with secure storage before release
                    .addHeader("Content-Type", "application/json")
                    .addHeader("HTTP-Referer", "https://your-app.com")
                    .addHeader("X-Title", "SafeBite")
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "API call failed", e);
                    runOnUiThread(() -> {
                        loadingBar.setVisibility(View.GONE);
                        Toast.makeText(RecipeRewriteActivity.this, "API Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        Log.e(TAG, "API error: " + response.code());
                        runOnUiThread(() -> {
                            loadingBar.setVisibility(View.GONE);
                            Toast.makeText(RecipeRewriteActivity.this, "API error: " + response.message(), Toast.LENGTH_LONG).show();
                        });
                        return;
                    }

                    String responseBody = response.body().string();
                    try {
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        JSONArray choices = jsonResponse.getJSONArray("choices");
                        JSONObject firstChoice = choices.getJSONObject(0);
                        JSONObject message = firstChoice.getJSONObject("message");
                        String text = message.getString("content");

                        runOnUiThread(() -> {
                            loadingBar.setVisibility(View.GONE);
                            tvResponse.setText(text.trim());
                            tvResponse.setVisibility(View.VISIBLE);
                            Toast.makeText(RecipeRewriteActivity.this, "Recipe Rewritten!", Toast.LENGTH_SHORT).show();

                            FirebaseAuth auth = FirebaseAuth.getInstance();
                            String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "anonymous";

                            Map<String, Object> recipeData = new HashMap<>();
                            recipeData.put("normalRecipe", recipe);
                            recipeData.put("ingredients", allergens);
                            recipeData.put("outputRecipe", text.trim());

                            FirebaseFirestore.getInstance()
                                    .collection("recipes")
                                    .document(uid)
                                    .collection("userRecipes")
                                    .add(recipeData)
                                    .addOnSuccessListener(docRef ->
                                            Toast.makeText(RecipeRewriteActivity.this, "Recipe saved", Toast.LENGTH_SHORT).show())
                                    .addOnFailureListener(e ->
                                            Toast.makeText(RecipeRewriteActivity.this, "Failed to save", Toast.LENGTH_SHORT).show());
                        });

                    } catch (Exception e) {
                        Log.e(TAG, "JSON parse error", e);
                        runOnUiThread(() -> {
                            loadingBar.setVisibility(View.GONE);
                            Toast.makeText(RecipeRewriteActivity.this, "Parse Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Request error", e);
            loadingBar.setVisibility(View.GONE);
            Toast.makeText(this, "Request Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}