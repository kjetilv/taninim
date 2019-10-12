FROM openjdk:13-alpine

MAINTAINER Kjetil Valstadsve <taninim@vlitejk.cotse.net>

RUN mkdir -p /usr/src/taninim
WORKDIR ./usr/src/taninim

RUN apk add --no-cache git

RUN git clone https://github.com/kjetilv/taninim.git
WORKDIR taninim
RUN ./gradlew shadowJar

EXPOSE 8080/tcp

RUN java -jar mediaserver/build/libs/mediaserver-1.0-SNAPSHOT-all.jar

#
#RUN javac Main.java
#CMD ["java", "Main"]
#
#RUN echo "deb http://archive.ubuntu.com/ubuntu trusty main universe" > /etc/apt/sources.list
#
#RUN apt-get -y update
#
#RUN mkdir taninim
#RUN git
#
#ENV MEDIA=/mnt
#
