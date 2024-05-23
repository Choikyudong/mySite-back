package com.videotest.rtmp.chunk.message;

import lombok.Getter;

@Getter
public class RtmpAbortMsg extends RtmpBaseMsg {

	private final int abortCsid;

	public RtmpAbortMsg(int abortCsid) {
		super((byte) 0x02);
		this.abortCsid = abortCsid;
	}

}
