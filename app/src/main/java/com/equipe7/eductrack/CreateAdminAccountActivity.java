package com.equipe7.eductrack;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class CreateAdminAccountActivity extends AppCompatActivity {

    private EditText etFullName, etEmail, etPassword, etConfirmPassword,
            etInstitutionName, etCompanyName, etTimNumber, etSdmcCode;

    private Button btnNext;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_admin_account);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Liaison des vues
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        etInstitutionName = findViewById(R.id.etInstitutionName);
        etCompanyName = findViewById(R.id.etCompanyName);
        etTimNumber = findViewById(R.id.etTimNumber);
        etSdmcCode = findViewById(R.id.etSdmcCode);
        btnNext = findViewById(R.id.btnNext);

        btnNext.setOnClickListener(v -> {
            String fullName = etFullName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String confirmPassword = etConfirmPassword.getText().toString().trim();
            String institutionName = etInstitutionName.getText().toString().trim();
            String companyName = etCompanyName.getText().toString().trim();
            String timNumber = etTimNumber.getText().toString().trim();
            String sdmcCode = etSdmcCode.getText().toString().trim();

            if (TextUtils.isEmpty(fullName) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)
                    || TextUtils.isEmpty(confirmPassword) || TextUtils.isEmpty(institutionName)
                    || TextUtils.isEmpty(companyName) || TextUtils.isEmpty(timNumber) || TextUtils.isEmpty(sdmcCode)) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirmPassword)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser firebaseUser = mAuth.getCurrentUser();
                            if (firebaseUser != null) {
                                Map<String, Object> adminData = new HashMap<>();
                                adminData.put("fullName", fullName);
                                adminData.put("email", email);
                                adminData.put("institutionName", institutionName);
                                adminData.put("companyName", companyName);
                                adminData.put("timNumber", timNumber);
                                adminData.put("sdmcCode", sdmcCode);
                                adminData.put("role", "admin");

                                db.collection("users")
                                        .document(firebaseUser.getUid())
                                        .set(adminData)
                                        .addOnSuccessListener(unused -> {
                                            firebaseUser.sendEmailVerification()
                                                    .addOnSuccessListener(aVoid -> {
                                                        Toast.makeText(this, "Account created. Please verify your email.", Toast.LENGTH_LONG).show();
                                                        mAuth.signOut();
                                                        startActivity(new Intent(this, AdminLoginActivity.class));
                                                        finish();
                                                    })
                                                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to send verification email", Toast.LENGTH_SHORT).show());
                                        })
                                        .addOnFailureListener(e -> Toast.makeText(this, "Failed to save data: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                            }
                        } else {
                            Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }
}


