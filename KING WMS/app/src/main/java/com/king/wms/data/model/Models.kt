package com.king.wms.data.model

import kotlinx.serialization.Serializable

// ─── Standard kingonesystem response envelope ────────────────────────────────
// Every backend route answers { success, data, error?, meta? }. The app unwraps
// `data` and surfaces `error.message`. Unknown keys (e.g. meta) are ignored by the
// Json config, so they don't need to be modelled here.
@Serializable
data class ApiResponse<T>(
    val success: Boolean = false,
    val data: T? = null,
    val error: ApiError? = null,
)

@Serializable
data class ApiError(
    val code: String? = null,
    val message: String? = null,
)

// ─── Auth ─────────────────────────────────────────────────────────────────────
@Serializable
data class LoginRequest(val username: String, val password: String)

// POST /auth/login → data: { accessToken, user }. The refreshToken is set as an
// httpOnly cookie (handled by the OkHttp cookie jar), never in the JSON body.
@Serializable
data class LoginData(val accessToken: String, val user: UserDto)

@Serializable
data class UserDto(
    val id: String,
    val username: String,
    val name: String = "",
    val department: String? = null,
    val permissions: List<PermissionDto> = emptyList(),
)

@Serializable
data class PermissionDto(
    val module: String = "",
    val action: String = "",
    val resource: String = "",
)

// POST /auth/refresh (sends the refresh cookie) → data: { accessToken }.
@Serializable
data class RefreshData(val accessToken: String)

// ─── Scanned item (dispatch & receiving share the same scan-item shape) ─────────
// GET /wms/{dispatch|receiving}/scan-item?code= → resolves a scanned item code or
// barcode to an item. Receiving additionally returns a defaultCost to pre-fill the
// unit-cost field.
@Serializable
data class ScannedItem(
    val id: String,
    val code: String,
    val name: String = "",
    val uom: String? = null,
    val inventoryUomName: String? = null,
    val defaultCost: Double? = null,
)

// ─── Goods Issue (Dispatch) ─────────────────────────────────────────────────────
// GET /wms/dispatch/m-source-bins?itemId= → bins holding stock of the item, FIFO
// (oldest stock first).
@Serializable
data class SourceBin(
    val binCode: String,
    val warehouseCode: String? = null,
    val qty: Double = 0.0,
    val lots: List<BinLot> = emptyList(),
)

@Serializable
data class BinLot(val lotNo: String = "", val qty: Double = 0.0)

// POST /wms/dispatch/mobile { itemId, fromBinCode, qty } → { giNumber }.
@Serializable
data class MobileIssueRequest(val itemId: String, val fromBinCode: String, val qty: Double)

@Serializable
data class MobileIssueResult(val giNumber: String)

// GET /wms/dispatch/mobile/recent → recent handheld goods issues (feed).
@Serializable
data class RecentIssue(
    val id: String,
    val giNumber: String? = null,
    val itemCode: String = "",
    val itemName: String = "",
    val uom: String? = null,
    val fromBin: String? = null,
    val qty: Double = 0.0,
    val createdByName: String? = null,
    val createdAt: String? = null,
)

// ─── Goods Receipt (Receiving) ──────────────────────────────────────────────────
// GET /wms/receiving/bins/lookup?warehouse=&search= → active bin locations.
@Serializable
data class BinLocationDto(
    val id: String? = null,
    val binCode: String,
    val warehouse: WarehouseRef? = null,
)

@Serializable
data class WarehouseRef(val code: String? = null, val name: String? = null)

// POST /wms/receiving/mobile { itemId, toBinCode, qty, unitCost? } → { grNumber }.
@Serializable
data class MobileReceiptRequest(
    val itemId: String,
    val toBinCode: String,
    val qty: Double,
    val unitCost: Double? = null,
)

@Serializable
data class MobileReceiptResult(val grNumber: String)

// GET /wms/receiving/mobile/recent → recent handheld goods receipts (feed).
@Serializable
data class RecentReceipt(
    val id: String,
    val grNumber: String? = null,
    val itemCode: String = "",
    val itemName: String = "",
    val uom: String? = null,
    val toBin: String? = null,
    val toWarehouse: String? = null,
    val qty: Double = 0.0,
    val createdByName: String? = null,
    val createdAt: String? = null,
)

// ─── Transfer Items (bin → bin) ─────────────────────────────────────────────────
// Reuses ScannedItem (scan-item), SourceBin (m-source-bins) and BinLocationDto
// (m-dest-bins). POST /wms/transfer-items/mobile → { transferNumber }.
@Serializable
data class MobileTransferRequest(
    val itemId: String,
    val fromBinCode: String,
    val toBinCode: String,
    val qty: Double,
)

@Serializable
data class TransferResult(val transferNumber: String)

// GET /wms/transfer-items/mobile/recent → recent handheld transfers (feed, max 20).
@Serializable
data class RecentTransfer(
    val id: String,
    val transferNumber: String? = null,
    val itemCode: String = "",
    val itemName: String = "",
    val uom: String? = null,
    val fromBin: String? = null,
    val toBin: String? = null,
    val qty: Double = 0.0,
    val createdByName: String? = null,
    val createdAt: String? = null,
)

// ─── Stock Movements (read-only ledger) ─────────────────────────────────────────
// GET /wms/stock-movements?search= → list of movements (newest first).
@Serializable
data class MovementRow(
    val id: String,
    val movementType: String = "",
    val qty: Double = 0.0,
    val lotNo: String? = null,
    val referenceDoc: String? = null,
    val createdAt: String? = null,
    val item: MovementItem? = null,
    val fromLocation: MovementLoc? = null,
    val toLocation: MovementLoc? = null,
)

@Serializable
data class MovementItem(val code: String = "", val name: String = "", val uom: String? = null)

@Serializable
data class MovementLoc(val code: String? = null, val warehouse: WarehouseRef? = null)

// ─── Inventory Counting & Posting (stock-count / stock-posting) ──────────────────
@Serializable
data class StockCountSummary(
    val id: String,
    val countNumber: String = "",
    val countDate: String? = null,
    val status: String = "",
    val lineCount: Int = 0,
)

@Serializable
data class StockCountDetail(
    val id: String,
    val countNumber: String = "",
    val countDate: String? = null,
    val status: String = "",
    val lines: List<CountLine> = emptyList(),
)

@Serializable
data class CountLine(
    val id: String,
    val itemCode: String = "",
    val itemName: String = "",
    val uom: String? = null,
    val locationCode: String = "",
    val lotNo: String? = null,
    val systemQty: Double = 0.0,
    val countedQty: Double? = null,
    val variance: Double? = null,
)

@Serializable
data class CountEntry(val lineId: String, val countedQty: Double?)

@Serializable
data class SaveCountsRequest(val lines: List<CountEntry>)

@Serializable
data class NewCountRequest(val countDate: String, val locationId: String? = null)

@Serializable
data class PostResult(val countNumber: String? = null, val adjustedLines: Int? = null)

// ─── Stock Check (item lookup → on-hand by warehouse) ───────────────────────────
@Serializable
data class ItemLookup(
    val id: String,
    val code: String = "",
    val name: String = "",
    val uom: String? = null,
)

@Serializable
data class WarehouseStock(
    val whseCode: String = "",
    val whseName: String = "",
    val inStock: Double = 0.0,
    val committed: Double = 0.0,
    val ordered: Double = 0.0,
)
