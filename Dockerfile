FROM alpine:3.22 AS builder

ENV HELMA_VERSION=25.3.1
ENV MARIADB_CONNECTOR_VERSION=3.5.6

RUN apk add --no-cache curl
RUN curl -fsSL https://code.host.antville.org/antville/helma/releases/download/${HELMA_VERSION}/helma-${HELMA_VERSION}.tgz | \
      tar -xzf - -C /opt && \
    mv /opt/helma-${HELMA_VERSION} /opt/helma
RUN mkdir -p /opt/helma/lib/ext && \
    curl -fsSL https://dlm.mariadb.com/4461085/Connectors/java/connector-java-${MARIADB_CONNECTOR_VERSION}/mariadb-java-client-${MARIADB_CONNECTOR_VERSION}.jar \
      -o /opt/helma/lib/ext/mariadb-java-client-${MARIADB_CONNECTOR_VERSION}.jar

FROM alpine:3.22
COPY --from=builder /opt/helma /opt/helma
RUN apk add --no-cache openjdk17-jre
WORKDIR /opt/helma
CMD ["bin/helma"]
