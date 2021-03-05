/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.workaround;

import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.base.OperationContext;
import okhttp3.OkHttpClient;

public class ServiceOperationsImpl
    extends io.fabric8.kubernetes.client.dsl.internal.core.v1.ServiceOperationsImpl {

  public static final String EXTERNAL_NAME = "ExternalName";

  public ServiceOperationsImpl(OkHttpClient client, Config config) {
    this(client, config, null);
  }

  public ServiceOperationsImpl(OkHttpClient client, Config config, String namespace) {
    this(new OperationContext().withOkhttpClient(client).withConfig(config).withNamespace(namespace)
        .withPropagationPolicy(DEFAULT_PROPAGATION_POLICY));
  }

  public ServiceOperationsImpl(OperationContext context) {
    super(context.withPlural("services"));
    this.type = Service.class;
    this.listType = ServiceList.class;
  }

  @Override
  public ServiceOperationsImpl newInstance(OperationContext context) {
    return new ServiceOperationsImpl(context);
  }

  @Override
  public Service patch(Service item) {
    return patchService(clusterIpIntoServiceAndPatch(item));
  }

  private Service clusterIpIntoServiceAndPatch(Service item) {
    if (!isExternalNameService(item)) {
      try {
        Service old = getMandatory();
        return new ServiceBuilder(item)
            .editSpec()
            .withClusterIP(old.getSpec().getClusterIP())
            .endSpec()
            .build();
      } catch (Exception e) {
        throw KubernetesClientException.launderThrowable(forOperationType("patch"), e);
      }
    }
    return item;
  }

  private boolean isExternalNameService(Service item) {
    if (item != null && item.getSpec() != null && item.getSpec().getType() != null) {
      return item.getSpec().getType().equals(EXTERNAL_NAME);
    }
    return false;
  }

  public Service patchService(Service item) {
    Exception caught = null;
    int maxTries = 10;
    for (int i = 0; i < maxTries; i++) {
      try {
        String resourceVersion;
        final Service got = fromServer().get();
        if (got == null) {
          return null;
        }
        if (got.getMetadata() != null) {
          resourceVersion = got.getMetadata().getResourceVersion();
        } else {
          resourceVersion = null;
        }
        final UnaryOperator<Service> visitor = resource -> {
          try {
            resource.getMetadata().setResourceVersion(resourceVersion);
            return handlePatch(got, resource);
          } catch (Exception e) {
            throw KubernetesClientException.launderThrowable(forOperationType("patch"), e);
          }
        };
        return visitor.apply(item);
      } catch (KubernetesClientException e) {
        caught = e;
        // Only retry if there's a conflict - this is normally to do with resource version & server
        // updates.
        if (e.getCode() != 409) {
          break;
        }
        if (i < maxTries - 1) {
          try {
            TimeUnit.SECONDS.sleep(1);
          } catch (InterruptedException e1) {
            // Ignore this... would only hide the proper exception
            // ...but make sure to preserve the interrupted status
            Thread.currentThread().interrupt();
          }
        }
      } catch (Exception e) {
        caught = e;
      }
    }
    throw KubernetesClientException.launderThrowable(forOperationType("patch"), caught);
  }

}
