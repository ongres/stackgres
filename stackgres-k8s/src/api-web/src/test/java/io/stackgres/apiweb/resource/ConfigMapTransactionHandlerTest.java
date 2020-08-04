/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.resource;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.stackgres.testutil.StringUtils;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConfigMapTransactionHandlerTest extends TransactionHandlerTest<ConfigMap> {

  @Override
  protected ResourceTransactionHandler<ConfigMap> getInstance() {
    ConfigMapTransactionHandler configMapTransactionHandler
        = new ConfigMapTransactionHandler();
    configMapTransactionHandler.setWriter(writer);
    return configMapTransactionHandler;
  }

  @Override
  protected ConfigMap getResource() {
    return new ConfigMapBuilder()
        .withNewMetadata()
        .withName("testConfigMap")
        .endMetadata()
        .withData(ImmutableMap.of("testKey", StringUtils.getRandomString()))
        .build();
  }

}