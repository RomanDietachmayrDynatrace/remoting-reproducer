#!/bin/bash
set -e

export OTEL_TRACES_EXPORTER=${OTEL_TRACES_EXPORTER:-logging}
export OTEL_METRICS_EXPORTER=${OTEL_METRICS_EXPORTER:-none}

# =======================================================================
# Returns the javaagent instrumentation option or an empty string.
# If the DISABLE_OTEL_AGENT is set to anything, the agent is disabled.
#
function get_opentelemetry_agent() {
  if [ -n "${DISABLE_OTEL_AGENT:-}" ]; then
    echo ""
  else
    echo " -javaagent:/opt/app/bin/opentelemetry-javaagent.jar"
  fi
}


if [[ "$1" = "start" ]]; then
    shift # skip start argument
    main_class=" org.dermuedejoe.spring.SpringBootApplication"
    java_options=$(get_opentelemetry_agent)

    exec java ${java_options} \
             -cp @/app/jib-classpath-file \
             ${java_options} \
             ${main_class} \
             ${JAVA_ARGS} \
             "$@"
fi

exec "$@"