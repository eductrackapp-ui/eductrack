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

import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

public class VerificationCodeActivity extends AppCompatActivity {

    private EditText[] otpEditTexts;
    private Button btnVerify, btnResendCode;
    private TextView tvTimer, tvPhoneNumber;
    private ProgressBar progressBar;

    private String verificationId;
    private String phoneNumber;
    private CountDownTimer countDownTimer;
    private boolean isTimerRunning = false;
    private boolean isLoading = false;

    private FirebaseManager firebaseManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verification_code);

        firebaseManager = FirebaseManager.getInstance();

        // Get data from intent
        verificationId = getIntent().getStringExtra("verificationId");
        phoneNumber = getIntent().getStringExtra("phoneNumber");

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
        tvPhoneNumber = findViewById(R.id.tvPhoneNumber);
        progressBar = findViewById(R.id.progressBar);

        // Set phone number
        if (phoneNumber != null) {
            tvPhoneNumber.setText("Code sent to " + phoneNumber);
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
                verifyCode();
            }
        });

        btnResendCode.setOnClickListener(v -> {
            if (!isTimerRunning) {
                resendCode();
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
        btnVerify.setAlpha(allFilled ? 1.0f : 0.5f);
    }

    private void verifyCode() {
        String code = getOTPCode();
        
        if (code.length() != 6) {
            Toast.makeText(this, "Please enter the complete verification code", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoadingState(true);

        // Create credential and verify
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        
        firebaseManager.getAuth().signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    setLoadingState(false);
                    
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Phone number verified successfully!", Toast.LENGTH_SHORT).show();
                        
                        // Navigate to appropriate activity
                        Intent intent = new Intent(this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        handleVerificationError(task.getException());
                    }
                });
    }

    private void resendCode() {
        Toast.makeText(this, "Resending verification code...", Toast.LENGTH_SHORT).show();
        
        // Clear OTP fields
        clearOTPFields();
        
        // Start timer again
        startTimer();
        
        // In a real implementation, you would trigger the resend logic here
        // For now, we'll just show a message
        Toast.makeText(this, "New verification code sent!", Toast.LENGTH_SHORT).show();
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
        btnVerify.setEnabled(!loading);
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        
        if (loading) {
            btnVerify.setText("Verifying...");
        } else {
            btnVerify.setText("Verify Code");
        }
    }

    private void handleVerificationError(Exception exception) {
        String errorMessage = "Verification failed";
        
        if (exception != null) {
            String message = exception.getMessage();
            if (message != null) {
                if (message.contains("invalid verification code")) {
                    errorMessage = "Invalid verification code. Please try again.";
                    clearOTPFields();
                } else if (message.contains("expired")) {
                    errorMessage = "Verification code has expired. Please request a new one.";
                } else {
                    errorMessage = message;
                }
            }
        }
        
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
    }

    private void startTimer() {
        isTimerRunning = true;
        btnResendCode.setEnabled(false);
        btnResendCode.setAlpha(0.5f);
        
        countDownTimer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished / 1000;
                tvTimer.setText("Resend code in " + seconds + "s");
            }

            @Override
            public void onFinish() {
                isTimerRunning = false;
                btnResendCode.setEnabled(true);
                btnResendCode.setAlpha(1.0f);
                tvTimer.setText("Didn't receive the code?");
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
