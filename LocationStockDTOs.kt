package com.veoneer.logisticinventoryapp.core.domain.model

data class LocationContentRequest(
    val reference: String,
    val location: String,
    val startingNumber: String? = null
)

data class LocationContentResponse(
    val nextNumber: String?,
    val boxGroups: List<BoxGroup>
)

data class BoxGroup(
    val boxCustomerNumber: String,
    val totalQuantity: Int,
    val lotGroups: List<LotGroup>
)

data class LotGroup(
    val lotNumber: String,
    val totalQuantity: Int,
    val reels: List<ReelInfo>
)

data class ReelInfo(
    val reelGaliaNumber: String,
    val quantity: Int,
    val serialNumberCustomerName: String
)
