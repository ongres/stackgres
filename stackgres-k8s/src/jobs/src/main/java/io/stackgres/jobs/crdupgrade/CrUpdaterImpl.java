/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.crdupgrade;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CrUpdaterImpl implements CrUpdater {

  private static final Logger LOGGER = LoggerFactory.getLogger(CrUpdaterImpl.class);

  private final CrdLoader crdLoader;

  public CrUpdaterImpl(CrdLoader crdLoader) {
    this.crdLoader = crdLoader;
  }

  @Override
  public void updateExistingCustomResources() {
    crdLoader.scanDefinitions()
        .stream()
        .forEach(installedCrd -> {
          LOGGER.info("Patching existing custom resources to apply defaults for CRD {}",
              installedCrd.getSpec().getNames().getKind());
          crdLoader.updateExistingCustomResources(installedCrd);
          LOGGER.info("Existing custom resources for CRD {}. Patched",
              installedCrd.getSpec().getNames().getKind());
        });
  }

}
