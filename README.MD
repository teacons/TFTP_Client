# TFTP Client

## Build

### Windows

`gradlew.bat build`

### Linux

`./gradlew build`

## Start

`java -jar TFTP_client-1.0-SNAPSHOT.jar ip port`

## Protocol:

### Packet format

TFTP support five types of packets:

- **1**     Read request (RRQ)
- **2**     Write request (WRQ)
- **3**     Data (DATA)
- **4**     Acknowledgment (ACK)
- **5**     Error (ERROR)

#### RRQ/WRQ packet

| 2 bytes |  string  | 1 byte | string | 1 byte |
|:-------:|:--------:|:------:|:------:|:------:|
| Opcode  | Filename |   0    |  Mode  |   0    |

#### DATA packet

| 2 bytes | 2 bytes | n bytes |
|:-------:|:-------:|:-------:|
| Opcode  | Block # |  Data   |

#### ACK packet

| 2 bytes | 2 bytes |
|:-------:|:-------:|
| Opcode  | Block # |

#### ERROR packet

| 2 bytes |  2 bytes  | string | 1 byte |
|:-------:|:---------:|:------:|:------:|
| Opcode  | ErrorCode | ErrMsg |   0    |

### Upload

1. **Client** sends a "WRQ" to **Server** with source = Client's TID, destination = port.
2. **Server** sends a "ACK" (with block number = 0) to **Client** with source = Server's TID, destination = Client's
   TID.
3. **Client** sends a "DATA" to **Server**
4. **Server** sends a "ACK" to *Client*
5. Repeats until "DATA" is less than 512

### Download

1. **Client** sends a "RRQ" to **Server** with source = Client's TID, destination = port.
2. **Server** sends a "DATA" (with block number = 0) to **Client** with source = Server's TID, destination = Client's
   TID.
3. **Client** sends a "ACK" to **Server**
4. **Server** sends a "DATA" to *Client*
5. Repeats until "DATA" is less than 512

