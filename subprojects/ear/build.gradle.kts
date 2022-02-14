plugins {
    id("ear")
}

dependencies {
    earlib(project(":echo-ejb-api"))
    earlib("org.slf4j:slf4j-api:1.7.30")

    earlib("io.opentelemetry:opentelemetry-api:1.5.0")
    earlib("io.opentelemetry:opentelemetry-semconv:1.5.0-alpha")

    earlib(platform("io.opentelemetry:opentelemetry-bom:1.5.0"))
    earlib("io.opentelemetry:opentelemetry-exporter-otlp-http-trace")

    deploy(project( ":echo-ejb"))
}

ear {
    deploymentDescriptor {
        libDirName = "APP-INF/lib"
    }
}
