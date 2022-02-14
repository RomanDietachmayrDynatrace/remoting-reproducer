package org.dermuedejoe.ejb.impl;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.dermuedejoe.context.ServiceContext;
import org.dermuedejoe.ejb.EchoServiceEjbApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Remote;
import javax.ejb.Stateless;

@Stateless
@Remote(EchoServiceEjbApi.class)
public class EchoServiceBean implements EchoServiceEjbApi {

   private static final Logger LOG = LoggerFactory.getLogger(EchoServiceEjbApi.class);

   private static final Tracer TRACER = GlobalOpenTelemetry.getTracer("syrius-ejb-instrumentation");

   @Override
   public String ping() {
      LOG.info("ping called");

      Span span = TRACER.spanBuilder("server-ping")
              .setSpanKind(SpanKind.SERVER)
              .startSpan();

      try (Scope scope = span.makeCurrent()) {

      } catch (Throwable t) {
         span.setStatus(StatusCode.ERROR, t.getMessage());
      } finally {
         span.end();
      }


      return "pong";
   }

   @Override
   public String ping(ServiceContext serviceContext) {
      LOG.info("ping with payload called");

      Span span = TRACER.spanBuilder("server-ping-pl")
              .setSpanKind(SpanKind.SERVER)
              .startSpan();

      try (Scope scope = span.makeCurrent()) {

      } catch (Throwable t) {
         span.setStatus(StatusCode.ERROR, t.getMessage());
      } finally {
         span.end();
      }


      return "pong " + serviceContext;
   }
}
