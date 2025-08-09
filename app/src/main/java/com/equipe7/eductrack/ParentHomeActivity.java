package com.equipe7.eductrack;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class ParentHomeActivity extends AppCompatActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Create a simple layout programmatically
        TextView textView = new TextView(this);
        textView.setText("Welcome Parent!\nThis is the Parent Home Activity.");
        textView.setTextSize(18);
        textView.setPadding(50, 50, 50, 50);
        
        setContentView(textView);
    }
}
