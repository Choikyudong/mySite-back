package com.videotest.rtmp.chunk.message;

import lombok.Getter;

/**
 * 공통적으로 사용되는 클래스
 */
@Getter
public abstract class RtmpBaseMsg {

	private final byte messageTypeId;

	public RtmpBaseMsg(byte messageTypeId) {
		this.messageTypeId = messageTypeId;
	}

}
