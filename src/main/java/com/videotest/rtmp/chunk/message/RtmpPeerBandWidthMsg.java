package com.videotest.rtmp.chunk.message;

import lombok.Getter;

@Getter
public class RtmpPeerBandWidthMsg extends RtmpWinAckMsg {

	private final byte limitType;

	public RtmpPeerBandWidthMsg(int acknowledgementSize, byte limitType) {
		super(acknowledgementSize);
		this.limitType = limitType;
	}

}
