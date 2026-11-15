package com.cs364.luckyshake.ui.home;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import com.cs364.luckyshake.data.DbHelper;
import com.cs364.luckyshake.data.HistoryEntry;
import com.cs364.luckyshake.data.HistoryRepository;
import com.cs364.luckyshake.data.Menu;
import com.cs364.luckyshake.data.MenuRepository;
import com.cs364.luckyshake.logic.DrawRound;
import com.cs364.luckyshake.logic.RedrawQuota;
import com.cs364.luckyshake.logic.WeightedDrawEngine;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Random;

/**
 * ยอมรับ/ขอใหม่ในหน้าผลต้องบันทึกประวัติลงฐานข้อมูลจริง และลบเมนูแล้วประวัติยังอ่านชื่อได้
 * รันบนอุปกรณ์/อีมูเลเตอร์ (ล้างฐานข้อมูลของแอปทุกเทสต์)
 */
public class HomeHistoryInstrumentedTest {
    private MenuRepository menus;
    private HistoryRepository history;
    private HomeSession session;

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        DbHelper.closeInstance();
        context.deleteDatabase(DbHelper.DB_NAME);
        menus = new MenuRepository(context);
        history = new HistoryRepository(context);
        // ต่อสายแบบเดียวกับ HomeViewModel
        session = new HomeSession(
                new DrawRound(new WeightedDrawEngine(new Random(7)), new RedrawQuota(3)),
                (setId, filter) -> menus.getMenus(setId, filter, null),
                menus::recordEvent,
                history::add);
        session.setSetId(menus.getSets().get(0).id);
    }

    @After
    public void tearDown() {
        DbHelper.closeInstance();
    }

    @Test
    public void redrawOnce_acceptOnce_savesTwoEntries_readableAfterMenusDeleted() {
        DrawState first = session.draw();
        DrawState second = session.redraw();
        assertNotNull(session.accept());
        Menu rejected = first.menu;
        Menu accepted = second.menu;
        assertNotNull(rejected);
        assertNotNull(accepted);

        List<HistoryEntry> list = history.list(null, 0);   // ใหม่สุดก่อน
        assertEquals(2, list.size());
        assertEquals(HistoryEntry.STATUS_ACCEPTED, list.get(0).status);
        assertEquals(HistoryEntry.STATUS_REDRAWN, list.get(1).status);
        assertEquals(accepted.nameTh, list.get(0).nameTh);
        assertEquals(rejected.nameTh, list.get(1).nameTh);
        assertEquals(second.stickNo, list.get(0).stickNo);
        assertEquals(first.stickNo, list.get(1).stickNo);
        assertEquals(HistoryEntry.MODE_SOLO, list.get(0).mode);
        assertEquals(1, history.count(HistoryEntry.STATUS_ACCEPTED));
        assertEquals(1, history.count(HistoryEntry.STATUS_REDRAWN));

        assertTrue(menus.deleteMenu(rejected.id));
        assertTrue(menus.deleteMenu(accepted.id));

        list = history.list(null, 0);
        assertEquals(2, list.size());
        assertNull(list.get(0).menuId);
        assertNull(list.get(1).menuId);
        assertEquals(accepted.nameTh, list.get(0).nameTh);
        assertEquals(accepted.nameEn, list.get(0).nameEn);
        assertEquals(rejected.nameTh, list.get(1).nameTh);
        assertEquals(rejected.nameEn, list.get(1).nameEn);
    }
}
