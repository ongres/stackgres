/*
 * Copyright (C) 2021 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import io.stackgres.operator.conciliation.factory.ContainerContext;
import io.stackgres.operator.conciliation.factory.PodTemplateFactory;

public interface PodTemplateFactoryDiscoverer<T extends ContainerContext> {

  PodTemplateFactory<T> discoverPodSpecFactory(T context);

}
