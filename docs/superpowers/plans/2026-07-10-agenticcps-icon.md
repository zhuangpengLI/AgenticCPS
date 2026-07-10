# AgenticCPS Icon Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Generate the approved AgenticCPS chip-and-rebate-loop icon and use it consistently for the admin Logo, default avatar, and primary tenant administrator avatar.

**Architecture:** Generate one square PNG master with the built-in image generation tool, then derive the existing PNG/GIF asset names so current Vue imports remain unchanged. Publish a stable `/agenticcps-icon.png` copy for database avatar URLs, update administrator seed rows, and synchronize the current local database.

**Tech Stack:** OpenAI built-in image generation, PNG/GIF assets, Pillow, Vue 3/Vite static assets, MySQL, SQL seed scripts.

---

### Task 1: Generate and validate the brand icon

**Files:**
- Create: `frontend/admin-vue3/public/agenticcps-icon.png`
- Modify: `frontend/admin-vue3/src/assets/imgs/logo.png`
- Modify: `frontend/admin-vue3/src/assets/imgs/avatar.gif`
- Modify: `frontend/admin-vue3/public/logo.gif`

- [x] **Step 1: Generate the approved square master**

Use the built-in image generation tool with this prompt:

```text
Use case: logo-brand
Asset type: square application icon for a CPS affiliate rebate and AI agent platform
Primary request: Create a premium rounded-square AgenticCPS brand icon. Place a bold AI microchip in the center and wrap it with two clear circular rebate arrows that form a continuous business loop.
Style/medium: clean vector-friendly digital logo, minimal geometric forms, subtle dimensional depth, crisp edges
Composition/framing: centered and symmetrical, generous safe padding, readable at 24px, 32px, and 40px
Color palette: deep navy #071E3D background and electric cyan #20E3D2 symbol, with restrained lighter cyan highlights
Constraints: no text, no letters, no coins, no people, no mascot, no watermark, no fine circuit clutter, no photorealism; strong silhouette on dark and white UI surfaces
```

Expected: one 1024×1024 square image whose chip and two-arrow loop are recognizable without text.

- [x] **Step 2: Inspect the generated master**

Open the generated image and confirm all acceptance points: centered chip, exactly two dominant loop arrows, deep navy/electric cyan palette, no text, and no edge clipping.

- [x] **Step 3: Save and derive project assets**

Copy the accepted PNG to `frontend/admin-vue3/public/agenticcps-icon.png` and `frontend/admin-vue3/src/assets/imgs/logo.png`. Use Pillow to write animated-compatible static GIF copies to `frontend/admin-vue3/src/assets/imgs/avatar.gif` and `frontend/admin-vue3/public/logo.gif` without changing aspect ratio.

- [x] **Step 4: Validate formats and small-size readability**

Run:

```powershell
python -c "from PIL import Image; from pathlib import Path; paths=['frontend/admin-vue3/public/agenticcps-icon.png','frontend/admin-vue3/src/assets/imgs/logo.png','frontend/admin-vue3/src/assets/imgs/avatar.gif','frontend/admin-vue3/public/logo.gif']; [(lambda im,p: print(p, im.format, im.size))(Image.open(p),p) for p in paths]"
```

Expected: PNG assets decode as PNG, GIF assets decode as GIF, and every file is square.

### Task 2: Update default administrator avatar data

**Files:**
- Modify: `backend/sql/mysql/ruoyi-vue-pro.sql`
- Modify: `backend/sql/postgresql/ruoyi-vue-pro.sql`
- Modify: `backend/sql/kingbase/ruoyi-vue-pro.sql`
- Modify: `backend/sql/opengauss/ruoyi-vue-pro.sql`
- Modify: `backend/sql/oracle/ruoyi-vue-pro.sql`
- Modify: `backend/sql/sqlserver/ruoyi-vue-pro.sql`
- Modify: `backend/sql/dm/ruoyi-vue-pro-dm8.sql`

- [x] **Step 1: Prove the seed rows still reference legacy remote avatars**

Run:

```powershell
rg -n "INSERT INTO .*system_users.*VALUES \(1, .*avatar" backend/sql
```

Expected: primary administrator seed rows contain remote `http://test.qiji.iocoder.cn/...` avatar URLs.

- [x] **Step 2: Replace only the primary administrator seed avatar**

For the `system_users` row with `id=1` and `tenant_id=1`, replace the avatar column value with `/agenticcps-icon.png` in each supported database dialect. Preserve passwords, names, timestamps, and all unrelated seed data.

- [x] **Step 3: Synchronize the current local database**

Read the local datasource configuration as UTF-8 using `yaml.safe_load_all`, connect to the configured MySQL database, and execute:

```sql
UPDATE system_users
SET avatar = '/agenticcps-icon.png'
WHERE id = 1 AND tenant_id = 1 AND deleted = 0;
```

- [x] **Step 4: Verify seed and live database values**

Run a UTF-8 script that asserts every primary administrator seed row contains `/agenticcps-icon.png`, then query the local database and assert the current `id=1, tenant_id=1` avatar equals `/agenticcps-icon.png`.

### Task 3: Verify frontend consumption and visual result

**Files:**
- Verify: `frontend/admin-vue3/src/layout/components/Logo/src/Logo.vue`
- Verify: `frontend/admin-vue3/src/layout/components/UserInfo/src/UserInfo.vue`
- Verify: `frontend/admin-vue3/src/views/Home/Index.vue`

- [x] **Step 1: Confirm existing consumers use the replaced asset names**

Run:

```powershell
rg -n "assets/imgs/logo.png|assets/imgs/avatar.gif|agenticcps-icon.png" frontend/admin-vue3/src frontend/admin-vue3/public backend/sql
```

Expected: Logo consumers use `logo.png`, default-avatar consumers use `avatar.gif`, and administrator database records use `/agenticcps-icon.png`.

- [x] **Step 2: Run resource and diff validation**

Run:

```powershell
git diff --check
pnpm build:prod
```

Expected: `git diff --check` exits 0 and the production build resolves all replaced assets. If the repository's existing frontend errors block the build, record the exact unrelated failure and retain the successful asset-decoding checks.

- [x] **Step 3: Perform browser visual verification**

Restart the frontend if necessary, log in as the primary administrator, and verify the same icon appears in the left navigation Logo, the right user avatar, and the default-avatar fallback. Capture a screenshot for evidence.

- [x] **Step 4: Commit the implementation**

Stage only the icon assets and administrator-avatar seed changes. Use a Lore-protocol commit describing the small-size readability constraint, the rejected text/coin-heavy alternatives, and the verification evidence.

## Execution evidence

- Generated and inspected the approved 1024×1024 icon; PNG corners are transparent and the center is fully opaque.
- Verified PNG/GIF decoding for all four consumed assets.
- Verified all seven administrator seed rows and the current local MySQL administrator avatar use `/agenticcps-icon.png`.
- `git diff --check` completed without whitespace errors.
- `pnpm build:prod` generated a fresh `dist-prod` containing the new PNG and GIF assets, but the command did not exit before the 300-second tool timeout.
- Playwright logged in successfully, reported zero console errors, and visually confirmed the icon in the left Logo, right user avatar, homepage avatar, and notice avatars.
