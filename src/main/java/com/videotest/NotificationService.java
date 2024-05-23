package com.videotest;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

// 웹소켓 기반 알림
@Component
public class NotificationService implements WebSocketHandler {

	// 서버에서 10초마다 알림을 보냄
	@Override
	public Mono<Void> handle(WebSocketSession session) {
		Flux<String> intervalFlux = Flux.interval(Duration.ofSeconds(10))
				.map(i -> "Server Time: " + System.currentTimeMillis());

		return session.send(intervalFlux.map(session::textMessage));
	}
}
