package com.videotest.rtmp.server;

import com.videotest.rtmp.util.pipeline.RtmpDecoder;
import com.videotest.rtmp.util.pipeline.RtmpEncoder;
import com.videotest.rtmp.util.pipeline.HandShakeHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

// 송신서버
@Slf4j
public class RtmpServer {

	private static final boolean epollAvailable = Epoll.isAvailable();

	/*
		이벤트 루프 그룹 설명
		EpollEventLoopGroup :  linux 에서 높은 네트워크 성능
		KQueueEventLoopGroup : Epoll 과 빗스하며 macOS와 BSD 에서 사용 (webFlux에없음)
		NioEventLoopGroup : 운영체제 독립적 NIO 기반 멀티플렉싱
		이벤트 루프 그룹의 존재 이유
		1. 비동기 I/O 처리
		2. 이벤트 루프 관리
		3. 멀리플렉싱 지원
		수행하는 작업
		1. 데이터 읽기/쓰기
		2. 연결 관리
		3. 타이멈 관리
	 */

	public void start() {
		EventLoopGroup bossGroup = selectGroup();
		EventLoopGroup workerGroup = selectGroup();
		try {
			ServerBootstrap bootstrap = new ServerBootstrap();
			bootstrap.group(bossGroup, workerGroup)
				.channel(selectChannel())
				.localAddress(new InetSocketAddress(1935))
				.childOption(ChannelOption.TCP_NODELAY, true)
				.childHandler(new ChannelInitializer<SocketChannel>() {
					@Override
					public void initChannel(@NonNull SocketChannel ch) {
						ch.pipeline()
							.addLast(new HandShakeHandler())
							.addLast(new RtmpDecoder())
							.addLast(new RtmpEncoder());
					}
				})
				.option(ChannelOption.SO_BACKLOG, 128);

			ChannelFuture channelFuture = bootstrap.bind().sync();
			channelFuture.channel().closeFuture().sync();
		} catch (InterruptedException e){
			log.info(e.getMessage());
		}  finally {
			workerGroup.shutdownGracefully();
			bossGroup.shutdownGracefully();
		}
	}

	private static EventLoopGroup selectGroup() {
		if (epollAvailable) {
			return new EpollEventLoopGroup();
		}
		return new NioEventLoopGroup();
	}

	private static Class<? extends ServerChannel> selectChannel() {
		if (epollAvailable) {
			return EpollServerSocketChannel.class;
		}
		return NioServerSocketChannel.class;
	}

}
