FROM alpine:3.22 AS builder

ARG WITH_MYSQL=false
ARG WITH_POSTGRES=false

ENV MARIADB_CONNECTOR_VERSION=3.5.6
ENV POSTGRES_CONNECTOR_VERSION=42.7.8

COPY build/distributions/helma-*.tgz /opt/helma.tgz
RUN tar -xzf /opt/helma.tgz -C /opt && \
    mv /opt/helma-*/ /opt/helma
RUN test "$WITH_MYSQL" = "true" && \
    mkdir -p /opt/helma/lib/ext && \
    curl -fsSL https://dlm.mariadb.com/4461085/Connectors/java/connector-java-${MARIADB_CONNECTOR_VERSION}/mariadb-java-client-${MARIADB_CONNECTOR_VERSION}.jar \
      -o /opt/helma/lib/ext/mariadb-java-client-${MARIADB_CONNECTOR_VERSION}.jar || true
RUN test "$WITH_POSTGRES" = "true" && \
    mkdir -p /opt/helma/lib/ext && \
    curl -fsSL https://jdbc.postgresql.org/download/postgresql-${POSTGRES_CONNECTOR_VERSION}.jar \
      -o /opt/helma/lib/ext/postgresql-${POSTGRES_CONNECTOR_VERSION}.jar || true

FROM alpine:3.22
COPY --from=builder /opt/helma /opt/helma
RUN apk add --no-cache openjdk17-jre
WORKDIR /opt/helma
CMD ["bin/helma"]
