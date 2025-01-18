plugins {
    java
    id("org.springframework.boot") version "3.4.0"
    id("io.spring.dependency-management") version "1.1.6"
    id("com.diffplug.spotless") version "7.0.0.BETA4"
    application
}

group = "dev.nateweisz"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    runtimeOnly("com.h2database:h2")
    implementation("io.github.freya022:BotCommands:3.0.0-alpha.21")
    implementation("net.dv8tion:JDA:5.2.1")
    implementation("org.flywaydb:flyway-core:11.0.1")
    runtimeOnly("org.flywaydb:flyway-database-postgresql:11.0.1")
    implementation("com.github.loki4j:loki-logback-appender:1.5.2")
    implementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.14")
    implementation("org.json:json:20240303")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

application {
    mainClass.set("dev.nateweisz.seacats.SeaCatScallywagsApplication")
}

spotless {
    java {
        trimTrailingWhitespace()
        removeUnusedImports()
        importOrder("\\#", "java", "javax", "dev.nateweisz", "")
        eclipse().configFile("spotless.xml")
    }
}