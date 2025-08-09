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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etEmail, etPassword;
    private Button btnSignIn;
    private ImageView logo;
    private TextView tvForgot, tvRegister;

    private FirebaseAuth auth;
    private DatabaseReference database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Liaison avec les éléments UI
        etUsername = findViewById(R.id.etUsername);
        etEmail    = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnSignIn  = findViewById(R.id.btnSignIn);
        logo       = findViewById(R.id.logo);
        tvForgot   = findViewById(R.id.tvForgot);
        tvRegister = findViewById(R.id.tvRegister);

        // Initialisation Firebase
        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance().getReference("users");

        // Connexion à l'application
        btnSignIn.setOnClickListener(view -> signInUser());

        // Lien : mot de passe oublié
        tvForgot.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });

        // ✅ Texte cliquable : "Don’t have an account? Create One"
        String fullText = "Don’t have an account? Create One";
        SpannableString spannableString = new SpannableString(fullText);

        // Partie "Create One" cliquable
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        };

        // Texte "Don’t have an account?" en noir
        spannableString.setSpan(
                new ForegroundColorSpan(android.graphics.Color.BLACK),
                0,
                23,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        // Texte "Create One" en bleu et cliquable
        spannableString.setSpan(clickableSpan, 24, fullText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
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

    // Méthode de connexion utilisateur avec vérification du rôle
    private void signInUser() {
        String username = etUsername.getText().toString().trim();
        String email    = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(username)) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String userId = auth.getCurrentUser().getUid();

                        // Récupération des infos depuis Realtime Database
                        database.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot snapshot) {
                                if (snapshot.exists()) {
                                    User user = snapshot.getValue(User.class);

                                    if (user != null && user.role != null) {
                                        Toast.makeText(LoginActivity.this,
                                                "Welcome " + user.username + " (" + user.role + ")",
                                                Toast.LENGTH_SHORT).show();

                                        // Redirection selon le rôle
                                        switch (user.role.toLowerCase()) {
                                            case "parent":
                                                startActivity(new Intent(LoginActivity.this, ParentHomeActivity.class));
                                                break;
                                            case "teacher":
                                                startActivity(new Intent(LoginActivity.this, TeacherHomeActivity.class));
                                                break;
                                            case "admin":
                                                startActivity(new Intent(LoginActivity.this, AdminHomeActivity.class));
                                                break;
                                            default:
                                                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                                break;
                                        }
                                        finish();
                                    } else {
                                        Toast.makeText(LoginActivity.this, "User role is not defined", Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    Toast.makeText(LoginActivity.this, "User not found in database", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError error) {
                                Toast.makeText(LoginActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        Toast.makeText(this, "Sign-in failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    // Classe interne représentant un utilisateur avec rôle
    public static class User {
        public String username;
        public String email;
        public String role;

        public User() {} // Requis pour Firebase

        public User(String username, String email, String role) {
            this.username = username;
            this.email = email;
            this.role = role;
        }
    }
}
