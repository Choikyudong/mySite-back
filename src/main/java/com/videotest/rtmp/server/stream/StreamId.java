package com.videotest.rtmp.server.stream;

import lombok.ToString;

@ToString
public class StreamId {
    private final String appName;
    private final String streamName;

    public StreamId(String appName, String streamName) {
        this.appName = appName;
        this.streamName = streamName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        StreamId streamId = (StreamId) o;

        if (!appName.equals(streamId.appName)) {
            return false;
        }

        return streamName.equals(streamId.streamName);
    }

    @Override
    public int hashCode() {
        int result = appName.hashCode();
        result = 31 * result + streamName.hashCode();
        return result;
    }

}
