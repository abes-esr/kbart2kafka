# Image pour la compilation
FROM maven:3.9-eclipse-temurin-21 AS build-image
WORKDIR /build/
# On lance la compilation Java
# On débute par une mise en cache docker des dépendances Java
# cf https://www.baeldung.com/ops/docker-cache-maven-dependencies
COPY ./pom.xml /build/kbart2kafka/pom.xml
RUN mvn -f /build/kbart2kafka/pom.xml verify --fail-never
# et la compilation du code Java
COPY ./   /build/

RUN mvn --batch-mode \
        -Dmaven.test.skip=false \
        -Duser.timezone=Europe/Paris \
        -Duser.language=fr \
        package spring-boot:repackage

FROM maven:3-eclipse-temurin-21 AS kbart2kafka-builder
WORKDIR /application
COPY --from=build-image /build/target/kbart2kafka.jar kbart2kafka.jar
RUN java -Djarmode=layertools -jar kbart2kafka.jar extract

FROM ossyupiik/java:21.0.8 AS kbart2kafka-image
WORKDIR /app/
COPY --from=kbart2kafka-builder /application/dependencies/ ./
COPY --from=kbart2kafka-builder /application/spring-boot-loader/ ./
COPY --from=kbart2kafka-builder /application/snapshot-dependencies/ ./
COPY --from=kbart2kafka-builder /application/*.jar ./kbart2kafka.jar

ENV TZ=Europe/Paris
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

CMD ["java", "-jar", "/app/kbart2kafka.jar"]