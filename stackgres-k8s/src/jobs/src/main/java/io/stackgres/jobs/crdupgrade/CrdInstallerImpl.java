/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.crdupgrade;

import java.util.Optional;
import java.util.logging.Logger;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinitionVersion;
import io.stackgres.jobs.common.ResourceFinder;
import io.stackgres.jobs.common.ResourceWriter;

public class CrdInstallerImpl implements CrdInstaller {

  private static final Logger LOGGER = Logger.getLogger(CrdInstallerImpl.class.getName());

  private final ResourceFinder<CustomResourceDefinition> customResourceDefinitionResourceFinder;

  private final ResourceWriter<CustomResourceDefinition> customResourceDefinitionResourceWriter;

  private final CrdLoader crdLoader;

  public CrdInstallerImpl(
      ResourceFinder<CustomResourceDefinition> customResourceDefinitionResourceFinder,
      ResourceWriter<CustomResourceDefinition> customResourceDefinitionResourceWriter,
      CrdLoader crdLoader) {
    this.customResourceDefinitionResourceFinder = customResourceDefinitionResourceFinder;
    this.customResourceDefinitionResourceWriter = customResourceDefinitionResourceWriter;
    this.crdLoader = crdLoader;
  }

  @Override
  public void installCustomResourceDefinitions() {

    crdLoader.scanDefinitions()
        .forEach(definition -> installCrd(definition.getName(), definition.getKind()));

  }

  protected void installCrd(String name, String kind) {

    Optional<CustomResourceDefinition> installedCrdOpt = customResourceDefinitionResourceFinder
        .findByName(name);

    CustomResourceDefinition currentCrd = getDefinition(kind);

    if (installedCrdOpt.isPresent()) {
      LOGGER.info("CRD " + name + " is present, patching it");
      CustomResourceDefinition installedCrd = installedCrdOpt.get();
      if (!isCurrentCrdInstalled(currentCrd, installedCrd)) {
        upgradeCrd(currentCrd, installedCrd);
      }
      updateOldSchemas(currentCrd, installedCrd);
      customResourceDefinitionResourceWriter.update(installedCrd);
      LOGGER.info("CRD " + name + " . Patched");

    } else {
      LOGGER.info("CRD " + name + " is not present, installing it");
      customResourceDefinitionResourceWriter.create(currentCrd);
      LOGGER.info("CRD " + name + " . Installed");
    }
  }

  private void updateOldSchemas(CustomResourceDefinition currentCrd,
                                CustomResourceDefinition installedCrd) {
    installedCrd.getSpec().getVersions().forEach(definition -> {
      Optional<CustomResourceDefinitionVersion> currentDefinition = currentCrd.getSpec()
          .getVersions()
          .stream()
          .filter(v -> v.getName().equals(definition.getName()))
          .findFirst();
      currentDefinition.ifPresent(cd -> definition.setSchema(cd.getSchema()));
    });
  }

  private void upgradeCrd(CustomResourceDefinition currentCrd,
                          CustomResourceDefinition installedCrd) {

    disableStorageVersions(installedCrd);
    addNewSchemaVersions(currentCrd, installedCrd);
    customResourceDefinitionResourceWriter.update(installedCrd);

  }

  private void disableStorageVersions(CustomResourceDefinition installedCrd) {
    installedCrd.getSpec().getVersions()
        .forEach(versionDefinition -> versionDefinition.setStorage(false));
  }

  private void addNewSchemaVersions(CustomResourceDefinition currentCrd,
                                    CustomResourceDefinition installedCrd) {
    ImmutableList<String> installedVersions = installedCrd.getSpec().getVersions()
        .stream().map(CustomResourceDefinitionVersion::getName)
        .collect(ImmutableList.toImmutableList());

    ImmutableList<String> versionsToInstall = currentCrd.getSpec().getVersions()
        .stream().map(CustomResourceDefinitionVersion::getName)
        .filter(version -> !installedVersions.contains(version))
        .collect(ImmutableList.toImmutableList());

    currentCrd.getSpec().getVersions().stream()
        .filter(version -> versionsToInstall.contains(version.getName()))
        .forEach(version -> installedCrd.getSpec().getVersions().add(version));

  }

  private CustomResourceDefinition getDefinition(String definition) {

    return crdLoader.load(definition);
  }

  private boolean isCurrentCrdInstalled(CustomResourceDefinition currentCrd,
                                        CustomResourceDefinition installedCrd) {
    final String currentVersion = currentCrd.getSpec().getVersions()
        .stream().filter(CustomResourceDefinitionVersion::getStorage).findFirst()
        .orElseThrow(() -> new RuntimeException("At least one CRD version must be stored"))
        .getName();
    return installedCrd.getSpec().getVersions().stream()
        .map(CustomResourceDefinitionVersion::getName)
        .anyMatch(installedVersion -> installedVersion.equals(currentVersion));

  }

}
