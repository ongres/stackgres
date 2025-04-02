ARG BASE_IMAGE

FROM scratch AS copy
  USER 1001
  WORKDIR /opt/app-root/src/
  USER 0
  COPY 'docker/LICENCE' '/licenses/stackgres'

  # Copying admin static resources to ngnix
  COPY 'target/public/' '/opt/app-root/src/admin/'

FROM "$BASE_IMAGE"
  COPY --from=copy / /

  USER root:root

  RUN cp '/usr/share/licenses/nginx/LICENSE' '/licenses/nginx' && \
    chown nginx:nginx '/opt/app-root/src' -R

  USER nginx:nginx

  ARG STACKGRES_VERSION BUILD_HASH

  RUN [ -n "$STACKGRES_VERSION" ] && \
    echo "{\"version\":\"$STACKGRES_VERSION\"}" > /opt/app-root/src/admin/info/sg-info.json

  #Expose port, set version and default user
  EXPOSE 8080

  ENV \
    STACKGRES_VERSION=$STACKGRES_VERSION

  LABEL \
    name=stackgres/admin-ui \
    maintainer="OnGres Team <stackgres@ongres.com>" \
    vendor="OnGres Inc." \
    version="$STACKGRES_VERSION" \
    release="$BUILD_HASH" \
    summary="StackGres Nginx" \
    description="StackGres Nginx" \
