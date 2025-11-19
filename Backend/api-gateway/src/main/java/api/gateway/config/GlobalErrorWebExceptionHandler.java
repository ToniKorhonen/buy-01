package api.gateway.config;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.*;
import reactor.core.publisher.Mono;
import java.util.Map;
/**
 * Global error handler for API Gateway that ensures security headers
 * are added to all error responses (404, 500, 401, 403, etc.)
 * This fixes ZAP alert about missing X-Content-Type-Options on error pages.
 */
@Component
@Order(-2) // Higher priority than default error handler
public class GlobalErrorWebExceptionHandler extends AbstractErrorWebExceptionHandler {
    public GlobalErrorWebExceptionHandler(
            ErrorAttributes errorAttributes,
            WebProperties.Resources resources,
            ApplicationContext applicationContext,
            ServerCodecConfigurer configurer) {
        super(errorAttributes, resources, applicationContext);
        this.setMessageWriters(configurer.getWriters());
    }
    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
        return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
    }
    private Mono<ServerResponse> renderErrorResponse(ServerRequest request) {
        Map<String, Object> errorAttributes = getErrorAttributes(request, ErrorAttributeOptions.defaults());
        int status = (int) errorAttributes.getOrDefault("status", 500);
        HttpStatus httpStatus = HttpStatus.valueOf(status);
        return ServerResponse
                .status(httpStatus)
                .contentType(MediaType.APPLICATION_JSON)
                // Add all security headers to error responses
                .header("Content-Security-Policy",
                    "default-src 'self'; " +
                    "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
                    "style-src 'self' 'unsafe-inline'; " +
                    "img-src 'self' data: http://localhost:8080 http://localhost:8083; " +
                    "font-src 'self' data:; " +
                    "connect-src 'self' http://localhost:8080 http://localhost:8081 http://localhost:8082 http://localhost:8083 http://localhost:9081; " +
                    "frame-ancestors 'self'; " +
                    "form-action 'self'; " +
                    "base-uri 'self'; " +
                    "object-src 'none';")
                .header("X-Frame-Options", "SAMEORIGIN")
                .header("X-Content-Type-Options", "nosniff")
                .header("X-XSS-Protection", "1; mode=block")
                .header("Referrer-Policy", "strict-origin-when-cross-origin")
                .header("Permissions-Policy",
                    "geolocation=(), microphone=(), camera=(), payment=(), usb=(), " +
                    "magnetometer=(), gyroscope=(), accelerometer=()")
                .body(BodyInserters.fromValue(errorAttributes));
    }
}
