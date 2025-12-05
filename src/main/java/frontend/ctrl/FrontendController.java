package frontend.ctrl;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import frontend.data.Sms;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping(path = "/sms")
public class FrontendController {

    private final String modelHost;
    private final RestTemplate restTemplate;
    private final MeterRegistry registry;

    public FrontendController(RestTemplateBuilder restBuilder, Environment env, MeterRegistry registry) {
        this.restTemplate = restBuilder.build();
        this.registry = registry;
        this.modelHost = env.getProperty("MODEL_HOST");
        assertModelHost();
    }

    private void assertModelHost() {
        if (modelHost == null || modelHost.strip().isEmpty()) {
            System.err.println("ERROR: ENV variable MODEL_HOST is null or empty");
            System.exit(1);
        }
        if (!modelHost.contains("://")) {
            System.err.printf("ERROR: ENV variable MODEL_HOST is missing protocol, like \"http://...\" (was: \"%s\")%n", modelHost);
            System.exit(1);
        }
        System.out.printf("Working with MODEL_HOST=\"%s\"%n", modelHost);
    }

    @GetMapping("")
    public String redirectToSlash(HttpServletRequest request) {
        return "redirect:" + request.getRequestURI() + "/";
    }

    @GetMapping("/")
    public String index(Model m) {
        m.addAttribute("hostname", modelHost);
        return "sms/index";
    }

    @PostMapping({ "", "/" })
    @ResponseBody
    public Sms predict(@RequestBody Sms sms) {
        long start = System.nanoTime();
        boolean success = false;

        try {
            System.out.printf("Requesting prediction for \"%s\" ...%n", sms.sms);
            sms.result = getPrediction(sms);
            System.out.printf("Prediction: %s%n", sms.result);
            success = true;
            return sms;
        } catch (Exception e) {
            throw e;
        } finally {
            double seconds = (System.nanoTime() - start) / 1_000_000_000.0;

            // Record latency
            Timer.builder("frontend_prediction_latency_seconds")
                 .tags("status", success ? "success" : "failure")
                 .publishPercentileHistogram()
                 .register(registry)
                 .record(Duration.ofMillis((long)(seconds * 1000)));

            // Increment counters
            registry.counter("frontend_sms_requests_total", "result", success ? sms.result : "unknown", "status", success ? "success" : "failure")
                    .increment();
        }
    }

    private String getPrediction(Sms sms) {
        try {
            URI url = new URI(modelHost + "/predict");
            return restTemplate.postForEntity(url, sms, Sms.class).getBody().result.trim();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
