# F5: This docker file uses 2 stages: build and execute
# Build has maven and creates a package of the app
FROM maven:3.9-eclipse-temurin-25-noble AS build
WORKDIR /app
# Credentials for ghcr
ARG GITHUB_USER
ARG GITHUB_TOKEN

# Create settings.xml w/ credentials
RUN mkdir -p /root/.m2 && \
    echo "<settings>\
    <servers>\
        <server>\
            <id>github</id>\
            <username>${GITHUB_USER}</username>\
            <password>${GITHUB_TOKEN}</password>\
        </server>\
    </servers>\
</settings>" > /root/.m2/settings.xml

COPY ["pom.xml", "./"]
COPY ["src", "./src"]
RUN ["mvn", "-B", "package"]
RUN rm -f /root/.m2/settings.xml

# The execute stage is smaller and copies the generated jar and executes it
# Additionally, it exposes port 8080 where the website is hosted
FROM eclipse-temurin:25-jre-noble AS run
COPY --from=build /app/target/*.jar app.jar
ENV MODEL_HOST="http://host.docker.internal:8081"
ENV SERVER_PORT=8080
ENTRYPOINT ["java", "-jar", "app.jar"]