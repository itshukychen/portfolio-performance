#!/bin/bash
set -e

SCRIPT_DIR="$(pwd)"

# Function to show usage
show_usage() {
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  --app          Build only the Portfolio Performance desktop application"
    echo "  --server       Build only the Portfolio Performance server executable"
    echo "  --api          Build only the Portfolio Performance API server"
    echo "  --both         Build both the desktop application and API server (default)"
    echo "  --all          Build desktop app, server executable, and API server"
    echo "  --clean        Clean Maven and P2 caches before building"
    echo "  --full         Full build with all dependencies"
    echo "  --test         Run tests"
    echo "  --open         Open the built application (app only)"
    echo "  --start-api    Start the API server after building"
    echo "  --start-server Start the server executable after building"
    echo "  --help         Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0                    # Build both app and API"
    echo "  $0 --app              # Build only the desktop application"
    echo "  $0 --server           # Build only the server executable"
    echo "  $0 --api --start-api  # Build and start the API server"
    echo "  $0 --server --start-server # Build and start the server executable"
    echo "  $0 --all              # Build desktop app, server executable, and API"
    echo "  $0 --clean --both     # Clean and build both projects"
}

# Default to building both projects
BUILD_APP=false
BUILD_SERVER=false
BUILD_API=false
CLEAN=false
FULL=false
TEST=false
OPEN=false
START_API=false
START_SERVER=true

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --app)
            BUILD_APP=true
            BUILD_SERVER=false
            BUILD_API=false
            shift
            ;;
        --server)
            BUILD_APP=false
            BUILD_SERVER=true
            BUILD_API=false
            shift
            ;;
        --api)
            BUILD_APP=false
            BUILD_SERVER=false
            BUILD_API=true
            shift
            ;;
        --both)
            BUILD_APP=true
            BUILD_SERVER=false
            BUILD_API=true
            shift
            ;;
        --all)
            BUILD_APP=true
            BUILD_SERVER=true
            BUILD_API=true
            shift
            ;;
        --clean)
            CLEAN=true
            shift
            ;;
        --full)
            FULL=true
            shift
            ;;
        --test)
            TEST=true
            shift
            ;;
        --open)
            OPEN=true
            shift
            ;;
        --start-api)
            START_API=true
            shift
            ;;
        --start-server)
            START_SERVER=true
            shift
            ;;
        --help)
            show_usage
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            show_usage
            exit 1
            ;;
    esac
done

# Clean caches if requested
if [[ "$CLEAN" == true ]]; then
    echo "🧹 Cleaning Maven and P2 caches..."
    sleep 2
    rm -rf ~/.m2
    rm -rf ~/.p2
    echo "✅ Cleanup completed"
fi

# Build functions
build_app() {
    echo "🏗️  Building Portfolio Performance Desktop Application..."
    
    if [[ "$FULL" == true ]]; then
        echo "📦 Full build with all dependencies"
        mvn3 -f $SCRIPT_DIR/portfolio-app/pom.xml clean install -Dcheckstyle.skip=true -Dtycho.targetPlatform=portfolio-target-definition/portfolio-target-definition.target -Dtycho.p2.transport.min-cache-minutes=1800 -Dtycho.p2.mirrors=false -Dtarget.os=macosx -Dtarget.ws=cocoa -Dtarget.arch=aarch64 -T 16 -DskipTests -Dmaven.main.skip=true | grep -v "Downloading from p2\|Unable to satisfy dependency\|Problems resolving provisioning plan\|Resolving MavenDependencyRoots\|Downloaded from p2"
    elif [[ "$TEST" == true ]]; then
        echo "🧪 Running tests"
        mvn3 -f $SCRIPT_DIR/portfolio-app/pom.xml verify -Dcheckstyle.skip=true -Dtycho.targetPlatform=portfolio-target-definition/portfolio-target-definition.target -Dtycho.p2.transport.min-cache-minutes=1800 -Dtycho.p2.mirrors=false -Dtarget.os=macosx -Dtarget.ws=cocoa -Dtarget.arch=aarch64 -T 16 -o -Dmaven.main.skip=true -pl '!name.abuchen.portfolio:name.abuchen.portfolio.bootstrap,!name.abuchen.portfolio:name.abuchen.portfolio.pdfbox3,!name.abuchen.portfolio:name.abuchen.portfolio.pdfbox1' | grep -v "Downloading from p2\|Unable to satisfy dependency\|Problems resolving provisioning plan\|Resolving MavenDependencyRoots\|Downloaded from p2"
    else
        echo "⚡ Standard build"
        mvn3 -f $SCRIPT_DIR/portfolio-app/pom.xml verify -Dcheckstyle.skip=true -Dtycho.targetPlatform=portfolio-target-definition/portfolio-target-definition.target -Dtycho.p2.transport.min-cache-minutes=1800 -Dtycho.p2.mirrors=false -Dtarget.os=macosx -Dtarget.ws=cocoa -Dtarget.arch=aarch64 -T 16 -o -DskipTests -Dmaven.main.skip=true -pl '!name.abuchen.portfolio:name.abuchen.portfolio.tests,!name.abuchen.portfolio:name.abuchen.portfolio.junit,!name.abuchen.portfolio:name.abuchen.portfolio.bootstrap,!name.abuchen.portfolio:name.abuchen.portfolio.pdfbox3,!name.abuchen.portfolio:name.abuchen.portfolio.pdfbox1' | grep -v "Downloading from p2\|Unable to satisfy dependency\|Problems resolving provisioning plan\|Resolving MavenDependencyRoots\|Downloaded from p2"
    fi
    
    APP_EXIT_CODE=${PIPESTATUS[0]}
    if [[ $APP_EXIT_CODE -eq 0 ]]; then
        echo "✅ Desktop application build completed successfully"
        return 0
    else
        echo "❌ Desktop application build failed"
        return $APP_EXIT_CODE
    fi
}

