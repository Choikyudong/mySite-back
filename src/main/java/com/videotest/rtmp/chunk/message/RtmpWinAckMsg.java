package com.videotest.rtmp.chunk.message;

import lombok.Getter;

@Getter
public class RtmpWinAckMsg extends RtmpBaseMsg {

	private final int acknowledgementSize;

	public RtmpWinAckMsg(int acknowledgementSize) {
		super((byte) 0x05);
		this.acknowledgementSize = acknowledgementSize;
	}
}
