plugins {
    id("java-library")
    id ("com.google.cloud.tools.jib") version "3.1.4"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

val wildflyVersion = "7.3.9.GA"
val springbootVersion = "2.5.7"
val openTelemetryVersion = "1.5.0"
val openTelemetryAgentVersion = "1.5.3"


dependencies {
    implementation(project(":echo-ejb-api"))

    implementation(enforcedPlatform("org.springframework.boot:spring-boot-dependencies:${springbootVersion}"))
    implementation("org.springframework.boot:spring-boot-starter-web")

    implementation(enforcedPlatform("org.jboss.eap:wildfly-ejb-client-bom:${wildflyVersion}"))
    implementation("org.wildfly.wildfly-http-client:wildfly-http-ejb-client")

    implementation("org.springframework.boot:spring-boot-starter-log4j2")
    implementation("org.springframework.boot:spring-boot-starter-undertow")

    implementation("io.opentelemetry:opentelemetry-api:${openTelemetryVersion}")
    implementation("io.opentelemetry:opentelemetry-semconv:${openTelemetryVersion}-alpha")

    implementation(platform("io.opentelemetry:opentelemetry-bom:1.5.0"))
    implementation("io.opentelemetry:opentelemetry-exporter-otlp-http-trace")
}

configurations.all {
    exclude(module = "spring-boot-starter-logging")
    exclude(module = "spring-boot-starter-tomcat")
}

val opentelemetryAgentConfig: Configuration by configurations.creating

dependencies {
    opentelemetryAgentConfig(
        group = "io.opentelemetry.javaagent",
        name = "opentelemetry-javaagent",
        classifier = "all",
        version = openTelemetryAgentVersion
    )
}

val downloadOpenTelemetryAgent = tasks.register<Sync>("downloadOpenTelemetryAgent") {
    from(opentelemetryAgentConfig) {
        rename("opentelemetry-javaagent-.*", "opentelemetry-javaagent.jar")
    }
    into("$buildDir/opentelemetry-javaagent/opt/app/bin")
}

tasks.getByName("processResources").dependsOn(downloadOpenTelemetryAgent)

jib {
    from {
        image = "openjdk:11.0.13-jdk"
        auth { // TODO replace with correct credentials
            username = "<user>"
            password = "<password>"
        }
    }
    to {
        image = "test/spring-server:1.0.0"
    }

    container {
        ports = listOf("8080")
        entrypoint = listOf("/docker-entrypoint.sh")
        args = listOf("start")
    }

    extraDirectories {
        paths {
            path {
                setFrom(file("$buildDir/opentelemetry-javaagent").toPath())
                into = "/"
            }
            path {
                setFrom(file("$projectDir/config/docker").toPath())
                into = "/"
            }
        }
        permissions = mapOf(
            "/docker-entrypoint.sh" to "755",
        )
    }
}
