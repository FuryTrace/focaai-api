package com.example.focaai_api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = "resend.api-key=chave-de-teste-sem-uso")
class FocaaiApiApplicationTests {

    @Test
    void contextLoads() {
    }
}
