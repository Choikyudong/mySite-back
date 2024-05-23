package com.videotest.rtmp.chunk.message;

import lombok.Getter;

@Getter
public class RtmpUserControlMsg extends RtmpBaseMsg {

	private int msgStreamID = 0;

	private int csid = 2;

	private short eventType;

	private int eventData;

	public RtmpUserControlMsg(short eventType, int eventData) {
		super((byte) 0x04);
		this.eventType = eventType;
		this.eventData = eventData;
	}

}
