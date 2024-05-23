package com.videotest.rtmp.chunk.message;

import lombok.Getter;

@Getter
public class RtmpAckMsg extends RtmpBaseMsg {

	private final int sequnceNumber;

	public RtmpAckMsg(int sequnceNumber) {
		super((byte) 0x03);
		this.sequnceNumber = sequnceNumber;
	}

}
