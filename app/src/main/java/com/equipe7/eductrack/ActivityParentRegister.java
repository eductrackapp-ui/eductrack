package com.equipe7.eductrack;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Patterns;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ActivityParentRegister extends AppCompatActivity {

    private EditText etFirstName, etLastName, etEmailPhone, etPassword, etConfirmPassword;
    private Spinner spinnerRelation, spinnerSchool;
    private CheckBox cbShowPassword;
    private Button btnNext;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_register);

        // Initialisation Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Liaison vues
        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etEmailPhone = findViewById(R.id.etEmailPhone);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        spinnerRelation = findViewById(R.id.spinnerRelation);
        spinnerSchool = findViewById(R.id.spinnerSchool);
        cbShowPassword = findViewById(R.id.cbShowPassword);
        btnNext = findViewById(R.id.btnNext);

        // Afficher / masquer mot de passe
        cbShowPassword.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                etPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                etConfirmPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            } else {
                etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                etConfirmPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
        });

        // Bouton NEXT
        btnNext.setOnClickListener(v -> registerParent());
    }

    private void registerParent() {
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String emailPhone = etEmailPhone.getText().toString().trim();
        String relation = spinnerRelation.getSelectedItem() != null ? spinnerRelation.getSelectedItem().toString() : "";
        String school = spinnerSchool.getSelectedItem() != null ? spinnerSchool.getSelectedItem().toString() : "";
        String password = etPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();

        // Validation
        if (TextUtils.isEmpty(firstName) || TextUtils.isEmpty(lastName) || TextUtils.isEmpty(emailPhone)
                || TextUtils.isEmpty(relation) || TextUtils.isEmpty(school)
                || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
            showToast("Please fill in all fields");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(emailPhone).matches()) {
            showToast("Please enter a valid email address");
            return;
        }

        if (password.length() < 6) {
            showToast("Password must be at least 6 characters");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showToast("Passwords do not match");
            return;
        }

        // Enregistrement sur Firebase Authentication
        mAuth.createUserWithEmailAndPassword(emailPhone, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String userId = mAuth.getCurrentUser().getUid();

                        // Préparer les données à stocker dans Firestore
                        Map<String, Object> parentData = new HashMap<>();
                        parentData.put("firstName", firstName);
                        parentData.put("lastName", lastName);
                        parentData.put("email", emailPhone);
                        parentData.put("relation", relation);
                        parentData.put("school", school);
                        parentData.put("role", "parent");

                        // Sauvegarder dans Firestore
                        db.collection("users").document(userId).set(parentData)
                                .addOnSuccessListener(aVoid -> {
                                    showToast("Parent account created successfully!");
                                    // Aller à la prochaine activité
                                    startActivity(new Intent(ActivityParentRegister.this, CreateAccountActivity.class));
                                    finish();
                                })
                                .addOnFailureListener(e -> showToast("Error saving data: " + e.getMessage()));
                    } else {
                        showToast("Error: " + task.getException().getMessage());
                    }
                });
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
