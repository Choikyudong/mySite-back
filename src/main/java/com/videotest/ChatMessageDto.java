package com.videotest;

import lombok.Data;

@Data // 테스트용
public class ChatMessageDto {
	
	public enum MessageType {
		ENTER, TALK
	}

	private MessageType messageType;
	private Long chatRoomId;
	private Long senderId;
	private String message;
	
}
