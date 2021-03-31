/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.KubernetesClientFactory;
import io.stackgres.jobs.app.KubernetesClientProvider;
import org.jetbrains.annotations.NotNull;

@ApplicationScoped
public class PatroniApiMetadataFinderImpl implements PatroniApiMetadataFinder {

  private static final String PATRONI_HOST_FORMAT = "%s-rest.%s.svc.cluster.local";
  private static final String RESTAPI_PASSWORD_KEY = "restapi-password";

  private final KubernetesClientFactory clientProvider;

  @Inject
  public PatroniApiMetadataFinderImpl(KubernetesClientFactory clientProvider) {
    this.clientProvider = clientProvider;
  }

  @Override
  public PatroniApiMetadata findPatroniRestApi(@NotNull String clusterName,
                                               @NotNull String namespace) {
    return ImmutablePatroniApiMetadata.builder()
        .host(getPatroniHost(clusterName, namespace))
        .port(getPatroniPort(clusterName, namespace))
        .username(getPatroniUser())
        .password(getPatroniPassword(clusterName, namespace))
        .build();
  }

  protected String getPatroniPassword(String name, String namespace) {
    try (KubernetesClient client = clientProvider.create()) {
      var secretOpt = Optional.ofNullable(client.secrets()
          .inNamespace(namespace)
          .withName(name)
          .get());

      var secret = secretOpt.orElseThrow(() -> new InvalidCluster(
          "Could not find secret " + name + " in namespace " + namespace));

      if (secret.getData().containsKey(RESTAPI_PASSWORD_KEY)) {
        final String encodedPassword = secret
            .getData().get(RESTAPI_PASSWORD_KEY);

        return new String(Base64.getDecoder().decode(encodedPassword), StandardCharsets.UTF_8);
      } else {
        throw new InvalidCluster("Could not find " + RESTAPI_PASSWORD_KEY + " in secret " + name);
      }

    }
  }

  protected int getPatroniPort(String name, String namespace) {
    String patroniServiceName = name + "-rest";
    try (KubernetesClient client = clientProvider.create()) {
      var service = Optional.ofNullable(client.services()
          .inNamespace(namespace)
          .withName(patroniServiceName)
          .get());

      if (service.isEmpty()) {
        throw new InvalidCluster("Could not find service "
            + name + "-rest in namespace " + namespace);
      }

      return service.get()
          .getSpec().getPorts().stream()
          .filter(servicePort -> servicePort.getName().equals("patroniport"))
          .findFirst()
          .orElseThrow(() -> new InvalidCluster("Could not find patroni port in service "
              + patroniServiceName))
          .getPort();
    }
  }

  protected String getPatroniHost(String name, String namespace) {
    return String.format(PATRONI_HOST_FORMAT, name, namespace);
  }

  protected String getPatroniUser() {
    return "superuser";
  }
}
