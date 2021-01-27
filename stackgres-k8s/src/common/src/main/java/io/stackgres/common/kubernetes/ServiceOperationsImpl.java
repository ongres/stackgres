/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.kubernetes;

import io.fabric8.kubernetes.api.model.DoneableService;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.base.OperationContext;
import okhttp3.OkHttpClient;

public class ServiceOperationsImpl extends io.fabric8.kubernetes.client.dsl.internal.ServiceOperationsImpl {

  public ServiceOperationsImpl(OkHttpClient client, Config config) {
    this(client, config, null);
  }

  public ServiceOperationsImpl(OkHttpClient client, Config config, String namespace) {
    this(new OperationContext().withOkhttpClient(client).withConfig(config).withNamespace(namespace));
  }

  public ServiceOperationsImpl(OperationContext context) {
    super(context.withPlural("services"));
    this.type = Service.class;
    this.listType = ServiceList.class;
    this.doneableType = DoneableService.class;
  }

  @Override
  public ServiceOperationsImpl newInstance(OperationContext context) {
    return new ServiceOperationsImpl(context);
  }

  @Override
  public Service replace(Service item) {
      try {
        if (item.getSpec() == null
            || item.getSpec().getType() == null
            || !item.getSpec().getType().equals("ExternalName")) {
          Service old = fromServer().get();
          item = new ServiceBuilder(item)
              .editSpec()
              .withClusterIP(old.getSpec().getClusterIP())
              .endSpec()
              .build();
        }
        return super.replace(item);
      } catch (Exception e) {
        throw KubernetesClientException.launderThrowable(forOperationType("replace"), e);
      }
  }

  @Override
  public Service patch(Service item) {
      try {
        if (item.getSpec() == null
            || item.getSpec().getType() == null
            || !item.getSpec().getType().equals("ExternalName")) {
          Service old = fromServer().get();
          item = new ServiceBuilder(item)
              .editSpec()
              .withClusterIP(old.getSpec().getClusterIP())
              .endSpec()
              .build();
        }
        return super.patch(item);
      } catch (Exception e) {
        throw KubernetesClientException.launderThrowable(forOperationType("patch"), e);
      }
  }

}
