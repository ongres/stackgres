/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresProperty;
import io.stackgres.common.StackGresVersion;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;

public enum Components {

  V_1_18(StackGresVersion.V_1_18, ComponentsV118.values()),
  V_1_19(StackGresVersion.V_1_19, ComponentsV119.values()),
  V_1_20(StackGresVersion.V_1_20, ComponentsV120.values());

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

  public static Map<StackGresVersion, Component> getComponentVersionMap(StackGresComponent component) {
    return Seq.of(values())
        .flatMap(cs -> Stream.of(cs)
            .map(c -> c.getComponent(component))
            .flatMap(Optional::stream)
            .map(c -> Tuple.tuple(cs.getVersion(), c))
            .flatMap(t -> StackGresVersion.sameVersions(t.v1).stream().map(v -> Tuple.tuple(v, t.v2))))
        .toMap(Tuple2::v1, Tuple2::v2);
  }

  public interface ComponentWrapper {

    String name();

    Component getComponent();

  }

  public enum ComponentVersionReader {
    V_1_18(new PropertiesVersionReader.ForFile("/versions-1.18.properties")),
    V_1_19(new PropertiesVersionReader.ForFile("/versions-1.19.properties")),
    V_1_20(new PropertiesVersionReader.ForFile("/versions-1.20.properties"));

    final PropertiesVersionReader.ForFile versionReaderForFile;

    ComponentVersionReader(PropertiesVersionReader.ForFile versionReaderForFile) {
      this.versionReaderForFile = versionReaderForFile;
    }

    PropertiesVersionReader create(String prefix) {
      return new PropertiesVersionReader(
          versionReaderForFile.componentVersions,
          prefix,
          null,
          null,
          null);
    }

    PropertiesVersionReader create(
        StackGresProperty imageTemplateProperty,
        String defaultImageTemplate) {
      return new PropertiesVersionReader(
          versionReaderForFile.componentVersions,
          null,
          imageTemplateProperty,
          defaultImageTemplate,
          null);
    }

    PropertiesVersionReader create(
        StackGresProperty imageTemplateProperty,
        String defaultImageTemplate,
        StackGresProperty componentVersionProperty) {
      return new PropertiesVersionReader(
          versionReaderForFile.componentVersions,
          null,
          imageTemplateProperty,
          defaultImageTemplate,
          componentVersionProperty);
    }
  }

  public enum ComponentsV120 implements ComponentWrapper {
    POSTGRESQL(new Component(StackGresComponent.POSTGRESQL, ComponentVersionReader.V_1_20.create("pg"))),
    BABELFISH(new Component(StackGresComponent.BABELFISH, ComponentVersionReader.V_1_20.create("bf"))),
    PATRONI(new Component(StackGresComponent.PATRONI, ComponentVersionReader.V_1_20.create(
        StackGresProperty.SG_IMAGE_PATRONI,
        "%1$s/ongres/patroni:v%2$s-%4$s-build-%3$s"),
        new Component[] {
            Components.ComponentsV120.POSTGRESQL.getComponent(),
            Components.ComponentsV120.BABELFISH.getComponent(),
        })),
    POSTGRES_UTIL(new Component(
        StackGresComponent.POSTGRES_UTIL,
        ComponentVersionReader.V_1_20.create(
            StackGresProperty.SG_IMAGE_POSTGRES_UTIL,
            "%1$s/ongres/postgres-util:v%2$s-build-%3$s"))),
    PGBOUNCER(new Component(
        StackGresComponent.PGBOUNCER,
        ComponentVersionReader.V_1_20.create(
            StackGresProperty.SG_IMAGE_PGBOUNCER,
            "%1$s/ongres/pgbouncer:v%2$s-build-%3$s"))),
    PROMETHEUS_POSTGRES_EXPORTER(new Component(
        StackGresComponent.PROMETHEUS_POSTGRES_EXPORTER,
        ComponentVersionReader.V_1_20.create(
            StackGresProperty.SG_IMAGE_PROMETHEUS_POSTGRES_EXPORTER,
            "%1$s/ongres/prometheus-postgres-exporter:v%2$s-build-%3$s"))),
    ENVOY(new Component(
        StackGresComponent.ENVOY,
        ComponentVersionReader.V_1_20.create(
            StackGresProperty.SG_IMAGE_ENVOY,
            "%1$s/ongres/envoy:v%2$s-build-%3$s"))),
    FLUENT_BIT(new Component(
        StackGresComponent.FLUENT_BIT,
        ComponentVersionReader.V_1_20.create(
            StackGresProperty.SG_IMAGE_FLUENT_BIT,
            "%1$s/ongres/fluentbit:v%2$s-build-%3$s"))),
    FLUENTD(new Component(
        StackGresComponent.FLUENTD,
        ComponentVersionReader.V_1_20.create(
            StackGresProperty.SG_IMAGE_FLUENTD,
            "%1$s/ongres/fluentd:v%2$s-build-%3$s"))),
    KUBECTL(new Component(
        StackGresComponent.KUBECTL,
        ComponentVersionReader.V_1_20.create(
            StackGresProperty.SG_IMAGE_KUBECTL,
            "%1$s/ongres/kubectl:v%2$s-build-%3$s"))),
    BABELFISH_COMPASS(new Component(
        StackGresComponent.BABELFISH_COMPASS,
        ComponentVersionReader.V_1_20.create(
            StackGresProperty.SG_IMAGE_BABELFISH_COMPASS,
            "%1$s/ongres/babelfish-compass:v%2$s-build-%3$s"))),
    OTEL_COLLECTOR(new Component(
        StackGresComponent.OTEL_COLLECTOR,
        ComponentVersionReader.V_1_20.create(
            StackGresProperty.SG_IMAGE_OTEL_COLLECTOR,
            "%1$s/ongres/otel-collector:v%2$s-build-%3$s")));

