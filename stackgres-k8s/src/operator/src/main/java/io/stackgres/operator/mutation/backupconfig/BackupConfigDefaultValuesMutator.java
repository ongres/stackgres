/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.backupconfig;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jsonpatch.JsonPatchOperation;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.operator.common.BackupConfigReview;
import io.stackgres.operator.mutation.DefaultValuesMutator;

@ApplicationScoped
public class BackupConfigDefaultValuesMutator
    extends DefaultValuesMutator<StackGresBackupConfig, BackupConfigReview>
    implements BackupConfigMutator {

  @Override
  public JsonNode getSourceNode(StackGresBackupConfig resource) {
    return toNode(resource)
        .get("spec");
  }

  @Override
  public JsonNode getTargetNode(StackGresBackupConfig resource) {
    return getSourceNode(resource);
  }

  @Override
  public List<JsonPatchOperation> mutate(BackupConfigReview review) {
    return mutate(SPEC_POINTER, review.getRequest().getObject());
  }

  @Override
  public List<JsonPatchOperation> applyDefaults(JsonPointer basePointer,
      JsonNode defaultNode,
      JsonNode incomingNode) {
    if (incomingNode.has("storage")
        && incomingNode.get("storage").has("type")
        && !incomingNode.get("storage").get("type").equals(
        defaultNode.get("storage").get("type"))) {
      defaultNode = defaultNode.deepCopy();
      ((ObjectNode) defaultNode).remove("storage");
    }
    return super.applyDefaults(basePointer, defaultNode, incomingNode);
  }

}
