FROM adoptopenjdk:11-jre-openj9

EXPOSE 8080/tcp

MAINTAINER Kjetil Valstadsve <taninim@vlitejk.cotse.net>

RUN mkdir -p /usr/lib
COPY mediaserver/build/libs/mediaserver-1.0-SNAPSHOT-all.jar /usr/lib/taninim.jar
WORKDIR /usr/lib
RUN rm -rf /usr/taninim

# "/Users/kjetil/FLAC/John Zorn:/var/media:ro"

VOLUME /var/media

CMD ["java", "-jar", "/usr/lib/taninim.jar", "/var/media" ]
ENTRYPOINT ["java", "-jar", "/usr/lib/taninim.jar", "/var/media" ]
