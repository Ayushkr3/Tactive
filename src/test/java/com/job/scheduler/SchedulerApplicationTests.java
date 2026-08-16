package com.job.scheduler;

import java.util.TimeZone;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SchedulerApplicationTests {

	@Test
	void contextLoads() {
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
	}

}
