package com.equipe7.eductrack;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class RoleSelectionActivity extends AppCompatActivity {

    private LinearLayout adminLayout, teacherLayout, parentLayout;
    private TextView tvCreateAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.selected_the_account);

        // Récupération des vues
        adminLayout = findViewById(R.id.adminLayout);
        teacherLayout = findViewById(R.id.teacherLayout);
        parentLayout = findViewById(R.id.parentLayout);
        tvCreateAccount = findViewById(R.id.tvRegister);

        // Clic sur Admin
        adminLayout.setOnClickListener(v -> {
            Intent intent = new Intent(RoleSelectionActivity.this, AdminLoginActivity.class);
            startActivity(intent);
        });

        // Clic sur Teacher
        teacherLayout.setOnClickListener(v -> {
            Intent intent = new Intent(RoleSelectionActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        // Clic sur Parent
        parentLayout.setOnClickListener(v -> {
            Intent intent = new Intent(RoleSelectionActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        // Rendre "Create One" cliquable
        String fullText = "Don’t have an account? Create One";
        SpannableString spannableString = new SpannableString(fullText);

        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                Intent intent = new Intent(RoleSelectionActivity.this, RegisterActivity.class);
                startActivity(intent);
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds);
                ds.setColor(Color.BLUE); // couleur du lien
                ds.setUnderlineText(true); // souligné
            }
        };

        // "Create One" commence à l'index 24
        spannableString.setSpan(clickableSpan, 24, fullText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        tvCreateAccount.setText(spannableString);
        tvCreateAccount.setMovementMethod(LinkMovementMethod.getInstance());
        tvCreateAccount.setHighlightColor(Color.TRANSPARENT);
    }
}
