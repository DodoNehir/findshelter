package com.example.findshelter

data class AreaCodeResponse(
    val stanReginCd: StanReginCd
)

data class StanReginCd(
    val row: Row
)

data class Row(
    val region_cd: String,
    val locatadd_nm: String
)