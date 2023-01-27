https://www.interactivebrokers.com/api/doc.html

```
mvn spring-boot:run
```

```
docker-compose up
docker-compose -- run api mvn spring-boot:run -Dspring-boot.run.arguments=load-data
docker-compose -- run api mvn spring-boot:run -Dspring-boot.run.arguments=update-data
docker-compose -- run api mvn spring-boot:run -Dspring-boot.run.arguments=update-minute-data
docker-compose -- run api mvn spring-boot:run -Dspring-boot.run.arguments=snapshot
docker-compose -- run api mvn spring-boot:run -Dspring-boot.run.arguments=explore
docker-compose -- run api mvn spring-boot:run -Dspring-boot.run.arguments=update-means
docker-compose -- run api mvn spring-boot:run -Dspring-boot.run.arguments=fix-data
docker-compose -- run api mvn spring-boot:run -Dspring-boot.run.arguments=open-minute
docker-compose -- run api mvn spring-boot:run -Dspring-boot.run.arguments=update-symbol-list
docker-compose -- run api mvn package
docker-compose -- run api mvn clean:clean
```
