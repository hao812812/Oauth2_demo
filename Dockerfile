FROM eclipse-temurin:17-jdk-jammy AS build

WORKDIR /app
COPY OIDC_PIC/mvnw pom.xml ./
COPY OIDC_PIC/.mvn .mvn
RUN chmod +x mvnw
RUN ./mvnw -q -DskipTests dependency:go-offline

COPY OIDC_PIC/src src
RUN ./mvnw -DskipTests package

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

ENV JAVA_OPTS=""
EXPOSE 9090
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
