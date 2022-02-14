package org.dermuedejoe.ejb.interceptor;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.baggage.BaggageBuilder;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import org.dermuedejoe.context.ServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;
import java.util.Map;
import java.util.Optional;

public class OpenTelemetryEjbServerInterceptor {

   private static final Logger LOG = LoggerFactory.getLogger(OpenTelemetryEjbServerInterceptor.class);

   private static final Tracer TRACER = GlobalOpenTelemetry.getTracer("syrius-ejb-instrumentation");
   private static final TextMapGetter<Map<String, String>> GETTER = new MapTextMapGetter();

   @AroundInvoke
   public Object wrap(InvocationContext ctx) throws Exception {

      Optional<ServiceContext> serviceContextOptional = getServiceContext(ctx);

      LOG.info("Service Context: {}", serviceContextOptional);

      // read trace-context and baggage from service-context and set them within the OpenTelemetry context
      Context context = serviceContextOptional
            .map(OpenTelemetryEjbServerInterceptor::createContextFromParams)
            .orElse(Context.current());

      try (Scope autoClosable = context.makeCurrent()) {
         // see semantic conventions for span attributes
         // https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/trace/semantic_conventions/rpc.md
         String service = ctx.getMethod().getDeclaringClass().getName();
         String method = ctx.getMethod().getName();
         String name = service + "/" + method;

         Span span = TRACER.spanBuilder(name)
               .setSpanKind(SpanKind.SERVER)
               .setAttribute(SemanticAttributes.RPC_SYSTEM, "ejb")
               .setAttribute(SemanticAttributes.RPC_SERVICE, service)
               .setAttribute(SemanticAttributes.RPC_METHOD, method)
               .startSpan();

         try (Scope autoClosable2 = span.makeCurrent()) {
            return ctx.proceed();
         } catch (Exception exc) {
            span.setStatus(StatusCode.ERROR);
            span.recordException(exc);
            throw exc;
         } finally {
            span.end();
         }
      }
   }

   private static Context createContextFromParams(ServiceContext serviceContext) {
      Map<String, String> traceContextMap = serviceContext.getTraceContext();
      Map<String, String> baggageMap = serviceContext.getBaggage();

      Context context = Context.current();

      // create new context with the manually-propagated trace-context included
      Context contextWithTrace = GlobalOpenTelemetry.get()
            .getPropagators()
            .getTextMapPropagator()
            .extract(context, traceContextMap, GETTER);

      BaggageBuilder baggageBuilder = Baggage.builder();
      if (baggageMap != null) {
         baggageMap.forEach(baggageBuilder::put);
      }
      Baggage baggage = baggageBuilder.build();

      // create & return new context with baggage
      return contextWithTrace.with(baggage);
   }

   public static Optional<ServiceContext> getServiceContext(InvocationContext ctx) {
      return getServiceContextFromContextData(ctx).or(() -> getServiceContextFromFirstParam(ctx));

   }

   /**
    * Returns the {@link ServiceContext} from the {@link InvocationContext}s ContextData.
    */
   private static Optional<ServiceContext> getServiceContextFromContextData(InvocationContext ctx) {
      Map<String, Object> contextData = ctx.getContextData();
      Object service_context = contextData.get("SERVICE_CONTEXT");
      return Optional.ofNullable((ServiceContext) service_context);
   }

   /**
    * Returns the {@link ServiceContext} from the {@link InvocationContext} methods parameter.
    */
   private static Optional<ServiceContext> getServiceContextFromFirstParam(InvocationContext ctx) {
      if (ctx.getParameters().length > 0 && ctx.getParameters()[0] instanceof ServiceContext) {
         return Optional.ofNullable((ServiceContext) ctx.getParameters()[0]);
      }
      return Optional.empty();
   }

   private static class MapTextMapGetter implements TextMapGetter<Map<String, String>> {

      @Override
      public String get(Map<String, String> carrier, String key) {
         if (carrier != null) {
            return carrier.get(key);
         }
         return null;
      }

      @Override
      public Iterable<String> keys(Map<String, String> carrier) {
         return carrier.keySet();
      }
   }

}
