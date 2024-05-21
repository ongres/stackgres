/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.stream;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.operator.conciliation.AbstractReconciliationHandler;
import io.stackgres.operator.conciliation.ReconciliationScope;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ReconciliationScope(value = StackGresStream.class, kind = "HasMetadata")
@ApplicationScoped
public class StreamDefaultReconciliationHandler
    extends AbstractReconciliationHandler<StackGresStream> {

  @Inject
  public StreamDefaultReconciliationHandler(KubernetesClient client) {
    super(client);
  }

}
