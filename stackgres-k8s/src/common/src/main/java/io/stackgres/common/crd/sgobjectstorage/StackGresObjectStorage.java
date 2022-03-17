/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgobjectstorage;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Version;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.CommonDefinition;
import io.stackgres.common.crd.storages.BackupStorage;

@JsonInclude(JsonInclude.Include.NON_NULL)
@RegisterForReflection
@Group(CommonDefinition.GROUP)
@Version(StackGresObjectStorage.VERSION)
@Kind(StackGresObjectStorage.KIND)
public class StackGresObjectStorage extends CustomResource<BackupStorage, Void>
    implements Namespaced {

  public static final String VERSION = "v1beta1";
  public static final String KIND = "SGObjectStorage";

  private static final long serialVersionUID = 1L;

  @Override
  public boolean equals(Object obj) {
    return obj instanceof StackGresObjectStorage other &&
        Objects.equals(spec, other.spec);
  }

  @Override
  public int hashCode() {
    return Objects.hash(spec);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
