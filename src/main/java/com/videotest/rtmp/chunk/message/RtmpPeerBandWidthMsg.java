package com.videotest.rtmp.chunk.message;

import lombok.Getter;

@Getter
public class RtmpPeerBandWidthMsg extends RtmpBaseMsg{

	private final int ackSize;
	private final byte limitType;

	public RtmpPeerBandWidthMsg(int ackSize, byte limitType) {
		super((byte) 0x06);
		this.ackSize = ackSize;
		this.limitType = limitType;
	}

}
