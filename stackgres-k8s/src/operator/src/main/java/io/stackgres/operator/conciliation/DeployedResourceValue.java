/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import java.util.Optional;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.fabric8.kubernetes.api.model.HasMetadata;

public record DeployedResourceValue(
    Optional<HasMetadata> required,
    HasMetadata deployed,
    ObjectNode deployedNode,
    HasMetadata latestDeployed,
    ObjectNode latestDeployedNode) {

  public static DeployedResourceValue create(
      HasMetadata required,
      HasMetadata deployed,
      ObjectNode deployedNode,
      HasMetadata latestDeployed,
      ObjectNode latestDeployedNode) {
    return new DeployedResourceValue(
        Optional.of(required),
        deployed,
        deployedNode,
        latestDeployed,
        latestDeployedNode);
  }

  public static DeployedResourceValue create(
      HasMetadata deployed,
      ObjectNode deployedNode,
      HasMetadata latestDeployed,
      ObjectNode latestDeployedNode) {
    return new DeployedResourceValue(
        Optional.empty(),
        deployed,
        deployedNode,
        latestDeployed,
        latestDeployedNode);
  }

  public static DeployedResourceValue create(
      HasMetadata required,
      HasMetadata deployed,
      ObjectNode deployedNode) {
    return new DeployedResourceValue(
        Optional.of(required),
        deployed,
        deployedNode,
        deployed,
        deployedNode);
  }

  public static DeployedResourceValue create(
      HasMetadata deployed,
      ObjectNode deployedNode) {
    return new DeployedResourceValue(
        Optional.empty(),
        deployed,
        deployedNode,
        deployed,
        deployedNode);
  }

}
