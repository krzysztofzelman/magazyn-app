package com.example.magazyn;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Requires a running PostgreSQL instance — excluded from CI")
class MagazynApplicationTests {

	@Test
	void contextLoads() {
	}

}
