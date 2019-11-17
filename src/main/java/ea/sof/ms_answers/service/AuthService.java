package ea.sof.ms_answers.service;

import ea.sof.shared.showcases.MsAuthShowcase;
import org.springframework.cloud.openfeign.FeignClient;

//@FeignClient(name = "${feign.name}", url = "${feign.url}")
@FeignClient(name="authService", url = "${AUTHENTICATE_SERVICE}")
public interface AuthService extends MsAuthShowcase {

}