    final Component component;

    ComponentsV120(Component component) {
      this.component = component;
    }

    @Override
    public Component getComponent() {
      return component;
    }
  }

  public enum ComponentsV119 implements ComponentWrapper {
    POSTGRESQL(new Component(StackGresComponent.POSTGRESQL, ComponentVersionReader.V_1_19.create("pg"))),
    BABELFISH(new Component(StackGresComponent.BABELFISH, ComponentVersionReader.V_1_19.create("bf"))),
    PATRONI(new Component(StackGresComponent.PATRONI, ComponentVersionReader.V_1_19.create(
        StackGresProperty.SG_IMAGE_PATRONI,
        "%1$s/ongres/patroni:v%2$s-%4$s-build-%3$s"),
        new Component[] {
            Components.ComponentsV119.POSTGRESQL.getComponent(),
            Components.ComponentsV119.BABELFISH.getComponent(),
        })),
    POSTGRES_UTIL(new Component(
        StackGresComponent.POSTGRES_UTIL,
        ComponentVersionReader.V_1_19.create(
            StackGresProperty.SG_IMAGE_POSTGRES_UTIL,
            "%1$s/ongres/postgres-util:v%2$s-build-%3$s"))),
    PGBOUNCER(new Component(
        StackGresComponent.PGBOUNCER,
        ComponentVersionReader.V_1_19.create(
            StackGresProperty.SG_IMAGE_PGBOUNCER,
            "%1$s/ongres/pgbouncer:v%2$s-build-%3$s"))),
    PROMETHEUS_POSTGRES_EXPORTER(new Component(
        StackGresComponent.PROMETHEUS_POSTGRES_EXPORTER,
        ComponentVersionReader.V_1_19.create(
            StackGresProperty.SG_IMAGE_PROMETHEUS_POSTGRES_EXPORTER,
            "%1$s/ongres/prometheus-postgres-exporter:v%2$s-build-%3$s"))),
    ENVOY(new Component(
        StackGresComponent.ENVOY,
        ComponentVersionReader.V_1_19.create(
            StackGresProperty.SG_IMAGE_ENVOY,
            "%1$s/ongres/envoy:v%2$s-build-%3$s"))),
    FLUENT_BIT(new Component(
        StackGresComponent.FLUENT_BIT,
        ComponentVersionReader.V_1_19.create(
            StackGresProperty.SG_IMAGE_FLUENT_BIT,
            "%1$s/ongres/fluentbit:v%2$s-build-%3$s"))),
    FLUENTD(new Component(
        StackGresComponent.FLUENTD,
        ComponentVersionReader.V_1_19.create(
            StackGresProperty.SG_IMAGE_FLUENTD,
            "%1$s/ongres/fluentd:v%2$s-build-%3$s"))),
    KUBECTL(new Component(
        StackGresComponent.KUBECTL,
        ComponentVersionReader.V_1_19.create(
            StackGresProperty.SG_IMAGE_KUBECTL,
            "%1$s/ongres/kubectl:v%2$s-build-%3$s"))),
    BABELFISH_COMPASS(new Component(
        StackGresComponent.BABELFISH_COMPASS,
        ComponentVersionReader.V_1_19.create(
            StackGresProperty.SG_IMAGE_BABELFISH_COMPASS,
            "%1$s/ongres/babelfish-compass:v%2$s-build-%3$s"))),
    OTEL_COLLECTOR(new Component(
        StackGresComponent.OTEL_COLLECTOR,
        ComponentVersionReader.V_1_19.create(
            StackGresProperty.SG_IMAGE_OTEL_COLLECTOR,
            "%1$s/ongres/otel-collector:v%2$s-build-%3$s")));

