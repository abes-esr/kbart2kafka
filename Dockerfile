###
# Image pour la compilation
FROM maven:3-eclipse-temurin-17 as build-image
WORKDIR /build/
# Installation et configuration de la locale FR
RUN apt update && DEBIAN_FRONTEND=noninteractive apt -y install locales
RUN sed -i '/fr_FR.UTF-8/s/^# //g' /etc/locale.gen && \
    locale-gen
ENV LANG fr_FR.UTF-8
ENV LANGUAGE fr_FR:fr
ENV LC_ALL fr_FR.UTF-8


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


###
# Image pour le module API
#FROM tomcat:9-jdk17 as api-image
#COPY --from=build-image /build/web/target/*.war /usr/local/tomcat/webapps/ROOT.war
#CMD [ "catalina.sh", "run" ]
FROM eclipse-temurin:17-jdk as kbart2kafka-image
RUN apt-get update
RUN apt-get install -y locales locales-all
ENV LC_ALL fr_FR.UTF-8
ENV LANG fr_FR.UTF-8
ENV LANGUAGE fr_FR.UTF-8
WORKDIR /app/
COPY --from=build-image /build/target/kbart2kafka.jar /app/kbart2kafka.jar
RUN mkdir /app/run
RUN cp /app/*.jar /app/run
RUN chmod 777 /app/run/*
RUN touch app.log
RUN chmod 777 app.log
ENV TZ=Europe/Paris
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
ENTRYPOINT ["tail","-f","app.log"]
