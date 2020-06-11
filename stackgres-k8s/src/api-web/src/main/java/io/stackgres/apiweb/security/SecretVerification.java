/*
 * Copyright (C) 2020 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.security;

import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.google.common.base.Strings;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.security.AuthenticationFailedException;
import io.stackgres.common.KubernetesClientFactory;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.resource.ResourceUtil;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class SecretVerification {

  @ConfigProperty(name = "stackgres.restapiNamespace")
  String namespace;

  private final KubernetesClientFactory clientFactory;

  @Inject
  public SecretVerification(KubernetesClientFactory clientFactory) {
    super();
    this.clientFactory = clientFactory;
  }

  /**
   * Get the K8s username if the api Username and password match.
   */
  public String verifyCredentials(String apiUsername, String password) {
    Objects.requireNonNull(apiUsername, "apiUsername");
    Objects.requireNonNull(password, "password");
    try (KubernetesClient client = clientFactory.create()) {
      Optional<Secret> user = client.secrets()
          .inNamespace(namespace)
          .withLabel("api.stackgres.io/auth", "user")
          .list().getItems().stream()
          .filter(s -> s.getMetadata().getName().startsWith("stackgres-api-"))
          .filter(s -> Optional.ofNullable(s.getData().get(StackGresContext.REST_APIUSER_KEY))
              .map(ResourceUtil::decodeSecret)
              .map(apiUsername::equals)
              .orElse(Optional.ofNullable(s.getData().get(StackGresContext.REST_K8SUSER_KEY))
                  .map(ResourceUtil::decodeSecret)
                  .map(apiUsername::equals)
                  .orElse(Boolean.FALSE)))
          .filter(s -> !Strings.isNullOrEmpty(s.getData().get(StackGresContext.REST_PASSWORD_KEY)))
          .findFirst();

      if (user.isPresent()) {
        char[] bcryptHash = getStoredPassword(user.get()).toCharArray();
        BCrypt.Result verify = BCrypt.verifyer().verify(password.toCharArray(), bcryptHash);
        if (verify.verified) {
          return getK8sUsername(user.get());
        }
      }
      throw new AuthenticationFailedException();
    }
  }

  private String getStoredPassword(Secret secret) {
    return decodeKey(secret, StackGresContext.REST_PASSWORD_KEY);
  }

  private String getK8sUsername(Secret secret) {
    return decodeKey(secret, StackGresContext.REST_K8SUSER_KEY);
  }

  private String decodeKey(Secret secret, String key) {
    return ResourceUtil.decodeSecret(secret.getData().get(key));
  }

}
