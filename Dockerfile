# Stage 1: Build the JAR with Maven
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app
COPY . .
RUN chmod +x ./mvnw
RUN ./mvnw clean package -DskipTests

# Stage 2: Run the application
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

ENV TZ="Asia/Kolkata"
ENV JAVA_TOOL_OPTIONS="-Djava.net.preferIPv4Stack=true -Duser.timezone=Asia/Kolkata"

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]