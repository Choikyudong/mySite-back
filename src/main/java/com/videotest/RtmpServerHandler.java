package com.videotest;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

// netty로 rtmp server 구현
public class RtmpServerHandler extends ChannelInboundHandlerAdapter {

	@Override
	public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
		System.out.println("channelRegistered..");
		ctx.fireChannelRegistered();
	}

	@Override
	public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
		ctx.fireChannelUnregistered();
		System.out.println("channelUnregistered..");
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		ctx.fireChannelActive();
		System.out.println("channelActive..");
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		ctx.fireChannelInactive();
		System.out.println("channelInactive..");
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		ctx.fireChannelRead(msg);
		System.out.println("channelRead..");
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		ctx.fireChannelReadComplete();
		System.out.println("channelReadComplete..");
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		ctx.fireUserEventTriggered(evt);
		System.out.println("userEventTriggered..");
	}

	@Override
	public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
		ctx.fireChannelWritabilityChanged();
		System.out.println("channelWritabilityChanged..");
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		ctx.fireExceptionCaught(cause);
		System.out.println("exceptionCaught..");
	}

}
