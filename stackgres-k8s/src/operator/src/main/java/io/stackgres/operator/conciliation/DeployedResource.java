/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import java.util.Optional;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.fabric8.kubernetes.api.model.HasMetadata;

public record DeployedResource(
    Optional<HasMetadata> required,
    HasMetadata deployed,
    ObjectNode deployedNode,
    HasMetadata foundDeployed,
    ObjectNode foundDeployedNode) {

  public static DeployedResource create(
      HasMetadata required,
      HasMetadata deployed,
      ObjectNode deployedNode,
      HasMetadata foundDeployed,
      ObjectNode foundDeployedNode) {
    return new DeployedResource(
        Optional.of(required),
        deployed,
        deployedNode,
        foundDeployed,
        foundDeployedNode);
  }

  public static DeployedResource create(
      HasMetadata deployed,
      ObjectNode deployedNode,
      HasMetadata foundDeployed,
      ObjectNode foundDeployedNode) {
    return new DeployedResource(
        Optional.empty(),
        deployed,
        deployedNode,
        foundDeployed,
        foundDeployedNode);
  }

  public static DeployedResource create(
      HasMetadata required,
      HasMetadata deployed,
      ObjectNode deployedNode) {
    return new DeployedResource(
        Optional.of(required),
        deployed,
        deployedNode,
        deployed,
        deployedNode);
  }

  public static DeployedResource create(
      HasMetadata deployed,
      ObjectNode deployedNode) {
    return new DeployedResource(
        Optional.empty(),
        deployed,
        deployedNode,
        deployed,
        deployedNode);
  }

}
