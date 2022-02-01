/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorageList;

@ApplicationScoped
public class ObjectStorageScanner extends AbstractCustomResourceScanner<
    StackGresObjectStorage, StackGresObjectStorageList> {

  @Inject
  public ObjectStorageScanner(KubernetesClient client) {
    super(client, StackGresObjectStorage.class, StackGresObjectStorageList.class);
  }

  public ObjectStorageScanner() {
    super(null, null, null);
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy();
  }
}
