package io.gofannon.gullfoss.mainapp;

import io.gofannon.gullfoss.person_provider.PersonProvider;
import io.gofannon.gullfoss.translator.Language;
import io.gofannon.gullfoss.translator.MessageProvider;

public class MainApplication {
    public static void main(String[] args) {
        PersonProvider personProvider = new PersonProvider();
        MessageProvider messageProvider = new MessageProvider();

        personProvider.getNames()
                .stream()
                .map(it -> messageProvider.sayHelloTo(it, Language.FRENCH))
                .forEach(System.out::println);

        System.out.println("Copyright of person-provider : " + personProvider.getCopyright());
        System.out.println("Copyright of translator : " + messageProvider.getCopyright());
    }
}