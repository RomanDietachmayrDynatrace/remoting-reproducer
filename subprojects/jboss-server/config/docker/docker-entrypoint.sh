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
    echo "-javaagent:/opt/app/bin/opentelemetry-javaagent.jar"
  fi
}


if [[ "$1" = "start" ]]; then
    shift # skip start argument
    echo "Adding USER: user:password to ApplicationRealm"
    /opt/eap/bin/add-user.sh -a user -p password
    cp /asd/ear.ear /deployments
    export ENABLE_JSON_LOGGING=TRUE
    export JAVA_OPTS_APPEND="$(get_opentelemetry_agent)"
    exec /opt/eap/bin/openshift-launch.sh
fi

exec "$@"