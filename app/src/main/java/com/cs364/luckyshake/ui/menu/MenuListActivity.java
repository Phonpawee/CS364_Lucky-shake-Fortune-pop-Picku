package com.cs364.luckyshake.ui.menu;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cs364.luckyshake.R;
import com.cs364.luckyshake.data.Menu;
import com.cs364.luckyshake.data.MenuRepository;
import com.cs364.luckyshake.data.MenuSet;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Issue #22: shows the dishes of the selected menu set. The user can switch the set,
 * search by name, and delete a dish after a confirmation.
 *
 * <p>Issue #25: the same screen also manages the sets themselves: add, rename and delete
 * (with a warning that the dishes inside are deleted too; the last set cannot be deleted).
 */
public class MenuListActivity extends AppCompatActivity {
    private static final String STATE_SET_ID = "state_set_id";
    private static final int MAX_SET_NAME_LENGTH = 60;
    // The add/rename dialog is rebuilt after a rotation from these three values.
    private static final String STATE_DIALOG_OPEN = "state_set_dialog_open";
    private static final String STATE_DIALOG_SET_ID = "state_set_dialog_set_id";
    private static final String STATE_DIALOG_TEXT = "state_set_dialog_text";

    private MenuRepository menus;
    private MenuListAdapter adapter;
    private View root;
    private Spinner setSpinner;
    private EditText searchInput;
    private RecyclerView recycler;
    private View emptyGroup;
    private TextView emptyText;
    private View emptyAddButton;

    private List<MenuSet> sets = new ArrayList<>();
    private long currentSetId = -1;

    /** The add/rename dialog while it is on screen (null otherwise), so it can be saved. */
    @Nullable
    private AlertDialog setNameDialog;
    @Nullable
    private EditText setNameInput;
    private long setNameDialogSetId;   // 0 = adding a new set, otherwise the set being renamed

    /** Opens the add/edit form (#23); the list is refreshed when a dish was saved. */
    private final ActivityResultLauncher<Intent> formLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    refreshList();
                    Snackbar.make(root, R.string.menu_form_msg_saved, Snackbar.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_menu_list);

        root = findViewById(R.id.menu_list_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout()
                            | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        menus = new MenuRepository(this);
        setSpinner = findViewById(R.id.menu_list_set_spinner);
        searchInput = findViewById(R.id.menu_list_search);
        recycler = findViewById(R.id.menu_list_recycler);
        emptyGroup = findViewById(R.id.menu_list_empty_group);
        emptyText = findViewById(R.id.menu_list_empty_text);
        emptyAddButton = findViewById(R.id.menu_list_empty_add_button);

        if (savedInstanceState != null) {
            currentSetId = savedInstanceState.getLong(STATE_SET_ID, -1);
            if (savedInstanceState.getBoolean(STATE_DIALOG_OPEN, false)) {
                // A rotation closed the name dialog: open it again with what was typed.
                long dialogSetId = savedInstanceState.getLong(STATE_DIALOG_SET_ID, 0);
                String typed = savedInstanceState.getString(STATE_DIALOG_TEXT, "");
                MenuSet renaming = dialogSetId > 0 ? menus.getSet(dialogSetId) : null;
                if (dialogSetId == 0 || renaming != null) {
                    root.post(() -> showSetNameDialog(renaming, typed));
                }
            }
        }

        adapter = new MenuListAdapter(isThai(), new MenuListAdapter.Listener() {
            @Override
            public void onEdit(Menu menu) {
                openMenuForm(menu);
            }

            @Override
            public void onDelete(Menu menu) {
                confirmDelete(menu);
            }
        });
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);
        DividerItemDecoration divider =
                new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        divider.setDrawable(ContextCompat.getDrawable(this, R.drawable.divider_menu_item));
        recycler.addItemDecoration(divider);

        setSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position < 0 || position >= sets.size()) {
                    return;
                }
                long newId = sets.get(position).id;
                if (newId != currentSetId) {   // ignore the call that comes from our own setSelection
                    currentSetId = newId;
                    refreshList();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                refreshList();
            }
        });

        View.OnClickListener addListener = v -> openMenuForm(null);
        findViewById(R.id.menu_list_add_button).setOnClickListener(addListener);
        emptyAddButton.setOnClickListener(addListener);

        findViewById(R.id.menu_sets_btn_add).setOnClickListener(v -> showSetNameDialog(null));
        findViewById(R.id.menu_sets_btn_rename).setOnClickListener(v -> {
            MenuSet set = currentSet();
            if (set == null) {
                Snackbar.make(root, R.string.menu_list_msg_empty_no_set, Snackbar.LENGTH_SHORT).show();
            } else {
                showSetNameDialog(set);
            }
        });
        findViewById(R.id.menu_sets_btn_delete).setOnClickListener(v -> confirmDeleteSet());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Data may have changed while another screen was open (e.g. the form or sets screen).
        reloadSets();
        refreshList();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(STATE_SET_ID, currentSetId);
        if (setNameDialog != null && setNameInput != null) {
            outState.putBoolean(STATE_DIALOG_OPEN, true);
            outState.putLong(STATE_DIALOG_SET_ID, setNameDialogSetId);
            outState.putString(STATE_DIALOG_TEXT, setNameInput.getText().toString());
        }
    }

    /** Fills the set picker and keeps the current set selected when it still exists. */
    private void reloadSets() {
        sets = menus.getSets();
        boolean thai = isThai();
        List<String> names = new ArrayList<>();
        int selected = 0;
        boolean found = false;
        for (int i = 0; i < sets.size(); i++) {
            MenuSet s = sets.get(i);
            names.add(s.getName(thai));
            if (s.id == currentSetId) {
                selected = i;
                found = true;
            }
        }
        ArrayAdapter<String> spinnerAdapter =
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        setSpinner.setAdapter(spinnerAdapter);

        if (sets.isEmpty()) {
            currentSetId = -1;
        } else {
            if (!found) {
                selected = 0;
            }
            currentSetId = sets.get(selected).id;
            setSpinner.setSelection(selected);
        }
    }

    /** Loads the dishes of the current set (filtered by the search text) into the list. */
    private void refreshList() {
        String query = searchInput.getText().toString();
        boolean searching = !query.trim().isEmpty();

        List<Menu> list = currentSetId < 0
                ? new ArrayList<>()
                : menus.getMenus(currentSetId, null, query);
        adapter.submitList(list);

        boolean empty = list.isEmpty();
        recycler.setVisibility(empty ? View.GONE : View.VISIBLE);
        emptyGroup.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty) {
            if (currentSetId < 0) {
                emptyText.setText(R.string.menu_list_msg_empty_no_set);
            } else if (searching) {
                emptyText.setText(R.string.menu_list_msg_empty_search);
            } else {
                emptyText.setText(R.string.menu_list_msg_empty_set);
            }
            // The "add" button inside the empty state only makes sense for an empty set.
            emptyAddButton.setVisibility(currentSetId >= 0 && !searching ? View.VISIBLE : View.GONE);
        }
    }

    private void confirmDelete(Menu menu) {
        String name = menu.getName(isThai());
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.menu_list_title_delete)
                .setMessage(getString(R.string.menu_list_msg_delete_confirm, name))
                .setNegativeButton(R.string.common_btn_cancel, null)
                .setPositiveButton(R.string.menu_list_btn_delete, (dialog, which) -> {
                    if (menus.deleteMenu(menu.id)) {
                        refreshList();
                        Snackbar.make(root, R.string.menu_list_msg_deleted, Snackbar.LENGTH_SHORT).show();
                    } else {
                        Snackbar.make(root, R.string.menu_list_error_delete_failed, Snackbar.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    // ------------------------------------------------------------------ sets (#25)

    /** The set that is selected right now, or null when there is none. */
    @Nullable
    private MenuSet currentSet() {
        for (MenuSet s : sets) {
            if (s.id == currentSetId) {
                return s;
            }
        }
        return null;
    }

    /**
     * Asks for a set name. {@code editing == null} adds a new set, otherwise it renames that
     * one. Errors are shown inside the dialog, which stays open until the name is valid.
     */
    private void showSetNameDialog(@Nullable MenuSet editing) {
        showSetNameDialog(editing, null);
    }

    /** @param initialText what to put in the field (after a rotation), or null for the default */
    private void showSetNameDialog(@Nullable MenuSet editing, @Nullable String initialText) {
        View content = getLayoutInflater().inflate(R.layout.dialog_menu_set_name, null);
        TextInputLayout nameLayout = content.findViewById(R.id.menu_sets_name_layout);
        EditText nameInput = content.findViewById(R.id.menu_sets_name_input);

        // Like the dish form: edit the name that is shown on screen.
        boolean editsEnglish = editing != null && !isThai()
                && editing.nameEn != null && !editing.nameEn.isEmpty();
        if (initialText != null) {
            nameInput.setText(initialText);
            nameInput.setSelection(nameInput.length());
        } else if (editing != null) {
            nameInput.setText(editsEnglish ? editing.nameEn : editing.nameTh);
            nameInput.setSelection(nameInput.length());
        }
        nameInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                nameLayout.setError(null);
            }
        });

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(editing == null ? R.string.menu_sets_title_add : R.string.menu_sets_title_rename)
                .setView(content)
                .setNegativeButton(R.string.common_btn_cancel, null)
                .setPositiveButton(R.string.common_btn_save, null)   // real handler below
                .create();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        setNameDialog = dialog;
        setNameInput = nameInput;
        setNameDialogSetId = editing == null ? 0 : editing.id;
        dialog.setOnDismissListener(d -> {
            setNameDialog = null;
            setNameInput = null;
        });
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String name = nameInput.getText().toString().trim();
                    String error = validateSetName(name, editing == null ? 0 : editing.id);
                    nameLayout.setError(error);
                    if (error != null) {
                        nameInput.requestFocus();
                        return;
                    }
                    if (editing == null) {
                        addSet(name);
                    } else {
                        renameSet(editing, name, editsEnglish);
                    }
                    dialog.dismiss();
                }));
        dialog.show();
    }

    /** Returns an error message for the set name, or null when it is fine. */
    @Nullable
    private String validateSetName(String name, long excludeId) {
        if (name.isEmpty()) {
            return getString(R.string.menu_sets_error_name_empty);
        }
        if (name.length() > MAX_SET_NAME_LENGTH) {
            return getString(R.string.menu_sets_error_name_too_long, MAX_SET_NAME_LENGTH);
        }
        String wanted = foldName(name);
        for (MenuSet s : sets) {
            if (s.id == excludeId) {
                continue;
            }
            String en = foldName(s.nameEn);
            if (wanted.equals(foldName(s.nameTh)) || (!en.isEmpty() && wanted.equals(en))) {
                return getString(R.string.menu_sets_error_name_duplicate);
            }
        }
        return null;
    }

    /** Same comparison rule as MenuRepository.nameExists: trimmed, case-insensitive. */
    private static String foldName(@Nullable String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    private void addSet(String name) {
        long id = menus.addSet(name, "");
        if (id <= 0) {
            Snackbar.make(root, R.string.menu_sets_error_save_failed, Snackbar.LENGTH_SHORT).show();
            return;
        }
        currentSetId = id;          // jump to the new set
        searchInput.setText("");    // an old search would hide the (empty) new set
        reloadSets();
        refreshList();
        Snackbar.make(root, R.string.menu_sets_msg_added, Snackbar.LENGTH_SHORT).show();
    }

    private void renameSet(MenuSet set, String name, boolean editsEnglish) {
        String th = editsEnglish ? set.nameTh : name;
        String en = editsEnglish ? name : (set.nameEn == null ? "" : set.nameEn);
        if (menus.renameSet(set.id, th, en)) {
            reloadSets();
            refreshList();
            Snackbar.make(root, R.string.menu_sets_msg_renamed, Snackbar.LENGTH_SHORT).show();
        } else {
            Snackbar.make(root, R.string.menu_sets_error_save_failed, Snackbar.LENGTH_SHORT).show();
        }
    }

    /** Warns how many dishes will go with the set; the last set can never be deleted. */
    private void confirmDeleteSet() {
        MenuSet set = currentSet();
        if (set == null) {
            Snackbar.make(root, R.string.menu_list_msg_empty_no_set, Snackbar.LENGTH_SHORT).show();
            return;
        }
        if (menus.countSets() <= 1) {
            Snackbar.make(root, R.string.menu_sets_error_last_set, Snackbar.LENGTH_LONG).show();
            return;
        }
        int count = menus.countMenus(set.id, null);
        String name = set.getName(isThai());
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.menu_sets_title_delete)
                .setMessage(getResources().getQuantityString(
                        R.plurals.menu_sets_msg_delete_confirm, count, name, count))
                .setNegativeButton(R.string.common_btn_cancel, null)
                .setPositiveButton(R.string.menu_list_btn_delete, (d, which) -> deleteSet(set))
                .show();
    }

    private void deleteSet(MenuSet set) {
        if (menus.countSets() <= 1) {   // re-check: the data may have changed
            Snackbar.make(root, R.string.menu_sets_error_last_set, Snackbar.LENGTH_LONG).show();
            return;
        }
        // The dishes inside are removed by ON DELETE CASCADE; history is kept.
        if (menus.deleteSet(set.id)) {
            currentSetId = -1;      // reloadSets() then picks the first remaining set
            reloadSets();
            refreshList();
            Snackbar.make(root, R.string.menu_sets_msg_deleted, Snackbar.LENGTH_SHORT).show();
        } else {
            Snackbar.make(root, R.string.menu_sets_error_delete_failed, Snackbar.LENGTH_SHORT).show();
        }
    }

    /** Opens the add (menu == null) or edit form (issue #23). */
    private void openMenuForm(@Nullable Menu menu) {
        if (menu == null && currentSetId < 0) {
            // A new dish needs a set to live in.
            Snackbar.make(root, R.string.menu_list_msg_empty_no_set, Snackbar.LENGTH_SHORT).show();
            return;
        }
        formLauncher.launch(MenuFormActivity.newIntent(this, currentSetId, menu));
    }

    private boolean isThai() {
        return "th".equals(getResources().getConfiguration().getLocales().get(0).getLanguage());
    }
}
