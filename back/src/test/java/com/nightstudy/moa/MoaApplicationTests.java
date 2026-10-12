
package com.nightstudy.moa;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
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