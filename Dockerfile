# Image pour la compilation
FROM maven:3-eclipse-temurin-21 as build-image
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

FROM maven:3-eclipse-temurin-21 as kbart2kafka-builder
WORKDIR application
COPY --from=build-image /build/target/kbart2kafka.jar kbart2kafka.jar
RUN java -Djarmode=layertools -jar kbart2kafka.jar extract

FROM eclipse-temurin:21-jdk as kbart2kafka-image
RUN apt-get update
RUN apt-get install -y locales locales-all
ENV LC_ALL fr_FR.UTF-8
ENV LANG fr_FR.UTF-8
ENV LANGUAGE fr_FR.UTF-8
ENV TZ=Europe/Paris
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
WORKDIR /app/

COPY --from=kbart2kafka-builder application/dependencies/ ./
COPY --from=kbart2kafka-builder application/spring-boot-loader/ ./
COPY --from=kbart2kafka-builder application/snapshot-dependencies/ ./
COPY --from=kbart2kafka-builder application/*.jar ./kbart2kafka.jar

#ENTRYPOINT ["java","-XX:MaxRAMPercentage=75","-XX:+UseG1GC","-XX:ConcGCThreads=5","-XX:+ExitOnOutOfMemoryError","-XX:MaxGCPauseMillis=100","-jar","/app/kbart2kafka.jar"]
ENTRYPOINT ["bash"]