/*
 * Copyright (C) 2020 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto;

import java.util.List;
import java.util.Map;

import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.apiweb.dto.PermissionsListDto.Namespaced;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
public record PermissionsListDto(
    Map<String, List<String>> unnamespaced,
    List<Namespaced> namespaced) {

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

  @RegisterForReflection
  public record Namespaced(
      String namespace,
      Map<String, List<String>> resources) {

    @Override
    public String toString() {
      return StackGresUtil.toPrettyYaml(this);
    }

  }

}
