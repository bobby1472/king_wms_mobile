package com.king.wms.data.api

import com.king.wms.data.model.*
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

// Maps 1:1 to the real kingonesystem Express API (base URL ends in /api/v1/).
// The server owns the PostgreSQL connection — the app never touches the DB directly.
interface WmsApi {

    // ── Auth ────────────────────────────────────────────────────────────────
    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): ApiResponse<LoginData>

    @POST("auth/refresh")
    suspend fun refresh(): ApiResponse<RefreshData>

    // ── Goods Issue (Dispatch) — WMS:dispatch permission ──────────────────────
    @GET("wms/dispatch/scan-item")
    suspend fun scanIssueItem(@Query("code") code: String): ApiResponse<ScannedItem>

    @GET("wms/dispatch/m-source-bins")
    suspend fun sourceBins(@Query("itemId") itemId: String): ApiResponse<List<SourceBin>>

    @POST("wms/dispatch/mobile")
    suspend fun mobileIssue(@Body body: MobileIssueRequest): ApiResponse<MobileIssueResult>

    @GET("wms/dispatch/mobile/recent")
    suspend fun recentIssues(): ApiResponse<List<RecentIssue>>

    // ── Goods Receipt (Receiving) — WMS:receiving permission ──────────────────
    @GET("wms/receiving/scan-item")
    suspend fun scanReceiveItem(@Query("code") code: String): ApiResponse<ScannedItem>

    @GET("wms/receiving/bins/lookup")
    suspend fun lookupBins(
        @Query("warehouse") warehouse: String?,
        @Query("search") search: String?,
    ): ApiResponse<List<BinLocationDto>>

    @POST("wms/receiving/mobile")
    suspend fun mobileReceive(@Body body: MobileReceiptRequest): ApiResponse<MobileReceiptResult>

    @GET("wms/receiving/mobile/recent")
    suspend fun recentReceipts(): ApiResponse<List<RecentReceipt>>

    // ── Transfer Items (bin → bin) — WMS:transfer-items-mobile permission ──────
    @GET("wms/transfer-items/scan-item")
    suspend fun scanTransferItem(@Query("code") code: String): ApiResponse<ScannedItem>

    @GET("wms/transfer-items/m-source-bins")
    suspend fun transferSourceBins(@Query("itemId") itemId: String): ApiResponse<List<SourceBin>>

    @GET("wms/transfer-items/m-dest-bins")
    suspend fun transferDestBins(@Query("search") search: String?): ApiResponse<List<BinLocationDto>>

    @POST("wms/transfer-items/mobile")
    suspend fun mobileTransfer(@Body body: MobileTransferRequest): ApiResponse<TransferResult>

    @GET("wms/transfer-items/mobile/recent")
    suspend fun recentTransfers(): ApiResponse<List<RecentTransfer>>

    // ── Stock Movements (read-only ledger) — WMS:READ:stock-movements ──────────
    @GET("wms/stock-movements")
    suspend fun movements(
        @Query("search") search: String?,
        @Query("limit") limit: Int = 50,
    ): ApiResponse<List<MovementRow>>

    // ── Inventory Counting (stock-count) — WMS:*:stock-count ───────────────────
    @GET("wms/stock-count")
    suspend fun stockCounts(): ApiResponse<List<StockCountSummary>>

    @GET("wms/stock-count/{id}")
    suspend fun stockCount(@Path("id") id: String): ApiResponse<StockCountDetail>

    @POST("wms/stock-count")
    suspend fun createStockCount(@Body body: NewCountRequest): ApiResponse<StockCountDetail>

    @PATCH("wms/stock-count/{id}/counts")
    suspend fun saveStockCounts(
        @Path("id") id: String,
        @Body body: SaveCountsRequest,
    ): ApiResponse<StockCountDetail>

    // ── Inventory Posting (stock-posting) — WMS:READ + WMS:APPROVE:stock-posting ─
    @GET("wms/stock-posting")
    suspend fun postingList(): ApiResponse<List<StockCountSummary>>

    @GET("wms/stock-posting/{id}")
    suspend fun postingDetail(@Path("id") id: String): ApiResponse<StockCountDetail>

    @POST("wms/stock-posting/{id}/post")
    suspend fun postStockCount(@Path("id") id: String): ApiResponse<PostResult>

    // ── Stock Check — item lookup (any authenticated user) + on-hand by warehouse ─
    @GET("wms/items/lookup")
    suspend fun itemsLookup(@Query("search") search: String?): ApiResponse<List<ItemLookup>>

    @GET("wms/items/{id}/warehouse-stock")
    suspend fun warehouseStock(@Path("id") id: String): ApiResponse<List<WarehouseStock>>
}
