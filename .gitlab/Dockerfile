###
# This Dockerfile is used in order to build in GitLab CI/CD the container image pushed to a Registry.
###
FROM registry.access.redhat.com/ubi8-minimal:8.0
LABEL maintainer="stackgres@ongres.com"
WORKDIR /work/
COPY target/*-runner /work/stackgres
COPY target/javalib/libsunec.so /app/library/
COPY target/cacerts /app/cert/cacerts
RUN chmod 775 /work
EXPOSE 8080
CMD ["./stackgres", "-Dquarkus.http.host=0.0.0.0", "-Djava.library.path=/app/library", "-Djavax.net.ssl.trustStore=/app/cert/cacert"]
