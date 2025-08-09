package com.equipe7.eductrack;

import android.content.Intent;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etEmail, etPassword;
    private Button btnSignIn;
    private ImageView logo;
    private TextView tvForgot, tvRegister;

    private FirebaseManager firebaseManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Manager
        firebaseManager = FirebaseManager.getInstance();

        // Liaison avec les éléments UI
        etUsername = findViewById(R.id.etUsername);
        etEmail    = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnSignIn  = findViewById(R.id.btnSignIn);
        logo       = findViewById(R.id.logo);
        tvForgot   = findViewById(R.id.tvForgot);
        tvRegister = findViewById(R.id.tvRegister);

        // Connexion à l'application
        btnSignIn.setOnClickListener(view -> signInUser());

        // Lien : mot de passe oublié
        tvForgot.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });

        // ✅ Texte cliquable : "Don't have an account? Create One"
        String fullText = "Don't have an account? Create One";
        SpannableString spannableString = new SpannableString(fullText);

        // Partie "Create One" cliquable
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        };

        // Texte "Don't have an account?" en noir
        spannableString.setSpan(
                new ForegroundColorSpan(android.graphics.Color.BLACK),
                0,
                23,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        // Texte "Create One" en bleu et cliquable
        spannableString.setSpan(clickableSpan, 24, fullText.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        spannableString.setSpan(
                new ForegroundColorSpan(android.graphics.Color.BLUE),
                24,
                fullText.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        tvRegister.setText(spannableString);
        tvRegister.setMovementMethod(LinkMovementMethod.getInstance());
        tvRegister.setHighlightColor(android.graphics.Color.TRANSPARENT);
    }

    private void signInUser() {
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Sign in with Firebase Auth
        firebaseManager.getAuth().signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String userId = firebaseManager.getCurrentUser().getUid();

                        // Get user data from Firestore
                        firebaseManager.getUserData(userId)
                                .addOnSuccessListener(documentSnapshot -> {
                                    if (documentSnapshot.exists()) {
                                        User user = documentSnapshot.toObject(User.class);
                                        
                                        if (user != null && user.role != null) {
                                            Toast.makeText(LoginActivity.this,
                                                    "Welcome " + (user.username != null ? user.username : user.email) + " (" + user.role + ")",
                                                    Toast.LENGTH_SHORT).show();

                                            // Check if user has accepted terms
                                            if (!user.acceptedTerms) {
                                                Intent intent = new Intent(LoginActivity.this, TermsOfUseActivity.class);
                                                startActivity(intent);
                                                finish();
                                                return;
                                            }

                                            // Redirect based on role
                                            redirectToRoleActivity(user.role);
                                        } else {
                                            Toast.makeText(LoginActivity.this, "Error loading user data", Toast.LENGTH_SHORT).show();
                                        }
                                    } else {
                                        Toast.makeText(LoginActivity.this, "User data not found", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(LoginActivity.this, "Error loading user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Toast.makeText(this, "Login failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void redirectToRoleActivity(String role) {
        Intent intent;
        switch (role.toLowerCase()) {
            case "admin":
                intent = new Intent(this, AdminHomeActivity.class);
                break;
            case "teacher":
                intent = new Intent(this, TeacherHomeActivity.class);
                break;
            case "parent":
                intent = new Intent(this, ParentHomeActivity.class);
                break;
            default:
                intent = new Intent(this, MainActivity.class);
                break;
        }
        startActivity(intent);
        finish();
    }
}
