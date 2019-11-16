package ea.sof.ms_answers.service;

import ea.sof.shared.showcases.MsAuthShowcase;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "${feign.name}", url = "${feign.url}")
public interface AuthService extends MsAuthShowcase {

}
