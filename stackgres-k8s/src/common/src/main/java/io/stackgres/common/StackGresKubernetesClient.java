/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Status;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.base.PatchContext;
import io.fabric8.kubernetes.client.utils.Serialization;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StackGresKubernetesClient extends DefaultKubernetesClient {

  private static final Logger LOGGER = LoggerFactory.getLogger(StackGresKubernetesClient.class);

  public static final MediaType APPLY_PATCH
      = MediaType.get("application/apply-patch+yaml");

  private static final String SERVER_SIDE_APPLY_DEFAULT_PATH_FORMAT =
      "/api%s/namespaces/%s/%s/%s?fieldManager=%s&force=%b";

  private static final String SERVER_SIDE_APPLY_GROUP_PATH_FORMAT =
      "/apis/%s/namespaces/%s/%s/%s?fieldManager=%s&force=%b";

  @SuppressWarnings("unchecked")
  public <T extends HasMetadata> T serverSideApply(PatchContext patchContext, T intent) {

    intent.getMetadata().setManagedFields(null);

    try {
      var applyUrl = getResourceUrl(patchContext, intent);

      return apply(intent, applyUrl);

    } catch (IOException e) {
      throw new RuntimeException(e);
    }

  }

  private <T extends HasMetadata> T apply(T intent, URL applyUrl) throws IOException {
    String content = Serialization.jsonMapper().valueToTree(intent).toString();

    LOGGER.trace("Performing Server Side Apply request to the endpoint: {}",
        applyUrl.toString());

    RequestBody body = RequestBody.create(APPLY_PATCH, content);

    Request request = new Request.Builder()
        .url(applyUrl)
        .patch(body)
        .build();

    OkHttpClient client = getHttpClient();

    final Call call = client.newCall(request);
    return executeRequest(intent, call);
  }

  private <T extends HasMetadata> T executeRequest(T intent, Call call) throws IOException {

    Response response = null;
    /*
      Response is Closeable, so it can be managed in the try, but for some reason spotbugs detects
      it as a redundant null check
     */

    try {
      response = call.execute();
      String responseString = response.body().string();
      if (response.isSuccessful()) {
        return (T) Serialization.unmarshal(responseString, intent.getClass());
      } else {
        var status = Serialization.unmarshal(responseString, Status.class);
        throw new KubernetesClientException(status);
      }
    } finally {
      if (response != null) {
        response.close();
      }
    }
  }

  protected  <T extends HasMetadata> URL getResourceUrl(PatchContext patchContext, T intent)
      throws MalformedURLException {
    var plural = HasMetadata.getPlural(intent.getClass());

    var name = intent.getMetadata().getName();

    var namespace = Optional.ofNullable(intent.getMetadata().getNamespace())
        .orElseGet(() -> Optional.ofNullable(getNamespace())
            .orElse("default"));
    var masterUrl = getMasterUrl();

    var group = Optional.ofNullable(HasMetadata.getGroup(intent.getClass()))
        .filter(s -> !s.isEmpty());

    var apiVersion = HasMetadata.getApiVersion(intent.getClass());

    final String fieldManager = patchContext.getFieldManager();
    Objects.requireNonNull(fieldManager);
    final Boolean force = Optional.ofNullable(patchContext.getForce())
        .orElse(false);

    if (group.isPresent()) {
      final String resourcePath = String.format(SERVER_SIDE_APPLY_GROUP_PATH_FORMAT,
          apiVersion, namespace, plural, name, fieldManager, force);
      return new URL(masterUrl, resourcePath);
    } else {
      final String format = String.format(SERVER_SIDE_APPLY_DEFAULT_PATH_FORMAT,
          apiVersion, namespace, plural, name, fieldManager, force);
      return new URL(masterUrl, format);
    }

  }

}
