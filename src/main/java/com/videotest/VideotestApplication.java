package com.videotest;

import com.videotest.rtmp.server.RtmpServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

@SpringBootApplication
@RestController
public class VideotestApplication {

	@Autowired
	private StreamingService service;

	@GetMapping(value = "video/{title}", produces = "video/mp4")
	public Mono<Resource> getViedo(@PathVariable String title, @RequestHeader("Range") String range) {
		System.out.println("range in bytes() : " + range);
		return service.getVideo(title);
	}

	@GetMapping(value = "hello")
	public Mono<String> getViedo() {
		return Mono.just("안녕");
	}

	// 전통적인 방법
	@GetMapping("/sync")
	public ResponseEntity<String> syncRequest() {
		RestTemplate restTemplate = new RestTemplate();
		String response = restTemplate.getForObject("https://naver.com", String.class);
		return ResponseEntity.ok(response);
	}

	// 리액티브 방법
	@GetMapping("/reactive")
	public Mono<ResponseEntity<String>> reactiveRequest() {
		WebClient webClient = WebClient.create();
		return webClient.get()
				.uri("https://naver.com")
				.retrieve()
				.toEntity(String.class);
	}

	// 스트리머 송출 여부 상태값
	private final AtomicBoolean isStreaming = new AtomicBoolean(false);

	// 송출 시작 알림
	@GetMapping(value = "/streamer/start", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<String> streamStartNotification() {
		// 몇초마다 알림을 하고 한 번 알림이 간 사람들은 목록에서 제거하고
		return Flux.interval(Duration.ofSeconds(5))
				.filter(tick -> isStreaming.get())
				.map(tick -> "Streamer has started streaming!");
	}

	// 스트리머가 방송을 시작할 때 호출되는 메서드
	@GetMapping("/streamer/startBroadcast")
	public void startStreaming() {
		isStreaming.set(true);
	}

	// 스트리머가 방송을 종료할 때 호출되는 메서드
	@GetMapping("/streamer/stopBroadcast")
	public void stopStreaming() {
		isStreaming.set(false);
	}

	public static void main(String[] args) throws InterruptedException {
		SpringApplication.run(VideotestApplication.class, args);
		new RtmpServer().start();
	}

}
