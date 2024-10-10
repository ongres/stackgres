/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.component;

import java.util.function.Supplier;

import io.stackgres.common.docir.DocirMetadataManager;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class StackGresContext {

  private final Supplier<DocirMetadataManager> metadataManager;

  /**
   * The {@link DocirMetadataManager} is resolved lazily since the concrete implementation is
   * provided by each application and some of them (like stream) do not use the components.
   */
  @Inject
  public StackGresContext(
      Instance<DocirMetadataManager> metadataManager) {
    this.metadataManager = metadataManager::get;
  }

  protected StackGresContext(
      DocirMetadataManager metadataManager) {
    this.metadataManager = () -> metadataManager;
  }

  public DocirMetadataManager getMetadataManager() {
    return metadataManager.get();
  }

}
