/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.resource;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.stackgres.common.StringUtil;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SecretTransactionHandlerTest extends TransactionHandlerTest<Secret> {

  @Override
  protected ResourceTransactionHandler<Secret> getInstance() {
    SecretTransactionHandler transactionHandler = new SecretTransactionHandler();
    transactionHandler.setWriter(writer);
    return transactionHandler;
  }

  @Override
  protected Secret getResource() {
    return new SecretBuilder()
        .withNewMetadata()
        .withName("testSecret")
        .endMetadata()
        .withData(ImmutableMap.of("testKey", StringUtil.generateRandom()))
        .build();
  }

}