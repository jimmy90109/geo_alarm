# GeoAlarm 每版發佈清單

操作流程以 [release-geoalarm](../../.agents/skills/release-geoalarm/SKILL.md) 為準。本清單可重複使用，個別版本結果記入 [release-status.md](release-status.md)。

- 確認目標版本、version code、來源 revision、發佈範圍、軌道與 rollout。
- 確認原型變更是否已有明確納入正式版授權。
- 檢查 Play 已使用的 version code、版本設定、產品行為與 [發布說明](release-notes.md)。
- 核對正式 upload key 簽章；debug signing 不得上傳。
- 執行 `./gradlew verifyReleaseSigning testDebugUnitTest lintRelease bundleRelease` 與本次必要驗證；不以一般 bundle task 成功作為 Play 憑證一致的證據。
- 檢查定位、背景服務、通知／到站提醒、排程、單一啟動鬧鐘與資料升級路徑。
- 裝置測試只用明確指定的專用模擬器，不清除日常 debug app 資料。
- 核對 AAB package、版本與公開憑證，保存同一建置 mapping 及適用的 native symbols；記錄路徑、SHA-256、命令與來源。
- 以正確 GeoAlarm profile 查詢 Play；validate 後於同一 edit 上傳、更新軌道、驗證並按授權 commit／送審。
- 檢查 Play Console 警告、裝置支援、國家／地區、測試設定與實際發布狀態。送審中即停止，等使用者要求續接。
- Production 僅依明確授權的版本與比例推出，不從 Beta 授權推定。
- 更新發版紀錄、README、網站或隱私政策中受本版影響的內容。
- 按授權完成 commit／push、`dev → main` PR、checks／merge、annotated tag 與 GitHub Release，並讀回驗證。
