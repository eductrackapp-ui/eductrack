package com.equipe7.eductrack;

import android.content.Intent;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Patterns;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.DocumentSnapshot;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private Button btnSignIn;
    private ImageView logo;
    private TextView tvForgot, tvRegister;
    private ProgressBar progressBar;

    private FirebaseManager firebaseManager;
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Manager
        firebaseManager = FirebaseManager.getInstance();

        // Initialize UI elements
        initializeViews();
        setupClickListeners();
        setupRegisterText();
    }

    private void initializeViews() {
        tilEmail = findViewById(R.id.email_input_layout);
        tilPassword = findViewById(R.id.password_input_layout);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnSignIn = findViewById(R.id.btnSignIn);
        logo = findViewById(R.id.logo);
        tvForgot = findViewById(R.id.tvForgot);
        tvRegister = findViewById(R.id.tvRegister);
        // Note: progressBar is not in the new layout, we'll handle loading differently
        // progressBar = findViewById(R.id.progressBar);
    }

    private void setupClickListeners() {
        btnSignIn.setOnClickListener(view -> {
            if (!isLoading) {
                signInUser();
            }
        });

        tvForgot.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });
    }

    private void setupRegisterText() {
        String fullText = "Don't have an account? Create One";
        SpannableString spannableString = new SpannableString(fullText);

        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        };

        // Style the text
        spannableString.setSpan(
                new ForegroundColorSpan(android.graphics.Color.BLACK),
                0, 23, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        spannableString.setSpan(clickableSpan, 24, fullText.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        spannableString.setSpan(
                new ForegroundColorSpan(android.graphics.Color.BLUE),
                24, fullText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        tvRegister.setText(spannableString);
        tvRegister.setMovementMethod(LinkMovementMethod.getInstance());
        tvRegister.setHighlightColor(android.graphics.Color.TRANSPARENT);
    }

    private void signInUser() {
        // Clear previous errors
        clearErrors();

        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validate inputs
        if (!validateInputs(email, password)) {
            return;
        }

        // Show loading state
        setLoadingState(true);

        // Sign in with Firebase Auth
        firebaseManager.getAuth().signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    setLoadingState(false);
                    
                    if (task.isSuccessful()) {
                        String userId = firebaseManager.getCurrentUser().getUid();
                        loadUserDataAndRedirect(userId);
                    } else {
                        handleLoginError(task.getException());
                    }
                });
    }

    private boolean validateInputs(String email, String password) {
        boolean isValid = true;

        // Validate email
        if (TextUtils.isEmpty(email)) {
            tilEmail.setError("Email is required");
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Please enter a valid email address");
            isValid = false;
        }

        // Validate password
        if (TextUtils.isEmpty(password)) {
            tilPassword.setError("Password is required");
            isValid = false;
        } else if (password.length() < 6) {
            tilPassword.setError("Password must be at least 6 characters");
            isValid = false;
        }

        return isValid;
    }

    private void clearErrors() {
        tilEmail.setError(null);
        tilPassword.setError(null);
    }

    private void setLoadingState(boolean loading) {
        isLoading = loading;
        btnSignIn.setEnabled(!loading);
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        
        if (loading) {
            btnSignIn.setText("Signing in...");
        } else {
            btnSignIn.setText("Sign in");
        }
    }

    private void loadUserDataAndRedirect(String userId) {
        firebaseManager.getUserData(userId)
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        
                        if (user != null && user.role != null) {
                            showWelcomeMessage(user);

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
                            showError("Error loading user data");
                        }
                    } else {
                        showError("User data not found");
                    }
                })
                .addOnFailureListener(e -> {
                    showError("Error loading user data: " + e.getMessage());
                });
    }

    private void handleLoginError(Exception exception) {
        String errorMessage = "Login failed";
        
        if (exception != null) {
            String message = exception.getMessage();
            if (message != null) {
                if (message.contains("password is invalid")) {
                    tilPassword.setError("Incorrect password");
                    return;
                } else if (message.contains("no user record")) {
                    tilEmail.setError("No account found with this email");
                    return;
                } else if (message.contains("badly formatted")) {
                    tilEmail.setError("Invalid email format");
                    return;
                }
                errorMessage = message;
            }
        }
        
        showError(errorMessage);
    }

    private void showWelcomeMessage(User user) {
        String displayName = user.username != null ? user.username : user.email;
        Toast.makeText(this, "Welcome " + displayName + " (" + user.role + ")",
                Toast.LENGTH_SHORT).show();
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
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
