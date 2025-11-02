package co.cobre.cbmm.accounts;

import co.cobre.cbmm.accounts.base.BaseContainerTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class MsAccountsApplicationTests extends BaseContainerTest {

	@Autowired
	private ApplicationContext applicationContext;

	@BeforeAll
	static void setup() {
		//initContainers();
	}

	@AfterAll
	static void tearDown() {
        //shutdownContainers();
	}

	@Test
	void contextLoads() {
		assertThat(applicationContext).isNotNull();
	}

}
