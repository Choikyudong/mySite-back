package com.videotest.rtmp.server;

import com.videotest.rtmp.chunk.message.*;
import com.videotest.rtmp.chunk.type.AMF0;
import com.videotest.rtmp.server.stream.Stream;
import com.videotest.rtmp.server.stream.StreamId;
import com.videotest.rtmp.server.stream.StreamManager;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class MessageHandler {

	private Stream stream;

	public void handleVideo(RtmpVideoMsg msg) {
		if (stream == null) {
			return;
		}
		stream.onRecvVideo(msg);
	}

	public void handleAudio(RtmpAudioMsg msg) {
		if (stream == null) {
			return;
		}
		stream.onRecvAudio(msg);
	}

	public void handleText(RtmpTextMsg msg) {
		if (stream == null) {
			return;
		}
		log.info("--> " + new String(msg.getTextBytes()));
		stream.onRecvText(msg);
	}

	public void handleAMF0Data(ChannelHandlerContext ctx, RtmpAMF0DataMsg data) {
		if (data == null) {
			return;
		}
		List<Object> dataList = data.getDataList();
		if ("@setDataFrame".equals(dataList.get(0))) {
			Map<String, Object> metaData = (Map<String, Object>) dataList.get(2);
			if (stream != null) {
				stream.setMetaData(metaData);
			}
		}
	}

	public void handleAMF0Command(ChannelHandlerContext ctx, RtmpAMF0CmdMsg command) {
		if (command == null) {
			return;
		}
		List<Object> decodedObjectList = command.getObjectList();
		if (decodedObjectList == null) {
			return;
		}
		String commandName = null;
		for (Object decodedObj : decodedObjectList) {
			int type = AMF0.getType(decodedObj);
			if (type == AMF0.String) {
				commandName = (String) decodedObj;
				break;
			}
		}
		if (commandName == null) {
			return;
		}

		doHandleAMF0Command(ctx, commandName, command);
	}


	private void doHandleAMF0Command(ChannelHandlerContext ctx, final String commandName, final RtmpAMF0CmdMsg command) {
		log.info("<-- recv command: " + commandName);
		switch (commandName) {
			case "connect":
				doHandleConnect(ctx, command);
				return;
			case "releaseStream":
				doHandleReleaseStream(ctx, command);
				return;
			case "FCPublish":
				doHandleFCPublish(ctx, command);
				return;
			case "createStream":
				doHandleCreateStream(ctx, command);
				return;
			case "publish":
				doHandlePublish(ctx, command);
				return;
			case "FCUnpublish":
				return;
			case "deleteStream":
				return;
			case "play":
				doHandlePlay(ctx, command);
				return;
			default:
				return; // todo : throw 필요
		}
	}

	private void doHandlePlay(final ChannelHandlerContext ctx, final RtmpAMF0CmdMsg command) {
		String streamName = (String) command.getObjectList().get(3);
		if (streamName == null) {
			log.error("invalid play message");
			return;
		}
		int idx = streamName.indexOf('?');
		if (idx != -1) {
			streamName = streamName.substring(0, idx);
		}
		StreamId streamId = new StreamId("kyu", streamName); // todo : 속성값으로 뺴야함
		Stream stream = StreamManager.getStream(streamId);
		if (stream == null) {
			log.error("stream=" + streamId + " not found");
			return;
		}

		// send streamBegin
		log.info("--> send streamBegin for play");
		RtmpUserControlMsg streamBegin = new RtmpUserControlMsg((short) 0, 0);
		ctx.channel().writeAndFlush(streamBegin);

		// send onStatus('NetStream.Play.Start')
		log.info("--> send onStatus('NetStream.Play.Start') for play");
		List<Object> onStatus = new ArrayList<>();
		onStatus.add("onStatus");
		onStatus.add(0);
		onStatus.add(null);
		LinkedHashMap<String, Object> amf0Object = new LinkedHashMap<>();
		amf0Object.put("level", "status");
		amf0Object.put("code", "NetStream.Play.Start");
		amf0Object.put("description", "Start live");
		onStatus.add(amf0Object);
		RtmpAMF0CmdMsg onStatusCommand = new RtmpAMF0CmdMsg(command.getCurStreamId(), onStatus);
		ctx.channel().writeAndFlush(onStatusCommand);

		// send |RtmpSampleAccess()
		log.info("--> send |RtmpSampleAccess() for play");
		List<Object> sampleAccess = new ArrayList<>();
		sampleAccess.add("|RtmpSampleAccess");
		sampleAccess.add(true);
		sampleAccess.add(true);
		RtmpAMF0DataMsg sampleAccessDataMessage = new RtmpAMF0DataMsg(command.getCurStreamId(), sampleAccess);
		ctx.channel().writeAndFlush(sampleAccessDataMessage);

		// send onMetaData()
		log.info("--> send onMetaData() for play");
		List<Object> onMetaData = new ArrayList<>();
		onMetaData.add("onMetaData");
		LinkedHashMap<String, Object> amf0Object1 = new LinkedHashMap<>();
		if (stream.getMetaData() != null) {
			amf0Object1.putAll(stream.getMetaData());
		}
		onMetaData.add(amf0Object1);
		RtmpAMF0DataMsg dataMessage = new RtmpAMF0DataMsg(command.getCurStreamId(), onMetaData);
		ctx.channel().writeAndFlush(dataMessage);

		// start playing
		log.info(streamId + " is playing");
		stream.addPlayer(ctx.channel());
	}

	private void doHandlePublish(final ChannelHandlerContext ctx, final RtmpAMF0CmdMsg command) {
		String streamName = (String) command.getObjectList().get(3);
		String appName = (String) command.getObjectList().get(4);
		if (appName == null || streamName == null) {
			log.error("invalid publish message");
			return;
		}
		if (!"kyu".equals(appName)) {
			log.error("app: " + appName + " not support");
			return;
		}
		int idx = streamName.indexOf('?');
		if (idx != -1) {
			streamName = streamName.substring(0, idx);
		}
		StreamId streamId = new StreamId(appName, streamName);
		Stream newStream = new Stream(streamId, ctx.channel());
		boolean createSuccess = StreamManager.createStream(streamId, newStream);
		if (!createSuccess) {
			log.error("refuse duplicated stream=" + streamId);
			return;
		}
		stream = newStream;
		log.info(streamId + " is publishing");

		// send onFCPublish()
		log.info("--> send onFCPublish() for publish");
		List<Object> onFCPublish = new ArrayList<>();
		onFCPublish.add("onFCPublish");
		onFCPublish.add(0);
		onFCPublish.add(null);
		LinkedHashMap<String, Object> amf0Object = new LinkedHashMap<>();
		amf0Object.put("code", "NetStream.Publish.Start");
		amf0Object.put("description", "Started publishing stream.");
		onFCPublish.add(amf0Object);
		RtmpAMF0CmdMsg onFCPublishCommand = new RtmpAMF0CmdMsg(command.getCurStreamId(), onFCPublish);
		ctx.channel().writeAndFlush(onFCPublishCommand);

		// send onStatus('NetStream.Publish.Start')
		log.info("--> send onStatus('NetStream.Publish.Start') for publish");
		List<Object> onStatus = new ArrayList<>();
		onStatus.add("onStatus");
		onStatus.add(0);
		onStatus.add(null);
		LinkedHashMap<String, Object> amf0Object1 = new LinkedHashMap<>();
		amf0Object1.put("level", "status");
		amf0Object1.put("code", "NetStream.Publish.Start");
		amf0Object1.put("description", "Started publishing stream.");
		amf0Object1.put("clientid", "ASAICiss");
		onStatus.add(amf0Object1);
		RtmpAMF0CmdMsg onStatusCommand = new RtmpAMF0CmdMsg(command.getCurStreamId(), onStatus);
		ctx.channel().writeAndFlush(onStatusCommand);
	}

	private void doHandleCreateStream(final ChannelHandlerContext ctx, final RtmpAMF0CmdMsg command) {
		Object number = getNumber(command.getObjectList());
		// send _result
		log.info("--> send _result for createStream");
		List<Object> _result = new ArrayList<>();
		_result.add("_result");
		_result.add(number);
		_result.add(null);
		RtmpAMF0CmdMsg _resultCommand = new RtmpAMF0CmdMsg(command.getCurStreamId(), _result);
		ctx.channel().writeAndFlush(_resultCommand);
	}

	private void doHandleFCPublish(final ChannelHandlerContext ctx, final RtmpAMF0CmdMsg command) {
		Object number = getNumber(command.getObjectList());
		// send _result
		log.info("--> send _result for FCPublish");
		List<Object> _result = new ArrayList<>();
		_result.add("_result");
		_result.add(number);
		_result.add(null);
		_result.add(null); //should be undefined
		RtmpAMF0CmdMsg _resultCommand = new RtmpAMF0CmdMsg(command.getCurStreamId(), _result);
		ctx.channel().writeAndFlush(_resultCommand);
	}


	private void doHandleReleaseStream(final ChannelHandlerContext ctx, final RtmpAMF0CmdMsg command) {
		Object number = getNumber(command.getObjectList());
		// send _result
		log.info("--> send _result for releaseStream");
		List<Object> _result = new ArrayList<>();
		_result.add("_result");
		_result.add(number);
		_result.add(null);
		_result.add(null); //should be undefined
		RtmpAMF0CmdMsg _resultCommand = new RtmpAMF0CmdMsg(command.getCurStreamId(), _result);
		ctx.channel().writeAndFlush(_resultCommand);
	}

	private void doHandleConnect(final ChannelHandlerContext ctx, final RtmpAMF0CmdMsg command) {
		// send Window Acknowledgement Size
		log.info("--> send Window Acknowledgement Size for connect");
		ctx.channel().writeAndFlush(new RtmpWinAckMsg(250000));

		// send SetPeerBandwidth
		log.info("--> send SetPeerBandwidth for connect");
		ctx.channel().writeAndFlush(new RtmpPeerBandWidthMsg(2500000, (byte) 2));

		// send SetChunkSize
		log.info("--> send SetChunkSize for connect");
		ctx.channel().writeAndFlush(new RtmpChunkMsg(4096));

		Object number = getNumber(command.getObjectList());
		// send _result('NetConnection.Connect.Success')
		log.info("--> send _result('NetConnection.Connect.Success') for connect");
		List<Object> _result = new ArrayList<>();
		_result.add("_result");
		_result.add(number);
		LinkedHashMap<String, Object> amf0Object1 = new LinkedHashMap<>();
		amf0Object1.put("fmsVer", "FMS/3,5,3,888");
		amf0Object1.put("capabilities", 127);
		amf0Object1.put("mode", 1);
		LinkedHashMap<String, Object> amf0Object2 = new LinkedHashMap<>();
		amf0Object2.put("level", "status");
		amf0Object2.put("code", "NetConnection.Connect.Success");
		amf0Object2.put("description", "Connection succeeded");
		amf0Object2.put("objectEncoding", 0);
		_result.add(amf0Object1);
		_result.add(amf0Object2);
		ctx.channel().writeAndFlush(new RtmpAMF0CmdMsg(command.getCurStreamId(), _result));

		// send onBWDone()
		log.info("--> send onBWDone() for connect");
		List<Object> onBWDone = new ArrayList<>();
		onBWDone.add("onBWDone");
		onBWDone.add(number);
		onBWDone.add(null);
		ctx.channel().writeAndFlush(new RtmpAMF0CmdMsg(command.getCurStreamId(), onBWDone));
	}



	private Object getNumber(List<Object> decodedObjectList) {
		Object number = 0;
		for (Object o : decodedObjectList) {
			int type = AMF0.getType(o);
			if (type == AMF0.Number) {
				number = o;
				break;
			}
		}
		return number;
	}

}
