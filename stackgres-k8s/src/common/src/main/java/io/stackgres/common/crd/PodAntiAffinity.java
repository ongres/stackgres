/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.fabric8.kubernetes.api.model.PodAffinityTerm;
import io.fabric8.kubernetes.api.model.WeightedPodAffinityTerm;
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
        @BuildableReference(io.fabric8.kubernetes.api.model.PodAntiAffinity.class)
    })
@SuppressFBWarnings(value = "NM_SAME_SIMPLE_NAME_AS_SUPERCLASS",
    justification = "Intentional name shadowing")
public class PodAntiAffinity extends io.fabric8.kubernetes.api.model.PodAntiAffinity {

  private static final long serialVersionUID = 1L;

  public PodAntiAffinity() {
    super();
  }

  public PodAntiAffinity(
      List<WeightedPodAffinityTerm> preferredDuringSchedulingIgnoredDuringExecution,
      List<PodAffinityTerm> requiredDuringSchedulingIgnoredDuringExecution) {
    super(preferredDuringSchedulingIgnoredDuringExecution,
        requiredDuringSchedulingIgnoredDuringExecution);
  }

  @Override
  public List<WeightedPodAffinityTerm> getPreferredDuringSchedulingIgnoredDuringExecution() {
    return super.getPreferredDuringSchedulingIgnoredDuringExecution();
  }

  @Override
  public void setPreferredDuringSchedulingIgnoredDuringExecution(
      List<WeightedPodAffinityTerm> preferredDuringSchedulingIgnoredDuringExecution) {
    super.setPreferredDuringSchedulingIgnoredDuringExecution(
        preferredDuringSchedulingIgnoredDuringExecution);
  }

  @Override
  public List<PodAffinityTerm> getRequiredDuringSchedulingIgnoredDuringExecution() {
    return super.getRequiredDuringSchedulingIgnoredDuringExecution();
  }

  @Override
  public void setRequiredDuringSchedulingIgnoredDuringExecution(
      List<PodAffinityTerm> requiredDuringSchedulingIgnoredDuringExecution) {
    super.setRequiredDuringSchedulingIgnoredDuringExecution(
        requiredDuringSchedulingIgnoredDuringExecution);
  }

}
