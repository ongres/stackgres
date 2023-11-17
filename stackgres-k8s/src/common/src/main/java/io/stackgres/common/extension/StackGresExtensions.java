/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.extension;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import org.jooq.lambda.Seq;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresExtensions {

  @Valid
  private List<StackGresExtensionPublisher> publishers;

  @Valid
  private List<StackGresExtension> extensions;

  @JsonIgnore
  @AssertTrue(message = "elements of publishers must have a unique id.")
  public boolean havePublishersUniqueNames() {
    return Seq.seq(publishers)
        .grouped(StackGresExtensionPublisher::getId)
        .noneMatch(group -> group.v2.count() > 1);
  }

  @JsonIgnore
  @AssertTrue(message = "elements of extensions must have a unique name.")
  public boolean isExtensionsUniqueNames() {
    return Seq.seq(extensions)
        .grouped(StackGresExtension::getName)
        .noneMatch(group -> group.v2.count() > 1);
  }

  @JsonIgnore
  @AssertTrue(message = "elements of extensions must belong to an element in publishers.")
  public boolean isExtensionsBelongsToPublisher() {
    return extensions.stream()
        .allMatch(extension -> publishers.stream()
            .anyMatch(publisher -> publisher.getId().equals(extension.getPublisher())));
  }

  public List<StackGresExtensionPublisher> getPublishers() {
    return publishers;
  }

  public void setPublishers(List<StackGresExtensionPublisher> publishers) {
    this.publishers = publishers;
  }

  public List<StackGresExtension> getExtensions() {
    return extensions;
  }

  public void setExtensions(List<StackGresExtension> extensions) {
    this.extensions = extensions;
  }

  @Override
  public int hashCode() {
    return Objects.hash(extensions, publishers);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresExtensions)) {
      return false;
    }
    StackGresExtensions other = (StackGresExtensions) obj;
    return Objects.equals(extensions, other.extensions)
        && Objects.equals(publishers, other.publishers);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
