package com.king.wms.data.repository

import com.king.wms.data.api.WmsApi
import com.king.wms.data.model.*
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

/** Carries the human-readable backend message (error.message) up to the UI. */
class ApiException(message: String) : Exception(message)

@Singleton
class WmsRepository @Inject constructor(
    private val api: WmsApi,
    private val tokenStore: TokenStore,
    private val cookieJar: InMemoryCookieJar,
    private val json: Json,
) {
    suspend fun login(username: String, password: String): Result<UserDto> = call {
        val data = api.login(LoginRequest(username, password)).unwrap()
        tokenStore.save(data.accessToken)
        data.user
    }

    suspend fun logout() {
        tokenStore.clear()
        cookieJar.clear()
    }

    // ── Goods Issue (Dispatch) ────────────────────────────────────────────────
    suspend fun scanIssueItem(code: String): Result<ScannedItem> =
        call { api.scanIssueItem(code).unwrap() }

    suspend fun sourceBins(itemId: String): Result<List<SourceBin>> =
        call { api.sourceBins(itemId).unwrap() }

    suspend fun mobileIssue(itemId: String, fromBinCode: String, qty: Double): Result<String> =
        call { api.mobileIssue(MobileIssueRequest(itemId, fromBinCode, qty)).unwrap().giNumber }

    suspend fun recentIssues(): Result<List<RecentIssue>> =
        call { api.recentIssues().unwrap() }

    // ── Goods Receipt (Receiving) ─────────────────────────────────────────────
    suspend fun scanReceiveItem(code: String): Result<ScannedItem> =
        call { api.scanReceiveItem(code).unwrap() }

    suspend fun lookupBins(warehouse: String?, search: String?): Result<List<BinLocationDto>> =
        call { api.lookupBins(warehouse, search).unwrap() }

    suspend fun mobileReceive(
        itemId: String,
        toBinCode: String,
        qty: Double,
        unitCost: Double?,
    ): Result<String> =
        call { api.mobileReceive(MobileReceiptRequest(itemId, toBinCode, qty, unitCost)).unwrap().grNumber }

    suspend fun recentReceipts(): Result<List<RecentReceipt>> =
        call { api.recentReceipts().unwrap() }

    // ── Transfer Items (bin → bin) ─────────────────────────────────────────────
    suspend fun scanTransferItem(code: String): Result<ScannedItem> =
        call { api.scanTransferItem(code).unwrap() }

    suspend fun transferSourceBins(itemId: String): Result<List<SourceBin>> =
        call { api.transferSourceBins(itemId).unwrap() }

    suspend fun transferDestBins(search: String?): Result<List<BinLocationDto>> =
        call { api.transferDestBins(search).unwrap() }

    suspend fun mobileTransfer(
        itemId: String,
        fromBinCode: String,
        toBinCode: String,
        qty: Double,
    ): Result<String> = call {
        api.mobileTransfer(MobileTransferRequest(itemId, fromBinCode, toBinCode, qty)).unwrap().transferNumber
    }

    suspend fun recentTransfers(): Result<List<RecentTransfer>> =
        call { api.recentTransfers().unwrap() }

    // ── Stock Movements (read-only ledger) ─────────────────────────────────────
    suspend fun movements(search: String?): Result<List<MovementRow>> =
        call { api.movements(search, 50).unwrap() }

    // ── Inventory Counting (stock-count) ───────────────────────────────────────
    suspend fun stockCounts(): Result<List<StockCountSummary>> =
        call { api.stockCounts().unwrap() }

    suspend fun stockCount(id: String): Result<StockCountDetail> =
        call { api.stockCount(id).unwrap() }

    suspend fun createStockCount(countDate: String): Result<StockCountDetail> =
        call { api.createStockCount(NewCountRequest(countDate)).unwrap() }

    suspend fun saveStockCounts(id: String, entries: List<CountEntry>): Result<StockCountDetail> =
        call { api.saveStockCounts(id, SaveCountsRequest(entries)).unwrap() }

    // ── Inventory Posting (stock-posting) ──────────────────────────────────────
    suspend fun postingList(): Result<List<StockCountSummary>> =
        call { api.postingList().unwrap() }

    suspend fun postingDetail(id: String): Result<StockCountDetail> =
        call { api.postingDetail(id).unwrap() }

    suspend fun postStockCount(id: String): Result<PostResult> =
        call { api.postStockCount(id).unwrap() }

    // ── Stock Check ────────────────────────────────────────────────────────────
    suspend fun itemsLookup(search: String?): Result<List<ItemLookup>> =
        call { api.itemsLookup(search).unwrap() }

    suspend fun warehouseStock(itemId: String): Result<List<WarehouseStock>> =
        call { api.warehouseStock(itemId).unwrap() }

    // ── helpers ────────────────────────────────────────────────────────────────
    private fun <T> ApiResponse<T>.unwrap(): T {
        if (success && data != null) return data
        throw ApiException(error?.message ?: "Request failed")
    }

    /** Run an API call, mapping backend errors (envelope or HTTP) to ApiException. */
    private suspend fun <T> call(block: suspend () -> T): Result<T> = runCatching {
        try {
            block()
        } catch (e: HttpException) {
            throw ApiException(e.parseMessage())
        }
    }

    /** Pull error.message out of a non-2xx { success:false, error } body. */
    private fun HttpException.parseMessage(): String {
        val raw = response()?.errorBody()?.string()
        if (!raw.isNullOrBlank()) {
            runCatching {
                json.decodeFromString(ApiResponse.serializer(ApiError.serializer()), raw)
            }.getOrNull()?.error?.message?.let { return it }
        }
        return when (code()) {
            401 -> "Session expired — please sign in again"
            403 -> "You don't have permission for this action"
            404 -> "Not found"
            else -> "Server error (${code()})"
        }
    }
}
