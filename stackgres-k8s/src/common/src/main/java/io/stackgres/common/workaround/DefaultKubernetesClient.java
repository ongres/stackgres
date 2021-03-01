/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.workaround;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.ServiceResource;
import okhttp3.OkHttpClient;

public class DefaultKubernetesClient extends io.fabric8.kubernetes.client.DefaultKubernetesClient {

  public DefaultKubernetesClient() {
    super();
  }

  public DefaultKubernetesClient(String masterUrl) {
    super(masterUrl);
  }

  public DefaultKubernetesClient(Config config) {
    super(config);
  }

  public DefaultKubernetesClient(OkHttpClient httpClient, Config config) {
    super(httpClient, config);
  }

  @Override
  public MixedOperation<Service, ServiceList, ServiceResource<Service>> services() {
    return new ServiceOperationsImpl(httpClient, getConfiguration());
  }

}
