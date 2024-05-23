package com.videotest.rtmp.chunk;

public enum ChunkFormat {

    /** 메신저 헤더 */
    FORMAT0((byte) 0x00),
    FORMAT1((byte) 0x01),
    FORMAT2((byte) 0x02),
    FORMAT3((byte) 0x03),

    /* ------------ RTMP 메시지 ------------ */
    /** Rtmp 설정 */
    SetChunkSize((byte) 0x01),
    AbortMsg((byte) 0x02),
    Acknowlegment((byte) 0x03),
    UserControlMsg((byte) 0x04),
    WindowAcknolegment((byte) 0x05),
    SetPeerBandWidth((byte) 0x06),

    /** 데이터  */
    AudioMsg((byte) 0x08),
    VideoMsg((byte) 0x09),
    TextMsg((byte) 0x0A),

    /** AMF */
    Amf0CmdMsg((byte) 0x11),
    Amf0DataMsg((byte) 0x12),
    Amf3CmdMsg((byte) 0x14),
    Amf3DataMsg((byte) 0x0F);
    /* ------------ RTMP 메시지 ------------ */

    final byte msgByte;

    ChunkFormat(byte msgByte) {
        this.msgByte = msgByte;
    }

    public static ChunkFormat selectFormatType(byte fmt) throws Exception {
        return switch (fmt) {
            case 0x00 -> ChunkFormat.FORMAT0;
            case 0x01 -> ChunkFormat.FORMAT1;
            case 0x02 -> ChunkFormat.FORMAT2;
            case 0x03 -> ChunkFormat.FORMAT3;
            default -> throw new Exception("Unknown format: " + fmt);
        };
    }

    public static ChunkFormat selectRtmpMsgType(byte fmt) throws Exception {
        return switch (fmt) {
            case 0x01 -> ChunkFormat.SetChunkSize;
            case 0x02 -> ChunkFormat.AbortMsg;
            case 0x03 -> ChunkFormat.Acknowlegment;
            case 0x04 -> ChunkFormat.UserControlMsg;
            case 0x05 -> ChunkFormat.WindowAcknolegment;
            case 0x06 -> ChunkFormat.SetPeerBandWidth;
            case 0x08 -> ChunkFormat.AudioMsg;
            case 0x09 -> ChunkFormat.VideoMsg;
            case 0x0A -> ChunkFormat.TextMsg;
            case 0x11 -> ChunkFormat.Amf3CmdMsg;
            case 0x14 -> ChunkFormat.Amf0CmdMsg;
            case 0x0F -> ChunkFormat.Amf3DataMsg;
            case 0x12 -> ChunkFormat.Amf0DataMsg;
            default -> throw new Exception("Unknown format: " + fmt);
        };
    }

}
