# 指令說明文件

> 所有指令僅在**開發人員模式**下可用。
> 開啟方式：遊戲中按 `F1` 啟用 Dev Mode，再按 `Enter` 開啟指令列。

---

## 操作方式

| 按鍵 | 功能 |
|------|------|
| `F1` | 切換 Dev Mode（關閉時同時關閉 OP Mode） |
| `F2` | 切換 OP Mode（需先開啟 Dev Mode） |
| `N` | 跳至下一個 Floor |
| `Enter` | 開啟 / 送出指令列 |
| `Escape` | 取消指令列 |
| `Backspace` | 刪除最後一個字元 |

---

## OP Mode 效果

| 效果 | 說明 |
|------|------|
| 無限 HP | 每幀自動恢復滿血，免疫所有狀態效果 |
| 9999 傷害 | 玩家子彈傷害固定 9999 |
| 移動速度 ×3 | 覆蓋緩速與加速效果 |

---

## 指令清單

### `/help`
列出所有可用指令及簡短說明。

```
/help
```

---

### `/summon <名稱>`
在玩家旁邊召喚指定的怪物或 Boss。
召喚時遊戲不會自動鎖房間，適合用於測試。

```
/summon <名稱>
```

**可召喚實體：**

| 名稱（英文） | 名稱（中文） | 類型 | 說明 |
|-------------|-------------|------|------|
| `squirrel` | `松鼠` | 普通小怪 | Stage 1 小怪 |
| `goose` | `鵝` | 普通小怪 | Stage 2 小怪 |
| `termite` | `白蟻` | 普通小怪 | Stage 3 小怪 |
| `wuxiaoguang` | `無小光` | Boss | Stage 1 Boss，隱形+地雷 |
| `chenqinhan` | `沉沁汗` | Boss | Stage 2 Boss，鑽地+邏輯閘子彈 |
| `shiguozhen` | `濕幗針` | Boss | Stage 3 Boss，液體軌跡+程式語言子彈 |

**範例：**
```
/summon squirrel
/summon 無小光
/summon chenqinhan
```

---

### `/shop`
在玩家**當前房間原地疊加一個商店**（SHOP RewardRoom Overlay）。  
商人與三件商品；按 `1` / `2` / `3` 購買。  
換樓層（`N` 或走傳送門）時自動關閉。

```
/shop
```

---

### `/chest`
在玩家**當前房間原地疊加一個寶箱**（CHEST RewardRoom Overlay）。  
按 `E` 開啟，隨機獲得一種獎勵。  
換樓層（`N` 或走傳送門）時自動關閉。

```
/chest
```

---

### `/closeshop`
關閉由 `/shop` 或 `/chest` 開啟的疊加物件，恢復當前房間的正常顯示。

```
/closeshop
```

---

## 新增指令備忘

新增指令時需要同步更新以下兩個地方：
1. `GameScene.java` → `executeCommand()` 方法加入新的 case
2. `GameScene.java` → `execHelp()` 方法更新說明文字
3. 本文件（`COMMANDS.md`）新增對應條目
