## Commit Messages

see [git-commit](https://chris.beams.io/posts/git-commit/) for an acceptable commit message structure


## QUICK START

- gitflow with default options
`git flow init -d` 
- compile and test
`sbt clean compile test`
- run
`sbt run`
- create docker image **
`sbt docker`
- create a docker image and push to remote registry **
`sbt dockerBuildAndPush`
- delete dangling docker images
`sbt removeDangling`
- release **
`sbt release`
- generate dependency graph
`sbt dependencyTree`
- run local docker image
`docker run -p 8080:8080 {ID}` where {ID} is the image id. To check that the service is up and running, goto http://localhost:8080/health

**IMPORTANT**
The release notes should be maintained in the ./notes folder with the naming convention of `${RELEASE_VERSION}.markdown`
Even though currently these are not set as the release notes on github for the released tag, it is intended to introduce a new release step to do just that. 
Hence, make sure that these exists before every new release via sbt release.

** see details below

## Details

The following sbt plugins are used 

- Git [sbt-git](https://github.com/sbt/sbt-git)

    Allows us to retrieve information about the commit such as the sha or the branch which is used to enhance the BuildInfo object

- BuildInfo [buildinfo](https://github.com/sbt/sbt-buildinfo)

    configured to generate a BuildInfo object on compile with
     - name
     - version
     - scalaVersion
     - sbtVersion
     - git.gitCurrentBranch
     - git.gitHeadCommit

    This information will be used to enhance the detailed health endpoint

- GitFlow [sbt-git-flow](https://github.com/ServiceRocket/sbt-git-flow)
    
    The gitflow plugin extends the release plugin and as such maintains the versioning in the `./version.sbt` file
    see [sbt-release](https://github.com/sbt/sbt-release) for more release options
    the [sbt-assembly](https://github.com/sbt/sbt-assembly) plugin is used to generate the fat jar in this pipeline
    see (git-flow-cheatsheet)[http://danielkummer.github.io/git-flow-cheatsheet/)

- Docker [sbt-docker](https://github.com/marcuslonnberg/sbt-docker)
    
    docker image naming convention is configured based on the fact that with gitflow we have two primary branches, master and develop
    It looks for an environment variable ${BRANCH_NAME}. If missing, its assumed that the docker image is being built for local testing

    Naming convention used: 
    - `no BRANCH_NAME` -> organization/name:latest
    - `develop` -> registry/name:version-sha
    - `master` -> registry/name:version
    
    *registry* is the remote registry where the image is to be pushed
    *version* is develop will be Major.Minor.Patch-Snapshot

- Dependency Graph [sbt-dependency-graph](https://github.com/jrudolph/sbt-dependency-graph)
    
    To generate dependency graph
    
- Format [scalafmt-github](https://github.com/scalameta/scalafmt)    

    see [scalafmt](http://scalameta.org/scalafmt/) for details

- Style [scalastyle](https://github.com/scalastyle)
    
    see [scalastyle-github](http://www.scalastyle.org/) for details

## Vagrant
Vagrantfile is defined in the project's root folder.

To check the status of the vagrant boxes defined in the file `vagrant status`

To bring up an individual box eg. the services box `vagrant up services`

## Consul
We are using Consul for service discovery using the docker image https://hub.docker.com/r/progrium/consul/

To pull the latest image `docker pull progrium/consul`

To check the installation manually `docker run -d -p 8500:8500 progrium/consul -server -bootstrap -ui-dir ui` and then go to `localhost:8500`

For local testing consul server will be deployed within the services vagrant box. This will be started with the `vagrant up services` command

The ui can then be acceessed at the {static-ip}:8500/ui where the {static-ip} is the ip defined in the vagrant file for the services box

Joining the cluster can be tested manually by running the following command from the terminal, which will connect the consul client to the server

`docker run -d -p 8300:8300 -p 8301:8301 -p 8301:8301/udp -p 8302:8302 -p 8302:8302/udp -p 8400:8400 -p 8500:8500 progrium/consul -advertise 127.0.0.1 -join {static-ip}` 
where the {static-ip} is the ip defined in the vagrant file for the services box


## LOGSTASH
If enabled in the configuration, Logstash with ES/Kibana will be used for logging.

For local testing, elastic search and kibana will be installed on the `services` box

With the services box running, kibana can be accessed at `http://{static-ip}:5601/`

To test locally, enable logstash in the configuration, and start up a local docker container running logstash which will 
forward the logs to the elastic search server running on the `services` vagrant box

`docker run -p 51515:51515 logstash -e 'input { tcp { port => 51515 codec => json_lines } } output { elasticsearch { hosts => ["192.168.1.10"]} }'`

-e flag allows us to specify the configuration as part of the command

## Extending the chassis
Microservices wishing to extend the chassis MUST define the akka {} configuration again in the conf files