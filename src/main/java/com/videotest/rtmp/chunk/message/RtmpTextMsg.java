package com.videotest.rtmp.chunk.message;

import lombok.Getter;

@Getter
public class RtmpTextMsg extends RtmpBaseMsg {

	private final long timeStamp;

	private final int timeStampDelta;

	private final byte[] textBytes;

	public RtmpTextMsg(long timeStamp, int timeStampDelta, byte[] textBytes) {
		super((byte) 0x09);
		this.timeStamp = timeStamp;
		this.timeStampDelta = timeStampDelta;
		this.textBytes = textBytes;
	}

}
