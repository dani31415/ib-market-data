```
mvn spring-boot:run
```

```
docker-compose -- run api mvn spring-boot:run -Dspring-boot.run.arguments=load-data
docker-compose -- run api mvn spring-boot:run -Dspring-boot.run.arguments=update-data
docker-compose -- run api mvn clean:clean

```
