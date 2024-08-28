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

  V_1_12(StackGresVersion.V_1_12, ComponentsV112.values()),
  V_1_13(StackGresVersion.V_1_13, ComponentsV113.values()),
  V_1_14(StackGresVersion.V_1_14, ComponentsV114.values());

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
    V_1_12(new VersionReader("/versions-1.12.properties")),
    V_1_13(new VersionReader("/versions-1.13.properties")),
    V_1_14(new VersionReader("/versions.properties"));

    final VersionReader versionReader;

    ComponentVersionReader(VersionReader versionReader) {
      this.versionReader = versionReader;
    }
  }

  public enum ComponentsV114 implements ComponentWrapper {
    POSTGRESQL(new Component(ComponentVersionReader.V_1_14.versionReader, "postgresql", "pg")),
    BABELFISH(new Component(ComponentVersionReader.V_1_14.versionReader, "babelfish", "bf")),
    PATRONI(new Component(ComponentVersionReader.V_1_14.versionReader, "patroni",
        StackGresProperty.SG_IMAGE_PATRONI,
        "%1$s/ongres/patroni:v%2$s-%4$s-build-%3$s",
        new Component[] {
            Components.ComponentsV114.POSTGRESQL.getComponent(),
            Components.ComponentsV114.BABELFISH.getComponent(),
        })),
    POSTGRES_UTIL(new Component(ComponentVersionReader.V_1_14.versionReader, "postgresql",
        StackGresProperty.SG_IMAGE_POSTGRES_UTIL,
        "%1$s/ongres/postgres-util:v%2$s-build-%3$s")),
    PGBOUNCER(new Component(ComponentVersionReader.V_1_14.versionReader, "pgbouncer",
        StackGresProperty.SG_IMAGE_PGBOUNCER,
        "%1$s/ongres/pgbouncer:v%2$s-build-%3$s")),
    PROMETHEUS_POSTGRES_EXPORTER(new Component(ComponentVersionReader.V_1_14.versionReader,
        "prometheus-postgres-exporter",
        StackGresProperty.SG_IMAGE_PROMETHEUS_POSTGRES_EXPORTER,
        "%1$s/ongres/prometheus-postgres-exporter:v%2$s-build-%3$s")),
    ENVOY(new Component(ComponentVersionReader.V_1_14.versionReader, "envoy",
        StackGresProperty.SG_IMAGE_ENVOY,
        "%1$s/ongres/envoy:v%2$s-build-%3$s")),
    FLUENT_BIT(new Component(ComponentVersionReader.V_1_14.versionReader, "fluentbit",
        StackGresProperty.SG_IMAGE_FLUENT_BIT,
        "%1$s/ongres/fluentbit:v%2$s-build-%3$s")),
    FLUENTD(new Component(ComponentVersionReader.V_1_14.versionReader, "fluentd",
        StackGresProperty.SG_IMAGE_FLUENTD,
        "%1$s/ongres/fluentd:v%2$s-build-%3$s")),
    KUBECTL(new Component(ComponentVersionReader.V_1_14.versionReader, "kubectl",
        StackGresProperty.SG_IMAGE_KUBECTL,
        "%1$s/ongres/kubectl:v%2$s-build-%3$s")),
    BABELFISH_COMPASS(new Component(ComponentVersionReader.V_1_14.versionReader,
        "babelfish-compass",
        StackGresProperty.SG_IMAGE_BABELFISH_COMPASS,
        "%1$s/ongres/babelfish-compass:v%2$s-build-%3$s"));

    final Component component;

    ComponentsV114(Component component) {
      this.component = component;
    }

    @Override
    public Component getComponent() {
      return component;
    }
  }

  public enum ComponentsV113 implements ComponentWrapper {
    POSTGRESQL(new Component(ComponentVersionReader.V_1_13.versionReader, "postgresql", "pg")),
    BABELFISH(new Component(ComponentVersionReader.V_1_13.versionReader, "babelfish", "bf")),
    PATRONI(new Component(ComponentVersionReader.V_1_13.versionReader, "patroni",
        StackGresProperty.SG_IMAGE_PATRONI,
        "%1$s/ongres/patroni:v%2$s-%4$s-build-%3$s",
        new Component[] {
            Components.ComponentsV113.POSTGRESQL.getComponent(),
            Components.ComponentsV113.BABELFISH.getComponent(),
        })),
    POSTGRES_UTIL(new Component(ComponentVersionReader.V_1_13.versionReader, "postgresql",
        StackGresProperty.SG_IMAGE_POSTGRES_UTIL,
        "%1$s/ongres/postgres-util:v%2$s-build-%3$s")),
    PGBOUNCER(new Component(ComponentVersionReader.V_1_13.versionReader, "pgbouncer",
        StackGresProperty.SG_IMAGE_PGBOUNCER,
        "%1$s/ongres/pgbouncer:v%2$s-build-%3$s")),
    PROMETHEUS_POSTGRES_EXPORTER(new Component(ComponentVersionReader.V_1_13.versionReader,
        "prometheus-postgres-exporter",
        StackGresProperty.SG_IMAGE_PROMETHEUS_POSTGRES_EXPORTER,
        "%1$s/ongres/prometheus-postgres-exporter:v%2$s-build-%3$s")),
    ENVOY(new Component(ComponentVersionReader.V_1_13.versionReader, "envoy",
        StackGresProperty.SG_IMAGE_ENVOY,
        "%1$s/ongres/envoy:v%2$s-build-%3$s")),
    FLUENT_BIT(new Component(ComponentVersionReader.V_1_13.versionReader, "fluentbit",
        StackGresProperty.SG_IMAGE_FLUENT_BIT,
        "%1$s/ongres/fluentbit:v%2$s-build-%3$s")),
    FLUENTD(new Component(ComponentVersionReader.V_1_13.versionReader, "fluentd",
        StackGresProperty.SG_IMAGE_FLUENTD,
        "%1$s/ongres/fluentd:v%2$s-build-%3$s")),
    KUBECTL(new Component(ComponentVersionReader.V_1_13.versionReader, "kubectl",
        StackGresProperty.SG_IMAGE_KUBECTL,
        "%1$s/ongres/kubectl:v%2$s-build-%3$s")),
    BABELFISH_COMPASS(new Component(ComponentVersionReader.V_1_13.versionReader,
        "babelfish-compass",
        StackGresProperty.SG_IMAGE_BABELFISH_COMPASS,
        "%1$s/ongres/babelfish-compass:v%2$s-build-%3$s"));

    final Component component;

    ComponentsV113(Component component) {
      this.component = component;
    }

    @Override
    public Component getComponent() {
      return component;
    }
  }

  public enum ComponentsV112 implements ComponentWrapper {
    POSTGRESQL(new Component(ComponentVersionReader.V_1_12.versionReader, "postgresql", "pg")),
    BABELFISH(new Component(ComponentVersionReader.V_1_12.versionReader, "babelfish", "bf")),
    PATRONI(new Component(ComponentVersionReader.V_1_12.versionReader, "patroni",
        StackGresProperty.SG_IMAGE_PATRONI,
        "%1$s/ongres/patroni:v%2$s-%4$s-build-%3$s",
        new Component[] {
            Components.ComponentsV112.POSTGRESQL.getComponent(),
            Components.ComponentsV112.BABELFISH.getComponent(),
        })),
    POSTGRES_UTIL(new Component(ComponentVersionReader.V_1_12.versionReader, "postgresql",
        StackGresProperty.SG_IMAGE_POSTGRES_UTIL,
        "%1$s/ongres/postgres-util:v%2$s-build-%3$s")),
    PGBOUNCER(new Component(ComponentVersionReader.V_1_12.versionReader, "pgbouncer",
        StackGresProperty.SG_IMAGE_PGBOUNCER,
        "%1$s/ongres/pgbouncer:v%2$s-build-%3$s")),
    PROMETHEUS_POSTGRES_EXPORTER(new Component(ComponentVersionReader.V_1_12.versionReader,
        "prometheus-postgres-exporter",
        StackGresProperty.SG_IMAGE_PROMETHEUS_POSTGRES_EXPORTER,
        "%1$s/ongres/prometheus-postgres-exporter:v%2$s-build-%3$s")),
    ENVOY(new Component(ComponentVersionReader.V_1_12.versionReader, "envoy",
        StackGresProperty.SG_IMAGE_ENVOY,
        "%1$s/ongres/envoy:v%2$s-build-%3$s")),
    FLUENT_BIT(new Component(ComponentVersionReader.V_1_12.versionReader, "fluentbit",
        StackGresProperty.SG_IMAGE_FLUENT_BIT,
        "%1$s/ongres/fluentbit:v%2$s-build-%3$s")),
    FLUENTD(new Component(ComponentVersionReader.V_1_12.versionReader, "fluentd",
        StackGresProperty.SG_IMAGE_FLUENTD,
        "%1$s/ongres/fluentd:v%2$s-build-%3$s")),
    KUBECTL(new Component(ComponentVersionReader.V_1_12.versionReader, "kubectl",
        StackGresProperty.SG_IMAGE_KUBECTL,
        "%1$s/ongres/kubectl:v%2$s-build-%3$s")),
    BABELFISH_COMPASS(new Component(ComponentVersionReader.V_1_12.versionReader,
        "babelfish-compass",
        StackGresProperty.SG_IMAGE_BABELFISH_COMPASS,
        "%1$s/ongres/babelfish-compass:v%2$s-build-%3$s"));

    final Component component;

    ComponentsV112(Component component) {
      this.component = component;
    }

    @Override
    public Component getComponent() {
      return component;
    }
  }

}
