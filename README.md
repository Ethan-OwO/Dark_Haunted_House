# 逃離中央大學

俯視角射擊遊戲，靈感來自元氣騎士（Soul Knight）。  
在中央大學的各個場景中闖關，打倒三位 Boss，成功逃離！

---

## 下載 & 執行（不需安裝 Java）

1. 到右側 **[Releases](../../releases)** 頁面
2. 下載最新版的 `逃離中央大學-1.0.exe`
3. 雙擊執行，即可遊玩

> 若出現 Windows Defender 警告，點「更多資訊」→「仍要執行」即可。

---

## 操作說明

| 按鍵 | 動作 |
|------|------|
| `WASD` | 移動 |
| 滑鼠移動 | 瞄準方向 |
| 滑鼠左鍵 | 射擊 |
| `Q` | 天賦主動技能 |
| `F1` | 開發者模式（Dev Mode） |
| `F2`（Dev Mode 中） | OP 模式（無限 HP） |
| `N`（Dev Mode 中） | 跳下一關 |

---

## 關卡設計

| Stage | 場景 | 小怪 | Boss |
|-------|------|------|------|
| 1 | 工程五館 | 松鼠 | 無小光 |
| 2 | 男13宿舍 | 鵝 | 沉沁汗 |
| 3 | 圖書館/操場 | 白蟻 | 濕幗針 |

每個 Stage 有 3 個 Floor，第 3 層固定是 Boss 房。

---

## 開發者模式執行（需要 Java 17 + Maven）

```bash
git clone https://github.com/Ethan-OwO/Dark-Haunted-House.git
cd Dark-Haunted-House
mvn javafx:run
```

---

## 技術架構

- **語言/框架**：Java 17 + JavaFX 21（Maven）
- **渲染**：Canvas + GraphicsContext
- **遊戲循環**：AnimationTimer

---

## 開發團隊

計算機實習期末專題，三人小組共同開發。
