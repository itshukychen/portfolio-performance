# Multi-stage Dockerfile for Portfolio Performance Server
# Stage 1: Build the server
FROM maven:3.9-eclipse-temurin-21 AS builder

# Install build dependencies
RUN apt-get update && apt-get install -y \
    git \
    wget \
    && rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /build

# Copy the entire project
COPY . .

# Build the server product for Linux
# This will create the server executable in portfolio-product/target/products/
RUN mvn -f portfolio-app/pom.xml clean install \
    -Dcheckstyle.skip=true \
    -Dtycho.targetPlatform=portfolio-target-definition/portfolio-target-definition.target \
    -Dtycho.p2.transport.min-cache-minutes=1800 \
    -Dtycho.p2.mirrors=false \
    -Dtarget.os=macosx \
    -Dtarget.ws=cocoa \
    -Dtarget.arch=aarch64 \
    -T 16 -DskipTests \
    -Dmaven.main.skip=true

# Stage 2: Runtime environment
FROM eclipse-temurin:21-jre

# Install runtime dependencies for SWT (requires X11 libraries)
# xvfb provides a virtual X server for headless operation
RUN apt-get update && apt-get install -y \
    xvfb \
    libgtk-3-0 \
    libxtst6 \
    libgl1 \
    libglu1-mesa \
    fontconfig \
    && rm -rf /var/lib/apt/lists/*

# Create app directory
WORKDIR /app

# Copy the built server from builder stage
COPY --from=builder /build/portfolio-product/target/products/name.abuchen.portfolio.server.product/linux/gtk/x86_64/ ./server/

# Create workspace directory
RUN mkdir -p /app/workspace

# Set environment variables
ENV PORTFOLIO_DIR=/app/portfolios \
    PORTFOLIO_SERVER_PORT=8080

# Create portfolios directory
RUN mkdir -p ${PORTFOLIO_DIR}

# Expose the API port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/v1/portfolios/health || exit 1

# Run the server with xvfb (virtual X server for headless SWT)
CMD ["xvfb-run", "-a", "./server/portfolio-server/PortfolioPerformanceServer", "-nosplash", "-consoleLog", "-vmargs", "-Dportfolio.server.port=${PORTFOLIO_SERVER_PORT}", "-Dosgi.instance.area=/app/workspace"]

