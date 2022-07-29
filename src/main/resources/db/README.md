# How to run Liquibase scripts locally

## Run Mysql docker image

Go to /docker folder and run

`docker-compose up -d database`

## Run The Application With The Following Environment Variables

- `MYSQL_HOST=localhost`
- `MYSQL_PORT=3306`
- `MYSQL_USER=root`
- `MYSQL_PASSWORD=rootPassword`
- `LIQUIBASE_CONTEXTS=ddl`

If migration data should be loaded the environment variable changes to:

- `LIQUIBASE_CONTEXTS=ddl`
