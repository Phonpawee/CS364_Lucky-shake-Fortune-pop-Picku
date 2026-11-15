package com.cs364.luckyshake.fortune;

import android.content.Context;

import com.cs364.luckyshake.R;

public class FortuneProvider {
    private final Context context;

    private static final int[][] FORTUNES = {
            {R.string.fortune_msg_level_1_1, R.string.fortune_msg_level_1_2, R.string.fortune_msg_level_1_3},
            {R.string.fortune_msg_level_2_1, R.string.fortune_msg_level_2_2, R.string.fortune_msg_level_2_3},
            {R.string.fortune_msg_level_3_1, R.string.fortune_msg_level_3_2, R.string.fortune_msg_level_3_3},
            {R.string.fortune_msg_level_4_1, R.string.fortune_msg_level_4_2, R.string.fortune_msg_level_4_3}
    };

    public FortuneProvider(Context context) {
        this.context = context.getApplicationContext();
    }

    // level: 1–4, index: 0–2
    public String getFortune(int level, int index, String menuName) {
        if (level < 1 || level > FORTUNES.length) {
            throw new IllegalArgumentException("level must be between 1 and 4");
        }
        if (index < 0 || index >= FORTUNES[level - 1].length) {
            throw new IllegalArgumentException("index must be between 0 and 2");
        }

        return context.getString(
                FORTUNES[level - 1][index],
                menuName == null ? "" : menuName
        );
    }

    public String getFoodTypeName(String key) {
        if (key == null) {
            return null;
        }

        int resourceId = context.getResources().getIdentifier(
                key, "string", context.getPackageName()
        );

        return resourceId == 0 ? null : context.getString(resourceId);
    }

    public String getDisclaimer() {
        return context.getString(R.string.result_msg_disclaimer);
    }
}