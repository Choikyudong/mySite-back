package com.videotest.rtmp.chunk.message;

import lombok.Getter;

/**
 * 데이터를 처리하는 클래스
 */
@Getter
public class RtmpChunkMsg extends RtmpBaseMsg {

	private final int chunkSize;

	public RtmpChunkMsg(int chunkSize) {
		super((byte) 0x01);
		this.chunkSize = chunkSize;
	}

}
