package org.dermuedejoe.spring;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.dermuedejoe.context.ServiceContext;
import org.dermuedejoe.ejb.EchoServiceEjbApi;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.ejb.client.EJBClientInterceptor;
import org.jboss.ejb.client.EJBClientInvocationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.naming.NamingException;
import java.util.Map;

import static org.dermuedejoe.spring.Remoting.*;

@RestController
public class EchoController {

   private static Logger LOG = LoggerFactory.getLogger(EchoController.class);

   private static final String applicationName = "ear";
   private static final String distinctName = "";
   private static final String moduleName = "echo-ejb";

   private static final Tracer TRACER = GlobalOpenTelemetry.getTracer("syrius-ejb-instrumentation");

   private static final StatelessRemoteBeanDefinition<EchoServiceEjbApi> BEAN_DEFINITION = StatelessRemoteBeanDefinition.build(
         new ApplicationDefinition(
               applicationName,
               moduleName,
               distinctName),
         "EchoServiceBean",
         EchoServiceEjbApi.class
   );


   @GetMapping("/ping-http")
   public String pingHttp() {
      return ping(() -> remoteWithHttpLookup(BEAN_DEFINITION));
   }

   @GetMapping("/ping-remote-http")
   public String pingRemotingUpgrade() {
      return ping(() -> remoteWithHttpUpgradeLookup(BEAN_DEFINITION));
   }

   private static void setContextWithInterceptor() {
      EJBClientContext ctxWithInterceptors = EJBClientContext.getCurrent()
            .withAddedInterceptors(new ClientInterceptor());

      EJBClientContext.getContextManager().setThreadDefault(ctxWithInterceptors);
   }

   private static String ping(ServiceSupplier<EchoServiceEjbApi> serviceSupplier) {

      Span span = TRACER.spanBuilder("client-ping").startSpan();

      try (Scope scope = span.makeCurrent()) {

         LOG.info("Got request for ping.");

         try {
            setContextWithInterceptor();
            LOG.info("Lookup remote service");
            EchoServiceEjbApi echoService = serviceSupplier.get();
            LOG.info("Call remote service");
            String ping = echoService.ping();
            LOG.info("Got response from remote service: {}", ping);
            return ping;
         } catch (NamingException e) {
            LOG.error("Failed to call remote service", e);
         }

      } catch (Throwable t) {
         span.setStatus(StatusCode.ERROR, t.getMessage());
      } finally {
         span.end();
      }

      return "FAILURE";
   }

   @GetMapping("/ping-http-pl")
   public String pingHttpWithPayload() {
      return pingWithPayload(() -> remoteWithHttpLookup(BEAN_DEFINITION));
   }

   @GetMapping("/ping-remote-http-pl")
   public String pingRemotingUpgradeWithPayload() {
      return pingWithPayload(() -> remoteWithHttpUpgradeLookup(BEAN_DEFINITION));
   }

   private static String pingWithPayload(ServiceSupplier<EchoServiceEjbApi> serviceSupplier) {

      Span span = TRACER.spanBuilder("client-ping-pl").startSpan();

      try (Scope scope = span.makeCurrent()) {

         LOG.info("Got request for ping.");

         try {
            LOG.info("Lookup remote service");
            EchoServiceEjbApi echoService = serviceSupplier.get();
            LOG.info("Call remote service");
            String ping = echoService.ping(getServiceContext());
            LOG.info("Got response from remote service: {}", ping);
            return ping;
         } catch (NamingException e) {
            LOG.error("Failed to call remote service", e);
         }

      } catch (Throwable t) {
         span.setStatus(StatusCode.ERROR, t.getMessage());
      } finally {
         span.end();
      }

      return "FAILURE";
   }


   @FunctionalInterface
   public static interface ServiceSupplier<T> {
      T get() throws NamingException;
   }

   public static ServiceContext getServiceContext() {
      ServiceContext serviceContext = new ServiceContext();
      serviceContext.setTraceContext(OpenTelemetryTraceContextHelper.obtainCurrentTraceContext());
      serviceContext.setBaggage(OpenTelemetryTraceContextHelper.obtainCurrentBaggage());
      LOG.info("Created SerciceContext: {}", serviceContext);
      return serviceContext;
   }

   public static class ClientInterceptor implements EJBClientInterceptor {

      public void handleInvocation(EJBClientInvocationContext context) throws Exception {
         Map<String, Object> contextData = context.getContextData();
         contextData.put("SERVICE_CONTEXT", getServiceContext());
         context.sendRequest();
      }

      public Object handleInvocationResult(EJBClientInvocationContext context) throws Exception {
         return context.getResult();
      }
   }
}
