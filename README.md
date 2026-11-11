# Lucky Shake Fortune Pick-up (เซียมซีกินอะไรดี)

แอป Android ช่วยตัดสินใจว่า **กินอะไรดี** เขย่ามือถือเหมือนเขย่ากระบอกเซียมซี ไม้เซียมซีหล่นออกมาบอกเมนูพร้อมคำทำนายสนุก ๆ
โปรเจกต์รายวิชา CS364 การพัฒนาโปรแกรมประยุกต์สำหรับอุปกรณ์พกพา (ปีการศึกษา 2568)

## ฟีเจอร์หลัก
- เขย่าเครื่องเพื่อสุ่มเมนู (ตรวจจับด้วย accelerometer ที่เขียนเอง) สุ่มถ่วงน้ำหนักและไม่ซ้ำติดกัน
- ตัวกรอง: ประเภท, ไม่เอาของทอด, ไม่เอาเผ็ด · ขอเซียมซีใหม่ได้ตามโควตา
- เพิ่ม/แก้ไข/ลบเมนูและชุดเมนูของตัวเอง (SQLite)
- โหมดตัดสินใจกลุ่ม: ผลัดกันเสนอกลุ่มอาหารหรือชื่อร้าน แล้วเขย่าสุ่มผู้ชนะ
- สมุดเซียมซี (ประวัติ) · แจ้งเตือนตามมื้อ · ไทย/อังกฤษ · ธีมสว่าง/มืด · รองรับแนวนอนและหลายขนาดจอ

## ทีมและความรับผิดชอบ

| ชื่อ | GitHub | งานหลัก |
| --- | --- | --- |
| ผิง | `PJ-ww` | แกนกลางเกม: เขย่า, สุ่ม, น้ำหนัก, สถานะ |
| มายด์ | `Mymynex` | หน้าหลัก, หน้าผล, ตัวกรอง, ปุ่ม Back, สไตล์ |
| แป้ง (หัวหน้าทีม) | `Phonpawee` | ฐานข้อมูล, จัดการเมนู, ตรวจ PR |
| เพลง | `ach1raya` | ภาษา, ธีม, ตั้งค่า, แจ้งเตือน, คำทำนาย |
| กานต์ | `Charatrawi-Wasintharakorn` | โหมดกลุ่ม, สมุดเซียมซี, สถิติ |

## เทคโนโลยี
Java + XML · SQLite (`SQLiteOpenHelper`) · minSdk 26 (Android 8.0) · Android Studio
(ยืนยันกับอาจารย์ก่อนว่าใช้ Java/XML และ SQLiteOpenHelper ได้ตามที่สอน)

## เริ่มทำงาน

```bash
git clone https://github.com/Phonpawee/CS364_Lucky-shake-Fortune-pop-Picku.git
cd CS364_Lucky-shake-Fortune-pop-Picku
```

เปิดโฟลเดอร์ใน Android Studio → รอ Gradle sync → กด Run บนมือถือจริง (เปิด USB debugging) หรือ emulator
ก่อนเริ่มงานให้ทุกคนยืนยันว่า build และรันได้

## วิธีทำงานกับ Git (1 Issue = 1 branch = 1 Pull Request)
1. เปิดแท็บ **Issues** กรอง Assignee เป็นตัวเอง เลือก Issue ที่จะทำ
   - Issue ที่ **Blocked** (มีรายการ Blocked by) ต้องรอให้ Issue ที่ block ถูก merge เข้า `main` ก่อน หรือใช้ stub ที่ตกลงกันไว้ไปก่อนตามที่เขียนใน Issue
2. สร้าง branch ตามชื่อใน หัวข้อ **Set Branch Name as** ของ Issue โดยแตกจาก `main` ล่าสุดเสมอ
   ```bash
   git checkout main
   git pull origin main
   git checkout -b feat/<ชื่อตาม Issue>
   ```
3. commit บ่อย ๆ ด้วยข้อความสั้น ๆ **ภาษาอังกฤษ** เช่น `F-02: add ShakeDetector with 1s debounce`
4. เมื่อเสร็จ: `git push -u origin feat/<ชื่อ>` แล้วเปิด **Pull Request** ไปที่ `main`
   - ใส่ `Closes #<เลข issue>` ในคำอธิบาย PR และทำตามเช็กลิสต์ใน template
   - ติ๊ก Acceptance Criteria ของ Issue ให้ครบก่อนขอรีวิว
   - แป้งตรวจและ merge อย่างน้อยวันละครั้ง
5. ทำงานใน branch ของตัวเองเท่านั้น **ห้าม push ตรงเข้า `main`** (ตั้งกฎกันไว้แล้ว)
6. ถ้า PR ชน (conflict) ให้ดึง main มารวมใน branch ตัวเองก่อน (`git pull origin main`) แก้ conflict แล้ว push ใหม่
7. ทุกเช้าดึง `main` ล่าสุดมาก่อนเริ่มงาน และถ้าติดปัญหาเกิน 1 ชั่วโมง ให้ถามในกลุ่มทันที

## กติกาโค้ด
- ข้อความทุกอย่างอยู่ใน `strings.xml` ทั้ง `values` (อังกฤษ) และ `values-th` (ไทย) **ห้าม hard-code**
- ขนาดอักษรใช้หน่วย `sp` เสมอ ตั้งชื่อ key ตามกติกาด้านล่าง
- หน้าของตัวเองต้องรองรับแนวนอนและจอเล็ก/ใหญ่ (`layout-land`, `sw600dp`) ตั้งแต่ตอนทำ
- หมุนจอ สลับแอป และกด Back แล้วข้อมูลของหน้าตัวเองต้องไม่หาย
- จะเปลี่ยน interface กลาง (`MenuRepository`, `HistoryRepository`, `DrawEngine`, `ShakeDetector`, `FortuneProvider`, คีย์ SharedPreferences) ต้องแจ้งทั้งกลุ่มก่อน
- ถ้านำโค้ดหรือแนวคิดจาก repo หรือบทความอื่นมาใช้ ให้จดที่มาและ license

