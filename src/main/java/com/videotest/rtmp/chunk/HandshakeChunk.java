package com.videotest.rtmp.chunk;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
@Setter
public class HandshakeChunk {
	private ByteBuf client;
	private ByteBuf server;
}
