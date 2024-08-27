/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.config;

import java.util.Map;
import java.util.Optional;

import io.stackgres.common.WebUtil;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgconfig.StackGresConfigGrafana;
import io.stackgres.common.crd.sgconfig.StackGresConfigSpec;
import io.stackgres.common.crd.sgconfig.StackGresConfigStatus;
import io.stackgres.common.crd.sgconfig.StackGresConfigStatusGrafana;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ConfigGrafanaIntegrationChecker {

  protected static final Logger LOGGER = LoggerFactory
      .getLogger(ConfigGrafanaIntegrationChecker.class);

  public boolean isGrafanaEmbedded(StackGresConfig config) {
    return Optional.of(config.getSpec())
        .map(StackGresConfigSpec::getGrafana)
        .map(StackGresConfigGrafana::getAutoEmbed)
        .orElse(false)
        && Optional.ofNullable(config.getStatus())
        .map(StackGresConfigStatus::getGrafana)
        .map(StackGresConfigStatusGrafana::getConfigHash)
        .map(String.valueOf(config.getSpec().getGrafana().hashCode())::equals)
        .orElse(false)
        && Optional.ofNullable(config.getStatus())
        .map(StackGresConfigStatus::getGrafana)
        .map(grafana -> grafana.getUrls() != null
          && !grafana.getUrls().isEmpty()
          && grafana.getToken() != null)
        .orElse(false);
  }

  public boolean isGrafanaIntegrated(StackGresConfig config) {
    return isGrafanaEmbedded(config)
        && Optional.ofNullable(config.getStatus())
        .map(StackGresConfigStatus::getGrafana)
        .map(grafana -> grafana.getUrls().stream()
            .allMatch(url -> {
              if (url.indexOf(":") >= 0) {
                var check = WebUtil.checkUnsecureUri(url.substring(url.indexOf(":") + 1),
                    Map.of("Authorization", "Bearer " + grafana.getToken()));
                if (check.isEmpty()) {
                  return true;
                }
                LOGGER.warn("Check failed for URL {}", url, check.get());
              }
              return false;
            }))
        .orElse(false);
  }

}
