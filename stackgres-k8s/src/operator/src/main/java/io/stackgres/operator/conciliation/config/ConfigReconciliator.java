/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.stackgres.common.crd.Condition;
import io.stackgres.common.crd.sgconfig.ConfigEventReason;
import io.stackgres.common.crd.sgconfig.ConfigStatusCondition;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgconfig.StackGresConfigStatus;
import io.stackgres.common.event.EventEmitter;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.operator.app.OperatorLockHolder;
import io.stackgres.operator.common.ConfigReview;
import io.stackgres.operator.common.PatchResumer;
import io.stackgres.operator.conciliation.AbstractConciliator;
import io.stackgres.operator.conciliation.AbstractReconciliator;
import io.stackgres.operator.conciliation.DeployedResourcesCache;
import io.stackgres.operator.conciliation.HandlerDelegator;
import io.stackgres.operator.conciliation.ReconciliationResult;
import io.stackgres.operator.conciliation.StatusManager;
import io.stackgres.operatorframework.admissionwebhook.mutating.MutationPipeline;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationPipeline;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.helpers.MessageFormatter;

@ApplicationScoped
public class ConfigReconciliator
    extends AbstractReconciliator<StackGresConfig, ConfigReview> {

  @Dependent
  static class Parameters {
    @Inject MutationPipeline<StackGresConfig, ConfigReview> mutatingPipeline;
    @Inject ValidationPipeline<ConfigReview> validatingPipeline;
    @Inject CustomResourceScanner<StackGresConfig> scanner;
    @Inject CustomResourceFinder<StackGresConfig> finder;
    @Inject AbstractConciliator<StackGresConfig> conciliator;
    @Inject DeployedResourcesCache deployedResourcesCache;
    @Inject HandlerDelegator<StackGresConfig> handlerDelegator;
    @Inject KubernetesClient client;
    @Inject StatusManager<StackGresConfig, Condition> statusManager;
    @Inject EventEmitter<StackGresConfig> eventController;
    @Inject CustomResourceScheduler<StackGresConfig> configScheduler;
    @Inject ObjectMapper objectMapper;
    @Inject OperatorLockHolder operatorLockReconciliator;
  }

  private final StatusManager<StackGresConfig, Condition> statusManager;
  private final EventEmitter<StackGresConfig> eventController;
  private final CustomResourceScheduler<StackGresConfig> configScheduler;
  private final PatchResumer<StackGresConfig> patchResumer;
  private final AtomicReference<Tuple2<String, String>> lastCertificateTuple =
      new AtomicReference<>();
  private final String operatorCertPath;
  private final String operatorCertKeyPath;

  @Inject
  public ConfigReconciliator(Parameters parameters) {
    super(
        parameters.mutatingPipeline, parameters.validatingPipeline,
        parameters.scanner, parameters.finder,
        parameters.conciliator, parameters.deployedResourcesCache,
        parameters.handlerDelegator, parameters.client,
        parameters.objectMapper,
        parameters.operatorLockReconciliator,
        StackGresConfig.KIND);
    this.statusManager = parameters.statusManager;
    this.eventController = parameters.eventController;
    this.configScheduler = parameters.configScheduler;
    this.patchResumer = new PatchResumer<>(parameters.objectMapper);
    this.operatorCertPath = ConfigProvider.getConfig().getValue(
        "quarkus.http.ssl.certificate.files", String.class);
    this.operatorCertKeyPath = ConfigProvider.getConfig().getValue(
        "quarkus.http.ssl.certificate.key-files", String.class);
  }

  @Override
  protected ConfigReview createReview() {
    return new ConfigReview();
  }

  void onStart(@Observes StartupEvent ev) {
    start();
  }

  void onStop(@Observes ShutdownEvent ev) {
    stop();
  }

  @Override
  protected void reconciliationCycle(StackGresConfig configKey, boolean load) {
    super.reconciliationCycle(configKey, load);
  }

  @Override
  protected void onPreReconciliation(StackGresConfig config) {
    try {
      Path operatorCertPath = Paths.get(this.operatorCertPath);
      Path operatorCertKeyPath = Paths.get(this.operatorCertKeyPath);
      String operatorCert = Files.exists(operatorCertPath)
          ? Files.readString(operatorCertPath) : "";
      String operatorCertKey = Files.exists(operatorCertKeyPath)
          ? Files.readString(operatorCertKeyPath) : "";
      var certificateTuple = Tuple.tuple(
          operatorCert,
          operatorCertKey);
      var lastCertificateTuple = this.lastCertificateTuple.get();
      if (lastCertificateTuple != null
          && !Objects.equals(lastCertificateTuple, certificateTuple)) {
        LOGGER.warn("Certificates changed! Proceding to restart...");
        Quarkus.asyncExit(0);
      }
      this.lastCertificateTuple.set(certificateTuple);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  protected void onPostReconciliation(StackGresConfig config) {
    statusManager.refreshCondition(config);

    configScheduler.updateStatus(config,
        (currentConfig) -> {
          if (config.getStatus() != null) {
            if (currentConfig.getStatus() == null) {
              currentConfig.setStatus(new StackGresConfigStatus());
            }
            currentConfig.getStatus().setVersion(config.getStatus().getVersion());
            currentConfig.getStatus().setConditions(config.getStatus().getConditions());
          }
        });
  }

  @Override
  protected void onConfigCreated(StackGresConfig config, ReconciliationResult result) {
    final String resourceChanged = patchResumer.resourceChanged(config, result);
    eventController.sendEvent(ConfigEventReason.CONFIG_CREATED,
        "Config " + config.getMetadata().getNamespace() + "."
            + config.getMetadata().getName() + " created: " + resourceChanged, config);
    statusManager.updateCondition(
        ConfigStatusCondition.FALSE_FAILED.getCondition(), config);
  }

  @Override
  protected void onConfigUpdated(StackGresConfig config, ReconciliationResult result) {
    final String resourceChanged = patchResumer.resourceChanged(config, result);
    eventController.sendEvent(ConfigEventReason.CONFIG_UPDATED,
        "Config " + config.getMetadata().getNamespace() + "."
            + config.getMetadata().getName() + " updated: " + resourceChanged, config);
    statusManager.updateCondition(
        ConfigStatusCondition.FALSE_FAILED.getCondition(), config);
  }

  @Override
  protected void onError(Exception ex, StackGresConfig config) {
    String message = MessageFormatter.arrayFormat(
        "Config reconciliation cycle failed",
        new String[]{
        }).getMessage();
    eventController.sendEvent(ConfigEventReason.CONFIG_ERROR,
        message + ": " + ex.getMessage(), config);
  }

}
