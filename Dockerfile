FROM openjdk:8-jdk-alpine
ADD target/ms_answers.jar ms_answers.jar
EXPOSE 8081
ENTRYPOINT ["java","-jar","ms_answers.jar"]
