package ea.sof.ms_answers.service;

import ea.sof.shared.showcases.MsAuthShowcase;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "authms", url = "http://104.154.33.123:8080/auth")
public interface AuthService extends MsAuthShowcase {

}
