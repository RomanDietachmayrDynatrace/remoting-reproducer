# Remoting Example

The projects contain 2 server applications:

* JBoss EAP 7.3.9
* Spring Boot Application

The implementation used is based on the `http`.
The `remote+http` (remoting with http upgrade) only serves as an alternate route to call but is not of relevance. 

The context propagation is currently achieved by adding the information into the payload as method argument.
As an alternative this could also be done in the EJB Context itself

![Architecture](docs/images/simple.png?raw=true "Generator")

## Project Structure

```shell
├── config
│    └── docker        # docker-entrypoint.sh
├── kubernetes         # deployment example in kubernetes
└── subprojects
    ├── ear            # Builds the ear to deploy
    ├── echo-ejb       # Bean Impl
    ├── echo-ejb-api   # API
    ├── jboss-server   # JBoss Container builder
    └── spring-server  # Spring Server and Docker Container

```

### JBoss EAP

The JBoss application contains one service.

```java
public interface EchoServiceEjbApi {
   
   /**
    * Returns always "pong".
    */
   String ping();

   /**
    * Returns always "pong " + serviceContext.toString().
    */
   String ping(ServiceContext serviceContext);
}
```

The implementation will always simple return the String `pong`.
In case of transporting the `ServiceContext` as argument, the `serviceContext.toString()` will also be added ad convenience.

#### Context propagation

The context propagation between the Spring Application and the JBoss is achieved by

- Intercepting the client and adding the context to the EJBContext 
- Adding the Context into the payload as method parameter

On the JBoss server, a ServerInterceptor will extract the ServiceContext either from the EJBContext map or from the first method parameter.
With the information a new OpenTelemetry Context will be created, containing the context and the baggage.

![Context Propagation](docs/images/contextpropagation.png?raw=true "Generator")

### Spring Boot Application

The Spring Boot Application exposes 2 Rest Endpoints simply doing remote calls to the JBoss Application server.

```shell
# Does a remote call to the JBoss Application server using http:
# http://jboss-service:8080/wildfly-services
/ping-http
# Same but transporting the payload as method argument
/ping-http-pl

# Does a remote call to the JBoss Application server using remote+http:
# remote+http://jboss-service:8080
/ping-remote-http
# Same but transporting the payload as method argument
/ping-remote-http-pl
```

Allowing to be called by a simple curl:

```shell
curl http://<host>:<port>/ping-http
curl http://<host>:<port>/ping-http-pl

curl http://<host>:<port>/ping-remote-http
curl http://<host>:<port>/ping-remote-http-pl
```

## Open telemetry agent

By default, the Opentelemetry autoinstumentation is enabled.
This can be disabled by passing the environment variable: `DISABLE_OTEL_AGENT=anyvalue`

## Build and Run

### Build

```shell
./gradlew clean build jibDockerBuild
```

Two docker containers are build.
```shell
test/spring-server:1.0.0
test/jboss-server:1.0.0
```


### Run

#### Plain Docker

```shell
# Create some network
> docker network create service

# Run the JBoss Application
> docker container run -it --rm \
    --network service \
    --name jboss-service \
    test/jboss-server:1.0.0
  
# Run Spring boot application
> docker container run -it --rm \
    --network service \
    --name spring-service \
    -p 8080:8080 \
    test/spring-server:1.0.0
    
    
# Disable the Otel agent add:
-e DISABLE_OTEL_AGENT=any
```

#### Kubernetes

See the `kubernetes` directory for a basic yaml

```shell
kubectl apply -f kubernetes/deployments.yaml
```

## Jaeger example

A complete example using Jaeger as backend not using the Otel Collector.
Additionally, a [jeager all-in-one](https://www.jaegertracing.io/docs/1.29/getting-started/#all-in-one) container is used for quick local testing.
We need to set the [OpenTelemetry Resource](https://github.com/open-telemetry/opentelemetry-java/blob/main/sdk-extensions/autoconfigure/README.md#opentelemetry-resource) for each application.

```shell
# Run the jaeger all in one image:
> docker container run -it --rm --name jaeger-all-in-one \
    --network service \
    --name jaeger-service \
    -e COLLECTOR_ZIPKIN_HTTP_PORT=14250 \
    -p 16686:16686 \
    jaegertracing/all-in-one:1.29.0

# Run the JBoss Application
docker container run -it --rm \
    --network service \
    --name spring-service \
    -p 8081:8080 \
    -e OTEL_TRACES_EXPORTER=jaeger \
    -e OTEL_EXPORTER_JAEGER_ENDPOINT=http://jaeger-service:14250 \
    -e OTEL_RESOURCE_ATTRIBUTES=service.name=spring-service \
    test/spring-server:1.0.0

# Run Spring boot application
docker container run -it --rm \
    --network service \
    --name jboss-service \
    -e OTEL_TRACES_EXPORTER=jaeger \
    -e OTEL_EXPORTER_JAEGER_ENDPOINT=http://jaeger-service:14250 \
    -e OTEL_RESOURCE_ATTRIBUTES=service.name=jboss-service \
    test/jboss-server:1.0.0
```

Running could look like the following image.

- left upper: Jaeger
- right upper: curls calls
- middle: JBoss
- down: spring
- Browser: 2 calls to the rest endpoint 

![Jeager Example](docs/images/jaeger.png?raw=true "Generator")