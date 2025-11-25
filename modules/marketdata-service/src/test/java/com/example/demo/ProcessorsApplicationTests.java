package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Confirms the demo application context can start and shut down cleanly.
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ProcessorsApplicationTests {

    @Test
    void contextLoads() {
        // given

        // when

        // then
    }

}
