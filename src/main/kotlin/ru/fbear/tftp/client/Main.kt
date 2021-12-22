package ru.fbear.tftp.client

import java.io.File
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlin.system.exitProcess


fun main(args: Array<String>) {
    if (args.size != 2) {
        println("Usage java -jar tftp_client.jar ip port")
        exitProcess(0)
    }
    if (args[1].toIntOrNull() == null) {
        println("Usage java -jar tftp_client.jar ip port")
        exitProcess(0)
    }

    Client(InetAddress.getByName(args[0]), args[1].toInt()).launch()
}

class Client(private val inetAddress: InetAddress, private val port: Int) {

    private var remotePort = port

    private val clientSocket = DatagramSocket()

    fun launch() {

        while (true) {
            print("Command: ")
            when (readLine()) {
                null -> exitProcess(0)
                "quit" -> exitProcess(0)
                "get" -> {
                    val file = getFile()
                    get(file)
                }
                "put" -> {
                    val file = getFile()
                    if (!file.exists()) println("File does not exist")
                    else put(file)
                }
                else -> println("Unknown command")
            }
        }
    }

    private fun getFile(): File {
        print("Filename: ")
        val filename = readLine() ?: exitProcess(0)
        return File(filename)
    }

    private fun put(file: File) {
        send(Packet(Opcode.WRQ, file.name))

        try {
            with(read()) {
                if (this.opcode == Opcode.ERROR) {
                    println(this.errorMessage)
                    return
                }
                if (this.opcode != Opcode.ACK) {
                    println("Server error: not received ACK")
                    return
                }
                if (this.block != 0) {
                    println("Server error: received wrong ACK block")
                    return
                }
            }
        } catch (e: IllegalArgumentException) {
            println(e.message)
            return
        }

        val fileBytes = file.readBytes()

        var size = fileBytes.size

        var lastSendByte = 0

        var packetNum = 1

        while (size != 0) {
            val byteToSend =
                if (size >= 512) fileBytes.slice(lastSendByte until lastSendByte + 512)
                else fileBytes.slice(lastSendByte until fileBytes.size)
            send(Packet(Opcode.DATA, packetNum, byteToSend))
            val packet = try {
                read()
            } catch (e: IllegalArgumentException) {
                println(e.message)
                return
            }
            if (packet.opcode == Opcode.ERROR) {
                println("Server: ${packet.errorMessage}")
                return
            }
            if (packet.opcode != Opcode.ACK) {
                println("Server error: not received ACK")
                return
            }
            if (packet.block == packetNum) {
                size -= byteToSend.size
                lastSendByte += byteToSend.size
                packetNum++
            } else throw IllegalStateException("Wrong ACK")
        }

        remotePort = port
    }

    @Throws(IllegalArgumentException::class)
    private fun send(packet: Packet) {
        val mode = "octet".toByteArray(Charsets.US_ASCII)
        val packetBytes = when (packet.opcode) {
            Opcode.RRQ -> {
                val opcodeBytes = ByteArray(1) + Opcode.RRQ.opcode.toByte()
                val filenameBytes = packet.filename!!.toByteArray(Charsets.US_ASCII)
                opcodeBytes + filenameBytes + ByteArray(1) + mode + ByteArray(1)
            }
            Opcode.WRQ -> {
                val opcodeBytes = ByteArray(1) + Opcode.WRQ.opcode.toByte()
                val filenameBytes = packet.filename!!.toByteArray(Charsets.US_ASCII)
                opcodeBytes + filenameBytes + ByteArray(1) + mode + ByteArray(1)
            }
            Opcode.DATA -> {
                val opcodeBytes = ByteArray(1) + Opcode.DATA.opcode.toByte()
                val blockBytes = ByteArray(2 - packet.block!!.toByteArray().size) + packet.block.toByteArray()
                opcodeBytes + blockBytes + packet.data!!
            }
            Opcode.ACK -> {
                val opcodeBytes = ByteArray(1) + Opcode.ACK.opcode.toByte()
                val blockBytes = ByteArray(2 - packet.block!!.toByteArray().size) + packet.block.toByteArray()
                opcodeBytes + blockBytes
            }
            else -> throw IllegalArgumentException("Wrong opcode")
        }

        clientSocket.send(DatagramPacket(packetBytes, packetBytes.size, inetAddress, remotePort))
    }

    @Throws(IllegalArgumentException::class)
    private fun read(): Packet {
        val receivingDataBuffer = ByteArray(516)
        val receivingPacket = DatagramPacket(receivingDataBuffer, receivingDataBuffer.size)
        clientSocket.receive(receivingPacket)
        val opcode = when (receivingDataBuffer.slice(0..1).toInt()) {
            3 -> Opcode.DATA
            4 -> Opcode.ACK
            5 -> Opcode.ERROR
            else -> throw IllegalArgumentException("Wrong opcode")
        }
        return when (opcode) {
            Opcode.DATA -> {
                val blockNum = receivingDataBuffer.slice(2..3).toInt()
                if (blockNum == 1) remotePort = receivingPacket.port
                val data = receivingDataBuffer.slice(4 until receivingPacket.length)
                Packet(opcode, blockNum, data)
            }
            Opcode.ACK -> {
                val blockNum = receivingDataBuffer.slice(2..3).toInt()
                if (blockNum == 0) remotePort = receivingPacket.port
                Packet(opcode, blockNum)
            }
            Opcode.ERROR -> {
                val errorCode = receivingDataBuffer.slice(2..3).toInt()
                val errorMessage = receivingDataBuffer.slice(4 until receivingPacket.length)
                    .dropLast(1)
                    .toByteArray()
                    .toString(Charsets.US_ASCII)
                Packet(opcode, errorCode, errorMessage)
            }
            else -> throw IllegalArgumentException("Wrong opcode")
        }
    }

    private fun get(file: File) {
        send(Packet(Opcode.RRQ, file.name))

        val fileBytes = mutableListOf<Byte>()

        while (true) {
            val packet = try {
                read()
            } catch (e: IllegalArgumentException) {
                println(e.message)
                return
            }
            if (packet.opcode == Opcode.ERROR) {
                println("Server: ${packet.errorMessage}")
                return
            }
            if (packet.opcode != Opcode.DATA) continue

            fileBytes.addAll(packet.data!!)

            send(Packet(Opcode.ACK, packet.block!!))

            if (packet.data.size < 512) break

        }

        if (!file.exists()) file.createNewFile()

        file.writeBytes(fileBytes.toByteArray())

        remotePort = port
    }
}

fun List<Byte>.toInt(): Int {
    var result = 0
    var shift = 0
    this.reversed().forEach {
        result += it.toUByte().toInt() shl shift
        shift += 8
    }
    return result
}

fun Int.toByteArray(): ByteArray {
    val bytes = mutableListOf<Byte>()

    var shift = 0

    var limit = this.countBits() / 8

    if (this.countBits() % 8 != 0) limit++

    for (i in 0 until limit) {
        bytes.add((this shr shift).toByte())
        shift += 8
    }

    return bytes.reversed().toByteArray()
}

fun Int.countBits(): Int {
    var n = this
    var count = 0
    while (n != 0) {
        count++
        n = n shr 1
    }
    return count
}