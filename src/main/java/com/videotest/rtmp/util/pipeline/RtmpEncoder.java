package com.videotest.rtmp.util.pipeline;

import com.videotest.rtmp.chunk.message.*;
import com.videotest.rtmp.chunk.type.AMF0;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class RtmpEncoder extends MessageToByteEncoder<RtmpBaseMsg> {

	// The output chunk size, default to min, set by peer
	private int outChunkSize = 128;

	private boolean firstVideo = true;
	private boolean firstAudio = true;
	private boolean firstText = true;
	private long firstVideoTimestamp = System.nanoTime();
	private long firstAudioTimestamp = System.nanoTime();
	private long firstTextTimestamp = System.nanoTime();

	// todo : instaceof 말고 다른 방법 찾아보기
	@Override
	protected void encode(ChannelHandlerContext ctx, RtmpBaseMsg msg, ByteBuf out) throws Exception {
		if (msg instanceof RtmpChunkMsg) {
			RtmpChunkMsg rtmpChunkMsg = (RtmpChunkMsg) msg;
			out.writeByte(2); // fmt + csid

			// timeStamp, message length, type Id 등 헤더의 값
			out.writeMedium(0)
				.writeMedium(4)
				.writeByte(rtmpChunkMsg.getMessageTypeId())
				.writeInt(0)
				.writeInt(rtmpChunkMsg.getChunkSize());

			// body
			outChunkSize = rtmpChunkMsg.getChunkSize();
		} else if (msg instanceof RtmpWinAckMsg) {
			RtmpWinAckMsg rtmpWinAckMsg = (RtmpWinAckMsg) msg;
			out.writeByte(2); // fmt + csid

			// timeStamp, message length, type Id 등 헤더의 값
			out.writeMedium(0)
				.writeMedium(4)
				.writeByte(rtmpWinAckMsg.getMessageTypeId())
				.writeInt(0);

			// body
			out.writeInt(rtmpWinAckMsg.getAcknowledgementSize());
		} else if (msg instanceof RtmpPeerBandWidthMsg) {
			RtmpPeerBandWidthMsg peerBandWidthMsg = (RtmpPeerBandWidthMsg) msg;

			out.writeByte(2); // fmt + csid

			// timeStamp, message length, type Id 등 헤더의 값
			out.writeMedium(0)
					.writeMedium(5)
					.writeByte(peerBandWidthMsg.getMessageTypeId())
					.writeInt(0);

			// body
			out.writeInt(peerBandWidthMsg.getAckSize())
				.writeInt(peerBandWidthMsg.getLimitType());
		} else if (msg instanceof RtmpUserControlMsg) {
			RtmpUserControlMsg rtmpUserControlMsg = (RtmpUserControlMsg) msg;

			out.writeByte(2); // fmt + csid

			// timeStamp, message length, type Id 등 헤더의 값
			out.writeMedium(0)
					.writeMedium(6)
					.writeByte(rtmpUserControlMsg.getMessageTypeId())
					.writeInt(rtmpUserControlMsg.getMsgStreamID());

			// body
			out.writeInt(rtmpUserControlMsg.getEventType())
				.writeInt(rtmpUserControlMsg.getEventData());
		} else if (msg instanceof RtmpAMF0CmdMsg) {
			RtmpAMF0CmdMsg rtmpAMF0CmdMsg = (RtmpAMF0CmdMsg) msg;

			out.writeByte(2); // fmt + csid
			ByteBuf amfEncode = AMF0.encodeAll(rtmpAMF0CmdMsg.getObjectList());
			if (amfEncode != null) {
				out.writeInt(rtmpAMF0CmdMsg.getCurStreamId());
				out.writeMedium(0)
					.writeMedium(amfEncode.writerIndex())
					.writeByte(rtmpAMF0CmdMsg.getMessageTypeId())
					.writeInt(0);
				out.writeBytes(amfEncode);
			}
		} else if (msg instanceof RtmpAMF0DataMsg) {
			RtmpAMF0DataMsg rtmpAMF0DataMsg = (RtmpAMF0DataMsg) msg;

			ByteBuf amfEncode = AMF0.encodeAll(rtmpAMF0DataMsg.getDataList());
			if (amfEncode != null) {
				out.writeInt(rtmpAMF0DataMsg.getCurStreamId());
				out.writeMedium(0)
						.writeMedium(amfEncode.writerIndex())
						.writeByte(rtmpAMF0DataMsg.getMessageTypeId())
						.writeInt(0);
				out.writeBytes(amfEncode);
			}
		} else if (msg instanceof RtmpVideoMsg) {
			RtmpVideoMsg rtmpVideoMsg = (RtmpVideoMsg) msg;

			if (firstVideo) {
				firstVideoTimestamp = System.nanoTime();
				encodeWithFmt0And3(msg, out);
				firstVideo = false;
			} else {
				encodeWithFmt1And3(msg, out);
			}
		} else if (msg instanceof RtmpAudioMsg) {
			RtmpAudioMsg rtmpAudioMsg = (RtmpAudioMsg) msg;

			if (firstAudio) {
				firstAudioTimestamp = System.nanoTime();
				encodeWithFmt0And3(msg, out);
				firstAudio = false;
			} else {
				encodeWithFmt1And3(msg, out);
			}
		} else if (msg instanceof RtmpTextMsg) {
			RtmpTextMsg rtmpTextMsg = (RtmpTextMsg) msg;

			if (firstText) {
				firstTextTimestamp = System.nanoTime();
				encodeWithFmt0And3(msg, out);
				firstText = false;
			} else {
				encodeWithFmt1And3(msg, out);
			}
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

	private void encodeWithFmt0And3(final RtmpBaseMsg msg, final ByteBuf out) {
		int outCsid;
		int payloadLength;
		ByteBuf payload;
		long firstMediaTimestamp;

		if (msg instanceof RtmpAudioMsg) {
			outCsid = 10;
			payloadLength = 1 + ((RtmpAudioMsg) msg).getAudioBytes().length; // control + audio data
			payload = Unpooled.buffer(payloadLength, payloadLength); // message payload
			payload.writeByte(((RtmpAudioMsg) msg).getControl());
			payload.writeBytes(((RtmpAudioMsg) msg).getAudioBytes());
			firstMediaTimestamp = firstAudioTimestamp;
		} else if (msg instanceof RtmpVideoMsg) {
			outCsid = 12;
			payloadLength = 1 + ((RtmpVideoMsg) msg).getVideoBytes().length; // control + video data
			payload = Unpooled.buffer(payloadLength, payloadLength); // message payload
			payload.writeByte(((RtmpVideoMsg) msg).getControl());
			payload.writeBytes(((RtmpVideoMsg) msg).getVideoBytes());
			firstMediaTimestamp = firstVideoTimestamp;
		} else if (msg instanceof RtmpTextMsg) {
			outCsid = 14;
			payloadLength = ((RtmpTextMsg) msg).getTextBytes().length;
			payload = Unpooled.buffer(payloadLength, payloadLength);
			payload.writeBytes(((RtmpTextMsg) msg).getTextBytes());
			firstMediaTimestamp = firstTextTimestamp;
		} else {
			return; // do nothing
		}

		ByteBuf tmpOut = Unpooled.buffer();
		byte[] basicHeader = encodeBasicHeader(0, outCsid);
		tmpOut.writeBytes(basicHeader); // basic header
		boolean needExtendedTimestamp = false;
		long timestamp = System.nanoTime() - firstMediaTimestamp;
		if (timestamp >= 0xffffff) { // timestamp
			needExtendedTimestamp = true;
			tmpOut.writeMedium(0xffffff);
		} else {
			tmpOut.writeMedium((int) timestamp);
		}
		tmpOut.writeMedium(payloadLength); // message length
		tmpOut.writeByte(msg.getMessageTypeId()); // message type id
		tmpOut.writeIntLE(0); // stream id default to 0
		if (needExtendedTimestamp) { // extended timestamp if necessary
			tmpOut.writeInt((int) timestamp);
		}

		if (payloadLength <= outChunkSize) {
			tmpOut.writeBytes(payload);
			out.writeBytes(tmpOut);
			return;
		}

		tmpOut.writeBytes(payload, outChunkSize);
		while (payload.isReadable()) {
			int remainSize = payload.readableBytes();
			int chunkSize = Math.min(outChunkSize, remainSize);
			byte[] fm3BasicHeader = encodeBasicHeader(3, outCsid);
			tmpOut.writeBytes(fm3BasicHeader);
			tmpOut.writeBytes(payload, chunkSize);
		}
		out.writeBytes(tmpOut);
	}

	private void encodeWithFmt1And3(final RtmpBaseMsg msg, final ByteBuf out) {
		int outCsid;
		int payloadLength;
		ByteBuf payload;
		int timestampDelta;

		if (msg instanceof RtmpAudioMsg) {
			outCsid = 10;
			payloadLength = 1 + ((RtmpAudioMsg) msg).getAudioBytes().length; // control + audio data
			payload = Unpooled.buffer(payloadLength, payloadLength); // message payload
			payload.writeByte(((RtmpAudioMsg) msg).getControl());
			payload.writeBytes(((RtmpAudioMsg) msg).getAudioBytes());
			timestampDelta = ((RtmpAudioMsg) msg).getTimeStampDelta();
		} else if (msg instanceof RtmpVideoMsg) {
			outCsid = 12;
			payloadLength = 1 + ((RtmpVideoMsg) msg).getVideoBytes().length; // control + video data
			payload = Unpooled.buffer(payloadLength, payloadLength); // message payload
			payload.writeByte(((RtmpVideoMsg) msg).getControl());
			payload.writeBytes(((RtmpVideoMsg) msg).getVideoBytes());
			timestampDelta = ((RtmpVideoMsg) msg).getTimeStampDelta();
		} else if (msg instanceof RtmpTextMsg) {
			outCsid = 14;
			payloadLength = ((RtmpTextMsg) msg).getTextBytes().length;
			payload = Unpooled.buffer(payloadLength, payloadLength);
			payload.writeBytes(((RtmpTextMsg) msg).getTextBytes());
			timestampDelta = ((RtmpTextMsg) msg).getTimeStampDelta();
		} else {
			return;
		}

		ByteBuf tmpOut = Unpooled.buffer();
		byte[] basicHeader = encodeBasicHeader(1, outCsid);
		tmpOut.writeBytes(basicHeader); // basic header
		tmpOut.writeMedium(timestampDelta); // timestamp delta
		tmpOut.writeMedium(payloadLength); // message length
		tmpOut.writeByte(msg.getMessageTypeId()); // message type id

		if (payloadLength <= outChunkSize) {
			tmpOut.writeBytes(payload);
			out.writeBytes(tmpOut);
			return;
		}

		tmpOut.writeBytes(payload, outChunkSize);
		while (payload.isReadable()) {
			int remainSize = payload.readableBytes();
			int chunkSize = Math.min(outChunkSize, remainSize);
			byte[] fm3BasicHeader = encodeBasicHeader(3, outCsid);
			tmpOut.writeBytes(fm3BasicHeader);
			tmpOut.writeBytes(payload, chunkSize);
		}
		out.writeBytes(tmpOut);
	}
}

