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

## 1.5.2 (2611180) — Beta 審查中（2026-11-18）

- 來源：`dev` 的 `da0643db5eccfff87551aacef8a369be221f7aff`；建置輸入包含工作樹既有的 `app/build.gradle.kts` 版本變更（1.5.1 → 1.5.2，2611030 → 2611180）。發布說明與本紀錄為本次新增的文件變更。
- 使用者明確同意本版包含地點提醒原型變更並發佈 Beta；原型尚未升格為正式產品規格。
- 範圍：排程時間／星期選擇器、鬧鐘／排程／提醒安全邊界與橫向排版、提醒英制距離與半徑刻度、選擇器轉場；Material 3 更新為 1.5.0-alpha24。
- 差異未變更 manifest 權限、背景服務、資料庫、排程觸發及單一啟動鬧鐘邏輯。未執行裝置端回歸驗證。
- `./gradlew verifyReleaseSigning testDebugUnitTest lintRelease bundleRelease --console=plain` 成功；129 個 JVM 測試通過，0 failure/error/skipped；lint 0 Error、168 Warning、7 Hint。
- 儀器測試未執行：未確認可用的專用拋棄式模擬器，未操作日常實機 debug app 資料。
- AAB：`app/build/outputs/bundle/release/app-release.aab`；17,293,911 bytes；package `com.github.jimmy90109.geoalarm`、versionName `1.5.2`、versionCode `2611180`、minSdk 31、targetSdk 37，已由 artifact scanner 核對。
- AAB SHA-256：`0f35ac3a4c4def650a1276039df0c38ba94dfb0fc2c3e9b6b8d65f443ef29963`。
- mapping：`app/build/outputs/mapping/release/mapping.txt`；SHA-256：`e101978831476d82a6ff1a00e73c5c9f4725a34e440c24ba1464b56727498b79`；與 AAB 內嵌 `BUNDLE-METADATA/com.android.tools.build.obfuscation/proguard.map` 完全一致。
- `jarsigner -verify` 成功；簽署者 `CN=Jim H`；SHA-256：`6A:FF:22:FD:F2:D0:9D:C8:FE:5C:B4:5C:01:6F:F5:9B:7D:1C:91:64:9D:A1:19:C3:1E:F4:F1:BF:D4:A7:63:3C`，與上一版被 Play 接受的憑證相同；未人工比對 Console upload certificate。
- 未產生 native symbols ZIP，`extractReleaseNativeSymbolTables/out` 為空；本版只有既有 AndroidX graphics path 與 DataStore 預編譯 native libraries，未新增自有 native code，未另傳 symbols。預檢仍提示 DataStore library 的內含符號。
- `gplay validate`（明確使用 `tw-live-noti` profile）完成遠端核對：0 blocking、13 warning、17 info、2 manual follow-up。警告涉及既有權限、backup、Google API key 限制、DataStore native symbols、Gradle metadata 與缺少本機 app-content inventory。
- Console 的發布總覽、裝置支援、國家／地區、測試者設定及政策頁未人工核對：Safari 電腦操作未獲允許。
- 未建立 commit、push、PR、tag 或 GitHub Release；Production 未操作。
- 同一 edit 完成 AAB 上傳、Beta 更新、`edits validate` 及送審 commit；上傳回應的 version code 與 SHA-256 均符合本機產物。mapping 已內嵌，未重複上傳。
- Beta 軌道內設定 100%。提交後 API 讀回：`1.5.2 / 2611180` 為 `RELEASE_LIFECYCLE_STATE_IN_REVIEW`；上一版 `1.5.1 / 2611030` 仍為 `RELEASE_LIFECYCLE_STATE_PUBLISHED`。尚未確認 1.5.2 過審或公開。
- 下一步：等待 Google Play 審查；已停止查詢，未建立輪詢或自動化。待使用者要求查詢或續接時再核對狀態。

## 1.5.2 (2611190) — Beta 已公開（2026-11-19）

