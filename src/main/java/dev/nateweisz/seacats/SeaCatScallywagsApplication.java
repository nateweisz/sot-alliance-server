package dev.nateweisz.seacats;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication(
    scanBasePackages = {"io.github.freya022.botcommands", "dev.nateweisz.seacats"}
)
@EnableCaching
public class SeaCatScallywagsApplication {

    public static void main(String[] args) {
        SpringApplication.run(SeaCatScallywagsApplication.class, args);
    }

}
