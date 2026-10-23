
package com.nightstudy.moa;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
// AI작성: 테스트에서는 실제 메일을 발송하지 않고 로컬 인증번호 발송 Bean을 사용한다.
@ActiveProfiles("local")
class MoaApplicationTests {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void contextLoads() {
	}

	@Test
	void databaseConnectionWorks() {
		String databaseName = jdbcTemplate.queryForObject(
				"SELECT DATABASE()", String.class
		);

		Long memberCount = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM members", Long.class
		);

		assertEquals("final_project", databaseName);
		assertNotNull(memberCount);

		System.out.println("연결된 DB: " + databaseName);
		System.out.println("저장된 회원 수: " + memberCount);
	}
}
