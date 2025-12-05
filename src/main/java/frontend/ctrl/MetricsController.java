package frontend.ctrl;

import frontend.metrics.MetricsRegistry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MetricsController {

    @GetMapping("/metrics")
    public String metrics() {
        return MetricsRegistry.renderPrometheus();
    }
}
