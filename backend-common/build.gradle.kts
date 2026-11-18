plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    `java-library`
}

kotlin { jvmToolchain(21) }

dependencies {
    api("org.springframework.boot:spring-boot-starter-web:3.5.16")
    api("org.springframework.boot:spring-boot-starter-security:3.5.16")
    api("org.springframework.boot:spring-boot-starter-validation:3.5.16")
    api("io.jsonwebtoken:jjwt-api:0.12.6")
    api("org.springframework.boot:spring-boot-starter-jdbc:3.5.16")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")
    implementation(kotlin("reflect"))
    testImplementation("org.springframework.boot:spring-boot-starter-test:3.5.16")
}
