/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jsonpatch.AddOperation;
import com.github.fge.jsonpatch.JsonPatchOperation;
import com.google.common.collect.ImmutableList;

import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.resource.SidecarFinder;
import io.stackgres.operatorframework.admissionwebhook.Operation;

@ApplicationScoped
public class DefaultSidecarMutator implements ClusterMutator {

  private JsonPointer sidecarsPointer;

  private final SidecarFinder sidecarFinder;

  @Inject
  public DefaultSidecarMutator(SidecarFinder sidecarFinder) {
    this.sidecarFinder = sidecarFinder;
  }

  @PostConstruct
  public void init() throws NoSuchFieldException {
    sidecarsPointer = getTargetPointer("sidecars");
  }

  @Override
  public List<JsonPatchOperation> mutate(StackGresClusterReview review) {
    if (review.getRequest().getOperation() == Operation.CREATE
        && review.getRequest().getObject().getSpec().getSidecars() == null) {

      List<TextNode> sidecarsNames = sidecarFinder.getAllOptionalSidecarNames()
          .stream().map(FACTORY::textNode)
          .collect(Collectors.toList());

      ArrayNode sidecars = FACTORY
          .arrayNode(sidecarsNames.size())
          .addAll(sidecarsNames);

      return ImmutableList.of(new AddOperation(sidecarsPointer, sidecars));
    }
    return ImmutableList.of();
  }

}
