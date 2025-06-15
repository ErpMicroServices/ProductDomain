Feature: Docker and Docker Compose Configuration
  As a developer
  I want Docker containerization for all services
  So that I can easily develop and deploy the ProductDomain microservice

  Background:
    Given the ProductDomain project is set up

  Scenario: API Module Dockerfile
    When I examine the API Dockerfile
    Then it should use multi-stage build
    And it should have a build stage with:
      | stage_name | base_image             | purpose                    |
      | builder    | gradle:8.5-jdk21       | Build the application      |
      | runtime    | eclipse-temurin:21-jre | Run the application        |
    And the build stage should:
      | action                | description                                |
      | Copy build files      | Copy Gradle files and source code          |
      | Build application     | Run Gradle build without tests             |
      | Extract layers        | Use layertools for optimized caching       |
    And the runtime stage should:
      | action                | description                                |
      | Copy layers           | Copy application layers from build stage   |
      | Set user              | Run as non-root user                       |
      | Expose port           | Expose port 8080                           |
      | Configure JVM         | Set appropriate JVM flags                  |
      | Set entrypoint        | Use exec form for proper signal handling   |

  Scenario: UI Components Dockerfile
    When I examine the UI Components Dockerfile
    Then it should use multi-stage build
    And it should have stages for:
      | stage_name | base_image          | purpose                    |
      | builder    | node:20-alpine      | Build the React app        |
      | runtime    | nginx:alpine        | Serve static files         |
    And the build stage should:
      | action                | description                                |
      | Install dependencies  | Use npm ci for faster installs             |
      | Build application     | Build production React app                 |
      | Run tests             | Execute tests during build                 |
    And the runtime stage should:
      | action                | description                                |
      | Copy built files      | Copy dist folder from builder              |
      | Configure nginx       | Use custom nginx configuration             |
      | Expose port           | Expose port 80                             |
      | Set user              | Run nginx as non-root                      |

  Scenario: Development Docker Compose
    When I examine the development Docker Compose file
    Then it should define services for:
      | service    | image/build           | ports       | purpose                    |
      | postgres   | postgres:16-alpine    | 5432:5432   | Database service           |
      | redis      | redis:7-alpine        | 6379:6379   | Caching service            |
      | api        | ./api                 | 8080:8080   | API service                |
      | ui         | ./ui-components       | 3000:80     | UI service                 |
    And the postgres service should have:
      | configuration         | value                                      |
      | environment           | POSTGRES_DB, POSTGRES_USER, POSTGRES_PASSWORD |
      | volumes               | postgres_data:/var/lib/postgresql/data     |
      | healthcheck           | pg_isready command                         |
    And the redis service should have:
      | configuration         | value                                      |
      | command               | redis-server with config                   |
      | volumes               | redis_data:/data                           |
      | healthcheck           | redis-cli ping                             |
    And the api service should have:
      | configuration         | value                                      |
      | depends_on            | postgres, redis                            |
      | environment           | Spring profiles and datasource config      |
      | volumes               | Source code mounted for hot reload         |
      | healthcheck           | HTTP health endpoint                       |
    And the ui service should have:
      | configuration         | value                                      |
      | depends_on            | api                                        |
      | environment           | API_URL configuration                      |
      | volumes               | Source code mounted for hot reload         |

  Scenario: Production Docker Compose
    When I examine the production Docker Compose file
    Then it should define services with production settings
    And services should NOT have:
      | configuration         | reason                                     |
      | source volumes        | Use built images only                      |
      | debug ports           | Security consideration                     |
      | development tools     | Reduced attack surface                     |
    And services should have:
      | configuration         | value                                      |
      | restart policy        | unless-stopped                             |
      | resource limits       | CPU and memory constraints                 |
      | read-only filesystem  | Where applicable                           |
      | security options      | no-new-privileges                          |

  Scenario: Docker networking
    When I examine the Docker Compose networking
    Then it should define custom networks:
      | network    | driver | purpose                                   |
      | backend    | bridge | Internal communication between services   |
      | frontend   | bridge | UI to API communication                   |
    And service network assignments should be:
      | service    | networks           | reason                         |
      | postgres   | backend            | Only accessible by API         |
      | redis      | backend            | Only accessible by API         |
      | api        | backend, frontend  | Bridge between data and UI     |
      | ui         | frontend           | Only needs API access          |

  Scenario: Health checks configuration
    When I examine health check configurations
    Then all services should have health checks
    And health checks should include:
      | service    | check_type | endpoint/command        | interval | timeout | retries |
      | postgres   | command    | pg_isready             | 10s      | 5s      | 5       |
      | redis      | command    | redis-cli ping         | 10s      | 5s      | 5       |
      | api        | http       | /actuator/health       | 30s      | 10s     | 3       |
      | ui         | http       | /                      | 30s      | 10s     | 3       |

  Scenario: Volume management
    When I examine volume configurations
    Then persistent volumes should be defined for:
      | volume_name    | service    | mount_point                    |
      | postgres_data  | postgres   | /var/lib/postgresql/data       |
      | redis_data     | redis      | /data                          |
    And development volumes should include:
      | volume_type    | service    | purpose                        |
      | bind mount     | api        | Hot reload for Java code       |
      | bind mount     | ui         | Hot reload for React code      |

  Scenario: Environment configuration
    When I examine environment configurations
    Then there should be environment files:
      | file              | purpose                                    |
      | .env.example      | Template for environment variables         |
      | .env.development  | Development-specific values                |
      | .env.production   | Production-specific values                 |
    And environment variables should include:
      | variable                | service    | purpose                    |
      | POSTGRES_DB            | postgres   | Database name              |
      | POSTGRES_USER          | postgres   | Database user              |
      | POSTGRES_PASSWORD      | postgres   | Database password          |
      | SPRING_PROFILES_ACTIVE | api        | Spring profile selection   |
      | REDIS_PASSWORD         | redis      | Redis authentication       |
      | API_BASE_URL           | ui         | API endpoint configuration |

  Scenario: Docker build optimization
    When I build Docker images
    Then build should use:
      | optimization           | benefit                                    |
      | Layer caching          | Faster rebuilds                            |
      | Multi-stage builds     | Smaller final images                       |
      | .dockerignore          | Exclude unnecessary files                  |
      | BuildKit               | Parallel builds and cache mounting         |
    And final image sizes should be:
      | service    | max_size   | optimization_used                 |
      | api        | 300MB      | JRE base, layered JAR             |
      | ui         | 50MB       | Alpine nginx, minimal static files|

  Scenario: Security hardening
    When I examine Docker security configurations
    Then containers should run with:
      | security_measure       | implementation                             |
      | Non-root user          | USER directive in Dockerfile               |
      | Read-only root fs      | read_only: true in compose                 |
      | No new privileges      | security_opt: no-new-privileges            |
      | Dropped capabilities   | cap_drop: ALL                              |
      | Health checks          | Proper liveness/readiness probes           |
    And secrets should be managed using:
      | method                 | usage                                      |
      | Docker secrets         | Production passwords                       |
      | Environment files      | Development only                           |
      | Build arguments        | Non-sensitive build-time config            |