package com.equipe7.eductrack;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.equipe7.eductrack.MainActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class TermsOfUseActivity extends AppCompatActivity {

    private TextView tvTerms;
    private Button btnAccept;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terms_of_use);

        tvTerms = findViewById(R.id.tv_terms);
        btnAccept = findViewById(R.id.btn_accept);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        tvTerms.setText(Html.fromHtml(getFormattedTerms()));

        btnAccept.setOnClickListener(view -> saveAcceptanceToFirebase());
    }

    private String getFormattedTerms() {
        return "<font color='red'><b>📘 1. Terms of Use (Conditions of Use)</b></font><br><br>" +
                "EduTrack – User Terms & Conditions<br>" +
                "Last updated: July 2025<br>" +
                "Welcome to EduTrack, an educational mobile application designed to help track student performance, attendance, and communication between teachers, parents, and school administrators.<br><br>" +

                "<font color='blue'><b>1. Acceptance of Terms</b></font><br>" +
                "By accessing or using EduTrack, you agree to comply with these Terms of Use. If you do not agree, please do not use the app.<br><br>" +

                "<font color='blue'><b>2. User Roles and Access</b></font><br>" +
                "EduTrack supports three types of users:<br>" +
                "- Teachers: Manage student data, record grades, attendance, and provide feedback.<br>" +
                "- Parents: View their child’s academic records and communicate with teachers.<br>" +
                "- Administrators: Manage users, classes, and general platform settings.<br>" +
                "Each user must access only the information they are authorized to see.<br><br>" +

                "<font color='blue'><b>3. Account Responsibility</b></font><br>" +
                "Users must:<br>" +
                "- Provide accurate information during registration.<br>" +
                "- Keep their login credentials confidential.<br>" +
                "- Report any unauthorized account use.<br>" +
                "EduTrack is not responsible for misuse due to shared or leaked credentials.<br><br>" +

                "<font color='blue'><b>4. Acceptable Use</b></font><br>" +
                "You agree not to:<br>" +
                "- Impersonate other users.<br>" +
                "- Share false or misleading data.<br>" +
                "- Access or attempt to access accounts that are not yours.<br>" +
                "- Use the app to harass, insult, or harm others.<br>" +
                "- Upload malware or engage in hacking.<br>" +
                "Violation may result in account suspension or legal action.<br><br>" +

                "<font color='blue'><b>5. Modifications to the Terms</b></font><br>" +
                "EduTrack reserves the right to update these Terms at any time. Changes will be communicated through the app or official communication channels.<br><br>" +

                "<font color='blue'><b>6. Limitation of Liability</b></font><br>" +
                "EduTrack is provided “as is.” We are not responsible for:<br>" +
                "- Loss of data due to user actions or device failure<br>" +
                "- Unforeseen technical problems<br>" +
                "- Unauthorized access resulting from user negligence";
    }

    private void saveAcceptanceToFirebase() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Utilisateur non connecté", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        Map<String, Object> data = new HashMap<>();
        data.put("acceptedTerms", true);

        db.collection("users")
                .document(userId)
                .update(data)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Conditions acceptées", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(TermsOfUseActivity.this, MainActivity.BindServiceFlags.class);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erreur lors de l'enregistrement", Toast.LENGTH_SHORT).show();
                });
    }
}
