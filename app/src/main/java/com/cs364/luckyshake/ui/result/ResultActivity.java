package com.cs364.luckyshake.ui.result;

import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.cs364.luckyshake.R;

public class ResultActivity extends AppCompatActivity {
    public static final String EXTRA_MENU_NAME = "com.cs364.luckyshake.extra.MENU_NAME";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int menu = getIntent().getIntExtra(EXTRA_MENU_NAME, 0);
        if (menu == 0) {
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_result);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.result_root), (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        TextView result = findViewById(R.id.result_label_menu);
        result.setText(getString(R.string.result_label_menu, getString(menu)));
        findViewById(R.id.result_btn_home).setOnClickListener(view -> finish());
    }
}
