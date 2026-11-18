package com.cs364.luckyshake.ui.home;

import android.app.Application;

import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;

import com.cs364.luckyshake.LuckyShakeApplication;
import com.cs364.luckyshake.data.HistoryRepository;
import com.cs364.luckyshake.data.Menu;
import com.cs364.luckyshake.data.MenuFilter;
import com.cs364.luckyshake.data.MenuRepository;
import com.cs364.luckyshake.logic.DrawEngine;
import com.cs364.luckyshake.logic.DrawRound;

import java.util.List;

/**
 * ViewModel ของหน้าหลักและหน้าผล (อิชชู #5) เก็บชุดเมนูที่เลือก, ตัวกรอง, ผลล่าสุด, โควตาที่เหลือ
 * และ recent id ไว้ หมุนจอหรือสลับแอปแล้วไม่หาย
 *
 * <p><b>วิธีใช้ (ทั้ง MainActivity และ ResultActivity):</b>
 * <pre>
 * HomeViewModel vm = HomeViewModel.get(this);
 * vm.getState().observe(this, state -> { ... แสดง state.menu, state.status, state.redrawsLeft ... });
 * vm.selectSet(setId);  vm.setFilter(filter);
 * DrawState s = vm.draw();      // หน้าหลัก: ถ้า s.hasResult() ไปหน้าผล ไม่งั้นแสดงข้อความตาม s.status
 * vm.redraw();  vm.accept();    // หน้าผล (บันทึกน้ำหนัก −1 / +1 และประวัติ redrawn / accepted ให้เอง)
 * </pre>
 *
 * <p>ต้องเรียกผ่าน {@link #get} เท่านั้น ห้ามใช้ {@code new ViewModelProvider(this)} เอง
 * เพราะหน้าหลักกับหน้าผลเป็นคนละ Activity ถ้าผูกกับ Activity จะได้ ViewModel คนละตัว
 * {@link #get} จึงผูกไว้กับ Application ให้ทั้งสองหน้าใช้ตัวเดียวกัน
 * (ข้อมูลอยู่จนกว่าระบบจะปิดโปรเซส)
 *
 * <p>เมธอดทั้งหมดเรียกจาก main thread
 */
public class HomeViewModel extends AndroidViewModel {
    private final HomeSession session;
    private final MutableLiveData<DrawState> state = new MutableLiveData<>(DrawState.IDLE);

    public HomeViewModel(@NonNull Application application) {
        super(application);
        MenuRepository repository = new MenuRepository(application);
        HistoryRepository history = new HistoryRepository(application);
        session = new HomeSession(new DrawRound(DrawEngine.create()),
                (setId, filter) -> repository.getMenus(setId, filter, null),
                repository::recordEvent,
                history::add);
    }

    /** ViewModel ตัวเดียวกันสำหรับทุกหน้า (ผูกกับ Application) */
    public static HomeViewModel get(@NonNull ComponentActivity activity) {
        LuckyShakeApplication app = (LuckyShakeApplication) activity.getApplication();
        return new ViewModelProvider(app,
                ViewModelProvider.AndroidViewModelFactory.getInstance(app))
                .get(HomeViewModel.class);
    }

    public LiveData<DrawState> getState() {
        return state;
    }

    /** ค่าปัจจุบัน (ไม่ต้อง observe) */
    @NonNull
    public DrawState currentState() {
        return session.getState();
    }

    // ------------------------------------------------------------ settings

    public long getSelectedSetId() {
        return session.getSetId();
    }

    public void selectSet(long setId) {
        session.setSetId(setId);
    }

    /** คืนสำเนา */
    @NonNull
    public MenuFilter getFilter() {
        return session.getFilter();
    }

    public void setFilter(MenuFilter filter) {
        session.setFilter(filter);
    }

    /** ค่าโควตาจากหน้าตั้งค่า (1–5) */
    public void setMaxRedraws(int max) {
        session.setMaxRedraws(max);
        state.setValue(session.getState());
    }

    public List<Long> getRecentIds() {
        return session.getRecentIds();
    }

    // ------------------------------------------------------------- actions

    public DrawState draw() {
        return publish(session.draw());
    }

    public DrawState redraw() {
        return publish(session.redraw());
    }

    /** @return เมนูที่ยอมรับ หรือ null */
    public Menu accept() {
        Menu accepted = session.accept();
        state.setValue(session.getState());
        return accepted;
    }

    public void discardPendingResult() {
        session.discardPendingResult();
        publish(session.getState());
    }

    public void reset() {
        session.reset();
        publish(session.getState());
    }

    private DrawState publish(DrawState s) {
        state.setValue(s);
        return s;
    }
}
