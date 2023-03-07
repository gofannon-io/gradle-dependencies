package io.gofannon.gullfoss.copyrighter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


class CopyrightProviderTest {

    private CopyrightProvider copyrightProvider;

    @BeforeEach
    void setUp() {
        copyrightProvider = new CopyrightProvider();
    }

    @Test
    void getCopyright() {
        assertThat(
                copyrightProvider.getCopyright()
        ).startsWith("Apache");
    }
}