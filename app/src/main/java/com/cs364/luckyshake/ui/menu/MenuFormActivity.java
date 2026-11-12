package com.cs364.luckyshake.ui.menu;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.cs364.luckyshake.R;
import com.cs364.luckyshake.data.Menu;
import com.cs364.luckyshake.data.MenuRepository;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

/**
 * Issue #23: add or edit one dish (name, type, tags). The weight is not edited here.
 *
 * <p>Open it with {@link #newIntent(Context, long, Menu)} and start it with an
 * ActivityResultLauncher; it returns RESULT_OK (with EXTRA_MENU_ID) after a successful save.
 *
 * <p>Hooks left for other issues:
 * <ul>
 *   <li>#24: {@link #validateForm()} (empty / duplicate name) — currently accepts everything.</li>
 *   <li>#14: Back and Cancel ask before discarding ({@link #requestExit()}); swap in the
 *       shared confirmation dialog there if the team wants one.</li>
 * </ul>
 */
public class MenuFormActivity extends AppCompatActivity {
    public static final String EXTRA_MENU_ID = "extra_menu_id";
    public static final String EXTRA_SET_ID = "extra_set_id";

    private static final String STATE_INITIAL_NAME = "state_initial_name";
    private static final String STATE_INITIAL_TYPE = "state_initial_type";
    private static final String STATE_INITIAL_TAGS = "state_initial_tags";

    /** Order of the type picker. */
    private static final String[] TYPES = {
            Menu.TYPE_RICE, Menu.TYPE_NOODLE, Menu.TYPE_DESSERT, Menu.TYPE_DRINK, Menu.TYPE_OTHER
    };

    private MenuRepository menus;
    private View root;
    private EditText nameInput;
    private Spinner typeSpinner;
    private CheckBox tagSpicy;
    private CheckBox tagFried;
    private CheckBox tagVeggie;
    private View saveButton;

    private long setId = -1;
    /** The dish being edited, or null when adding a new one. */
    private Menu editing;

    // What the form held when it opened; hasUnsavedChanges() compares against this.
    private String initialName = "";
    private int initialType = 0;
    private int initialTags = 0;

    /**
     * @param setId the set a new dish is added to (ignored when editing)
     * @param menu  the dish to edit, or null to add a new one
     */
    public static Intent newIntent(Context context, long setId, @Nullable Menu menu) {
        Intent intent = new Intent(context, MenuFormActivity.class);
        intent.putExtra(EXTRA_SET_ID, setId);
        intent.putExtra(EXTRA_MENU_ID, menu == null ? 0L : menu.id);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_menu_form);

        root = findViewById(R.id.menu_form_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout()
                            | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        menus = new MenuRepository(this);
        nameInput = findViewById(R.id.menu_form_name);
        typeSpinner = findViewById(R.id.menu_form_type);
        tagSpicy = findViewById(R.id.menu_form_tag_spicy);
        tagFried = findViewById(R.id.menu_form_tag_fried);
        tagVeggie = findViewById(R.id.menu_form_tag_veggie);
        saveButton = findViewById(R.id.menu_form_save);
        TextView title = findViewById(R.id.menu_form_title);

        setId = getIntent().getLongExtra(EXTRA_SET_ID, -1);
        long menuId = getIntent().getLongExtra(EXTRA_MENU_ID, 0);
        if (menuId > 0) {
            editing = menus.getMenu(menuId);
            if (editing == null) {          // deleted in the meantime
                setResult(RESULT_CANCELED);
                finish();
                return;
            }
            setId = editing.setId;
        }
        title.setText(editing == null ? R.string.menu_form_title_add : R.string.menu_form_title_edit);

        List<String> typeNames = new ArrayList<>();
        for (String type : TYPES) {
            typeNames.add(MenuLabels.type(this, type));
        }
        ArrayAdapter<String> typeAdapter =
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, typeNames);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(typeAdapter);

        if (savedInstanceState == null) {
            fillFromDish();
            initialName = currentName();
            initialType = currentTypeIndex();
            initialTags = currentTags();
        } else {
            // The views restore their own text / selection / checks (they all have ids);
            // only the "what was it when opened" snapshot needs to be restored by hand.
            initialName = savedInstanceState.getString(STATE_INITIAL_NAME, "");
            initialType = savedInstanceState.getInt(STATE_INITIAL_TYPE, 0);
            initialTags = savedInstanceState.getInt(STATE_INITIAL_TAGS, 0);
        }

