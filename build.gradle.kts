plugins {
    id("java")
    application
}

group = "exchange"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.netty:netty-all:4.1.108.Final")
    implementation("org.slf4j:slf4j-api:2.0.13")
    runtimeOnly("org.slf4j:slf4j-simple:2.0.13")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("it.unimi.dsi:fastutil:8.5.9")
}

application {
    mainClass.set("exchange.Main")
}

tasks.test {
    useJUnitPlatform()
}
