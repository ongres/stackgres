/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.fabric8.kubernetes.api.model.NodeSelector;
import io.fabric8.kubernetes.api.model.PreferredSchedulingTerm;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder",
    refs = {
        @BuildableReference(io.fabric8.kubernetes.api.model.NodeAffinity.class),
        @BuildableReference(io.fabric8.kubernetes.api.model.PreferredSchedulingTerm.class),
        @BuildableReference(io.fabric8.kubernetes.api.model.NodeSelector.class),
    })
@SuppressFBWarnings(value = "NM_SAME_SIMPLE_NAME_AS_SUPERCLASS",
    justification = "Intentional name shadowing")
public class NodeAffinity extends io.fabric8.kubernetes.api.model.NodeAffinity {

  private static final long serialVersionUID = 1L;

  public NodeAffinity() {
    super();
  }

  public NodeAffinity(
      List<PreferredSchedulingTerm> preferredDuringSchedulingIgnoredDuringExecution,
      NodeSelector requiredDuringSchedulingIgnoredDuringExecution) {
    super(preferredDuringSchedulingIgnoredDuringExecution,
        requiredDuringSchedulingIgnoredDuringExecution);
  }

  @Override
  public List<PreferredSchedulingTerm> getPreferredDuringSchedulingIgnoredDuringExecution() {
    return super.getPreferredDuringSchedulingIgnoredDuringExecution();
  }

  @Override
  public void setPreferredDuringSchedulingIgnoredDuringExecution(
      List<PreferredSchedulingTerm> preferredDuringSchedulingIgnoredDuringExecution) {
    super.setPreferredDuringSchedulingIgnoredDuringExecution(
        preferredDuringSchedulingIgnoredDuringExecution);
  }

  @Override
  public NodeSelector getRequiredDuringSchedulingIgnoredDuringExecution() {
    return super.getRequiredDuringSchedulingIgnoredDuringExecution();
  }

  @Override
  public void setRequiredDuringSchedulingIgnoredDuringExecution(
      NodeSelector requiredDuringSchedulingIgnoredDuringExecution) {
    super.setRequiredDuringSchedulingIgnoredDuringExecution(
        requiredDuringSchedulingIgnoredDuringExecution);
  }

}
