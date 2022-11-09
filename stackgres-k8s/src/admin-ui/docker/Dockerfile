ARG BASE_IMAGE
FROM "$BASE_IMAGE"
  USER root:root

  COPY 'docker/LICENCE' '/licenses/stackgres'
  RUN cp '/usr/share/licenses/nginx/LICENSE' '/licenses/nginx'

  # Copying admin static resources to ngnix
  COPY 'target/public/' '/opt/app-root/src/admin/'
  RUN chown nginx:nginx '/opt/app-root/src' -R

  #Expose port and default user
  EXPOSE 8080
  USER nginx:nginx
