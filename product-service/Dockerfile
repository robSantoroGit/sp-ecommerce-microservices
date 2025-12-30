# ============================================
# STAGE 1: Build
# ============================================
FROM maven:3.9-eclipse-temurin-21-alpine AS build

# Imposta directory di lavoro
WORKDIR /app

# Copia solo pom.xml prima per cache layer Maven dependencies
COPY pom.xml .

# Copia Maven wrapper (se usi ./mvnw)

# Download dependencies (questo layer viene cachato se pom.xml non cambia)
RUN mvn dependency:go-offline -B

# Copia tutto il codice sorgente
COPY src ./src

# Build dell'applicazione (skip test per velocità - test già eseguiti in CI/CD)
RUN mvn clean package -DskipTests

# ============================================
# STAGE 2: Runtime
# ============================================
FROM eclipse-temurin:21-jre-alpine

# Metadata
LABEL maintainer="rob@example.com"
LABEL version="1.0"
LABEL description="Product Service - E-commerce Microservices"

# Crea utente non-root per sicurezza
RUN addgroup -S spring && adduser -S spring -G spring

# Imposta directory di lavoro
WORKDIR /app

# Copia JAR dalla stage build
COPY --from=build /app/target/*.jar app.jar

# Cambia ownership del file
RUN chown spring:spring app.jar

# Cambia utente
USER spring:spring

# Espone porta 8082
EXPOSE 8082

# Healthcheck
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8082/actuator/health || exit 1

# Variabili d'ambiente con valori di default
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-Xms256m -Xmx512m"

# Comando di avvio
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]