/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.fabric8.kubernetes.api.model.LabelSelector;
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
        @BuildableReference(io.fabric8.kubernetes.api.model.TopologySpreadConstraint.class),
        @BuildableReference(io.fabric8.kubernetes.api.model.LabelSelector.class),
    })
@SuppressFBWarnings(value = "NM_SAME_SIMPLE_NAME_AS_SUPERCLASS",
    justification = "Intentional name shadowing")
public class TopologySpreadConstraint
    extends io.fabric8.kubernetes.api.model.TopologySpreadConstraint {

  private static final long serialVersionUID = 1L;

  public TopologySpreadConstraint() {
    super();
  }

  public TopologySpreadConstraint(LabelSelector labelSelector, List<String> matchLabelKeys,
      Integer maxSkew, Integer minDomains, String nodeAffinityPolicy, String nodeTaintsPolicy,
      String topologyKey, String whenUnsatisfiable) {
    super(labelSelector, matchLabelKeys, maxSkew, minDomains, nodeAffinityPolicy, nodeTaintsPolicy,
        topologyKey, whenUnsatisfiable);
  }

  @Override
  public LabelSelector getLabelSelector() {
    return super.getLabelSelector();
  }

  @Override
  public void setLabelSelector(LabelSelector labelSelector) {
    super.setLabelSelector(labelSelector);
  }

  @Override
  public List<String> getMatchLabelKeys() {
    return super.getMatchLabelKeys();
  }

  @Override
  public void setMatchLabelKeys(List<String> matchLabelKeys) {
    super.setMatchLabelKeys(matchLabelKeys);
  }

  @Override
  public Integer getMaxSkew() {
    return super.getMaxSkew();
  }

  @Override
  public void setMaxSkew(Integer maxSkew) {
    super.setMaxSkew(maxSkew);
  }

  @Override
  public Integer getMinDomains() {
    return super.getMinDomains();
  }

  @Override
  public void setMinDomains(Integer minDomains) {
    super.setMinDomains(minDomains);
  }

  @Override
  public String getNodeAffinityPolicy() {
    return super.getNodeAffinityPolicy();
  }

  @Override
  public void setNodeAffinityPolicy(String nodeAffinityPolicy) {
    super.setNodeAffinityPolicy(nodeAffinityPolicy);
  }

  @Override
  public String getNodeTaintsPolicy() {
    return super.getNodeTaintsPolicy();
  }

  @Override
  public void setNodeTaintsPolicy(String nodeTaintsPolicy) {
    super.setNodeTaintsPolicy(nodeTaintsPolicy);
  }

  @Override
  public String getTopologyKey() {
    return super.getTopologyKey();
  }

  @Override
  public void setTopologyKey(String topologyKey) {
    super.setTopologyKey(topologyKey);
  }

  @Override
  public String getWhenUnsatisfiable() {
    return super.getWhenUnsatisfiable();
  }

  @Override
  public void setWhenUnsatisfiable(String whenUnsatisfiable) {
    super.setWhenUnsatisfiable(whenUnsatisfiable);
  }

}
