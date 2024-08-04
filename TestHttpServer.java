import co.elastic.apm.api.ElasticApm;
import co.elastic.apm.api.Transaction;
import co.elastic.apm.api.Span;
import java.io.IOException;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.net.InetSocketAddress;

public class TestHttpServer {
    public static void main(String[] args) throws IOException {
        // Create an HTTP server and bind it to port 8001
        HttpServer server = HttpServer.create(new InetSocketAddress(8001), 0);

        // Define handlers for the /api/simulate endpoint
        server.createContext("/api/simulate", new ApiHandler());

        // Start the server
        server.setExecutor(null);
        server.start();
        System.out.println("Server is listening on port 8001");

        // Start a main transaction
        Transaction mainTransaction = ElasticApm.startTransaction();
        try {
            mainTransaction.setName("Main Transaction");
            mainTransaction.setType("custom");

            // Run a loop to generate continuous traces
            while (true) {
                // Simulate different API calls
                simulateApiCall("GET /api/users");
                simulateApiCall("POST /api/orders");
                simulateApiCall("PUT /api/products/123");
                simulateApiCall("DELETE /api/items/456");
                simulateApiCall("GET /api/error");

                // Sleep to simulate continuous work and control trace generation rate
                try {
                    Thread.sleep(5000); // Adjust the sleep duration as needed
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Thread.currentThread().interrupt(); // Preserve interrupt status
                }
            }
        } finally {
            // End the main transaction (this part may be reached when stopping the application)
            mainTransaction.end();
        }
    }

    private static void simulateApiCall(String apiEndpoint) {
        // Start a new transaction for each API call
        Transaction transaction = ElasticApm.startTransaction();
        try {
            transaction.setName(apiEndpoint);
            transaction.setType("request");

            // Simulate work by creating spans
            Span span = transaction.createSpan();
            try {
                span.setName("Handle " + apiEndpoint);
                span.setType("operation");
                span.addLabel("api.endpoint", apiEndpoint);

                // Custom error conditions
                if (apiEndpoint.contains("error")) {
                    throw new CustomException("Simulated error for " + apiEndpoint, 1001);
                } else if (apiEndpoint.contains("users")) {
                    throw new CustomException("User data fetch error for " + apiEndpoint, 1002);
                } else if (apiEndpoint.contains("orders")) {
                    throw new CustomException("Order processing error for " + apiEndpoint, 1003);
                } else if (apiEndpoint.contains("products")) {
                    throw new CustomException("Product update error for " + apiEndpoint, 1004);
                } else if (apiEndpoint.contains("items")) {
                    throw new CustomException("Item deletion error for " + apiEndpoint, 1005);
                }

                simulateWork(); // Simulate work duration
            } catch (CustomException e) {
                // Capture custom error in span
                span.captureException(e);
                span.addLabel("error.code", e.getErrorCode());
                span.addLabel("http.status_code", "500"); // Simulate HTTP 500 Internal Server Error
            } finally {
                span.end(); // End the span
            }
        } finally {
            // End the transaction
            transaction.end();
        }
    }

    private static void simulateWork() {
        try {
            // Simulate some work
            Thread.sleep(1000); // Simulate work duration
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt(); // Preserve interrupt status
        }
    }

    // Custom exception class
    static class CustomException extends Exception {
        private final int errorCode;

        public CustomException(String message, int errorCode) {
            super(message);
            this.errorCode = errorCode;
        }

        public int getErrorCode() {
            return errorCode;
        }
    }

    static class ApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            String response = "{\"message\": \"Simulated API call successful\"}";
            if (query != null && query.contains("endpoint=")) {
                String apiEndpoint = query.split("endpoint=")[1];
                simulateApiCall(apiEndpoint);
            }
            exchange.sendResponseHeaders(200, response.getBytes().length);
            exchange.getResponseBody().write(response.getBytes());
            exchange.close();
        }
    }
}


// Reach our rahul.fiem@gmail.com for more details.