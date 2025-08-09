package com.equipe7.eductrack;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText emailInput, codeInput, newPassword;
    private Button btnSendCode, btnResetPassword;

    private String generatedCode;
    private FirebaseManager firebaseManager;
    private String userUidToReset = null;

    private final OkHttpClient client = new OkHttpClient();

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        emailInput = findViewById(R.id.emailOrPhone);
        codeInput = findViewById(R.id.codeInput);
        newPassword = findViewById(R.id.newPassword);
        btnSendCode = findViewById(R.id.btnSendCode);
        btnResetPassword = findViewById(R.id.btnResetPassword);

        firebaseManager = FirebaseManager.getInstance();

        btnSendCode.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Saisis un email valide", Toast.LENGTH_SHORT).show();
                return;
            }

            // Search for user in Firestore by email
            firebaseManager.getFirestore().collection("users")
                    .whereEqualTo("email", email)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                userUidToReset = document.getId();
                                break;
                            }

                            generatedCode = String.format("%06d", new Random().nextInt(999999));

                            // Send email via EmailJS
                            sendEmailWithEmailJS(email, generatedCode);

                            // Save reset code in Firestore for verification
                            if (userUidToReset != null) {
                                Map<String, Object> resetData = new HashMap<>();
                                resetData.put("resetCode", generatedCode);
                                resetData.put("resetCodeTimestamp", System.currentTimeMillis());
                                
                                firebaseManager.updateUserData(userUidToReset, resetData)
                                        .addOnSuccessListener(aVoid -> 
                                            Toast.makeText(this, "Code envoyé à votre email", Toast.LENGTH_SHORT).show())
                                        .addOnFailureListener(e -> 
                                            Toast.makeText(this, "Erreur lors de la sauvegarde", Toast.LENGTH_SHORT).show());
                            }

                        } else {
                            Toast.makeText(ForgotPasswordActivity.this, "Aucun compte avec cet email", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(ForgotPasswordActivity.this, "Erreur : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        btnResetPassword.setOnClickListener(v -> {
            String enteredCode = codeInput.getText().toString().trim();
            String newPass = newPassword.getText().toString().trim();

            if (userUidToReset == null) {
                Toast.makeText(this, "Veuillez d'abord demander un code", Toast.LENGTH_SHORT).show();
                return;
            }

            if (TextUtils.isEmpty(enteredCode) || TextUtils.isEmpty(newPass)) {
                Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newPass.length() < 6) {
                Toast.makeText(this, "Le mot de passe doit avoir au moins 6 caractères", Toast.LENGTH_SHORT).show();
                return;
            }

            // Verify the reset code from Firestore
            firebaseManager.getUserData(userUidToReset)
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String storedCode = documentSnapshot.getString("resetCode");
                            Long resetTimestamp = documentSnapshot.getLong("resetCodeTimestamp");
                            
                            // Check if code is valid and not expired (15 minutes)
                            if (storedCode != null && storedCode.equals(enteredCode)) {
                                if (resetTimestamp != null && 
                                    (System.currentTimeMillis() - resetTimestamp) < 15 * 60 * 1000) {
                                    
                                    // Reset password using Firebase Auth
                                    String userEmail = documentSnapshot.getString("email");
                                    if (userEmail != null) {
                                        resetPasswordForUser(userEmail, newPass);
                                    }
                                } else {
                                    Toast.makeText(this, "Code expiré. Veuillez demander un nouveau code", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(this, "Code incorrect", Toast.LENGTH_SHORT).show();
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Erreur lors de la vérification", Toast.LENGTH_SHORT).show();
                    });
        });
    }

    private void resetPasswordForUser(String email, String newPassword) {
        // Note: Firebase doesn't allow direct password reset from client side
        // This would typically require a custom backend or using Firebase Admin SDK
        // For now, we'll use the standard password reset flow
        
        firebaseManager.getAuth().sendPasswordResetEmail(email)
                .addOnSuccessListener(aVoid -> {
                    // Clear the reset code
                    Map<String, Object> clearData = new HashMap<>();
                    clearData.put("resetCode", null);
                    clearData.put("resetCodeTimestamp", null);
                    
                    firebaseManager.updateUserData(userUidToReset, clearData);
                    
                    Toast.makeText(this, "Email de réinitialisation envoyé", Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erreur : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void sendEmailWithEmailJS(String toEmail, String code) {
        new Thread(() -> {
            try {
                JSONObject emailData = new JSONObject();
                emailData.put("service_id", "service_eductrack");
                emailData.put("template_id", "template_forgot_password");
                emailData.put("user_id", "YOUR_EMAILJS_USER_ID");

                JSONObject templateParams = new JSONObject();
                templateParams.put("to_email", toEmail);
                templateParams.put("reset_code", code);
                templateParams.put("app_name", "EduTrack");

                emailData.put("template_params", templateParams);

                RequestBody body = RequestBody.create(
                        emailData.toString(),
                        MediaType.get("application/json")
                );

                Request request = new Request.Builder()
                        .url("https://api.emailjs.com/api/v1.0/email/send")
                        .post(body)
                        .addHeader("Content-Type", "application/json")
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    runOnUiThread(() -> {
                        if (response.isSuccessful()) {
                            Toast.makeText(this, "Code envoyé à " + toEmail, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Erreur lors de l'envoi de l'email", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } catch (JSONException | IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Erreur : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
}
