/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresProperty;
import io.stackgres.common.StackGresVersion;

public enum Components {

  V_1_0(StackGresVersion.V_1_0, ComponentsV10.values()),
  V_1_1(StackGresVersion.V_1_1, ComponentsV11.values()),
  V_1_2(StackGresVersion.V_1_2, ComponentsV12.values()),
  V_1_3(StackGresVersion.V_1_3, ComponentsV13.values());

  final StackGresVersion version;
  final List<ComponentWrapper> components;

  Components(StackGresVersion version, ComponentWrapper[] components) {
    this.version = version;
    this.components = Stream.of(components)
        .collect(ImmutableList.toImmutableList());
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
    V_1_0(new VersionReader("/versions-1.0.properties")),
    V_1_1(new VersionReader("/versions-1.1.properties")),
    V_1_2(new VersionReader("/versions-1.2.properties")),
    V_1_3(new VersionReader("/versions.properties"));

    final VersionReader versionReader;

    ComponentVersionReader(VersionReader versionReader) {
      this.versionReader = versionReader;
    }
  }

  public enum ComponentsV13 implements ComponentWrapper {
    POSTGRESQL(new Component(ComponentVersionReader.V_1_3.versionReader, "postgresql", "pg")),
    BABELFISH(new Component(ComponentVersionReader.V_1_3.versionReader, "babelfish", "bf")),
    PATRONI(new Component(ComponentVersionReader.V_1_3.versionReader, "patroni",
        StackGresProperty.SG_IMAGE_PATRONI,
        "%1$s/ongres/patroni:v%2$s-%4$s-build-%3$s",
        new Component[] {
            Components.ComponentsV13.POSTGRESQL.getComponent(),
            Components.ComponentsV13.BABELFISH.getComponent(),
        })),
    POSTGRES_UTIL(new Component(ComponentVersionReader.V_1_3.versionReader, "postgresql",
        StackGresProperty.SG_IMAGE_POSTGRES_UTIL,
        "%1$s/ongres/postgres-util:v%2$s-build-%3$s")),
    PGBOUNCER(new Component(ComponentVersionReader.V_1_3.versionReader, "pgbouncer",
        StackGresProperty.SG_IMAGE_PGBOUNCER,
        "%1$s/ongres/pgbouncer:v%2$s-build-%3$s")),
    PROMETHEUS_POSTGRES_EXPORTER(new Component(ComponentVersionReader.V_1_3.versionReader,
        "prometheus-postgres-exporter",
        StackGresProperty.SG_IMAGE_PROMETHEUS_POSTGRES_EXPORTER,
        "%1$s/ongres/prometheus-postgres-exporter:v%2$s-build-%3$s")),
    ENVOY(new Component(ComponentVersionReader.V_1_3.versionReader, "envoy",
        StackGresProperty.SG_IMAGE_ENVOY,
        "%1$s/ongres/envoy:v%2$s-build-%3$s")),
    FLUENT_BIT(new Component(ComponentVersionReader.V_1_3.versionReader, "fluentbit",
        StackGresProperty.SG_IMAGE_FLUENT_BIT,
        "%1$s/ongres/fluentbit:v%2$s-build-%3$s")),
    FLUENTD(new Component(ComponentVersionReader.V_1_3.versionReader, "fluentd",
        StackGresProperty.SG_IMAGE_FLUENTD,
        "%1$s/ongres/fluentd:v%2$s-build-%3$s")),
    KUBECTL(new Component(ComponentVersionReader.V_1_3.versionReader, "kubectl",
        StackGresProperty.SG_IMAGE_KUBECTL,
        "%1$s/ongres/kubectl:v%2$s-build-%3$s")),
    BABELFISH_COMPASS(new Component(ComponentVersionReader.V_1_3.versionReader,
        "babelfish-compass",
        StackGresProperty.SG_IMAGE_BABELFISH_COMPASS,
        "%1$s/ongres/babelfish-compass:v%2$s-build-%3$s"));

    final Component component;

    ComponentsV13(Component component) {
      this.component = component;
    }

    @Override
    public Component getComponent() {
      return component;
    }
  }

