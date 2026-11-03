# GeoAlarm 發版紀錄

## 流程建立基線（2026-11-03）

- 本機 `app/build.gradle.kts`：`1.5.0 (2610050)`。這不是 Google Play 已發布狀態的證據。
- 發版路徑：Beta 公開測試 → 按授權推出 Production；Git 使用 `dev → main`。
- 目前 Gradle release 指向 debug signing；正式上傳前須確認正式 upload key 簽署方式並核對實際 AAB 憑證。沒有 `verifyReleaseSigning` task。
- 本次僅建立技能與文件，未建置 AAB、查詢或變更 Play／GitHub 外部狀態。
- GeoAlarm 專用 gplay profile、服務帳號權限、目前 Play 版本與審查狀態：尚未核對。

## 每版紀錄格式

每次發版新增一筆，未知項目標示「未核對」，只填已確認的事實：

- 日期、versionName、versionCode。
- 來源 revision、工作樹建置輸入差異與發布範圍。
- 驗證命令、結果與未執行項目的原因。
- 簽章公開憑證核對結果。
- AAB／mapping／適用 native symbols 的路徑及 SHA-256。
- Play 軌道、rollout、審查／公開狀態、查核日期與證據來源。
- 已完成的 commit／PR／merge／tag／GitHub Release。
- 下一步與尚待使用者或外部審查處理的項目。

## 正式簽署設定（2026-11-03）

- Gradle release 已改用獨立的 release signing config，由本機且不進 Git 的 `keystore.properties` 提供設定，不再使用 debug signing。
- 新增 `verifyReleaseSigning`，檢查必要欄位、keystore、alias 及私鑰密碼；release 簽章驗證依賴此檢查。
- 已指定既有 keystore 路徑；alias 與密碼尚待在本機填入。尚未核對 Play upload certificate，尚未產生正式簽署產物。

不保存密碼、token、服務帳號 JSON 或一次性 edit ID。API track 的 `completed` 不表示過審；送審中與正式上線分開記錄。

## 1.5.1 (2611030) — Beta 已公開（2026-11-03）

- 來源：`dev` 的 `ec80f64` 加上工作樹中的版本與正式簽署設定變更；未建立 commit、PR、tag 或 GitHub Release。
- 使用者已明確同意本版 Beta 包含目前 dev 的地點提醒原型變更；原型尚未因此升格為正式產品規格。
- 版本：`1.5.1`；version code：`2611030`；package：`com.github.jimmy90109.geoalarm`。
- 發布說明：[release-notes.md](release-notes.md)，包含 `zh-TW` 與 `en-US`。
- 本次使用 `tw-live-noti` profile；使用者補上 GeoAlarm 權限後，查詢、上傳、更新及 commit 均成功，未變更全域預設 profile。
- 驗證：`./gradlew verifyReleaseSigning testDebugUnitTest lintRelease bundleRelease --console=plain` 成功；124 個 JVM 測試全部通過；lint 無 Error，167 個 Warning、7 個 Hint。
- 儀器測試未執行：未確認可用的專用拋棄式模擬器，沒有對日常 debug app 安裝、清除資料或執行測試。
- AAB：`app/build/outputs/bundle/release/app-release.aab`；16,960,065 bytes。
- AAB SHA-256：`438ac3dcf92991815964ec9a7a4759488aab83995e4ac9ad5c74a7e310b14503`；Google Play 上傳回應的雜湊與本機一致。
- R8 mapping：`app/build/outputs/mapping/release/mapping.txt`。
- mapping SHA-256：`81c0c1f5253c5f4b94cfd1475af070ec655de1ef451be3f3a8ce712efa4e28c5`；已核對與 AAB 內嵌 `BUNDLE-METADATA/com.android.tools.build.obfuscation/proguard.map` 完全相同，未重複上傳。
- 未產生獨立 native debug symbols ZIP；bundle 包含 AndroidX graphics path 與 DataStore 的 native libraries；未另傳 native symbols。
- `jarsigner -verify` 成功，簽署者為 `CN=Jim H`，憑證 SHA-256：`6A:FF:22:FD:F2:D0:9D:C8:FE:5C:B4:5C:01:6F:F5:9B:7D:1C:91:64:9D:A1:19:C3:1E:F4:F1:BF:D4:A7:63:3C`。Play 接受本次上傳；未透過 Console 人工比對憑證。
- `gplay validate`：0 blocking、13 warning、17 info、2 manual follow-up。警告涵蓋既有權限聲明、backup、Google API key 限制、DataStore native symbols 與缺少本機 app-content inventory；未為此變更權限或商店政策聲明。
- 同一 edit 的 AAB 上傳、Beta 更新、`edits validate` 及送審 commit 成功。Beta 軌道內設定 100%；Production 未操作。
- commit 後讀回 `tracks releases list`：本次 `1.5.1 / 2611030` 為 `RELEASE_LIFECYCLE_STATE_IN_REVIEW`，上一版 `2610050` 仍為 `RELEASE_LIFECYCLE_STATE_PUBLISHED`。
- Console 頁面的裝置支援變化、政策警告、國家／地區與發布總覽未人工核對：Safari 電腦操作未獲允許。API 審查狀態已確認，但不表示已過審或公開。
- 下一步：等待審查；本次送審後停止查詢，未建立輪詢或自動化。使用者要求查詢／續接時再確認外部狀態。

### Git 收尾續接（2026-11-03）

- 使用者要求 commit、push、tag 與 GitHub Release。
- 重新讀取 Beta API，確認 `1.5.1 / 2611030` 已為 `RELEASE_LIFECYCLE_STATE_PUBLISHED`；Production 未操作，未宣稱正式版全面推出。
- 以下 Git 操作按實際結果續記；上方送審紀錄保留為當時狀態。
