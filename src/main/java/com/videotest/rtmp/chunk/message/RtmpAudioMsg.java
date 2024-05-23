package com.videotest.rtmp.chunk.message;

import lombok.Getter;

@Getter
public class RtmpAudioMsg extends RtmpBaseMsg {

	private final long timeStamp;

	private final int timeStampDelta;

	private final int control;

	private final byte[] audioBytes;

	public RtmpAudioMsg(long timeStamp, int timeStampDelta, int control, byte[] audioBytes) {
		super((byte) 0x08);
		this.timeStamp = timeStamp;
		this.timeStampDelta = timeStampDelta;
		this.control = control;
		this.audioBytes = audioBytes;
	}
}
