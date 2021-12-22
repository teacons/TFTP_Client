package ru.fbear.tftp.client

enum class Opcode(val opcode: Int) {
    RRQ(1),     // Read request
    WRQ(2),     // Write request
    DATA(3),    // Data
    ACK(4),     // Acknowledgment
    ERROR(5)    // Error
}

data class Packet(
    val opcode: Opcode,
    val filename: String?,
    val block: Int?,
    val data: List<Byte>?,
    val errorCode: Int?,
    val errorMessage: String?,
) {
    constructor(opcode: Opcode, filename: String) : this(opcode, filename, null, null, null, null)
    constructor(opcode: Opcode, block: Int, data: List<Byte>) : this(opcode, null, block, data, null, null)
    constructor(opcode: Opcode, block: Int) : this(opcode, null, block, null, null, null)
    constructor(opcode: Opcode, errorCode: Int, errorMessage: String) :
            this(opcode, null, null, null, errorCode, errorMessage)
}