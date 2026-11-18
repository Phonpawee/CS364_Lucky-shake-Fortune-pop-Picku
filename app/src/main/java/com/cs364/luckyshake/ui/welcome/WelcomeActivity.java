package com.cs364.luckyshake.ui.welcome;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.cs364.luckyshake.MainActivity;
import com.cs364.luckyshake.R;
import com.cs364.luckyshake.ui.settings.FontSizeHelper;

/** Illustrated app entrance; it never creates or changes a draw. */
public class WelcomeActivity extends AppCompatActivity {
    private boolean entering;
    private SakuraPetalsView petals;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_welcome);
        petals = findViewById(R.id.welcome_petals);
        View content = findViewById(R.id.welcome_content);
        if (content == null) content = findViewById(R.id.welcome_landscape_content);
        int left = content.getPaddingLeft(), top = content.getPaddingTop();
        int right = content.getPaddingRight(), bottom = content.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(content, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(left + bars.left, top + bars.top, right + bars.right, bottom + bars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(content);
        FontSizeHelper.apply(this, content);
        findViewById(R.id.welcome_start).setOnClickListener(view -> {
            if (entering) return;
            entering = true;
            view.setEnabled(false);
            startActivity(new Intent(this, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
            finish();
        });
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (petals != null) petals.setActive(true);
    }

    @Override
    protected void onPause() {
        if (petals != null) petals.setActive(false);
        super.onPause();
    }

}
