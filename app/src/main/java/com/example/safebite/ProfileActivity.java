package com.example.safebite;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileActivity extends AppCompatActivity {

    Button btnLogout;
    RecyclerView recipeRecyclerView;
    TextView textAllRecipes, userName, userEmail;
    ShapeableImageView profileImage;

    FirebaseAuth mAuth;
    FirebaseUser currentUser;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Enable ActionBar with back arrow
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("My Profile");
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Redirect to SignIn if not logged in
        if (currentUser == null) {
            startActivity(new Intent(ProfileActivity.this, SignInActivity.class));
            finish();
            return;
        }

        // Initialize views
        recipeRecyclerView = findViewById(R.id.recipeRecyclerView);
        btnLogout = findViewById(R.id.logoutBtn);
        textAllRecipes = findViewById(R.id.textAllRecipes);
        userName = findViewById(R.id.userName);
        userEmail = findViewById(R.id.userEmail);
        profileImage = findViewById(R.id.profileImage);

        // Set user name & email
        userName.setText(currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "No Name");
        userEmail.setText(currentUser.getEmail());

        // Load user recipes from Firestore
        loadUserRecipes(currentUser.getUid());

        // Logout functionality
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Toast.makeText(ProfileActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(ProfileActivity.this, SignInActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void loadUserRecipes(String uid) {
        FirebaseFirestore.getInstance()
                .collection("recipes")
                .document(uid)
                .collection("userRecipes")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        StringBuilder allRecipes = new StringBuilder();
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            String normal = doc.getString("normalRecipe");
                            String ing = doc.getString("ingredients");
                            String out = doc.getString("outputRecipe");

                            allRecipes.append("Normal: ").append(normal).append("\n")
                                    .append("Ingredients: ").append(ing).append("\n")
                                    .append("Output: ").append(out).append("\n\n");
                        }

                        textAllRecipes.setText(allRecipes.toString());
                    } else {
                        textAllRecipes.setText("No recipes found.");
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading recipes", Toast.LENGTH_SHORT).show();
                });
    }

    // Handle back arrow to go to DashboardActivity
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Intent intent = new Intent(ProfileActivity.this, RecipeRewriteActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}