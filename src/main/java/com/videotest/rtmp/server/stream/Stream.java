package com.videotest.rtmp.server.stream;

import com.videotest.rtmp.chunk.message.RtmpAudioMsg;
import com.videotest.rtmp.chunk.message.RtmpBaseMsg;
import com.videotest.rtmp.chunk.message.RtmpTextMsg;
import com.videotest.rtmp.chunk.message.RtmpVideoMsg;
import io.netty.channel.Channel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedList;
import java.util.Map;

@Slf4j
public class Stream {
    private final StreamId streamId;
    private final LinkedList<Channel> players;
    private final LinkedList<RtmpBaseMsg> gopCache;

    @Getter
    private Channel publisher;

    @Getter
    @Setter
    private Map<String, Object> metaData;

    // AVCDecoderConfigurationRecord defined in ISO-14496-15 AVC file format
    private RtmpVideoMsg avcSequenceHeader;
    // AudioSpecificConfig defined in ISO-14496-3 Audio
    private RtmpAudioMsg aacSequenceHeader;


    public Stream(StreamId streamId, Channel publisher) {
        this.streamId = streamId;
        this.publisher = publisher;
        this.players = new LinkedList<>();
        this.gopCache = new LinkedList<>();
    }

    public synchronized void addPlayer(Channel player) {
        if (player != null && player.isActive()) {
            players.add(player);

            // write AVC/AAC Sequence Header first
            if (avcSequenceHeader != null) {
                player.writeAndFlush(avcSequenceHeader);
            }
            if (aacSequenceHeader != null) {
                player.writeAndFlush(aacSequenceHeader);
            }
            // write gop cache then
            for (RtmpBaseMsg msg : gopCache) {
                player.writeAndFlush(msg);
            }
        }
    }

    public synchronized void onRecvVideo(RtmpVideoMsg msg) {
        if (msg.isAVCSequenceHeader()) {
            log.info("<-- recv AVC Sequence Header, stream=" + streamId);
            avcSequenceHeader = msg;
        }
        if (msg.isH264KeyFrame()) {
            log.info("<-- recv key frame, stream=" + streamId);
            gopCache.clear();
        }

        gopCache.add(msg);
        broadcastToPlayers(msg);
    }

    public synchronized void onRecvAudio(RtmpAudioMsg msg) {
        if (msg.isAACSequenceHeader()) {
            log.info("<-- recv AAC Sequence Header, stream=" + streamId);
            aacSequenceHeader = msg;
        }

        gopCache.add(msg);
        broadcastToPlayers(msg);
    }

    public void onRecvText(RtmpTextMsg msg) {
        broadcastToPlayers(msg);
    }

    private void broadcastToPlayers(RtmpBaseMsg msg) {
        for (Channel player : players) {
            if (player.isActive()) {
                player.writeAndFlush(msg);
            }
        }
    }

}
