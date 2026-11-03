# GeoAlarm gplay 操作

僅在查詢或修改 Google Play 時閱讀。以下是操作順序；實際參數以目前安裝的 CLI `--help` 為準，寫入前逐一核對。

## 認證與唯讀核對

- Package 固定為 `com.github.jimmy90109.geoalarm`，不可沿用 LiveStatus package 或 `.debug`。
- GeoAlarm profile 名稱尚未確認。使用 CLI 支援的唯讀 profile 資訊辨識，核對其對目標 package 的實際權限；不要改寫全域預設 profile 或假定 `tw-live-noti` 適用。
- 不讀出、複製或提交服務帳號 JSON、私鑰、token；不自行建立、下載或輪替憑證。需要變更帳號權限時確認已有明確授權。

```sh
gplay version
gplay auth doctor
gplay tracks releases list --package com.github.jimmy90109.geoalarm --track beta --pretty
```

按 CLI 說明明確選定正確 profile，再執行以上查詢。`auth doctor` 不證明 Play API 權限；實際 track API 才能核對。`apps list` 另依賴 Reporting API，不作為 Publisher 發版的必要 gate。403 時區分 API 啟用、package 權限與權限傳播，不盲目擴權或重建金鑰。

## 驗證與同一 edit 上傳

1. 使用 `gplay validate` 搭配目標 package、`--track beta`、`--bundle <signed-aab>` 及 `--release-notes @<json>`，確認無 blocking issue。notes 陣列元素形如 `{"language":"zh-TW","text":"本版發布說明"}`；本專案亦提供相符英文 notes。
2. `gplay edits create` 建立 edit；保存 edit ID 供本次操作續接。
3. `gplay bundles upload` 在同一 edit 上傳已驗證 AAB，核對回傳 version code。
4. 先確認 AAB 內嵌 mapping／symbols 是否已被接受。需另傳時用 `gplay deobfuscation upload`，指定同一 edit、`--apk-version <version-code>` 及相同建置檔案；mapping 使用 `--type proguard`，native symbols 使用 `--type nativeCode`，以當前 help 核對。
5. `gplay tracks update` 在同一 edit 更新 Beta，`--releases @<json>` 內容只包含本次明確要保留／發布的 release，先讀現況避免覆蓋其他應保留版本。Beta 100% 的本次項目為：

   ```json
   [{
     "name": "<versionName>",
     "status": "completed",
     "versionCodes": ["<versionCode>"],
     "releaseNotes": [
       {"language": "zh-TW", "text": "<繁體中文發布說明>"},
       {"language": "en-US", "text": "<English release notes>"}
     ]
   }]
   ```

6. `gplay edits validate` 後，預覽版本、軌道、status、rollout、notes、檔案及雜湊。
7. 僅上傳／準備時，以 CLI 支援的 `--changes-not-sent-for-review` commit；只有送審或完整 Beta 發版授權才使用提交審查的 commit。若 API 行為或帳號狀態無法滿足要求，停止並回報，不自行改成送審。
8. 讀回 track，並在 Play Console 核對實際審查／公開狀態及 API 未涵蓋的警告。`completed` 是 track release status，不等於過審。

一次性 JSON 放 repository 外暫存目錄；edit ID 可在出錯交付中回報供續接，不寫入永久發版文件。失敗後先查 edit／track 與已上傳 bundle，不盲目重傳、建立第二個 edit 或再次 commit。刪除 edit 前確認授權。

避免以自動 commit 的一鍵 publish 取代上述流程，除非已確認其能滿足本次符號上傳與不送審的要求。

## Production 與停止條件

唯讀核對使用同一 package 搭配 `--track production`。只有已明確授權 Production 目標版本、比例及動作時才 promote／更新／commit；Beta 授權不延伸至 Production。沿用相同已驗證 version code；分階段 rollout 使用 CLI 支援的正確狀態與比例，不把 Beta 的 `completed` 範例直接套用。

送審成功且確認審查中後記錄並停止，不輪詢或建立自動化。使用者要求查詢／續接時再讀外部最新狀態。
