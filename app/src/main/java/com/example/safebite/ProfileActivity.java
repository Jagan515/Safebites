package com.example.safebite;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class ProfileActivity extends AppCompatActivity {

    MaterialButton btnLogout;
    RecyclerView recipeRecyclerView;
    TextView textAllRecipes, userName, userEmail;
    ShapeableImageView profileImage;

    FirebaseAuth mAuth;
    FirebaseUser currentUser;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("My Profile");
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            startActivity(new Intent(ProfileActivity.this, SignInActivity.class));
            finish();
            return;
        }

        recipeRecyclerView = findViewById(R.id.recipeRecyclerView);
        btnLogout = findViewById(R.id.logoutBtn);
        textAllRecipes = findViewById(R.id.textAllRecipes);
        userName = findViewById(R.id.userName);
        userEmail = findViewById(R.id.userEmail);
        profileImage = findViewById(R.id.profileImage);
        profileImage.setImageResource(R.drawable.logo);


        recipeRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        userName.setText(currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "Safebites");
        userEmail.setText(currentUser.getEmail());

        loadUserRecipes(currentUser.getUid());

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
                        List<DocumentSnapshot> docs = queryDocumentSnapshots.getDocuments();

                        RecyclerView.Adapter<RecyclerView.ViewHolder> adapter = new RecyclerView.Adapter<>() {
                            @Override
                            public int getItemCount() {
                                return docs.size();
                            }

                            @Override
                            public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                                MaterialCardView card = new MaterialCardView(ProfileActivity.this);
                                card.setLayoutParams(new RecyclerView.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.WRAP_CONTENT
                                ));
                                card.setCardElevation(6f);
                                card.setRadius(20f);
                                card.setUseCompatPadding(true);
                                card.setContentPadding(24, 24, 24, 24);
                                card.setClickable(true);

                                LinearLayout layout = new LinearLayout(ProfileActivity.this);
                                layout.setOrientation(LinearLayout.VERTICAL);

                                TextView title = new TextView(ProfileActivity.this);
                                title.setTextSize(18f);
                                title.setTypeface(null, Typeface.BOLD);
                                layout.addView(title);

                                TextView shortDesc = new TextView(ProfileActivity.this);
                                shortDesc.setMaxLines(2);
                                shortDesc.setEllipsize(TextUtils.TruncateAt.END);
                                layout.addView(shortDesc);

                                TextView readMore = new TextView(ProfileActivity.this);
                                readMore.setText("Read More");
                                readMore.setTextColor(Color.parseColor("#6200EE"));
                                readMore.setPadding(0, 16, 0, 0);
                                layout.addView(readMore);

                                card.addView(layout);

                                return new RecyclerView.ViewHolder(card) {
                                    TextView tvTitle = title;
                                    TextView tvShortDesc = shortDesc;
                                    TextView tvReadMore = readMore;
                                };
                            }

                            @Override
                            public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
                                DocumentSnapshot doc = docs.get(position);
                                String normal = doc.getString("normalRecipe");
                                String ing = doc.getString("ingredients");
                                String out = doc.getString("outputRecipe");

                                ViewGroup layout = (ViewGroup) ((MaterialCardView) holder.itemView).getChildAt(0);
                                TextView tvTitle = (TextView) layout.getChildAt(0);
                                TextView tvShort = (TextView) layout.getChildAt(1);
                                TextView tvRead = (TextView) layout.getChildAt(2);

                                tvTitle.setText(normal);
                                tvShort.setText(out);

                                tvRead.setOnClickListener(v -> new AlertDialog.Builder(ProfileActivity.this)
                                        .setTitle(normal)
                                        .setMessage("Ingredients:\n" + ing + "\n\nOutput:\n" + out)
                                        .setPositiveButton("Close", null)
                                        .show());
                            }
                        };

                        recipeRecyclerView.setVisibility(View.VISIBLE);
                        textAllRecipes.setVisibility(View.GONE);
                        recipeRecyclerView.setAdapter(adapter);
                    } else {
                        recipeRecyclerView.setVisibility(View.GONE);
                        textAllRecipes.setText("No recipes found.");
                        textAllRecipes.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error loading recipes", Toast.LENGTH_SHORT).show());
    }

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