package com.videotest.rtmp.chunk.message;

import lombok.Getter;

@Getter
public class RtmpVideoMsg extends RtmpBaseMsg {

	private final long timeStamp;

	private final int timeStampDelta;

	private final int control;

	private final byte[] videoBytes;

	public RtmpVideoMsg(long timeStamp, int timeStampDelta, int control, byte[] videoBytes) {
		super((byte) 0x09);
		this.timeStamp = timeStamp;
		this.timeStampDelta = timeStampDelta;
		this.control = control;
		this.videoBytes = videoBytes;
	}

}
