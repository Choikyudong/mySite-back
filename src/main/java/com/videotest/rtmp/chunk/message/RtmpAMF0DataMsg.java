package com.videotest.rtmp.chunk.message;

import lombok.Getter;

import java.util.List;

@Getter
public class RtmpAMF0DataMsg extends RtmpBaseMsg {

	private int curStreamId;

	private List<Object> dataList;

	public RtmpAMF0DataMsg(int curStreamId, List<Object> dataList) {
		super((byte) 18);
		this.curStreamId = curStreamId;
		this.dataList = dataList;
	}

}
