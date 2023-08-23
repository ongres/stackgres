/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.app;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretVolumeSource;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgconfig.StackGresConfigCert;
import io.stackgres.common.crd.sgconfig.StackGresConfigCertManager;
import io.stackgres.common.crd.sgconfig.StackGresConfigSpec;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceWriter;
import io.stackgres.operator.conciliation.config.ImmutableStackGresConfigContext;
import io.stackgres.operator.conciliation.config.StackGresConfigContext;
import io.stackgres.operator.conciliation.factory.config.OperatorSecret;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class CertificateInstaller {

  private static final Logger LOGGER = LoggerFactory.getLogger(CertificateInstaller.class);

  String operatorName = OperatorProperty.OPERATOR_NAME.getString();

  String operatorNamespace = OperatorProperty.OPERATOR_NAMESPACE.getString();

  private final CustomResourceFinder<StackGresConfig> configFinder;
  private final ResourceFinder<Secret> secretFinder;
  private final ResourceWriter<Secret> secretWriter;
  private final ResourceFinder<Pod> podFinder;
  private final OperatorSecret operatorSecret;
  private final String operatorCertPath;
  private final String operatorCertKeyPath;

  @Inject
  public CertificateInstaller(
      CustomResourceFinder<StackGresConfig> configFinder,
      ResourceFinder<Secret> secretFinder,
      ResourceWriter<Secret> secretWriter,
      ResourceFinder<Pod> podFinder,
      @Any OperatorSecret operatorSecret) {
    this.configFinder = configFinder;
    this.secretFinder = secretFinder;
    this.secretWriter = secretWriter;
    this.podFinder = podFinder;
    this.operatorSecret = operatorSecret;
    this.operatorCertPath = ConfigProvider.getConfig().getValue(
        "quarkus.http.ssl.certificate.files", String.class);
    this.operatorCertKeyPath = ConfigProvider.getConfig().getValue(
        "quarkus.http.ssl.certificate.key-files", String.class);
  }

  public void installOrUpdateCertificate() {
    var config = configFinder.findByNameAndNamespace(operatorName, operatorNamespace)
        .orElseThrow(() -> new IllegalArgumentException(
            "SGConfig " + operatorNamespace + "." + operatorName + " was not found"));
    if (!Optional.of(config.getSpec())
        .map(StackGresConfigSpec::getCert)
        .map(StackGresConfigCert::getCreateForOperator)
        .orElse(true)
        || Optional.of(config.getSpec())
        .map(StackGresConfigSpec::getCert)
        .map(StackGresConfigCert::getCertManager)
        .map(StackGresConfigCertManager::getAutoConfigure)
        .orElse(false)) {
      LOGGER.info("Skipping creation of operator certificate");
      return;
    }
    String certSecretName = OperatorSecret.name(config);
    var certSecretFound = secretFinder.findByNameAndNamespace(certSecretName, operatorNamespace);
    StackGresConfigContext context = ImmutableStackGresConfigContext.builder()
        .source(config)
        .operatorSecret(certSecretFound)
        .isGrafanaIntegrated(false)
        .isGrafanaIntegrationJobFailed(false)
        .build();
    var certSecretGenerated = operatorSecret.generateResource(context)
        .filter(Secret.class::isInstance)
        .map(Secret.class::cast)
        .findFirst();
    if (certSecretGenerated.isPresent()
        && certSecretFound
        .map(certSecret -> !Objects.equals(
            certSecret.getData(),
            certSecretGenerated.get().getData()))
        .orElse(true)) {
      if (certSecretFound.isEmpty()) {
        secretWriter.create(certSecretGenerated.get());
        LOGGER.info("Secret with certificate was created");
      } else {
        secretWriter.update(certSecretGenerated.get());
        LOGGER.info("Secret with certificate was updated");
      }
      LOGGER.warn("Recreating the operator Pod with the created certificate");
      throw new RuntimeException("Sutting down operator in order to release the lock");
    }
  }

  public void waitForCertificate() {
    var config = configFinder.findByNameAndNamespace(operatorName, operatorNamespace)
        .orElseThrow(() -> new IllegalArgumentException(
            "SGConfig " + operatorNamespace + "." + operatorName + " was not found"));
    String certSecretName = podFinder
        .findByNameAndNamespace(
            OperatorProperty.OPERATOR_POD_NAME.getString(), operatorNamespace)
        .flatMap(pod -> pod.getSpec().getContainers()
            .stream()
            .map(Container::getVolumeMounts)
            .flatMap(List::stream)
            .filter(volumeMount -> Optional.of(Paths.get(this.operatorCertPath))
                .map(Path::getParent)
                .map(Object::toString)
                .map(operatorCertParentPath -> operatorCertParentPath
                    .equals(volumeMount.getMountPath()))
                .orElse(false))
            .map(VolumeMount::getName)
            .flatMap(volumeName -> pod.getSpec().getVolumes().stream()
                .filter(volume -> volumeName.equals(volume.getName()))
                .map(Volume::getSecret)
                .map(SecretVolumeSource::getSecretName))
            .findFirst())
        .orElseGet(() -> OperatorSecret.name(config));
    Instant end = Instant.now().plus(
        OperatorProperty.CERTIFICATE_TIMEOUT.get()
        .map(Long::parseLong)
        .orElse(300L),
        ChronoUnit.SECONDS);
    String certValue = null;
    String certKeyValue = null;
    while (end.isAfter(Instant.now())) {
      if (certValue == null && certKeyValue == null) {
        var certSecretFound = secretFinder.findByNameAndNamespace(
            certSecretName, operatorNamespace);
        certValue = certSecretFound.map(Secret::getData)
            .map(data -> data.get("tls.crt"))
            .map(ResourceUtil::decodeSecret)
            .orElse(null);
        certKeyValue = certSecretFound.map(Secret::getData)
            .map(data -> data.get("tls.key"))
            .map(ResourceUtil::decodeSecret)
            .orElse(null);
      }
      try {
        if (certValue != null && certKeyValue != null
            && Files.exists(Paths.get(this.operatorCertPath))
            && Files.readString(Paths.get(this.operatorCertPath)).equals(certValue)
            && Files.exists(Paths.get(this.operatorCertKeyPath))
            && Files.readString(Paths.get(this.operatorCertKeyPath)).equals(certKeyValue)) {
          return;
        }
      } catch (IOException ex) {
        LOGGER.warn("Error while trying to read certificate or certificate key, will retry...", ex);
      }
      try {
        Thread.sleep(1000);
      } catch (InterruptedException ex) {
        throw new RuntimeException(ex);
      }
    }
    throw new RuntimeException("Timeout waiting for the certificate to be found at "
        + this.operatorCertPath + " and " + this.operatorCertKeyPath);
  }
}
