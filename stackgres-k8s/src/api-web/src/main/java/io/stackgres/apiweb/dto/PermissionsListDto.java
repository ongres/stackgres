/*
 * Copyright (C) 2020 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto;

import java.util.List;
import java.util.Map;

import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
public class PermissionsListDto {

  Map<String, List<String>> unnamespaced;
  List<Namespaced> namespaced;

  public Map<String, List<String>> getUnnamespaced() {
    return unnamespaced;
  }

  public void setUnnamespaced(Map<String, List<String>> unnamespaced) {
    this.unnamespaced = unnamespaced;
  }

  public List<Namespaced> getNamespaced() {
    return namespaced;
  }

  public void setNamespaced(List<Namespaced> namespaced) {
    this.namespaced = namespaced;
  }

  @RegisterForReflection
  public static class Namespaced {

    String namespace;
    Map<String, List<String>> resources;

    public String getNamespace() {
      return namespace;
    }

    public void setNamespace(String namespace) {
      this.namespace = namespace;
    }

    public Map<String, List<String>> getResources() {
      return resources;
    }

    public void setResources(Map<String, List<String>> resources) {
      this.resources = resources;
    }

    @Override
    public String toString() {
      return StackGresUtil.toPrettyYaml(this);
    }

  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
