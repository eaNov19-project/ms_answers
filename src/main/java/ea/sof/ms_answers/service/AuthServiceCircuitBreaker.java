package ea.sof.ms_answers.service;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import ea.sof.shared.models.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
//@Slf4j
public class AuthServiceCircuitBreaker {
    @Autowired
    AuthService authService;

    @HystrixCommand(fallbackMethod = "fallback")
    public ResponseEntity<Response> validateToken(String token){
        return authService.validateToken(token);
    }

    public ResponseEntity<Response> fallback(String token) {
//        log.warn("AuthService is not available: validateToken fallback");
        return ResponseEntity.ok(new Response(false, "Authentication service is unavailable. Try later"));
    }
}
