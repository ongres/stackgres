/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory;

public interface PodTemplateFactory<T extends ContainerContext> {

  PodTemplateResult getPodTemplateSpec(T context);
}
