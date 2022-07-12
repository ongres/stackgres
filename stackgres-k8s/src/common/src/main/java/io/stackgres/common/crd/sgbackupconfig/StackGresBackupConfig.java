/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgbackupconfig;

import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Version;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.CommonDefinition;
import io.sundr.builder.annotations.Buildable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@RegisterForReflection
@Group(CommonDefinition.GROUP)
@Version(CommonDefinition.VERSION)
@Kind(StackGresBackupConfig.KIND)
@Buildable(editableEnabled = false, validationEnabled = false, lazyCollectionInitEnabled = false)
public final class StackGresBackupConfig
    extends CustomResource<StackGresBackupConfigSpec, Void>
    implements Namespaced {

  private static final long serialVersionUID = 8062109585634644327L;

  public static final String KIND = "SGBackupConfig";

  @JsonProperty("spec")
  @NotNull(message = "The specification is required")
  @Valid
  private StackGresBackupConfigSpec spec;

  public StackGresBackupConfig() {
    super();
  }

  @Override
  public StackGresBackupConfigSpec getSpec() {
    return spec;
  }

  @Override
  public void setSpec(StackGresBackupConfigSpec spec) {
    this.spec = spec;
  }

  @Override
  public int hashCode() {
    return Objects.hash(spec);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresBackupConfig)) {
      return false;
    }
    StackGresBackupConfig other = (StackGresBackupConfig) obj;
    return Objects.equals(spec, other.spec);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
