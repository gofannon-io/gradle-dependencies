package io.gofannon.gullfoss.person_provider;

import io.gofannon.gullfoss.copyrighter.CopyrightProvider;
import org.jetbrains.annotations.NotNull;

import java.util.List;


public class PersonProvider {
    @NotNull
    public List<String> getNames() {
        return List.of("John", "Jane", "Olivier", "Olivia");
    }

    public String getCopyright() {
        return new CopyrightProvider().getCopyright();
    }
}
