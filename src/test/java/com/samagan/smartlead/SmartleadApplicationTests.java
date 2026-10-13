package com.samagan.smartlead;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "spring.ai.openai.api-key=test-key")
class SmartleadApplicationTests {

    @Test
    void contextLoads() {
    }

}
