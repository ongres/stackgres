/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.kubernetesclient.workaround;

import java.util.function.Supplier;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.ClientContext;
import io.fabric8.kubernetes.client.dsl.base.OperationContext;
import io.fabric8.kubernetes.client.dsl.internal.HasMetadataOperationsImpl;

public class ServiceOperationsImpl
    extends io.fabric8.kubernetes.client.dsl.internal.core.v1.ServiceOperationsImpl {

  public ServiceOperationsImpl(ClientContext clientContext) {
    this(clientContext, null);
  }

  public ServiceOperationsImpl(ClientContext clientContext, String namespace) {
    this(HasMetadataOperationsImpl.defaultContext(clientContext).withNamespace(namespace));
  }

  public ServiceOperationsImpl(OperationContext context) {
    super(context);
  }

  @Override
  public ServiceOperationsImpl newInstance(OperationContext context) {
    return new ServiceOperationsImpl(context);
  }

  @Override
  protected Service modifyItemForReplaceOrPatch(Supplier<Service> currentSupplier, Service item) {
    Service modified = super.modifyItemForReplaceOrPatch(currentSupplier, item);
    modified.getMetadata().setManagedFields(item.getMetadata().getManagedFields());
    return modified;
  }

}
