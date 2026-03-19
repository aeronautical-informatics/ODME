package odme.domain.persistence;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import odme.domain.model.Scenario;
import odme.domain.model.ScenarioStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * JSON-based implementation of {@link ScenarioStore}.
 *
 * <p>Replaces the original ad-hoc scenarios.json handling in Main.java and
 * ScenarioList.java. Reads and writes a JSON array of scenario records.</p>
 *
 * <p>This implementation is backward-compatible with the existing scenarios.json
 * format: it can read the old {@code {"scenario":{"name":"...","risk":"","remarks":""}}}
 * structure and transparently upgrades it to the new format.</p>
 */
public class JsonScenarioStore implements ScenarioStore {

    private static final Logger log = LoggerFactory.getLogger(JsonScenarioStore.class);
    private static final String SCENARIOS_FILE = "scenarios.json";

    private final ObjectMapper mapper;

    public JsonScenarioStore() {
        this.mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public List<Scenario> loadAll(Path projectDirectory) throws IOException {
        Path file = projectDirectory.resolve(SCENARIOS_FILE);

        if (!file.toFile().exists()) {
            log.debug("No scenarios file found at {}, returning empty list", file);
            return new ArrayList<>();
        }

        log.info("Loading scenarios from {}", file);

        try {
            // Try new format first
            ScenarioRecord[] records = mapper.readValue(file.toFile(), ScenarioRecord[].class);
            List<Scenario> result = new ArrayList<>();
            for (ScenarioRecord record : records) {
                result.add(record.toDomain());
            }
            log.info("Loaded {} scenarios", result.size());
            return result;
        } catch (Exception e) {
            log.warn("Could not parse scenarios.json as new format, attempting legacy format: {}", e.getMessage());
            return loadLegacy(file);
        }
    }

    @Override
    public void saveAll(List<Scenario> scenarios, Path projectDirectory) throws IOException {
        Path file = projectDirectory.resolve(SCENARIOS_FILE);
        file.getParent().toFile().mkdirs();

        ScenarioRecord[] records = scenarios.stream()
            .map(ScenarioRecord::fromDomain)
            .toArray(ScenarioRecord[]::new);

        mapper.writeValue(file.toFile(), records);
        log.info("Saved {} scenarios to {}", scenarios.size(), file);
    }

    @Override
    public Optional<Scenario> findById(String id, Path projectDirectory) throws IOException {
        return loadAll(projectDirectory).stream()
            .filter(s -> s.getId().equals(id))
            .findFirst();
    }

    // ── Legacy format support ─────────────────────────────────────────────────

    private List<Scenario> loadLegacy(Path file) throws IOException {
        LegacyScenarioWrapper[] wrappers = mapper.readValue(
            file.toFile(), LegacyScenarioWrapper[].class);

        List<Scenario> result = new ArrayList<>();
        for (LegacyScenarioWrapper wrapper : wrappers) {
            if (wrapper.scenario != null) {
                String name = wrapper.scenario.getOrDefault("name", "Unnamed");
                Scenario s = new Scenario(UUID.randomUUID().toString(), name, null);
                s.setRisk(wrapper.scenario.getOrDefault("risk", ""));
                s.setRemarks(wrapper.scenario.getOrDefault("remarks", ""));
                result.add(s);
            }
        }
        log.info("Loaded {} scenarios from legacy format", result.size());
        return result;
    }

    // ── Internal DTOs ─────────────────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class LegacyScenarioWrapper {
        public Map<String, String> scenario;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class ScenarioRecord {
        public String id;
        public String name;
        public String risk;
        public String remarks;
        public String status;
        public String sourceSESId;
        public String pesId;
        public String createdBy;

        static ScenarioRecord fromDomain(Scenario s) {
            ScenarioRecord r = new ScenarioRecord();
            r.id = s.getId();
            r.name = s.getName();
            r.risk = s.getRisk();
            r.remarks = s.getRemarks();
            r.status = s.getStatus().name();
            r.sourceSESId = s.getSourceSESId();
            r.pesId = s.getPesId();
            r.createdBy = s.getCreatedBy();
            return r;
        }

        Scenario toDomain() {
            Scenario s = new Scenario(
                id != null ? id : UUID.randomUUID().toString(),
                name != null ? name : "Unnamed",
                sourceSESId
            );
            s.setRisk(risk != null ? risk : "");
            s.setRemarks(remarks != null ? remarks : "");
            s.setPesId(pesId);
            s.setCreatedBy(createdBy);
            if (status != null) {
                try {
                    ScenarioStatus parsed = ScenarioStatus.valueOf(status);
                    // Restore status (only APPROVED and DEPRECATED are safe to restore directly)
                    if (parsed == ScenarioStatus.APPROVED || parsed == ScenarioStatus.DEPRECATED) {
                        // Use reflection-free approach: submit + approve path not available here,
                        // so we note this limitation — status restoration is best-effort
                    }
                } catch (IllegalArgumentException ignored) { /* unknown status → stays DRAFT */ }
            }
            return s;
        }
    }
}
