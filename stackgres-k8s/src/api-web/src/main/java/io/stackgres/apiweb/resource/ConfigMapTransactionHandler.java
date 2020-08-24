/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.resource;

import javax.enterprise.context.ApplicationScoped;

import io.fabric8.kubernetes.api.model.ConfigMap;

@ApplicationScoped
public class ConfigMapTransactionHandler extends AbstractTransactionHandler<ConfigMap> {

}
