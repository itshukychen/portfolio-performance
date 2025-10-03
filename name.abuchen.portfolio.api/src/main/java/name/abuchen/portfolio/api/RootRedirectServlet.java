package name.abuchen.portfolio.api;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Root redirect servlet that provides API information at the root path
 */
public class RootRedirectServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type");
        
        Map<String, Object> apiInfo = new HashMap<>();
        apiInfo.put("name", "Portfolio Performance API Server");
        apiInfo.put("version", "0.78.2-SNAPSHOT");
        apiInfo.put("status", "running");
        apiInfo.put("timestamp", LocalDateTime.now().toString());
        apiInfo.put("message", "Welcome to Portfolio Performance API!");
        apiInfo.put("documentation", "Available endpoints at /api/v1");
        apiInfo.put("endpoints", new String[]{
            "/api/v1/hello",
            "/api/v1/health", 
            "/api/v1/info",
            "/api/v1/portfolio/info",
            "/api/v1/portfolio/statistics",
            "/api/v1/files/open",
            "/api/v1/files/info",
            "/api/v1/files/cache/stats",
            "/api/v1/files/cache/clear",
            "/api/v1/files/health"
        });
        
        String json = mapToJson(apiInfo);
        response.getWriter().write(json);
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
}
