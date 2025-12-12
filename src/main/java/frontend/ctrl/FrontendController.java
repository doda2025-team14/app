package frontend.ctrl;

import java.net.URI;
import java.net.URISyntaxException;

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
import frontend.metrics.MetricsRegistry;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping(path = "/sms")
public class FrontendController {

    private final String modelHost;
    private final RestTemplate restTemplate;

    public FrontendController(RestTemplateBuilder restBuilder, Environment env) {
        this.restTemplate = restBuilder.build();
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

            // Success counter
            MetricsRegistry.incCounter(
                    "frontend_sms_requests_total",
                    Map.of("result", sms.result, "status", "success")
            );

            return sms;

        } catch (Exception e) {

            // Failure counter
            MetricsRegistry.incCounter(
                    "frontend_sms_requests_total",
                    Map.of("result", "unknown", "status", "failure")
            );

            throw e;

        } finally {
            double seconds = (System.nanoTime() - start) / 1_000_000_000.0;

            // Latency histogram
            MetricsRegistry.observeHistogram(
                    "frontend_prediction_latency_seconds",
                    seconds,
                    new double[]{0.1, 0.2, 0.5, 1, 2, 5},   // Prometheus buckets
                    Map.of("status", success ? "success" : "failure")
            );
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
