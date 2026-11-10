package com.cs364.luckyshake.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

/**
 * Runs on a device/emulator: right-click this file in Android Studio and choose Run.
 * It starts every test with a fresh database (the app's data on that device is reset).
 */
public class RepositoriesInstrumentedTest {
    private Context context;
    private MenuRepository menus;
    private HistoryRepository history;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        DbHelper.closeInstance();
        context.deleteDatabase(DbHelper.DB_NAME);
        menus = new MenuRepository(context);
        history = new HistoryRepository(context);
    }

    @After
    public void tearDown() {
        DbHelper.closeInstance();
    }

    private static HistoryEntry entry(Long menuId, String nameTh, String nameEn, String status, long at) {
        HistoryEntry e = new HistoryEntry();
        e.menuId = menuId;
        e.nameTh = nameTh;
        e.nameEn = nameEn;
        e.level = 3;
        e.stickNo = 1;
        e.status = status;
        e.mode = HistoryEntry.MODE_SOLO;
        e.createdAt = at;
        return e;
    }

    @Test
    public void seedData_isThereOnFirstRun() {
        List<MenuSet> sets = menus.getSets();
        assertEquals(3, sets.size());
        assertEquals(10, menus.countMenus(sets.get(0).id, null));
        assertEquals(6, menus.countMenus(sets.get(1).id, null));
        assertEquals(6, menus.countMenus(sets.get(2).id, null));
        assertEquals(0, history.count(null));
    }

    @Test
    public void filters_removeFriedSpicyAndOtherTypes() {
        long lunch = menus.getSets().get(0).id;
        assertEquals(8, menus.countMenus(lunch, new MenuFilter(null, true, false)));   // 2 fried
        assertEquals(7, menus.countMenus(lunch, new MenuFilter(null, false, true)));   // 3 spicy
        assertEquals(5, menus.countMenus(lunch, new MenuFilter(null, true, true)));
        assertEquals(3, menus.countMenus(lunch, new MenuFilter(Menu.TYPE_NOODLE, false, false)));
        assertEquals(10, menus.getMenus(lunch, MenuFilter.none(), null).size());
        for (Menu m : menus.getMenus(lunch, new MenuFilter(null, true, true), null)) {
            assertFalse(m.hasTag(Menu.TAG_FRIED));
            assertFalse(m.hasTag(Menu.TAG_SPICY));
        }
    }

    @Test
    public void search_matchesThaiEnglishAndTreatsPercentLiterally() {
        long dinner = menus.getSets().get(1).id;
        assertEquals(1, menus.getMenus(dinner, null, "Pad").size());
        assertEquals(1, menus.getMenus(dinner, null, "ต้มยำ").size());
        assertEquals(0, menus.getMenus(dinner, null, "%").size());
        assertEquals(6, menus.getMenus(dinner, null, "").size());
    }

    @Test
    public void addUpdateDeleteMenu_andNameExists() {
        long lunch = menus.getSets().get(0).id;
        Menu m = new Menu();
        m.setId = lunch;
        m.nameTh = "ข้าวขาหมู";
        m.nameEn = "Stewed pork leg rice";
        m.type = Menu.TYPE_RICE;
        long id = menus.addMenu(m);
        assertTrue(id > 0);
        assertEquals(11, menus.countMenus(lunch, null));
        assertEquals(DbHelper.DEFAULT_WEIGHT, menus.getMenu(id).weight);

        assertTrue(menus.nameExists(lunch, "ข้าวขาหมู", 0));
        assertTrue(menus.nameExists(lunch, "stewed PORK leg rice", 0));
        assertFalse(menus.nameExists(lunch, "ข้าวขาหมู", id));      // editing itself is fine
        assertFalse(menus.nameExists(lunch, "ไม่มีเมนูนี้", 0));

        m.type = Menu.TYPE_OTHER;
        m.tags = Menu.TAG_SPICY;
        m.nameTh = "ข้าวขาหมูเผ็ด";
        assertTrue(menus.updateMenu(m));
        Menu back = menus.getMenu(id);
        assertEquals("ข้าวขาหมูเผ็ด", back.nameTh);
        assertEquals(Menu.TYPE_OTHER, back.type);
        assertTrue(back.hasTag(Menu.TAG_SPICY));

        assertTrue(menus.deleteMenu(id));
        assertNull(menus.getMenu(id));
        assertEquals(-1, menus.addMenu(new Menu()));               // empty name is rejected
    }

    @Test
    public void deleteSet_removesItsMenus_butKeepsOtherSets() {
        List<MenuSet> sets = menus.getSets();
        long lunch = sets.get(0).id;
        long sweets = sets.get(2).id;
        assertTrue(menus.deleteSet(sweets));
        assertEquals(2, menus.countSets());
        assertEquals(0, menus.getMenus(sweets, null, null).size());
        assertEquals(10, menus.countMenus(lunch, null));

        long extra = menus.addSet("  ชุดทดสอบ ", "Test set");
        assertTrue(extra > 0);
        assertEquals("ชุดทดสอบ", menus.getSet(extra).nameTh);
        assertTrue(menus.renameSet(extra, "ชุดใหม่", ""));
        assertEquals("ชุดใหม่", menus.getSet(extra).nameTh);
        assertEquals(-1, menus.addSet("   ", "x"));
    }

    @Test
    public void historySurvivesDeletingItsMenu() {
        long lunch = menus.getSets().get(0).id;
        Menu first = menus.getMenus(lunch, null, null).get(0);
        history.add(entry(first.id, first.nameTh, first.nameEn, HistoryEntry.STATUS_ACCEPTED, 0));
        assertTrue(menus.deleteMenu(first.id));

        List<HistoryEntry> list = history.list(null, 0);
        assertEquals(1, list.size());
        assertNull(list.get(0).menuId);
        assertEquals(first.nameTh, list.get(0).nameTh);
        assertEquals(first.nameEn, list.get(0).nameEn);
    }

    @Test
    public void weights_areClampedAndStamped() {
        long lunch = menus.getSets().get(0).id;
        long id = menus.getMenus(lunch, null, null).get(0).id;
        assertEquals(10, menus.adjustWeight(id, 10));
        assertEquals(1, menus.adjustWeight(id, -20));
        assertEquals(-1, menus.adjustWeight(999999, 1));
        assertTrue(menus.updateWeight(id, 7, 12345L));
        Menu m = menus.getMenu(id);
        assertEquals(7, m.weight);
        assertEquals(12345L, m.lastTouched);
        menus.resetWeights(lunch);
        assertEquals(DbHelper.DEFAULT_WEIGHT, menus.getMenu(id).weight);
    }

    @Test
    public void history_listFilterAndStats() {
        long now = System.currentTimeMillis();
        history.add(entry(null, "กะเพรา", "Basil", HistoryEntry.STATUS_ACCEPTED, now - 3000));
        history.add(entry(null, "กะเพรา", "Basil", HistoryEntry.STATUS_REDRAWN, now - 2000));
        history.add(entry(null, "ส้มตำ", "Papaya salad", HistoryEntry.STATUS_ACCEPTED, now - 1000));

        assertEquals(3, history.count(null));
        assertEquals(2, history.count(HistoryEntry.STATUS_ACCEPTED));
        assertEquals(1, history.list(HistoryEntry.STATUS_REDRAWN, 0).size());
        assertEquals(2, history.list(null, 2).size());
        assertEquals("ส้มตำ", history.list(null, 0).get(0).nameTh);          // newest first

        List<HistoryRepository.NameCount> top = history.topDishes(5);
        assertEquals("กะเพรา", top.get(0).nameTh);
        assertEquals(2, top.get(0).count);
        assertEquals("Basil", top.get(0).getName(false));

        int[] perDay = history.countPerDay(7);
        assertEquals(7, perDay.length);
        int total = 0;
        for (int c : perDay) total += c;
        assertEquals(3, total);                                              // all are from today

        assertNotNull(history.list(null, 0).get(0));
        assertEquals(3, history.clearAll());
        assertEquals(0, history.count(null));
    }
}
