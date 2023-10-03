/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgobjectstorage;

import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Plural;
import io.fabric8.kubernetes.model.annotation.Singular;
import io.fabric8.kubernetes.model.annotation.Version;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.CommonDefinition;
import io.stackgres.common.crd.storages.BackupStorage;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(value = { "status" }, ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder",
    refs = {
        @BuildableReference(io.fabric8.kubernetes.client.CustomResource.class),
        @BuildableReference(io.fabric8.kubernetes.api.model.ObjectMeta.class),
    })
@Group(CommonDefinition.GROUP)
@Version(StackGresObjectStorage.VERSION)
@Kind(StackGresObjectStorage.KIND)
@Singular("sgobjectstorage")
@Plural("sgobjectstorages")
public class StackGresObjectStorage extends CustomResource<BackupStorage, Void>
    implements Namespaced {

  private static final long serialVersionUID = 1L;

  public static final String KIND = "SGObjectStorage";

  public static final String VERSION = "v1beta1";

  @NotNull(message = "The specification is required")
  @Valid
  private BackupStorage spec;

  @Override
  public ObjectMeta getMetadata() {
    return super.getMetadata();
  }

  @Override
  public void setMetadata(ObjectMeta metadata) {
    super.setMetadata(metadata);
  }

  @Override
  public BackupStorage getSpec() {
    return spec;
  }

  @Override
  public void setSpec(BackupStorage spec) {
    this.spec = spec;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + Objects.hash(spec);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!super.equals(obj)) {
      return false;
    }
    if (!(obj instanceof StackGresObjectStorage)) {
      return false;
    }
    StackGresObjectStorage other = (StackGresObjectStorage) obj;
    return Objects.equals(spec, other.spec);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
