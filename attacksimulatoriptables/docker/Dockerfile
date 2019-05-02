FROM alpine

RUN apk --update add iptables bash

COPY iptables_config ./iptables_config

CMD ./iptables_config