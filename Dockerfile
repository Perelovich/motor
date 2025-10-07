# ---------- build stage ----------
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build

# Сначала только pom.xml, чтобы кеш зависимостей работал предсказуемо
COPY pom.xml .

# Полностью очистим локальный Maven-репозиторий и форс-обновим зависимости
RUN rm -rf /root/.m2/repository && mvn -B -U -q -DskipTests dependency:go-offline

# Теперь – исходники
COPY src ./src

# Ещё раз собираем на чистом кеше (чтобы точно попала flyway 10.22.0 из pom.xml)
RUN rm -rf /root/.m2/repository && mvn -B -U -q -DskipTests clean package

# ---------- runtime stage ----------
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=builder /build/target/*-bot.jar /app/app.jar

# (необязательно) часовой пояс
ENV TZ=Europe/Moscow

ENTRYPOINT ["java","-jar","/app/app.jar"]
