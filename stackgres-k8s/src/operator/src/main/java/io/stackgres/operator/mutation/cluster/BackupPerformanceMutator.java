/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import java.util.List;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jsonpatch.JsonPatchOperation;
import com.google.common.collect.ImmutableList;
import io.stackgres.common.crd.sgbackupconfig.StackGresBaseBackupPerformance;
import io.stackgres.common.crd.sgcluster.StackGresClusterBackupConfiguration;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfiguration;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import org.jooq.lambda.Seq;

@ApplicationScoped
public class BackupPerformanceMutator implements ClusterMutator {

  protected static final JsonMapper JSON_MAPPER = new JsonMapper();

  private JsonPointer backupsPointer;
  private JsonPointer maxDiskBandwidthPointer;
  private JsonPointer maxDiskBandwitdhPointer;
  private JsonPointer maxNetworkBandwidthPointer;
  private JsonPointer maxNetworkBandwitdhPointer;

  @PostConstruct
  public void init() throws NoSuchFieldException {
    String configurationJson = getJsonMappingField("configuration",
        StackGresClusterSpec.class);
    String backupsJson = getJsonMappingField("backups",
        StackGresClusterConfiguration.class);
    String performanceJson = getJsonMappingField("performance",
        StackGresClusterBackupConfiguration.class);
    String maxDiskBandwidthJson = getJsonMappingField("maxDiskBandwidth",
        StackGresBaseBackupPerformance.class);
    String maxDiskBandwitdhJson = getJsonMappingField("maxDiskBandwitdh",
        StackGresBaseBackupPerformance.class);
    String maxNetworkBandwidthJson = getJsonMappingField("maxNetworkBandwidth",
        StackGresBaseBackupPerformance.class);
    String maxNetworkBandwitdhJson = getJsonMappingField("maxNetworkBandwitdh",
        StackGresBaseBackupPerformance.class);

    backupsPointer = SPEC_POINTER
        .append(configurationJson)
        .append(backupsJson);
    maxDiskBandwidthPointer = JsonPointer.of(performanceJson).append(maxDiskBandwidthJson);
    maxDiskBandwitdhPointer = JsonPointer.of(performanceJson).append(maxDiskBandwitdhJson);
    maxNetworkBandwidthPointer = JsonPointer.of(performanceJson).append(maxNetworkBandwidthJson);
    maxNetworkBandwitdhPointer = JsonPointer.of(performanceJson).append(maxNetworkBandwitdhJson);
  }

  @Override
  public List<JsonPatchOperation> mutate(StackGresClusterReview review) {
    if (review.getRequest().getOperation() == Operation.CREATE
        || review.getRequest().getOperation() == Operation.UPDATE) {
      final List<StackGresClusterBackupConfiguration> backups =
          Optional.ofNullable(review.getRequest().getObject().getSpec().getConfiguration())
          .map(StackGresClusterConfiguration::getBackups)
          .orElse(List.of());

      ImmutableList.Builder<JsonPatchOperation> operations = ImmutableList.builder();

      Seq.seq(backups)
          .map(StackGresClusterBackupConfiguration::getPerformance)
          .zipWithIndex()
          .forEach(t -> {
            if (t.v1.getMaxDiskBandwitdh() != null) {
              operations.add(applyRemoveValue(
                  backupsPointer.append(t.v2.intValue()).append(maxDiskBandwitdhPointer)));
              if (t.v1.getMaxDiskBandwidth() == null) {
                operations.add(applyAddValue(
                    backupsPointer.append(t.v2.intValue()).append(maxDiskBandwidthPointer),
                    FACTORY.numberNode(t.v1.getMaxDiskBandwitdh())));
              }
            }
            if (t.v1.getMaxNetworkBandwitdh() != null) {
              operations.add(applyRemoveValue(
                  backupsPointer.append(t.v2.intValue()).append(maxNetworkBandwitdhPointer)));
              if (t.v1.getMaxNetworkBandwidth() == null) {
                operations.add(applyAddValue(
                    backupsPointer.append(t.v2.intValue()).append(maxNetworkBandwidthPointer),
                    FACTORY.numberNode(t.v1.getMaxNetworkBandwitdh())));
              }
            }
          });

      return operations.build();
    }

    return ImmutableList.of();
  }

}
