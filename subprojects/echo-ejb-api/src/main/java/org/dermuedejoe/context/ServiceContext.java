package org.dermuedejoe.context;

import java.io.Serializable;
import java.util.Map;

public class ServiceContext implements Serializable {

   private Map<String, String> traceContext;
   private Map<String, String> baggage;

   public Map<String, String> getTraceContext() {
      return traceContext;
   }

   public void setTraceContext(Map<String, String> traceContext) {
      this.traceContext = traceContext;
   }

   public Map<String, String> getBaggage() {
      return baggage;
   }

   public void setBaggage(Map<String, String> baggage) {
      this.baggage = baggage;
   }

   @Override
   public String toString() {
      return "ServiceContext{" +
            "traceContext=" + traceContext +
            ", baggage=" + baggage +
            '}';
   }
}
