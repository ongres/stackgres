/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.external.prometheus;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class RelabelConfig {

  private List<String> sourceLabels;

  private String separator;

  private String targetLabel;

  private String regex;

  private Long modulus;

  private String replacement;

  private String action;

  public List<String> getSourceLabels() {
    return sourceLabels;
  }

  public void setSourceLabels(List<String> sourceLabels) {
    this.sourceLabels = sourceLabels;
  }

  public String getSeparator() {
    return separator;
  }

  public void setSeparator(String separator) {
    this.separator = separator;
  }

  public String getTargetLabel() {
    return targetLabel;
  }

  public void setTargetLabel(String targetLabel) {
    this.targetLabel = targetLabel;
  }

  public String getRegex() {
    return regex;
  }

  public void setRegex(String regex) {
    this.regex = regex;
  }

  public Long getModulus() {
    return modulus;
  }

  public void setModulus(Long modulus) {
    this.modulus = modulus;
  }

  public String getReplacement() {
    return replacement;
  }

  public void setReplacement(String replacement) {
    this.replacement = replacement;
  }

  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action;
  }

  @Override
  public int hashCode() {
    return Objects.hash(action, modulus, regex, replacement, separator, sourceLabels, targetLabel);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof RelabelConfig)) {
      return false;
    }
    RelabelConfig other = (RelabelConfig) obj;
    return Objects.equals(action, other.action) && Objects.equals(modulus, other.modulus)
        && Objects.equals(regex, other.regex) && Objects.equals(replacement, other.replacement)
        && Objects.equals(separator, other.separator)
        && Objects.equals(sourceLabels, other.sourceLabels)
        && Objects.equals(targetLabel, other.targetLabel);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