  public enum ComponentsV12 implements ComponentWrapper {
    POSTGRESQL(new Component(ComponentVersionReader.V_1_2.versionReader, "postgresql", "pg")),
    BABELFISH(new Component(ComponentVersionReader.V_1_2.versionReader, "babelfish", "bf")),
    PATRONI(new Component(ComponentVersionReader.V_1_2.versionReader, "patroni",
        StackGresProperty.SG_IMAGE_PATRONI,
        "%1$s/ongres/patroni:v%2$s-%4$s-build-%3$s",
        new Component[] {
            Components.ComponentsV12.POSTGRESQL.getComponent(),
            Components.ComponentsV12.BABELFISH.getComponent(),
        })),
    POSTGRES_UTIL(new Component(ComponentVersionReader.V_1_2.versionReader, "postgresql",
        StackGresProperty.SG_IMAGE_POSTGRES_UTIL,
        "%1$s/ongres/postgres-util:v%2$s-build-%3$s")),
    PGBOUNCER(new Component(ComponentVersionReader.V_1_2.versionReader, "pgbouncer",
        StackGresProperty.SG_IMAGE_PGBOUNCER,
        "%1$s/ongres/pgbouncer:v%2$s-build-%3$s")),
    PROMETHEUS_POSTGRES_EXPORTER(new Component(ComponentVersionReader.V_1_2.versionReader,
        "prometheus-postgres-exporter",
        StackGresProperty.SG_IMAGE_PROMETHEUS_POSTGRES_EXPORTER,
        "%1$s/ongres/prometheus-postgres-exporter:v%2$s-build-%3$s")),
    ENVOY(new Component(ComponentVersionReader.V_1_2.versionReader, "envoy",
        StackGresProperty.SG_IMAGE_ENVOY,
        "%1$s/ongres/envoy:v%2$s-build-%3$s")),
    FLUENT_BIT(new Component(ComponentVersionReader.V_1_2.versionReader, "fluentbit",
        StackGresProperty.SG_IMAGE_FLUENT_BIT,
        "%1$s/ongres/fluentbit:v%2$s-build-%3$s")),
    FLUENTD(new Component(ComponentVersionReader.V_1_2.versionReader, "fluentd",
        StackGresProperty.SG_IMAGE_FLUENTD,
        "%1$s/ongres/fluentd:v%2$s-build-%3$s")),
    KUBECTL(new Component(ComponentVersionReader.V_1_2.versionReader, "kubectl",
        StackGresProperty.SG_IMAGE_KUBECTL,
        "%1$s/ongres/kubectl:v%2$s-build-%3$s")),
    BABELFISH_COMPASS(new Component(ComponentVersionReader.V_1_2.versionReader,
        "babelfish-compass",
        StackGresProperty.SG_IMAGE_BABELFISH_COMPASS,
        "%1$s/ongres/babelfish-compass:v%2$s-build-%3$s"));

    final Component component;

    ComponentsV12(Component component) {
      this.component = component;
    }

    @Override
    public Component getComponent() {
      return component;
    }
  }

