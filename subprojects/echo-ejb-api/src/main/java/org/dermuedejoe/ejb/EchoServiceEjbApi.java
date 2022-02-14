package org.dermuedejoe.ejb;

import org.dermuedejoe.context.ServiceContext;

public interface EchoServiceEjbApi {

   String ping();

   String ping(ServiceContext serviceContext);
}
