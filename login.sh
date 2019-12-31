#!/usr/bin/env bash

./gradlew shadowjar --build-cache
docker build -t taninim .
`~/.local/lib/aws/bin/aws ecr get-login --no-include-email --region eu-north-1`
docker tag taninim:latest 732946774009.dkr.ecr.eu-north-1.amazonaws.com/taninim:latest
docker push 732946774009.dkr.ecr.eu-north-1.amazonaws.com/taninim:latest
