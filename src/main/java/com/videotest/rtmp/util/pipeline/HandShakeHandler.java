package com.videotest.rtmp.util.pipeline;

import com.videotest.rtmp.chunk.HandshakeChunk;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class HandShakeHandler extends ByteToMessageDecoder {

	private boolean isReadingClient = false;
	private boolean handshakeDone = false;

	private HandshakeChunk handshakeChunk;

	@Override
	protected void decode(ChannelHandlerContext channelHandler, ByteBuf input, List<Object> output) {
		if (handshakeDone) {
			channelHandler.fireChannelRead(input);
			return;
		}

		if (handshakeChunk == null) {
			handshakeChunk = new HandshakeChunk();
		}

		// Handshake Sequence
		if (!isReadingClient) {
			this.readClientChunk(input);
			this.writeSeverChunk(channelHandler);
			isReadingClient = true;
		} else {
			this.readChunk(input);
			handshakeDone = true;
			channelHandler.channel().pipeline().remove(this); // 핸드쉐이크 종료시 연결도 종료
			log.info("handshake success from channel: " + channelHandler.channel().id());
		}
	}

	/**
	 * Handshake Sequence, c0 과 c1을 처리한다.
	 * @param input 입력받은 데이터
	 */
	private void readClientChunk(ByteBuf input) {
		if (this.handshakeChunk == null) {
			log.error("handshakeChunk is null");
			return;
		}

		try {
			handshakeChunk.setClient(Unpooled.buffer(1537));
			input.readBytes(handshakeChunk.getClient());
		} catch (Exception e) {
			log.error("reading data is failed");
		}
	}

	/**
	 * Handshake Sequence, c2를 처리한다.
	 * @param input 입력받은 데이터
	 */
	private void readChunk(ByteBuf input) {
		if (handshakeChunk.getClient() != null) {
			return;
		}
		handshakeChunk.setClient(Unpooled.buffer(1536));
		input.readBytes(handshakeChunk.getClient());
	}

	/**
	 * Handshake Sequence, s0, s1, s2 의 입력을 처리한다.
	 * @param channelHandler 핸들러
	 */
	private void writeSeverChunk(ChannelHandlerContext channelHandler) {
		if (handshakeChunk.getServer() == null) {
			handshakeChunk.setServer(Unpooled.buffer(3073));
		} else {
			handshakeChunk.getServer().clear();
		}
	}

}
