/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.component;

import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import io.stackgres.common.StackGresComponent;
import io.stackgres.common.component.Components.ComponentsV120;

/**
 * The components whose images are combined by the StackGres docir REST API (see
 * {@code SGCluster.spec.configurations.registry}). Unlike {@link Components} they are not bound to
 * an operator version but to the docir repository the images are retrieved from: the components
 * resolved through the default repository (configured in {@code SGConfig.spec.repository.url})
 * are held by {@link #DEFAULT} while a per cluster repository (configured in
 * {@code SGCluster.spec.configurations.registry.url}) gets its own instance through
 * {@link #of(Optional)}.
 *
 * <p>The patroni component is composed with the wal-g and hdrhistogram addons and the Postgres
 * flavor (the image of the patroni container) while every other addon (pgbouncer,
 * postgres-exporter, kubectl, fluent-bit, fluentd and otel-collector) is a component of its own
 * whose image combines the base image, the Postgres flavor and the addon. Components not served
 * by docir (envoy, postgres-util, babelfish-compass) fall back to the ones of the latest operator
 * version.</p>
 */
public class DocirComponents {

  private static final Map<Optional<URI>, DocirComponents> INSTANCES = new ConcurrentHashMap<>();

  private final Map<StackGresComponent, Component> components;

  public static DocirComponents of(Optional<URI> repositoryUri) {
    return INSTANCES.computeIfAbsent(repositoryUri, DocirComponents::new);
  }

  private DocirComponents(Optional<URI> repositoryUri) {
    final URI uri = repositoryUri.orElse(null);
    final Component postgresql = new Component(
        StackGresComponent.POSTGRESQL,
        new DocirVersionReader(ComponentsV120.POSTGRESQL, uri));
    final Component babelfish = new Component(
        StackGresComponent.BABELFISH,
        new DocirVersionReader(ComponentsV120.BABELFISH, uri));
    final Component walg = new Component(
        StackGresComponent.WALG,
        new DocirVersionReader(null, uri));
    final Component hdrhistogram = new Component(
        StackGresComponent.HDRHISTOGRAM,
        new DocirVersionReader(null, uri));
    final Component patroni = new Component(
        StackGresComponent.PATRONI,
        new DocirVersionReader(ComponentsV120.PATRONI, uri),
        new Component[] {
            walg,
        },
        new Component[] {
            hdrhistogram,
        },
        new Component[] {
            postgresql,
            babelfish,
        });
    this.components = Map.ofEntries(
        Map.entry(StackGresComponent.POSTGRESQL, postgresql),
        Map.entry(StackGresComponent.BABELFISH, babelfish),
        Map.entry(StackGresComponent.WALG, walg),
        Map.entry(StackGresComponent.HDRHISTOGRAM, hdrhistogram),
        Map.entry(StackGresComponent.PATRONI, patroni),
        Map.entry(StackGresComponent.PGBOUNCER, new Component(
            StackGresComponent.PGBOUNCER,
            new DocirVersionReader(ComponentsV120.PGBOUNCER, uri))),
        Map.entry(StackGresComponent.PROMETHEUS_POSTGRES_EXPORTER, new Component(
            StackGresComponent.PROMETHEUS_POSTGRES_EXPORTER,
            new DocirVersionReader(ComponentsV120.PROMETHEUS_POSTGRES_EXPORTER, uri))),
        Map.entry(StackGresComponent.KUBECTL, new Component(
            StackGresComponent.KUBECTL,
            new DocirVersionReader(ComponentsV120.KUBECTL, uri))),
        Map.entry(StackGresComponent.FLUENT_BIT, new Component(
            StackGresComponent.FLUENT_BIT,
            new DocirVersionReader(ComponentsV120.FLUENT_BIT, uri))),
        Map.entry(StackGresComponent.FLUENTD, new Component(
            StackGresComponent.FLUENTD,
            new DocirVersionReader(ComponentsV120.FLUENTD, uri))),
        Map.entry(StackGresComponent.OTEL_COLLECTOR, new Component(
            StackGresComponent.OTEL_COLLECTOR,
            new DocirVersionReader(ComponentsV120.OTEL_COLLECTOR, uri))));
  }

  public Optional<Component> getComponent(StackGresComponent component) {
    return Optional.ofNullable(components.get(component));
  }

}
