package io.gofannon.gullfoss.translator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class MessageProviderTest {

    private MessageProvider messageProvider;

    @BeforeEach
    void setUp() {
        messageProvider = new MessageProvider();
    }

    @AfterEach
    void tearDown() {
    }


    @DisplayName("sayHelloTo shall translate in provided language")
    @ParameterizedTest
    @CsvSource({"ENGLISH, Hello John !", "FRENCH, Bonjour John !"})
    void sayHelloTo_english(Language language, String expectedMessage) {
        assertThat(messageProvider.sayHelloTo("John", language))
                .isEqualTo(expectedMessage);
    }
}