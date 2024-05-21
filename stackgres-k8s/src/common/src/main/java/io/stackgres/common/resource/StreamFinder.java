/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.common.crd.sgstream.StackGresStreamList;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class StreamFinder
    extends AbstractCustomResourceFinder<StackGresStream> {

  /**
   * Create a {@code StreamFinder} instance.
   */
  @Inject
  public StreamFinder(KubernetesClient client) {
    super(client, StackGresStream.class, StackGresStreamList.class);
  }

}
