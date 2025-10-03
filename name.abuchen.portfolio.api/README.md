# Portfolio Performance API Server

This module provides a REST API server for the Portfolio Performance application, exposing portfolio data and calculations through HTTP endpoints.

## Overview

The API server is built using Spring Boot and provides REST endpoints to access portfolio data and perform calculations using the Portfolio Performance core modules.

## Features

- **Hello World Endpoints**: Basic endpoints to verify the API is working
- **Portfolio Data Access**: Endpoints to retrieve portfolio information and statistics
- **File Operations**: Open and load portfolio files from the filesystem
- **Client Caching**: Intelligent caching of loaded portfolio clients
- **Graceful Shutdown**: Proper handling of SIGTERM signals (Ctrl+C)
- **Health Checks**: Built-in health monitoring endpoints
- **Future Integration**: Ready for integration with Portfolio Performance core modules

## API Endpoints

### Basic Endpoints
- `GET /api/v1/hello` - Hello world endpoint
- `GET /api/v1/health` - Health check endpoint  
- `GET /api/v1/info` - API information endpoint

### Portfolio Endpoints
- `GET /api/v1/portfolio/info` - Portfolio information
- `GET /api/v1/portfolio/statistics` - Portfolio statistics

### File Operations Endpoints
- `POST /api/v1/files/open` - Open and load a portfolio file
- `GET /api/v1/files/info?filePath=<path>` - Get basic file information
- `GET /api/v1/files/health` - File service health check
- `GET /api/v1/files/cache/stats` - Get cache statistics
- `POST /api/v1/files/cache/clear` - Clear client cache

### Management Endpoints (Spring Boot Actuator)
- `GET /actuator/health` - Detailed health information
- `GET /actuator/info` - Application information
- `GET /actuator/metrics` - Application metrics

## Running the Application

### Using Maven
```bash
cd name.abuchen.portfolio.api
mvn spring-boot:run
```

### Using Exec Plugin
```bash
cd name.abuchen.portfolio.api
mvn exec:java
```

### Building and Running JAR
```bash
cd name.abuchen.portfolio.api
mvn clean package
java -jar target/name.abuchen.portfolio.api-0.78.2-SNAPSHOT.jar
```

## Graceful Shutdown

The API server now supports graceful shutdown:

- **Ctrl+C**: The server responds properly to SIGTERM signals
- **Automatic Cleanup**: Client cache and resources are cleaned up on shutdown
- **Timeout**: 30-second timeout for graceful shutdown
- **Logging**: Shutdown process is logged for debugging

### Testing Graceful Shutdown

```bash
# Start the server
cd name.abuchen.portfolio.api
mvn spring-boot:run

# Press Ctrl+C to test graceful shutdown
# You should see "Shutting down Portfolio Performance API Server..." message
```

## Using the File Opening API

### Opening a Portfolio File

To open a portfolio file, send a POST request to `/api/v1/files/open`:

```bash
curl -X POST http://localhost:8080/api/v1/files/open \
  -H "Content-Type: application/json" \
  -d '{
    "filePath": "/path/to/your/portfolio.portfolio",
    "password": null
  }'
```

For encrypted files, include the password:
```bash
curl -X POST http://localhost:8080/api/v1/files/open \
  -H "Content-Type: application/json" \
  -d '{
    "filePath": "/path/to/encrypted/portfolio.portfolio",
    "password": "your-password"
  }'
```

### Getting File Information

To get basic information about a file without loading it:

```bash
curl "http://localhost:8080/api/v1/files/info?filePath=/path/to/portfolio.portfolio"
```

### Cache Management

View cache statistics:
```bash
curl http://localhost:8080/api/v1/files/cache/stats
```

Clear the cache:
```bash
curl -X POST http://localhost:8080/api/v1/files/cache/clear
```

## Configuration

The application is configured via `src/main/resources/application.yml`:

- **Port**: 8080 (default)
- **Context Path**: `/`
- **Logging**: Configured for INFO level
- **JSON**: Pretty-printed JSON responses

## Development

### Project Structure
```
src/
├── main/
│   ├── java/name/abuchen/portfolio/api/
│   │   ├── PortfolioApiApplication.java          # Main Spring Boot application
│   │   ├── controller/                           # REST controllers
│   │   │   ├── HelloWorldController.java         # Basic endpoints
│   │   │   └── PortfolioController.java          # Portfolio endpoints
│   │   └── service/                              # Business logic services
│   │       └── PortfolioService.java             # Portfolio service
│   └── resources/
│       └── application.yml                        # Configuration
└── test/
    └── java/name/abuchen/portfolio/api/
        └── controller/                            # Unit tests
            └── HelloWorldControllerTest.java
```

### Adding New Endpoints

1. Create a new controller in the `controller` package
2. Add service classes in the `service` package for business logic
3. Add unit tests in the `test` package
4. Update this README with new endpoint documentation

## Integration with Portfolio Performance

This API server is designed to integrate with the existing Portfolio Performance modules:

- **Core Module**: `name.abuchen.portfolio` - Main portfolio functionality
- **UI Module**: `name.abuchen.portfolio.ui` - UI components and services
- **Tests Module**: `name.abuchen.portfolio.tests` - Test utilities

Future development will include:
- Portfolio data loading and management
- Performance calculations
- Report generation
- Data export/import functionality

## Dependencies

- **Spring Boot 3.2.0**: Web framework and application server
- **Jackson 2.16.0**: JSON processing
- **Portfolio Performance Core**: Integration with existing modules
- **JUnit 4.13.2**: Unit testing framework

## License

This module is part of the Portfolio Performance project and follows the same license terms.
