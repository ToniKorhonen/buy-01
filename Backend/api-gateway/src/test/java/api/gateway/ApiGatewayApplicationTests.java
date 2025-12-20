package api.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
class ApiGatewayApplicationTests {

	@Test
	void contextLoads() {
		// This test is intentionally empty - it verifies that the Spring application context loads successfully
	}

}
