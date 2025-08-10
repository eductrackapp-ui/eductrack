package com.equipe7.eductrack;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;

public class VerificationCodeActivity extends AppCompatActivity {

    private EditText[] otpEditTexts;
    private Button btnVerify, btnResendCode;
    private TextView tvTimer, tvEmailAddress, tvTitle;
    private ProgressBar progressBar;

    private String email;
    private String otpType;
    private boolean fromLogin;
    private CountDownTimer countDownTimer;
    private boolean isTimerRunning = false;
    private boolean isLoading = false;

    private FirebaseManager firebaseManager;
    private EmailOTPService otpService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verification_code);

        firebaseManager = FirebaseManager.getInstance();
        otpService = new EmailOTPService(this);

        // Get data from intent
        email = getIntent().getStringExtra("email");
        otpType = getIntent().getStringExtra("otpType");
        fromLogin = getIntent().getBooleanExtra("fromLogin", false);

        initializeViews();
        setupOTPInputs();
        setupClickListeners();
        startTimer();
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

    private void setupOTPInputs() {
        for (int i = 0; i < otpEditTexts.length; i++) {
            final int index = i;
            
            otpEditTexts[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() == 1) {
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

        // Back button - remove this if btnBack doesn't exist in layout
        // findViewById(R.id.btnBack).setOnClickListener(v -> {
        //     finish();
        // });
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
        btnVerify.setAlpha(allFilled ? 1.0f : 0.5f);
    }

    private void verifyOTP() {
        String code = getOTPCode();
        
        if (code.length() != 6) {
            Toast.makeText(this, "Veuillez entrer le code de vérification complet", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoadingState(true);

        // Verify OTP by checking Firestore
        verifyOTPFromFirestore(email, code);
    }

    private void handleLoginSuccess() {
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
                            // Sign in the user with Firebase Auth
                            signInUserWithFirebase(user);
                        } else {
                            showError("Erreur lors du chargement des données utilisateur");
                        }
                    } else {
                        showError("Utilisateur non trouvé");
                    }
                })
                .addOnFailureListener(e -> {
                    showError("Erreur lors de la vérification: " + e.getMessage());
                });
    }

    private void signInUserWithFirebase(User user) {
        // For OTP login, we'll create a temporary password and sign in
        // In a production app, you might want to use custom tokens instead
        String tempPassword = "temp_" + System.currentTimeMillis();
        
        // First try to sign in (user might already exist in Firebase Auth)
        firebaseManager.getAuth().signInWithEmailAndPassword(email, tempPassword)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // User signed in successfully
                        proceedToUserHome(user);
                    } else {
                        // User doesn't exist in Firebase Auth, create account
                        firebaseManager.getAuth().createUserWithEmailAndPassword(email, tempPassword)
                                .addOnCompleteListener(createTask -> {
                                    if (createTask.isSuccessful()) {
                                        proceedToUserHome(user);
                                    } else {
                                        showError("Erreur lors de la connexion: " + createTask.getException().getMessage());
                                    }
                                });
                    }
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
        finish();
    }

    private void resendOTP() {
        setLoadingState(true);
        
        // Get user data to determine OTP type
        firebaseManager.getFirestore()
                .collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        DocumentSnapshot userDoc = task.getResult().getDocuments().get(0);
                        User user = userDoc.toObject(User.class);
                        
                        if (user != null) {
                            String currentOtpType = "admin".equals(user.role) ? "admin_login" : "login";
                            sendNewOTP(user.username, currentOtpType);
                        } else {
                            setLoadingState(false);
                            showError("Erreur lors du chargement des données utilisateur");
                        }
                    } else {
                        setLoadingState(false);
                        showError("Utilisateur non trouvé");
                    }
                });
    }

    private void sendNewOTP(String username, String currentOtpType) {
        String otpCode = otpService.generateOTP();
        
        // Store new OTP in Firestore
        storeOTPInFirestore(email, otpCode);
        
        EmailOTPService.OTPCallback callback = new EmailOTPService.OTPCallback() {
            @Override
            public void onSuccess(String message) {
                setLoadingState(false);
                Toast.makeText(VerificationCodeActivity.this, "Nouveau code envoyé!", Toast.LENGTH_SHORT).show();
                clearOTPFields();
                startTimer();
            }

            @Override
            public void onFailure(String error) {
                setLoadingState(false);
                showError("Erreur lors de l'envoi: " + error);
            }
        };
        
        if ("admin_login".equals(currentOtpType)) {
            otpService.sendAdminLoginOTP(email, username, otpCode, callback);
        } else {
            otpService.sendLoginOTP(email, username, otpCode, callback);
        }
    }
    
    private void verifyOTPFromFirestore(String email, String code) {
        firebaseManager.getFirestore()
                .collection("otps")
                .document(email)
                .get()
                .addOnCompleteListener(task -> {
                    setLoadingState(false);
                    
                    if (task.isSuccessful() && task.getResult().exists()) {
                        String storedCode = task.getResult().getString("code");
                        Long timestamp = task.getResult().getLong("timestamp");
                        Boolean used = task.getResult().getBoolean("used");
                        
                        if (storedCode != null && storedCode.equals(code)) {
                            if (used != null && used) {
                                handleVerificationError("Ce code a déjà été utilisé");
                                return;
                            }
                            
                            if (timestamp != null && otpService.isOTPExpired(timestamp)) {
                                handleVerificationError("Le code a expiré");
                                return;
                            }
                            
                            // Mark OTP as used
                            task.getResult().getReference().update("used", true);
                            
                            if (fromLogin) {
                                handleLoginSuccess();
                            } else {
                                Toast.makeText(this, "Code vérifié avec succès!", Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        } else {
                            handleVerificationError("Code incorrect");
                        }
                    } else {
                        handleVerificationError("Code non trouvé ou expiré");
                    }
                })
                .addOnFailureListener(e -> {
                    setLoadingState(false);
                    handleVerificationError("Erreur de vérification: " + e.getMessage());
                });
    }
    
    private void storeOTPInFirestore(String email, String otpCode) {
        firebaseManager.getFirestore()
                .collection("otps")
                .document(email)
                .set(new java.util.HashMap<String, Object>() {{
                    put("code", otpCode);
                    put("timestamp", System.currentTimeMillis());
                    put("used", false);
                }});
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
                clearOTPFields();
            } else if (error.contains("expired")) {
                errorMessage = "Le code de vérification a expiré. Veuillez en demander un nouveau.";
            } else {
                errorMessage = error;
            }
        }
        
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
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
                btnResendCode.setAlpha(1.0f);
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
    }
}
