/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.kubernetesclient;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.SecretList;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.api.model.Status;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1beta1.CronJob;
import io.fabric8.kubernetes.api.model.rbac.Role;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.Handlers;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.ServiceResource;
import io.fabric8.kubernetes.client.dsl.base.PatchContext;
import io.fabric8.kubernetes.client.internal.PatchUtils;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresKubernetesClient;
import io.stackgres.common.kubernetesclient.workaround.SecretOperationsImpl;
import io.stackgres.common.kubernetesclient.workaround.ServiceOperationsImpl;
import io.stackgres.common.prometheus.ServiceMonitor;
import io.stackgres.common.prometheus.ServiceMonitorList;
import io.stackgres.common.resource.KubernetesClientStatusUpdateException;
import io.stackgres.common.resource.ResourceWriter;
import io.stackgres.operatorframework.resource.ResourceUtil;
import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StackGresDefaultKubernetesClient extends DefaultKubernetesClient
    implements StackGresKubernetesClient {

  static {
    Handlers.register(Secret.class, SecretOperationsImpl::new);
  }

  public static final MediaType APPLY_PATCH = MediaType.get("application/apply-patch+yaml");
  private static final Logger LOGGER = LoggerFactory
      .getLogger(StackGresDefaultKubernetesClient.class);
  private static final String SERVER_SIDE_APPLY_DEFAULT_PATH_FORMAT =
      "/api%s/namespaces/%s/%s/%s?fieldManager=%s&force=%b";

  private static final String SERVER_SIDE_APPLY_GROUP_PATH_FORMAT =
      "/apis/%s/namespaces/%s/%s/%s?fieldManager=%s&force=%b";

  private static final String LIST_DEFAULT_PATH_FORMAT =
      "/api%s/namespaces/%s/%s";

  private static final String LIST_GROUP_PATH_FORMAT =
      "/apis/%s/namespaces/%s/%s";

  private static final String LIST_ANY_DEFAULT_PATH_FORMAT =
      "/api%s/%s";

  private static final String LIST_ANY_GROUP_PATH_FORMAT =
      "/apis/%s/%s";

  private static final String OPEN_API =
      "/openapi/v2";

  private static final Object CACHE_KEY = new Object();
  private static final Cache<Object, ObjectNode> OPEN_API_CACHE = CacheBuilder.newBuilder()
      .expireAfterWrite(Duration.ofMinutes(1))
      .build();

  public StackGresDefaultKubernetesClient() {
    super();
  }

  public StackGresDefaultKubernetesClient(String masterUrl) {
    super(masterUrl);
  }

  public StackGresDefaultKubernetesClient(Config config) {
    super(config);
  }

  public StackGresDefaultKubernetesClient(OkHttpClient httpClient, Config config) {
    super(httpClient, config);
  }

  @Override
  public <T extends HasMetadata> T serverSideApply(@NotNull PatchContext patchContext, T intent,
      Optional<T> deployed) {
    try {
      var applyUrl = getResourceUrl(patchContext, intent);
      return apply(intent, deployed, applyUrl);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public <T extends HasMetadata> List<T> findManagedIntents(Class<T> resource, String fieldManager,
      Map<String, String> labels, String namespace) {
    try {
      var listUrl = getResourceListUrl(resource, namespace);
      var listHttpUrl = buildHttpUrl(listUrl, labels);
      return list(listHttpUrl, resource, fieldManager);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public <T extends HasMetadata> List<T> findManagedIntents(Class<T> resource, String fieldManager,
      Map<String, String> labels) {
    try {
      var listUrl = getResourceListUrl(resource);
      var listHttpUrl = buildHttpUrl(listUrl, labels);
      return list(listHttpUrl, resource, fieldManager);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends HasMetadata, S, L extends KubernetesResourceList<T>> T updateStatus(
      @NotNull Class<T> resourceClass, @NotNull Class<L> resourceListClass, @NotNull T resource,
      @NotNull Function<T, S> statusGetter, @NotNull BiConsumer<T, S> statusSetter) {
    try {
      T resourceOverwrite = resources(resourceClass, resourceListClass)
          .inNamespace(resource.getMetadata().getNamespace())
          .withName(resource.getMetadata().getName())
          .get();
      if (resourceOverwrite == null) {
        throw new RuntimeException("Can not update status of resource "
            + HasMetadata.getKind(resourceClass)
            + "." + HasMetadata.getGroup(resourceClass)
            + " " + resource.getMetadata().getNamespace()
            + "." + resource.getMetadata().getName()
            + ": resource not found");
      }
      statusSetter.accept(resourceOverwrite, statusGetter.apply(resource));
      var replaceDeleteable = resources(resourceClass, resourceListClass)
          .inNamespace(resource.getMetadata().getNamespace())
          .withName(resource.getMetadata().getName())
          .lockResourceVersion(resource.getMetadata().getResourceVersion());
      Method replaceMethod = replaceDeleteable.getClass().getSuperclass()
          .getDeclaredMethod("replace", HasMetadata.class, boolean.class);
      AccessController.doPrivileged((PrivilegedAction<?>) () -> {
        replaceMethod.setAccessible(true);
        return null;
      });
      return (T) replaceMethod.invoke(replaceDeleteable, resourceOverwrite, true);
    } catch (KubernetesClientException ex) {
      throw new KubernetesClientStatusUpdateException(ex);
    } catch (InvocationTargetException ex) {
      Throwable targetEx = ex.getTargetException();
      if (targetEx instanceof KubernetesClientException) {
        throw new KubernetesClientStatusUpdateException(
            (KubernetesClientException) targetEx);
      } else if (targetEx instanceof RuntimeException) {
        throw (RuntimeException) targetEx;
      } else if (targetEx instanceof Error) {
        throw (Error) targetEx;
      }
      throw new RuntimeException(targetEx);
    } catch (RuntimeException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  @SuppressWarnings("unchecked")
  private <T extends HasMetadata> T apply(T intent, Optional<T> deployed, URL applyUrl)
      throws IOException {
    LOGGER.trace("Performing Server Side Apply request to the endpoint: {}", applyUrl);

    deployed.filter(this::resourceHasUnmanagedFields)
        .ifPresent(this::ownResourceUnmanagedFields);
    setManagedByServerSideApplyAnnotation(intent);

    intent.getMetadata().setManagedFields(null);

    final T modifiedIntent;
    if (intent instanceof Secret) {
      Secret secret = (Secret) intent;
      if (secret.getStringData() != null && !secret.getStringData().isEmpty()) {
        LOGGER.trace("Convert stringData to data for secret {}.{}",
            secret.getMetadata().getNamespace(), secret.getMetadata().getName());
        modifiedIntent = (T) new SecretBuilder(secret)
            .withData(secret.getStringData().entrySet().stream()
                .map(entry -> Tuple.tuple(
                    entry.getKey(), ResourceUtil.encodeSecret(entry.getValue())))
                .collect(Collectors.toMap(Tuple2::v1, Tuple2::v2)))
            .withStringData(null)
            .build();
      } else {
        modifiedIntent = intent;
      }
    } else {
      modifiedIntent = intent;
    }

    String content = PatchUtils.patchMapper().valueToTree(modifiedIntent).toString();
    RequestBody body = RequestBody.create(APPLY_PATCH, content);
    Request request = new Request.Builder()
        .url(applyUrl)
        .patch(body)
        .build();

    OkHttpClient client = getHttpClient();

    final Call call = client.newCall(request);
    return executeRequest(modifiedIntent, call);
  }

  private <T extends HasMetadata> List<T> list(HttpUrl listUrl, Class<T> resource,
      String fieldManager) throws IOException {
    LOGGER.trace("Performing list request to the endpoint: {}", listUrl);
    Request request = new Request.Builder()
        .url(listUrl)
        .get()
        .build();

    OkHttpClient client = getHttpClient();

    final Call call = client.newCall(request);
    return executeListRequest(call, resource, fieldManager);

  }

  @SuppressWarnings("unchecked")
  private <T extends HasMetadata> T executeRequest(T intent, Call call) throws IOException {
    try (Response response = call.execute()) {

      String responseString = response.body().string();
      if (response.isSuccessful()) {
        return (T) Serialization.unmarshal(responseString, intent.getClass());
      } else {
        var status = Serialization.unmarshal(responseString, Status.class);
        throw new KubernetesClientException(status);
      }
    }
  }

  private <T extends HasMetadata> List<T> executeListRequest(
      Call call,
      Class<T> format,
      String fieldManager) throws IOException {
    try (Response response = call.execute()) {

      String responseString = response.body().string();

      if (response.isSuccessful()) {

        ObjectNode list = (ObjectNode) Serialization.jsonMapper().readTree(responseString);
        return parseListObject(list, format, fieldManager);
      } else {
        if (response.code() == 404) {
          return List.of();
        }
        try {
          var status = Serialization.unmarshal(responseString, Status.class);
          throw new KubernetesClientException(status);
        } catch (Exception ex) {
          throw new KubernetesClientException(responseString);
        }
      }
    }
  }

  protected <T extends HasMetadata> List<T> parseListObject(
      ObjectNode listObject,
      Class<T> format,
      String fieldManager) throws JsonProcessingException {

    if (listObject.has("items")) {
      var result = new ArrayList<T>();
      ArrayNode data = (ArrayNode) listObject.get("items");
      for (JsonNode item : data) {
        final ObjectNode itemObject = (ObjectNode) item;
        if (!itemObject.has("kind")) {
          itemObject.set("kind", new TextNode(HasMetadata.getKind(format)));
        }
        ObjectNode filteredItem;
        if (ManagedFieldsReader.hasManagedFieldsConfiguration(itemObject, fieldManager)) {
          filteredItem = ManagedFieldsReader.readManagedFields(itemObject, fieldManager);
          ((ObjectNode) filteredItem.get("metadata")).set("managedFields",
              itemObject.get("metadata").get("managedFields"));
        } else {
          filteredItem = itemObject;
        }
        result.add(
            Serialization.jsonMapper().treeToValue(filteredItem, format)
        );
      }
      return result;

    } else {
      throw new IllegalArgumentException("not a list object, received "
          + listObject);
    }
  }

  protected <T extends HasMetadata> URL getResourceUrl(PatchContext patchContext, T intent)
      throws MalformedURLException {
    var plural = HasMetadata.getPlural(intent.getClass());

    var name = intent.getMetadata().getName();

    var namespace = Optional.ofNullable(intent.getMetadata().getNamespace())
        .orElseGet(() -> Optional.ofNullable(getNamespace()).orElse("default"));

    var masterUrl = getMasterUrl();

    var group = Optional.ofNullable(HasMetadata.getGroup(intent.getClass()))
        .filter(s -> !s.isEmpty());

    var apiVersion = HasMetadata.getApiVersion(intent.getClass());

    final String fieldManager =
        Objects.requireNonNull(patchContext.getFieldManager(), "fieldManager");
    final Boolean force = Optional.ofNullable(patchContext.getForce())
        .orElse(Boolean.FALSE);

    if (group.isPresent()) {
      final String groupPath = String.format(SERVER_SIDE_APPLY_GROUP_PATH_FORMAT,
          apiVersion, namespace, plural, name, fieldManager, force);
      return new URL(masterUrl, groupPath);
    } else {
      final String defaultPath = String.format(SERVER_SIDE_APPLY_DEFAULT_PATH_FORMAT,
          apiVersion, namespace, plural, name, fieldManager, force);
      return new URL(masterUrl, defaultPath);
    }
  }

  protected <T extends HasMetadata> URL getResourceListUrl(Class<T> resource, String namespace)
      throws MalformedURLException {
    var plural = HasMetadata.getPlural(resource);

    var masterUrl = getMasterUrl();

    var group = Optional.ofNullable(HasMetadata.getGroup(resource))
        .filter(s -> !s.isEmpty());

    var apiVersion = HasMetadata.getApiVersion(resource);

    if (group.isPresent()) {
      final String groupPath = String.format(LIST_GROUP_PATH_FORMAT,
          apiVersion, namespace, plural);
      return new URL(masterUrl, groupPath);
    } else {
      final String defaultPath = String.format(LIST_DEFAULT_PATH_FORMAT,
          apiVersion, namespace, plural);
      return new URL(masterUrl, defaultPath);
    }
  }

  protected <T extends HasMetadata> URL getResourceListUrl(Class<T> resource)
      throws MalformedURLException {
    var plural = HasMetadata.getPlural(resource);

    var masterUrl = getMasterUrl();

    var group = Optional.ofNullable(HasMetadata.getGroup(resource))
        .filter(s -> !s.isEmpty());

    var apiVersion = HasMetadata.getApiVersion(resource);

    if (group.isPresent()) {
      final String groupPath = String.format(LIST_ANY_GROUP_PATH_FORMAT,
          apiVersion, plural);
      return new URL(masterUrl, groupPath);
    } else {
      final String defaultPath = String.format(LIST_ANY_DEFAULT_PATH_FORMAT,
          apiVersion, plural);
      return new URL(masterUrl, defaultPath);
    }
  }

  protected HttpUrl buildHttpUrl(@NotNull URL url, @NotNull Map<String, String> labels) {
    final HttpUrl.Builder builder = HttpUrl.get(url).newBuilder();

    String labelSelector = getLabelSelectorQueryParam(labels);

    builder.addQueryParameter("labelSelector", labelSelector);

    return builder.build();
  }

  protected String getLabelSelectorQueryParam(Map<String, String> labels) {
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<String, String> entry : labels.entrySet()) {
      if (sb.length() > 0) {
        sb.append(",");
      }
      if (entry.getValue() != null) {
        sb.append(entry.getKey()).append("=").append(entry.getValue());
      } else {
        sb.append(entry.getKey());
      }
    }
    return sb.toString();
  }

  @Override
  public ObjectNode getOpenApi() {
    try {
      var masterUrl = getMasterUrl();
      final URL opeApiUrl = new URL(masterUrl, OPEN_API);
      final HttpUrl openApiHttpUrl = HttpUrl.get(opeApiUrl);

      LOGGER.trace("Performing openApi request to the endpoint: {}", OPEN_API);

      Request request = new Request.Builder()
          .url(openApiHttpUrl)
          .get()
          .build();

      OkHttpClient client = getHttpClient();

      final Call call = client.newCall(request);
      try (Response response = call.execute()) {
        String responseString = response.body().string();
        if (response.isSuccessful()) {
          return (ObjectNode) Serialization.jsonMapper().readTree(responseString);
        } else {
          throw new KubernetesClientException(responseString);
        }
      }
    } catch (RuntimeException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  private boolean resourceHasUnmanagedFields(HasMetadata deployedResource) {
    return Optional.ofNullable(deployedResource)
        .map(HasMetadata::getMetadata)
        .map(ObjectMeta::getAnnotations)
        .stream()
        .map(Map::keySet)
        .flatMap(Set::stream)
        .noneMatch(StackGresContext.MANAGED_BY_SERVER_SIDE_APPLY_KEY::equals);
  }

  private HasMetadata ownResourceUnmanagedFields(HasMetadata unmanagedResource) {
    LOGGER.info("Owning resource {}.{} of kind {}",
        unmanagedResource.getMetadata().getNamespace(),
        unmanagedResource.getMetadata().getName(),
        unmanagedResource.getKind());
    HasMetadata sanitizedUnmanagedResource =
        sanitizeForServerSideApply(unmanagedResource);
    sanitizedUnmanagedResource.getMetadata().setManagedFields(null);
    getResourceOperation(this, unmanagedResource)
        .inNamespace(unmanagedResource.getMetadata().getNamespace())
        .withName(unmanagedResource.getMetadata().getName())
        .lockResourceVersion(unmanagedResource.getMetadata().getResourceVersion())
        .replace(sanitizedUnmanagedResource);
    HasMetadata ownedResourceWithBfa =
        serverSideApply(new PatchContext.Builder()
            .withFieldManager(ResourceWriter.STACKGRES_FIELD_MANAGER)
            .withForce(true)
            .build(), sanitizedUnmanagedResource, Optional.empty());
    Optional.ofNullable(ownedResourceWithBfa)
        .map(HasMetadata::getMetadata)
        .map(ObjectMeta::getManagedFields)
        .stream()
        .flatMap(List::stream)
        .filter(managedFieldsEntry -> Objects.equals(
            managedFieldsEntry.getManager(), "before-first-apply"))
        .collect(Collectors.toList())
        .forEach(ownedResourceWithBfa.getMetadata().getManagedFields()::remove);
    HasMetadata ownedResource = getResourceOperation(this, ownedResourceWithBfa)
        .inNamespace(ownedResourceWithBfa.getMetadata().getNamespace())
        .withName(ownedResourceWithBfa.getMetadata().getName())
        .lockResourceVersion(ownedResourceWithBfa.getMetadata().getResourceVersion())
        .replace(ownedResourceWithBfa);
    return ownedResource;
  }

  @Override
  public void setManagedByServerSideApplyAnnotation(HasMetadata intent) {
    if (Optional.ofNullable(intent.getMetadata().getAnnotations())
        .stream()
        .map(Map::keySet)
        .flatMap(Set::stream)
        .noneMatch(StackGresContext.MANAGED_BY_SERVER_SIDE_APPLY_KEY::equals)) {
      if (intent.getMetadata().getAnnotations() == null) {
        intent.getMetadata().setAnnotations(ImmutableMap.of());
      }
      intent.getMetadata().setAnnotations(ImmutableMap.<String, String>builder()
          .putAll(intent.getMetadata().getAnnotations())
          .put(StackGresContext.MANAGED_BY_SERVER_SIDE_APPLY_KEY, StackGresContext.RIGHT_VALUE)
          .build());
    }
  }

  @Override
  public <T extends HasMetadata> T sanitizeForServerSideApply(T intent) {
    try {
      return new ServerSideApplySanitizer(
          OPEN_API_CACHE.get(CACHE_KEY, () -> getOpenApi()))
          .sanitize(intent);
    } catch (ExecutionException ex) {
      throw new RuntimeException(ex);
    }
  }

  @SuppressWarnings("unchecked")
  private <M extends HasMetadata> MixedOperation<M, ? extends KubernetesResourceList<M>,
      ? extends Resource<M>> getResourceOperation(
      @NotNull KubernetesClient client, @NotNull M resource) {
    return (MixedOperation<M, ? extends KubernetesResourceList<M>, ? extends Resource<M>>) Optional
        .ofNullable(getResourceOperations(resource))
        .map(function -> function.apply(client))
        .orElseThrow(() -> new RuntimeException("Resource of type " + resource.getKind()
            + " is not configured"));
  }

  private <M extends HasMetadata> Function<KubernetesClient, MixedOperation<? extends HasMetadata,
      ? extends KubernetesResourceList<? extends HasMetadata>,
      ? extends Resource<? extends HasMetadata>>> getResourceOperations(M resource) {
    return RESOURCE_OPERATIONS.get(resource.getClass());
  }

  static final ImmutableMap<
      Class<? extends HasMetadata>,
      Function<
          KubernetesClient,
          MixedOperation<
              ? extends HasMetadata,
              ? extends KubernetesResourceList<? extends HasMetadata>,
              ? extends Resource<? extends HasMetadata>>>>
      RESOURCE_OPERATIONS =
      ImmutableMap.<Class<? extends HasMetadata>, Function<KubernetesClient,
          MixedOperation<? extends HasMetadata,
              ? extends KubernetesResourceList<? extends HasMetadata>,
              ? extends Resource<? extends HasMetadata>>>>builder()
          .put(StatefulSet.class, client -> client.apps().statefulSets())
          .put(Service.class, KubernetesClient::services)
          .put(ServiceAccount.class, KubernetesClient::serviceAccounts)
          .put(Role.class, client -> client.rbac().roles())
          .put(RoleBinding.class, client -> client.rbac().roleBindings())
          .put(Secret.class, KubernetesClient::secrets)
          .put(ConfigMap.class, KubernetesClient::configMaps)
          .put(Endpoints.class, KubernetesClient::endpoints)
          .put(CronJob.class, client -> client.batch().v1beta1().cronjobs())
          .put(Pod.class, KubernetesClient::pods)
          .put(Job.class, client -> client.batch().v1().jobs())
          .put(ServiceMonitor.class, client -> client
              .resources(ServiceMonitor.class, ServiceMonitorList.class))
          .build();

  @Override
  public MixedOperation<Service, ServiceList, ServiceResource<Service>> services() {
    return new ServiceOperationsImpl(httpClient, getConfiguration());
  }

  @Override
  public MixedOperation<Secret, SecretList, Resource<Secret>> secrets() {
    return new SecretOperationsImpl(httpClient, getConfiguration());
  }

}
