package io.gofannon.gullfoss.translator;

import io.gofannon.gullfoss.copyrighter.CopyrightProvider;
import org.jetbrains.annotations.NotNull;

public class MessageProvider {

    @NotNull
    public String sayHelloTo(@NotNull String personName, @NotNull Language language) {
        String template = getSayHelloToTemplate(language);
        return String.format(template, personName);
    }

    private String getSayHelloToTemplate(@NotNull Language language) {
        return switch (language) {
            case FRENCH -> "Bonjour %s !";
            case ENGLISH -> "Hello %s !";
        };
    }

    public String getCopyright() {
        return new CopyrightProvider().getCopyright();
    }
}