# Attack simulator iptables filter

This project allows to setup iptables rules inside docker for parent machine.
Currently there are special rules to allow measuring timeouts in attack simulator.

Rules are located in `iptables_config` file. Docker image will install iptables
and execute content of this script. Note that docker container needs to be run
with special capabilities to modify machine etwork configuration.

## Local run

To run you will need to add `--cap-add=NET_ADMIN` to `docker run` and configured
ports in iptables script.

```
docker run -it \
    -p 8081:8081 -p 8082:8082 \
    --cap-add=NET_ADMIN \
    --rm tower-street/attacksimulatoriptables:staging
```

In simulator nginx configuration use location object to map port access:

```
...
  location /measure/maxtimeout {
    proxy_pass http://attacksimulatoriptables:8081/;
  }
...
```