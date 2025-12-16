package frontend.ctrl;

import com.doda2025_team14.lib_version.VersionUtil;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Map;

@Controller
public class HelloWorldController {

    @Value("${app.version}")
    private String appVersion;

    private final String modelHost;
    private final RestTemplate restTemplate;

    public HelloWorldController(RestTemplateBuilder restBuilder, Environment env) {
        this.restTemplate = restBuilder.build();
        this.modelHost = env.getProperty("MODEL_HOST");
    }

    private String getModelServiceVersion() {
        try {
            if (modelHost == null) {
                return "unknown (MODEL_HOST not set)";
            }
            URI url = new URI(modelHost + "/version");
            Map<String, String> response = restTemplate.getForObject(url, Map.class);
            return response != null ? response.get("version") : "unknown";
        } catch (Exception e) {
            return "unknown (error: " + e.getMessage() + ")";
        }
    }

    @GetMapping("/")
    public String redirectToSms() {
        return "redirect:/sms";
    }

    @GetMapping("/version")
    @ResponseBody
    public String index() {
        return "Library version: " + VersionUtil.getVersion() + "\n" +
               "App version: " + appVersion + "\n" +
               "Model-service version: " + getModelServiceVersion();
    }
}