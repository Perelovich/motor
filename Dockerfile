# ---------- STAGE 1: build ----------
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build
COPY pom.xml .
# кешируем зависимости
RUN mvn -B -q -DskipTests dependency:go-offline
# копируем код и собираем
COPY src ./src
RUN mvn -B -q -DskipTests clean package

# ---------- STAGE 2: runtime ----------
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
# копируем собранный fat-jar
COPY --from=builder /build/target/*-bot.jar /app/app.jar
ENV JAVA_OPTS="-Xms256m -Xmx512m"
ENTRYPOINT ["bash","-lc","java $JAVA_OPTS -jar /app/app.jar"]