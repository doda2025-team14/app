package frontend.metrics;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class MetricsController {

    @GetMapping(value = "/metrics", produces = MediaType.TEXT_PLAIN_VALUE) // Prometheus scrapes this
    public String metrics() {
        StringBuilder sb = new StringBuilder();

        // Counters
        MetricsRegistry.getCounters().forEach((k, v) -> {
            sb.append(k).append(" ").append(v.doubleValue()).append("\n");
        });

        // Gauges
        MetricsRegistry.getGauges().forEach((k, v) -> {
            sb.append(k).append(" ").append(v.doubleValue()).append("\n");
        });

        // Histograms (sorted by key)
        MetricsRegistry.getHistograms().entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(e -> sb.append(e.getKey()).append(" ").append(e.getValue().doubleValue()).append("\n"));

        return sb.toString();
    }
}
