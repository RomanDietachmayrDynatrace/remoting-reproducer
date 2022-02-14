plugins {
    id("java-library")
}

val javaeeVersion = "7.0"
val slf4jApiVersion = "1.7.30"
val openTelemetryVersion = "1.5.0"

dependencies {
    api(project(":echo-ejb-api"))

    implementation("org.slf4j:slf4j-api:${slf4jApiVersion}")

    compileOnly("javax:javaee-api:${javaeeVersion}")

    implementation("io.opentelemetry:opentelemetry-api:${openTelemetryVersion}")
    implementation("io.opentelemetry:opentelemetry-semconv:${openTelemetryVersion}-alpha")

    implementation(platform("io.opentelemetry:opentelemetry-bom:1.5.0"))
    implementation("io.opentelemetry:opentelemetry-exporter-otlp-http-trace")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}