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
import io.stackgres.common.patroni.StackGresPasswordKeys;

@ApplicationScoped
public class PatroniApiMetadataFinderImpl implements PatroniApiMetadataFinder {

  private static final String PATRONI_HOST_FORMAT = "%s-rest.%s";
  private static final String RESTAPI_PASSWORD_KEY = "restapi-password";

  private final KubernetesClient client;

  @Inject
  public PatroniApiMetadataFinderImpl(KubernetesClient client) {
    this.client = client;
  }

  @Override
  public PatroniApiMetadata findPatroniRestApi(String clusterName, String namespace) {
    return ImmutablePatroniApiMetadata.builder()
        .host(getPatroniHost(clusterName, namespace))
        .port(getPatroniPort(clusterName, namespace))
        .username(getPatroniUser())
        .password(getPatroniPassword(clusterName, namespace))
        .build();
  }

  protected String getPatroniPassword(String name, String namespace) {
    var secretOpt = Optional.ofNullable(client.secrets()
        .inNamespace(namespace)
        .withName(name)
        .get());

    var secret = secretOpt.orElseThrow(() -> new InvalidClusterException(
        "Could not find secret " + name + " in namespace " + namespace));

    if (secret.getData().containsKey(RESTAPI_PASSWORD_KEY)) {
      final String encodedPassword = secret
          .getData().get(RESTAPI_PASSWORD_KEY);

      return new String(Base64.getDecoder().decode(encodedPassword), StandardCharsets.UTF_8);
    } else {
      throw new InvalidClusterException(
          "Could not find " + RESTAPI_PASSWORD_KEY + " in secret " + name);
    }
  }

  protected int getPatroniPort(String name, String namespace) {
    String patroniServiceName = name + "-rest";
    var service = Optional.ofNullable(client.services()
        .inNamespace(namespace)
        .withName(patroniServiceName)
        .get());

    if (service.isEmpty()) {
      throw new InvalidClusterException("Could not find service "
          + name + "-rest in namespace " + namespace);
    }

    return service.get()
        .getSpec().getPorts().stream()
        .filter(servicePort -> servicePort.getName().equals("patroniport"))
        .findFirst()
        .orElseThrow(() -> new InvalidClusterException("Could not find patroni port in service "
            + patroniServiceName))
        .getPort();
  }

  protected String getPatroniHost(String name, String namespace) {
    return String.format(PATRONI_HOST_FORMAT, name, namespace);
  }

  protected String getPatroniUser() {
    return StackGresPasswordKeys.RESTAPI_USERNAME;
  }
}
