import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple Portfolio Performance API Server
 * 
 * A standalone HTTP server that exposes the Portfolio Performance API endpoints
 * without requiring the full OSGi runtime.
 */
public class SimpleApiServer {
    
    private static final int PORT = 8080;
    private static final String API_BASE_PATH = "/api/v1";
    private ServerSocket serverSocket;
    private boolean running = false;
    
    public static void main(String[] args) {
        System.out.println("🚀 Starting Portfolio Performance API Server...");
        System.out.println("🌐 Server will be available at: http://localhost:" + PORT);
        System.out.println("📡 API endpoints: http://localhost:" + PORT + API_BASE_PATH);
        System.out.println("🛑 Press Ctrl+C to stop the server");
        System.out.println();
        
        SimpleApiServer server = new SimpleApiServer();
        server.start();
    }
    
    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            running = true;
            
            System.out.println("✅ API Server started successfully!");
            System.out.println("📋 Available endpoints:");
            System.out.println("   GET " + API_BASE_PATH + "/hello");
            System.out.println("   GET " + API_BASE_PATH + "/health");
            System.out.println("   GET " + API_BASE_PATH + "/info");
            System.out.println();
            
            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    handleRequest(clientSocket);
                } catch (IOException e) {
                    if (running) {
                        System.err.println("❌ Error accepting connection: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("❌ Failed to start server: " + e.getMessage());
            System.exit(1);
        }
    }
    
    private void handleRequest(Socket clientSocket) {
        new Thread(() -> {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
                
                String requestLine = in.readLine();
                if (requestLine != null) {
                    System.out.println("📨 " + requestLine);
                    
                    // Parse the request
                    String[] parts = requestLine.split(" ");
                    if (parts.length >= 2) {
                        String method = parts[0];
                        String path = parts[1];
                        
                        if ("GET".equals(method)) {
                            handleGetRequest(path, out);
                        } else {
                            sendMethodNotAllowed(out);
                        }
                    } else {
                        sendBadRequest(out);
                    }
                }
            } catch (IOException e) {
                System.err.println("❌ Error handling request: " + e.getMessage());
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }).start();
    }
    
    private void handleGetRequest(String path, PrintWriter out) {
        if (path.equals(API_BASE_PATH + "/hello")) {
            handleHelloEndpoint(out);
        } else if (path.equals(API_BASE_PATH + "/health")) {
            handleHealthEndpoint(out);
        } else if (path.equals(API_BASE_PATH + "/info")) {
            handleInfoEndpoint(out);
        } else if (path.equals("/") || path.equals(API_BASE_PATH)) {
            handleRootEndpoint(out);
        } else {
            sendNotFound(out);
        }
    }
    
    private void handleHelloEndpoint(PrintWriter out) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Hello from Portfolio Performance API!");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", "running");
        response.put("version", "0.78.2-SNAPSHOT");
        
        sendJsonResponse(out, response);
    }
    
    private void handleHealthEndpoint(PrintWriter out) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Portfolio Performance API");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("uptime", "running");
        
        sendJsonResponse(out, response);
    }
    
    private void handleInfoEndpoint(PrintWriter out) {
        Map<String, Object> response = new HashMap<>();
        response.put("name", "Portfolio Performance API");
        response.put("version", "0.78.2-SNAPSHOT");
        response.put("description", "REST API for Portfolio Performance");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("endpoints", new String[]{
            API_BASE_PATH + "/hello",
            API_BASE_PATH + "/health", 
            API_BASE_PATH + "/info"
        });
        
        sendJsonResponse(out, response);
    }
    
    private void handleRootEndpoint(PrintWriter out) {
        Map<String, Object> response = new HashMap<>();
        response.put("name", "Portfolio Performance API Server");
        response.put("version", "0.78.2-SNAPSHOT");
        response.put("status", "running");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("message", "Welcome to Portfolio Performance API!");
        response.put("documentation", "Available endpoints at " + API_BASE_PATH);
        
        sendJsonResponse(out, response);
    }
    
    private void sendJsonResponse(PrintWriter out, Map<String, Object> data) {
        String json = mapToJson(data);
        
        out.println("HTTP/1.1 200 OK");
        out.println("Content-Type: application/json; charset=UTF-8");
        out.println("Content-Length: " + json.getBytes().length);
        out.println("Access-Control-Allow-Origin: *");
        out.println("Access-Control-Allow-Methods: GET, POST, OPTIONS");
        out.println("Access-Control-Allow-Headers: Content-Type");
        out.println("Connection: close");
        out.println();
        out.println(json);
        out.flush();
    }
    
    private void sendNotFound(PrintWriter out) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", "Not Found");
        error.put("message", "API endpoint not found");
        error.put("timestamp", LocalDateTime.now().toString());
        
        String json = mapToJson(error);
        
        out.println("HTTP/1.1 404 Not Found");
        out.println("Content-Type: application/json; charset=UTF-8");
        out.println("Content-Length: " + json.getBytes().length);
        out.println("Access-Control-Allow-Origin: *");
        out.println("Connection: close");
        out.println();
        out.println(json);
        out.flush();
    }
    
    private void sendMethodNotAllowed(PrintWriter out) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", "Method Not Allowed");
        error.put("message", "Only GET requests are supported");
        error.put("timestamp", LocalDateTime.now().toString());
        
        String json = mapToJson(error);
        
        out.println("HTTP/1.1 405 Method Not Allowed");
        out.println("Content-Type: application/json; charset=UTF-8");
        out.println("Content-Length: " + json.getBytes().length);
        out.println("Access-Control-Allow-Origin: *");
        out.println("Connection: close");
        out.println();
        out.println(json);
        out.flush();
    }
    
    private void sendBadRequest(PrintWriter out) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", "Bad Request");
        error.put("message", "Invalid request format");
        error.put("timestamp", LocalDateTime.now().toString());
        
        String json = mapToJson(error);
        
        out.println("HTTP/1.1 400 Bad Request");
        out.println("Content-Type: application/json; charset=UTF-8");
        out.println("Content-Length: " + json.getBytes().length);
        out.println("Access-Control-Allow-Origin: *");
        out.println("Connection: close");
        out.println();
        out.println(json);
        out.flush();
    }
    
    private String mapToJson(Map<String, Object> map) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) {
                json.append(",");
            }
            first = false;
            
            json.append("\"").append(entry.getKey()).append("\":");
            
            Object value = entry.getValue();
            if (value instanceof String) {
                json.append("\"").append(escapeJson((String) value)).append("\"");
            } else if (value instanceof Number || value instanceof Boolean) {
                json.append(value);
            } else if (value instanceof Object[]) {
                json.append("[");
                Object[] array = (Object[]) value;
                for (int i = 0; i < array.length; i++) {
                    if (i > 0) json.append(",");
                    json.append("\"").append(escapeJson(array[i].toString())).append("\"");
                }
                json.append("]");
            } else {
                json.append("\"").append(escapeJson(value.toString())).append("\"");
            }
        }
        
        json.append("}");
        return json.toString();
    }
    
    private String escapeJson(String str) {
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
    
    public void stop() {
        running = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error stopping server: " + e.getMessage());
        }
    }
}