- 續接前 API 已確認 `1.5.2 / 2611180` 在 Beta 為 `RELEASE_LIFECYCLE_STATE_PUBLISHED`。
- 使用者要求建置 `2611190` 並上 Beta；沿用先前明確同意的 1.5.2 原型範圍。本次增加 `android.hardware.wifi` 的 `required="false"`，保留 Places SDK 帶入的 `ACCESS_WIFI_STATE` 權限，以取消非必要的 Wi-Fi 裝置篩選。
- 來源 revision：`da0643db5eccfff87551aacef8a369be221f7aff`；工作樹建置輸入：`app/build.gradle.kts` 的 `1.5.2 / 2611190` 版本設定，以及 `app/src/main/AndroidManifest.xml` 的 Wi-Fi 選用宣告。發布說明與發版紀錄另有未提交文件變更。
- `./gradlew verifyReleaseSigning testDebugUnitTest lintRelease bundleRelease --console=plain` 成功；129 個 JVM 測試通過，0 failure/error/skipped；lint 0 Error、168 Warning、7 Hint。release 合併 Manifest 已確認 Wi-Fi 為選用。
- 儀器測試未執行：未確認可用的專用拋棄式模擬器，未操作日常實機 debug app 資料。相較 2611180，未變更定位／通知權限、背景服務、資料庫或鬧鐘邏輯。
- AAB：`app/build/outputs/bundle/release/app-release.aab`；17,293,961 bytes；SHA-256：`35031463902eed01cf60e0d530ffdc9ad82724fe4faf972608c224fbadf58508`。artifact scanner 確認 package `com.github.jimmy90109.geoalarm`、versionName `1.5.2`、versionCode `2611190`、minSdk 31、targetSdk 37。
- mapping：`app/build/outputs/mapping/release/mapping.txt`；SHA-256：`e101978831476d82a6ff1a00e73c5c9f4725a34e440c24ba1464b56727498b79`；與 AAB 內嵌 mapping 完全相同，未另傳。未產生 native symbols ZIP，native libraries 仍為既有 AndroidX graphics path 與 DataStore 預編譯程式庫，未新增自有 native code。
- `jarsigner -verify` 成功；簽署者 `CN=Jim H`；憑證 SHA-256：`6A:FF:22:FD:F2:D0:9D:C8:FE:5C:B4:5C:01:6F:F5:9B:7D:1C:91:64:9D:A1:19:C3:1E:F4:F1:BF:D4:A7:63:3C`，與先前 Play 接受的憑證一致。
- `gplay validate` 使用 `tw-live-noti` profile：0 blocking、13 warning、17 info、2 manual follow-up；警告項目與 2611180 相同。Console 專屬的裝置支援、國家／地區、測試者設定及政策頁仍未人工核對，先前 Safari 操作未獲允許。
- 未操作 Production，未建立 commit、push、PR、tag 或 GitHub Release。
- 同一 edit 完成 AAB 上傳、Beta 100% 更新、edit 驗證與送審提交；Play 回傳 SHA-256 與本機一致。
- 提交後讀回 Beta：`2611190` 為 `RELEASE_LIFECYCLE_STATE_IN_REVIEW`，`2611180` 仍為 `RELEASE_LIFECYCLE_STATE_PUBLISHED`。尚未確認 2611190 過審或公開。
- 已停止查詢，不輪詢或建立自動化；等待使用者要求查詢／續接。
- 收尾時觀察到 HEAD 已由外部操作前進至 `ce6931d`（僅提交本次相同的 Wi-Fi 宣告）；本次發版流程未執行 Git commit。建置輸入仍符合上述來源與版本設定。
- 2026-11-19 使用者通知已過審後重新查核：Beta API 僅列出 `1.5.2 / 2611190`，狀態為 `RELEASE_LIFECYCLE_STATE_PUBLISHED`，確認 Beta 已公開。上方審查中紀錄保留為提交當時狀態。
- Production 尚未操作；後續須依使用者指定的正式版推出範圍與比例續接。

### Git 收尾（2026-11-19）

- 版本與發版文件已提交為 `a20614f`（Prepare 1.5.2 release metadata），推送至 `dev`；另以 `94454b4` 同步上一版 `main` 合併紀錄，未改變建置輸入。
- [PR #33](https://github.com/jimmy90109/geo_alarm/pull/33) 的兩項 `build` CI 均通過。首次合併因審查規則受阻；使用者完成 GitHub 操作後，讀回確認已合併至 `main`，merge revision 為 `75ed8969b18ee63d52eb33a632c53869d5acd9b1`。
- 已建立並推送 annotated tag `v1.5.2`，指向上述 `main` revision；tag 內版本為 `1.5.2 (2611190)`、minSdk 31。
- [GitHub Release 1.5.2](https://github.com/jimmy90109/geo_alarm/releases/tag/v1.5.2) 已發布；讀回確認非 Draft、非 prerelease、為 Latest，未附產物。繁體中文說明標示 Beta 已公開、Production 尚未推出。
- 長期 `dev` 分支保留；本次未操作 Google Play Production。