  public enum ComponentsV11 implements ComponentWrapper {
    POSTGRESQL(new Component(ComponentVersionReader.V_1_1.versionReader, "postgresql", "pg")),
    BABELFISH(new Component(ComponentVersionReader.V_1_1.versionReader, "babelfish", "bf")),
    PATRONI(new Component(ComponentVersionReader.V_1_1.versionReader, "patroni",
        StackGresProperty.SG_IMAGE_PATRONI,
        "%1$s/ongres/patroni:v%2$s-%4$s-build-%3$s",
        new Component[] {
            Components.ComponentsV11.POSTGRESQL.getComponent(),
            Components.ComponentsV11.BABELFISH.getComponent(),
        })),
    POSTGRES_UTIL(new Component(ComponentVersionReader.V_1_1.versionReader, "postgresql",
        StackGresProperty.SG_IMAGE_POSTGRES_UTIL,
        "%1$s/ongres/postgres-util:v%2$s-build-%3$s")),
    PGBOUNCER(new Component(ComponentVersionReader.V_1_1.versionReader, "pgbouncer",
        StackGresProperty.SG_IMAGE_PGBOUNCER,
        "%1$s/ongres/pgbouncer:v%2$s-build-%3$s")),
    PROMETHEUS_POSTGRES_EXPORTER(new Component(ComponentVersionReader.V_1_1.versionReader,
        "prometheus-postgres-exporter",
        StackGresProperty.SG_IMAGE_PROMETHEUS_POSTGRES_EXPORTER,
        "%1$s/ongres/prometheus-postgres-exporter:v%2$s-build-%3$s")),
    ENVOY(new Component(ComponentVersionReader.V_1_1.versionReader, "envoy",
        StackGresProperty.SG_IMAGE_ENVOY,
        "%1$s/ongres/envoy:v%2$s-build-%3$s")),
    FLUENT_BIT(new Component(ComponentVersionReader.V_1_1.versionReader, "fluentbit",
        StackGresProperty.SG_IMAGE_FLUENT_BIT,
        "%1$s/ongres/fluentbit:v%2$s-build-%3$s")),
    FLUENTD(new Component(ComponentVersionReader.V_1_1.versionReader, "fluentd",
        StackGresProperty.SG_IMAGE_FLUENTD,
        "%1$s/ongres/fluentd:v%2$s-build-%3$s")),
    KUBECTL(new Component(ComponentVersionReader.V_1_1.versionReader, "kubectl",
        StackGresProperty.SG_IMAGE_KUBECTL,
        "%1$s/ongres/kubectl:v%2$s-build-%3$s"));

    final Component component;

    ComponentsV11(Component component) {
      this.component = component;
    }

    @Override
    public Component getComponent() {
      return component;
    }
  }

  public enum ComponentsV10 implements ComponentWrapper {
    POSTGRESQL(new Component(ComponentVersionReader.V_1_0.versionReader, "postgresql", "pg")),
    PATRONI(new Component(ComponentVersionReader.V_1_0.versionReader, "patroni",
        StackGresProperty.SG_IMAGE_PATRONI,
        "%1$s/ongres/patroni:v%2$s-%4$s-build-%3$s",
        new Component[] {
            Components.ComponentsV10.POSTGRESQL.getComponent(),
        })),
    POSTGRES_UTIL(new Component(ComponentVersionReader.V_1_0.versionReader, "postgresql",
        StackGresProperty.SG_IMAGE_POSTGRES_UTIL,
        "%1$s/ongres/postgres-util:v%2$s-build-%3$s")),
    PGBOUNCER(new Component(ComponentVersionReader.V_1_0.versionReader, "pgbouncer",
        StackGresProperty.SG_IMAGE_PGBOUNCER,
        "%1$s/ongres/pgbouncer:v%2$s-build-%3$s")),
    PROMETHEUS_POSTGRES_EXPORTER(new Component(ComponentVersionReader.V_1_0.versionReader,
        "prometheus-postgres-exporter",
        StackGresProperty.SG_IMAGE_PROMETHEUS_POSTGRES_EXPORTER,
        "%1$s/ongres/prometheus-postgres-exporter:v%2$s-build-%3$s")),
    ENVOY(new Component(ComponentVersionReader.V_1_0.versionReader, "envoy",
        StackGresProperty.SG_IMAGE_ENVOY,
        "%1$s/ongres/envoy:v%2$s-build-%3$s")),
    FLUENT_BIT(new Component(ComponentVersionReader.V_1_0.versionReader, "fluentbit",
        StackGresProperty.SG_IMAGE_FLUENT_BIT,
        "%1$s/ongres/fluentbit:v%2$s-build-%3$s")),
    FLUENTD(new Component(ComponentVersionReader.V_1_0.versionReader, "fluentd",
        StackGresProperty.SG_IMAGE_FLUENTD,
        "%1$s/ongres/fluentd:v%2$s-build-%3$s")),
    KUBECTL(new Component(ComponentVersionReader.V_1_0.versionReader, "kubectl",
        StackGresProperty.SG_IMAGE_KUBECTL,
        "%1$s/ongres/kubectl:v%2$s-build-%3$s"));

    final Component component;

    ComponentsV10(Component component) {
      this.component = component;
    }

    @Override
    public Component getComponent() {
      return component;
    }
  }

}