    final Component component;

    ComponentsV119(Component component) {
      this.component = component;
    }

    @Override
    public Component getComponent() {
      return component;
    }
  }

  public enum ComponentsV118 implements ComponentWrapper {
    POSTGRESQL(new Component(StackGresComponent.POSTGRESQL, ComponentVersionReader.V_1_18.create("pg"))),
    BABELFISH(new Component(StackGresComponent.BABELFISH, ComponentVersionReader.V_1_18.create("bf"))),
    PATRONI(new Component(StackGresComponent.PATRONI, ComponentVersionReader.V_1_18.create(
        StackGresProperty.SG_IMAGE_PATRONI,
        "%1$s/ongres/patroni:v%2$s-%4$s-build-%3$s"),
        new Component[] {
            Components.ComponentsV118.POSTGRESQL.getComponent(),
            Components.ComponentsV118.BABELFISH.getComponent(),
        })),
    POSTGRES_UTIL(new Component(
        StackGresComponent.POSTGRES_UTIL,
        ComponentVersionReader.V_1_18.create(
            StackGresProperty.SG_IMAGE_POSTGRES_UTIL,
            "%1$s/ongres/postgres-util:v%2$s-build-%3$s"))),
    PGBOUNCER(new Component(
        StackGresComponent.PGBOUNCER,
        ComponentVersionReader.V_1_18.create(
            StackGresProperty.SG_IMAGE_PGBOUNCER,
            "%1$s/ongres/pgbouncer:v%2$s-build-%3$s"))),
    PROMETHEUS_POSTGRES_EXPORTER(new Component(
        StackGresComponent.PROMETHEUS_POSTGRES_EXPORTER,
        ComponentVersionReader.V_1_18.create(
            StackGresProperty.SG_IMAGE_PROMETHEUS_POSTGRES_EXPORTER,
            "%1$s/ongres/prometheus-postgres-exporter:v%2$s-build-%3$s"))),
    ENVOY(new Component(
        StackGresComponent.ENVOY,
        ComponentVersionReader.V_1_18.create(
            StackGresProperty.SG_IMAGE_ENVOY,
            "%1$s/ongres/envoy:v%2$s-build-%3$s"))),
    FLUENT_BIT(new Component(
        StackGresComponent.FLUENT_BIT,
        ComponentVersionReader.V_1_18.create(
            StackGresProperty.SG_IMAGE_FLUENT_BIT,
            "%1$s/ongres/fluentbit:v%2$s-build-%3$s"))),
    FLUENTD(new Component(
        StackGresComponent.FLUENTD,
        ComponentVersionReader.V_1_18.create(
            StackGresProperty.SG_IMAGE_FLUENTD,
            "%1$s/ongres/fluentd:v%2$s-build-%3$s"))),
    KUBECTL(new Component(
        StackGresComponent.KUBECTL,
        ComponentVersionReader.V_1_18.create(
            StackGresProperty.SG_IMAGE_KUBECTL,
            "%1$s/ongres/kubectl:v%2$s-build-%3$s"))),
    BABELFISH_COMPASS(new Component(
        StackGresComponent.BABELFISH_COMPASS,
        ComponentVersionReader.V_1_18.create(
            StackGresProperty.SG_IMAGE_BABELFISH_COMPASS,
            "%1$s/ongres/babelfish-compass:v%2$s-build-%3$s"))),
    OTEL_COLLECTOR(new Component(
        StackGresComponent.OTEL_COLLECTOR,
        ComponentVersionReader.V_1_18.create(
            StackGresProperty.SG_IMAGE_OTEL_COLLECTOR,
            "%1$s/ongres/otel-collector:v%2$s-build-%3$s")));

    final Component component;

    ComponentsV118(Component component) {
      this.component = component;
    }

    @Override
    public Component getComponent() {
      return component;
    }
  }

}
