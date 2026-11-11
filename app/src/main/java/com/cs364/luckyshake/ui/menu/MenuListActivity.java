package com.cs364.luckyshake.ui.menu;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cs364.luckyshake.R;
import com.cs364.luckyshake.data.Menu;
import com.cs364.luckyshake.data.MenuRepository;
import com.cs364.luckyshake.data.MenuSet;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

/**
 * Issue #22: shows the dishes of the selected menu set. The user can switch the set,
 * search by name, and delete a dish after a confirmation.
 */
public class MenuListActivity extends AppCompatActivity {
    private static final String STATE_SET_ID = "state_set_id";

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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_menu_list);

        root = findViewById(R.id.menu_list_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
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

    /**
     * Opens the add (menu == null) or edit form.
     * TODO(#23): launch MenuFormActivity with an ActivityResultLauncher and pass the menu id;
     * refresh the list when it returns RESULT_OK. Until then this only shows a message.
     */
    private void openMenuForm(@Nullable Menu menu) {
        Snackbar.make(root, R.string.menu_list_msg_form_todo, Snackbar.LENGTH_SHORT).show();
    }

    private boolean isThai() {
        return "th".equals(getResources().getConfiguration().getLocales().get(0).getLanguage());
    }
}
