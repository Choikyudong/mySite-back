package com.videotest;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketChatHandler extends TextWebSocketHandler {

	private final ObjectMapper mapper;

	// 연결 세션 저장함
	private final Set<WebSocketSession> sessions = new HashSet<>();

	// 채팅룸 저장함
	private final Map<Long, Set<WebSocketSession>> chatRommSessionStorage = new HashMap<>();

	// 소켓연결확인
	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		sessions.add(session);
	}

	// 소켓통신 메시지 처리부분
	public void handleTextMessage(WebSocketSession socketSession, TextMessage message) throws Exception {
		String payload = message.getPayload();

		ChatMessageDto chatMessageDto = mapper.readValue(payload, ChatMessageDto.class);
		Long chatRoomId = chatMessageDto.getChatRoomId();

		// 세션 아이디가 없으면 새로운 채팅방 개설
		if (!chatRommSessionStorage.containsKey(chatRoomId)) {
			chatRommSessionStorage.put(chatRoomId, new HashSet<>());
		}

		Set<WebSocketSession> chatRoomSession = chatRommSessionStorage.get(chatRoomId);

		if (chatMessageDto.getMessageType().equals(ChatMessageDto.MessageType.ENTER)) {
			// 입장일 경우
			chatRoomSession.add(socketSession);
		}

		// 세션 닫는 방법
		if (chatRoomSession.size() > 3) {
			this.removeClosedSession(chatRoomSession);
		}

		this.sendMessageToChatRoom(chatMessageDto, chatRoomSession);
	}

	// 소켓 종료시
	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
		super.afterConnectionClosed(session, status);
	}

	private void removeClosedSession(Set<WebSocketSession> chatRoomSession) {
		chatRoomSession.removeIf(sess -> !sessions.contains(sess));
	}

	private void sendMessageToChatRoom(ChatMessageDto chatMessageDto, Set<WebSocketSession> chatRoomSession) {
		chatRoomSession.parallelStream().forEach(sess -> sendMessage(sess, chatMessageDto));
	}

	private <T> void sendMessage(WebSocketSession session, T message) {
		try {
			session.sendMessage(new TextMessage(mapper.writeValueAsString(message)));
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}
	}
}