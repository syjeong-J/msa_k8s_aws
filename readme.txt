1. config server 기동 가장 먼저
명령어
> cd configserver
> ./gradlew bootRun

다만, cloud bus 사용하게 된다면 config server 전에 docker 이용해서 
rabbitMQ가 실행되야 함

2. eureka server 기동
명령어
> cd eureka
> ./gradlew bootJar
> java -jar xxxxx.jar

3. api gateway 기동
> cd apigateway
> ./gradlew bootJar
> java -jar xxxxx.jar

4. 각 서비스객체가 기동(user, product, order)
> cd user, product, order
> ./gradlew bootJar
> java -jar xxxxx.jar

ps. 특이사항
order 기동 시 kafka(비동기) - zookeeper - docker-compose

endpoint
http://localhost:port/user-service/user/signIn
http://localhost:port/product-service/product/create
http://localhost:port/order-service/order/create
