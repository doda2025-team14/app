package frontend.metrics;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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
        String key = name + "_bucket";
        String labelKey = labelsToKeyWithLe(labels, b);
        histograms.merge(key + labelKey, 1.0, Double::sum);

        if (value <= b) break;
    }

    String countKey = name + "_count" + labelsToKey(labels);
    histograms.merge(countKey, 1.0, Double::sum);

    String sumKey = name + "_sum" + labelsToKey(labels);
    histograms.merge(sumKey, value, Double::sum);
}

    private static String labelsToKeyWithLe(Map<String, String> labels, double le) {
    StringBuilder sb = new StringBuilder("{");

    if (labels != null) {
        labels.forEach((k,v) -> sb.append(k).append("=\"").append(v).append("\","));
    }

    sb.append("le=\"").append(le).append("\"}");
    return sb.toString();
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
    return labels.entrySet().stream()
        .map(e -> e.getKey() + "=\"" + e.getValue() + "\"")
        .collect(Collectors.joining(",", "{", "}"));
}



   public static void incGauge(String name, double delta) {
    gauges.merge(name, delta, (oldValue, newValue) -> oldValue + newValue);
}



    public static Map<String, Double> getCounters() {
        return Map.copyOf(counters);
    }

    public static Map<String, Double> getGauges() {
        return Map.copyOf(gauges);
    }

    public static Map<String, Double> getHistograms() {
    StringBuilder sb = new StringBuilder();
    histograms.entrySet().stream()
    .sorted(Map.Entry.comparingByKey())
    .forEach(e -> sb.append(e.getKey()).append(" ").append(e.getValue()).append("\n"));

    return Map.copyOf(histograms);
}

}


