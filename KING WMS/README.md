# KING WMS — Android Warehouse Management (Kotlin)

A kiosk-mode Android WMS app for warehouse handhelds. It talks to the existing
**kingonesystem** Express backend (which owns the **PostgreSQL** connection). The
app never connects to the database directly.

```
[Android handheld on warehouse WiFi]  →  HTTP/REST  →  [kingonesystem API @ LAN IP :4000]  →  PostgreSQL
```

It drives the backend's scan-driven mobile WMS flows:

**Operations**

- **Goods Issue (Dispatch)** — scan item → source bin (FIFO) → qty → `POST …/dispatch/mobile` (`GI-…`).
- **Goods Receipt (Receiving)** — scan item → destination bin → qty (+ optional cost) → `POST …/receiving/mobile` (`GR-…`).
- **Transfer Items** — scan item → source bin → destination bin + qty → `POST …/transfer-items/mobile` (`TR-…`).

**Inventory**

- **Stock Check** — scan/type an item → on-hand by warehouse (item lookup + warehouse-stock).
- **Stock Movements** — read-only movement ledger, searchable by item / reference.
- **Inventory Counting** — create/open a count, key in counted quantities, save (variance computed server-side).
- **Inventory Posting** — review entered counts and post the variance adjustments to stock + GL.

> The original draft of this app targeted an invented `pick_tasks`/`products` schema and a
> bundled `backend-api/wms-api.js`. Neither exists in kingonesystem, so both were removed —
> the app now calls the **real** API contract below.

---

## 1. Backend — nothing to add

The endpoints already exist in `apps/backend` (modules `wms/dispatch`, `wms/receiving`,
`wms/transfer`, `wms/stock-movements`, `wms/stock-count`, `wms/stock-posting`). You only need
the server reachable from the device's WiFi:

- Run the backend so it binds on the LAN (default port **4000**). The device must be able to
  reach `http://<server-LAN-IP>:4000/api/v1/`.
- The warehouse user signing in needs the matching **WMS permissions** (else that action
  returns **403**):
  - `dispatch`, `receiving` — `READ` + `CREATE`
  - `transfer-items-mobile` — `READ` + `CREATE`
  - `stock-movements` — `READ`
  - `stock-count` — `READ` + `CREATE` + `UPDATE`
  - `stock-posting` — `READ` + `APPROVE`
  - `items` — `READ` (Stock Check; the lookup itself is open to any signed-in user)

The drawer/Home **hide features the user has no permission for** (derived from the
permissions returned at login; a wildcard or empty set shows everything).

### API contract the app uses (`/api/v1`)

| Method & path                                 | Body / query                            | Returns (`data`)                                                     |
| --------------------------------------------- | --------------------------------------- | -------------------------------------------------------------------- |
| `POST auth/login`                             | `{username, password}`                  | `{accessToken, user}` (refresh token set as httpOnly cookie)         |
| `POST auth/refresh`                           | — (uses cookie)                         | `{accessToken}`                                                      |
| `GET  wms/dispatch/scan-item`                 | `?code=`                                | `{id, code, name, uom}`                                              |
| `GET  wms/dispatch/m-source-bins`             | `?itemId=`                              | `[{binCode, warehouseCode, qty, lots[]}]` (FIFO)                     |
| `POST wms/dispatch/mobile`                    | `{itemId, fromBinCode, qty}`            | `{giNumber}`                                                         |
| `GET  wms/receiving/scan-item`                | `?code=`                                | `{id, code, name, uom, defaultCost}`                                 |
| `GET  wms/receiving/bins/lookup`              | `?warehouse=&search=`                   | `[{binCode, warehouse{code,name}}]`                                  |
| `POST wms/receiving/mobile`                   | `{itemId, toBinCode, qty, unitCost?}`   | `{grNumber}`                                                         |
| `GET  wms/transfer-items/scan-item`           | `?code=`                                | `{id, code, name, uom}`                                              |
| `GET  wms/transfer-items/m-source-bins`       | `?itemId=`                              | `[{binCode, warehouseCode, qty, lots[]}]`                            |
| `GET  wms/transfer-items/m-dest-bins`         | `?search=`                              | `[{binCode, warehouse{code,name}}]`                                  |
| `POST wms/transfer-items/mobile`              | `{itemId, fromBinCode, toBinCode, qty}` | `{transferNumber}`                                                   |
| `GET  wms/transfer-items/mobile/recent`       | —                                       | recent transfers (max 20) for the scan-screen feed                   |
| `GET  wms/{dispatch,receiving}/mobile/recent` | —                                       | recent issues / receipts (max 20) for the scan-screen feeds          |
| `GET  wms/stock-movements`                    | `?search=&limit=`                       | `[{movementType, item{...}, fromLocation, toLocation, qty, …}]`      |
| `GET  wms/stock-count`                        | —                                       | `[{id, countNumber, countDate, status, lineCount}]`                  |
| `GET  wms/stock-count/{id}`                   | —                                       | `{id, countNumber, status, lines[{…systemQty,countedQty,variance}]}` |
| `POST wms/stock-count`                        | `{countDate}`                           | new count (snapshots all stock into lines)                           |
| `PATCH wms/stock-count/{id}/counts`           | `{lines:[{lineId, countedQty}]}`        | updated count (with variances)                                       |
| `GET  wms/stock-posting`                      | —                                       | counts in `IN_PROGRESS`/`COMPLETED`/`APPROVED`                       |
| `POST wms/stock-posting/{id}/post`            | —                                       | `{countNumber, adjustedLines}`                                       |

