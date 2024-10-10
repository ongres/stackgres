/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.docir;

import java.net.URI;
import java.util.Optional;

import io.stackgres.common.OperatorProperty;
import io.stackgres.common.crd.sgconfig.StackGresConfigRepository;

/**
 * Resolution of the StackGres docir REST API URL configured in {@code SGConfig.spec.repository.url}.
 *
 * <p>When the {@code DOCIR_REPOSITORY_URL} environment variable (or the
 * {@code stackgres.docirRepositoryUrl} system property) is explicitly set it overrides the URL
 * configured in SGConfig. The Helm chart leaves it unset (so SGConfig is used), but other
 * installation methods such as the OLM bundle Subscription may set it to keep control over the
 * repository URL.</p>
 */
public interface DocirConfigUtil {

  String DEFAULT_REPOSITORY_URL = "https://sgcr.dev";

  static String getRepositoryUrl(StackGresConfigRepository repository) {
    return getRepositoryUrlOverride()
        .or(() -> Optional.ofNullable(repository)
            .map(StackGresConfigRepository::getUrl))
        .orElse(DEFAULT_REPOSITORY_URL);
  }

  static URI getRepositoryUri(StackGresConfigRepository repository) {
    return URI.create(getRepositoryUrl(repository));
  }

  /**
   * Whether only the images published in the docir repository are used (the default). Set the
   * {@code USE_PUBLISHED_IMAGES} environment variable (or the {@code stackgres.usePublishedImages}
   * system property) to {@code false} to also use the images not yet published (for instance to
   * test them before publication).
   */
  static boolean isUsePublishedImages() {
    return getOverride(OperatorProperty.USE_PUBLISHED_IMAGES)
        .map(value -> !value.equalsIgnoreCase("false"))
        .orElse(true);
  }

  /**
   * The application properties default is deliberately ignored so that an unset override falls
   * back to SGConfig.
   */
  private static Optional<String> getRepositoryUrlOverride() {
    return getOverride(OperatorProperty.DOCIR_REPOSITORY_URL);
  }

  private static Optional<String> getOverride(OperatorProperty property) {
    return Optional
        .ofNullable(System.getProperty(property.getPropertyName()))
        .or(() -> Optional.ofNullable(System.getenv(property.getEnvironmentVariableName())))
        .filter(value -> !value.isBlank());
  }

}
