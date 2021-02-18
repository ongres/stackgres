/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.workaround;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import io.fabric8.kubernetes.api.model.DeletionPropagation;
import io.fabric8.kubernetes.api.model.DoneableService;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.ListOptions;
import io.fabric8.kubernetes.api.model.RootPaths;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.LocalPortForward;
import io.fabric8.kubernetes.client.OperationInfo;
import io.fabric8.kubernetes.client.PortForward;
import io.fabric8.kubernetes.client.ResourceNotFoundException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.EditReplacePatchDeletable;
import io.fabric8.kubernetes.client.dsl.FilterWatchListDeletable;
import io.fabric8.kubernetes.client.dsl.Gettable;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Replaceable;
import io.fabric8.kubernetes.client.dsl.ServiceResource;
import io.fabric8.kubernetes.client.dsl.Watchable;
import io.fabric8.kubernetes.client.dsl.base.BaseOperation;
import io.fabric8.kubernetes.client.dsl.base.OperationContext;
import okhttp3.OkHttpClient;

//CHECKSTYLE:OFF
public class ServiceOperationsImpl
    extends io.fabric8.kubernetes.client.dsl.internal.core.v1.ServiceOperationsImpl {

  private final HasMetadataOperation hasMetadataOperation;

  public ServiceOperationsImpl(OkHttpClient client, Config config) {
    this(client, config, null);
  }

  public ServiceOperationsImpl(OkHttpClient client, Config config, String namespace) {
    this(new OperationContext().withOkhttpClient(client)
        .withConfig(config).withNamespace(namespace));
  }

  public ServiceOperationsImpl(OperationContext context) {
    super(context.withPlural("services"));
    this.type = Service.class;
    this.listType = ServiceList.class;
    this.doneableType = DoneableService.class;
    this.hasMetadataOperation = new HasMetadataOperation(context);
  }

  @Override
  public ServiceOperationsImpl newInstance(OperationContext context) {
    return new ServiceOperationsImpl(context);
  }

  @Override
  public Service replace(Service item) {
    try {
      if (item.getSpec() == null || item.getSpec().getType() == null
          || !item.getSpec().getType().equals("ExternalName")) {
        Service old = fromServer().get();
        item = new ServiceBuilder(item).editSpec().withClusterIP(old.getSpec().getClusterIP())
            .endSpec().build();
      }
      return hasMetadataOperation.replace(item);
    } catch (Exception e) {
      throw KubernetesClientException.launderThrowable(forOperationType("replace"), e);
    }
  }

  @Override
  public Service patch(Service item) {
    try {
      if (item.getSpec() == null || item.getSpec().getType() == null
          || !item.getSpec().getType().equals("ExternalName")) {
        Service old = fromServer().get();
        item = new ServiceBuilder(item).editSpec().withClusterIP(old.getSpec().getClusterIP())
            .endSpec().build();
      }
      return hasMetadataOperation.patch(item);
    } catch (Exception e) {
      throw KubernetesClientException.launderThrowable(forOperationType("patch"), e);
    }
  }

  @SuppressWarnings("deprecation")
  private class HasMetadataOperation
      extends io.fabric8.kubernetes.client.dsl.base.HasMetadataOperation<
          Service, ServiceList, DoneableService, ServiceResource<Service, DoneableService>>
      implements ServiceResource<Service, DoneableService> {

    private final ServiceOperationsImpl outer;

    public Service create(Service... item) {
      return outer.create(item);
    }

    public DoneableService createOrReplaceWithNew() {
      return outer.createOrReplaceWithNew();
    }

    public ServiceList list() {
      return outer.list();
    }

    public DoneableService createNew() {
      return outer.createNew();
    }

    public FilterWatchListDeletable<Service, ServiceList, Boolean, Watch, Watcher<Service>> withoutLabels(
        Map<String, String> labels) {
      return outer.withoutLabels(labels);
    }

    public FilterWatchListDeletable<Service, ServiceList, Boolean, Watch, Watcher<Service>> withLabelIn(
        String key, String... values) {
      return outer.withLabelIn(key, values);
    }

    public FilterWatchListDeletable<Service, ServiceList, Boolean, Watch, Watcher<Service>> withLabelNotIn(
        String key, String... values) {
      return outer.withLabelNotIn(key, values);
    }

    public FilterWatchListDeletable<Service, ServiceList, Boolean, Watch, Watcher<Service>> withoutFields(
        Map<String, String> fields) {
      return outer.withoutFields(fields);
    }

    public DoneableService edit() {
      return outer.edit();
    }

    public ServiceOperationsImpl newInstance(OperationContext context) {
      return outer.newInstance(context);
    }

    public int hashCode() {
      return outer.hashCode();
    }

    public Service waitUntilReady(long amount, TimeUnit timeUnit) throws InterruptedException {
      return outer.waitUntilReady(amount, timeUnit);
    }

    public String getURL(String portName) {
      return outer.getURL(portName);
    }

    public String getAPIGroup() {
      return outer.getAPIGroup();
    }

    public String getAPIVersion() {
      return outer.getAPIVersion();
    }

    public String getNamespace() {
      return outer.getNamespace();
    }

    public String getName() {
      return outer.getName();
    }

    public URL getRootUrl() {
      return outer.getRootUrl();
    }

    public boolean equals(Object obj) {
      return outer.equals(obj);
    }

    public URL getNamespacedUrl(String namespace) throws MalformedURLException {
      return outer.getNamespacedUrl(namespace);
    }

    public URL getNamespacedUrl() throws MalformedURLException {
      return outer.getNamespacedUrl();
    }

    public <T> URL getNamespacedUrl(T item) throws MalformedURLException {
      return outer.getNamespacedUrl(item);
    }

    public PortForward portForward(int port, ReadableByteChannel in, WritableByteChannel out) {
      return outer.portForward(port, in, out);
    }

    public URL getResourceUrl(String namespace, String name) throws MalformedURLException {
      return outer.getResourceUrl(namespace, name);
    }

    public LocalPortForward portForward(int port, int localPort) {
      return outer.portForward(port, localPort);
    }

    public URL getResourceUrl() throws MalformedURLException {
      return outer.getResourceUrl();
    }

    public LocalPortForward portForward(int port) {
      return outer.portForward(port);
    }

    public Service get() {
      return outer.get();
    }

    public Service require() throws ResourceNotFoundException {
      return outer.require();
    }

    public Service getMandatory() {
      return outer.getMandatory();
    }

    public RootPaths getRootPaths() {
      return outer.getRootPaths();
    }

    public String toString() {
      return outer.toString();
    }

    public ServiceResource<Service, DoneableService> withName(String name) {
      return outer.withName(name);
    }

    public Replaceable<Service, Service> lockResourceVersion(String resourceVersion) {
      return outer.lockResourceVersion(resourceVersion);
    }

    public NonNamespaceOperation<Service, ServiceList, DoneableService, ServiceResource<Service, DoneableService>> inNamespace(
        String namespace) {
      return outer.inNamespace(namespace);
    }

    public NonNamespaceOperation<Service, ServiceList, DoneableService, ServiceResource<Service, DoneableService>> inAnyNamespace() {
      return outer.inAnyNamespace();
    }

    public EditReplacePatchDeletable<Service, Service, DoneableService, Boolean> cascading(
        boolean cascading) {
      return outer.cascading(cascading);
    }

    public ServiceResource<Service, DoneableService> load(InputStream is) {
      return outer.load(is);
    }

    public ServiceResource<Service, DoneableService> load(URL url) {
      return outer.load(url);
    }

    public ServiceResource<Service, DoneableService> load(File file) {
      return outer.load(file);
    }

    public ServiceResource<Service, DoneableService> load(String path) {
      return outer.load(path);
    }

    public Gettable<Service> fromServer() {
      return outer.fromServer();
    }

    public Service create(Service resource) {
      return outer.create(resource);
    }

    public Service createOrReplace(Service... items) {
      return outer.createOrReplace(items);
    }

    public FilterWatchListDeletable<Service, ServiceList, Boolean, Watch, Watcher<Service>> withLabels(
        Map<String, String> labels) {
      return outer.withLabels(labels);
    }

    public FilterWatchListDeletable<Service, ServiceList, Boolean, Watch, Watcher<Service>> withLabelSelector(
        LabelSelector selector) {
      return outer.withLabelSelector(selector);
    }

    public FilterWatchListDeletable<Service, ServiceList, Boolean, Watch, Watcher<Service>> withLabel(
        String key, String value) {
      return outer.withLabel(key, value);
    }

    public FilterWatchListDeletable<Service, ServiceList, Boolean, Watch, Watcher<Service>> withLabel(
        String key) {
      return outer.withLabel(key);
    }

    public FilterWatchListDeletable<Service, ServiceList, Boolean, Watch, Watcher<Service>> withoutLabel(
        String key, String value) {
      return outer.withoutLabel(key, value);
    }

    public FilterWatchListDeletable<Service, ServiceList, Boolean, Watch, Watcher<Service>> withoutLabel(
        String key) {
      return outer.withoutLabel(key);
    }

    public FilterWatchListDeletable<Service, ServiceList, Boolean, Watch, Watcher<Service>> withFields(
        Map<String, String> fields) {
      return outer.withFields(fields);
    }

    public FilterWatchListDeletable<Service, ServiceList, Boolean, Watch, Watcher<Service>> withField(
        String key, String value) {
      return outer.withField(key, value);
    }

    public FilterWatchListDeletable<Service, ServiceList, Boolean, Watch, Watcher<Service>> withoutField(
        String key, String value) {
      return outer.withoutField(key, value);
    }

    public String getLabelQueryParam() {
      return outer.getLabelQueryParam();
    }

    public String getFieldQueryParam() {
      return outer.getFieldQueryParam();
    }

    public ServiceList list(Integer limitVal, String continueVal) {
      return outer.list(limitVal, continueVal);
    }

    public ServiceList list(ListOptions listOptions) {
      return outer.list(listOptions);
    }

    public Boolean delete() {
      return outer.delete();
    }

    public Boolean delete(Service... items) {
      return outer.delete(items);
    }

    public Boolean delete(List<Service> items) {
      return outer.delete(items);
    }

    public Service updateStatus(Service item) {
      return outer.updateStatus(item);
    }

    public BaseOperation<Service, ServiceList, DoneableService, ServiceResource<Service, DoneableService>> withItem(
        Service item) {
      return outer.withItem(item);
    }

    public Watchable<Watch, Watcher<Service>> withResourceVersion(String resourceVersion) {
      return outer.withResourceVersion(resourceVersion);
    }

    public Config getConfig() {
      return outer.getConfig();
    }

    public Watch watch(Watcher<Service> watcher) {
      return outer.watch(watcher);
    }

    public Watch watch(String resourceVersion, Watcher<Service> watcher) {
      return outer.watch(resourceVersion, watcher);
    }

    public Watch watch(ListOptions options, Watcher<Service> watcher) {
      return outer.watch(options, watcher);
    }

    public boolean isResourceNamespaced() {
      return outer.isResourceNamespaced();
    }

    public Boolean isCascading() {
      return outer.isCascading();
    }

    public Service getItem() {
      return outer.getItem();
    }

    public String getResourceVersion() {
      return outer.getResourceVersion();
    }

    public Boolean getReloadingFromServer() {
      return outer.getReloadingFromServer();
    }

    public Boolean isReloadingFromServer() {
      return outer.isReloadingFromServer();
    }

    public Long getGracePeriodSeconds() {
      return outer.getGracePeriodSeconds();
    }

    public DeletionPropagation getPropagationPolicy() {
      return outer.getPropagationPolicy();
    }

    public String getResourceT() {
      return outer.getResourceT();
    }

    public Class<Service> getType() {
      return outer.getType();
    }

    public Class<ServiceList> getListType() {
      return outer.getListType();
    }

    public Class<DoneableService> getDoneableType() {
      return outer.getDoneableType();
    }

    public String getKind() {
      return outer.getKind();
    }

    public String getOperationType() {
      return outer.getOperationType();
    }

    public OperationInfo forOperationType(String type) {
      return outer.forOperationType(type);
    }

    public FilterWatchListDeletable<Service, ServiceList, Boolean, Watch, Watcher<Service>> withGracePeriod(
        long gracePeriodSeconds) {
      return outer.withGracePeriod(gracePeriodSeconds);
    }

    public EditReplacePatchDeletable<Service, Service, DoneableService, Boolean> withPropagationPolicy(
        DeletionPropagation propagationPolicy) {
      return outer.withPropagationPolicy(propagationPolicy);
    }

    public BaseOperation<Service, ServiceList, DoneableService, ServiceResource<Service, DoneableService>> withWaitRetryBackoff(
        long initialBackoff, TimeUnit backoffUnit, double backoffMultiplier) {
      return outer.withWaitRetryBackoff(initialBackoff, backoffUnit, backoffMultiplier);
    }

    public String getApiVersion() {
      return outer.getApiVersion();
    }

    public boolean isApiGroup() {
      return outer.isApiGroup();
    }

    public Boolean isReady() {
      return outer.isReady();
    }

    public Service waitUntilCondition(Predicate<Service> condition, long amount, TimeUnit timeUnit)
        throws InterruptedException {
      return outer.waitUntilCondition(condition, amount, timeUnit);
    }

    public void setType(Class<Service> type) {
      outer.setType(type);
    }

    public void setListType(Class<ServiceList> listType) {
      outer.setListType(listType);
    }

    public void setNamespace(String namespace) {
      outer.setNamespace(namespace);
    }

    public HasMetadataOperation(OperationContext ctx) {
      super(ctx);
      this.outer = ServiceOperationsImpl.this;
    }
  }
}
