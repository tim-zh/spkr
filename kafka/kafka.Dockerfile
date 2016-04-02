FROM java:8-jre-alpine

ENV KAFKA_VERSION="0.9.0.1" \
    SCALA_VERSION="2.11"

RUN apk add --no-cache wget bash && \
    mkdir /opt && \
    wget -q -O - http://www-eu.apache.org/dist/kafka/$KAFKA_VERSION/kafka_$SCALA_VERSION-$KAFKA_VERSION.tgz | tar -xvz -C /opt && \
    mv /opt/kafka_$SCALA_VERSION-$KAFKA_VERSION /opt/kafka

WORKDIR /opt/kafka

EXPOSE 9092

ENTRYPOINT ["/opt/kafka/bin/kafka-server-start.sh"]

CMD ["config/server.properties"]