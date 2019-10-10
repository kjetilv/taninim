FROM openjdk:13-alpine

MAINTAINER Kjetil Valstadsve <taninim@vlitejk.cotse.net>

COPY . /usr/src/myapp
WORKDIR /usr/src/myapp

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
