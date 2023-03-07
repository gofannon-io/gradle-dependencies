package io.gofannon.gullfoss.person_provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

//import static org.junit.jupiter.api.Assertions.*;

class PersonProviderTest {

    private PersonProvider personProvider;

    @BeforeEach
    void setUp() {
        personProvider = new PersonProvider();
    }

    @Test
    void getNames() {
        assertThat(personProvider.getNames())
                .containsExactly(
                        "John", "Jane", "Olivier", "Olivia"
                );
    }
}