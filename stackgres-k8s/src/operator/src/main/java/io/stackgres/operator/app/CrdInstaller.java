/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.app;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinitionVersion;
import io.fabric8.kubernetes.api.model.apiextensions.v1.JSONSchemaProps;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.kubernetes.client.impl.BaseClient;
import io.stackgres.common.CrdLoader;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.YamlMapperProvider;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.kubernetesclient.ProxiedKubernetesClientProducer.KubernetesClientInvocationHandler;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceWriter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jooq.lambda.tuple.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class CrdInstaller {

  private static final long OLDEST = StackGresVersion.OLDEST.getVersionAsNumber();
  /*
   * When add new version just increase the above to the next minor if latest minor is greater than 2.
   */
  private static final long V_1_8 = StackGresVersion.V_1_8.getVersionAsNumber();

  private static final Logger LOGGER = LoggerFactory.getLogger(CrdInstaller.class);

  private final ResourceFinder<CustomResourceDefinition> crdResourceFinder;
  private final ResourceWriter<CustomResourceDefinition> crdResourceWriter;
  private final CrdLoader crdLoader;
  private final KubernetesClient client;

  @Inject
  public CrdInstaller(
      ResourceFinder<CustomResourceDefinition> crdResourceFinder,
      ResourceWriter<CustomResourceDefinition> crdResourceWriter,
      YamlMapperProvider yamlMapperProvider,
      KubernetesClient client) {
    this.crdResourceFinder = crdResourceFinder;
    this.crdResourceWriter = crdResourceWriter;
    this.client = client;
    this.crdLoader = new CrdLoader(yamlMapperProvider.get());
  }

  public void checkUpgrade() {
    var resourcesRequiringUpgrade = crdLoader.scanCrds().stream()
        .map(crd -> crdResourceFinder.findByName(crd.getMetadata().getName()))
        .flatMap(Optional::stream)
        .flatMap(crd -> client
          .genericKubernetesResources(CustomResourceDefinitionContext.fromCrd(crd))
          .inAnyNamespace()
          .list()
          .getItems()
          .stream()
          .map(resource -> Tuple.tuple(resource, Optional.of(resource)
              .map(StackGresVersion::getStackGresVersionFromResourceAsNumber)
              .filter(version -> version < OLDEST)))
          .filter(t -> t.v2.isPresent())
          .map(t -> t.map2(Optional::get))
          .map(t -> t.concat("version at " + StackGresVersion
              .getStackGresRawVersionFromResource(t.v1)))
          .filter(t -> List.of(
              HasMetadata.getKind(StackGresCluster.class),
              HasMetadata.getKind(StackGresShardedCluster.class),
              HasMetadata.getKind(StackGresDistributedLogs.class))
              .contains(t.v1.getKind())
              || t.v2.longValue() > V_1_8))
        .toList();
    if (!resourcesRequiringUpgrade.isEmpty()) {
      throw new RuntimeException("Can not upgrade due to some resources still at version"
          + " older than \"" + StackGresVersion.OLDEST.getVersion() + "\"."
          + " Please, downgrade to a previous version of the operator and run a SGDbOps of"
          + " type securityUpgrade on all the SGClusters of the following list"
          + " (if any is present):\n"
          + resourcesRequiringUpgrade.stream()
          .map(t -> t.v1.getKind() + " "
              + t.v1.getMetadata().getNamespace() + "."
              + t.v1.getMetadata().getName() + ": " + t.v3)
          .collect(Collectors.joining("\n")));
    }
  }

  public void installCustomResourceDefinitions() {
    LOGGER.info("Installing CRDs");
    crdLoader.scanCrds()
        .stream()
        .map(this::fixCrd)
        .forEach(this::installCrd);
  }

  protected void installCrd(@Nonnull CustomResourceDefinition currentCrd) {
    String name = currentCrd.getMetadata().getName();
    LOGGER.info("Installing CRD {}", name);
    Optional<CustomResourceDefinition> installedCrdOpt = crdResourceFinder
        .findByName(name);

    if (installedCrdOpt.isPresent()) {
      LOGGER.debug("CRD {} is present, patching it", name);
      CustomResourceDefinition installedCrd = installedCrdOpt.get();
      if (!isCurrentCrdInstalled(currentCrd, installedCrd)) {
        upgradeCrd(currentCrd, installedCrd);
      }
      updateAlreadyInstalledVersions(currentCrd, installedCrd);
      crdResourceWriter.update(installedCrd, foundCrd -> {
        fixCrd(installedCrd);
        foundCrd.setSpec(installedCrd.getSpec());
        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace("Updating CRD:\n{}",
              serializeToJsonAsKubernetesClient(foundCrd));
        }
      });
    } else {
      LOGGER.info("CRD {} is not present, installing it", name);
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("Creating CRD:\n{}", serializeToJsonAsKubernetesClient(currentCrd));
      }
      crdResourceWriter.create(currentCrd);
    }
  }

  private String serializeToJsonAsKubernetesClient(CustomResourceDefinition foundCrd) {
    try {
      return BaseClient.class.cast(
          KubernetesClientInvocationHandler.class.cast(
              Proxy.getInvocationHandler(client)).getClient())
          .getKubernetesSerialization().asJson(foundCrd);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  private void updateAlreadyInstalledVersions(CustomResourceDefinition currentCrd,
      CustomResourceDefinition installedCrd) {
    installedCrd.getSpec().getVersions().forEach(installedVersion -> {
      currentCrd.getSpec()
          .getVersions()
          .stream()
          .filter(v -> v.getName().equals(installedVersion.getName()))
          .forEach(currentVersion -> updateAlreadyDeployedVersion(
              installedVersion, currentVersion));
    });
  }

  private void updateAlreadyDeployedVersion(CustomResourceDefinitionVersion installedVersion,
      CustomResourceDefinitionVersion currentVersion) {
    installedVersion.setSchema(currentVersion.getSchema());
    installedVersion.setSubresources(currentVersion.getSubresources());
    installedVersion.setAdditionalPrinterColumns(currentVersion.getAdditionalPrinterColumns());
  }

  private void upgradeCrd(
      CustomResourceDefinition currentCrd,
      CustomResourceDefinition installedCrd) {
    disableStorageVersions(installedCrd);
    addNewSchemaVersions(currentCrd, installedCrd);
    crdResourceWriter.update(installedCrd, foundCrd -> {
      fixCrd(installedCrd);
      foundCrd.setSpec(installedCrd.getSpec());
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("Updating CRD:\n{}",
            serializeToJsonAsKubernetesClient(foundCrd));
      }
    });
  }

  private void disableStorageVersions(CustomResourceDefinition installedCrd) {
    installedCrd.getSpec().getVersions()
        .forEach(versionDefinition -> versionDefinition.setStorage(false));
  }

  private void addNewSchemaVersions(
      CustomResourceDefinition currentCrd,
      CustomResourceDefinition installedCrd) {
    List<String> installedVersions = installedCrd.getSpec().getVersions()
        .stream()
        .map(CustomResourceDefinitionVersion::getName)
        .toList();

    List<String> versionsToInstall = currentCrd.getSpec().getVersions()
        .stream()
        .map(CustomResourceDefinitionVersion::getName)
        .filter(Predicate.not(installedVersions::contains))
        .toList();

    currentCrd.getSpec().getVersions().stream()
        .filter(version -> versionsToInstall.contains(version.getName()))
        .forEach(installedCrd.getSpec().getVersions()::add);
  }

  private boolean isCurrentCrdInstalled(
      CustomResourceDefinition currentCrd,
      CustomResourceDefinition installedCrd) {
    final String currentVersion = currentCrd.getSpec().getVersions()
        .stream()
        .filter(CustomResourceDefinitionVersion::getStorage).findFirst()
        .orElseThrow(() -> new RuntimeException("At least one CRD version must be stored"))
        .getName();
    return installedCrd.getSpec().getVersions().stream()
        .map(CustomResourceDefinitionVersion::getName)
        .anyMatch(installedVersion -> installedVersion.equals(currentVersion));
  }

  public void checkCustomResourceDefinitions() {
    crdLoader.scanCrds()
        .forEach(this::checkCrd);
  }

  protected void checkCrd(@Nonnull CustomResourceDefinition currentCrd) {
    String name = currentCrd.getMetadata().getName();
    Optional<CustomResourceDefinition> installedCrdOpt = crdResourceFinder
        .findByName(name);

    if (installedCrdOpt.isEmpty()) {
      throw new RuntimeException("CRD " + name + " was not found.");
    }
  }

  private CustomResourceDefinition fixCrd(CustomResourceDefinition crd) {
    crd.getSpec().getVersions()
        .forEach(crdVersion -> fixOpenApiProps(
            "openAPIV3Schema",
            crdVersion.getSchema().getOpenAPIV3Schema()));
    return crd;
  }

  private void fixOpenApiProps(String key, JSONSchemaProps props) {
    LOGGER.trace("Inspecting key {}", key);
    if (props == null) {
      return;
    }
    if (props.getDependencies() != null) {
      LOGGER.trace("Setting dependencies null for key {}", key);
      props.setDependencies(null);
    }
    props.getProperties().entrySet().forEach(e -> fixOpenApiProps(
        key + "." + e.getKey(), e.getValue()));
    props.getDefinitions().entrySet().forEach(e -> fixOpenApiProps(
        key + "." + e.getKey(), e.getValue()));
    if (props.getItems() != null) {
      fixOpenApiProps(key + "[]", props.getItems().getSchema());
    }
  }

}
