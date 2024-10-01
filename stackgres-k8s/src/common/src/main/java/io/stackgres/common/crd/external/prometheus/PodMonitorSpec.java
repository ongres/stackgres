/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.external.prometheus;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class PodMonitorSpec {

  private String jobLabel;

  private List<String> podTargetLabels; 
  
  private List<PodMetricsEndpoint> podMetricsEndpoints;

  private LabelSelector selector;

  private NamespaceSelector namespaceSelector;

  private Integer sampleLimit;

  private Integer targetLimit;

  private List<String> scrapeProtocols;

  private Integer labelLimit;

  private Integer labelNameLengthLimit;

  private Integer labelValueLengthLimit;

  private Integer keepDroppedTargets;

  private AttachMetadata attachMetadata;

  private String scrapeClass;

  private String bodySizeLimit;

  public String getJobLabel() {
    return jobLabel;
  }

  public void setJobLabel(String jobLabel) {
    this.jobLabel = jobLabel;
  }

  public List<String> getPodTargetLabels() {
    return podTargetLabels;
  }

  public void setPodTargetLabels(List<String> podTargetLabels) {
    this.podTargetLabels = podTargetLabels;
  }

  public List<PodMetricsEndpoint> getPodMetricsEndpoints() {
    return podMetricsEndpoints;
  }

  public void setPodMetricsEndpoints(List<PodMetricsEndpoint> podMetricsEndpoints) {
    this.podMetricsEndpoints = podMetricsEndpoints;
  }

  public LabelSelector getSelector() {
    return selector;
  }

  public void setSelector(LabelSelector selector) {
    this.selector = selector;
  }

  public NamespaceSelector getNamespaceSelector() {
    return namespaceSelector;
  }

  public void setNamespaceSelector(NamespaceSelector namespaceSelector) {
    this.namespaceSelector = namespaceSelector;
  }

  public Integer getSampleLimit() {
    return sampleLimit;
  }

  public void setSampleLimit(Integer sampleLimit) {
    this.sampleLimit = sampleLimit;
  }

  public Integer getTargetLimit() {
    return targetLimit;
  }

  public void setTargetLimit(Integer targetLimit) {
    this.targetLimit = targetLimit;
  }

  public List<String> getScrapeProtocols() {
    return scrapeProtocols;
  }

  public void setScrapeProtocols(List<String> scrapeProtocols) {
    this.scrapeProtocols = scrapeProtocols;
  }

  public Integer getLabelLimit() {
    return labelLimit;
  }

  public void setLabelLimit(Integer labelLimit) {
    this.labelLimit = labelLimit;
  }

  public Integer getLabelNameLengthLimit() {
    return labelNameLengthLimit;
  }

  public void setLabelNameLengthLimit(Integer labelNameLengthLimit) {
    this.labelNameLengthLimit = labelNameLengthLimit;
  }

  public Integer getLabelValueLengthLimit() {
    return labelValueLengthLimit;
  }

  public void setLabelValueLengthLimit(Integer labelValueLengthLimit) {
    this.labelValueLengthLimit = labelValueLengthLimit;
  }

  public Integer getKeepDroppedTargets() {
    return keepDroppedTargets;
  }

  public void setKeepDroppedTargets(Integer keepDroppedTargets) {
    this.keepDroppedTargets = keepDroppedTargets;
  }

  public AttachMetadata getAttachMetadata() {
    return attachMetadata;
  }

  public void setAttachMetadata(AttachMetadata attachMetadata) {
    this.attachMetadata = attachMetadata;
  }

  public String getScrapeClass() {
    return scrapeClass;
  }

  public void setScrapeClass(String scrapeClass) {
    this.scrapeClass = scrapeClass;
  }

  public String getBodySizeLimit() {
    return bodySizeLimit;
  }

  public void setBodySizeLimit(String bodySizeLimit) {
    this.bodySizeLimit = bodySizeLimit;
  }

  @Override
  public int hashCode() {
    return Objects.hash(attachMetadata, bodySizeLimit, jobLabel, keepDroppedTargets, labelLimit,
        labelNameLengthLimit, labelValueLengthLimit, namespaceSelector, podMetricsEndpoints,
        podTargetLabels, sampleLimit, scrapeClass, scrapeProtocols, selector, targetLimit);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof PodMonitorSpec)) {
      return false;
    }
    PodMonitorSpec other = (PodMonitorSpec) obj;
    return Objects.equals(attachMetadata, other.attachMetadata)
        && Objects.equals(bodySizeLimit, other.bodySizeLimit)
        && Objects.equals(jobLabel, other.jobLabel)
        && Objects.equals(keepDroppedTargets, other.keepDroppedTargets)
        && Objects.equals(labelLimit, other.labelLimit)
        && Objects.equals(labelNameLengthLimit, other.labelNameLengthLimit)
        && Objects.equals(labelValueLengthLimit, other.labelValueLengthLimit)
        && Objects.equals(namespaceSelector, other.namespaceSelector)
        && Objects.equals(podMetricsEndpoints, other.podMetricsEndpoints)
        && Objects.equals(podTargetLabels, other.podTargetLabels)
        && Objects.equals(sampleLimit, other.sampleLimit)
        && Objects.equals(scrapeClass, other.scrapeClass)
        && Objects.equals(scrapeProtocols, other.scrapeProtocols)
        && Objects.equals(selector, other.selector)
        && Objects.equals(targetLimit, other.targetLimit);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
