package com.videotest.rtmp.chunk.message;

import com.videotest.rtmp.chunk.header.MessageHeader;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class RtmpData {
	private byte format;
	private int curStreamId;
	private MessageHeader messageHeader;
	private ByteBuf payload;
	private long msgCount;
}
