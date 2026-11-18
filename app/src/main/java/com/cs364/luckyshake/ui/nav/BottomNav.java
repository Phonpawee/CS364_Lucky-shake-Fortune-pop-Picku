package com.cs364.luckyshake.ui.nav;

import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.cs364.luckyshake.MainActivity;
import com.cs364.luckyshake.R;
import com.cs364.luckyshake.ui.group.GroupSetupActivity;
import com.cs364.luckyshake.ui.journal.JournalActivity;
import com.cs364.luckyshake.ui.menu.MenuListActivity;
import com.cs364.luckyshake.ui.settings.SettingsActivity;
import com.cs364.luckyshake.ui.settings.FontSizeHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.navigationrail.NavigationRailView;
import com.google.android.material.snackbar.Snackbar;

/**
 * Bottom tab bar shared by the main screens (Home, Menus, Group, Settings), like the web demo.
 *
 * <p>Call {@link #attach} in onCreate, after setContentView and after the screen's own
 * window-insets listener. It puts the screen's layout above the bar, so the layout files do not
 * change. Back on any tab other than Home goes to Home. To add a tab (for example the journal,
 * issue #40) add an item to res/menu/bottom_nav.xml and a case in {@link #intentFor}.
 */
public final class BottomNav {

    private BottomNav() {
    }

    /** Re-applies the saved text size to the bar (call after the user changes it). */
    public static void applyFontSize(AppCompatActivity activity) {
        View nav = activity.findViewById(R.id.bottom_nav);
        if (nav != null) {
            FontSizeHelper.apply(activity, nav);
        }
    }

    /** Anchors the snackbar above the visible bottom bar in portrait. */
    public static void show(AppCompatActivity activity, Snackbar snackbar) {
        View nav = activity.findViewById(R.id.bottom_nav);
        if (nav instanceof BottomNavigationView && nav.getVisibility() == View.VISIBLE) {
            snackbar.setAnchorView(nav);
        }
        snackbar.show();
    }

    public static void attach(AppCompatActivity activity, @IdRes int selectedId) {
        ViewGroup content = activity.findViewById(android.R.id.content);
        attach(activity, selectedId, content.getChildAt(0));
    }

    /** Allows a full-bleed backdrop while keeping the interactive content clear of system bars. */
    public static void attach(AppCompatActivity activity, @IdRes int selectedId, View insetTarget) {
        ViewGroup content = activity.findViewById(android.R.id.content);
        View root = content.getChildAt(0);
        content.removeView(root);

        LinearLayout container = new LinearLayout(activity);
        NavigationBarView nav = (NavigationBarView) activity.getLayoutInflater()
                .inflate(R.layout.view_bottom_nav, container, false);
        boolean sideRail = nav instanceof NavigationRailView;
        container.setOrientation(sideRail ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        nav.setBackgroundColor(ContextCompat.getColor(activity, R.color.home_paper));
        nav.setItemIconTintList(ContextCompat.getColorStateList(activity, R.color.home_nav_tint));
        nav.setItemTextColor(ContextCompat.getColorStateList(activity, R.color.home_nav_tint));
        nav.setItemActiveIndicatorColor(ContextCompat.getColorStateList(activity, R.color.home_selected));
        if (sideRail) {
            container.addView(nav);
            container.addView(root, new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        } else {
            container.addView(root, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
            container.addView(nav);
        }
        content.addView(container, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // The bar handles the bottom system inset itself. The screen keeps the other sides.
        // While the keyboard is open the bar is hidden and the screen makes room for the keyboard.
        final int left = insetTarget.getPaddingLeft();
        final int top = insetTarget.getPaddingTop();
        final int right = insetTarget.getPaddingRight();
        final int bottom = insetTarget.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(insetTarget, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            boolean imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime());
            int imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            nav.setVisibility(imeVisible ? View.GONE : View.VISIBLE);
            v.setPadding(left + bars.left, top + bars.top, right + bars.right,
                    bottom + (imeVisible ? imeBottom : sideRail ? bars.bottom : 0));
            return insets;
        });
        ViewCompat.requestApplyInsets(insetTarget);

        nav.setSelectedItemId(selectedId);
        // Selecting a tab makes Material reset its label size, so scale after that. Home stays
        // alive while other tabs open, so scale again every time the screen comes back.
        FontSizeHelper.apply(activity, nav);
        activity.getLifecycle().addObserver(new DefaultLifecycleObserver() {
            @Override
            public void onResume(@NonNull LifecycleOwner owner) {
                FontSizeHelper.apply(activity, nav);
            }
        });
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id != selectedId) {
                open(activity, id, selectedId);
            }
            return false;   // the new screen shows its own selected tab
        });

        if (selectedId != R.id.nav_home) {
            activity.getOnBackPressedDispatcher().addCallback(activity,
                    new OnBackPressedCallback(true) {
                        @Override
                        public void handleOnBackPressed() {
                            open(activity, R.id.nav_home, selectedId);
                        }
                    });
        }
    }

    private static void open(AppCompatActivity from, int targetId, int currentId) {
        from.startActivity(intentFor(from, targetId));
        if (currentId != R.id.nav_home) {
            from.finish();   // keep the stack as Home -> one tab
        }
    }

    private static Intent intentFor(AppCompatActivity from, int id) {
        Intent intent;
        if (id == R.id.nav_menu) {
            intent = new Intent(from, MenuListActivity.class);
        } else if (id == R.id.nav_group) {
            intent = GroupSetupActivity.newIntent(from);
        } else if (id == R.id.nav_journal) {
            intent = JournalActivity.newIntent(from);
        } else if (id == R.id.nav_settings) {
            intent = new Intent(from, SettingsActivity.class);
        } else {
            // Home: reuse the Home screen that is already open.
            intent = new Intent(from, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        }
        return intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
    }
}
