# ---------- frontend: build + export ----------
FROM node:20-alpine AS frontend
WORKDIR /frontend

COPY frontend/package*.json ./
RUN npm ci

COPY frontend/ ./
RUN npm run build

# ---------- backend: build jar ----------
FROM maven:3.9-eclipse-temurin-21 AS backend
WORKDIR /backend

COPY backend/pom.xml ./
COPY backend/src ./src

# Copy the static Next export into Spring Boot static resources
RUN mkdir -p ./src/main/resources/static
COPY --from=frontend /frontend/out ./src/main/resources/static

RUN mvn -DskipTests package

# ---------- runtime ----------
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=backend /backend/target/*.jar app.jar

EXPOSE 8080
CMD ["sh","-c","java -Dserver.port=${PORT:-8080} -Dserver.address=0.0.0.0 -jar app.jar"]