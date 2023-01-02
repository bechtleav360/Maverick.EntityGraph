# Installation Guide

Note: The Helm chart has been adapted to allow for installation as Azure App Service.

# Install on Kubernetes

Copy file `values.yaml.template` to `values.yaml` and adapt

Copy file `application-properties.json.template` to `application-properties.json` and set passwords

Run command to install

`$ helm upgrade --install -f values.yaml name-of-your-deployment .`

# Configuring the secret

The images are pushed to the Github Container Registry. If it is set to private, you need to have the image pull secret
defined in the values. Create it with the following commands.

Generate a personal access token in Github with scope `package:read`

Run `$ docker login https://ghcr.io -u <User name>` to create the file `~/.docker/config.json`.

Create the secret with the following command:

``
oc create secret generic registry.ghcr.<postfix> \
--from-file=.dockerconfigjson=<path to config.json> \
--type=kubernetes.io/dockerconfigjson
``