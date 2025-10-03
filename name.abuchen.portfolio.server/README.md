# Portfolio Performance Server

A headless server distribution that runs the Portfolio Performance HTTP API without opening any windows.

## Purpose

This server product boots the HTTP server defined in the UI project (`name.abuchen.portfolio.ui.api.PortfolioApiServer`) without displaying any graphical interface. It creates an SWT Display to support UI-dependent code but does not open any Shell or Workbench windows.

## Building

The server product is built automatically as part of the main build:

```bash
mvn clean verify
```

The server binaries will be available in:
```
portfolio-product/target/products/name.abuchen.portfolio.server.product/
```

## Running

### Quick Start

The easiest way to run the server:

```bash
# Build and start in one command
./compile.sh --server --start-server

# Or just run if already built
./compile.sh --run-server

# Or use the convenience script
./run-server.sh [port]
```

### macOS

The server requires `-XstartOnFirstThread` VM argument (already configured in the product):

```bash
cd portfolio-product/target/products/name.abuchen.portfolio.server.product/macosx/cocoa/x86_64
./PortfolioPerformanceServer -nosplash -consoleLog
```

Or for ARM64:
```bash
cd portfolio-product/target/products/name.abuchen.portfolio.server.product/macosx/cocoa/aarch64
./PortfolioPerformanceServer -nosplash -consoleLog
```

### Linux

If no X server is available, use `xvfb-run`:

```bash
cd portfolio-product/target/products/name.abuchen.portfolio.server.product/linux/gtk/x86_64
xvfb-run -a ./PortfolioPerformanceServer -nosplash -consoleLog
```

Or with X available:
```bash
./PortfolioPerformanceServer -nosplash -consoleLog
```

### Windows

Run in a user session (no window will appear):

```bash
cd portfolio-product\target\products\name.abuchen.portfolio.server.product\win32\win32\x86_64
PortfolioPerformanceServer.exe -nosplash -consoleLog
```

### Using Equinox Launcher (Portable)

```bash
java -jar plugins/org.eclipse.equinox.launcher_*.jar \
     -application name.abuchen.portfolio.server.application \
     -nosplash -consoleLog
```

### Using compile.sh Script

The project includes a build script with server support:

```bash
# Build the server
./compile.sh --server

# Build and start the server
./compile.sh --server --start-server

# Run the server (if already built)
./compile.sh --run-server

# Or use the dedicated run script
./run-server.sh          # Run on port 8080
./run-server.sh 9090     # Run on port 9090
```

## Configuration

### Port Configuration

By default, the server runs on port 8080. You can change the port using the system property:

```bash
./PortfolioPerformanceServer -nosplash -consoleLog -vmargs -Dportfolio.server.port=9090
```

### Memory Configuration

Default memory is 256MB min, 1GB max. Adjust via VM args in the product file or command line:

```bash
./PortfolioPerformanceServer -nosplash -consoleLog -vmargs -Xms512m -Xmx2g
```

## API Endpoints

Once started, the following endpoints are available:

- `GET /api/hello` - Hello world endpoint
- `GET /api/v1/portfolios` - List all portfolios
- `GET /api/v1/portfolios/health` - Health check
- `GET /api/v1/portfolios/{portfolioId}` - Get portfolio by ID
- `GET /api/v1/portfolios/{portfolioId}/widgetData` - Get widget data

Example health check:
```bash
curl http://localhost:8080/api/v1/portfolios/health
```

## Stopping the Server

Press `Ctrl+C` to gracefully stop the server.

## Platform Notes

### macOS
- Requires `-XstartOnFirstThread` VM argument (already configured)
- Works on both Intel (x86_64) and Apple Silicon (aarch64)

### Linux
- May require X server or xvfb for SWT Display
- Use `xvfb-run -a` wrapper if running headless
- GTK 3 is used by default

### Windows
- Runs in console mode without showing any windows
- Requires a user session (not as a Windows Service by default)

## Workspace Location

The server uses separate workspace locations by OS:

- **Windows**: `%LOCALAPPDATA%\PortfolioPerformanceServer\workspace`
- **macOS**: `~/Library/Application Support/name.abuchen.portfolio.server/workspace`
- **Linux**: `~/.PortfolioPerformanceServer/workspace`

## Troubleshooting

### Server won't start

1. Check if the port is already in use
2. Ensure Java 21 or later is available
3. Check the console output for error messages

### Linux: Display errors

If you see display-related errors on Linux:

```bash
xvfb-run -a ./PortfolioPerformanceServer -nosplash -consoleLog
```

### macOS: Thread errors

Ensure `-XstartOnFirstThread` is set (should be automatic):

```bash
./PortfolioPerformanceServer -nosplash -consoleLog -vmargs -XstartOnFirstThread
```

## Development

To test the server during development:

1. Build the product: `mvn clean verify`
2. Navigate to the product directory
3. Run the launcher as described above
4. Test endpoints with curl or your API client

## License

Copyright 2012 - 2025 Andreas Buchen. All rights reserved.
Eclipse Public License Version 1.0 (EPL-1.0)

