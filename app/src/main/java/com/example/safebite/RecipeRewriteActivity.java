package com.example.safebite;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class RecipeRewriteActivity extends AppCompatActivity {

    private EditText etInputRecipe, etAllergies;
    private Button btnUploadFile, btnSubmit;
    private ImageButton btnProfile;
    private TextView tvResponse;

    private static final int FILE_SELECT_CODE = 0;
    private static final String TAG = "SafeBite";

    private static final String OPENROUTER_URL = "https://openrouter.ai/api/v1/chat/completions";
    private static final String MODEL_ID = "cognitivecomputations/dolphin-mistral-24b-venice-edition:free";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard); // Ensure layout name matches your XML

        // Init views
        etInputRecipe = findViewById(R.id.etInputRecipe);
        etAllergies = findViewById(R.id.etAllergies);
        btnUploadFile = findViewById(R.id.btnUploadFile);
        btnSubmit = findViewById(R.id.btnSubmit);
        btnProfile = findViewById(R.id.btnProfile);
        tvResponse = findViewById(R.id.tvResponse);

        btnProfile.setOnClickListener(v ->
                startActivity(new Intent(RecipeRewriteActivity.this, ProfileActivity.class)));

        btnUploadFile.setOnClickListener(view -> showFileChooser());

        btnSubmit.setOnClickListener(view -> {
            String recipe = etInputRecipe.getText().toString().trim();
            String allergens = etAllergies.getText().toString().trim();

            if (recipe.isEmpty() || allergens.isEmpty()) {
                Toast.makeText(this, "Please fill both recipe and preferences", Toast.LENGTH_SHORT).show();
            } else {
                callOpenRouterAPI(recipe, allergens);
            }
        });
    }

    private void showFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("text/plain");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, "Select a file"), FILE_SELECT_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "Please install a File Manager.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_SELECT_CODE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            StringBuilder content = new StringBuilder();

            try (InputStream inputStream = getContentResolver().openInputStream(uri);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }

                etInputRecipe.setText(content.toString().trim());
                Toast.makeText(this, "File loaded into recipe field", Toast.LENGTH_SHORT).show();

            } catch (IOException e) {
                Toast.makeText(this, "Failed to read file", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "File read error", e);
            }
        }
    }

    private void callOpenRouterAPI(String recipe, String allergens) {
        OkHttpClient client = new OkHttpClient.Builder()
                .protocols(Collections.singletonList(Protocol.HTTP_1_1))
                .build();

        String prompt = "Rewrite the following recipe with allergy-safe alternatives and explain each replacement.\n"
                + "Allergens/Dietary Restrictions: " + allergens + "\n\n"
                + "Recipe:\n" + recipe;

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
                    .addHeader("Authorization", "Bearer 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") // Secure API key
                    .addHeader("Content-Type", "application/json")
                    .addHeader("HTTP-Referer", "https://your-app.com")
                    .addHeader("X-Title", "SafeBite")
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "API call failed", e);
                    runOnUiThread(() ->
                            Toast.makeText(RecipeRewriteActivity.this, "API Failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                    );
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        Log.e(TAG, "API error: " + response.code());
                        runOnUiThread(() ->
                                Toast.makeText(RecipeRewriteActivity.this, "API error: " + response.message(), Toast.LENGTH_LONG).show()
                        );
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
                        runOnUiThread(() ->
                                Toast.makeText(RecipeRewriteActivity.this, "Parse Error: " + e.getMessage(), Toast.LENGTH_LONG).show()
                        );
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Request error", e);
            Toast.makeText(this, "Request Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
