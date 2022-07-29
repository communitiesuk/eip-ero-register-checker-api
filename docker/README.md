# docker-compose
Docker Compose configuration and scripts to run the server side components locally, primarily to support front end UI development.

## Running
To run the environment you must have built the APIs to generate the latest docker images (because we don't currently push our
docker images to docker hub or similar. This may change in the future):
```
(from the project root directory)
$ ./gradlew clean check bootBuildImage
```
Once gradle has built the docker images of the API components, start the environment with the command:
```
(in this directory)
$ docker-compose up
```
