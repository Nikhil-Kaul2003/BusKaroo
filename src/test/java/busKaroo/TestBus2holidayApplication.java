package busKaroo;

import org.springframework.boot.SpringApplication;

public class TestBus2holidayApplication {

    public static void main(String[] args) {
        SpringApplication.from(Bus2holidayApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
