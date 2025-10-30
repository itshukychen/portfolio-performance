# Unified Multi-stage Dockerfile for Portfolio Performance Server and UI
# Use RUN_MODE environment variable to choose: server (API) or ui (Desktop with VNC)

# Stage 1: Build both the server and UI
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

# Build both server and UI products for Linux
# This will create both executables in portfolio-product/target/products/
RUN mvn -f portfolio-app/pom.xml clean install \
    -Dcheckstyle.skip=true \
    -Dtycho.targetPlatform=portfolio-target-definition/portfolio-target-definition.target \
    -Dtycho.p2.transport.min-cache-minutes=1800 \
    -Dtycho.p2.mirrors=false \
    -Dtarget.os=linux \
    -Dtarget.ws=gtk \
    -Dtarget.arch=x86_64 \
    -T 16 -DskipTests \
    -Dmaven.main.skip=true

# Stage 2: Runtime environment with optional VNC support
FROM eclipse-temurin:21-jre

ENV DEBIAN_FRONTEND=noninteractive \
    LANG=C.UTF-8 LC_ALL=C.UTF-8 \
    TZ=Etc/UTC \
    DISPLAY=:0

# Create non-root user/group
RUN groupadd -g 10001 ppuser && \
    useradd -m -u 10001 -g 10001 -s /bin/bash ppuser

# Install runtime dependencies: JVM, X stack, VNC, WM, tools
# These cover both server (minimal) and UI (with VNC) needs
RUN apt-get update && apt-get install -y --no-install-recommends \
    xvfb x11vnc xauth x11-xserver-utils x11-utils \
    fluxbox \
    gosu \
    curl ca-certificates wget \
    fonts-dejavu-core \
    net-tools procps \
    libgtk-3-0 libgtk-3-bin \
    libxtst6 \
    libgl1 \
    libglu1-mesa \
    fontconfig \
    libxext6 libxrender1 libxi6 libxrandr2 libxxf86vm1 \
    libasound2t64 libfontconfig1 libfreetype6 \
    libglib2.0-0 libnss3 libatk1.0-0 libcairo2 \
    libpango-1.0-0 libpangocairo-1.0-0 \
    && rm -rf /var/lib/apt/lists/*

# Create app and script directories
RUN mkdir -p /opt/pp /opt/bin
WORKDIR /opt/pp

# Copy both the server and UI from builder stage
COPY --from=builder /build/portfolio-product/target/products/name.abuchen.portfolio.server.product/linux/gtk/x86_64/ ./server/
COPY --from=builder /build/portfolio-product/target/products/name.abuchen.portfolio.product/linux/gtk/x86_64/ ./ui/

# Copy entrypoint and helper scripts
COPY scripts/entrypoint.sh      /opt/bin/entrypoint.sh
COPY scripts/start_x_vnc.sh     /opt/bin/start_x_vnc.sh
COPY scripts/start_pp_ui.sh     /opt/bin/start_pp_ui.sh
COPY scripts/start_pp_server.sh /opt/bin/start_pp_server.sh
RUN chmod 0755 /opt/bin/*.sh

# Create workspace and portfolios directories
RUN mkdir -p /home/ppuser/workspace /opt/pp/portfolios && \
    chown -R ppuser:ppuser /home/ppuser /opt/pp

# Set environment variables
ENV PORTFOLIO_DIR=/opt/pp/portfolios \
    WORKSPACE_DIR=/home/ppuser/workspace \
    PORTFOLIO_SERVER_PORT=8080 \
    RUN_MODE=server

# Runtime defaults
USER root
WORKDIR /home/ppuser

# Expose both server API and VNC ports
# - 8080: REST API (server mode)
# - 5900: VNC GUI (ui mode)
EXPOSE 8080 5900

# Health check (works for server mode)
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD if [ "$RUN_MODE" = "server" ]; then wget --no-verbose --tries=1 --spider http://localhost:8080/api/v1/portfolios/health || exit 1; else exit 0; fi

# Persist workspace and portfolios
VOLUME ["/home/ppuser/workspace"]
VOLUME ["/opt/pp/portfolios"]

# Entrypoint (chooses server or UI based on RUN_MODE env var)
ENTRYPOINT ["/opt/bin/entrypoint.sh"]
