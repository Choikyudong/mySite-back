package com.videotest.rtmp.chunk.message;

import lombok.Getter;

import java.util.List;

@Getter
public class RtmpAMF0CmdMsg extends RtmpBaseMsg {

    private int curStreamId;

    private List<Object> objectList;

    public RtmpAMF0CmdMsg(int curStreamId, List<Object> objectList) {
        super((byte) 20);
        this.curStreamId = curStreamId;
        this.objectList = objectList;
    }

}
