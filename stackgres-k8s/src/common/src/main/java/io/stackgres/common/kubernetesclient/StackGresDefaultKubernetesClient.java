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
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.Status;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.base.PatchContext;
import io.fabric8.kubernetes.client.internal.PatchUtils;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.stackgres.common.StackGresKubernetesClient;
import io.stackgres.common.resource.KubernetesClientStatusUpdateException;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StackGresDefaultKubernetesClient extends DefaultKubernetesClient
    implements StackGresKubernetesClient {

  public static final MediaType APPLY_PATCH = MediaType.get("application/apply-patch+yaml");
  private static final Logger LOGGER = LoggerFactory
      .getLogger(StackGresDefaultKubernetesClient.class);
  private static final String SERVER_SIDE_APPLY_DEFAULT_PATH_FORMAT =
      "/api%s/namespaces/%s/%s/%s?fieldManager=%s&force=%b";

  private static final String SERVER_SIDE_APPLY_GROUP_PATH_FORMAT =
      "/apis/%s/namespaces/%s/%s/%s?fieldManager=%s&force=%b";

  @Override
  public <T extends HasMetadata> T serverSideApply(PatchContext patchContext, T intent) {
    intent.getMetadata().setManagedFields(null);

    try {
      var applyUrl = getResourceUrl(patchContext, intent);
      return apply(intent, applyUrl);
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

  private <T extends HasMetadata> T apply(T intent, URL applyUrl) throws IOException {
    LOGGER.trace("Performing Server Side Apply request to the endpoint: {}", applyUrl);

    String content = PatchUtils.patchMapper().valueToTree(intent).toString();
    RequestBody body = RequestBody.create(APPLY_PATCH, content);
    Request request = new Request.Builder()
        .url(applyUrl)
        .patch(body)
        .build();

    OkHttpClient client = getHttpClient();

    final Call call = client.newCall(request);
    return executeRequest(intent, call);
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

}
