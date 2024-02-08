/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true,
    value = {"clusterName", "deletionGracePeriodSeconds",
        "generateName", "generation", "managedFields",
        "selfLink"})
public class Metadata {

  private Map<String, String> annotations;

  private Map<String, String> labels;

  private String creationTimestamp;

  private Long deletionGracePeriodSeconds;

  private String deletionTimestamp;

  private List<String> finalizers;

  private String generateName;

  private Long generation;

  private String name;

  private String namespace;

  private List<OwnerReference> ownerReferences;

  private String resourceVersion;

  private String selfLink;

  private String uid;

  public Map<String, String> getAnnotations() {
    return annotations;
  }

  public void setAnnotations(Map<String, String> annotations) {
    this.annotations = annotations;
  }

  public Map<String, String> getLabels() {
    return labels;
  }

  public void setLabels(Map<String, String> labels) {
    this.labels = labels;
  }

  public String getCreationTimestamp() {
    return creationTimestamp;
  }

  public void setCreationTimestamp(String creationTimestamp) {
    this.creationTimestamp = creationTimestamp;
  }

  public Long getDeletionGracePeriodSeconds() {
    return deletionGracePeriodSeconds;
  }

  public void setDeletionGracePeriodSeconds(Long deletionGracePeriodSeconds) {
    this.deletionGracePeriodSeconds = deletionGracePeriodSeconds;
  }

  public String getDeletionTimestamp() {
    return deletionTimestamp;
  }

  public void setDeletionTimestamp(String deletionTimestamp) {
    this.deletionTimestamp = deletionTimestamp;
  }

  public List<String> getFinalizers() {
    return finalizers;
  }

  public void setFinalizers(List<String> finalizers) {
    this.finalizers = finalizers;
  }

  public String getGenerateName() {
    return generateName;
  }

  public void setGenerateName(String generateName) {
    this.generateName = generateName;
  }

  public Long getGeneration() {
    return generation;
  }

  public void setGeneration(Long generation) {
    this.generation = generation;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getNamespace() {
    return namespace;
  }

  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }

  public List<OwnerReference> getOwnerReferences() {
    return ownerReferences;
  }

  public void setOwnerReferences(List<OwnerReference> ownerReferences) {
    this.ownerReferences = ownerReferences;
  }

  public String getResourceVersion() {
    return resourceVersion;
  }

  public void setResourceVersion(String resourceVersion) {
    this.resourceVersion = resourceVersion;
  }

  public String getSelfLink() {
    return selfLink;
  }

  public void setSelfLink(String selfLink) {
    this.selfLink = selfLink;
  }

  public String getUid() {
    return uid;
  }

  public void setUid(String uid) {
    this.uid = uid;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
