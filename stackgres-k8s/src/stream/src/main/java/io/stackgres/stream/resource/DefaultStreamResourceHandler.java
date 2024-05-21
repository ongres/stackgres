/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.stream.resource;

import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.common.labels.LabelFactory;
import io.stackgres.stream.common.StackGresStreamContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DefaultStreamResourceHandler
    extends AbstractStreamResourceHandler {

  private final LabelFactory<StackGresStream> labelFactory;

  @Inject
  public DefaultStreamResourceHandler(
      LabelFactory<StackGresStream> labelFactory) {
    this.labelFactory = labelFactory;
  }

  @Override
  public Stream<HasMetadata> getResources(KubernetesClient client,
      StackGresStreamContext context) {
    return STACKGRES_STREAM_RESOURCE_OPERATIONS.values()
        .stream()
        .flatMap(resourceOperationGetter -> {
          return resourceOperationGetter.apply(client)
              .inNamespace(context.getStream().getMetadata().getNamespace())
              .withLabels(labelFactory.genericLabels(context.getStream()))
              .list()
              .getItems()
              .stream();
        });
  }

}
