package frontend.metrics;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ActiveUserTracker implements Filter {

    private static final Map<String, Instant> activity = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest r = (HttpServletRequest) req;
        String ip = r.getRemoteAddr();
        activity.put(ip, Instant.now());

        // update gauge
        long active = activity.values().stream()
                .filter(t -> t.isAfter(Instant.now().minusSeconds(300))) // Last 5 mins
                .count();
        MetricsRegistry.setGauge("frontend_active_users", active);

        chain.doFilter(req, res);
    }
}
