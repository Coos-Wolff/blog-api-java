# syntax=docker/dockerfile:1
# Stage 1 - Build
FROM eclipse-temurin:25-jdk AS build

WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
RUN ./mvnw -B dependency:go-offline
COPY src/ src/

RUN ./mvnw -B -Dmaven.test.skip=true clean package

# Version-independent jar name via glob. ARG lets CI override the path if needed.
ARG JAR_FILE=target/blog-api-*.jar
RUN cp ${JAR_FILE} target/app.jar

RUN java -Djarmode=tools -jar target/app.jar extract --layers --destination extracted

# Stage 2 - Runtime
FROM eclipse-temurin:25-jre

WORKDIR /application

RUN groupadd --system spring && useradd --system --gid spring --no-create-home spring

COPY --from=build --chown=spring:spring /app/extracted/dependencies/ ./
COPY --from=build --chown=spring:spring /app/extracted/spring-boot-loader/ ./
COPY --from=build --chown=spring:spring /app/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=spring:spring /app/extracted/application/ ./

USER spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
