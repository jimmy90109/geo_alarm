# GeoAlarm 發版參考

## 專案識別與檔案

- Repository：`jimmy90109/geo_alarm`
- Module：`app`；正式 package：`com.github.jimmy90109.geoalarm`；debug package 有 `.debug` 後綴，不可上傳。
- 開發分支：`dev`；正式分支：`main`。
- 新正式 tag：`v<versionName>`，沿用近期 `v1.4.0`、`v1.5.0`；歷史含 version code 的 tag 不改名。
- 版本與簽署：`app/build.gradle.kts`；依賴：`gradle/libs.versions.toml`。
- 發版紀錄：`docs/play-store/release-status.md`。
- 每版清單：`docs/play-store/version-release-checklist-zh-TW.md`。
- 發布說明：`docs/play-store/release-notes.md`，依版本保存繁體中文與英文內容。
- 產品規則：`PRD.md`；隱私政策：`docs/privacy-policy.html`；產品網站：`docs/index.html`。

## 正式簽章與建置

技能建立時版本為 `1.5.0 (2610050)`，僅為本機基線，不表示 Play 狀態；每次從目前 revision 與 Play 重新確認。release 現使用獨立 signing config，從不進 Git 的 `keystore.properties` 讀取 `storeFile`、`storePassword`、`keyAlias`、`keyPassword`；參考 `keystore.properties.example`。`verifyReleaseSigning` 驗證 keystore、alias 與私鑰密碼，release 簽章驗證會依賴此 task。尚須核對實際憑證是否符合 Play upload key。

發版時先確認現有的 IDE 或外部簽署流程；一般建置成功不代表正式簽署完成。檢查 AAB 的 JAR 簽章與憑證 SHA-256，與 Play Console「應用程式完整性」的 upload key certificate 比對，勿與 Play App Signing 的 app signing key 混淆。可用 `jarsigner -verify -verbose -certs <aab>` 與 `keytool -printcert -jarfile <aab>`；只查看公開憑證資訊，不輸出密碼或私鑰。使用可用的 bundletool 或等效工具核對 bundle manifest 的 package、versionName 與 versionCode。

若需要改造 Gradle 正式簽署設定，先在本次授權範圍內實作並驗證，禁止 debug fallback；缺少正式 key 或密碼時停止上傳。秘密只由既有安全設定提供，不新增到 tracked 檔案，也不讀出 `local.properties` 的秘密值。

基本檢查：

```sh
./gradlew verifyReleaseSigning testDebugUnitTest lintRelease bundleRelease
```

常見產物路徑如下，以實際建置輸出為準：

```text
app/build/outputs/bundle/release/app-release.aab
app/build/outputs/mapping/release/mapping.txt
app/build/outputs/native-debug-symbols/release/
```

核對 R8 mapping 是否已內嵌於 AAB；若另傳，使用相同建置的檔案。native symbols 僅在有相應 native library 與符號產物時處理，不以 ZIP 存在推定內容有效。

```sh
shasum -a 256 <aab-path> <mapping-path>
```

若有 symbols，亦計算 SHA-256。記錄 revision 與未提交建置輸入，避免誤用同名舊產物。無需為此執行 LiveStatus 的 YouBike 更新工具。

## 發版驗收重點

- 定位與背景權限、通知、精確鬧鐘、前景服務、全螢幕提醒及電池引導。
- 單一啟動鬧鐘、排程、到站停止及 widget／外部入口的驗證。
- 原型是否獲准納入本次正式版；依實際 revision 判定。
- Room／DataStore 變更的升級相容性；目的地維持本機處理。
- Maps／Places 與選用 AdMob 設定是否符合本次產品配置，避免輸出 key。
- 英文與台灣繁體中文、README／隱私政策與實際行為一致。
- 裝置測試只用明確指定的專用模擬器，不碰日常實機 debug 資料。

## GitHub Release 文案

先讀最近一個正式 Release 的格式，再以此結構撰寫，內容只取自該 tag 的發布說明與實際 diff／PR：

```markdown
## 更新內容

- <本版實際更新>

## 發布資訊

- 版本：`<versionName> (<versionCode>)`
- Google Play：<已確認的軌道、推出比例與審查／上線狀態>
- 支援 Android <從該 tag 的 minSdk 核對的系統版本> 以上版本。
```

版本與 minSdk 必須從 tag 的 `app/build.gradle.kts` 取得。沒有 Production 已公開的證據，不寫「已全面推出」。不加自動 notes、commit 清單、compare URL 或附件，除非使用者要求。編修舊 Release 不改動較新版的 Latest。
