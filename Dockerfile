FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY . .
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
EXPOSE 8080
# Runtime environment variables (override at `docker run` with -e).
# Do NOT bake production secrets into the image; override them at deploy time.
ENV SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/client
ENV SPRING_DATASOURCE_USERNAME=postgres
ENV SPRING_DATASOURCE_PASSWORD=12345

# Spring Boot will read environment variables like SPRING_DATASOURCE_URL
# and bind them to `spring.datasource.url` automatically.
CMD ["java", "-jar", "app.jar"]