All responses use the standard envelope `{ success, data, error{code,message} }`. The app
unwraps `data` and surfaces `error.message`. On a `401` it transparently calls `auth/refresh`
(the access token lives ~15 min) and retries once; if that fails the user is returned to login.

## 2. Point the app at your server

In [`app/build.gradle.kts`](app/build.gradle.kts) set your backend's LAN IP (keep the
`/api/v1/` suffix **and** the trailing slash):

```kotlin
buildConfigField("String", "API_BASE_URL", "\"http://192.168.1.150:4000/api/v1/\"")
```

This baked-in value is just the **default**. You can also re-point the app **at runtime** with
no rebuild: open the hamburger menu → **Server settings**, enter `host:port` (e.g.
`192.168.1.150:4000`) and Save. It's stored on-device and rewrites every request's host (login
refresh included). Cleartext HTTP is permitted to any host (internal LAN tool) via
[`network_security_config.xml`](app/src/main/res/xml/network_security_config.xml) — use HTTPS in
production where you can.

## 3. Build & install

**Android Studio:** copy `local.properties.example` → `local.properties`, set `sdk.dir`, open
the folder, let Gradle sync, then Run.

**Command line (debug APK, sideloadable):**

```powershell
cd "D:\Claude\KING_WMS\KING WMS"
.\gradlew.bat assembleDebug
# → app\build\outputs\apk\debug\app-debug.apk
```

Copy the APK to the device and install (Settings → _Install unknown apps_ for the file
manager/browser). Toolchain: AGP 8.11.1 · Gradle 8.13 · compileSdk 36 · minSdk 26 (Android 8+).

---

## 4. Kiosk mode (lock the device to ONLY this app)

### Level 1 — App pinning (testing)

`startLockTask()` is called after login. The user can still exit by holding Back + Recents.

### Level 2 — True kiosk via Device Owner (production)

Provision a **factory-reset device with no Google account** as Device Owner, then from a PC:

```
adb shell dpm set-device-owner com.king.wms/.kiosk.AdminReceiver
```

Once Device Owner, `KioskManager.applyKioskPolicies()` whitelists only KING WMS, becomes the
home launcher (relaunches on boot), disables status bar/keyguard, and blocks safe boot, factory
reset, adding users, and unknown-source installs. To exit for maintenance, gate the **EXIT**
button behind a supervisor PIN, or remove Device Owner:

```
adb shell dpm remove-active-admin com.king.wms/.kiosk.AdminReceiver
```

For fleets, use an EMM/MDM (Android Enterprise, Scalefusion, Esper) with QR provisioning.

---

## Project layout

```
app/src/main/java/com/king/wms/
  MainActivity.kt                  nav (login → home → 6 feature screens) + kiosk lifecycle
  KingWmsApp.kt                    Hilt application
  data/api/WmsApi.kt               Retrofit endpoints (/api/v1)
  data/model/Models.kt             DTOs + { success, data } envelope
  data/repository/                 repository (envelope unwrap), token store,
                                   in-memory cookie jar, 401 token-refresh authenticator
  data/repository/SettingsStore.kt runtime server address + host-rewrite interceptor
  di/NetworkModule.kt              Retrofit/OkHttp/Hilt wiring
  kiosk/                           AdminReceiver, KioskManager, BootReceiver
  ui/theme/Theme.kt                luxe dark midnight + gold Material 3 theme (serif wordmark)
  ui/components/BarcodeScanner.kt  CameraX + ML Kit
  ui/screens/
    LoginScreen · HomeScreen (dashboard + recent activity) · LuxeDrawer (hamburger menu)
    ScanCommon (shared luxe widgets: ScanPanel, LuxeField, GoldButton, SuccessScreen, LuxeTopBar)
    DispatchScreen · ReceivingScreen · TransferScreen          (scan flows + ViewModels)
    StockCheckScreen · StockMovementsScreen                    (lookup / read-only ledger)
    InventoryCountScreen · InventoryPostingScreen              (list → detail + actions)
    SettingsScreen                                             (runtime server address)
```

Navigation is a **hamburger drawer** (☰, top-left of every screen): switch features from
anywhere; the menu only shows what the signed-in user is permitted to use. Home is a dashboard
with **recent activity** (last goods issues/receipts). Mid-flow, the ☰ becomes a ← to step back.

## Tech stack

Jetpack Compose · Material 3 · Hilt · Retrofit + kotlinx.serialization · OkHttp (cookie jar +
auth refresh) · CameraX + ML Kit barcode scanning · DataStore · DevicePolicyManager (kiosk).

## Design — luxe dark + gold

A premium, dark-first look for the KING brand: deep midnight-navy surfaces, champagne-gold
accents, thin gold hairline borders, a serif `KING` wordmark, tracked-out small-caps labels,
gold scanner corner-brackets, and a gold-ring confirmation screen. Glove-friendly big touch
targets; hardware scanner guns that emit keystrokes work via the manual code field.

## Next steps to consider

- Bundle a luxury serif (e.g. Playfair Display) for the wordmark/headings.
- Offline queue (Room) so issues/receipts survive WiFi dropouts and sync later.
- Haptic + sound feedback on scan success/failure; remember last-used bin/warehouse.
- Thai / English language toggle (the rest of King One uses Thai labels).
- Supervisor PIN on the EXIT button; signed release APK + kiosk (Device Owner) provisioning.
