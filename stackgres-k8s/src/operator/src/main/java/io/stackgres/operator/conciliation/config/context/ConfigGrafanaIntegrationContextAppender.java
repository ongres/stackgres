/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.config.context;

import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.stackgres.common.JobUtil;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operator.conciliation.ContextAppender;
import io.stackgres.operator.conciliation.config.StackGresConfigContext.Builder;
import io.stackgres.operator.conciliation.factory.config.webconsole.WebConsoleGrafanaIntegrationJob;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ConfigGrafanaIntegrationContextAppender
    extends ContextAppender<StackGresConfig, Builder> {

  private final ConfigGrafanaIntegrationChecker grafanaIntegrationChecker;

  private final ResourceFinder<Job> jobFinder;

  public ConfigGrafanaIntegrationContextAppender(
      ConfigGrafanaIntegrationChecker grafanaIntegrationChecker,
      ResourceFinder<Job> jobFinder) {
    this.grafanaIntegrationChecker = grafanaIntegrationChecker;
    this.jobFinder = jobFinder;
  }

  public void appendContext(StackGresConfig config, Builder contextBuilder) {
    boolean isGrafanaEmbedded = grafanaIntegrationChecker.isGrafanaEmbedded(config);
    boolean isGrafanaIntegrated = grafanaIntegrationChecker.isGrafanaIntegrated(config);
    boolean isGrafanaIntegrationJobFailed = JobUtil.hasJobFailed(
        jobFinder.findByNameAndNamespace(
            WebConsoleGrafanaIntegrationJob.name(config),
            config.getMetadata().getNamespace()))
        .orElse(false);
    contextBuilder
        .isGrafanaEmbedded(isGrafanaEmbedded)
        .isGrafanaIntegrated(isGrafanaIntegrated)
        .isGrafanaIntegrationJobFailed(isGrafanaIntegrationJobFailed);
  }

}
