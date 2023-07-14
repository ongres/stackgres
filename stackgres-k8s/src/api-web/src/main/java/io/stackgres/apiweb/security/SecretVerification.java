/*
 * Copyright (C) 2020 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.security;

import java.util.Objects;
import java.util.Optional;

import com.google.common.base.Strings;
import io.fabric8.kubernetes.api.model.Secret;
import io.quarkus.security.AuthenticationFailedException;
import io.stackgres.apiweb.config.WebApiProperty;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.resource.ResourceScanner;
import io.stackgres.common.resource.ResourceUtil;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class SecretVerification {

  private ResourceScanner<Secret> secretScanner;
  private String namespace;

  @PostConstruct
  public void init() {
    this.namespace = WebApiProperty.RESTAPI_NAMESPACE.getString();
  }

  /**
   * Get the K8s username if the api Username and password match.
   */
  public String verifyCredentials(String apiUsername, String password) {
    Objects.requireNonNull(apiUsername, StackGresContext.REST_APIUSER_KEY);
    Objects.requireNonNull(password, StackGresContext.REST_PASSWORD_KEY);
    String passwordHash = TokenUtils.sha256(apiUsername + password);
    return secretScanner.findResourcesInNamespace(namespace)
        .stream()
        .filter(s -> s.getMetadata().getLabels() != null)
        .filter(s -> Objects.equals(s.getMetadata().getLabels()
            .get(StackGresContext.AUTH_KEY), StackGresContext.AUTH_USER_VALUE))
        .filter(s -> !Strings.isNullOrEmpty(s.getData().get(StackGresContext.REST_K8SUSER_KEY)))
        .filter(s -> !Strings.isNullOrEmpty(s.getData().get(StackGresContext.REST_PASSWORD_KEY)))
        .filter(s -> Optional.ofNullable(s.getData().get(StackGresContext.REST_APIUSER_KEY))
            .map(ResourceUtil::decodeSecret)
            .map(apiUsername::equals)
            .orElse(Optional.of(s.getData().get(StackGresContext.REST_K8SUSER_KEY))
                .map(ResourceUtil::decodeSecret)
                .map(apiUsername::equals)
                .orElse(Boolean.FALSE)))
        .filter(s -> Objects.equals(passwordHash, getStoredPassword(s)))
        .map(this::getK8sUsername)
        .findFirst()
        .orElseThrow(AuthenticationFailedException::new);
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

  @Inject
  public void setSecretScanner(ResourceScanner<Secret> secretScanner) {
    this.secretScanner = secretScanner;
  }
}
