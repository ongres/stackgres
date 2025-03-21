/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresProperty;
import io.stackgres.common.StackGresVersion;

public enum Components {

  V_1_15(StackGresVersion.V_1_15, ComponentsV115.values()),
  V_1_16(StackGresVersion.V_1_16, ComponentsV116.values()),
  V_1_17(StackGresVersion.V_1_17, ComponentsV117.values());

  final StackGresVersion version;
  final List<ComponentWrapper> components;

  Components(StackGresVersion version, ComponentWrapper[] components) {
    this.version = version;
    this.components = Stream.of(components)
        .toList();
  }

  public StackGresVersion getVersion() {
    return version;
  }

  public Optional<Component> getComponent(StackGresComponent component) {
    return components
        .stream()
        .filter(c -> c.name().equals(component.name()))
        .map(ComponentWrapper::getComponent)
        .findAny();
  }

  public interface ComponentWrapper {

    String name();

    Component getComponent();

  }

  public enum ComponentVersionReader {
    V_1_15(new VersionReader("/versions-1.15.properties")),
    V_1_16(new VersionReader("/versions-1.16.properties")),
    V_1_17(new VersionReader("/versions.properties"));

    final VersionReader versionReader;

    ComponentVersionReader(VersionReader versionReader) {
      this.versionReader = versionReader;
    }
  }

  public enum ComponentsV117 implements ComponentWrapper {
    POSTGRESQL(new Component(ComponentVersionReader.V_1_17.versionReader, "postgresql", "pg")),
    BABELFISH(new Component(ComponentVersionReader.V_1_17.versionReader, "babelfish", "bf")),
    PATRONI(new Component(ComponentVersionReader.V_1_17.versionReader, "patroni",
        StackGresProperty.SG_IMAGE_PATRONI,
        "%1$s/ongres/patroni:v%2$s-%4$s-build-%3$s",
        new Component[] {
            Components.ComponentsV117.POSTGRESQL.getComponent(),
            Components.ComponentsV117.BABELFISH.getComponent(),
        })),
    POSTGRES_UTIL(new Component(ComponentVersionReader.V_1_17.versionReader, "postgresql",
        StackGresProperty.SG_IMAGE_POSTGRES_UTIL,
        "%1$s/ongres/postgres-util:v%2$s-build-%3$s")),
    PGBOUNCER(new Component(ComponentVersionReader.V_1_17.versionReader, "pgbouncer",
        StackGresProperty.SG_IMAGE_PGBOUNCER,
        "%1$s/ongres/pgbouncer:v%2$s-build-%3$s")),
    PROMETHEUS_POSTGRES_EXPORTER(new Component(ComponentVersionReader.V_1_17.versionReader,
        "prometheus-postgres-exporter",
        StackGresProperty.SG_IMAGE_PROMETHEUS_POSTGRES_EXPORTER,
        "%1$s/ongres/prometheus-postgres-exporter:v%2$s-build-%3$s")),
    ENVOY(new Component(ComponentVersionReader.V_1_17.versionReader, "envoy",
        StackGresProperty.SG_IMAGE_ENVOY,
        "%1$s/ongres/envoy:v%2$s-build-%3$s")),
    FLUENT_BIT(new Component(ComponentVersionReader.V_1_17.versionReader, "fluentbit",
        StackGresProperty.SG_IMAGE_FLUENT_BIT,
        "%1$s/ongres/fluentbit:v%2$s-build-%3$s")),
    FLUENTD(new Component(ComponentVersionReader.V_1_17.versionReader, "fluentd",
        StackGresProperty.SG_IMAGE_FLUENTD,
        "%1$s/ongres/fluentd:v%2$s-build-%3$s")),
    KUBECTL(new Component(ComponentVersionReader.V_1_17.versionReader, "kubectl",
        StackGresProperty.SG_IMAGE_KUBECTL,
        "%1$s/ongres/kubectl:v%2$s-build-%3$s")),
    BABELFISH_COMPASS(new Component(ComponentVersionReader.V_1_17.versionReader,
        "babelfish-compass",
        StackGresProperty.SG_IMAGE_BABELFISH_COMPASS,
        "%1$s/ongres/babelfish-compass:v%2$s-build-%3$s")),
    OTEL_COLLECTOR(new Component(ComponentVersionReader.V_1_17.versionReader,
        "otel-collector",
        StackGresProperty.SG_IMAGE_OTEL_COLLECTOR,
        "%1$s/ongres/otel-collector:v%2$s-build-%3$s"));

    final Component component;

    ComponentsV117(Component component) {
      this.component = component;
    }

    @Override
    public Component getComponent() {
      return component;
    }
  }

