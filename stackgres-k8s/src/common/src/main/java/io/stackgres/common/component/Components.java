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

  V_1_7(StackGresVersion.V_1_7, ComponentsV17.values()),
  V_1_8(StackGresVersion.V_1_8, ComponentsV18.values()),
  V_1_9(StackGresVersion.V_1_9, ComponentsV18.values());

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
    V_1_7(new VersionReader("/versions-1.7.properties")),
    V_1_8(new VersionReader("/versions-1.8.properties")),
    V_1_9(new VersionReader("/versions.properties"));

    final VersionReader versionReader;

    ComponentVersionReader(VersionReader versionReader) {
      this.versionReader = versionReader;
    }
  }

  public enum ComponentsV19 implements ComponentWrapper {
    POSTGRESQL(new Component(ComponentVersionReader.V_1_9.versionReader, "postgresql", "pg")),
    BABELFISH(new Component(ComponentVersionReader.V_1_9.versionReader, "babelfish", "bf")),
    PATRONI(new Component(ComponentVersionReader.V_1_9.versionReader, "patroni",
        StackGresProperty.SG_IMAGE_PATRONI,
        "%1$s/ongres/patroni:v%2$s-%4$s-build-%3$s",
        new Component[] {
            Components.ComponentsV19.POSTGRESQL.getComponent(),
            Components.ComponentsV19.BABELFISH.getComponent(),
        })),
    POSTGRES_UTIL(new Component(ComponentVersionReader.V_1_9.versionReader, "postgresql",
        StackGresProperty.SG_IMAGE_POSTGRES_UTIL,
        "%1$s/ongres/postgres-util:v%2$s-build-%3$s")),
    PGBOUNCER(new Component(ComponentVersionReader.V_1_9.versionReader, "pgbouncer",
        StackGresProperty.SG_IMAGE_PGBOUNCER,
        "%1$s/ongres/pgbouncer:v%2$s-build-%3$s")),
    PROMETHEUS_POSTGRES_EXPORTER(new Component(ComponentVersionReader.V_1_9.versionReader,
        "prometheus-postgres-exporter",
        StackGresProperty.SG_IMAGE_PROMETHEUS_POSTGRES_EXPORTER,
        "%1$s/ongres/prometheus-postgres-exporter:v%2$s-build-%3$s")),
    ENVOY(new Component(ComponentVersionReader.V_1_9.versionReader, "envoy",
        StackGresProperty.SG_IMAGE_ENVOY,
        "%1$s/ongres/envoy:v%2$s-build-%3$s")),
    FLUENT_BIT(new Component(ComponentVersionReader.V_1_9.versionReader, "fluentbit",
        StackGresProperty.SG_IMAGE_FLUENT_BIT,
        "%1$s/ongres/fluentbit:v%2$s-build-%3$s")),
    FLUENTD(new Component(ComponentVersionReader.V_1_9.versionReader, "fluentd",
        StackGresProperty.SG_IMAGE_FLUENTD,
        "%1$s/ongres/fluentd:v%2$s-build-%3$s")),
    KUBECTL(new Component(ComponentVersionReader.V_1_9.versionReader, "kubectl",
        StackGresProperty.SG_IMAGE_KUBECTL,
        "%1$s/ongres/kubectl:v%2$s-build-%3$s")),
    BABELFISH_COMPASS(new Component(ComponentVersionReader.V_1_9.versionReader,
        "babelfish-compass",
        StackGresProperty.SG_IMAGE_BABELFISH_COMPASS,
        "%1$s/ongres/babelfish-compass:v%2$s-build-%3$s"));

    final Component component;

    ComponentsV19(Component component) {
      this.component = component;
    }

    @Override
    public Component getComponent() {
      return component;
    }
  }

  public enum ComponentsV18 implements ComponentWrapper {
    POSTGRESQL(new Component(ComponentVersionReader.V_1_8.versionReader, "postgresql", "pg")),
    BABELFISH(new Component(ComponentVersionReader.V_1_8.versionReader, "babelfish", "bf")),
    PATRONI(new Component(ComponentVersionReader.V_1_8.versionReader, "patroni",
        StackGresProperty.SG_IMAGE_PATRONI,
        "%1$s/ongres/patroni:v%2$s-%4$s-build-%3$s",
        new Component[] {
            Components.ComponentsV18.POSTGRESQL.getComponent(),
            Components.ComponentsV18.BABELFISH.getComponent(),
        })),
    POSTGRES_UTIL(new Component(ComponentVersionReader.V_1_8.versionReader, "postgresql",
        StackGresProperty.SG_IMAGE_POSTGRES_UTIL,
        "%1$s/ongres/postgres-util:v%2$s-build-%3$s")),
    PGBOUNCER(new Component(ComponentVersionReader.V_1_8.versionReader, "pgbouncer",
        StackGresProperty.SG_IMAGE_PGBOUNCER,
        "%1$s/ongres/pgbouncer:v%2$s-build-%3$s")),
    PROMETHEUS_POSTGRES_EXPORTER(new Component(ComponentVersionReader.V_1_8.versionReader,
        "prometheus-postgres-exporter",
        StackGresProperty.SG_IMAGE_PROMETHEUS_POSTGRES_EXPORTER,
        "%1$s/ongres/prometheus-postgres-exporter:v%2$s-build-%3$s")),
    ENVOY(new Component(ComponentVersionReader.V_1_8.versionReader, "envoy",
        StackGresProperty.SG_IMAGE_ENVOY,
        "%1$s/ongres/envoy:v%2$s-build-%3$s")),
    FLUENT_BIT(new Component(ComponentVersionReader.V_1_8.versionReader, "fluentbit",
        StackGresProperty.SG_IMAGE_FLUENT_BIT,
        "%1$s/ongres/fluentbit:v%2$s-build-%3$s")),
    FLUENTD(new Component(ComponentVersionReader.V_1_8.versionReader, "fluentd",
        StackGresProperty.SG_IMAGE_FLUENTD,
        "%1$s/ongres/fluentd:v%2$s-build-%3$s")),
    KUBECTL(new Component(ComponentVersionReader.V_1_8.versionReader, "kubectl",
        StackGresProperty.SG_IMAGE_KUBECTL,
        "%1$s/ongres/kubectl:v%2$s-build-%3$s")),
    BABELFISH_COMPASS(new Component(ComponentVersionReader.V_1_8.versionReader,
        "babelfish-compass",
        StackGresProperty.SG_IMAGE_BABELFISH_COMPASS,
        "%1$s/ongres/babelfish-compass:v%2$s-build-%3$s"));

    final Component component;

    ComponentsV18(Component component) {
      this.component = component;
    }

    @Override
    public Component getComponent() {
      return component;
    }
  }

  public enum ComponentsV17 implements ComponentWrapper {
    POSTGRESQL(new Component(ComponentVersionReader.V_1_7.versionReader, "postgresql", "pg")),
    BABELFISH(new Component(ComponentVersionReader.V_1_7.versionReader, "babelfish", "bf")),
    PATRONI(new Component(ComponentVersionReader.V_1_7.versionReader, "patroni",
        StackGresProperty.SG_IMAGE_PATRONI,
        "%1$s/ongres/patroni:v%2$s-%4$s-build-%3$s",
        new Component[] {
            Components.ComponentsV17.POSTGRESQL.getComponent(),
            Components.ComponentsV17.BABELFISH.getComponent(),
        })),
    POSTGRES_UTIL(new Component(ComponentVersionReader.V_1_7.versionReader, "postgresql",
        StackGresProperty.SG_IMAGE_POSTGRES_UTIL,
        "%1$s/ongres/postgres-util:v%2$s-build-%3$s")),
    PGBOUNCER(new Component(ComponentVersionReader.V_1_7.versionReader, "pgbouncer",
        StackGresProperty.SG_IMAGE_PGBOUNCER,
        "%1$s/ongres/pgbouncer:v%2$s-build-%3$s")),
    PROMETHEUS_POSTGRES_EXPORTER(new Component(ComponentVersionReader.V_1_7.versionReader,
        "prometheus-postgres-exporter",
        StackGresProperty.SG_IMAGE_PROMETHEUS_POSTGRES_EXPORTER,
        "%1$s/ongres/prometheus-postgres-exporter:v%2$s-build-%3$s")),
    ENVOY(new Component(ComponentVersionReader.V_1_7.versionReader, "envoy",
        StackGresProperty.SG_IMAGE_ENVOY,
        "%1$s/ongres/envoy:v%2$s-build-%3$s")),
    FLUENT_BIT(new Component(ComponentVersionReader.V_1_7.versionReader, "fluentbit",
        StackGresProperty.SG_IMAGE_FLUENT_BIT,
        "%1$s/ongres/fluentbit:v%2$s-build-%3$s")),
    FLUENTD(new Component(ComponentVersionReader.V_1_7.versionReader, "fluentd",
        StackGresProperty.SG_IMAGE_FLUENTD,
        "%1$s/ongres/fluentd:v%2$s-build-%3$s")),
    KUBECTL(new Component(ComponentVersionReader.V_1_7.versionReader, "kubectl",
        StackGresProperty.SG_IMAGE_KUBECTL,
        "%1$s/ongres/kubectl:v%2$s-build-%3$s")),
    BABELFISH_COMPASS(new Component(ComponentVersionReader.V_1_7.versionReader,
        "babelfish-compass",
        StackGresProperty.SG_IMAGE_BABELFISH_COMPASS,
        "%1$s/ongres/babelfish-compass:v%2$s-build-%3$s"));

    final Component component;

    ComponentsV17(Component component) {
      this.component = component;
    }

    @Override
    public Component getComponent() {
      return component;
    }
  }

}
