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
import com.google.firebase.database.*;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
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
    private FirebaseAuth mAuth;
    private DatabaseReference databaseRef;

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

        mAuth = FirebaseAuth.getInstance();
        databaseRef = FirebaseDatabase.getInstance().getReference("users");

        btnSendCode.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Saisis un email valide", Toast.LENGTH_SHORT).show();
                return;
            }

            databaseRef.orderByChild("email").equalTo(email)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                for (DataSnapshot userSnap : snapshot.getChildren()) {
                                    userUidToReset = userSnap.getKey();
                                    break;
                                }

                                generatedCode = String.format("%06d", new Random().nextInt(999999));

                                // Envoi de l'email via EmailJS
                                sendEmailWithEmailJS(email, generatedCode);

                                // Optionnel : sauvegarder le code dans Firebase pour traçabilité
                                if (userUidToReset != null) {
                                    databaseRef.child(userUidToReset).child("resetCode").setValue(generatedCode);
                                }

                            } else {
                                Toast.makeText(ForgotPasswordActivity.this, "Aucun compte avec cet email", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError error) {
                            Toast.makeText(ForgotPasswordActivity.this, "Erreur : " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        btnResetPassword.setOnClickListener(v -> {
            String enteredCode = codeInput.getText().toString().trim();
            String newPass = newPassword.getText().toString().trim();

            if (userUidToReset == null) {
                Toast.makeText(this, "Veuillez d'abord envoyer le code", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!enteredCode.equals(generatedCode)) {
                Toast.makeText(this, "Code incorrect", Toast.LENGTH_SHORT).show();
                return;
            }

            if (TextUtils.isEmpty(newPass) || newPass.length() < 6) {
                Toast.makeText(this, "Le mot de passe doit contenir au moins 6 caractères", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null && user.getUid().equals(userUidToReset)) {
                user.updatePassword(newPass)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(this, "Mot de passe mis à jour", Toast.LENGTH_LONG).show();
                                finish();
                            } else {
                                Toast.makeText(this, "Erreur : " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                Toast.makeText(this, "Reconnecte-toi pour changer le mot de passe", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void sendEmailWithEmailJS(String email, String code) {
        new Thread(() -> {
            try {
                JSONObject json = new JSONObject();
                json.put("service_id", "service_yvl11d5");
                json.put("template_id", "template_zlp263e");
                json.put("user_id", "Un7snKzeE4AGeorc");

                JSONObject templateParams = new JSONObject();
                templateParams.put("user_email", email);
                templateParams.put("code", code);

                json.put("template_params", templateParams);

                RequestBody body = RequestBody.create(
                        json.toString(),
                        MediaType.parse("application/json; charset=utf-8")
                );

                Request request = new Request.Builder()
                        .url("https://api.emailjs.com/api/v1.0/email/send")
                        .post(body)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        runOnUiThread(() ->
                                Toast.makeText(ForgotPasswordActivity.this, "Code envoyé à votre email", Toast.LENGTH_LONG).show());
                    } else {
                        runOnUiThread(() ->
                                Toast.makeText(ForgotPasswordActivity.this, "Échec de l'envoi du mail", Toast.LENGTH_LONG).show());
                    }
                }

            } catch (JSONException | IOException e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(ForgotPasswordActivity.this, "Erreur lors de l'envoi de l'email", Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}
