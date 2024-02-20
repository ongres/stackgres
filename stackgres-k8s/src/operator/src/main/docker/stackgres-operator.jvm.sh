#!/bin/sh

APP_PATH="${APP_PATH:-/app}"
if [ "$DEBUG_OPERATOR" = true ]
then
  set -x
  DEBUG_JAVA_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,address=*:8000,suspend=$([ "$DEBUG_OPERATOR_SUSPEND" = true ] && echo y || echo n)"
fi
if [ -n "$OPERATOR_LOG_LEVEL" ]
then
  APP_OPTS="$APP_OPTS -Dquarkus.log.level=$OPERATOR_LOG_LEVEL"
fi
if [ "$OPERATOR_SHOW_STACK_TRACES" = true ]
then
  APP_OPTS="$APP_OPTS -Dquarkus.log.console.format=%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c{4.}] (%t) %s%e%n"
fi
if [ "$JAVA_CDS_GENERATION" = true ]
then
  cat << 'EOF' > /tmp/ExportPrivateKey.java 
public class ExportPrivateKey
{
  public static void main(String args[]) throws Exception {
    java.security.KeyStore keystore = java.security.KeyStore.getInstance(args[1]);
    java.util.Base64.Encoder encoder = java.util.Base64.getEncoder();
    keystore.load(new java.io.FileInputStream(new java.io.File(args[0])), args[2].toCharArray());
    java.security.Key key = keystore.getKey(args[3], args[4].toCharArray());
    String encoded = encoder.encodeToString(key.getEncoded());
    java.io.FileWriter fw = new java.io.FileWriter(new java.io.File(args[5]));
    fw.write("-----BEGIN PRIVATE KEY-----\n");
    fw.write(encoded);
    fw.write("\n");
    fw.write("-----END PRIVATE KEY-----");
    fw.close();
  }
}
EOF
  keytool -genkey -keystore /tmp/tmp.jks -keyalg RSA -keysize 2048 -validity 10000 -alias app -dname "cn=Unknown, ou=Unknown, o=Unknown, c=Unknown" -storepass changeit -keypass changeit
  keytool -export -keystore /tmp/tmp.jks -storepass changeit -alias app -rfc -file /tmp/tmp.crt
  keytool -export -keystore /tmp/tmp.jks -storepass changeit -alias app -file /tmp/tmp.pub
  java /tmp/ExportPrivateKey.java /tmp/tmp.jks jks changeit app changeit /tmp/tmp.key
  export KUBERNETES_MASTER=240.0.0.1
  export DISABLE_RECONCILIATION=true
  java \
    -XX:ArchiveClassesAtExit="$APP_PATH"/quarkus-run.jsa \
    -XX:MaxRAMPercentage=75.0 \
    -Djava.net.preferIPv4Stack=true \
    -Djava.awt.headless=true \
    -Djava.util.logging.manager=org.jboss.logmanager.LogManager \
    -Dquarkus.http.ssl.certificate.files=/tmp/tmp.crt \
    -Dquarkus.http.ssl.certificate.key-files=/tmp/tmp.key \
    $JAVA_OPTS $DEBUG_JAVA_OPTS -jar "$APP_PATH"/quarkus-run.jar \
    -Dquarkus.http.host=0.0.0.0 \
    $APP_OPTS &
  PID=$!
  until curl -s localhost:8080/q/health/ready
  do
    sleep 1
  done
  kill "$PID"
  wait "$PID" || true
  exit
fi
exec java \
  -XX:SharedArchiveFile="$APP_PATH"/quarkus-run.jsa \
  -XX:MaxRAMPercentage=75.0 \
  -Djava.net.preferIPv4Stack=true \
  -Djava.awt.headless=true \
  -Djava.util.logging.manager=org.jboss.logmanager.LogManager \
  $(
    [ -z "$OPERATOR_CERT_FILE" ] || printf ' %s' "-Dquarkus.http.ssl.certificate.files=$OPERATOR_CERT_FILE"
    [ -z "$OPERATOR_KEY_FILE" ] || printf ' %s' "-Dquarkus.http.ssl.certificate.key-files=$OPERATOR_KEY_FILE"
  ) \
  $JAVA_OPTS $DEBUG_JAVA_OPTS -jar "$APP_PATH"/quarkus-run.jar \
  -Dquarkus.http.host=0.0.0.0 \
  $APP_OPTS
