package ea.sof.ms_answers.service;

import ea.sof.shared.showcases.MsAuthShowcase;
import org.springframework.cloud.openfeign.FeignClient;

//@FeignClient(name = "${feign.name}", url = "${feign.url}")
@FeignClient(name="authService", url = "${AUTHENTICATE_SERVICE}")
/*authService is the name of the object autowired in the contoller using this client*/
public interface AuthService extends MsAuthShowcase {

}
