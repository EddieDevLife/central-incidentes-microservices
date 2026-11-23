# Etapa 1: compila o projeto (não precisa ter Java nem Maven na máquina)
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src src
RUN mvn -q -B package -DskipTests

# Etapa 2: imagem final, só com o necessário para rodar
FROM eclipse-temurin:25-jre
COPY --from=build /app/target/app.jar /app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app.jar"]
