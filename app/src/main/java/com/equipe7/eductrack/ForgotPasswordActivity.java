package com.equipe7.eductrack;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputLayout tilEmail;
    private TextInputEditText etEmail;
    private Button btnSendResetEmail, btnBackToLogin;
    private ProgressBar progressBar;

    private FirebaseManager firebaseManager;
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        firebaseManager = FirebaseManager.getInstance();
        
        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        tilEmail = findViewById(R.id.tilEmail);
        etEmail = findViewById(R.id.etEmail);
        btnSendResetEmail = findViewById(R.id.btnSendResetEmail);
        btnBackToLogin = findViewById(R.id.btnBackToLogin);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupClickListeners() {
        btnSendResetEmail.setOnClickListener(v -> {
            if (!isLoading) {
                sendPasswordResetEmail();
            }
        });

        btnBackToLogin.setOnClickListener(v -> {
            finish(); // Go back to login activity
        });
    }

    private void sendPasswordResetEmail() {
        // Clear previous errors
        tilEmail.setError(null);

        String email = etEmail.getText().toString().trim();

        // Validate email
        if (!validateEmail(email)) {
            return;
        }

        // Show loading state
        setLoadingState(true);

        // Send password reset email using Firebase Auth
        firebaseManager.getAuth().sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    setLoadingState(false);
                    
                    if (task.isSuccessful()) {
                        showSuccessMessage(email);
                    } else {
                        handleResetError(task.getException());
                    }
                });
    }

    private boolean validateEmail(String email) {
        if (TextUtils.isEmpty(email)) {
            tilEmail.setError("Email is required");
            return false;
        }
        
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Please enter a valid email address");
            return false;
        }
        
        return true;
    }

    private void setLoadingState(boolean loading) {
        isLoading = loading;
        btnSendResetEmail.setEnabled(!loading);
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        
        if (loading) {
            btnSendResetEmail.setText("Sending...");
        } else {
            btnSendResetEmail.setText("Send Reset Email");
        }
    }

    private void showSuccessMessage(String email) {
        Toast.makeText(this, 
                "Password reset email sent to " + email + ". Please check your inbox.", 
                Toast.LENGTH_LONG).show();
        
        // Optionally, you can finish the activity or show a success screen
        // For now, we'll just clear the email field
        etEmail.setText("");
    }

    private void handleResetError(Exception exception) {
        String errorMessage = "Failed to send reset email";
        
        if (exception != null) {
            String message = exception.getMessage();
            if (message != null) {
                if (message.contains("no user record")) {
                    tilEmail.setError("No account found with this email address");
                    return;
                } else if (message.contains("badly formatted")) {
                    tilEmail.setError("Invalid email format");
                    return;
                } else if (message.contains("too many requests")) {
                    errorMessage = "Too many requests. Please try again later.";
                } else {
                    errorMessage = message;
                }
            }
        }
        
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
    }
}
