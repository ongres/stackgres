/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.fabric8.kubernetes.api.model.NodeAffinity;
import io.fabric8.kubernetes.api.model.PodAffinity;
import io.fabric8.kubernetes.api.model.PodAntiAffinity;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder",
    refs = {
        @BuildableReference(io.fabric8.kubernetes.api.model.Affinity.class)
    })
@SuppressFBWarnings(value = "NM_SAME_SIMPLE_NAME_AS_SUPERCLASS",
    justification = "Intentional name shadowing")
public class Affinity extends io.fabric8.kubernetes.api.model.Affinity {

  private static final long serialVersionUID = 1L;

  public Affinity() {
    super();
  }

  public Affinity(
      NodeAffinity nodeAffinity,
      PodAffinity podAffinity,
      PodAntiAffinity podAntiAffinity) {
    super(nodeAffinity, podAffinity, podAntiAffinity);
  }

  @Override
  public NodeAffinity getNodeAffinity() {
    return super.getNodeAffinity();
  }

  @Override
  public void setNodeAffinity(NodeAffinity nodeAffinity) {
    super.setNodeAffinity(nodeAffinity);
  }

  @Override
  public PodAffinity getPodAffinity() {
    return super.getPodAffinity();
  }

  @Override
  public void setPodAffinity(PodAffinity podAffinity) {
    super.setPodAffinity(podAffinity);
  }

  @Override
  public PodAntiAffinity getPodAntiAffinity() {
    return super.getPodAntiAffinity();
  }

  @Override
  public void setPodAntiAffinity(PodAntiAffinity podAntiAffinity) {
    super.setPodAntiAffinity(podAntiAffinity);
  }

}
