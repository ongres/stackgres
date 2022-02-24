/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.kubernetesclient.workaround;

import java.util.function.Supplier;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.SecretList;
import io.fabric8.kubernetes.client.ClientContext;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.base.HasMetadataOperation;
import io.fabric8.kubernetes.client.dsl.base.OperationContext;
import io.fabric8.kubernetes.client.dsl.internal.HasMetadataOperationsImpl;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;

public class SecretOperationsImpl extends HasMetadataOperation<Secret, SecretList, Resource<Secret>>
    implements Resource<Secret> {

  public SecretOperationsImpl(ClientContext clientContext) {
    this(clientContext, null);
  }

  public SecretOperationsImpl(ClientContext clientContext, String namespace) {
    this(HasMetadataOperationsImpl.defaultContext(clientContext).withNamespace(namespace));
  }

  public SecretOperationsImpl(OperationContext context) {
    super(context.withPlural("secrets"), Secret.class, SecretList.class);
  }

  @Override
  public SecretOperationsImpl newInstance(OperationContext context) {
    return new SecretOperationsImpl(context);
  }

  @Override
  protected Secret modifyItemForReplaceOrPatch(Supplier<Secret> currentSupplier, Secret item) {
    if (item.getStringData() != null && !item.getStringData().isEmpty()) {
      Secret modified = new SecretBuilder(item)
          .withData(item.getStringData().entrySet().stream()
              .map(entry -> Tuple.tuple(
                  entry.getKey(), ResourceUtil.encodeSecret(entry.getValue())))
              .collect(Collectors.toMap(Tuple2::v1, Tuple2::v2)))
          .withStringData(null)
          .build();
      modified.getMetadata().setManagedFields(item.getMetadata().getManagedFields());
      return modified;
    }
    return item;
  }

}
