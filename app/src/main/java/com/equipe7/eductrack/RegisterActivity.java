package com.equipe7.eductrack;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.equipe7.eductrack.CreateAdminAccountActivity;
import com.equipe7.eductrack.databinding.ActivityParentRegisterBinding;

public class RegisterActivity extends AppCompatActivity {

    private LinearLayout adminLayout, parentLayout;
    private TextView tvAlreadyHaveAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register_activity);

        adminLayout = findViewById(R.id.adminLayout);
        parentLayout = findViewById(R.id.parentLayout);
        tvAlreadyHaveAccount = findViewById(R.id.tvAlreadyHaveAccount);

        // Redirection selon le rôle choisi
        adminLayout.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, CreateAdminAccountActivity.class);
            startActivity(intent);
        });

        parentLayout.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, ActivityParentRegister.class);
            startActivity(intent);
        });

        // Lien vers LoginActivity
        setLoginTextWithLink();
    }

    private void setLoginTextWithLink() {
        String fullText = "Already have an account? Log in";
        SpannableString spannableString = new SpannableString(fullText);

        spannableString.setSpan(
                new ForegroundColorSpan(Color.BLACK),
                0,
                24,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        ClickableSpan loginClickable = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            }
        };

        spannableString.setSpan(loginClickable, 25, fullText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(new ForegroundColorSpan(Color.BLUE), 25, fullText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        tvAlreadyHaveAccount.setText(spannableString);
        tvAlreadyHaveAccount.setMovementMethod(LinkMovementMethod.getInstance());
    }
}
