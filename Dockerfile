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
        -Dmaven.test.skip=true \
        -Duser.timezone=Europe/Paris \
        -Duser.language=fr \
        package -Passembly

FROM ossyupiik/java:21.0.8 AS kbart2kafka-image
WORKDIR /
COPY --from=build-image /build/target/kbart2kafka-distribution.tar.gz /
RUN tar xvfz kbart2kafka-distribution.tar.gz
RUN rm -f /kbart2kafka-distribution.tar.gz

ENV TZ=Europe/Paris
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

#CMD ["java", "-cp", "/kbart2kafka/lib/*", "fr.abes.kbart2kafka.Kbart2kafkaApplication"]
CMD ["tail", "-f","/dev/null"]

