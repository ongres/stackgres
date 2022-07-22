/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.util.List;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import com.fasterxml.jackson.databind.node.TextNode;
import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jsonpatch.JsonPatchOperation;
import com.google.common.collect.ImmutableList;
import io.stackgres.common.BackupStorageUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterBackupConfiguration;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfiguration;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import org.jooq.lambda.Seq;

@ApplicationScoped
public class DefaultBackupPathMutator implements ClusterMutator {

  private JsonPointer backupsPointer;
  private JsonPointer backupsPathPointer;

  @PostConstruct
  public void init() throws NoSuchFieldException {
    String configurationPathJson = getJsonMappingField("configuration",
        StackGresClusterSpec.class);
    String backupsJson = getJsonMappingField("backups",
        StackGresClusterConfiguration.class);
    String backupsPathJson = getJsonMappingField("path",
        StackGresClusterBackupConfiguration.class);

    backupsPointer = SPEC_POINTER.append(configurationPathJson).append(backupsJson);
    backupsPathPointer = JsonPointer.of(backupsPathJson);
  }

  @Override
  public List<JsonPatchOperation> mutate(StackGresClusterReview review) {
    if (review.getRequest().getOperation() == Operation.CREATE
        || review.getRequest().getOperation() == Operation.UPDATE) {
      final StackGresCluster cluster = review.getRequest().getObject();
      final StackGresClusterConfiguration configuration =
          Optional.ofNullable(cluster.getSpec().getConfiguration())
              .orElseGet(StackGresClusterConfiguration::new);

      ImmutableList.Builder<JsonPatchOperation> operations = ImmutableList.builder();
      Optional.ofNullable(configuration.getBackups())
          .map(Seq::seq)
          .orElse(Seq.of())
          .zipWithIndex()
          .forEach(t -> {
            if (t.v1.getPath() == null) {
              final String backupsPath = getBackupPath(cluster);
              operations.add(applyAddValue(
                  backupsPointer.append(t.v2.intValue()).append(backupsPathPointer),
                  TextNode.valueOf(backupsPath)));
            }
          });
      return operations.build();
    }

    return List.of();
  }

  private String getBackupPath(final StackGresCluster cluster) {
    final String postgresVersion = cluster.getSpec().getPostgres().getVersion();
    final String postgresFlavor = cluster.getSpec().getPostgres().getFlavor();
    final String postgresMajorVersion = getPostgresFlavorComponent(postgresFlavor)
        .get(cluster).getMajorVersion(postgresVersion);
    return BackupStorageUtil.getPath(
        cluster.getMetadata().getNamespace(),
        cluster.getMetadata().getName(),
        postgresMajorVersion);
  }

}
