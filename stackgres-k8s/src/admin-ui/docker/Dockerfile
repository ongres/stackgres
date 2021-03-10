ARG BASE_IMAGE
FROM "$BASE_IMAGE"
  USER root
  #Overriding default listen from port 80 to port 8080
  RUN sed 's/listen       80;/listen       8080;/' -i /etc/nginx/conf.d/default.conf
  RUN sed 's/listen  \[::\]:80;/listen  [::]:8080;/' -i /etc/nginx/conf.d/default.conf
  RUN sed 's/user  nginx;//' -i /etc/nginx/nginx.conf

  # Copying admin static resources to ngnix
  COPY 'target/public/' '/usr/share/nginx/html/admin/'
  RUN chown nginx:nginx '/usr/share/nginx/html/admin' -R

  #Expose port and default user
  EXPOSE 8080
  USER nginx
