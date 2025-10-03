# Portfolio Performance API Server

A standalone REST API server for Portfolio Performance that provides programmatic access to portfolio data and calculations.

## 🚀 Quick Start

### Option 1: Using the Startup Script (Recommended)
```bash
cd name.abuchen.portfolio.api
./start-api-server.sh
```

### Option 2: Direct Java Execution
```bash
cd name.abuchen.portfolio.api
java -jar target/portfolio-api-server.jar
```

### Option 3: Custom Port
```bash
cd name.abuchen.portfolio.api
java -jar target/portfolio-api-server.jar 9090
```

## 📋 Available Endpoints

### General Endpoints
- `GET /` - API information and available endpoints
- `GET /api/v1/hello` - Hello world endpoint
- `GET /api/v1/health` - Health check
- `GET /api/v1/info` - API information

### Portfolio Endpoints
- `GET /api/v1/portfolio/info` - Portfolio information
- `GET /api/v1/portfolio/statistics` - Portfolio statistics

### File Management Endpoints
- `POST /api/v1/files/open` - Open a portfolio file
- `GET /api/v1/files/info` - Get file information
- `GET /api/v1/files/cache/stats` - Cache statistics
- `POST /api/v1/files/cache/clear` - Clear cache
- `GET /api/v1/files/health` - File service health

## 🔧 Building the Server

To build the executable JAR:

```bash
cd name.abuchen.portfolio.api
mvn3 clean package
```

This creates:
- `target/portfolio-api-server.jar` - Executable JAR with all dependencies
- `target/name.abuchen.portfolio.api-0.78.2-SNAPSHOT.jar` - OSGi bundle

## 🌐 Testing the API

Once the server is running, you can test it:

```bash
# Test basic endpoints
curl http://localhost:8080/api/v1/hello
curl http://localhost:8080/api/v1/health
curl http://localhost:8080/

# Test portfolio endpoints
curl http://localhost:8080/api/v1/portfolio/info
curl http://localhost:8080/api/v1/portfolio/statistics
```

## 📦 Architecture

The API server is built using:
- **Jetty** - HTTP server
- **JAX-RS (Jersey)** - REST framework
- **Jackson** - JSON processing
- **SLF4J** - Logging
- **Portfolio Performance Core** - Business logic

## 🔄 Integration with Portfolio Performance

The API server integrates with the Portfolio Performance core modules:
- Uses the same data models and calculations
- Provides REST access to portfolio functionality
- Maintains compatibility with the main application

## 🛠️ Development

### Project Structure
```
name.abuchen.portfolio.api/
├── src/main/java/
│   ├── controller/          # REST controllers
│   ├── service/            # Business logic services
│   ├── dto/                # Data transfer objects
│   └── StandaloneApiServer.java  # Main server class
├── META-INF/
│   └── MANIFEST.MF         # OSGi bundle manifest
├── pom.xml                 # Maven configuration
├── start-api-server.sh     # Startup script
└── target/
    └── portfolio-api-server.jar  # Executable JAR
```

### Adding New Endpoints

1. Create a new controller class in `src/main/java/controller/`
2. Use JAX-RS annotations (`@Path`, `@GET`, `@POST`, etc.)
3. Add the controller to `StandaloneApiServer.java`
4. Rebuild with `mvn3 clean package`

## 🚨 Troubleshooting

### Server Won't Start
- Check if port 8080 is available: `lsof -i :8080`
- Use a different port: `java -jar target/portfolio-api-server.jar 9090`
- Check Java version: `java -version` (requires Java 21+)

### Build Issues
- Ensure Maven 3 is available: `mvn3 --version`
- Clean and rebuild: `mvn3 clean package`
- Check for dependency conflicts

### API Not Responding
- Verify server is running: `curl http://localhost:8080/`
- Check server logs for errors
- Ensure firewall allows connections to port 8080

## 📝 License

This API server is part of the Portfolio Performance project and follows the same licensing terms.
