package org.dermuedejoe.spring;

import java.util.HashMap;
import java.util.Map;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapSetter;

public class OpenTelemetryTraceContextHelper {

   private static final TextMapSetter<Map<String, String>> SETTER = new MapTextMapSetter();

   /**
    * Returns the current trace context as map.
    */
   public static Map<String, String> obtainCurrentTraceContext() {
      Map<String, String> result = new HashMap<>();

      Context context = Context.current();
      GlobalOpenTelemetry.get()
            .getPropagators()
            .getTextMapPropagator()
            .inject(context, result, SETTER);

      return result;
   }

   /**
    * Returns the current baggage as map.
    */
   public static Map<String, String> obtainCurrentBaggage() {
      Map<String, String> result = new HashMap<>();
      Baggage.current().asMap().forEach((key, value) -> result.put(key, value.getValue()));
      return result;
   }

   private static class MapTextMapSetter implements TextMapSetter<Map<String, String>> {
      @Override
      public void set(Map<String, String> carrier, String key, String value) {
         carrier.put(key, value);
      }
   }
}
