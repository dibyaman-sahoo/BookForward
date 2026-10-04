# Backend

Spring Boot modular monolith. Build/run: `mvn spring-boot:run`; test: `mvn verify`. Configuration in `src/main/resources/application.yml` (all values env-driven). Conventions: controllers validate and delegate; services own transactions and authorization; DTOs at the API boundary; errors use `ErrorResponse`.
