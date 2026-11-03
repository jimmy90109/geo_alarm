---
name: release-geoalarm
description: 執行與續接 GeoAlarm Android 發版，涵蓋 AAB 建置驗證、Google Play Beta 公開測試與 Production、發版紀錄、PR、tag 及 GitHub Release。用於包 AAB、上傳、送審、正式推出及發版收尾；不適用一般功能開發或 debug 建置。
---

# GeoAlarm 發版

只執行使用者要求的階段。每次續接先核對本機與外部狀態，避免重複上傳、送審、建立 PR、tag 或 Release。讀取本技能不代表已獲得發布授權；既有明確授權有效，不重複要求確認。

## 開始前

1. 閱讀根目錄 `AGENTS.md` 與 [發版參考](references/release-checklist.md)。涉及產品、權限或隱私變更時閱讀 `PRD.md`；涉及 UI 時閱讀 `DESIGN.md`。
2. 檢查 Git 狀態、分支、遠端、相關 diff、版本與 tags，並讀取 `docs/play-store/release-status.md`、`docs/play-store/version-release-checklist-zh-TW.md`。
3. 確定目標版本、變更範圍、軌道、rollout 與要求涵蓋的階段。能從本次指示及現有慣例唯一判定時直接進行，否則只詢問缺少的關鍵資訊。
4. `PlaceReminder*`、`place_reminder_*` 與 Reminders 分頁仍是原型；發版不等於授權將原型納入正式產品。若待發布 revision 包含這些變更，先核對是否已有明確納入授權，未授權時停止發布並指出具體範圍，不自行刪除或修改原型。

## 1. 準備版本與產物

- 版本設定為 `app/build.gradle.kts`。核對 Play 已使用的 version code，沿用專案編碼慣例，不直接照抄 LiveStatus 的版本規則。
- 更新本次繁體中文與英文發布說明及必要文件；對照實際 diff，避免混入尚未交付功能。
- 建置前確認正式 upload key 的簽署方式。release 已改由本機 `keystore.properties` 提供正式 signing config；先執行 `./gradlew verifyReleaseSigning` 驗證 keystore、alias 與密碼，不可把一般 `bundleRelease` 成功直接當成 Play 憑證一致的證據。依發版參考驗證實際 AAB 憑證；若尚未具備正式簽署方式，回報此阻擋，不自行產生或更換金鑰。
- 執行 `./gradlew verifyReleaseSigning testDebugUnitTest lintRelease bundleRelease`；正式簽署另依已確認的設定建置。依實際任務與輸出定位產物，不假設 IDE 匯出的 `app/release/` 是最新建置。
- 檢查精確／背景定位、通知、精確鬧鐘、前景服務、全螢幕到站提醒、電池限制引導、單一啟動鬧鐘及本機目的地資料處理。依變更執行必要測試；儀器測試只可用專用拋棄式模擬器並明確設定 `ANDROID_SERIAL`。
- 確認 AAB package、版本、簽章與預期一致，記錄來源 revision、工作樹差異、建置命令、檔案路徑及 SHA-256。程式、資源、設定或依賴輸入變更後，重新建置；相同已驗證輸入可重用產物。
- GeoAlarm release 啟用 R8：保存同一建置的 `mapping.txt`，確認 AAB 內嵌 mapping 或按需上傳。若產生 native debug symbols，核對其為同一建置並上傳；無 native symbols 時先確認原因，不盲目套用 LiveStatus 的固定 zip 路徑。

## 2. Beta 公開測試

涉及 Play 時讀取 [gplay 操作](references/gplay-publishing.md)。預設新測試發版使用 Beta／Open testing；先核對實際 track 與公開測試設定，不擅自改動其他測試軌道。

1. 確認選定 profile、package、現有 release 與 version code，再驗證本次已簽署 AAB 與 notes。
2. 在同一 Play edit 上傳 AAB 與所需的 mapping／native symbols，再設定 Beta release、驗證 edit。
3. commit 前展示版本、軌道、rollout、notes、簽章驗證及產物雜湊。已明確要求 Beta 發布且未指定比例時，採 Beta 軌道內 100%。
4. 只要求上傳／準備時不得送審；要求送審或完整 Beta 發版時依既有授權提交。
5. 讀回 track，並在 Play Console 確認發布總覽、審查狀態、警告、裝置支援變化、國家／地區及測試者設定。API release 的 `completed` 不代表審查通過或已公開。
6. 確認送審中後，記錄事實並立即結束；不輪詢、不等待過審、不建立自動化。等使用者要求查詢或表示過審時才重新確認並續接。

## 3. Production

- Beta 的授權不延伸到 Production 或服務帳號權限變更。先核對本次是否已明確授權目標版本及 rollout；缺少時提供具體預覽後詢問。
- 確認使用者指定的測試／審查條件已達成，重用已驗證的相同版本產物；核對 upload key、version code、來源與雜湊。
- 核對 Play 警告與裝置支援損失；若出現非預期影響，提供實際差異，等待決定。
- 按指定比例推出；明確要求全面發布才採 100%。送審後同樣停止，不把送審記成上線。

## 4. 文件與 Git 收尾

1. 更新 `docs/play-store/release-status.md`，只記錄已確認的版本、軌道、日期、狀態、來源、驗證、產物與雜湊。可重用清單不累積個別版本勾選狀態。
2. `git diff --check` 並檢視待提交範圍。只按明確授權 stage／commit／push／開 PR／merge，不夾帶產物、秘密或無關變更。
3. 沿用 `dev → main` 發版路徑；新工作分支用 `codex/`。PR 預設 Draft；檢查 checks、review 及 mergeability，只有明確要求才轉 Ready／merge。不刪除長期 `dev`。
4. 要求 tag 時 fetch 遠端 `main` 與 tags，確認發版 revision 已在 `main`；核對本機及遠端同名 tag 後，以 `v<versionName>` 建立 annotated tag。正確既有 tag 直接重用，不移動或覆寫。
5. 要求 GitHub Release 時依 [文案規則](references/release-checklist.md#github-release-文案) 從 tag 的實際內容撰寫繁體中文 notes。正式版標題使用 `<versionName>`，非 Draft／prerelease；新穩定版設 Latest，補舊版時保留較新 Latest。預覽版依明確要求標示 prerelease。
6. 建立或更新後讀回並核對 title、body、tag target、附件及 Draft／prerelease／Latest。不使用自動 notes，不預設附產物。

## 中斷、續接與交付

外部操作失敗先確認 edit、bundle、track、PR、tag 或 Release 是否部分完成，再決定重試。需新授權、簽章不符、驗證失敗或外部審查時，回報具體狀態及下一步，不重跑已完成階段。

交付列出版本與 version code、實際驗證及限制、AAB／mapping／symbols 路徑與 SHA-256、已確認 Play 狀態，以及已完成的 commit／PR／tag／Release 連結。使用繁體中文，不宣稱未執行的驗證通過。
