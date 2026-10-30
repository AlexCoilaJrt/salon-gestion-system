# Etapa 1: Compilación de la aplicación con Maven y JDK 21
FROM maven:3.9.8-eclipse-temurin-21-alpine AS builder
WORKDIR /build

# Copiar descriptor de dependencias primero para aprovechar caché de Docker
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# Copiar el código fuente y compilar
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa 2: Imagen final ultra-liviana con solo JRE 21 (sin compilador ni Maven)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Crear usuario no root por seguridad
RUN addgroup -S appgroup && adduser -S appuser -G appgroup     && mkdir -p /app/keys && chown appuser:appgroup /app/keys
USER appuser

# Copiar el artefacto generado desde la etapa de compilación
COPY --from=builder /build/target/*.jar app.jar

# Exponer el puerto del servidor Spring Boot
EXPOSE 8080

# Parámetros optimizados para ejecución en contenedores
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
