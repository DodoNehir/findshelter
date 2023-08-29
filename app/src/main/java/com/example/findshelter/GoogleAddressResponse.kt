package com.example.findshelter

data class GoogleAddressResponse(
    val plus_code: PlusCode,
    val results: List<Result>,
    val status: String
)

data class PlusCode(
    val compound_code: String,
    val global_code: String
)

data class Result(
    val address_components: List<AddressComponent>,
    val formatted_address: String
)

data class AddressComponent(
    val long_name: String,
    val short_name: String,
    val types: List<String>
)