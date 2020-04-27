# https://github.com/ktorio/ktor-samples/blob/master/deployment/docker/Dockerfile
FROM openjdk:8-jdk-alpine AS build

# Copy our project code to the current directory and build it. This will generate a JAR file we can use later
COPY . .
RUN ./gradlew build

FROM openjdk:8-jre-alpine

# Define the user we will use in this instance to prevent using root that even in a container, can be a security risk.
ENV APPLICATION_USER ktor

# Then we add the user, create the /app folder, and give permissions to our user.
RUN adduser -D -g '' $APPLICATION_USER
RUN mkdir /app
RUN chown -R $APPLICATION_USER /app

# Marks this container to use the specified $APPLICATION_USER
USER $APPLICATION_USER

WORKDIR /app

# Copy the JAR file we built into the /app directory
COPY --from=build ./build/libs/shadow.jar .

# We launch java to execute the jar, with good defauls intended for containers.
CMD ["java", "-server", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseCGroupMemoryLimitForHeap", "-XX:InitialRAMFraction=2", "-XX:MinRAMFraction=2", "-XX:MaxRAMFraction=2", "-XX:+UseG1GC", "-XX:MaxGCPauseMillis=100", "-XX:+UseStringDeduplication", "-jar", "shadow.jar"]
