/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.kubernetesclient.workaround;

import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.EndpointsList;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.Handlers;
import io.fabric8.kubernetes.client.LocalPortForward;
import io.fabric8.kubernetes.client.PortForward;
import io.fabric8.kubernetes.client.ServiceToURLProvider;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.base.HasMetadataOperation;
import io.fabric8.kubernetes.client.dsl.base.OperationContext;
import io.fabric8.kubernetes.client.dsl.internal.core.v1.PodOperationsImpl;
import io.fabric8.kubernetes.client.utils.URLUtils;
import okhttp3.OkHttpClient;

public class ServiceOperationsImpl
    extends io.fabric8.kubernetes.client.dsl.internal.core.v1.ServiceOperationsImpl {

  public static final String EXTERNAL_NAME = "ExternalName";

  public ServiceOperationsImpl(OkHttpClient client, Config config) {
    super(client, config);
  }

  public ServiceOperationsImpl(OkHttpClient client, Config config, String namespace) {
    super(client, config, namespace);
  }

  public ServiceOperationsImpl(OperationContext context) {
    super(context);
  }

  @Override
  public ServiceOperationsImpl newInstance(OperationContext context) {
    return new ServiceOperationsImpl(context);
  }

  @Override
  public Service waitUntilReady(long amount, TimeUnit timeUnit) {
    long started = System.nanoTime();
    super.waitUntilReady(amount, timeUnit);
    long alreadySpent = System.nanoTime() - started;

    // if awaiting existence took very long, let's give at least 10 seconds to awaiting readiness
    long remaining = Math.max(10_000, timeUnit.toNanos(amount) - alreadySpent);

    HasMetadataOperation<Endpoints, EndpointsList, Resource<Endpoints>> endpointsOperation =
        (HasMetadataOperation<Endpoints, EndpointsList, Resource<Endpoints>>) Handlers
            .getOperation(Endpoints.class, EndpointsList.class, client, config)
            .newInstance(context);
    endpointsOperation.waitUntilReady(remaining, TimeUnit.MILLISECONDS);

    return get();
  }

  public String getURL(String portName) {
    String clusterIP = getMandatory().getSpec().getClusterIP();
    if ("None".equals(clusterIP)) {
      throw new IllegalStateException("Service: " + getMandatory().getMetadata().getName()
          + " in namespace " + namespace + " is head-less. Search for endpoints instead");
    }
    return getUrlHelper(portName);
  }

  private String getUrlHelper(String portName) {
    ServiceLoader<ServiceToURLProvider> urlProvider = ServiceLoader.load(ServiceToURLProvider.class,
        Thread.currentThread().getContextClassLoader());
    Iterator<ServiceToURLProvider> iterator = urlProvider.iterator();
    List<ServiceToURLProvider> servicesList = new ArrayList<>();

    while (iterator.hasNext()) {
      servicesList.add(iterator.next());
    }

    // Sort all loaded implementations according to priority
    Collections.sort(servicesList, new ServiceToUrlSortComparator());
    for (ServiceToURLProvider serviceToUrlProvider : servicesList) {
      String url = serviceToUrlProvider.getURL(getMandatory(), portName, namespace,
          new DefaultKubernetesClient(client, getConfig()));
      if (url != null && URLUtils.isValidURL(url)) {
        return url;
      }
    }

    return null;
  }

  private Pod matchingPod() {
    Service item = requireFromServer();
    Map<String, String> labels = item.getSpec().getSelector();
    PodList list = new PodOperationsImpl(client, config)
        .inNamespace(item.getMetadata().getNamespace()).withLabels(labels).list();
    return list.getItems().stream().findFirst().orElseThrow(
        () -> new IllegalStateException("Could not find matching pod for service:" + item + "."));
  }

  @Override
  public PortForward portForward(int port, ReadableByteChannel in, WritableByteChannel out) {
    Pod m = matchingPod();
    return new PodOperationsImpl(client, config).inNamespace(m.getMetadata().getNamespace())
        .withName(m.getMetadata().getName()).portForward(port, in, out);
  }

  @Override
  public LocalPortForward portForward(int port, int localPort) {
    Pod m = matchingPod();
    return new PodOperationsImpl(client, config).inNamespace(m.getMetadata().getNamespace())
        .withName(m.getMetadata().getName()).portForward(port, localPort);
  }

  @Override
  public LocalPortForward portForward(int port) {
    Pod m = matchingPod();
    return new PodOperationsImpl(client, config).inNamespace(m.getMetadata().getNamespace())
        .withName(m.getMetadata().getName()).portForward(port);
  }

  public class ServiceToUrlSortComparator implements Comparator<ServiceToURLProvider> {
    public int compare(ServiceToURLProvider first, ServiceToURLProvider second) {
      return first.getPriority() - second.getPriority();
    }
  }

  @Override
  protected Service modifyItemForReplaceOrPatch(Supplier<Service> currentSupplier, Service item) {
    if (!isExternalNameService(item)) {
      Service old = currentSupplier.get();
      Service modified = new ServiceBuilder(item).editSpec()
          .withClusterIP(old.getSpec().getClusterIP()).endSpec().build();
      modified.getMetadata().setManagedFields(item.getMetadata().getManagedFields());
      return modified;
    }
    return item;
  }

  private boolean isExternalNameService(Service item) {
    if (item.getSpec() != null && item.getSpec().getType() != null) {
      return item.getSpec().getType().equals(EXTERNAL_NAME);
    }
    return false;
  }

}
