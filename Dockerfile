# ---- build stage ----
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

COPY src/ src/
RUN ./mvnw -B clean package -DskipTests

# ---- run stage ----
# se la 25-jre non fosse disponibile, sostituire con eclipse-temurin:25-jdk
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Render inietta la propria PORT a runtime; in locale resta 8000
EXPOSE 8000
ENTRYPOINT ["java", "-jar", "app.jar"]