        // Back and Cancel share one exit path, so unsaved changes are never dropped silently.
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                requestExit();
            }
        });
        findViewById(R.id.menu_form_cancel).setOnClickListener(v -> requestExit());
        saveButton.setOnClickListener(v -> save());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_INITIAL_NAME, initialName);
        outState.putInt(STATE_INITIAL_TYPE, initialType);
        outState.putInt(STATE_INITIAL_TAGS, initialTags);
    }

    /** True when the user has typed or changed something that is not saved yet (for #14). */
    public boolean hasUnsavedChanges() {
        return !currentName().equals(initialName)
                || currentTypeIndex() != initialType
                || currentTags() != initialTags;
    }

    /**
     * Leaves the form. Asks first when there are unsaved changes; with nothing to lose it
     * closes at once. Local dialog for now: #14 may swap it for the shared confirmation dialog.
     */
    private void requestExit() {
        if (!hasUnsavedChanges()) {
            setResult(RESULT_CANCELED);
            finish();
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.menu_form_title_discard)
                .setMessage(R.string.menu_form_msg_discard)
                .setNegativeButton(R.string.menu_form_btn_keep, null)
                .setPositiveButton(R.string.menu_form_btn_discard, (dialog, which) -> {
                    setResult(RESULT_CANCELED);
                    finish();
                })
                .show();
    }

    /**
     * Hook for #24: check the form (empty name, duplicate name via
     * MenuRepository.nameExists) and show the errors. Return false to stop the save.
     */
    private boolean validateForm() {
        return true;
    }

    private void save() {
        if (!validateForm()) {
            return;
        }
        String name = currentName();
        Menu m = editing != null ? editing : new Menu();
        if (editsEnglishName()) {
            m.nameEn = name;
        } else {
            m.nameTh = name;
        }
        m.type = TYPES[currentTypeIndex()];
        m.tags = currentTags();

        boolean ok;
        if (editing != null) {
            ok = menus.updateMenu(m);
        } else {
            m.setId = setId;
            ok = setId >= 0 && menus.addMenu(m) > 0;
        }

        if (ok) {
            saveButton.setEnabled(false);   // avoid a second tap while the screen closes
            Intent data = new Intent().putExtra(EXTRA_MENU_ID, m.id);
            setResult(RESULT_OK, data);
            finish();
        } else {
            Snackbar.make(root, R.string.menu_form_error_save_failed, Snackbar.LENGTH_SHORT).show();
        }
    }

    /** Puts the dish being edited (or the defaults for a new one) into the views. */
    private void fillFromDish() {
        if (editing == null) {
            nameInput.setText("");
            typeSpinner.setSelection(0);
            return;
        }
        nameInput.setText(editsEnglishName() ? editing.nameEn : editing.nameTh);
        typeSpinner.setSelection(indexOfType(editing.type));
        tagSpicy.setChecked(editing.hasTag(Menu.TAG_SPICY));
        tagFried.setChecked(editing.hasTag(Menu.TAG_FRIED));
        tagVeggie.setChecked(editing.hasTag(Menu.TAG_VEGGIE));
    }

    /**
     * The form has one name field. It edits the name the list shows: the English name when the
     * app is in English and the dish has one, otherwise the Thai name. New dishes are stored in
     * the Thai-name column (the repository requires it); their English name stays empty.
     */
    private boolean editsEnglishName() {
        return editing != null && !isThai()
                && editing.nameEn != null && !editing.nameEn.isEmpty();
    }

    private String currentName() {
        return nameInput.getText().toString().trim();
    }

    private int currentTypeIndex() {
        int pos = typeSpinner.getSelectedItemPosition();
        return pos < 0 ? 0 : pos;
    }

    private int currentTags() {
        int tags = 0;
        if (tagSpicy.isChecked()) {
            tags |= Menu.TAG_SPICY;
        }
        if (tagFried.isChecked()) {
            tags |= Menu.TAG_FRIED;
        }
        if (tagVeggie.isChecked()) {
            tags |= Menu.TAG_VEGGIE;
        }
        return tags;
    }

    private static int indexOfType(String type) {
        for (int i = 0; i < TYPES.length; i++) {
            if (TYPES[i].equals(type)) {
                return i;
            }
        }
        return TYPES.length - 1;   // unknown type -> "other"
    }

    private boolean isThai() {
        return "th".equals(getResources().getConfiguration().getLocales().get(0).getLanguage());
    }
}
