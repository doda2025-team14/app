package frontend.metrics;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MetricsRegistry {

    
    private static final Map<String, Double> counters = new ConcurrentHashMap<>();


    private static final Map<String, Double> gauges = new ConcurrentHashMap<>();


    private static final Map<String, Double> histograms = new ConcurrentHashMap<>();


    /* Chosen Metrics */

     //  1. Counter
  
    public static void incCounter(String name, Map<String, String> labels) {
        String key = name + labelsToKey(labels);
        counters.merge(key, 1.0, Double::sum);
    }

      // 2. Gauge

    public static void setGauge(String name, double value) {
        gauges.put(name, value);
    }

      // 3. Histogram

    public static void observeHistogram(String name, double value, double[] buckets, Map<String, String> labels) {
        for (double b : buckets) {
            if (value <= b) {
                String key = name + "_bucket" + labelsToKey(labels) + "_le_" + b;
                histograms.merge(key, 1.0, Double::sum);
            }
        }
        String countKey = name + "_count" + labelsToKey(labels);
        histograms.merge(countKey, 1.0, Double::sum);

        String sumKey = name + "_sum" + labelsToKey(labels);
        histograms.merge(sumKey, value, Double::sum);
    }


      // Expose Prometheus format

    public static String renderPrometheus() {
        StringBuilder sb = new StringBuilder();

        counters.forEach((k,v) -> {
            sb.append(k).append(" ").append(v).append("\n");
        });

        gauges.forEach((k,v) -> {
            sb.append(k).append(" ").append(v).append("\n");
        });

        histograms.forEach((k,v) -> {
            sb.append(k).append(" ").append(v).append("\n");
        });

        return sb.toString();
    }

    private static String labelsToKey(Map<String,String> labels) {
        if (labels == null || labels.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("{");
        labels.forEach((k,v) -> sb.append(k).append("=\"").append(v).append("\","));
        sb.append("}");
        return sb.toString();
    }
}
