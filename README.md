# Edulearn Backend

Edulearn Backend is a Spring Boot microservices backend with service discovery, authentication, courses, lessons, enrollment, discussions, notifications, payments, Kafka support, and Docker Compose infrastructure.

## Services

- `Discovery-Server` - Eureka service registry
- `auth-service` - authentication and user management
- `course-service` - course management
- `lesson-service` - lesson and resource management
- `enrollment-service` - enrollment workflows
- `discussion-service` - discussion and messaging workflows
- `notification-service` - notification handling
- `payment-service` - payment and subscription handling

## Requirements

- Java 17
- Maven 3.9+
- Docker and Docker Compose for local infrastructure

## CI/CD

The root `Jenkinsfile` builds and tests each service independently so Jenkins can use this repository as a monorepo pipeline.
