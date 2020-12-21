package dk.thrane.playground

enum class WebSocketOpCode(val opcode: Int) {
    CONTINUATION(0x0),
    TEXT(0x1),
    BINARY(0x2),
    CONNECTION_CLOSE(0x8),
    PING(0x9),
    PONG(0xA)
}
