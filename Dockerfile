FROM openjdk:11

EXPOSE 8080/tcp
MAINTAINER Kjetil Valstadsve <taninim@vlitejk.cotse.net>

RUN echo "JAVA_HOME="${JAVA_HOME}
RUN java -version

RUN mkdir -p /usr/src
WORKDIR ./usr/src

RUN git clone https://github.com/kjetilv/taninim.git
WORKDIR /usr/src/taninim
RUN ./gradlew shadowJar

RUN mkdir -p /usr/src/lib
RUN cp /usr/src/taninim/mediaserver/build/libs/mediaserver-1.0-SNAPSHOT-all.jar /usr/src/lib/taninim.jar
WORKDIR /usr/src/lib
RUN rm -rf /usr/src/taninim

VOLUME /mnt/media

CMD java -jar /usr/src/lib/taninim.jar /mnt/media
