package frontend.metrics;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(1)  // make sure filter runs early
public class ActiveUserTracker implements Filter {

    // Track last activity timestamps per IP
    private static final Map<String, Instant> activity = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // Register a gauge using MetricsRegistry
        MetricsRegistry.setGauge("frontend_active_users", 0); // initial value
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest r = (HttpServletRequest) req;
        String ip = r.getRemoteAddr();

        // Record activity
        activity.put(ip, Instant.now());

        // Update gauge
        long active = activity.values().stream()
                .filter(t -> t.isAfter(Instant.now().minusSeconds(300))) // last 5 min
                .count();
        MetricsRegistry.setGauge("frontend_active_users", active);

        chain.doFilter(req, res);
    }
}
