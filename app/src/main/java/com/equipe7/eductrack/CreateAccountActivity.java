package com.equipe7.eductrack;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

public class CreateAccountActivity extends AppCompatActivity {

    private EditText etFirstName, etLastName, etEmailPhone, etPassword, etConfirmPassword;
    private Spinner spinnerRelation, spinnerSchool, spinnerNurseryClass;
    private CheckBox cbShowPassword, cbTerms;
    private TextView tvTerms;
    private Button btnRegister;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_student_account);

        // Init views
        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etEmailPhone = findViewById(R.id.etEmailPhone);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        spinnerRelation = findViewById(R.id.spinnerRelation);
        spinnerSchool = findViewById(R.id.spinnerSchool);
        spinnerNurseryClass = findViewById(R.id.spinnerNurseryClass);
        cbShowPassword = findViewById(R.id.cbShowPassword);
        cbTerms = findViewById(R.id.cbTerms);
        tvTerms = findViewById(R.id.tvTerms);
        btnRegister = findViewById(R.id.btnRegister);

        // Show/hide password
        cbShowPassword.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int inputType = isChecked ?
                    InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD :
                    InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD;

            etPassword.setInputType(inputType);
            etConfirmPassword.setInputType(inputType);

            etPassword.setSelection(etPassword.getText().length());
            etConfirmPassword.setSelection(etConfirmPassword.getText().length());
        });

        // On Terms of Use text click
        tvTerms.setOnClickListener(v -> {
            Intent intent = new Intent(CreateAccountActivity.this, TermsOfUseActivity.class);
            startActivity(intent);
        });

        // Register button
        btnRegister.setOnClickListener(v -> {
            if (!cbTerms.isChecked()) {
                Toast.makeText(this, "You must accept the Conditions of Use", Toast.LENGTH_SHORT).show();
                return;
            }

            // Continue registration process here...
            Toast.makeText(this, "Registration success (simulate)", Toast.LENGTH_SHORT).show();
        });
    }
}
