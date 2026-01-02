# ---------- frontend: build + export ----------
FROM node:20-alpine AS frontend
WORKDIR /frontend

COPY frontend/package*.json ./
RUN npm ci

COPY frontend/ ./
RUN npm run build
# for Next static export (with output:"export") it generates /frontend/out automatically

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

ENV PORT=10000
EXPOSE 10000
CMD ["sh","-c","java -Dserver.port=$PORT -jar app.jar"]
