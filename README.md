## Commit Messages
https://chris.beams.io/posts/git-commit/

## Gitflow
We use gitflow for releasing. View documentation at http://danielkummer.github.io/git-flow-cheatsheet/

## Consul
We are using Consul for service discovery using the docker image https://hub.docker.com/r/progrium/consul/

To pull the latest image `docker pull progrium/consul`

To check the installation `docker run -d -p 8500:8500 progrium/consul -server -bootstrap -ui-dir ui` and then go to `localhost:8500`
