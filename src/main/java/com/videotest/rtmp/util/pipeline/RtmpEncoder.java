package com.videotest.rtmp.util.pipeline;

import com.videotest.rtmp.chunk.message.RtmpBaseMsg;
import com.videotest.rtmp.chunk.message.RtmpChunkMsg;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class RtmpEncoder extends MessageToByteEncoder<RtmpBaseMsg> {

	// The output chunk size, default to min, set by peer
	private int outChunkSize = 128;

	private boolean firstVideo = true;
	private boolean firstAudio = true;
	private boolean firstText = true;
	private long firstVideoTimestamp = System.currentTimeMillis();
	private long firstAudioTimestamp = System.currentTimeMillis();
	private long firstTextTimestamp = System.currentTimeMillis();

	@Override
	protected void encode(ChannelHandlerContext ctx, RtmpBaseMsg msg, ByteBuf out) throws Exception {
		if (msg instanceof RtmpChunkMsg) {
			RtmpChunkMsg rtmpChunkMsg = (RtmpChunkMsg) msg;
			out.writeByte(2); // todo : 테스트용값
			out.writeMedium(0)
				.writeMedium(4)
				.writeByte(rtmpChunkMsg.getMessageTypeId())
				.writeInt(0) // todo : 테스트용값
				.writeInt(rtmpChunkMsg.getChunkSize());
			outChunkSize = rtmpChunkMsg.getChunkSize();
		}
	}

	private static byte[] encodeBasicHeader(final int fmt, final int csid) {
		if (csid <= 63) {
			return new byte[] { (byte) ((fmt << 6) + csid) };
		} else if (csid <= 320) {
			return new byte[] { (byte) (fmt << 6), (byte) (csid - 64) };
		} else {
			return new byte[] { (byte) ((fmt << 6) | 1), (byte) ((csid - 64) & 0xff), (byte) ((csid - 64) >> 8) };
		}
	}
}