### กติกาตั้งชื่อ key ใน strings.xml (เพลงปรับได้ภายใน 10 พ.ย.)
ตั้งชื่อ key ตามรูปแบบ `<หน้าจอ>_<ชนิด>_<ความหมาย>` โดยชนิดที่ใช้ร่วมกันคือ `btn`, `label`, `title`, `msg`, `error` และ `hint` เช่น `home_btn_draw`, `result_label_menu`, `menu_form_error_name_empty`

เพิ่มข้อความใน `app/src/main/res/values/strings.xml` (ภาษาอังกฤษ ค่าเริ่มต้น) และ `app/src/main/res/values-th/strings.xml` (ภาษาไทย) โดยใช้ key เดียวกันทั้งสองไฟล์ ตัวอย่างข้อความที่มีตัวแปรใช้ format args เช่น `<string name="result_label_menu">Menu for today: %1$s</string>` และแสดงผลด้วย `getString(R.string.result_label_menu, menuName)` ห้ามต่อข้อความด้วย `+` เอง

## โครงโฟลเดอร์ที่แนะนำ (package `com.cs364.luckyshake`)
```
data/       ฐานข้อมูล, MenuRepository, HistoryRepository   (แป้ง)
logic/      DrawEngine, GameConfig, WeightPolicy           (ผิง)
sensor/     ShakeDetector                                   (ผิง)
fortune/    FortuneProvider                                 (เพลง)
reminder/   ReminderScheduler, BootReceiver                 (เพลง)
ui/home/    หน้าหลัก, หน้าผล, ตัวกรอง                         (มายด์)
ui/menu/    รายการเมนู, ฟอร์ม, ชุดเมนู                        (แป้ง)
ui/group/   โหมดกลุ่ม                                       (กานต์)
ui/journal/ สมุดเซียมซี, สถิติ                               (กานต์)
ui/settings/ ตั้งค่า                                        (เพลง)
```

## ระดับงานและ Milestone

| ระดับ | ความหมาย | ต้องเสร็จภายใน |
| --- | --- | --- |
| A | ต้องมี | 16 พ.ย. |
| B | ควรมี (ทำเมื่อระดับ A ผ่านแล้ว) | 17 พ.ย. เที่ยง |

| Milestone | กำหนด |
| --- | --- |
| M0 ตั้งต้นเสร็จ | 10 พ.ย. |
| M1 เส้นทางหลักใช้ได้ | 14 พ.ย. |
| M2 ฟีเจอร์ระดับ A ครบ | 16 พ.ย. |
| M3 ปิดโค้ด (หลังจากนี้แก้ได้เฉพาะบั๊ก) | 17 พ.ย. |
| M4 พร้อมนำเสนอ | 19 พ.ย. |
| นำเสนอ | 20 พ.ย. |

รายการงานทั้งหมดอยู่ที่แท็บ **Issues** (สรุปไว้ที่ `docs/ISSUES.md`) ใครเสร็จงานของตัวเองแล้วให้ช่วยคนที่ช้าที่สุดก่อนเริ่มระดับ B
งานเอกสารและสไลด์ทำร่วมกันนอก Git

## รายงานบั๊ก
เปิด Issue ด้วย template **Bug** ใส่ขั้นตอนที่ทำซ้ำได้ รุ่นเครื่อง/เวอร์ชัน Android และติดป้าย `blocks-demo` ถ้ากระทบเดโม (เจ้าของต้องแก้ภายในวันที่ถูกแจ้ง)


## สไตล์กลางของแอป

- ใช้ theme `Theme.LuckyShake` กับหน้าจอ XML Views สีใน `values/colors.xml` และ `values-night/colors.xml` ใช้ชื่อเดียวกันเพื่อให้ระบบเลือกตามโหมด
- พื้นหน้าจอใช้ `@color/app_background` พื้นการ์ดใช้ `@color/app_surface` ข้อความบนทั้งสองพื้นใช้ `@color/app_on_surface` และใช้ `@color/app_accent` สำหรับรายละเอียดสีทอง
- ปุ่ม `MaterialButton` ใช้ `style="@style/Widget.LuckyShake.Button.Primary"` การ์ด `MaterialCardView` ใช้ `style="@style/Widget.LuckyShake.Card"` และหัวข้อใช้ `android:textAppearance="@style/TextAppearance.LuckyShake.Heading"`
- ใช้ `@dimen/spacing_md` และขนาดอื่นจาก `dimens.xml` ระยะและมุมใช้ `dp` ขนาดตัวอักษรใช้ `sp`
- ใช้ `@drawable/ic_tube` และ `@drawable/ic_stick` เป็นภาพกระบอกและไม้เซียมซี ภาพวาดขึ้นสำหรับโปรเจกต์นี้และแยกไฟล์เพื่อขยับได้อิสระ
- ตั้งชื่อสี ขนาด และ drawable แบบ `lower_snake_case` โดยใช้ `app_` สำหรับสีของแอป และ `ic_` สำหรับภาพไอคอน ตั้งชื่อสไตล์ตามรูปแบบ `Widget.LuckyShake.*` หรือ `TextAppearance.LuckyShake.*`
