/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.shardedcluster;

import io.stackgres.common.ErrorType;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterCoordinator;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterCoordinatorConfigurations;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterShardingSphere;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterShardingSphereMode;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpec;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardingSphereModeType;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardingSphereRepositoryType;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardingType;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import jakarta.inject.Singleton;
import org.jooq.lambda.tuple.Tuple;

@Singleton
@ValidationType(ErrorType.CONSTRAINT_VIOLATION)
public class ShardingSphereValidator implements ShardedClusterValidator {

  private final String constraintViolationUri = ErrorType
      .getErrorTypeUri(ErrorType.CONSTRAINT_VIOLATION);

  private final String typeField;

  private final String shardingSphereField;

  private final String coordinatorInstancesField;

  private final String shardingSphereModeTypeField;

  private final String shardingSphereRepositoryTypeField;

  @SuppressWarnings("unchecked")
  public ShardingSphereValidator() {
    typeField = getFieldPath(
        StackGresShardedCluster.class, "spec",
        StackGresShardedClusterSpec.class, "type");
    coordinatorInstancesField = getFieldPath(
        StackGresShardedCluster.class, "spec",
        StackGresShardedClusterSpec.class, "coordinator",
        StackGresClusterSpec.class, "instances");
    shardingSphereField = getFieldPath(
        StackGresShardedCluster.class, "spec",
        StackGresShardedClusterSpec.class, "coordinator",
        StackGresShardedClusterCoordinator.class, "configurationsForCoordinator",
        StackGresShardedClusterCoordinatorConfigurations.class, "shardingSphere");
    shardingSphereModeTypeField = getFieldPath(
        Tuple.tuple(StackGresShardedCluster.class, "spec"),
        Tuple.tuple(StackGresShardedClusterSpec.class, "coordinator"),
        Tuple.tuple(StackGresShardedClusterCoordinator.class, "configurationsForCoordinator"),
        Tuple.tuple(StackGresShardedClusterCoordinatorConfigurations.class, "shardingSphere"),
        Tuple.tuple(StackGresShardedClusterShardingSphere.class, "mode"),
        Tuple.tuple(StackGresShardedClusterShardingSphereMode.class, "type"));
    shardingSphereRepositoryTypeField = getFieldPath(
        Tuple.tuple(StackGresShardedCluster.class, "spec"),
        Tuple.tuple(StackGresShardedClusterSpec.class, "coordinator"),
        Tuple.tuple(StackGresShardedClusterCoordinator.class, "configurationsForCoordinator"),
        Tuple.tuple(StackGresShardedClusterCoordinatorConfigurations.class, "shardingSphere"),
        Tuple.tuple(StackGresShardedClusterShardingSphere.class, "mode"),
        Tuple.tuple(StackGresShardedClusterShardingSphereMode.class, "repository"));
  }

  @Override
  public void validate(StackGresShardedClusterReview review) throws ValidationFailed {
    StackGresShardedCluster cluster = review.getRequest().getObject();
    if (review.getRequest().getOperation() == Operation.UPDATE
        || review.getRequest().getOperation() == Operation.CREATE) {
      if (!StackGresShardingType.SHARDING_SPHERE.toString().equals(
          cluster.getSpec().getType())) {
        return;
      }
      if (cluster.getSpec().getCoordinator()
          .getConfigurationsForCoordinator().getShardingSphere() == null) {
        failWithReasonAndFields(constraintViolationUri,
            "shardingSphere section can not be empty when sharding type is shardingsphere",
            typeField,
            shardingSphereField);
      }
      if (StackGresShardingSphereModeType.STANDALONE.toString().equals(
          cluster.getSpec().getCoordinator()
          .getConfigurationsForCoordinator().getShardingSphere().getMode().getType())
          && cluster.getSpec().getCoordinator().getInstances() > 1) {
        failWithReasonAndFields(constraintViolationUri,
            "Standalone mode can not be set with more than 1 coordinator instances",
            shardingSphereModeTypeField,
            coordinatorInstancesField);
      }
      if (StackGresShardingSphereModeType.STANDALONE.equals(
          StackGresShardingSphereModeType.fromString(cluster.getSpec().getCoordinator()
              .getConfigurationsForCoordinator().getShardingSphere().getMode().getType()))
          && !StackGresShardingSphereRepositoryType.MEMORY.toString().equals(
              cluster.getSpec().getCoordinator()
              .getConfigurationsForCoordinator().getShardingSphere().getMode()
              .getRepository().getType())) {
        failWithReasonAndFields(constraintViolationUri,
            "Standalone mode can only be used with repository type Memory",
            shardingSphereModeTypeField,
            shardingSphereRepositoryTypeField);
      }
      if (StackGresShardingSphereModeType.CLUSTER.equals(
          StackGresShardingSphereModeType.fromString(cluster.getSpec().getCoordinator()
              .getConfigurationsForCoordinator().getShardingSphere().getMode().getType()))
          && StackGresShardingSphereRepositoryType.MEMORY.equals(
              StackGresShardingSphereRepositoryType.fromString(cluster.getSpec().getCoordinator()
                  .getConfigurationsForCoordinator().getShardingSphere().getMode()
                  .getRepository().getType()))) {
        failWithReasonAndFields(constraintViolationUri,
            "Cluster mode can not be used with repository type Memory",
            shardingSphereModeTypeField,
            shardingSphereRepositoryTypeField);
      }
    }
  }

}
