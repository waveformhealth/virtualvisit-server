# https://github.com/ktorio/ktor-samples/blob/master/deployment/docker/Dockerfile
FROM openjdk:8-jdk-alpine AS build

COPY . .
RUN ./gradlew build

FROM openjdk:8-jre-alpine

# We define the user we will use in this instance to prevent using root that even in a container, can be a security risk.
ENV APPLICATION_USER ktor

# Then we add the user, create the /app folder and give permissions to our user.
RUN adduser -D -g '' $APPLICATION_USER
RUN mkdir /app
RUN chown -R $APPLICATION_USER /app

# Marks this container to use the specified $APPLICATION_USER
USER $APPLICATION_USER

WORKDIR /app

# We copy the FAT Jar we built into the /app folder
COPY --from=build ./build/libs/shadow.jar .

# We launch java to execute the jar, with good defauls intended for containers.
CMD ["java", "-server", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseCGroupMemoryLimitForHeap", "-XX:InitialRAMFraction=2", "-XX:MinRAMFraction=2", "-XX:MaxRAMFraction=2", "-XX:+UseG1GC", "-XX:MaxGCPauseMillis=100", "-XX:+UseStringDeduplication", "-jar", "shadow.jar"]
