package com.cs364.luckyshake.data;
import com.cs364.luckyshake.logic.GameConfig;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Creates the SQLite database (3 tables) and fills it with the starter menus.
 * Repositories (MenuRepository / HistoryRepository) use the constants below.
 */
public class DbHelper extends SQLiteOpenHelper {
    public static final String DB_NAME = "luckyshake.db";
    public static final int DB_VERSION = 1;

    // Keep in sync with the default weight in GameConfig (issue #1).
    public static final int DEFAULT_WEIGHT = GameConfig.DEFAULT_WEIGHT;

    // Table: menu_set
    public static final String T_SET = "menu_set";
    public static final String C_SET_ID = "id";
    public static final String C_SET_NAME_TH = "name_th";
    public static final String C_SET_NAME_EN = "name_en";

    // Table: menu
    public static final String T_MENU = "menu";
    public static final String C_MENU_ID = "id";
    public static final String C_MENU_SET_ID = "set_id";
    public static final String C_MENU_NAME_TH = "name_th";
    public static final String C_MENU_NAME_EN = "name_en";
    public static final String C_MENU_TYPE = "type";
    public static final String C_MENU_TAGS = "tags";
    public static final String C_MENU_WEIGHT = "weight";
    public static final String C_MENU_LAST_TOUCHED = "last_touched";
    public static final String C_MENU_CREATED_AT = "created_at";

    // Table: history
    public static final String T_HISTORY = "history";
    public static final String C_HIST_ID = "id";
    public static final String C_HIST_MENU_ID = "menu_id";
    public static final String C_HIST_NAME_TH = "name_th";
    public static final String C_HIST_NAME_EN = "name_en";
    public static final String C_HIST_LEVEL = "level";
    public static final String C_HIST_STICK_NO = "stick_no";
    public static final String C_HIST_STATUS = "status";
    public static final String C_HIST_MODE = "mode";
    public static final String C_HIST_CREATED_AT = "created_at";

    private static final String CREATE_SET =
            "CREATE TABLE " + T_SET + " ("
                    + C_SET_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + C_SET_NAME_TH + " TEXT NOT NULL, "
                    + C_SET_NAME_EN + " TEXT NOT NULL DEFAULT '')";

    // Deleting a set also deletes its menus (ON DELETE CASCADE).
    private static final String CREATE_MENU =
            "CREATE TABLE " + T_MENU + " ("
                    + C_MENU_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + C_MENU_SET_ID + " INTEGER NOT NULL REFERENCES " + T_SET + "(" + C_SET_ID + ") ON DELETE CASCADE, "
                    + C_MENU_NAME_TH + " TEXT NOT NULL, "
                    + C_MENU_NAME_EN + " TEXT NOT NULL DEFAULT '', "
                    + C_MENU_TYPE + " TEXT NOT NULL, "
                    + C_MENU_TAGS + " INTEGER NOT NULL DEFAULT 0, "
                    + C_MENU_WEIGHT + " INTEGER NOT NULL DEFAULT " + DEFAULT_WEIGHT + ", "
                    + C_MENU_LAST_TOUCHED + " INTEGER NOT NULL, "
                    + C_MENU_CREATED_AT + " INTEGER NOT NULL)";

    // Deleting a menu keeps its history rows (menu_id becomes NULL).
    private static final String CREATE_HISTORY =
            "CREATE TABLE " + T_HISTORY + " ("
                    + C_HIST_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + C_HIST_MENU_ID + " INTEGER REFERENCES " + T_MENU + "(" + C_MENU_ID + ") ON DELETE SET NULL, "
                    + C_HIST_NAME_TH + " TEXT NOT NULL, "
                    + C_HIST_NAME_EN + " TEXT NOT NULL DEFAULT '', "
                    + C_HIST_LEVEL + " INTEGER NOT NULL, "
                    + C_HIST_STICK_NO + " INTEGER NOT NULL, "
                    + C_HIST_STATUS + " TEXT NOT NULL, "
                    + C_HIST_MODE + " TEXT NOT NULL, "
                    + C_HIST_CREATED_AT + " INTEGER NOT NULL)";

    private static DbHelper instance;

    /** One shared helper for the whole app (Android recommends a single instance). */
    public static synchronized DbHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DbHelper(context.getApplicationContext());
        }
        return instance;
    }

    /** For tests: closes the shared helper so the database file can be deleted. */
    public static synchronized void closeInstance() {
        if (instance != null) {
            instance.close();
            instance = null;
        }
    }

    public DbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        // SQLite ignores foreign keys unless this is switched on for every connection.
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_SET);
        db.execSQL(CREATE_MENU);
        db.execSQL(CREATE_HISTORY);
        db.execSQL("CREATE INDEX idx_menu_set ON " + T_MENU + "(" + C_MENU_SET_ID + ")");
        db.execSQL("CREATE INDEX idx_history_created ON " + T_HISTORY + "(" + C_HIST_CREATED_AT + ")");
        SeedData.insertAll(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Course project: no migrations. Drop everything and start again.
        db.execSQL("DROP TABLE IF EXISTS " + T_HISTORY);
        db.execSQL("DROP TABLE IF EXISTS " + T_MENU);
        db.execSQL("DROP TABLE IF EXISTS " + T_SET);
        onCreate(db);
    }
}