  public enum ComponentsV116 implements ComponentWrapper {
    POSTGRESQL(new Component(ComponentVersionReader.V_1_16.versionReader, "postgresql", "pg")),
    BABELFISH(new Component(ComponentVersionReader.V_1_16.versionReader, "babelfish", "bf")),
    PATRONI(new Component(ComponentVersionReader.V_1_16.versionReader, "patroni",
        StackGresProperty.SG_IMAGE_PATRONI,
        "%1$s/ongres/patroni:v%2$s-%4$s-build-%3$s",
        new Component[] {
            Components.ComponentsV116.POSTGRESQL.getComponent(),
            Components.ComponentsV116.BABELFISH.getComponent(),
        })),
    POSTGRES_UTIL(new Component(ComponentVersionReader.V_1_16.versionReader, "postgresql",
        StackGresProperty.SG_IMAGE_POSTGRES_UTIL,
        "%1$s/ongres/postgres-util:v%2$s-build-%3$s")),
    PGBOUNCER(new Component(ComponentVersionReader.V_1_16.versionReader, "pgbouncer",
        StackGresProperty.SG_IMAGE_PGBOUNCER,
        "%1$s/ongres/pgbouncer:v%2$s-build-%3$s")),
    PROMETHEUS_POSTGRES_EXPORTER(new Component(ComponentVersionReader.V_1_16.versionReader,
        "prometheus-postgres-exporter",
        StackGresProperty.SG_IMAGE_PROMETHEUS_POSTGRES_EXPORTER,
        "%1$s/ongres/prometheus-postgres-exporter:v%2$s-build-%3$s")),
    ENVOY(new Component(ComponentVersionReader.V_1_16.versionReader, "envoy",
        StackGresProperty.SG_IMAGE_ENVOY,
        "%1$s/ongres/envoy:v%2$s-build-%3$s")),
    FLUENT_BIT(new Component(ComponentVersionReader.V_1_16.versionReader, "fluentbit",
        StackGresProperty.SG_IMAGE_FLUENT_BIT,
        "%1$s/ongres/fluentbit:v%2$s-build-%3$s")),
    FLUENTD(new Component(ComponentVersionReader.V_1_16.versionReader, "fluentd",
        StackGresProperty.SG_IMAGE_FLUENTD,
        "%1$s/ongres/fluentd:v%2$s-build-%3$s")),
    KUBECTL(new Component(ComponentVersionReader.V_1_16.versionReader, "kubectl",
        StackGresProperty.SG_IMAGE_KUBECTL,
        "%1$s/ongres/kubectl:v%2$s-build-%3$s")),
    BABELFISH_COMPASS(new Component(ComponentVersionReader.V_1_16.versionReader,
        "babelfish-compass",
        StackGresProperty.SG_IMAGE_BABELFISH_COMPASS,
        "%1$s/ongres/babelfish-compass:v%2$s-build-%3$s")),
    OTEL_COLLECTOR(new Component(ComponentVersionReader.V_1_16.versionReader,
        "otel-collector",
        StackGresProperty.SG_IMAGE_OTEL_COLLECTOR,
        "%1$s/ongres/otel-collector:v%2$s-build-%3$s"));

    final Component component;

    ComponentsV116(Component component) {
      this.component = component;
    }

    @Override
    public Component getComponent() {
      return component;
    }
  }

  public enum ComponentsV115 implements ComponentWrapper {
    POSTGRESQL(new Component(ComponentVersionReader.V_1_15.versionReader, "postgresql", "pg")),
    BABELFISH(new Component(ComponentVersionReader.V_1_15.versionReader, "babelfish", "bf")),
    PATRONI(new Component(ComponentVersionReader.V_1_15.versionReader, "patroni",
        StackGresProperty.SG_IMAGE_PATRONI,
        "%1$s/ongres/patroni:v%2$s-%4$s-build-%3$s",
        new Component[] {
            Components.ComponentsV115.POSTGRESQL.getComponent(),
            Components.ComponentsV115.BABELFISH.getComponent(),
        })),
    POSTGRES_UTIL(new Component(ComponentVersionReader.V_1_15.versionReader, "postgresql",
        StackGresProperty.SG_IMAGE_POSTGRES_UTIL,
        "%1$s/ongres/postgres-util:v%2$s-build-%3$s")),
    PGBOUNCER(new Component(ComponentVersionReader.V_1_15.versionReader, "pgbouncer",
        StackGresProperty.SG_IMAGE_PGBOUNCER,
        "%1$s/ongres/pgbouncer:v%2$s-build-%3$s")),
    PROMETHEUS_POSTGRES_EXPORTER(new Component(ComponentVersionReader.V_1_15.versionReader,
        "prometheus-postgres-exporter",
        StackGresProperty.SG_IMAGE_PROMETHEUS_POSTGRES_EXPORTER,
        "%1$s/ongres/prometheus-postgres-exporter:v%2$s-build-%3$s")),
    ENVOY(new Component(ComponentVersionReader.V_1_15.versionReader, "envoy",
        StackGresProperty.SG_IMAGE_ENVOY,
        "%1$s/ongres/envoy:v%2$s-build-%3$s")),
    FLUENT_BIT(new Component(ComponentVersionReader.V_1_15.versionReader, "fluentbit",
        StackGresProperty.SG_IMAGE_FLUENT_BIT,
        "%1$s/ongres/fluentbit:v%2$s-build-%3$s")),
    FLUENTD(new Component(ComponentVersionReader.V_1_15.versionReader, "fluentd",
        StackGresProperty.SG_IMAGE_FLUENTD,
        "%1$s/ongres/fluentd:v%2$s-build-%3$s")),
    KUBECTL(new Component(ComponentVersionReader.V_1_15.versionReader, "kubectl",
        StackGresProperty.SG_IMAGE_KUBECTL,
        "%1$s/ongres/kubectl:v%2$s-build-%3$s")),
    BABELFISH_COMPASS(new Component(ComponentVersionReader.V_1_15.versionReader,
        "babelfish-compass",
        StackGresProperty.SG_IMAGE_BABELFISH_COMPASS,
        "%1$s/ongres/babelfish-compass:v%2$s-build-%3$s")),
    OTEL_COLLECTOR(new Component(ComponentVersionReader.V_1_15.versionReader,
        "otel-collector",
        StackGresProperty.SG_IMAGE_OTEL_COLLECTOR,
        "%1$s/ongres/otel-collector:v%2$s-build-%3$s"));

    final Component component;

    ComponentsV115(Component component) {
      this.component = component;
    }

    @Override
    public Component getComponent() {
      return component;
    }
  }

}
