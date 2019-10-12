FROM openjdk:13-alpine

EXPOSE 8080/tcp
MAINTAINER Kjetil Valstadsve <taninim@vlitejk.cotse.net>

RUN mkdir -p /usr/src/taninim
WORKDIR ./usr/src/taninim

RUN apk add --no-cache git

RUN git clone https://github.com/kjetilv/taninim.git
WORKDIR taninim
RUN ./gradlew shadowJar

RUN cp mediaserver/build/libs/mediaserver-1.0-SNAPSHOT-all.jar /usr/src/lib/taninim.jar
WORKDIR /usr/src/lib
RUN rm -rf /usr/src/taninim

RUN java -jar taninim.jar

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