build_server() {
    echo "🏗️  Building Portfolio Performance Server Executable..."
    
    if [[ "$FULL" == true ]]; then
        echo "📦 Full build with all dependencies"
        mvn3 -f $SCRIPT_DIR/portfolio-app/pom.xml clean install -Dcheckstyle.skip=true -Dtycho.targetPlatform=portfolio-target-definition/portfolio-target-definition.target -Dtycho.p2.transport.min-cache-minutes=1800 -Dtycho.p2.mirrors=false -Dtarget.os=macosx -Dtarget.ws=cocoa -Dtarget.arch=aarch64 -T 16 -DskipTests -Dmaven.main.skip=true | grep -v "Downloading from p2\|Unable to satisfy dependency\|Problems resolving provisioning plan\|Resolving MavenDependencyRoots\|Downloaded from p2"
    elif [[ "$TEST" == true ]]; then
        echo "🧪 Running tests"
        mvn3 -f $SCRIPT_DIR/portfolio-app/pom.xml verify -Dcheckstyle.skip=true -Dtycho.targetPlatform=portfolio-target-definition/portfolio-target-definition.target -Dtycho.p2.transport.min-cache-minutes=1800 -Dtycho.p2.mirrors=false -Dtarget.os=macosx -Dtarget.ws=cocoa -Dtarget.arch=aarch64 -T 16 -o -Dmaven.main.skip=true -pl '!name.abuchen.portfolio:name.abuchen.portfolio.bootstrap,!name.abuchen.portfolio:name.abuchen.portfolio.pdfbox3,!name.abuchen.portfolio:name.abuchen.portfolio.pdfbox1' | grep -v "Downloading from p2\|Unable to satisfy dependency\|Problems resolving provisioning plan\|Resolving MavenDependencyRoots\|Downloaded from p2"
    else
        echo "⚡ Standard build"
        mvn3 -f $SCRIPT_DIR/portfolio-app/pom.xml verify -Dcheckstyle.skip=true -Dtycho.targetPlatform=portfolio-target-definition/portfolio-target-definition.target -Dtycho.p2.transport.min-cache-minutes=1800 -Dtycho.p2.mirrors=false -Dtarget.os=macosx -Dtarget.ws=cocoa -Dtarget.arch=aarch64 -T 16 -o -DskipTests -Dmaven.main.skip=true -pl '!name.abuchen.portfolio:name.abuchen.portfolio.tests,!name.abuchen.portfolio:name.abuchen.portfolio.junit,!name.abuchen.portfolio:name.abuchen.portfolio.bootstrap,!name.abuchen.portfolio:name.abuchen.portfolio.pdfbox3,!name.abuchen.portfolio:name.abuchen.portfolio.pdfbox1' | grep -v "Downloading from p2\|Unable to satisfy dependency\|Problems resolving provisioning plan\|Resolving MavenDependencyRoots\|Downloaded from p2"
    fi
    
    SERVER_EXIT_CODE=${PIPESTATUS[0]}
    if [[ $SERVER_EXIT_CODE -eq 0 ]]; then
        echo "✅ Server executable build completed successfully"
        echo "📦 Server location: $SCRIPT_DIR/portfolio-product/target/products/name.abuchen.portfolio.server.product/macosx/cocoa/aarch64/PortfolioPerformanceServer.app"
        return 0
    else
        echo "❌ Server executable build failed"
        return $SERVER_EXIT_CODE
    fi
}

