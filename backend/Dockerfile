# 1) Compila e empacota a API
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package -DskipTests

# 2) Imagem de execução só com o JRE, usuário sem privilégios
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S -u 10001 app -G app
COPY --from=build /app/target/gestao-contratos-1.0.0.jar app.jar
USER 10001
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=3s --start-period=40s CMD wget -qO- http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
