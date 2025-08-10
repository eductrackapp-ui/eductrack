package com.equipe7.eductrack;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.DocumentSnapshot;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private Button btnSignIn, btnOTPLogin;
    private ImageView logo;
    private TextView tvForgot, tvRegister, tvOTPInfo;
    private ProgressBar progressBar;
    private LinearLayout passwordContainer, otpInfoContainer;

    private FirebaseManager firebaseManager;
    private EmailOTPService otpService;
    private boolean isLoading = false;
    private boolean isOTPMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Manager
        firebaseManager = FirebaseManager.getInstance();
        
        // Initialize OTP Service
        otpService = new EmailOTPService(this);

        // Initialize UI elements
        initializeViews();
        setupClickListeners();
        setupRegisterText();
        
        // Test EmailJS connection on startup
        testEmailJSConnection();
    }

    private void initializeViews() {
        tilEmail = findViewById(R.id.email_input_layout);
        tilPassword = findViewById(R.id.password_input_layout);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnSignIn = findViewById(R.id.btnSignIn);
        btnOTPLogin = findViewById(R.id.btnOTPLogin);
        logo = findViewById(R.id.logo);
        tvForgot = findViewById(R.id.tvForgot);
        tvRegister = findViewById(R.id.tvRegister);
        tvOTPInfo = findViewById(R.id.tvOTPInfo);
        progressBar = findViewById(R.id.progressBar);
        
        // Get containers for animations
        passwordContainer = findViewById(R.id.password_input_layout);
        otpInfoContainer = findViewById(R.id.tvOTPInfo);
    }

    private void setupClickListeners() {
        btnSignIn.setOnClickListener(view -> {
            if (!isLoading) {
                if (isOTPMode) {
                    requestOTPLogin();
                } else {
                    signInWithPassword();
                }
            }
        });

        btnOTPLogin.setOnClickListener(view -> {
            if (!isLoading) {
                toggleAuthenticationMode();
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

    private void toggleAuthenticationMode() {
        if (isOTPMode) {
            switchToPasswordMode();
        } else {
            switchToOTPMode();
        }
    }

    private void switchToOTPMode() {
        Log.d(TAG, "Switching to OTP mode");
        isOTPMode = true;
        
        // Animate password field out
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(passwordContainer, "alpha", 1f, 0f);
        fadeOut.setDuration(300);
        fadeOut.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                passwordContainer.setVisibility(View.GONE);
                
                // Show OTP info with animation
                otpInfoContainer.setVisibility(View.VISIBLE);
                ObjectAnimator fadeIn = ObjectAnimator.ofFloat(otpInfoContainer, "alpha", 0f, 1f);
                fadeIn.setDuration(300);
                fadeIn.start();
            }
        });
        fadeOut.start();
        
        // Update button texts with animation
        animateButtonTextChange(btnSignIn, "Envoyer le code OTP");
        animateButtonTextChange(btnOTPLogin, "Connexion avec mot de passe");
    }

    private void switchToPasswordMode() {
        Log.d(TAG, "Switching to password mode");
        isOTPMode = false;
        
        // Animate OTP info out
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(otpInfoContainer, "alpha", 1f, 0f);
        fadeOut.setDuration(300);
        fadeOut.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                otpInfoContainer.setVisibility(View.GONE);
                
                // Show password field with animation
                passwordContainer.setVisibility(View.VISIBLE);
                ObjectAnimator fadeIn = ObjectAnimator.ofFloat(passwordContainer, "alpha", 0f, 1f);
                fadeIn.setDuration(300);
                fadeIn.start();
            }
        });
        fadeOut.start();
        
        // Update button texts with animation
        animateButtonTextChange(btnSignIn, "Se connecter");
        animateButtonTextChange(btnOTPLogin, "Se connecter avec OTP");
    }

    private void animateButtonTextChange(Button button, String newText) {
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(button, "alpha", 1f, 0.5f);
        fadeOut.setDuration(150);
        fadeOut.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                button.setText(newText);
                ObjectAnimator fadeIn = ObjectAnimator.ofFloat(button, "alpha", 0.5f, 1f);
                fadeIn.setDuration(150);
                fadeIn.start();
            }
        });
        fadeOut.start();
    }

    private void signInWithPassword() {
        Log.d(TAG, "Attempting password login");
        
        // Clear previous errors
        clearErrors();

        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validate inputs
        if (!validatePasswordInputs(email, password)) {
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

    private void requestOTPLogin() {
        Log.d(TAG, "Requesting OTP login");
        
        // Clear previous errors
        clearErrors();

        String email = etEmail.getText().toString().trim();

        // Validate email
        if (!validateEmailOnly(email)) {
            return;
        }

        // Show loading state
        setLoadingState(true);

        // First check if user exists in Firestore
        firebaseManager.getFirestore()
                .collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        // User exists, generate and send OTP
                        DocumentSnapshot userDoc = task.getResult().getDocuments().get(0);
                        User user = userDoc.toObject(User.class);
                        
                        if (user != null) {
                            sendOTPToUser(user);
                        } else {
                            setLoadingState(false);
                            showError("Erreur lors du chargement des données utilisateur");
                        }
                    } else {
                        setLoadingState(false);
                        tilEmail.setError("Aucun compte trouvé avec cet email");
                    }
                })
                .addOnFailureListener(e -> {
                    setLoadingState(false);
                    showError("Erreur lors de la vérification de l'utilisateur: " + e.getMessage());
                });
    }

    private void sendOTPToUser(User user) {
        Log.d(TAG, "Sending OTP to user: " + user.email);
        
        // Generate OTP
        String otpCode = otpService.generateOTP();
        
        // Store OTP in Firestore for verification
        storeOTPInFirestore(user.email, otpCode);
        
        // Send OTP via EmailJS
        otpService.sendOTP(user.email, user.username, otpCode, new EmailOTPService.OTPCallback() {
            @Override
            public void onSuccess(String message) {
                setLoadingState(false);
                Log.d(TAG, "OTP sent successfully to: " + user.email);
                
                // Show success message with animation
                showSuccessMessage("Code OTP envoyé à " + user.email);
                
                // Navigate to verification activity
                Intent intent = new Intent(LoginActivity.this, VerificationCodeActivity.class);
                intent.putExtra("email", user.email);
                intent.putExtra("username", user.username);
                intent.putExtra("fromLogin", true);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
            }

            @Override
            public void onFailure(String error) {
                setLoadingState(false);
                Log.e(TAG, "Failed to send OTP: " + error);
                showError("Erreur lors de l'envoi de l'OTP: " + error);
            }
        });
    }

    private void storeOTPInFirestore(String email, String otpCode) {
        Log.d(TAG, "Storing OTP in Firestore for: " + email);
        
        firebaseManager.getFirestore()
                .collection("otps")
                .document(email)
                .set(new java.util.HashMap<String, Object>() {{
                    put("code", otpCode);
                    put("timestamp", System.currentTimeMillis());
                    put("used", false);
                }})
                .addOnSuccessListener(aVoid -> Log.d(TAG, "OTP stored successfully"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to store OTP", e));
    }

    private void testEmailJSConnection() {
        Log.d(TAG, "Testing EmailJS connection...");
        // This is a silent test - no UI feedback unless there's an error
        // otpService.testConnection(null);
    }

    private boolean validatePasswordInputs(String email, String password) {
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

    private boolean validateEmailOnly(String email) {
        boolean isValid = true;

        // Validate email
        if (TextUtils.isEmpty(email)) {
            tilEmail.setError("Email is required");
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Please enter a valid email address");
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
        btnOTPLogin.setEnabled(!loading);
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        
        if (loading) {
            if (isOTPMode) {
                btnSignIn.setText("Envoi en cours...");
            } else {
                btnSignIn.setText("Connexion...");
            }
        } else {
            if (isOTPMode) {
                btnSignIn.setText("Envoyer le code OTP");
            } else {
                btnSignIn.setText("Se connecter");
            }
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
        
        // Add shake animation to the form
        logo.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
    }

    private void showSuccessMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        
        // Add success animation
        logo.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
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
