# 3.1.0 visual regression
- GUI scale 2, 3 and 4: open inventory > journal. All five pages, title, charts, labels and buttons must remain sharp regardless of menu blur setting.
- Switch between pages repeatedly; refresh packet must update without reopening. Esc / Inventory returns to previous screen.
- HUD: compare copper corner bevels, meter alignment at 0/50/100%, penalty strip; no alpha blur. Existing position configuration retained.
- Check Russian/English and recipe-book layout. F3 or third-party debug overlay may still overlap the HUD; verticalOffset remains available.
- Check 158x68 HUD, 158x15 hint and 16x16 journal frame packaged in JAR. No gameplay values changed.
