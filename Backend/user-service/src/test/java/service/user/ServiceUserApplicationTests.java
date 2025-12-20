package service.user;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
class ServiceUserApplicationTests {

	@Test
	void contextLoads() {
		// This test is intentionally empty - it verifies that the Spring application context loads successfully
	}

}
