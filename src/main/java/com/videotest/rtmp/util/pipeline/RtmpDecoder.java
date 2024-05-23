package com.videotest.rtmp.util.pipeline;

import com.videotest.rtmp.chunk.message.*;
import com.videotest.rtmp.chunk.header.MessageHeader;
import com.videotest.rtmp.chunk.type.AMF0;
import com.videotest.rtmp.util.DecodeState;
import com.videotest.rtmp.chunk.ChunkFormat;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class RtmpDecoder extends ReplayingDecoder<DecodeState> {

	private int chunkSize = 128;
	private byte curFmt = -1; // format
	private int curStreamId = -1; // stream id
	
	// todo : redis 로 대체 가능 여부 확인
	private final Map<Integer, RtmpData> streamIdMap = new HashMap<>();

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		DecodeState state = super.state();
		
		if (state == null || DecodeState.READY_TO_DECODE_HEADER.equals(state)) {
			this.readHeader(in);
			this.readHeaderMsg(in);
			super.checkpoint(DecodeState.READY_TO_DECODE_PAYLOAD);
		} else if (DecodeState.READY_TO_DECODE_PAYLOAD.equals(state)) {
			RtmpData rtmpData = streamIdMap.get(curStreamId);
			if (rtmpData == null || rtmpData.getMessageHeader() == null) {
				throw new RuntimeException("inner error: rtmpData or rtmpData message header should not be null");
			}
			int payloadLength = rtmpData.getMessageHeader().getPayloadlength();
			if (1 > payloadLength) {
				log.info("ignore a message with no payload");
				super.checkpoint(DecodeState.READY_TO_DECODE_HEADER);
				rtmpData.setPayload(null);
				return;
			}
			ByteBuf payload = rtmpData.getPayload();
			if (payload == null) {
				payload = Unpooled.buffer(payloadLength, payloadLength);
				rtmpData.setPayload(payload);
			}

			int toReadSize = Math.min(payload.writableBytes(), chunkSize);
			final byte[] bytes = new byte[toReadSize];
			in.readBytes(bytes);
			payload.writeBytes(bytes);
			super.checkpoint(DecodeState.READY_TO_DECODE_HEADER);

			if (payload.isWritable()) {
				log.debug("payload not complete yet");
				return;
			}

			rtmpData.setPayload(null);
			RtmpBaseMsg rtmpBaseMsg = this.onRecvCompleteMessage(ctx, rtmpData.getMessageHeader(), payload);
			if (rtmpBaseMsg == null) {
				log.warn("ignore an uninterested message");
				return;
			}
			out.add(rtmpBaseMsg);
		}
	}

	private RtmpBaseMsg onRecvCompleteMessage(ChannelHandlerContext ctx, MessageHeader header, ByteBuf payload) throws Exception {
		RtmpBaseMsg rtmpBaseMsg = null;
		ChunkFormat msgFormat = ChunkFormat.selectRtmpMsgType(header.getTypeId());
		switch (msgFormat) {
			case SetChunkSize:
				int chunkSize = payload.readInt();
				if (chunkSize > 65536) {
					log.warn("accept large chunk size=" + chunkSize);
				}

				if (chunkSize < 128) {
					throw new Exception("청크 사이즈가 너무 작습니다.");
				}

				this.chunkSize = chunkSize;
				rtmpBaseMsg = new RtmpChunkMsg(chunkSize);
				break;
			case AbortMsg:
				rtmpBaseMsg = new RtmpAbortMsg(payload.readInt());
				break;
			case UserControlMsg:
				short eventType = payload.readShort();
				int eventData = payload.readInt();
				rtmpBaseMsg = new RtmpUserControlMsg(eventType, eventData);
				break;
			case Acknowlegment:
				rtmpBaseMsg = new RtmpAckMsg(payload.readInt());
				break;
			case WindowAcknolegment:
				rtmpBaseMsg = new RtmpWinAckMsg(payload.readInt());
				break;
			case SetPeerBandWidth:
				rtmpBaseMsg = new RtmpPeerBandWidthMsg(payload.readInt(), payload.readByte());
				break;
			case AudioMsg:
				int audioControl = payload.readUnsignedByte();
				if (audioControl == 175) {
					log.info("ingnore auido meesage : {}", audioControl);
					break;
				}
				byte[] audioBytes = new byte[payload.readableBytes()];
				payload.readBytes(audioBytes);
				rtmpBaseMsg = new RtmpAudioMsg(header.getTimeStamp(), header.getTimeStampDelta(), audioControl, audioBytes);
				// todo : 오디오 데이터를 받을 경우
				break;
			case VideoMsg:
				int control = payload.readUnsignedByte();
				if (control != 23 && control != 39) {
					log.info("ingnore video meesage : {}", control);
					break;
				}
				byte[] videoBytes = new byte[payload.readableBytes()];
				payload.readBytes(videoBytes);
				rtmpBaseMsg = new RtmpAudioMsg(header.getTimeStamp(), header.getTimeStampDelta(), control, videoBytes);
				// todo : 비디오 데이터를 받을 경우
				break;
			case TextMsg:
				byte[] textBytes = new byte[payload.readableBytes()];
				payload.readBytes(textBytes);
				rtmpBaseMsg = new RtmpTextMsg(header.getTimeStamp(), header.getTimeStampDelta(), textBytes);
				// todo : 비디오 데이터를 받을 경우
				break;
			case Amf0CmdMsg:
				List<Object> decodedList = AMF0.decodeAll(payload);
				rtmpBaseMsg = new RtmpAMF0CmdMsg(curStreamId, decodedList);
				// todo : AMF0 Command를 받았을 때
				break;
			case Amf0DataMsg:
				List<Object> dataList = AMF0.decodeAll(payload);
				rtmpBaseMsg = new RtmpAMF0DataMsg(curStreamId, dataList);
				// todo : AMF0 data를 받았을 때
				break;
			case Amf3CmdMsg:
				// todo : 필요여부 확인필요
				break;
			case Amf3DataMsg:
				// todo : 필요여부 확인필요
				break;
			default:
				log.warn("알수없는 타입 : {}", msgFormat);
				break;
		}
		return rtmpBaseMsg;
	}

	/** RtmpData Basic Header */
	private void readChunkBasicHeader(ByteBuf in) {
		byte firstByte = in.readByte();
		byte fmt = (byte) ((firstByte & 0xff) >> 6);
		int csid = firstByte & 0x3f;
		if (csid == 0) {
			// Basic Header: 2-byte form
			csid = 64 + in.readByte() & 0xff;
		} else if (csid == 1) {
			// Basic Header: 3-byte form
			csid = 64;
			csid += in.readByte() & 0xff;
			csid += (in.readByte() & 0xff) * 256;
		}

		curFmt = fmt;
		curStreamId = csid;
	}

	/**
	 * header의 데이터를 읽는다.
	 * @param in 전달받은 데이터
	 */
	private void readHeader(ByteBuf in) {
		byte headerByte = in.readByte();
		byte fmt = (byte) ((headerByte & 0xff) >> 6);
		int csid = headerByte & 0x3f;
		if (csid == 0) {
			csid = 64 + headerByte & 0xff;
		} else {
			csid = 64;
			csid += headerByte & 0xff;
			csid += (headerByte & 0xff) * 256;
		}
		curFmt = fmt;
		curStreamId = csid;
	}

	/**
	 * header의 메시지를 읽는다.
	 * @param in
	 * @throws Exception
	 */
	private void readHeaderMsg(ByteBuf in) throws Exception {
		byte fmt = curFmt;
		int csid = curStreamId;


		RtmpData rtmpData = streamIdMap.get(csid); // todo : Redis로 대체
		boolean isFirstMsg;
		if (rtmpData == null) {
			rtmpData = new RtmpData();
			streamIdMap.put(csid, rtmpData);
			isFirstMsg = true;
		} else {
			isFirstMsg = false;
		}
		rtmpData.setFormat(fmt);
		rtmpData.setCurStreamId(csid);

		int timestampDelta;
		int payloadLength;
		byte msgTypeId;
		MessageHeader msgHeader;
		ChunkFormat msgFormat = ChunkFormat.selectFormatType(fmt);
		switch (msgFormat) {
			case FORMAT0 :
				if (!isFirstMsg) {
					throw new Exception("for existed rtmpData, fmt should not be 0");
				}
				timestampDelta = in.readMedium();
				payloadLength = in.readMedium();
				msgTypeId = (byte) (in.readByte() & 0xff);
				int msgStreamId = in.readIntLE();
				msgHeader = rtmpData.getMessageHeader();
				if (msgHeader == null) {
					msgHeader = new MessageHeader();
				}
				msgHeader.setPayloadlength(payloadLength);
				msgHeader.setMsgStreamId(msgTypeId);
				msgHeader.setMsgStreamId(msgStreamId);
				msgHeader.setTimeStampDelta(timestampDelta);
				if (timestampDelta >= 0xffffff) {
					msgHeader.setTimeStamp(in.readInt());
				} else {
					msgHeader.setTimeStamp(timestampDelta);
				}

				rtmpData.setMessageHeader(msgHeader);
				rtmpData.setPayload(Unpooled.buffer(payloadLength, payloadLength));
				break;
			case FORMAT1 :
				if (rtmpData.getMsgCount() == 0) {
					log.info("fresh rtmpData starts with fmt=1");
				}
				timestampDelta = in.readMedium();
				payloadLength = in.readMedium();
				msgTypeId = (byte) (in.readByte() & 0xff);
				msgHeader = rtmpData.getMessageHeader();
				if (msgHeader == null) {
					msgHeader = new MessageHeader();
				}
				if (isFirstMsg) {
					msgHeader.setPayloadlength(payloadLength);
				} else if (msgHeader.getPayloadlength() != payloadLength) {
					throw new Exception("msg in rtmpData cache, size="+ msgHeader.getPayloadlength() + " cannot change to " + payloadLength);
				}
				msgHeader.setTypeId(msgTypeId);
				msgHeader.setMsgStreamId(msgHeader.getMsgStreamId());
				msgHeader.setTimeStampDelta(timestampDelta);
				if (timestampDelta >= 0xffffff) {
					int extendedTimestamp = in.readInt();
					if (isFirstMsg) {
						msgHeader.setTimeStamp(extendedTimestamp);
					} else {
						msgHeader.setTimeStamp(msgHeader.getTimeStamp() + extendedTimestamp);
					}
				} else {
					msgHeader.setTimeStamp(msgHeader.getTimeStamp() + timestampDelta);
				}

				rtmpData.setMessageHeader(msgHeader);
				if (isFirstMsg) {
					rtmpData.setPayload(Unpooled.buffer(payloadLength, payloadLength));
				}
				break;
			case FORMAT2 :
				if (rtmpData.getMsgCount() == 0) {
					throw new Exception("fresh rtmpData expect fmt=0, actual=" + fmt + ", csid=" + csid);
				}
				timestampDelta = in.readMedium();
				msgHeader = rtmpData.getMessageHeader();
				if (msgHeader == null) {
					msgHeader = new MessageHeader();
				}
				msgHeader.setTimeStampDelta(timestampDelta);
				if (timestampDelta >= 0xffffff) {
					msgHeader.setTimeStamp(msgHeader.getTimeStamp() + in.readInt());
				} else {
					msgHeader.setTimeStamp(msgHeader.getTimeStamp() + timestampDelta);
				}
				rtmpData.setMessageHeader(msgHeader);
				break;
			case FORMAT3 :
				if (rtmpData.getMsgCount() == 0) {
					throw new Exception("fresh rtmpData expect fmt=0, actual="+fmt+", csid="+csid);
				}
				break;
			default:
				throw new Exception("read rtmp header: invalid fmt=" + fmt);
		}

		rtmpData.setMsgCount(rtmpData.getMsgCount() + 1);
	}
}
