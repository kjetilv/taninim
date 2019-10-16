FROM adoptopenjdk:11-jre-openj9

EXPOSE 80/tcp

MAINTAINER Kjetil Valstadsve <taninim@vlitejk.cotse.net>

RUN mkdir -p /usr/lib
COPY mediaserver/build/libs/mediaserver-1.0-SNAPSHOT-all.jar /usr/lib/taninim.jar
WORKDIR /usr/lib

CMD ["java", "-jar", "/usr/lib/taninim.jar" ]
