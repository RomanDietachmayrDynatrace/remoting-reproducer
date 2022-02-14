plugins {
    id("java-library") // ugly hack for using jib...

    id ("com.google.cloud.tools.jib") version "3.1.4"
}

val openTelemetryAgentVersion = "1.5.3"

jib {
    from {
         // https://catalog.redhat.com/software/containers/jboss-eap-7/eap73-openjdk11-openshift-rhel8/5df2b327d70cc54e2d109df8?container-tabs=overview
         image = "registry.redhat.io/jboss-eap-7/eap73-openjdk11-openshift-rhel8:7.3.9-3.1638383381"
         auth { // TODO replace with correct credentials
             username = "<user>"
             password = "<password>"
         }
    }
    to {
        image = "test/jboss-server:1.0.0"
    }

    // the application as jar, not single class files
    containerizingMode = "packaged"

    container {
        user = "jboss"

        appRoot = "/opt/app"
        ports = listOf("8080", "9990", "8787")
        entrypoint = listOf("/docker-entrypoint.sh")
        args = listOf("start")
    }

    extraDirectories {
        paths {
            path {
                setFrom(project.file("config/docker").toPath())
                into = "/"
            }
            path {
                setFrom(file("${project(":ear").buildDir}/libs").toPath())
                into = "/asd" // somewhere as jib will change folder rights to root and jboss cannot handle this
            }
            path {
                setFrom(file("${projectDir}/config/extensions").toPath())
                into = "/opt/eap/extensions" // somewhere as jib will change folder rights to root and jboss cannot handle this
            }
            path {
                setFrom(file("$buildDir/opentelemetry-javaagent").toPath())
                into = "/"
            }
        }
        permissions = mapOf(
            "/docker-entrypoint.sh" to "755",
            "/opt/eap/extensions/postconfigure.sh" to "755",
            "/opt/eap/extensions/console.sh" to "755",
        )
    }
}

tasks.jib {
    dependsOn(":ear:ear")
}
tasks.jibBuildTar {
    dependsOn(":ear:ear")
}
tasks.jibDockerBuild {
    dependsOn(":ear:ear")
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