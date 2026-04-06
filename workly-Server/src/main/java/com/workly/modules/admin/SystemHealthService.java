package com.workly.modules.admin;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class SystemHealthService {

    private static final int TIMEOUT_MS = 2000;

    private final RestTemplate healthClient;

    public SystemHealthService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(TIMEOUT_MS);
        factory.setReadTimeout(TIMEOUT_MS);
        this.healthClient = new RestTemplate(factory);
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public SystemHealthReport getHealth() {
        log.debug("SystemHealthService: [ENTER] getHealth");
        SystemHealthReport report = new SystemHealthReport();
        report.setServices(collectServiceHealth());
        report.setInfrastructure(collectInfraHealth());
        log.debug("SystemHealthService: [EXIT] getHealth - {} services, {} infra nodes",
                report.getServices().size(), report.getInfrastructure().size());
        return report;
    }

    // -------------------------------------------------------------------------
    // Microservice health
    // -------------------------------------------------------------------------

    private List<ServiceHealth> collectServiceHealth() {
        List<ServiceHealth> results = new ArrayList<>();
        for (ServiceDef def : ServiceDef.values()) {
            results.add(probeService(def));
        }
        return results;
    }

    @SuppressWarnings("unchecked")
    private ServiceHealth probeService(ServiceDef def) {
        ServiceHealth h = new ServiceHealth();
        h.setName(def.displayName);
        h.setPort(def.port);

        String baseUrl = "http://localhost:" + def.port;

        try {
            ResponseEntity<Map> response = healthClient.getForEntity(baseUrl + "/actuator/health", Map.class);
            Map<?, ?> body = response.getBody();
            if (body == null) {
                h.setStatus("UNKNOWN");
                return h;
            }
            Object statusVal = body.get("status");
            h.setStatus(statusVal != null ? String.valueOf(statusVal) : "UNKNOWN");

            // Extract sub-component statuses
            Object comp = body.get("components");
            if (comp instanceof Map<?, ?> compMap) {
                Map<String, String> components = new LinkedHashMap<>();
                compMap.forEach((k, v) -> {
                    if (v instanceof Map<?, ?> vMap) {
                        Object compStatus = vMap.get("status");
                        components.put(String.valueOf(k), compStatus != null ? String.valueOf(compStatus) : "UNKNOWN");
                    }
                });
                h.setComponents(components);
            }
        } catch (Exception e) {
            log.debug("SystemHealthService: {} health probe failed — {}", def.displayName, e.getMessage());
            h.setStatus("DOWN");
            h.setError(e.getMessage());
            return h;
        }

        // JVM memory
        try {
            ResponseEntity<Map> memResp = healthClient.getForEntity(
                    baseUrl + "/actuator/metrics/jvm.memory.used", Map.class);
            Map<?, ?> memBody = memResp.getBody();
            if (memBody != null) {
                List<?> measurements = (List<?>) memBody.get("measurements");
                if (measurements != null && !measurements.isEmpty()) {
                    Object val = ((Map<?, ?>) measurements.get(0)).get("value");
                    if (val instanceof Number) {
                        h.setMemoryUsedBytes(((Number) val).longValue());
                    }
                }
            }
        } catch (Exception ignored) {
            log.trace("SystemHealthService: {} jvm.memory.used unavailable", def.displayName);
        }

        // JVM memory max
        try {
            ResponseEntity<Map> memMaxResp = healthClient.getForEntity(
                    baseUrl + "/actuator/metrics/jvm.memory.max", Map.class);
            Map<?, ?> memMaxBody = memMaxResp.getBody();
            if (memMaxBody != null) {
                List<?> measurements = (List<?>) memMaxBody.get("measurements");
                if (measurements != null && !measurements.isEmpty()) {
                    Object val = ((Map<?, ?>) measurements.get(0)).get("value");
                    if (val instanceof Number n && n.longValue() > 0) {
                        h.setMemoryMaxBytes(n.longValue());
                    }
                }
            }
        } catch (Exception ignored) {
            log.trace("SystemHealthService: {} jvm.memory.max unavailable", def.displayName);
        }

        // CPU usage
        try {
            ResponseEntity<Map> cpuResp = healthClient.getForEntity(
                    baseUrl + "/actuator/metrics/process.cpu.usage", Map.class);
            Map<?, ?> cpuBody = cpuResp.getBody();
            if (cpuBody != null) {
                List<?> measurements = (List<?>) cpuBody.get("measurements");
                if (measurements != null && !measurements.isEmpty()) {
                    Object val = ((Map<?, ?>) measurements.get(0)).get("value");
                    if (val instanceof Number) {
                        h.setCpuUsage(((Number) val).doubleValue());
                    }
                }
            }
        } catch (Exception ignored) {
            log.trace("SystemHealthService: {} process.cpu.usage unavailable", def.displayName);
        }

        return h;
    }

    // -------------------------------------------------------------------------
    // Infrastructure TCP probes
    // -------------------------------------------------------------------------

    private List<InfraHealth> collectInfraHealth() {
        List<InfraHealth> results = new ArrayList<>();
        for (InfraDef def : InfraDef.values()) {
            results.add(probeInfra(def));
        }
        return results;
    }

    private InfraHealth probeInfra(InfraDef def) {
        InfraHealth h = new InfraHealth();
        h.setName(def.displayName);
        h.setCategory(def.category);
        h.setHost(def.host);
        h.setPort(def.port);
        h.setStatus(tcpReachable(def.host, def.port) ? "UP" : "DOWN");
        return h;
    }

    private boolean tcpReachable(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), TIMEOUT_MS);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Definitions
    // -------------------------------------------------------------------------

    private enum ServiceDef {
        SERVER("workly-server", 8080),
        CHAT("workly-chat-service", 8082),
        SEARCH("workly-search-service", 8083),
        CONFIG("workly-config-service", 8084),
        AUTH("workly-auth-service", 8085),
        NOTIFICATION("workly-notification-service", 8086),
        TRACKING("workly-tracking-service", 8087),
        PROFILE("workly-profile-service", 8088),
        MATCHING("workly-matching-service", 8089);

        final String displayName;
        final int port;

        ServiceDef(String displayName, int port) {
            this.displayName = displayName;
            this.port = port;
        }
    }

    private enum InfraDef {
        MONGO_PRIMARY("MongoDB Primary", "DATABASE", "localhost", 27017),
        MONGO_SECONDARY1("MongoDB Secondary 1", "DATABASE", "localhost", 27018),
        MONGO_SECONDARY2("MongoDB Secondary 2", "DATABASE", "localhost", 27019),
        POSTGRESQL("PostgreSQL", "DATABASE", "localhost", 5432),
        PGBOUNCER("PgBouncer", "DATABASE", "localhost", 6432),
        REDIS("Redis Master", "CACHE", "localhost", 6379),
        KAFKA_1("Kafka Broker 1", "MESSAGING", "127.0.0.1", 9094),
        KAFKA_2("Kafka Broker 2", "MESSAGING", "127.0.0.1", 9095),
        KAFKA_3("Kafka Broker 3", "MESSAGING", "127.0.0.1", 9096),
        ELASTICSEARCH("Elasticsearch", "SEARCH", "localhost", 9200),
        PROMETHEUS("Prometheus", "OBSERVABILITY", "localhost", 9090),
        GRAFANA("Grafana", "OBSERVABILITY", "localhost", 3000),
        JAEGER("Jaeger", "OBSERVABILITY", "localhost", 16686);

        final String displayName;
        final String category;
        final String host;
        final int port;

        InfraDef(String displayName, String category, String host, int port) {
            this.displayName = displayName;
            this.category = category;
            this.host = host;
            this.port = port;
        }
    }

    // -------------------------------------------------------------------------
    // DTOs
    // -------------------------------------------------------------------------

    public static class SystemHealthReport {
        private List<ServiceHealth> services;
        private List<InfraHealth> infrastructure;

        public List<ServiceHealth> getServices() { return services; }
        public void setServices(List<ServiceHealth> services) { this.services = services; }
        public List<InfraHealth> getInfrastructure() { return infrastructure; }
        public void setInfrastructure(List<InfraHealth> infrastructure) { this.infrastructure = infrastructure; }
    }

    public static class ServiceHealth {
        private String name;
        private int port;
        private String status;
        private Map<String, String> components;
        private Double cpuUsage;
        private Long memoryUsedBytes;
        private Long memoryMaxBytes;
        private String error;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Map<String, String> getComponents() { return components; }
        public void setComponents(Map<String, String> components) { this.components = components; }
        public Double getCpuUsage() { return cpuUsage; }
        public void setCpuUsage(Double cpuUsage) { this.cpuUsage = cpuUsage; }
        public Long getMemoryUsedBytes() { return memoryUsedBytes; }
        public void setMemoryUsedBytes(Long memoryUsedBytes) { this.memoryUsedBytes = memoryUsedBytes; }
        public Long getMemoryMaxBytes() { return memoryMaxBytes; }
        public void setMemoryMaxBytes(Long memoryMaxBytes) { this.memoryMaxBytes = memoryMaxBytes; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }

    public static class InfraHealth {
        private String name;
        private String category;
        private String host;
        private int port;
        private String status;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}
