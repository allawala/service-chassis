## Current Release Version
1.0.4

## Commit Messages

see [git-commit](https://chris.beams.io/posts/git-commit/) for an acceptable commit message structure


## QUICK START

- gitflow with default options
`git flow init -d` 
- compile and test
`sbt clean compile test`
- run
`sbt run` and then goto http://localhost:8080/health
- create docker image **
`sbt docker`
- create a docker image and push to remote registry **
`sbt dockerBuildAndPush`
- delete dangling docker images
`sbt removeDangling`
- release **
`sbt release`
- publish to s3
`sbt publish`
- generate dependency graph
`sbt dependencyTree`
- run local docker image
`docker run -p 8080:8080 {ID}` where {ID} is the image id. To check that the service is up and running, goto http://localhost:8080/health

** see details below

## Chassis Documentation
[Documentation](https://allawala.github.io/service-chassis/)

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

- s3 Resolver [fm-sbt-s3-resolver](https://github.com/frugalmechanic/fm-sbt-s3-resolver)
    s3 will be used to host the snapshot and release jars


## Documentation
Uses gh-pages branch

`jekyll serve --watch`