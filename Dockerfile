FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY src src

RUN chmod +x mvnw && ./mvnw -DskipTests clean package

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

RUN mkdir -p /app/uploads/products \
    && groupadd --system springecom \
    && useradd --system --gid springecom springecom \
    && chown -R springecom:springecom /app

USER springecom

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]