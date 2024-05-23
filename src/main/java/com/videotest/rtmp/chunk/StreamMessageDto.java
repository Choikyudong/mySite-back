package com.videotest.rtmp.chunk;

import lombok.Data;

import java.util.List;

@Data
public class StreamMessageDto {

	private byte messageTypeId;
	private int curStreamId;
	private List<Object> list;

	public StreamMessageDto(int curStreamId, List<Object> list) {
		this.messageTypeId = 20;
		this.curStreamId = curStreamId;
		this.list = list;
	}
}
