# รายการ Issue ทั้งหมด (ฉบับปรับปรุง)

ชื่อ Issue เป็นภาษาอังกฤษ เนื้อหาเป็นภาษาไทย แต่ละ Issue มี Goal / Set Branch Name as / Dependencies / Tasks / Acceptance Criteria / Notes

งานเอกสารและสไลด์ทำร่วมกันนอก Git จึงไม่อยู่ในรายการ (Issue #48–#53 ถูกลบ)

## สรุปจำนวนต่อคน

| คน | จำนวน Issue | ชั่วโมงงานพัฒนาระดับ A |
| --- | --- | --- |
| ผิง | 9 | 27 |
| มายด์ | 10 | 28 |
| แป้ง | 10 | 27 |
| เพลง | 9 | 30 |
| กานต์ | 10 | 24 |

---

## #1 [Setup] Define game constants and publish DrawEngine/ShakeDetector stubs

**Owner:** ผิง · **Level:** A · **Due:** 9–10 พ.ย. · **Estimate:** 3 ชม. · **Milestone:** M0 ตั้งต้นเสร็จ

## Goal
รวมค่าเกมทั้งหมดไว้ที่เดียว และปล่อยโครง (interface + stub) ของ `DrawEngine` กับ `ShakeDetector` ให้มายด์และกานต์เรียกใช้ได้ก่อนที่ของจริงจะเสร็จ ทีมจะได้ทำงานคู่ขนานโดยไม่ต้องรอกัน

## Set Branch Name as
`chore/game-config-stubs`

```bash
git checkout main
git pull origin main
git checkout -b chore/game-config-stubs
```
เมื่อเสร็จให้เปิด Pull Request ไปที่ `main` และใส่ `Closes #1` ในคำอธิบาย PR

## Dependencies
**Blocked by:** ไม่มี — เริ่มได้ทันที

**Blocks:** #2, #3, #10, #34

## Tasks
- [ ] สร้าง package `logic` และ `sensor` ใน `com.cs364.luckyshake`
- [ ] เขียน `GameConfig` เก็บค่าคงที่: โควตาขอใหม่ `MAX_REDRAWS = 3`, สัดส่วนระดับดวง 20/40/30/10, น้ำหนักเมนู `MIN=1 / DEFAULT=5 / MAX=10`, ตัวคูณลดโอกาสเมนูที่เพิ่งออก `0.3` (จำ 2 ครั้งล่าสุด), หน่วงการเขย่า `1000 ms`
- [ ] เขียนคลาส `Candidate` (id, weight) เพื่อให้ `DrawEngine` ไม่ต้องรู้จัก model ของแป้ง
- [ ] เขียน interface + stub ของ `DrawEngine`: `Candidate pick(List<Candidate> pool, List<Long> recentIds)`, `int pickLevel()`, `<T> T weightedPick(List<T> items, List<Double> weights)`
- [ ] เขียน interface + stub ของ `ShakeDetector`: `start()`, `stop()`, `setOnShakeListener(...)`, `setSensitivity(int)` และเมธอด `simulateShake()` สำหรับทดสอบ
- [ ] เปิด Pull Request, merge เข้า main และแจ้งทีมในกลุ่ม

## Acceptance Criteria
- [ ] ค่าเกมทั้งหมดอยู่ใน `GameConfig` ที่เดียว ไม่มีตัวเลขเกมกระจายอยู่ในคลาสอื่น
- [ ] มายด์และกานต์ import แล้วเรียก stub ได้ โดย build ผ่าน
- [ ] merge เข้า main ภายใน 10 พ.ย.

## Notes
- ตัวเลขทั้งหมดเป็นค่าตั้งต้นจากเดโมเว็บ ปรับได้ถ้าทีมตกลงกัน
- หลังปล่อยแล้วห้ามเปลี่ยนลายเซ็นเมธอดโดยไม่แจ้งทีม

---

## #2 [F-02] Implement ShakeDetector (accelerometer, debounce, lifecycle-aware)

**Owner:** ผิง · **Level:** A · **Due:** 11–13 พ.ย. · **Estimate:** 5 ชม. · **Milestone:** M1 เส้นทางหลักใช้ได้

## Goal
ตรวจจับการเขย่าเครื่องด้วย accelerometer ที่เขียนเอง ให้เขย่า 1 ครั้งเรียก callback เพียง 1 ครั้ง และไม่กินแบตเมื่อแอปอยู่พื้นหลัง

## Set Branch Name as
`feat/shake-detector`

```bash
git checkout main
git pull origin main
git checkout -b feat/shake-detector
```
เมื่อเสร็จให้เปิด Pull Request ไปที่ `main` และใส่ `Closes #2` ในคำอธิบาย PR

## Dependencies
**Blocked by** (ต้องเสร็จและ merge เข้า main ก่อน หรือใช้ stub ที่ตกลงกันไว้ไปก่อน):
- #1 — ต้องมี interface และ `GameConfig` ก่อน

**Blocks:** #47

## Tasks
- [ ] อ่านค่า `Sensor.TYPE_ACCELEROMETER` ผ่าน `SensorManager` (ไม่ใช้ไลบรารีสำเร็จรูป เพื่อให้เป็นความท้าทายเชิงเทคนิคที่เล่าได้)
- [ ] คำนวณความเร่งรวมลบแรงโน้มถ่วง: `g = sqrt(x²+y²+z²) − SensorManager.GRAVITY_EARTH` แล้วเทียบกับเกณฑ์
- [ ] แปลงค่าความไว 1–10 (จาก SharedPreferences ของเพลง) เป็นเกณฑ์ความเร่ง
- [ ] หน่วงเวลาอย่างน้อย 1 วินาทีระหว่างการเขย่าแต่ละครั้ง (ค่าจาก `GameConfig`)
- [ ] `start()` ลงทะเบียน listener ใน `onResume` และ `stop()` ยกเลิกใน `onPause` ของหน้าที่ใช้
- [ ] ทดสอบบนมือถือจริงอย่างน้อย 2 รุ่น แล้วจดค่าเกณฑ์ที่เหมาะสม

## Acceptance Criteria
- [ ] เขย่า 1 ครั้ง callback ถูกเรียก 1 ครั้ง ไม่ลั่นซ้ำ
- [ ] สลับแอปแล้ว sensor ถูกยกเลิก (ตรวจใน Logcat หรือโค้ดทดสอบ)
- [ ] ปรับความไวแล้วเขย่าเบา/แรงต่างกันตามที่คาด
- [ ] ทำงานบนเครื่องจริงอย่างน้อย 2 รุ่น

## Notes
- เกณฑ์ตั้งต้นจากเดโมเว็บ: `เกณฑ์ = 22 − 1.6 × ความไว` (m/s²) ใช้เป็นจุดเริ่มแล้วปรับตามเครื่องจริง
- ทดสอบบน emulator: Extended controls → Virtual sensors → Move เพื่อจำลองการเขย่า (ไม่แทนเครื่องจริง)
- ถ้ายังไม่มีค่าความไวจากหน้าตั้งค่าของเพลง ให้ใช้ค่าเริ่มต้น 5 ไปก่อน

---

## #3 [F-03] Implement DrawEngine (weighted random, recent penalty, fortune level)

**Owner:** ผิง · **Level:** A · **Due:** 11–13 พ.ย. · **Estimate:** 6 ชม. · **Milestone:** M1 เส้นทางหลักใช้ได้

## Goal
สุ่มเมนูแบบถ่วงน้ำหนักและลดโอกาสเมนูที่เพิ่งออก พร้อมสุ่มระดับดวง และมีฟังก์ชันสุ่มถ่วงน้ำหนักแบบทั่วไปให้กานต์ใช้ในโหมดกลุ่ม

## Set Branch Name as
`feat/draw-engine`

```bash
git checkout main
git pull origin main
git checkout -b feat/draw-engine
```
เมื่อเสร็จให้เปิด Pull Request ไปที่ `main` และใส่ `Closes #3` ในคำอธิบาย PR

## Dependencies
**Blocked by** (ต้องเสร็จและ merge เข้า main ก่อน หรือใช้ stub ที่ตกลงกันไว้ไปก่อน):
- #1 — ใช้ `Candidate` และ `GameConfig` จาก #1

**Blocks:** #4, #5, #6, #37

## Tasks
- [ ] สุ่มจาก `List<Candidate>` โดยน้ำหนัก = `weight × (อยู่ใน recent 2 ครั้งล่าสุด ? 0.3 : 1)` ด้วยวิธีสะสมน้ำหนัก (cumulative sum)
- [ ] ถ้ามีมากกว่า 1 ตัวเลือก ห้ามคืนตัวที่ออกล่าสุดซ้ำทันทีเมื่อเป็นการขอใหม่ (ส่ง id ที่ต้องยกเว้นเข้ามา)
- [ ] สุ่มระดับดวง 4 ระดับ (ดีมาก/ดี/กลาง/ระวัง) ตามสัดส่วนใน `GameConfig`
- [ ] ทำ `weightedPick(items, weights)` แบบทั่วไป และ **ส่งให้กานต์ภายใน 12 พ.ย.**
- [ ] เขียน JUnit test (ใน `app/src/test`) สุ่ม 1,000 ครั้ง เทียบสัดส่วนกับน้ำหนัก

## Acceptance Criteria
- [ ] สุ่ม 1,000 ครั้งในโค้ดทดสอบแล้วสัดส่วนใกล้น้ำหนักที่ตั้ง (คลาดไม่เกิน ±5 จุดเปอร์เซ็นต์)
- [ ] เมนูที่เพิ่งออกมีโอกาสลดลงตามตัวคูณ
- [ ] `weightedPick` ใช้ได้กับ list ใด ๆ และกานต์เรียกได้
- [ ] เมื่อ pool มี 1 ตัว ไม่ล่ม และคืนตัวนั้น

## Notes
- ViewModel เป็นคนแปลง `Menu` → `Candidate(id, weight)` ก่อนส่งเข้า engine
- ใช้ `java.util.Random` ที่ inject ได้ เพื่อให้ทดสอบซ้ำได้ด้วย seed

---

## #4 [F-05/F-07] Add re-draw quota and edge-case result codes

**Owner:** ผิง · **Level:** A · **Due:** 14 พ.ย. · **Estimate:** 3 ชม. · **Milestone:** M1 เส้นทางหลักใช้ได้

## Goal
นับโควตาขอเซียมซีใหม่ต่อรอบ และคืนรหัสผลสำหรับกรณีขอบ เพื่อให้หน้าจอแสดงข้อความได้ถูกต้องโดยแอปไม่ล่ม

## Set Branch Name as
`feat/redraw-quota-edge-cases`

```bash
git checkout main
git pull origin main
git checkout -b feat/redraw-quota-edge-cases
```
เมื่อเสร็จให้เปิด Pull Request ไปที่ `main` และใส่ `Closes #4` ในคำอธิบาย PR

## Dependencies
**Blocked by** (ต้องเสร็จและ merge เข้า main ก่อน หรือใช้ stub ที่ตกลงกันไว้ไปก่อน):
- #3 — ใช้ผลสุ่มจาก `DrawEngine`

**Blocks:** #5, #47

## Tasks
- [ ] เขียน `RedrawQuota`: `remaining()`, `consume()`, `reset()` (ค่าสูงสุดอ่านจากค่าตั้งค่าของเพลง ค่าเริ่มต้น 3)
- [ ] เขียน enum `DrawStatus`: `OK`, `QUOTA_EMPTY`, `NO_MATCH` (ไม่มีเมนูผ่านตัวกรอง), `SET_EMPTY` (ชุดว่าง), `FEW_ITEMS` (เมนูน้อยกว่า 3)
- [ ] ให้ `DrawEngine`/ViewModel คืน `DrawStatus` ร่วมกับผลสุ่ม
- [ ] ส่งรหัสผลให้มายด์ **ภายใน 14 พ.ย.** พร้อมคำอธิบายสั้น ๆ ว่าแต่ละรหัสควรแสดงข้อความอะไร

## Acceptance Criteria
- [ ] ทุกกรณีขอบไม่ทำให้แอปล่ม
- [ ] โควตาลดลงทุกครั้งที่ขอใหม่ และ reset เมื่อยอมรับหรือเริ่มรอบใหม่
- [ ] มายด์แสดงข้อความตามรหัสผลได้ครบ

## Notes
- เมนูน้อยกว่า 3 = เตือนว่าสุ่มได้แต่ไม่สนุก ไม่ใช่ error

---

## #5 [F-23] Add ViewModel for Home/Result to survive rotation and app switch

**Owner:** ผิง · **Level:** A · **Due:** 14–15 พ.ย. · **Estimate:** 4 ชม. · **Milestone:** M2 ฟีเจอร์ระดับ A ครบ

## Goal
เก็บสถานะของหน้าหลักและหน้าผล (เมนูที่สุ่มได้ ตัวกรอง ตัวนับขอใหม่) ใน ViewModel ให้หมุนจอหรือสลับแอปแล้วไม่หาย

## Set Branch Name as
`feat/draw-viewmodel`

```bash
git checkout main
git pull origin main
git checkout -b feat/draw-viewmodel
```
เมื่อเสร็จให้เปิด Pull Request ไปที่ `main` และใส่ `Closes #5` ในคำอธิบาย PR

## Dependencies
**Blocked by** (ต้องเสร็จและ merge เข้า main ก่อน หรือใช้ stub ที่ตกลงกันไว้ไปก่อน):
- #3 — ViewModel เรียก DrawEngine
- #4 — ต้องใช้รหัสผลและโควตา

**Blocks:** #47

## Tasks
- [ ] สร้าง `HomeViewModel` (androidx lifecycle) เก็บ: ชุดเมนูที่เลือก, ตัวกรอง, ผลที่สุ่มได้ล่าสุด, โควตาที่เหลือ, รายการ recent id
- [ ] เปิดเมธอด `draw()`, `redraw()`, `accept()` ให้หน้าจอเรียก (เรียก `DrawEngine` และ `RedrawQuota`)
- [ ] ถ้ายังไม่มี dependency ของ lifecycle ให้เพิ่มใน `gradle/libs.versions.toml` / `app/build.gradle` แล้วแจ้งทีม
- [ ] ทดสอบหมุนจอและสลับแอปกลางหน้าผล

## Acceptance Criteria
- [ ] หมุนจอกลางหน้าผลแล้วผลและโควตาไม่เปลี่ยน
- [ ] สลับแอปแล้วกลับมา ผลยังอยู่
- [ ] มายด์ใช้ ViewModel นี้ในหน้าหลัก/หน้าผลได้

## Notes
- ถ้าอยากให้รอดกรณีระบบปิดโปรเซส ใช้ `SavedStateHandle` เพิ่มได้ (ระดับเสริม)

---

## #6 [F-16] Implement adaptive menu weights (accept +1 / re-draw -1 / decay)

**Owner:** ผิง · **Level:** A · **Due:** 15–16 พ.ย. · **Estimate:** 3 ชม. · **Milestone:** M2 ฟีเจอร์ระดับ A ครบ

## Goal
ให้แอป "จำนิสัย" ผู้ใช้: เมนูที่ยอมรับบ่อยออกบ่อยขึ้น เมนูที่ขอใหม่บ่อยออกน้อยลง โดยไม่ทำให้เมนูใดหายไปถาวร

## Set Branch Name as
`feat/adaptive-weights`

```bash
git checkout main
git pull origin main
git checkout -b feat/adaptive-weights
```
เมื่อเสร็จให้เปิด Pull Request ไปที่ `main` และใส่ `Closes #6` ในคำอธิบาย PR

## Dependencies
**Blocked by** (ต้องเสร็จและ merge เข้า main ก่อน หรือใช้ stub ที่ตกลงกันไว้ไปก่อน):
- #3 — ต้องมีตรรกะสุ่มก่อน
- #21 — ต้องบันทึกน้ำหนักผ่าน `MenuRepository`

**Blocks:** #42, #47

## Tasks
- [ ] เขียน `WeightPolicy`: ยอมรับ +1, ขอใหม่ −1, จำกัดช่วง 1–10 (ค่าจาก `GameConfig`)
- [ ] ค่อย ๆ กลับสู่ค่าเริ่มต้น 5 สัปดาห์ละ 1 โดยคำนวณจากเวลาที่แตะล่าสุด (`last_touched`) ตอนอ่านเมนู
- [ ] บันทึกน้ำหนักผ่านเมธอดของ `MenuRepository` (แป้ง) แล้วให้ ViewModel เรียกเมื่อยอมรับ/ขอใหม่
- [ ] เขียน JUnit test สำหรับกฎ +1 / −1 / ขอบเขต / การคืนค่า

## Acceptance Criteria
- [ ] ขอใหม่กับเมนูเดิมหลายครั้งแล้วโอกาสออกลดลงตามที่คาด
- [ ] น้ำหนักไม่ต่ำกว่า 1 และไม่เกิน 10 เสมอ
- [ ] เมนูที่ไม่ถูกแตะ 1 สัปดาห์ขยับเข้าใกล้ 5 อีก 1 ขั้น

## Notes
- ให้ `WeightPolicy` เป็นฟังก์ชันล้วน (รับน้ำหนักเดิม+เหตุการณ์ คืนน้ำหนักใหม่) ทดสอบง่ายและแป้งเรียกใช้ได้

---

## #7 [F-13] Log draw history on accept and re-draw

**Owner:** ผิง · **Level:** A · **Due:** 15 พ.ย. · **Estimate:** 1 ชม. · **Milestone:** M2 ฟีเจอร์ระดับ A ครบ

## Goal
บันทึกประวัติทุกครั้งที่ผู้ใช้กดยอมรับหรือขอเซียมซีใหม่ เพื่อใช้แสดงในสมุดและสถิติ (ย้ายมาจากแป้งเพื่อสมดุลงานของหัวหน้าทีม)

## Set Branch Name as
`feat/history-logging`

```bash
git checkout main
git pull origin main
git checkout -b feat/history-logging
```
เมื่อเสร็จให้เปิด Pull Request ไปที่ `main` และใส่ `Closes #7` ในคำอธิบาย PR

## Dependencies
**Blocked by** (ต้องเสร็จและ merge เข้า main ก่อน หรือใช้ stub ที่ตกลงกันไว้ไปก่อน):
- #21 — ต้องมี `HistoryRepository`
- #12 — ต้องมีปุ่มยอมรับ/ขอใหม่ให้เรียก

**Blocks:** #8, #47

## Tasks
- [ ] เรียก `HistoryRepository.add(...)` ใน ViewModel เมื่อ `accept()` และ `redraw()`
- [ ] เก็บ: ชื่อเมนู (ไทย+อังกฤษ), ระดับดวง, เลขไม้, สถานะ (accepted/redrawn), โหมด (solo), เวลา
- [ ] ตรวจว่าลบเมนูแล้วประวัติเดิมยังอ่านได้ (เพราะเก็บชื่อไว้ในประวัติเอง)

## Acceptance Criteria
- [ ] ยอมรับ 1 ครั้งและขอใหม่ 1 ครั้ง ได้ประวัติ 2 รายการตรงกับที่ทำ
- [ ] ลบเมนูแล้วประวัติเดิมยังแสดงชื่อได้

---

## #8 [F-25] Add demo helpers (sample history, instant notification)

**Owner:** ผิง · **Level:** A · **Due:** 16 พ.ย. · **Estimate:** 2 ชม. · **Milestone:** M2 ฟีเจอร์ระดับ A ครบ

## Goal
เพิ่มปุ่มซ่อนสำหรับซ้อมเดโมโดยไม่ต้องรอเวลาจริง (ย้ายมาจากแป้งเพื่อสมดุลงานของหัวหน้าทีม)

## Set Branch Name as
`feat/demo-helper`

```bash
git checkout main
git pull origin main
git checkout -b feat/demo-helper
```
เมื่อเสร็จให้เปิด Pull Request ไปที่ `main` และใส่ `Closes #8` ในคำอธิบาย PR

## Dependencies
**Blocked by** (ต้องเสร็จและ merge เข้า main ก่อน หรือใช้ stub ที่ตกลงกันไว้ไปก่อน):
- #7 — ใช้ `HistoryRepository` สร้างประวัติตัวอย่าง
- #30 — ต้องมี `ReminderScheduler`

**Blocks:** #47

## Tasks
- [ ] ซ่อนไว้ในหน้าตั้งค่า (เช่น กดค้างที่ชื่อเวอร์ชัน) ให้ผู้ใช้ทั่วไปไม่เห็น
- [ ] ปุ่ม "เติมประวัติตัวอย่าง": สร้างประวัติราว 24 รายการกระจายใน 7 วันล่าสุด
- [ ] ปุ่ม "ยิงแจ้งเตือนทันที": เรียก `ReminderScheduler` ให้แจ้งเตือนใน 5–10 วินาที

## Acceptance Criteria
- [ ] กดเติมข้อมูลตัวอย่างแล้วสมุดและสถิติมีข้อมูลให้โชว์
- [ ] กดยิงแจ้งเตือนแล้วได้แจ้งเตือนภายในไม่กี่วินาที

---

## #9 [Setup] Create shared UI style, theme tokens and tube/stick drawables

**Owner:** มายด์ · **Level:** A · **Due:** 9–10 พ.ย. · **Estimate:** 4 ชม. · **Milestone:** M0 ตั้งต้นเสร็จ

## Goal
วางสไตล์กลางและภาพประกอบหลักของแอป เพื่อให้ทุกหน้าที่ทุกคนทำมีหน้าตาไปในทางเดียวกัน

## Set Branch Name as
`chore/ui-style-assets`

```bash
git checkout main
git pull origin main
git checkout -b chore/ui-style-assets
```
เมื่อเสร็จให้เปิด Pull Request ไปที่ `main` และใส่ `Closes #9` ในคำอธิบาย PR

## Dependencies
**Blocked by:** ไม่มี — เริ่มได้ทันที

**Blocks:** #10, #22, #28, #34, #40

## Tasks
- [ ] กำหนดชุดสีใน `colors.xml` (แดงลงรัก, ทอง, สีกระดาษ) พร้อมชุดสำหรับโหมดมืดใน `values-night`
- [ ] สร้างสไตล์ปุ่ม การ์ด หัวข้อ ใน `themes.xml` / `styles.xml` และขนาดมาตรฐานใน `dimens.xml` (ข้อความใช้ `sp`)
- [ ] วาดภาพกระบอกเซียมซีและไม้เซียมซีเป็น vector drawable (`ic_tube.xml`, `ic_stick.xml`)
- [ ] เขียนกติกาสั้น ๆ ใน README ว่าจะใช้สไตล์ใด ตั้งชื่อ resource อย่างไร
- [ ] merge เข้า main และแจ้งทีม

## Acceptance Criteria
- [ ] merge เข้า main ภายใน 10 พ.ย.
- [ ] ทุกคนอ้างสีและสไตล์กลางได้ และอ่านออกทั้งโหมดสว่างและมืด
- [ ] ภาพกระบอกและไม้แสดงคมชัดทุกขนาดจอ (เป็น vector)

## Notes
- ใช้ภาพที่วาดเองหรือ license อนุญาตเท่านั้น ห้ามใช้ตัวละคร/โลโก้ที่มีลิขสิทธิ์

---

## #10 [F-01] Build Home screen (set picker, filters summary, tube, draw button)

**Owner:** มายด์ · **Level:** A · **Due:** 10–12 พ.ย. · **Estimate:** 6 ชม. · **Milestone:** M1 เส้นทางหลักใช้ได้

## Goal
สร้างหน้าหลักที่ผู้ใช้เลือกชุดเมนู เห็นตัวกรอง และเขย่าหรือกดปุ่มเพื่อสุ่มได้

## Set Branch Name as
`feat/home-screen`

```bash
git checkout main
git pull origin main
git checkout -b feat/home-screen
```
เมื่อเสร็จให้เปิด Pull Request ไปที่ `main` และใส่ `Closes #10` ในคำอธิบาย PR

## Dependencies
**Blocked by** (ต้องเสร็จและ merge เข้า main ก่อน หรือใช้ stub ที่ตกลงกันไว้ไปก่อน):
- #9 — ต้องใช้สไตล์และภาพกลาง
- #1 — ผูกกับ stub ของ `ShakeDetector`

**Blocks:** #11, #12, #13, #14, #15, #43

## Tasks
- [ ] วาง layout ด้วย `ConstraintLayout`: Spinner เลือกชุดเมนู, ส่วนสรุปตัวกรอง, ภาพกระบอก, ปุ่ม "สุ่มเลย"
- [ ] ผูกกับ `ShakeDetector` (ใช้ stub ของผิงก่อน): `start()` ใน `onResume`, `stop()` ใน `onPause`
- [ ] เมื่อเขย่าหรือกดปุ่ม ให้เรียก `draw()` แล้วไปหน้าผล (ข้อมูลปลอมก่อนก็ได้)
- [ ] ตั้งชื่อ view id และ string key ตามกติกาของเพลง (ไม่ hard-code ข้อความ)

## Acceptance Criteria
- [ ] เขย่า (จำลอง) หรือกดปุ่มแล้วไปหน้าผลได้
- [ ] Spinner แสดงชุดเมนูและเปลี่ยนชุดได้
- [ ] ข้อความทุกอย่างมาจาก `strings.xml` ทั้งสองภาษา

## Notes
- รอข้อมูลจริงจากแป้งไม่ได้ ให้ใช้ข้อมูลปลอมก่อนแล้วค่อยสลับ

---

## #11 [F-12] Implement menu filters with persistence

**Owner:** มายด์ · **Level:** A · **Due:** 11–13 พ.ย. · **Estimate:** 3 ชม. · **Milestone:** M1 เส้นทางหลักใช้ได้

## Goal
ให้ผู้ใช้กรองเมนูก่อนสุ่มได้ (ประเภท, ไม่เอาของทอด, ไม่เอาเผ็ด) และจำค่าล่าสุดเมื่อปิดแอป

## Set Branch Name as
`feat/filters`

```bash
git checkout main
git pull origin main
git checkout -b feat/filters
```
เมื่อเสร็จให้เปิด Pull Request ไปที่ `main` และใส่ `Closes #11` ในคำอธิบาย PR

## Dependencies
**Blocked by** (ต้องเสร็จและ merge เข้า main ก่อน หรือใช้ stub ที่ตกลงกันไว้ไปก่อน):
- #10 — ต้องมีหน้าหลักให้วางตัวกรอง
- #21 — ต้องดึงจำนวนเมนูจาก `MenuRepository`

**Blocks:** #43

## Tasks
- [ ] ประเภท (Spinner), ไม่เอาของทอด/เผ็ด (Switch), ปุ่มล้างตัวกรอง
- [ ] เก็บค่าใน SharedPreferences (คีย์ เช่น `filter_type`, `filter_no_fried`, `filter_no_spicy`)
- [ ] แสดงจำนวนเมนูที่เหลือหลังกรอง (เช่น "เหลือ 7/10") โดยถามจาก `MenuRepository`

## Acceptance Criteria
- [ ] ปิดแอปแล้วเปิดใหม่ ตัวกรองยังอยู่
- [ ] จำนวนเมนูที่เหลือถูกต้องเมื่อเปลี่ยนตัวกรอง
- [ ] กดล้างตัวกรองแล้วกลับเป็นค่าเริ่มต้น

---

## #12 [F-04] Build Result screen (stick number, level, fortune, accept/re-draw)

**Owner:** มายด์ · **Level:** A · **Due:** 12–14 พ.ย. · **Estimate:** 5 ชม. · **Milestone:** M1 เส้นทางหลักใช้ได้

## Goal
แสดงผลเซียมซี: เลขไม้ ชื่อเมนู ระดับดวง คำทำนาย พร้อมปุ่มยอมรับและขอใหม่

## Set Branch Name as
`feat/result-screen`

```bash
git checkout main
git pull origin main
git checkout -b feat/result-screen
```
เมื่อเสร็จให้เปิด Pull Request ไปที่ `main` และใส่ `Closes #12` ในคำอธิบาย PR

## Dependencies
**Blocked by** (ต้องเสร็จและ merge เข้า main ก่อน หรือใช้ stub ที่ตกลงกันไว้ไปก่อน):
- #10 — ต้องมีหน้าหลักที่นำทางมาหน้าผล
- #27 — ต้องใช้ `FortuneProvider`

**Blocks:** #7, #13, #14, #15, #17, #32, #43

## Tasks
- [ ] แสดงเลขไม้ (ลำดับเมนูในชุด), ชื่อเมนูตามภาษา, ระดับดวงเป็นดาว, ประเภท/แท็ก
- [ ] ดึงคำทำนายจาก `FortuneProvider` (แทรกชื่อเมนู) และแสดงตามภาษาปัจจุบัน
- [ ] ปุ่มยอมรับ/ขอใหม่ เรียก ViewModel และแสดงจุดโควตาที่เหลือ
- [ ] เมื่อโควตาหมด ปิดปุ่มขอใหม่และแสดงข้อความ (ตามรหัสผลของผิง)
- [ ] หลังยอมรับให้แสดงสถานะ "ยอมรับแล้ว" และปุ่มกลับหน้าหลัก

## Acceptance Criteria
- [ ] ขอใหม่ครบโควตาแล้วปุ่มถูกปิดและมีข้อความบอก
- [ ] คำทำนายเปลี่ยนภาษาตามที่ตั้งไว้
- [ ] หมุนจอแล้วผลที่แสดงไม่เปลี่ยน
- [ ] ข้อความทุกอย่างอยู่ใน `strings.xml` ทั้งสองภาษา

## Notes
- รหัสผลจากผิง (#4) มาถึงภายใน 14 พ.ย. ก่อนหน้านั้นใช้ค่าปลอม
- ข้อความคำทำนายเขียนไว้เป็นเพื่อความบันเทิง ควรมีบรรทัดแจ้งในหน้านี้

---

## #13 Add shake animation and falling-stick animation

**Owner:** มายด์ · **Level:** A · **Due:** 14 พ.ย. · **Estimate:** 2 ชม. · **Milestone:** M2 ฟีเจอร์ระดับ A ครบ

## Goal
ทำให้การสุ่มดูมีชีวิต: กระบอกสั่นแล้วไม้เซียมซีหล่นออกมา โดยแอนิเมชันไม่ค้างเมื่อสลับแอป

## Set Branch Name as
`feat/shake-animation`

```bash
git checkout main
git pull origin main
git checkout -b feat/shake-animation
```
เมื่อเสร็จให้เปิด Pull Request ไปที่ `main` และใส่ `Closes #13` ในคำอธิบาย PR

## Dependencies
**Blocked by** (ต้องเสร็จและ merge เข้า main ก่อน หรือใช้ stub ที่ตกลงกันไว้ไปก่อน):
- #10 — ต้องมีหน้าหลักและกระบอก
- #12 — ต้องมีหน้าผล

**Blocks:** #43

## Tasks
- [ ] ทำแอนิเมชันหมุนกระบอกซ้าย-ขวาราว 1 วินาที ด้วย `ObjectAnimator`
- [ ] ทำแอนิเมชันไม้หล่น (translation + rotation) ที่หน้าผล
- [ ] ยกเลิกแอนิเมชันใน `onPause` และเริ่มใหม่ได้ถูกต้องเมื่อกลับมา

## Acceptance Criteria
- [ ] สลับแอปกลางแอนิเมชันแล้วกลับมา ไม่ค้างและไม่ล่ม
- [ ] แอนิเมชันเสร็จแล้วไปหน้าผลตามปกติ

## Notes
- ถ้าผู้ใช้ปิดแอนิเมชันในระบบ (animator duration scale = 0) ต้องยังไปหน้าผลได้

---

## #14 [F-22] Implement Back-button rules and confirmation dialogs

**Owner:** มายด์ · **Level:** A · **Due:** 14–16 พ.ย. · **Estimate:** 5 ชม. · **Milestone:** M2 ฟีเจอร์ระดับ A ครบ

## Goal
ปุ่ม Back ทำงานตามกติกาทุกหน้า และมีกล่องถามยืนยันเมื่อมีงานค้าง (รูบริคข้อ 4)

## Set Branch Name as
`feat/back-navigation`

```bash
git checkout main
git pull origin main
git checkout -b feat/back-navigation
```
เมื่อเสร็จให้เปิด Pull Request ไปที่ `main` และใส่ `Closes #14` ในคำอธิบาย PR

## Dependencies
**Blocked by** (ต้องเสร็จและ merge เข้า main ก่อน หรือใช้ stub ที่ตกลงกันไว้ไปก่อน):
- #10 — ต้องมีหน้าหลัก
- #12 — ต้องมีหน้าผล

**Blocks:** #43

## Tasks
- [ ] ใช้ `OnBackPressedDispatcher` + `OnBackPressedCallback` ในแต่ละหน้า
- [ ] หน้าลูก (ผล, ฟอร์ม, สมุด, ตั้งค่า ฯลฯ) กด Back แล้วกลับหน้าหลัก ไม่ปิดแอป
- [ ] หน้าหลักที่มีผลยังไม่ตัดสินใจ: ถามก่อนออก
- [ ] ทำกล่องยืนยันกลาง (`AlertDialog`) ให้แป้งเรียกตอนกรอกฟอร์มค้าง และกานต์เรียกตอนอยู่กลางโหมดกลุ่ม
- [ ] ทำตารางทดสอบ Back ของทุกหน้า แล้วไล่ทดสอบ

## Acceptance Criteria
- [ ] ทุกหน้าทำตามตารางกติกา Back
- [ ] กลางฟอร์มที่ยังไม่บันทึก / กลางโหมดกลุ่ม กด Back แล้วมีกล่องถามยืนยัน
- [ ] ตอบ "ทิ้ง" แล้วกลับ ตอบ "อยู่ต่อ" แล้วข้อมูลเดิมไม่หาย

## Notes
- callback จากแป้ง (ฟอร์ม, #23) และกานต์ (โหมดกลุ่ม, #38) จะมาภายใน 14 พ.ย. ให้เชื่อมเมื่อพร้อม ไม่ต้องรอเริ่มงานส่วนอื่น
- กติกา: หน้าหลักกด Back = ออกจากแอป (ถามถ้ามีงานค้าง) / หน้าอื่น = กลับหน้าหลัก / งานค้าง = ถามยืนยัน

---

## #15 Add landscape and small/large screen layouts for Home and Result

**Owner:** มายด์ · **Level:** A · **Due:** 15–16 พ.ย. · **Estimate:** 3 ชม. · **Milestone:** M2 ฟีเจอร์ระดับ A ครบ

## Goal
หน้าหลักและหน้าผลต้องใช้งานได้ดีในแนวนอนและหลายขนาดจอ (รูบริคข้อ 3)

## Set Branch Name as
`feat/landscape-layouts`

```bash
git checkout main
git pull origin main
git checkout -b feat/landscape-layouts
```
เมื่อเสร็จให้เปิด Pull Request ไปที่ `main` และใส่ `Closes #15` ในคำอธิบาย PR

## Dependencies
**Blocked by** (ต้องเสร็จและ merge เข้า main ก่อน หรือใช้ stub ที่ตกลงกันไว้ไปก่อน):
- #10 — ต้องมีหน้าหลัก
- #12 — ต้องมีหน้าผล

**Blocks:** #16, #43

## Tasks
- [ ] เพิ่ม `layout-land` ของหน้าหลักและหน้าผล (แบ่งสองคอลัมน์)
- [ ] ทดสอบจอเล็ก (เช่น 320dp), จอใหญ่ และตั้งตัวอักษรของเครื่องเป็นใหญ่สุด
- [ ] แก้ข้อความที่ถูกตัดหรือปุ่มที่หายไป

## Acceptance Criteria
- [ ] ไม่มีข้อความตัดหรือปุ่มหายในแนวนอนและจอเล็ก
- [ ] ตัวอักษรใหญ่สุดของเครื่องแล้วอ่านและกดได้
- [ ] หมุนจอแล้วสถานะไม่หาย

---

## #16 [B] Add tablet layout (sw600dp)

**Owner:** มายด์ · **Level:** B · **Due:** ถ้ามีเวลา ภายใน 17 พ.ย. · **Estimate:** 2 ชม. · **Milestone:** M3 ปิดโค้ด

## Goal
ทำให้หน้าหลักและหน้าผลดูดีบนแท็บเล็ต (ไม่ยืดและไม่โล่งเกินไป)

## Set Branch Name as
`feat/tablet-layout`

```bash
git checkout main
git pull origin main
git checkout -b feat/tablet-layout
```
เมื่อเสร็จให้เปิด Pull Request ไปที่ `main` และใส่ `Closes #16` ในคำอธิบาย PR

## Dependencies
**Blocked by** (ต้องเสร็จและ merge เข้า main ก่อน หรือใช้ stub ที่ตกลงกันไว้ไปก่อน):
- #15 — ต่อยอดจาก layout แนวนอน

## Tasks
- [ ] เพิ่ม `layout-sw600dp` ของหน้าหลักและหน้าผล
- [ ] ทดสอบบน emulator แท็บเล็ตทั้งแนวตั้งและแนวนอน

## Acceptance Criteria
- [ ] หน้าหลักและหน้าผลไม่ดูยืดหรือโล่งเกินไปบนแท็บเล็ต

---

## #17 [B][F-24] Save result card as image to gallery

**Owner:** มายด์ · **Level:** B · **Due:** ถ้ามีเวลา ภายใน 17 พ.ย. · **Estimate:** 3 ชม. · **Milestone:** M3 ปิดโค้ด

## Goal
ให้ผู้ใช้บันทึกการ์ดผลเซียมซีเป็นภาพ (แทนการแชร์)

## Set Branch Name as
`feat/save-result-image`

```bash
git checkout main
git pull origin main
git checkout -b feat/save-result-image
```
เมื่อเสร็จให้เปิด Pull Request ไปที่ `main` และใส่ `Closes #17` ในคำอธิบาย PR

## Dependencies
**Blocked by** (ต้องเสร็จและ merge เข้า main ก่อน หรือใช้ stub ที่ตกลงกันไว้ไปก่อน):
- #12 — ต้องมีการ์ดผลให้แปลงเป็นภาพ

## Tasks
- [ ] แปลง view ของการ์ดผลเป็น `Bitmap` (วาดลง `Canvas`)
- [ ] บันทึกเป็นไฟล์ PNG ด้วย Storage Access Framework (`ActivityResultContracts.CreateDocument("image/png")`) จึงไม่ต้องขอสิทธิ์เก็บไฟล์ทุกเวอร์ชัน
- [ ] แสดงข้อความเมื่อบันทึกสำเร็จหรือผู้ใช้ยกเลิก

## Acceptance Criteria
- [ ] กดปุ่มแล้วเลือกที่เก็บได้ และได้ไฟล์ภาพที่เปิดดูได้
- [ ] ภาพมีเลขไม้ ชื่อเมนู ระดับดวง และคำทำนายครบ
- [ ] ผู้ใช้ยกเลิกแล้วแอปไม่ล่ม

## Notes
- ถ้าต้องการบันทึกลงแกลเลอรีโดยตรงผ่าน `MediaStore` ต้องเช็กสิทธิ์สำหรับ Android ต่ำกว่า 10 (SAF ตัดปัญหานี้)

---

## #18 [Lead] Prepare repository for the team

**Owner:** แป้ง · **Level:** A · **Due:** 9–10 พ.ย. · **Estimate:** 2 ชม. · **Milestone:** M0 ตั้งต้นเสร็จ

## Goal
เตรียม repo ให้ทีมเริ่มงานได้: โปรเจกต์ Android, สิทธิ์สมาชิก, กฎ main, README, template และ Issue

## Set Branch Name as
(ไม่ใช้)

## Dependencies
**Blocked by:** ไม่มี — เริ่มได้ทันที

## Tasks
- [ ] สร้างโปรเจกต์ Android (Java, minSdk 26) แล้ว push เข้า main
- [ ] เพิ่ม Collaborator และตั้งกฎ main (ต้องผ่าน PR, ต้องมีผู้อนุมัติ 1 คน)
- [ ] วาง README, template ของ Issue/PR และสร้าง Issue ทั้งหมด

## Acceptance Criteria
- [ ] ทุกคน clone แล้ว build ได้
- [ ] ทุกคนมี Issue ของตัวเอง

## Notes
- งานนี้ทำเสร็จแล้ว ปิดอัตโนมัติด้วยสคริปต์

---

## #19 [Lead] Review and merge pull requests daily

**Owner:** แป้ง · **Level:** A · **Due:** 10–17 พ.ย. · **Estimate:** 4 ชม. · **Milestone:** M3 ปิดโค้ด

## Goal
ตรวจและ merge Pull Request ของทุกคนทุกวัน เพื่อให้ main build ผ่านเสมอและไม่มีใครรอ

## Set Branch Name as
(ไม่ใช้)

## Dependencies
**Blocked by:** ไม่มี — เริ่มได้ทันที

## Tasks
- [ ] ตรวจ PR ตามเช็กลิสต์ใน template (รันเครื่องจริง, หมุนจอ, สลับแอป, Back, ภาษา, แนวนอน)
- [ ] เช็กว่าทุก PR มี `Closes #<เลข issue>` และ build ผ่านหลังดึง main
- [ ] แก้ conflict ร่วมกับเจ้าของ PR และ merge
- [ ] ดูว่ามี Issue ไหนที่ถูก block นานเกิน 1 วัน แล้วช่วยปลดล็อก

## Acceptance Criteria
- [ ] PR ทุกอันถูกตรวจภายใน 1 วันหลังเปิด
- [ ] main build ผ่านทุกเย็นจนถึงปิดโค้ด 17 พ.ย.

## Notes
- ใช้ Squash merge เพื่อให้ประวัติ main อ่านง่าย (หรือตามที่ทีมตกลง)
- หลังปิดโค้ด 17 พ.ย. merge ได้เฉพาะการแก้บั๊ก

---

## #20 [Data] Design SQLite schema and seed initial data

**Owner:** แป้ง · **Level:** A · **Due:** 9–10 พ.ย. · **Estimate:** 6 ชม. · **Milestone:** M0 ตั้งต้นเสร็จ

## Goal
ออกแบบฐานข้อมูลและใส่ข้อมูลเริ่มต้น เพื่อให้เปิดแอปครั้งแรกมีเมนูให้สุ่มทันที

## Set Branch Name as
`feat/db-schema-seed`

```bash
git checkout main
git pull origin main
git checkout -b feat/db-schema-seed
```
เมื่อเสร็จให้เปิด Pull Request ไปที่ `main` และใส่ `Closes #20` ในคำอธิบาย PR

## Dependencies
**Blocked by:** ไม่มี — เริ่มได้ทันที

**Blocks:** #21

## Tasks
- [ ] เขียน `SQLiteOpenHelper` (ตั้ง `PRAGMA foreign_keys = ON` ใน `onConfigure`)
- [ ] ตาราง `menu_set(id, name_th, name_en)`
- [ ] ตาราง `menu(id, set_id, name_th, name_en, type, tags, weight DEFAULT 5, last_touched, created_at)`
- [ ] ตาราง `history(id, menu_id NULL, name_th, name_en, level, stick_no, status, mode, created_at)` — เก็บชื่อเมนูซ้ำไว้ในประวัติ เพื่อให้ลบเมนูแล้วประวัติยังอ่านได้
- [ ] เขียนคลาส model (POJO) `MenuSet`, `Menu`, `HistoryEntry`
- [ ] ใส่ข้อมูลเริ่มต้น 3 ชุด รวม 22 เมนู ตามรายการในหมายเหตุ

## Acceptance Criteria
- [ ] เปิดแอปครั้งแรกมีข้อมูลครบ 3 ชุด 22 เมนู
- [ ] เปิดแอปครั้งต่อไปไม่สร้างข้อมูลซ้ำ
- [ ] ชื่อเมนูมีทั้งไทยและอังกฤษ

## Notes
- ชุด "อาหารกลางวัน" (10): ข้าวมันไก่ (Hainanese chicken rice) / กะเพราไก่ไข่ดาว (Basil chicken & fried egg, เผ็ด) / ก๋วยเตี๋ยวเรือ (Boat noodles, เผ็ด) / ส้มตำไก่ย่าง (Papaya salad & grilled chicken, เผ็ด) / ข้าวหน้าไก่ทอด (Fried chicken rice, ทอด) / ผัดซีอิ๊ว (Pad see ew) / ข้าวผัดปู (Crab fried rice) / ราดหน้าหมู (Pork gravy noodles) / ข้าวผัดผักรวม (Mixed veggie rice, มังสวิรัติ) / ข้าวหมูทอดกระเทียม (Garlic pork rice, ทอด)
- ชุด "อาหารเย็น" (6): หมูกระทะ (Thai BBQ hotpot) / ต้มยำกุ้ง (Tom yum goong, เผ็ด) / ปลาทอดน้ำปลา (Fried fish with fish sauce, ทอด) / แกงเขียวหวานไก่ (Green curry chicken, เผ็ด) / ข้าวต้มปลา (Fish rice porridge) / ผัดไทยกุ้งสด (Pad Thai with shrimp)
- ชุด "ของหวาน" (6): มะม่วงข้าวเหนียว (Mango sticky rice) / บัวลอย (Bua loy) / ไอศกรีมกะทิ (Coconut ice cream) / ชานมไข่มุก (Bubble milk tea) / กล้วยทอด (Fried bananas, ทอด) / น้ำแข็งใสรวมมิตร (Shaved ice medley)
- ประเภท: ข้าว, เส้น, ของหวาน, เครื่องดื่ม, อื่น ๆ · แท็ก: เผ็ด, ทอด, มังสวิรัติ (เก็บเป็นรหัสคั่นด้วยจุลภาคหรือ bitmask ก็ได้ แต่ต้องตกลงกับผิงและมายด์)
- ไม่มีราคา — ทีมตัดข้อมูลราคาออกแล้ว

---

## #21 [Data] Implement MenuRepository and HistoryRepository

**Owner:** แป้ง · **Level:** A · **Due:** 10–11 พ.ย. · **Estimate:** 6 ชม. · **Milestone:** M1 เส้นทางหลักใช้ได้

## Goal
ทำชั้นเข้าถึงข้อมูลให้ผิง มายด์ และกานต์เรียกใช้ได้ (ปล่อยให้ใช้แทน stub ภายใน 11 พ.ย.)

## Set Branch Name as
`feat/repositories`

```bash
git checkout main
git pull origin main
git checkout -b feat/repositories
```
เมื่อเสร็จให้เปิด Pull Request ไปที่ `main` และใส่ `Closes #21` ในคำอธิบาย PR

## Dependencies
**Blocked by** (ต้องเสร็จและ merge เข้า main ก่อน หรือใช้ stub ที่ตกลงกันไว้ไปก่อน):
- #20 — ต้องมี schema และ `SQLiteOpenHelper`

**Blocks:** #6, #7, #11, #22, #23, #25, #31, #39, #40, #42

## Tasks
- [ ] `MenuRepository`: ดึงชุด/เพิ่ม/เปลี่ยนชื่อ/ลบชุด (ลบเมนูข้างในด้วย), ดึงเมนูตามชุดและตัวกรอง, ดึง/เพิ่ม/แก้/ลบเมนู, ปรับน้ำหนักและ `last_touched`
- [ ] `HistoryRepository`: เพิ่มประวัติ, ดึงรายการ (กรองตามสถานะ), ลบทีละรายการ, ล้างทั้งหมด, เมธอดนับสำหรับสถิติ
- [ ] เขียนเอกสารสั้น ๆ (Javadoc หรือข้อความใน PR) อธิบายแต่ละเมธอด
- [ ] merge แล้วแจ้งผู้ใช้ (ผิง มายด์ กานต์)

## Acceptance Criteria
- [ ] ผิงดึงเมนูตามตัวกรองจากฐานข้อมูลจริงได้ภายใน 11 พ.ย.
- [ ] ลบชุดแล้วไม่มีเมนูค้างในฐานข้อมูล
- [ ] ลบเมนูแล้วประวัติเดิมยังอยู่

## Notes
- ข้อมูลน้อย เรียกบน main thread ได้ในช่วงเดโม ถ้ามีเวลาค่อยย้ายไป background (`ExecutorService`)
- ปล่อย interface ก่อน แล้วค่อยเติมการทำงานจริง เพื่อให้คนอื่นเริ่มได้ไว

---

## #22 [F-08] Build Menu list screen (search, delete with confirmation)

**Owner:** แป้ง · **Level:** A · **Due:** 12–13 พ.ย. · **Estimate:** 5 ชม. · **Milestone:** M1 เส้นทางหลักใช้ได้

## Goal
หน้าแสดงเมนูของชุดที่เลือก ค้นหาได้ ลบได้ และเปิดไปหน้าเพิ่ม/แก้ไขได้

## Set Branch Name as
`feat/menu-list`

```bash
git checkout main
git pull origin main
git checkout -b feat/menu-list
```
เมื่อเสร็จให้เปิด Pull Request ไปที่ `main` และใส่ `Closes #22` ในคำอธิบาย PR

## Dependencies
**Blocked by** (ต้องเสร็จและ merge เข้า main ก่อน หรือใช้ stub ที่ตกลงกันไว้ไปก่อน):
- #21 — ดึง/ลบเมนูผ่าน `MenuRepository`
- #9 — ใช้สไตล์กลาง

**Blocks:** #23, #25, #44

## Tasks
- [ ] `RecyclerView` + adapter (แนะนำ `ListAdapter` + `DiffUtil`) แสดงชื่อ ประเภท แท็ก
- [ ] ช่องค้นหา (กรองตามชื่อไทย/อังกฤษ)
- [ ] ปุ่มลบพร้อมกล่องยืนยัน
- [ ] สถานะว่าง ("ชุดนี้ยังไม่มีเมนู") และปุ่มเพิ่มเมนู

## Acceptance Criteria
- [ ] ลบแล้วรายการอัปเดตทันที
- [ ] ค้นหาแล้วรายการกรองถูก
- [ ] ชุดว่างแสดงข้อความและปุ่มเพิ่มเมนู
- [ ] ข้อความอยู่ใน `strings.xml` ทั้งสองภาษา

---

## #23 [F-09] Build Add/Edit menu form with ActivityResult

**Owner:** แป้ง · **Level:** A · **Due:** 13–15 พ.ย. · **Estimate:** 5 ชม. · **Milestone:** M2 ฟีเจอร์ระดับ A ครบ

## Goal
ฟอร์มเพิ่ม/แก้ไขเมนู (ชื่อ ประเภท แท็ก) ที่ส่งผลกลับหน้ารายการด้วย ActivityResult และไม่หายเมื่อหมุนจอ

## Set Branch Name as
`feat/menu-form`

```bash
git checkout main
git pull origin main
git checkout -b feat/menu-form
```
เมื่อเสร็จให้เปิด Pull Request ไปที่ `main` และใส่ `Closes #23` ในคำอธิบาย PR

## Dependencies
**Blocked by** (ต้องเสร็จและ merge เข้า main ก่อน หรือใช้ stub ที่ตกลงกันไว้ไปก่อน):
- #22 — เปิดฟอร์มจากหน้ารายการ
- #21 — บันทึกผ่าน `MenuRepository`

**Blocks:** #24, #32, #44

## Tasks
- [ ] สร้าง `MenuFormActivity` เปิดด้วย `ActivityResultLauncher` (`registerForActivityResult`)
- [ ] ช่อง: ชื่อ (`TextInputEditText`), ประเภท (Spinner), แท็ก (CheckBox ×3) — ไม่มีราคา
- [ ] ส่งผลกลับด้วย `RESULT_OK` และให้หน้ารายการรีเฟรช
- [ ] ทำให้ข้อมูลที่กรอกอยู่รอดตอนหมุนจอ (id ของ view ครบ หรือใช้ ViewModel)
- [ ] เปิดเมธอด `hasUnsavedChanges()` ให้มายด์ใช้เรียกกล่องยืนยันตอนกด Back (ส่งภายใน 14 พ.ย.)

## Acceptance Criteria
- [ ] หมุนจอกลางฟอร์มแล้วข้อมูลในช่องไม่หาย
- [ ] กด Back กลางฟอร์มที่ยังไม่บันทึกแล้วมีกล่องถามยืนยัน (ร่วมกับมายด์)
- [ ] บันทึกแล้วรายการในหน้ารายการเปลี่ยนทันที
- [ ] แก้ไขเมนูเดิมแล้วข้อมูลถูกอัปเดต

---

## #24 [F-10] Add form validation (empty / duplicate name)

**Owner:** แป้ง · **Level:** A · **Due:** 14–15 พ.ย. · **Estimate:** 2 ชม. · **Milestone:** M2 ฟีเจอร์ระดับ A ครบ

## Goal
ตรวจข้อมูลในฟอร์มเมนู และแสดงข้อความผิดพลาดสองภาษาที่ช่องนั้น

## Set Branch Name as
`feat/menu-form-validation`

```bash
git checkout main
git pull origin main
git checkout -b feat/menu-form-validation
```
เมื่อเสร็จให้เปิด Pull Request ไปที่ `main` และใส่ `Closes #24` ในคำอธิบาย PR

## Dependencies
**Blocked by** (ต้องเสร็จและ merge เข้า main ก่อน หรือใช้ stub ที่ตกลงกันไว้ไปก่อน):
- #23 — ตรวจที่ฟอร์ม

**Blocks:** #44

## Tasks
- [ ] ชื่อต้องไม่ว่างหลังตัดช่องว่าง และยาวไม่เกิน 60 ตัวอักษร
- [ ] ชื่อต้องไม่ซ้ำกับเมนูอื่นในชุดเดียวกัน (ไม่สนตัวพิมพ์เล็กใหญ่)
- [ ] แสดงข้อผิดพลาดด้วย `TextInputLayout.setError(...)` จาก `strings.xml`

## Acceptance Criteria
- [ ] ทุกกรณีแสดงข้อความที่ช่องนั้น
- [ ] ข้อความผิดพลาดมีทั้งไทยและอังกฤษ
- [ ] แก้ถูกแล้วบันทึกผ่าน

---

## #25 [F-11] Manage menu sets (switch, add, delete)

**Owner:** แป้ง · **Level:** A · **Due:** 15–16 พ.ย. · **Estimate:** 3 ชม. · **Milestone:** M2 ฟีเจอร์ระดับ A ครบ

## Goal
ให้ผู้ใช้สลับ เพิ่ม เปลี่ยนชื่อ และลบชุดเมนูได้

## Set Branch Name as
`feat/menu-sets`

```bash
git checkout main
git pull origin main
git checkout -b feat/menu-sets
```
เมื่อเสร็จให้เปิด Pull Request ไปที่ `main` และใส่ `Closes #25` ในคำอธิบาย PR

## Dependencies
**Blocked by** (ต้องเสร็จและ merge เข้า main ก่อน หรือใช้ stub ที่ตกลงกันไว้ไปก่อน):
- #22 — ต่อยอดจากหน้ารายการ
- #21 — จัดการชุดผ่าน `MenuRepository`

**Blocks:** #44

## Tasks
- [ ] เพิ่มชุดใหม่ด้วยกล่องกรอกชื่อ
- [ ] เปลี่ยนชื่อชุด
- [ ] ลบชุดพร้อมเตือนว่าเมนูข้างในจะหายด้วย (บอกจำนวน)
- [ ] ห้ามลบชุดสุดท้าย

## Acceptance Criteria
- [ ] ลบชุดแล้วไม่มีเมนูค้างในฐานข้อมูล
- [ ] ลบชุดสุดท้ายไม่ได้และมีข้อความบอก
- [ ] ชุดที่เพิ่มใหม่เลือกใช้ที่หน้าหลักได้

---

## #26 [Setup] Set up bilingual string resources and key naming convention

**Owner:** เพลง · **Level:** A · **Due:** 9–10 พ.ย. · **Estimate:** 3 ชม. · **Milestone:** M0 ตั้งต้นเสร็จ

## Goal
ตั้งโครงข้อความสองภาษาและกติกาตั้งชื่อ key ให้ทุกคนใช้ตั้งแต่วันแรก จะได้ไม่ต้องไล่แก้ทีหลัง

## Set Branch Name as
`chore/strings-resources`

```bash
git checkout main
git pull origin main
git checkout -b chore/strings-resources
```
เมื่อเสร็จให้เปิด Pull Request ไปที่ `main` และใส่ `Closes #26` ในคำอธิบาย PR

## Dependencies
**Blocked by:** ไม่มี — เริ่มได้ทันที

**Blocks:** #27, #28, #29, #30

## Tasks
- [ ] สร้าง `values/strings.xml` (อังกฤษ เป็นค่าเริ่มต้น) และ `values-th/strings.xml` (ไทย)
- [ ] กำหนดกติกา key: `<หน้าจอ>_<ชนิด>_<ความหมาย>` เช่น `home_btn_draw`, `menu_form_error_name_empty` (ชนิด: btn, label, title, msg, error, hint)
- [ ] ให้ข้อความที่มีตัวแปรใช้ format args (`%1$s`) ห้ามต่อ string เอง
- [ ] เขียนกติกานี้ลง README
- [ ] merge แล้วแจ้งทุกคน

## Acceptance Criteria
- [ ] ทุกคนใช้ key รูปแบบเดียวกันและไม่ hard-code ข้อความ
- [ ] มีตัวอย่างข้อความไทย/อังกฤษอย่างน้อย 10 คีย์
- [ ] merge เข้า main ภายใน 10 พ.ย.

## Notes
- Android Studio: Analyze → Inspect Code จะเตือน `HardcodedText` ให้เห็นง่าย

---

## #27 [Data] Implement FortuneProvider (fortunes and food-type names, TH/EN)

**Owner:** เพลง · **Level:** A · **Due:** 10–11 พ.ย. · **Estimate:** 4 ชม. · **Milestone:** M1 เส้นทางหลักใช้ได้

## Goal
จัดเตรียมคำทำนายและชื่อกลุ่มอาหารสองภาษา ให้มายด์ (หน้าผล) และกานต์ (โหมดกลุ่ม) เรียกใช้ได้

## Set Branch Name as
`feat/fortune-provider`

```bash
git checkout main
git pull origin main
git checkout -b feat/fortune-provider
```
เมื่อเสร็จให้เปิด Pull Request ไปที่ `main` และใส่ `Closes #27` ในคำอธิบาย PR

## Dependencies
**Blocked by** (ต้องเสร็จและ merge เข้า main ก่อน หรือใช้ stub ที่ตกลงกันไว้ไปก่อน):
- #26 — ใช้โครง strings และกติกา key

**Blocks:** #12, #35, #39

## Tasks
- [ ] เขียนคำทำนาย 4 ระดับ ระดับละ 3 ข้อ ไทย/อังกฤษ โดยมีช่องแทรกชื่อเมนู (`%1$s`)
- [ ] เขียนชื่อกลุ่มอาหาร 10 กลุ่มสองภาษา: ข้าว/อาหารตามสั่ง, ก๋วยเตี๋ยว, ปิ้งย่าง/หมูกระทะ, ชาบู/สุกี้, ญี่ปุ่น, เกาหลี, อีสาน, ฟาสต์ฟู้ด, ของหวาน/คาเฟ่, สุขภาพ/เจ
- [ ] เขียน `FortuneProvider`: `getFortune(level, index, menuName)` และ `getFoodTypeName(key)` ตามภาษาปัจจุบัน
- [ ] merge ภายใน 11 พ.ย.

## Acceptance Criteria
- [ ] มายด์และกานต์เรียกขอคำทำนายตามภาษาได้ภายใน 11 พ.ย.
- [ ] คำทำนายแทรกชื่อเมนูถูกทั้งสองภาษา
- [ ] มีข้อความ "เพื่อความบันเทิง" ใช้ร่วมกัน

## Notes
- ข้อความตัวอย่าง 12 ข้อมีในเดโมเว็บ ใช้เป็นฐานได้
- เขียนเองทั้งหมด ห้ามคัดลอกใบเซียมซีจากวัดหรือเว็บอื่น
- หลีกเลี่ยงคำทำนายเรื่องสุขภาพ/การเงิน/กฎหมาย

---

## #28 Set up DayNight theme and enforce sp units

**Owner:** เพลง · **Level:** A · **Due:** 11–12 พ.ย. · **Estimate:** 4 ชม. · **Milestone:** M1 เส้นทางหลักใช้ได้

## Goal
ให้แอปสลับธีมสว่าง/มืด/ตามระบบได้ และทุกหน้าใช้หน่วย `sp` กับข้อความ

## Set Branch Name as
`feat/theme-daynight`

```bash
git checkout main
git pull origin main
git checkout -b feat/theme-daynight
```
เมื่อเสร็จให้เปิด Pull Request ไปที่ `main` และใส่ `Closes #28` ในคำอธิบาย PR

## Dependencies
**Blocked by** (ต้องเสร็จและ merge เข้า main ก่อน หรือใช้ stub ที่ตกลงกันไว้ไปก่อน):
- #26 — ใช้โครง resource
- #9 — ใช้ชุดสีกลางของมายด์

**Blocks:** #29

## Tasks
- [ ] ตั้งธีมหลักเป็นแบบ DayNight (Material) และตรวจ `values-night`
- [ ] ตั้งฟังก์ชันสลับโหมดด้วย `AppCompatDelegate.setDefaultNightMode(...)` ให้หน้าตั้งค่าเรียกใช้
- [ ] ตรวจทุก layout ที่ส่งมาว่าใช้ `sp` กับข้อความและสีจากธีม ไม่ fix สี
- [ ] แจ้งเจ้าของหน้าที่ยังไม่ผ่าน

## Acceptance Criteria
- [ ] สลับธีมแล้วอ่านออกทุกหน้า
- [ ] ไม่มี `textSize` หน่วย `dp`/`px` ใน layout
- [ ] ตั้งตัวอักษรของเครื่องเป็นใหญ่สุดแล้วไม่มีข้อความทับกัน

---

## #29 [F-19] Build Settings screen

**Owner:** เพลง · **Level:** A · **Due:** 12–14 พ.ย. · **Estimate:** 6 ชม. · **Milestone:** M1 เส้นทางหลักใช้ได้

## Goal
หน้าตั้งค่าของแอป พร้อมคีย์ SharedPreferences กลางที่ทุกคนอ่านได้

## Set Branch Name as
`feat/settings-screen`

```bash
git checkout main
git pull origin main
git checkout -b feat/settings-screen
```
เมื่อเสร็จให้เปิด Pull Request ไปที่ `main` และใส่ `Closes #29` ในคำอธิบาย PR

## Dependencies
**Blocked by** (ต้องเสร็จและ merge เข้า main ก่อน หรือใช้ stub ที่ตกลงกันไว้ไปก่อน):
- #26 — ใช้โครง strings
- #28 — ใช้ฟังก์ชันสลับธีม

**Blocks:** #31, #45

## Tasks
- [ ] ภาษา: ไทย/อังกฤษ ด้วย `AppCompatDelegate.setApplicationLocales(...)` (สลับแล้วหน้าจอวาดใหม่ ไม่ล่ม)
- [ ] ธีม: สว่าง/มืด/ตามระบบ
- [ ] ความไวการเขย่า: SeekBar 1–10
- [ ] โควตาขอใหม่ต่อรอบ: Spinner 1–5
- [ ] เขียนคลาส `PrefKeys` รวมคีย์ทั้งหมด และ **แจ้งผิงกับมายด์ภายใน 12 พ.ย.**

## Acceptance Criteria
- [ ] ปิดแอปแล้วเปิดใหม่ ค่ายังอยู่
- [ ] ผิงและมายด์อ่านค่าตั้งค่าได้
- [ ] สลับภาษาแล้วทุกหน้าเปลี่ยนภาษา

## Notes
- Android ต่ำกว่า 13 ถ้าใช้ `setApplicationLocales` และอยากให้จำภาษาหลังปิดแอป ให้ตั้ง `AppLocalesMetadataHolderService` ตามเอกสาร AndroidX (ตรวจเวอร์ชัน appcompat ที่ใช้)

---

## #30 [F-18] Implement meal reminders (AlarmManager + notification + permission)

**Owner:** เพลง · **Level:** A · **Due:** 13–15 พ.ย. · **Estimate:** 7 ชม. · **Milestone:** M2 ฟีเจอร์ระดับ A ครบ

## Goal
แจ้งเตือนตามมื้ออาหารแม้ปิดแอปอยู่ และกดแจ้งเตือนแล้วเปิดแอปที่ชุดเมนูนั้น

## Set Branch Name as
`feat/meal-reminders`

```bash
git checkout main
git pull origin main
git checkout -b feat/meal-reminders
```
เมื่อเสร็จให้เปิด Pull Request ไปที่ `main` และใส่ `Closes #30` ในคำอธิบาย PR

## Dependencies
**Blocked by** (ต้องเสร็จและ merge เข้า main ก่อน หรือใช้ stub ที่ตกลงกันไว้ไปก่อน):
- #26 — ใช้ข้อความสองภาษาในแจ้งเตือน

**Blocks:** #8, #31, #33, #45

## Tasks
- [ ] เขียน `ReminderScheduler`: ตั้ง/ยกเลิก alarm ตามมื้อ (ชั่วโมง นาที ชุดเมนู)
- [ ] ใช้ `AlarmManager.setAndAllowWhileIdle(...)` (ไม่ต้องขอสิทธิ์ exact alarm; เตือนอาจคลาดได้บ้าง) และตั้งรอบถัดไปใหม่ทุกครั้งหลังเตือน
- [ ] เขียน `BroadcastReceiver` สร้าง notification พร้อม notification channel (`PendingIntent` ใช้ `FLAG_IMMUTABLE`)
- [ ] ขอ permission `POST_NOTIFICATIONS` ตอน runtime (Android 13+) และจัดการกรณีปฏิเสธ
- [ ] กดแจ้งเตือนแล้วเปิดแอปที่ชุดเมนูของมื้อนั้น

## Acceptance Criteria
- [ ] เตือนตรงเวลาตอนปิดแอปอยู่ (ทดสอบเครื่องจริง)
- [ ] ปิดสวิตช์แล้วไม่เตือน
- [ ] ปฏิเสธ permission แล้วแอปไม่ล่มและแจ้งผู้ใช้ได้
- [ ] กดแจ้งเตือนแล้วเปิดแอปที่ชุดเมนูถูกต้อง

## Notes
- บางยี่ห้อ (ประหยัดแบต) อาจฆ่า alarm: ทดสอบหลายเครื่องและจดข้อจำกัดใส่รายงาน
- การเตือนเรียก `ReminderScheduler` จาก #31 (ตั้งค่า) และ #8 (ตัวช่วยเดโม)

---

## #31 Add reminder settings UI and Clear-all-data button

**Owner:** เพลง · **Level:** A · **Due:** 15–16 พ.ย. · **Estimate:** 3 ชม. · **Milestone:** M2 ฟีเจอร์ระดับ A ครบ

## Goal
ให้ผู้ใช้เปิด/ปิดและตั้งเวลาเตือนต่อมื้อ ผูกกับชุดเมนู และมีปุ่มล้างข้อมูลทั้งหมด

## Set Branch Name as
`feat/reminder-settings-ui`

```bash
git checkout main
git pull origin main
git checkout -b feat/reminder-settings-ui
```
เมื่อเสร็จให้เปิด Pull Request ไปที่ `main` และใส่ `Closes #31` ในคำอธิบาย PR

## Dependencies
**Blocked by** (ต้องเสร็จและ merge เข้า main ก่อน หรือใช้ stub ที่ตกลงกันไว้ไปก่อน):
- #29 — อยู่ในหน้าตั้งค่า
- #30 — ต้องมี `ReminderScheduler`
- #21 — ต้องดึงรายชื่อชุดเมนู

**Blocks:** #32, #45

## Tasks
- [ ] ในหน้าตั้งค่า: แถวต่อมื้อ (เช้า/กลางวัน/เย็น) มี Switch, เวลา (`TimePickerDialog`/`MaterialTimePicker`), ชุดเมนู (Spinner)
- [ ] บันทึกค่าและเรียก `ReminderScheduler` ตั้ง/ยกเลิก alarm
- [ ] ปุ่ม "ล้างข้อมูลทั้งหมด": ถามยืนยัน → ล้างฐานข้อมูล ค่าตั้งค่า และยกเลิก alarm

## Acceptance Criteria
- [ ] ตั้งค่าแล้วปิดแอปเปิดใหม่ยังอยู่
- [ ] ล้างข้อมูลแล้วแอปกลับสู่สถานะเริ่มต้นและไม่ล่ม
- [ ] แจ้งเตือนผูกกับชุดเมนูที่เลือก

---

## #32 [i18n] Audit all screens for hard-coded and mixed-language text

**Owner:** เพลง · **Level:** A · **Due:** 16 พ.ย. · **Estimate:** 3 ชม. · **Milestone:** M2 ฟีเจอร์ระดับ A ครบ

## Goal
ตรวจทุกหน้าว่ามีสองภาษาครบและไม่มีข้อความ hard-code (รูบริคข้อ 3)

## Set Branch Name as
`chore/i18n-audit`

```bash
git checkout main
git pull origin main
git checkout -b chore/i18n-audit
```
เมื่อเสร็จให้เปิด Pull Request ไปที่ `main` และใส่ `Closes #32` ในคำอธิบาย PR

## Dependencies
**Blocked by** (ต้องเสร็จและ merge เข้า main ก่อน หรือใช้ stub ที่ตกลงกันไว้ไปก่อน):
- #12 — ตรวจหน้าผล
- #23 — ตรวจฟอร์ม
- #31 — ตรวจตั้งค่า
- #39 — ตรวจโหมดกลุ่ม
- #40 — ตรวจสมุด

**Blocks:** #45

## Tasks
- [ ] รัน Analyze → Inspect Code ดู `HardcodedText` และค้น string literal ในโค้ด Java
- [ ] เปิดแอปในภาษาไทยและอังกฤษ ไล่ทุกหน้า จดข้อความหลงภาษาหรือถูกตัด
- [ ] แก้เอง หรือส่งให้เจ้าของหน้านั้นแก้ผ่าน Issue/PR

## Acceptance Criteria
- [ ] ไม่มีข้อความหลงภาษา
- [ ] ไม่มี hard-code ใน layout และโค้ด
- [ ] ข้อความภาษาไทยไม่ถูกตัดหรือซ้อนกัน

---

## #33 [B] Reschedule reminders after reboot (BootReceiver)

**Owner:** เพลง · **Level:** B · **Due:** ถ้ามีเวลา ภายใน 17 พ.ย. · **Estimate:** 2 ชม. · **Milestone:** M3 ปิดโค้ด

## Goal
ให้แจ้งเตือนยังทำงานหลังรีบูตเครื่อง

## Set Branch Name as
`feat/boot-receiver`

```bash
git checkout main
git pull origin main
git checkout -b feat/boot-receiver
```
เมื่อเสร็จให้เปิด Pull Request ไปที่ `main` และใส่ `Closes #33` ในคำอธิบาย PR

## Dependencies
**Blocked by** (ต้องเสร็จและ merge เข้า main ก่อน หรือใช้ stub ที่ตกลงกันไว้ไปก่อน):
- #30 — ต้องมี `ReminderScheduler`

## Tasks
- [ ] เขียน `BootReceiver` รับ `BOOT_COMPLETED` และประกาศ `RECEIVE_BOOT_COMPLETED` ใน manifest
- [ ] ตั้ง alarm ใหม่ให้ทุกมื้อที่เปิดอยู่

## Acceptance Criteria
- [ ] รีบูตเครื่องแล้วแจ้งเตือนยังมาตามเวลา

## Notes
- ทดสอบด้วย emulator: restart เครื่อง หรือ `adb shell am broadcast -a android.intent.action.BOOT_COMPLETED`

---

## #34 [F-17] Implement GroupSession and Group setup screen

**Owner:** กานต์ · **Level:** A · **Due:** 10–11 พ.ย. · **Estimate:** 3 ชม. · **Milestone:** M1 เส้นทางหลักใช้ได้

## Goal
วางแกนโหมดตัดสินใจกลุ่ม: เก็บสถานะรอบ และหน้าเลือกจำนวนคนกับวิธีเสนอ

## Set Branch Name as
`feat/group-session`

```bash
git checkout main
git pull origin main
git checkout -b feat/group-session
```
เมื่อเสร็จให้เปิด Pull Request ไปที่ `main` และใส่ `Closes #34` ในคำอธิบาย PR

## Dependencies
**Blocked by** (ต้องเสร็จและ merge เข้า main ก่อน หรือใช้ stub ที่ตกลงกันไว้ไปก่อน):
- #1 — ใช้ stub ของฟังก์ชันสุ่มถ่วงน้ำหนัก
- #9 — ใช้สไตล์กลาง

**Blocks:** #35, #36

## Tasks
- [ ] เขียน `GroupViewModel`/`GroupSession` เก็บ: จำนวนคน (2–8), วิธีเสนอ (กลุ่มอาหาร/ชื่อร้าน), ตาปัจจุบัน, รายการที่เสนอ, ขั้น (SETUP/COLLECT/FINAL/DONE)
- [ ] หน้าตั้งค่าโหมดกลุ่ม: ตัวเพิ่ม/ลดจำนวนคน, เลือกวิธีเสนอ, ปุ่มเริ่ม
- [ ] อธิบายวิธีเล่นสั้น ๆ บนหน้า (ผลัดกันเสนอแบบไม่เปิดเผย แล้วเขย่าสุ่ม)

## Acceptance Criteria
- [ ] เลือกจำนวนคนและวิธีเสนอได้แล้วเริ่มรอบได้
- [ ] จำนวนคนจำกัด 2–8
- [ ] ข้อความอยู่ใน `strings.xml` ทั้งสองภาษา

---

## #35 [F-17] Group proposals: food-type picker

**Owner:** กานต์ · **Level:** A · **Due:** 11–12 พ.ย. · **Estimate:** 3 ชม. · **Milestone:** M1 เส้นทางหลักใช้ได้

## Goal
ให้แต่ละคนผลัดกันเลือกกลุ่มอาหารที่อยากกิน 1 กลุ่ม โดยไม่โชว์ว่าคนก่อนหน้าเลือกอะไร

## Set Branch Name as
`feat/group-food-types`

```bash
git checkout main
git pull origin main
git checkout -b feat/group-food-types
```
เมื่อเสร็จให้เปิด Pull Request ไปที่ `main` และใส่ `Closes #35` ในคำอธิบาย PR

## Dependencies
**Blocked by** (ต้องเสร็จและ merge เข้า main ก่อน หรือใช้ stub ที่ตกลงกันไว้ไปก่อน):
- #34 — ต่อยอดจากแกนโหมดกลุ่ม
- #27 — ใช้ชื่อกลุ่มอาหารสองภาษา

**Blocks:** #37

## Tasks
- [ ] แสดงตาราง 10 กลุ่ม (ชื่อจาก `FortuneProvider`) เลือกได้ 1 กลุ่มต่อคน
- [ ] ปุ่มยืนยันและข้ามตา
- [ ] หลังยืนยัน ล้างตัวเลือกและไปตาถัดไป ("ส่งมือถือให้คนถัดไป")

## Acceptance Criteria
- [ ] แต่ละตาบันทึก 1 กลุ่ม
- [ ] ไม่โชว์ผลของตาก่อน
- [ ] ยืนยันโดยไม่เลือกแล้วมีข้อความเตือน

---

## #36 [F-17] Group proposals: restaurant-name input

**Owner:** กานต์ · **Level:** A · **Due:** 12–13 พ.ย. · **Estimate:** 3 ชม. · **Milestone:** M1 เส้นทางหลักใช้ได้

## Goal
ให้แต่ละคนพิมพ์ชื่อร้านที่อยากกินได้สูงสุด 3 ร้าน

## Set Branch Name as
`feat/group-restaurant-names`

```bash
git checkout main
git pull origin main
git checkout -b feat/group-restaurant-names
```
เมื่อเสร็จให้เปิด Pull Request ไปที่ `main` และใส่ `Closes #36` ในคำอธิบาย PR

## Dependencies
**Blocked by** (ต้องเสร็จและ merge เข้า main ก่อน หรือใช้ stub ที่ตกลงกันไว้ไปก่อน):
- #34 — ต่อยอดจากแกนโหมดกลุ่ม

**Blocks:** #37

## Tasks
- [ ] ช่องกรอกชื่อร้าน 3 ช่อง (ช่องที่ 2–3 ไม่บังคับ)
- [ ] ตัดช่องว่าง จำกัด 40 ตัวอักษร ตัดชื่อซ้ำของคนเดียวกัน (ไม่สนตัวพิมพ์)
- [ ] ต้องกรอกอย่างน้อย 1 ร้านจึงยืนยันได้

## Acceptance Criteria
- [ ] ใส่ชื่อว่างหมดแล้วแจ้งเตือน ไม่ผ่านตา
- [ ] ชื่อซ้ำของคนเดียวกันถูกรวมเหลือ 1
- [ ] ข้อความอยู่ใน `strings.xml` ทั้งสองภาษา

---

## #37 [F-17] Tally votes and draw the winner (weighted by votes)

**Owner:** กานต์ · **Level:** A · **Due:** 13–14 พ.ย. · **Estimate:** 4 ชม. · **Milestone:** M1 เส้นทางหลักใช้ได้

## Goal
รวมคะแนนเสียงของทุกคนและเขย่าสุ่มผู้ชนะ โดยตัวเลือกที่มีคนเสนอซ้ำมีโอกาสมากขึ้นตามจำนวนเสียง

## Set Branch Name as
`feat/group-vote-draw`

```bash
git checkout main
git pull origin main
git checkout -b feat/group-vote-draw
```
เมื่อเสร็จให้เปิด Pull Request ไปที่ `main` และใส่ `Closes #37` ในคำอธิบาย PR

## Dependencies
**Blocked by** (ต้องเสร็จและ merge เข้า main ก่อน หรือใช้ stub ที่ตกลงกันไว้ไปก่อน):
- #35 — ต้องมีข้อมูลจากโหมดกลุ่มอาหาร
- #36 — ต้องมีข้อมูลจากโหมดชื่อร้าน
- #3 — ต้องใช้ `weightedPick` ของจริง

**Blocks:** #38, #39

## Tasks
- [ ] รวมผลเป็น map: ตัวเลือก → จำนวนเสียง + คนที่เสนอ (ชื่อร้านเทียบแบบไม่สนตัวพิมพ์)
- [ ] หน้าสรุปแสดงรายการพร้อมจำนวนเสียง (เช่น ×2)
- [ ] เขย่าหรือกดปุ่ม สุ่มด้วย `weightedPick` ของผิง (น้ำหนัก = จำนวนเสียง)
- [ ] เขียน JUnit test ของการรวมเสียงและความน่าจะเป็น

## Acceptance Criteria
- [ ] ตัวเลือกที่มีคนเสนอซ้ำ 2 คน ชนะบ่อยกว่าตัวเลือกเดียวตามสัดส่วนในโค้ดทดสอบ
- [ ] ทุกคนข้ามตา (ไม่มีตัวเลือก) แล้วกลับไปหน้าตั้งค่าพร้อมข้อความ

---

## #38 [F-17] Persist group state and add Back confirmation callback

**Owner:** กานต์ · **Level:** A · **Due:** 14–15 พ.ย. · **Estimate:** 4 ชม. · **Milestone:** M2 ฟีเจอร์ระดับ A ครบ

## Goal
ให้โหมดกลุ่มรอดการหมุนจอและสลับแอปกลางทาง และถามยืนยันก่อนยกเลิก

## Set Branch Name as
`feat/group-state-back`

```bash
git checkout main
git pull origin main
git checkout -b feat/group-state-back
```
เมื่อเสร็จให้เปิด Pull Request ไปที่ `main` และใส่ `Closes #38` ในคำอธิบาย PR

## Dependencies
**Blocked by** (ต้องเสร็จและ merge เข้า main ก่อน หรือใช้ stub ที่ตกลงกันไว้ไปก่อน):
- #37 — ต้องมีโหมดกลุ่มครบก่อนจึงเก็บสถานะได้ครบ

**Blocks:** #46

## Tasks
- [ ] เก็บสถานะทั้งหมดใน ViewModel (ตาปัจจุบัน, รายการที่เสนอ, ขั้น) แยกจาก Activity
- [ ] ทำ callback ถามยืนยันก่อนยกเลิก ส่งให้มายด์เชื่อมกับ Back **ภายใน 14 พ.ย.**
- [ ] ยกเลิกแล้วรีเซ็ตกลับหน้าตั้งค่าโดยเก็บจำนวนคนและวิธีเสนอเดิมไว้

## Acceptance Criteria
- [ ] หมุนจอหรือสลับแอปกลางตาที่ 3 แล้วยังเป็นตาที่ 3
- [ ] กด Back กลางโหมดแล้วมีกล่องถามยืนยัน
- [ ] ยกเลิกแล้วเริ่มรอบใหม่ได้ ข้อมูลเก่าไม่ค้าง

---

## #39 [F-17] Build group winner screen and log to history

**Owner:** กานต์ · **Level:** A · **Due:** 15 พ.ย. · **Estimate:** 2 ชม. · **Milestone:** M2 ฟีเจอร์ระดับ A ครบ

## Goal
แสดงผู้ชนะของโหมดกลุ่มพร้อมรายละเอียด และบันทึกลงประวัติ

## Set Branch Name as
`feat/group-winner-screen`

```bash
git checkout main
git pull origin main
git checkout -b feat/group-winner-screen
```
เมื่อเสร็จให้เปิด Pull Request ไปที่ `main` และใส่ `Closes #39` ในคำอธิบาย PR

## Dependencies
**Blocked by** (ต้องเสร็จและ merge เข้า main ก่อน หรือใช้ stub ที่ตกลงกันไว้ไปก่อน):
- #37 — ต้องมีผลผู้ชนะ
- #21 — บันทึกประวัติผ่าน `HistoryRepository`
- #27 — ใช้คำทำนาย

**Blocks:** #32, #46

## Tasks
- [ ] แสดงไม้เซียมซี ชื่อที่ชนะ (กลุ่มอาหารหรือชื่อร้าน) ใครเสนอ จำนวนเสียง ระดับดวง และคำทำนาย
- [ ] บันทึกลงประวัติ (โหมด `group`, สถานะ `accepted`)
- [ ] ปุ่ม "เริ่มรอบใหม่"

## Acceptance Criteria
- [ ] ประวัติมีรายการโหมดกลุ่มพร้อมชื่อที่ชนะ
- [ ] ชื่อที่ชนะแสดงตามที่ผู้ใช้พิมพ์ (ไม่แปล) หรือชื่อกลุ่มอาหารตามภาษา

---

## #40 [F-14] Build Journal (history) screen

**Owner:** กานต์ · **Level:** A · **Due:** 15–16 พ.ย. · **Estimate:** 5 ชม. · **Milestone:** M2 ฟีเจอร์ระดับ A ครบ

## Goal
สมุดเซียมซี: แสดงประวัติการสุ่ม กรอง ลบทีละรายการหรือล้างทั้งหมด

## Set Branch Name as
`feat/journal`

```bash
git checkout main
git pull origin main
git checkout -b feat/journal
```
เมื่อเสร็จให้เปิด Pull Request ไปที่ `main` และใส่ `Closes #40` ในคำอธิบาย PR

## Dependencies
**Blocked by** (ต้องเสร็จและ merge เข้า main ก่อน หรือใช้ stub ที่ตกลงกันไว้ไปก่อน):
- #21 — ดึงประวัติผ่าน `HistoryRepository`
- #9 — ใช้สไตล์กลาง

**Blocks:** #32, #41, #46

## Tasks
- [ ] `RecyclerView` เรียงล่าสุดก่อน แสดงชื่อ ระดับดวง เวลา (ตามภาษา) และโหมด
- [ ] ตัวกรอง: ทั้งหมด/ยอมรับ/ขอใหม่
- [ ] ลบทีละรายการ และล้างทั้งหมดพร้อมกล่องยืนยัน
- [ ] สถานะว่าง

## Acceptance Criteria
- [ ] ประวัติหลายร้อยรายการเลื่อนได้ไม่กระตุก
- [ ] ล้างทั้งหมดแล้วมีกล่องยืนยันก่อน
- [ ] เวลาแสดงตามภาษาที่ตั้งไว้

---

## #41 [B][F-15] Build Stats screen and charts

**Owner:** กานต์ · **Level:** B · **Due:** ถ้ามีเวลา ภายใน 17 พ.ย. · **Estimate:** 5 ชม. · **Milestone:** M3 ปิดโค้ด

## Goal
สรุปสถิติจากประวัติ: เมนูที่ออกบ่อย สัดส่วนยอมรับ/ขอใหม่ และ 7 วันล่าสุด

## Set Branch Name as
`feat/stats`

```bash
git checkout main
git pull origin main
git checkout -b feat/stats
```
เมื่อเสร็จให้เปิด Pull Request ไปที่ `main` และใส่ `Closes #41` ในคำอธิบาย PR

## Dependencies
**Blocked by** (ต้องเสร็จและ merge เข้า main ก่อน หรือใช้ stub ที่ตกลงกันไว้ไปก่อน):
- #40 — ใช้ข้อมูลประวัติและโครงหน้าเดียวกัน

## Tasks
- [ ] เมนูที่ออกบ่อยสุด 5 อันดับ
- [ ] สัดส่วนยอมรับ/ขอใหม่
- [ ] จำนวนครั้งใน 7 วันล่าสุด
- [ ] วาดกราฟแบบง่ายด้วย View/`ProgressBar` (ไม่ต้องเพิ่มไลบรารีกราฟ)

## Acceptance Criteria
- [ ] ตัวเลขตรงกับการนับด้วยมือจากข้อมูลทดสอบ
- [ ] ไม่มีข้อมูลแล้วแสดงสถานะว่าง ไม่ล่ม

---

## #42 [B] Build 'How the app thinks' screen (menu weights)

**Owner:** กานต์ · **Level:** B · **Due:** ถ้ามีเวลา ภายใน 17 พ.ย. · **Estimate:** 4 ชม. · **Milestone:** M3 ปิดโค้ด

## Goal
หน้าที่โชว์น้ำหนักของแต่ละเมนูเพื่ออธิบายว่าแอปเรียนรู้ความชอบอย่างไร (ย้ายมาจากผิง)

## Set Branch Name as
`feat/how-it-thinks`

```bash
git checkout main
git pull origin main
git checkout -b feat/how-it-thinks
```
เมื่อเสร็จให้เปิด Pull Request ไปที่ `main` และใส่ `Closes #42` ในคำอธิบาย PR

## Dependencies
**Blocked by** (ต้องเสร็จและ merge เข้า main ก่อน หรือใช้ stub ที่ตกลงกันไว้ไปก่อน):
- #6 — ต้องมีตรรกะน้ำหนัก
- #21 — อ่าน/เขียนน้ำหนักผ่าน `MenuRepository`

## Tasks
- [ ] แสดงรายการเมนูในชุดพร้อมแถบน้ำหนัก 1–10
- [ ] ปุ่มรีเซ็ตน้ำหนักทั้งชุดกลับเป็น 5
- [ ] อธิบายกติกาสั้น ๆ (ยอมรับ +1, ขอใหม่ −1, กลับสู่ 5 สัปดาห์ละ 1)

## Acceptance Criteria
- [ ] ปรับน้ำหนักแล้วหน้านี้แสดงตรงกับฐานข้อมูล
- [ ] กดรีเซ็ตแล้วทุกเมนูเป็น 5

---

## #43 [Test] Ping tests Mind's work (round-robin)

**Owner:** ผิง · **Level:** A · **Due:** 16–17 พ.ย. (รอบแรก) และ 18 พ.ย. (รอบสอง) · **Estimate:** 3 ชม. · **Milestone:** M3 ปิดโค้ด

## Goal
ทดสอบงานของมายด์ (หน้าหลัก ผลเซียมซี ตัวกรอง แอนิเมชัน ปุ่ม Back และ layout แนวนอน) บนมือถือจริงตามเช็กลิสต์ เพื่อหาบั๊กที่เจ้าของมองไม่เห็นก่อนปิดโค้ด

## Set Branch Name as
(ไม่ต้องสร้าง — ถ้าต้องแก้บั๊กใช้ `fix/<ชื่อบั๊ก>`)

## Dependencies
**Blocked by** (ต้องเสร็จและ merge เข้า main ก่อน หรือใช้ stub ที่ตกลงกันไว้ไปก่อน):
- #10 — ต้องเสร็จก่อนจึงทดสอบได้
- #11 — ต้องเสร็จก่อนจึงทดสอบได้
- #12 — ต้องเสร็จก่อนจึงทดสอบได้
- #13 — ต้องเสร็จก่อนจึงทดสอบได้
- #14 — ต้องเสร็จก่อนจึงทดสอบได้
- #15 — ต้องเสร็จก่อนจึงทดสอบได้

**Blocks:** #54

## Tasks
- [ ] ทดสอบทุกหน้าของมายด์ ตามเช็กลิสต์ด้านล่างบนมือถือจริงอย่างน้อย 1 เครื่อง
- [ ] เจอบั๊กให้เปิด Issue ด้วย template บั๊ก ใส่ขั้นตอนทำซ้ำ รุ่นเครื่อง/Android และติดป้าย `blocks-demo` ถ้ากระทบเดโม
- [ ] ทดสอบซ้ำหลังเจ้าของแก้ (รอบสอง 18 พ.ย.)

## Acceptance Criteria
- [ ] หมุนจอกลางหน้านั้นแล้วข้อมูลไม่หาย
- [ ] สลับแอปแล้วกลับมา งานเดิมยังอยู่และทำต่อได้
- [ ] ล็อกจอแล้วปลดล็อก แอปไม่รีเซ็ต
- [ ] กด Back ในหน้าที่ไม่ใช่เมนูหลักแล้วกลับหน้าหลัก ไม่ปิดแอปทันที
- [ ] กด Back กลางฟอร์มที่ยังไม่บันทึกหรือกลางโหมดกลุ่ม ต้องมีกล่องถามยืนยัน
- [ ] สลับภาษาไทย/อังกฤษแล้วไม่มีข้อความหลงภาษาหรือถูกตัด
- [ ] สลับธีมสว่าง/มืดแล้วอ่านออกทุกหน้า
- [ ] ตั้งตัวอักษรเครื่องเป็นใหญ่สุดแล้วไม่มีปุ่มหายหรือข้อความทับกัน
- [ ] ทดสอบแนวนอน จอเล็ก และจอใหญ่
- [ ] ปฏิเสธ permission แจ้งเตือนแล้วแอปไม่ล่ม
- [ ] แจ้งเตือนมาตรงเวลาตอนปิดแอปอยู่
- [ ] โหมดกลุ่มทดสอบที่ 2 คนและ 8 คน ทั้งสองวิธีเสนอ
- [ ] ไม่มีเมนูผ่านตัวกรอง และชุดเมนูว่าง แอปไม่ล่ม

## Notes
- รอบแรกทดสอบ 16–17 พ.ย. หลังฟีเจอร์ระดับ A ครบ รอบสองทดสอบ 18 พ.ย.
- บั๊กที่บล็อกเดโมเจ้าของต้องแก้ภายในวันที่ถูกแจ้ง

---

## #44 [Test] Mind tests Pang's work (round-robin)

**Owner:** มายด์ · **Level:** A · **Due:** 16–17 พ.ย. (รอบแรก) และ 18 พ.ย. (รอบสอง) · **Estimate:** 3 ชม. · **Milestone:** M3 ปิดโค้ด

## Goal
ทดสอบงานของแป้ง (รายการเมนู ฟอร์มเพิ่ม/แก้ไข ตรวจข้อมูล และชุดเมนู) บนมือถือจริงตามเช็กลิสต์ เพื่อหาบั๊กที่เจ้าของมองไม่เห็นก่อนปิดโค้ด

## Set Branch Name as
(ไม่ต้องสร้าง — ถ้าต้องแก้บั๊กใช้ `fix/<ชื่อบั๊ก>`)

## Dependencies
**Blocked by** (ต้องเสร็จและ merge เข้า main ก่อน หรือใช้ stub ที่ตกลงกันไว้ไปก่อน):
- #22 — ต้องเสร็จก่อนจึงทดสอบได้
- #23 — ต้องเสร็จก่อนจึงทดสอบได้
- #24 — ต้องเสร็จก่อนจึงทดสอบได้
- #25 — ต้องเสร็จก่อนจึงทดสอบได้

**Blocks:** #54

## Tasks
- [ ] ทดสอบทุกหน้าของแป้ง ตามเช็กลิสต์ด้านล่างบนมือถือจริงอย่างน้อย 1 เครื่อง
- [ ] เจอบั๊กให้เปิด Issue ด้วย template บั๊ก ใส่ขั้นตอนทำซ้ำ รุ่นเครื่อง/Android และติดป้าย `blocks-demo` ถ้ากระทบเดโม
- [ ] ทดสอบซ้ำหลังเจ้าของแก้ (รอบสอง 18 พ.ย.)

## Acceptance Criteria
- [ ] หมุนจอกลางหน้านั้นแล้วข้อมูลไม่หาย
- [ ] สลับแอปแล้วกลับมา งานเดิมยังอยู่และทำต่อได้
- [ ] ล็อกจอแล้วปลดล็อก แอปไม่รีเซ็ต
- [ ] กด Back ในหน้าที่ไม่ใช่เมนูหลักแล้วกลับหน้าหลัก ไม่ปิดแอปทันที
- [ ] กด Back กลางฟอร์มที่ยังไม่บันทึกหรือกลางโหมดกลุ่ม ต้องมีกล่องถามยืนยัน
- [ ] สลับภาษาไทย/อังกฤษแล้วไม่มีข้อความหลงภาษาหรือถูกตัด
- [ ] สลับธีมสว่าง/มืดแล้วอ่านออกทุกหน้า
- [ ] ตั้งตัวอักษรเครื่องเป็นใหญ่สุดแล้วไม่มีปุ่มหายหรือข้อความทับกัน
- [ ] ทดสอบแนวนอน จอเล็ก และจอใหญ่
- [ ] ปฏิเสธ permission แจ้งเตือนแล้วแอปไม่ล่ม
- [ ] แจ้งเตือนมาตรงเวลาตอนปิดแอปอยู่
- [ ] โหมดกลุ่มทดสอบที่ 2 คนและ 8 คน ทั้งสองวิธีเสนอ
- [ ] ไม่มีเมนูผ่านตัวกรอง และชุดเมนูว่าง แอปไม่ล่ม

## Notes
- รอบแรกทดสอบ 16–17 พ.ย. หลังฟีเจอร์ระดับ A ครบ รอบสองทดสอบ 18 พ.ย.
- บั๊กที่บล็อกเดโมเจ้าของต้องแก้ภายในวันที่ถูกแจ้ง

---

## #45 [Test] Pang tests Phleng's work (round-robin)

**Owner:** แป้ง · **Level:** A · **Due:** 16–17 พ.ย. (รอบแรก) และ 18 พ.ย. (รอบสอง) · **Estimate:** 3 ชม. · **Milestone:** M3 ปิดโค้ด

## Goal
ทดสอบงานของเพลง (ตั้งค่า ภาษา ธีม แจ้งเตือน และข้อความสองภาษาทุกหน้า) บนมือถือจริงตามเช็กลิสต์ เพื่อหาบั๊กที่เจ้าของมองไม่เห็นก่อนปิดโค้ด

## Set Branch Name as
(ไม่ต้องสร้าง — ถ้าต้องแก้บั๊กใช้ `fix/<ชื่อบั๊ก>`)

## Dependencies
**Blocked by** (ต้องเสร็จและ merge เข้า main ก่อน หรือใช้ stub ที่ตกลงกันไว้ไปก่อน):
- #29 — ต้องเสร็จก่อนจึงทดสอบได้
- #30 — ต้องเสร็จก่อนจึงทดสอบได้
- #31 — ต้องเสร็จก่อนจึงทดสอบได้
- #32 — ต้องเสร็จก่อนจึงทดสอบได้

**Blocks:** #54

## Tasks
- [ ] ทดสอบทุกหน้าของเพลง ตามเช็กลิสต์ด้านล่างบนมือถือจริงอย่างน้อย 1 เครื่อง
- [ ] เจอบั๊กให้เปิด Issue ด้วย template บั๊ก ใส่ขั้นตอนทำซ้ำ รุ่นเครื่อง/Android และติดป้าย `blocks-demo` ถ้ากระทบเดโม
- [ ] ทดสอบซ้ำหลังเจ้าของแก้ (รอบสอง 18 พ.ย.)

## Acceptance Criteria
- [ ] หมุนจอกลางหน้านั้นแล้วข้อมูลไม่หาย
- [ ] สลับแอปแล้วกลับมา งานเดิมยังอยู่และทำต่อได้
- [ ] ล็อกจอแล้วปลดล็อก แอปไม่รีเซ็ต
- [ ] กด Back ในหน้าที่ไม่ใช่เมนูหลักแล้วกลับหน้าหลัก ไม่ปิดแอปทันที
- [ ] กด Back กลางฟอร์มที่ยังไม่บันทึกหรือกลางโหมดกลุ่ม ต้องมีกล่องถามยืนยัน
- [ ] สลับภาษาไทย/อังกฤษแล้วไม่มีข้อความหลงภาษาหรือถูกตัด
- [ ] สลับธีมสว่าง/มืดแล้วอ่านออกทุกหน้า
- [ ] ตั้งตัวอักษรเครื่องเป็นใหญ่สุดแล้วไม่มีปุ่มหายหรือข้อความทับกัน
- [ ] ทดสอบแนวนอน จอเล็ก และจอใหญ่
- [ ] ปฏิเสธ permission แจ้งเตือนแล้วแอปไม่ล่ม
- [ ] แจ้งเตือนมาตรงเวลาตอนปิดแอปอยู่
- [ ] โหมดกลุ่มทดสอบที่ 2 คนและ 8 คน ทั้งสองวิธีเสนอ
- [ ] ไม่มีเมนูผ่านตัวกรอง และชุดเมนูว่าง แอปไม่ล่ม

## Notes
- รอบแรกทดสอบ 16–17 พ.ย. หลังฟีเจอร์ระดับ A ครบ รอบสองทดสอบ 18 พ.ย.
- บั๊กที่บล็อกเดโมเจ้าของต้องแก้ภายในวันที่ถูกแจ้ง

---

## #46 [Test] Phleng tests Kan's work (round-robin)

**Owner:** เพลง · **Level:** A · **Due:** 16–17 พ.ย. (รอบแรก) และ 18 พ.ย. (รอบสอง) · **Estimate:** 3 ชม. · **Milestone:** M3 ปิดโค้ด

## Goal
ทดสอบงานของกานต์ (โหมดกลุ่มทั้งสองวิธีเสนอ สมุดเซียมซี และสถิติ) บนมือถือจริงตามเช็กลิสต์ เพื่อหาบั๊กที่เจ้าของมองไม่เห็นก่อนปิดโค้ด

## Set Branch Name as
(ไม่ต้องสร้าง — ถ้าต้องแก้บั๊กใช้ `fix/<ชื่อบั๊ก>`)

## Dependencies
**Blocked by** (ต้องเสร็จและ merge เข้า main ก่อน หรือใช้ stub ที่ตกลงกันไว้ไปก่อน):
- #38 — ต้องเสร็จก่อนจึงทดสอบได้
- #39 — ต้องเสร็จก่อนจึงทดสอบได้
- #40 — ต้องเสร็จก่อนจึงทดสอบได้

**Blocks:** #54

## Tasks
- [ ] ทดสอบทุกหน้าของกานต์ ตามเช็กลิสต์ด้านล่างบนมือถือจริงอย่างน้อย 1 เครื่อง
- [ ] เจอบั๊กให้เปิด Issue ด้วย template บั๊ก ใส่ขั้นตอนทำซ้ำ รุ่นเครื่อง/Android และติดป้าย `blocks-demo` ถ้ากระทบเดโม
- [ ] ทดสอบซ้ำหลังเจ้าของแก้ (รอบสอง 18 พ.ย.)

## Acceptance Criteria
- [ ] หมุนจอกลางหน้านั้นแล้วข้อมูลไม่หาย
- [ ] สลับแอปแล้วกลับมา งานเดิมยังอยู่และทำต่อได้
- [ ] ล็อกจอแล้วปลดล็อก แอปไม่รีเซ็ต
- [ ] กด Back ในหน้าที่ไม่ใช่เมนูหลักแล้วกลับหน้าหลัก ไม่ปิดแอปทันที
- [ ] กด Back กลางฟอร์มที่ยังไม่บันทึกหรือกลางโหมดกลุ่ม ต้องมีกล่องถามยืนยัน
- [ ] สลับภาษาไทย/อังกฤษแล้วไม่มีข้อความหลงภาษาหรือถูกตัด
- [ ] สลับธีมสว่าง/มืดแล้วอ่านออกทุกหน้า
- [ ] ตั้งตัวอักษรเครื่องเป็นใหญ่สุดแล้วไม่มีปุ่มหายหรือข้อความทับกัน
- [ ] ทดสอบแนวนอน จอเล็ก และจอใหญ่
- [ ] ปฏิเสธ permission แจ้งเตือนแล้วแอปไม่ล่ม
- [ ] แจ้งเตือนมาตรงเวลาตอนปิดแอปอยู่
- [ ] โหมดกลุ่มทดสอบที่ 2 คนและ 8 คน ทั้งสองวิธีเสนอ
- [ ] ไม่มีเมนูผ่านตัวกรอง และชุดเมนูว่าง แอปไม่ล่ม

## Notes
- รอบแรกทดสอบ 16–17 พ.ย. หลังฟีเจอร์ระดับ A ครบ รอบสองทดสอบ 18 พ.ย.
- บั๊กที่บล็อกเดโมเจ้าของต้องแก้ภายในวันที่ถูกแจ้ง

---

## #47 [Test] Kan tests Ping's work (round-robin)

**Owner:** กานต์ · **Level:** A · **Due:** 16–17 พ.ย. (รอบแรก) และ 18 พ.ย. (รอบสอง) · **Estimate:** 3 ชม. · **Milestone:** M3 ปิดโค้ด

## Goal
ทดสอบงานของผิง (การเขย่า การสุ่ม โควตา กรณีขอบ สถานะรอดหมุนจอ และน้ำหนักปรับตัว) บนมือถือจริงตามเช็กลิสต์ เพื่อหาบั๊กที่เจ้าของมองไม่เห็นก่อนปิดโค้ด

## Set Branch Name as
(ไม่ต้องสร้าง — ถ้าต้องแก้บั๊กใช้ `fix/<ชื่อบั๊ก>`)

## Dependencies
**Blocked by** (ต้องเสร็จและ merge เข้า main ก่อน หรือใช้ stub ที่ตกลงกันไว้ไปก่อน):
- #2 — ต้องเสร็จก่อนจึงทดสอบได้
- #4 — ต้องเสร็จก่อนจึงทดสอบได้
- #5 — ต้องเสร็จก่อนจึงทดสอบได้
- #6 — ต้องเสร็จก่อนจึงทดสอบได้
- #7 — ต้องเสร็จก่อนจึงทดสอบได้
- #8 — ต้องเสร็จก่อนจึงทดสอบได้

**Blocks:** #54

## Tasks
- [ ] ทดสอบทุกหน้าของผิง ตามเช็กลิสต์ด้านล่างบนมือถือจริงอย่างน้อย 1 เครื่อง
- [ ] เจอบั๊กให้เปิด Issue ด้วย template บั๊ก ใส่ขั้นตอนทำซ้ำ รุ่นเครื่อง/Android และติดป้าย `blocks-demo` ถ้ากระทบเดโม
- [ ] ทดสอบซ้ำหลังเจ้าของแก้ (รอบสอง 18 พ.ย.)

## Acceptance Criteria
- [ ] หมุนจอกลางหน้านั้นแล้วข้อมูลไม่หาย
- [ ] สลับแอปแล้วกลับมา งานเดิมยังอยู่และทำต่อได้
- [ ] ล็อกจอแล้วปลดล็อก แอปไม่รีเซ็ต
- [ ] กด Back ในหน้าที่ไม่ใช่เมนูหลักแล้วกลับหน้าหลัก ไม่ปิดแอปทันที
- [ ] กด Back กลางฟอร์มที่ยังไม่บันทึกหรือกลางโหมดกลุ่ม ต้องมีกล่องถามยืนยัน
- [ ] สลับภาษาไทย/อังกฤษแล้วไม่มีข้อความหลงภาษาหรือถูกตัด
- [ ] สลับธีมสว่าง/มืดแล้วอ่านออกทุกหน้า
- [ ] ตั้งตัวอักษรเครื่องเป็นใหญ่สุดแล้วไม่มีปุ่มหายหรือข้อความทับกัน
- [ ] ทดสอบแนวนอน จอเล็ก และจอใหญ่
- [ ] ปฏิเสธ permission แจ้งเตือนแล้วแอปไม่ล่ม
- [ ] แจ้งเตือนมาตรงเวลาตอนปิดแอปอยู่
- [ ] โหมดกลุ่มทดสอบที่ 2 คนและ 8 คน ทั้งสองวิธีเสนอ
- [ ] ไม่มีเมนูผ่านตัวกรอง และชุดเมนูว่าง แอปไม่ล่ม

## Notes
- รอบแรกทดสอบ 16–17 พ.ย. หลังฟีเจอร์ระดับ A ครบ รอบสองทดสอบ 18 พ.ย.
- บั๊กที่บล็อกเดโมเจ้าของต้องแก้ภายในวันที่ถูกแจ้ง

---

## #54 [Team] Rehearse the demo twice and prepare the backup plan

**Owner:** แป้ง · **Level:** A · **Due:** 19 พ.ย. · **Estimate:** 2 ชม. · **Milestone:** M4 พร้อมนำเสนอ

## Goal
ซ้อมเดโมบนเครื่องที่จะใช้นำเสนอจริง 2 รอบ และเตรียมแผนสำรอง (งานเอกสารและสไลด์ทำร่วมกันนอก Git)

## Set Branch Name as
(ไม่ต้องสร้าง)

## Dependencies
**Blocked by** (ต้องเสร็จและ merge เข้า main ก่อน หรือใช้ stub ที่ตกลงกันไว้ไปก่อน):
- #43 — ต้องทดสอบเสร็จก่อน
- #44 — ต้องทดสอบเสร็จก่อน
- #45 — ต้องทดสอบเสร็จก่อน
- #46 — ต้องทดสอบเสร็จก่อน
- #47 — ต้องทดสอบเสร็จก่อน

## Tasks
- [ ] ซ้อม 2 รอบตามลำดับเดโม (ผิง → มายด์ → แป้ง → เพลง → กานต์ ประมาณ 7 นาที)
- [ ] เตรียมมือถือสำรอง 1 เครื่อง
- [ ] อัดวิดีโอหน้าจอเดโมไว้เป็นแผนสำรองเผื่อเซ็นเซอร์หรือเครื่องมีปัญหา
- [ ] ติดตั้ง APK เวอร์ชันสุดท้ายบนเครื่องที่ใช้นำเสนอ

## Acceptance Criteria
- [ ] ซ้อมครบ 2 รอบไม่มีบั๊กที่บล็อกเดโม
- [ ] มีวิดีโอสำรองและมือถือสำรองพร้อม

---
