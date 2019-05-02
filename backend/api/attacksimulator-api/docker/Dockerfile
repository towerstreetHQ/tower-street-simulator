FROM openjdk:8-jre-alpine
RUN mkdir -p /opt/app
WORKDIR /opt/app

# Alows to redefine used config on target environment
# In AWS use env variables in configuration
ENV TS_CONFIG_VARIANT=staging.conf

COPY ./target/scala-2.12/attacksimulator-api.jar ./

RUN mkdir -p ./cert
COPY ./target/docker/attacksimulator-api/cert/* ./cert/

CMD java \
  -Dconfig.resource=$TS_CONFIG_VARIANT \
  -jar attacksimulator-api.jar
