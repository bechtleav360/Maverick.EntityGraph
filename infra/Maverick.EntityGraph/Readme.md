# Installation Guide

Note: The Helm chart has been adapted to allow for installation as Azure App Service. 

# Install on Kubernetes 

Copy file `values.yaml.template` to `values.yaml` and adapt

Copy file `application-properties.json.template` to `application-properties.json` and set passwords


Run command to install

`> helm upgrade --install -f values.yaml name-of-your-deployment .`   



# Configuring the secret 
If the package 