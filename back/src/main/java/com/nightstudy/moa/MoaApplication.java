package com.nightstudy.moa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
// AI작성: 거래 조회 중 자동 분류를 별도 작업으로 처리해 HTTP 응답을 지연시키지 않는다.
@EnableAsync
@EnableScheduling // AI작성: SSE 연결 유지를 위한 예약 작업을 활성화합니다.
public class MoaApplication {

	public static void main(String[] args) {
		SpringApplication.run(MoaApplication.class, args);
	}

}
