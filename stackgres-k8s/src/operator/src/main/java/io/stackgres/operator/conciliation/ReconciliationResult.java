/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import java.util.List;
import java.util.Objects;

import javax.validation.constraints.NotNull;

import io.fabric8.kubernetes.api.model.HasMetadata;

public class ReconciliationResult {

  private List<HasMetadata> creations;

  private List<HasMetadata> patches;

  private List<HasMetadata> deletions;

  public ReconciliationResult(@NotNull List<HasMetadata> creations,
                              @NotNull List<HasMetadata> patches,
                              @NotNull List<HasMetadata> deletions) {
    Objects.requireNonNull(creations);
    Objects.requireNonNull(patches);
    Objects.requireNonNull(deletions);
    this.creations = creations;
    this.patches = patches;
    this.deletions = deletions;
  }

  public boolean isUpToDate() {
    return creations.isEmpty() && patches.isEmpty() && deletions.isEmpty();
  }

  public List<HasMetadata> getCreations() {
    return creations;
  }

  public void setCreations(List<HasMetadata> creations) {
    this.creations = creations;
  }

  public List<HasMetadata> getPatches() {
    return patches;
  }

  public void setPatches(List<HasMetadata> patches) {
    this.patches = patches;
  }

  public List<HasMetadata> getDeletions() {
    return deletions;
  }

  public void setDeletions(List<HasMetadata> deletions) {
    this.deletions = deletions;
  }
}
