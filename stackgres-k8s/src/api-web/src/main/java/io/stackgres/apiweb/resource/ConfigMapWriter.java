/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.resource;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Namespaceable;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.resource.AbstractResourceWriter;

@ApplicationScoped
public class ConfigMapWriter extends AbstractResourceWriter<
    ConfigMap, ConfigMapList> {

  @Inject
  public ConfigMapWriter(KubernetesClient client) {
    super(client);
  }

  public ConfigMapWriter() {
    super(null);
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy();
  }

  @Override
  protected Namespaceable
      <NonNamespaceOperation<
          ConfigMap,
          ConfigMapList,
          Resource<ConfigMap>>> getResourceEndpoints(KubernetesClient client) {
    return client.configMaps();
  }

}
