package com.videotest.rtmp.chunk.header;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
@Setter
public class MessageHeader {

	private byte typeId;

	private int msgStreamId;

	private int payloadlength;

	private long timeStamp;

	private int timeStampDelta;

}