build_api() {
    echo "🏗️  Building Portfolio Performance API Server..."
    
    if [[ "$FULL" == true ]]; then
        echo "📦 Full build with all dependencies"
        mvn3 -f $SCRIPT_DIR/name.abuchen.portfolio.api/pom.xml clean package -Dcheckstyle.skip=true -Dtycho.targetPlatform=portfolio-target-definition/portfolio-target-definition.target -Dtycho.p2.transport.min-cache-minutes=1800 -Dtycho.p2.mirrors=false -Dtarget.os=macosx -Dtarget.ws=cocoa -Dtarget.arch=aarch64 -T 16 -DskipTests -Dmaven.main.skip=true | grep -v "Downloading from p2\|Unable to satisfy dependency\|Problems resolving provisioning plan\|Resolving MavenDependencyRoots\|Downloaded from p2"
    elif [[ "$TEST" == true ]]; then
        echo "🧪 Running tests"
        mvn3 -f $SCRIPT_DIR/name.abuchen.portfolio.api/pom.xml clean verify -Dcheckstyle.skip=true -Dtycho.targetPlatform=portfolio-target-definition/portfolio-target-definition.target -Dtycho.p2.transport.min-cache-minutes=1800 -Dtycho.p2.mirrors=false -Dtarget.os=macosx -Dtarget.ws=cocoa -Dtarget.arch=aarch64 -T 16 -o -Dmaven.main.skip=true -pl '!name.abuchen.portfolio:name.abuchen.portfolio.bootstrap,!name.abuchen.portfolio:name.abuchen.portfolio.pdfbox3,!name.abuchen.portfolio:name.abuchen.portfolio.pdfbox1' | grep -v "Downloading from p2\|Unable to satisfy dependency\|Problems resolving provisioning plan\|Resolving MavenDependencyRoots\|Downloaded from p2"
    else
        echo "⚡ Standard build"
        mvn3 -f $SCRIPT_DIR/name.abuchen.portfolio.api/pom.xml verify -Dcheckstyle.skip=true -Dtycho.targetPlatform=portfolio-target-definition/portfolio-target-definition.target -Dtycho.p2.transport.min-cache-minutes=1800 -Dtycho.p2.mirrors=false -Dtarget.os=macosx -Dtarget.ws=cocoa -Dtarget.arch=aarch64 -T 16 -o -DskipTests -Dmaven.main.skip=true | grep -v "Downloading from p2\|Unable to satisfy dependency\|Problems resolving provisioning plan\|Resolving MavenDependencyRoots\|Downloaded from p2"
    fi
    
    API_EXIT_CODE=${PIPESTATUS[0]}
    if [[ $API_EXIT_CODE -eq 0 ]]; then
        echo "✅ API server build completed successfully"
        echo "📦 JAR location: $SCRIPT_DIR/name.abuchen.portfolio.api/target/portfolio-api-server.jar"
        return 0
    else
        echo "❌ API server build failed"
        return $API_EXIT_CODE
    fi
}

# Main build logic
OVERALL_EXIT_CODE=0

if [[ "$BUILD_APP" == true ]]; then
    build_app
    APP_EXIT_CODE=$?
    if [[ $APP_EXIT_CODE -ne 0 ]]; then
        OVERALL_EXIT_CODE=$APP_EXIT_CODE
    fi
fi

if [[ "$BUILD_SERVER" == true ]]; then
    build_server
    SERVER_EXIT_CODE=$?
    if [[ $SERVER_EXIT_CODE -ne 0 ]]; then
        OVERALL_EXIT_CODE=$SERVER_EXIT_CODE
    fi
fi

if [[ "$BUILD_API" == true ]]; then
    build_api
    API_EXIT_CODE=$?
    if [[ $API_EXIT_CODE -ne 0 ]]; then
        OVERALL_EXIT_CODE=$API_EXIT_CODE
    fi
fi

# Post-build actions
if [[ $OVERALL_EXIT_CODE -eq 0 ]]; then
    echo ""
    echo "🎉 Build completed successfully!"
    
    if [[ "$OPEN" == true ]]; then
        echo "🚀 Opening Portfolio Performance application..."
        open $SCRIPT_DIR/portfolio-product/target/products/name.abuchen.portfolio.product/macosx/cocoa/aarch64/PortfolioPerformance.app
    fi
    
    if [[ "$START_API" == true ]]; then
        echo "🚀 Starting Portfolio Performance API server..."
        $SCRIPT_DIR/name.abuchen.portfolio.api/start-api-server.sh
    fi
    
    if [[ "$START_SERVER" == true ]]; then
        echo "🚀 Starting Portfolio Performance Server..."
        echo "📋 Server will run on http://localhost:8080 (or specify port with -Dportfolio.server.port=XXXX)"
        echo "📋 Press Ctrl+C to stop the server"
        echo ""
        SERVER_APP="$SCRIPT_DIR/portfolio-product/target/products/name.abuchen.portfolio.server.product/macosx/cocoa/aarch64/portfolio-server.app"
        if [[ -d "$SERVER_APP" ]]; then
            PORTFOLIO_DIR=$(realpath $SCRIPT_DIR/portfolios) "$SERVER_APP/Contents/MacOS/PortfolioPerformanceServer" -nosplash -consoleLog
        else
            echo "❌ Server executable not found at: $SERVER_APP"
            echo "   Please build the server first with: $0 --server"
            exit 1
        fi
    fi
else
    echo ""
    echo "❌ Build failed with exit code $OVERALL_EXIT_CODE"
    exit $OVERALL_EXIT_CODE
fi