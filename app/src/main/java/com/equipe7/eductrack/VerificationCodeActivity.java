package com.equipe7.eductrack;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;

public class VerificationCodeActivity extends AppCompatActivity {

    private static final String TAG = "VerificationCodeActivity";

    private EditText[] otpEditTexts;
    private Button btnVerify, btnResendCode;
    private TextView tvTimer, tvEmailAddress, tvTitle;
    private ProgressBar progressBar;

    private String email;
    private String username;
    private boolean fromLogin;
    private CountDownTimer countDownTimer;
    private boolean isTimerRunning = false;
    private boolean isLoading = false;

    private FirebaseManager firebaseManager;
    private EnhancedEmailOTPService enhancedOtpService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verification_code);

        firebaseManager = FirebaseManager.getInstance();
        enhancedOtpService = new EnhancedEmailOTPService(this);

        // Get data from intent
        email = getIntent().getStringExtra("email");
        username = getIntent().getStringExtra("username");
        fromLogin = getIntent().getBooleanExtra("fromLogin", false);

        Log.d(TAG, "Starting verification for email: " + email + ", fromLogin: " + fromLogin);

        initializeViews();
        setupOTPInputs();
        setupClickListeners();
        startTimer();
        
        // Add entrance animation
        addEntranceAnimation();
    }

    private void initializeViews() {
        // Initialize OTP EditTexts
        otpEditTexts = new EditText[6];
        otpEditTexts[0] = findViewById(R.id.etOtp1);
        otpEditTexts[1] = findViewById(R.id.etOtp2);
        otpEditTexts[2] = findViewById(R.id.etOtp3);
        otpEditTexts[3] = findViewById(R.id.etOtp4);
        otpEditTexts[4] = findViewById(R.id.etOtp5);
        otpEditTexts[5] = findViewById(R.id.etOtp6);

        btnVerify = findViewById(R.id.btnVerify);
        btnResendCode = findViewById(R.id.btnResendCode);
        tvTimer = findViewById(R.id.tvTimer);
        tvEmailAddress = findViewById(R.id.tvPhoneNumber); // Reusing the phone number TextView for email
        tvTitle = findViewById(R.id.tvTitle);
        progressBar = findViewById(R.id.progressBar);

        // Set email address and title based on context
        if (email != null) {
            tvEmailAddress.setText("Code envoyé à " + email);
        }

        if (fromLogin) {
            tvTitle.setText("Vérification de connexion");
        } else {
            tvTitle.setText("Vérification du code");
        }
    }

    private void addEntranceAnimation() {
        // Animate title
        tvTitle.setAlpha(0f);
        tvTitle.animate().alpha(1f).setDuration(500).start();
        
        // Animate email text
        tvEmailAddress.setAlpha(0f);
        tvEmailAddress.animate().alpha(1f).setDuration(500).setStartDelay(200).start();
        
        // Animate OTP fields with stagger
        for (int i = 0; i < otpEditTexts.length; i++) {
            otpEditTexts[i].setAlpha(0f);
            otpEditTexts[i].setTranslationY(50f);
            otpEditTexts[i].animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(400)
                    .setStartDelay(300 + (i * 50))
                    .start();
        }
        
        // Animate buttons
        btnVerify.setAlpha(0f);
        btnVerify.animate().alpha(1f).setDuration(500).setStartDelay(800).start();
        
        btnResendCode.setAlpha(0f);
        btnResendCode.animate().alpha(1f).setDuration(500).setStartDelay(900).start();
    }

    private void setupOTPInputs() {
        for (int i = 0; i < otpEditTexts.length; i++) {
            final int index = i;
            
            otpEditTexts[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() == 1) {
                        // Add input animation
                        animateFieldInput(otpEditTexts[index]);
                        
                        // Move to next field
                        if (index < otpEditTexts.length - 1) {
                            otpEditTexts[index + 1].requestFocus();
                        }
                        
                        // Check if all fields are filled
                        checkAllFieldsFilled();
                    } else if (s.length() == 0) {
                        // Move to previous field on backspace
                        if (index > 0) {
                            otpEditTexts[index - 1].requestFocus();
                        }
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });

            // Handle backspace
            otpEditTexts[i].setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (otpEditTexts[index].getText().toString().isEmpty() && index > 0) {
                        otpEditTexts[index - 1].requestFocus();
                        otpEditTexts[index - 1].setText("");
                    }
                }
                return false;
            });
        }
    }

    private void animateFieldInput(EditText field) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(field, "scaleX", 1f, 1.1f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(field, "scaleY", 1f, 1.1f, 1f);
        scaleX.setDuration(200);
        scaleY.setDuration(200);
        scaleX.start();
        scaleY.start();
    }

    private void setupClickListeners() {
        btnVerify.setOnClickListener(v -> {
            if (!isLoading) {
                verifyOTP();
            }
        });

        btnResendCode.setOnClickListener(v -> {
            if (!isTimerRunning) {
                resendOTP();
            }
        });
    }

    private void checkAllFieldsFilled() {
        boolean allFilled = true;
        for (EditText editText : otpEditTexts) {
            if (editText.getText().toString().trim().isEmpty()) {
                allFilled = false;
                break;
            }
        }
        
        btnVerify.setEnabled(allFilled);
        
        // Animate button state change
        if (allFilled) {
            btnVerify.animate().alpha(1.0f).scaleX(1.05f).scaleY(1.05f).setDuration(200)
                    .withEndAction(() -> btnVerify.animate().scaleX(1f).scaleY(1f).setDuration(100).start())
                    .start();
        } else {
            btnVerify.animate().alpha(0.5f).setDuration(200).start();
        }
    }

    private void verifyOTP() {
        String code = getOTPCode();
        
        Log.d(TAG, "Verifying OTP code: " + code);
        
        if (code.length() != 6) {
            showError("Veuillez entrer le code de vérification complet");
            shakeOTPFields();
            return;
        }

        setLoadingState(true);

        // Verify OTP using enhanced service
        enhancedOtpService.verifyOTP(email, code, new EnhancedEmailOTPService.OTPCallback() {
            @Override
            public void onSuccess(String message) {
                setLoadingState(false);
                Log.d(TAG, "OTP verification successful");
                
                // Success animation
                animateSuccess();
                
                if (fromLogin) {
                    handleLoginSuccess();
                } else {
                    Toast.makeText(VerificationCodeActivity.this, message, Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
            
            @Override
            public void onFailure(String error) {
                setLoadingState(false);
                Log.e(TAG, "OTP verification failed: " + error);
                handleVerificationError(error);
            }
        });
    }


    private void animateSuccess() {
        // Green flash animation for success
        for (EditText field : otpEditTexts) {
            field.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
            field.animate().alpha(0.7f).setDuration(200)
                    .withEndAction(() -> {
                        field.setBackgroundResource(R.drawable.otp_digit_background);
                        field.animate().alpha(1f).setDuration(200).start();
                    }).start();
        }
    }

    private void shakeOTPFields() {
        for (EditText field : otpEditTexts) {
            field.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
            // Add red flash for error
            field.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));
            field.animate().alpha(0.7f).setDuration(200)
                    .withEndAction(() -> {
                        field.setBackgroundResource(R.drawable.otp_digit_background);
                        field.animate().alpha(1f).setDuration(200).start();
                    }).start();
        }
    }

    private void handleLoginSuccess() {
        Log.d(TAG, "OTP verification successful, proceeding with login");
        
        // Get user data and sign them in
        firebaseManager.getFirestore()
                .collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        DocumentSnapshot userDoc = task.getResult().getDocuments().get(0);
                        User user = userDoc.toObject(User.class);
                        
                        if (user != null) {
                            Log.d(TAG, "User data retrieved, signing in: " + user.email);
                            // For OTP login, we'll use a simplified approach
                            proceedToUserHome(user);
                        } else {
                            showError("Erreur lors du chargement des données utilisateur");
                        }
                    } else {
                        showError("Utilisateur non trouvé");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error retrieving user data", e);
                    showError("Erreur lors de la vérification: " + e.getMessage());
                });
    }

    private void proceedToUserHome(User user) {
        Toast.makeText(this, "Connexion réussie! Bienvenue " + user.username, Toast.LENGTH_SHORT).show();
        
        // Check if user has accepted terms
        if (!user.acceptedTerms) {
            Intent intent = new Intent(this, TermsOfUseActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        // Redirect based on role
        Intent intent;
        switch (user.role.toLowerCase()) {
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
        
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }

    private void resendOTP() {
        Log.d(TAG, "Resending OTP to: " + email);
        setLoadingState(true);
        
        // Send new OTP using enhanced service
        enhancedOtpService.sendOTP(email, username, new EnhancedEmailOTPService.OTPCallback() {
            @Override
            public void onSuccess(String message) {
                setLoadingState(false);
                Log.d(TAG, "New OTP sent successfully");
                Toast.makeText(VerificationCodeActivity.this, "Nouveau code envoyé!", Toast.LENGTH_SHORT).show();
                clearOTPFields();
                startTimer();
            }

            @Override
            public void onFailure(String error) {
                setLoadingState(false);
                Log.e(TAG, "Failed to send new OTP: " + error);
                showError("Erreur lors de l'envoi: " + error);
            }
        });
    }


    private String getOTPCode() {
        StringBuilder code = new StringBuilder();
        for (EditText editText : otpEditTexts) {
            code.append(editText.getText().toString().trim());
        }
        return code.toString();
    }

    private void clearOTPFields() {
        for (EditText editText : otpEditTexts) {
            editText.setText("");
        }
        otpEditTexts[0].requestFocus();
        btnVerify.setEnabled(false);
        btnVerify.setAlpha(0.5f);
    }

    private void setLoadingState(boolean loading) {
        isLoading = loading;
        btnVerify.setEnabled(!loading && isAllFieldsFilled());
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        
        if (loading) {
            btnVerify.setText("Vérification...");
        } else {
            btnVerify.setText("Vérifier le code");
        }
    }

    private boolean isAllFieldsFilled() {
        for (EditText editText : otpEditTexts) {
            if (editText.getText().toString().trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private void handleVerificationError(String error) {
        String errorMessage = "Vérification échouée";
        
        if (error != null) {
            if (error.contains("invalid") || error.contains("incorrect")) {
                errorMessage = "Code de vérification invalide. Veuillez réessayer.";
                shakeOTPFields();
                clearOTPFields();
            } else if (error.contains("expired")) {
                errorMessage = "Le code de vérification a expiré. Veuillez en demander un nouveau.";
            } else {
                errorMessage = error;
            }
        }
        
        showError(errorMessage);
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void startTimer() {
        isTimerRunning = true;
        btnResendCode.setEnabled(false);
        btnResendCode.setAlpha(0.5f);
        
        countDownTimer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished / 1000;
                tvTimer.setText("Renvoyer le code dans " + seconds + "s");
            }

            @Override
            public void onFinish() {
                isTimerRunning = false;
                btnResendCode.setEnabled(true);
                btnResendCode.animate().alpha(1.0f).setDuration(300).start();
                tvTimer.setText("Vous n'avez pas reçu le code ?");
            }
        }.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        if (enhancedOtpService != null) {
            enhancedOtpService.shutdown();
        }
    }
}
