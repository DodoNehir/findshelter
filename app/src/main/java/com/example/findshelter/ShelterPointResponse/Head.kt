package com.example.findshelter.ShelterPointResponse

data class Head(
    val RESULT: RESULT,
    val numOfRows: String,
    val pageNo: String,
    val totalCount: Int,
    val type: String
)