package odme.sampling;

import odme.sampling.model.Parameter;
import odme.sampling.model.Scenario;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Parses a scenario .yaml file into a {@link Scenario} object.
 * Supports hierarchical YAML structures, categorical parameters, numerical
 * parameters with min/max, distribution parameters, and HasConstraint blocks.
 */
public class ScenarioParser {

    public static List<String> constraintList = new ArrayList<>();

    @SuppressWarnings("unchecked")
    public Scenario parse(String yamlFilePath) throws FileNotFoundException {
        // Reset per-parse to avoid constraint leakage between calls
        constraintList = new ArrayList<>();

        Yaml yaml = new Yaml();
        try (InputStream inputStream = new FileInputStream(yamlFilePath)) {
            Object loaded = yaml.load(inputStream);

            if (!(loaded instanceof Map)) {
                Scenario empty = new Scenario();
                empty.setParameters(new ArrayList<>());
                return empty;
            }

            Map<String, Object> data = (Map<String, Object>) loaded;
            String rootKey = data.containsKey("Scenario") ? "Scenario" :
                    (data.isEmpty() ? null : data.keySet().iterator().next());

            Scenario scenario = new Scenario();
            scenario.setParameters(new ArrayList<>());

            if (rootKey == null) return scenario;

            Object rootValue = data.get(rootKey);
            Map<String, Object> scenarioData;
            if (rootValue instanceof Map) {
                scenarioData = (Map<String, Object>) rootValue;
            } else {
                scenarioData = new LinkedHashMap<>();
            }

            for (Map.Entry<String, Object> entry : scenarioData.entrySet()) {
                parseEntity(entry.getKey(), entry.getValue(), scenario);
                scenario.setConstraint(constraintList);
            }

            return scenario;

        } catch (IOException e) {
            throw new RuntimeException("Failed to read YAML file: " + yamlFilePath, e);
        }
    }

    @SuppressWarnings("unchecked")
    private void parseEntity(String entityName, Object entityValue, Scenario scenario) {
        if (entityValue == null) return;

        if (entityValue instanceof List) {
            for (Object item : (List<?>) entityValue) {
                if (item instanceof Map) {
                    Map<String, Object> paramMap = (Map<String, Object>) item;
                    if (!paramMap.isEmpty()) {
                        String paramKey = paramMap.keySet().iterator().next();
                        parseParameter(entityName, paramKey, paramMap.get(paramKey), scenario);
                    }
                }
            }
            return;
        }

        if (entityValue instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) entityValue;

            // Check if all values are null/empty-map → treat as a categorical parameter
            boolean allNullOrEmpty = true;
            for (Object v : map.values()) {
                if (v != null) {
                    if (!(v instanceof Map) || !((Map<?, ?>) v).isEmpty()) {
                        allNullOrEmpty = false;
                        break;
                    }
                }
            }
            if (allNullOrEmpty && !map.isEmpty()) {
                Parameter p = new Parameter();
                p.setName(entityName);
                p.setType("categorical");
                p.setOptions(new ArrayList<>(map.keySet()));
                scenario.getParameters().add(p);
                return;
            }

            for (Map.Entry<String, Object> e : map.entrySet()) {
                String subKey = e.getKey();
                Object subVal = e.getValue();
                if (subVal == null) continue;

                if (subVal instanceof Map) {
                    Map<String, Object> subMap = (Map<String, Object>) subVal;
                    if (isParameterDetailMap(subMap)) {
                        parseParameter(entityName, subKey, subMap, scenario);
                    } else {
                        boolean subAllNullOrEmpty = true;
                        for (Object vv : subMap.values()) {
                            if (vv != null) {
                                if (!(vv instanceof Map) || !((Map<?, ?>) vv).isEmpty()) {
                                    subAllNullOrEmpty = false;
                                    break;
                                }
                            }
                        }
                        if (subAllNullOrEmpty && !subMap.isEmpty()) {
                            Parameter p = new Parameter();
                            p.setName(entityName + "_" + subKey);
                            p.setType("categorical");
                            p.setOptions(new ArrayList<>(subMap.keySet()));
                            scenario.getParameters().add(p);
                        } else {
                            parseEntity(entityName + "_" + subKey, subMap, scenario);
                        }
                    }
                } else if (subVal instanceof List) {
                    parseEntity(entityName + "_" + subKey, subVal, scenario);
                }
            }
        }
    }

    private boolean isParameterDetailMap(Map<String, Object> map) {
        if (map == null || map.isEmpty()) return false;
        Set<String> indicatorKeys = new HashSet<>(Arrays.asList(
                "type", "min", "max", "options", "IntraConstraint", "InterConstraint",
                "distributionName", "distributionDetails"));
        for (String key : map.keySet()) {
            if (indicatorKeys.contains(key)) return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private void parseParameter(String entityName, String paramKey, Object paramValue, Scenario scenario) {
        if ("HasConstraint".equals(paramKey)) {
            if (paramValue instanceof Map) {
                Object intra = ((Map<?, ?>) paramValue).get("IntraConstraint");
                Object inter = ((Map<?, ?>) paramValue).get("InterConstraint");
                if (intra instanceof String) constraintList.add((String) intra);
                if (inter instanceof String) constraintList.add((String) inter);
            }
            return;
        }

        if (paramValue instanceof Map) {
            Map<String, Object> details = (Map<String, Object>) paramValue;

            Object optionsObj = details.get("options");
            if (optionsObj instanceof List) {
                Parameter param = new Parameter();
                param.setName(entityName + "_" + paramKey);
                param.setType("categorical");
                param.setOptions(new ArrayList<>((List<String>) optionsObj));
                scenario.getParameters().add(param);
                return;
            }

            Parameter param = new Parameter();
            param.setName(entityName + "_" + paramKey);

            Object typeObj = details.get("type");
            param.setType(typeObj instanceof String ? (String) typeObj : "numerical");

            Object minVal = details.get("min");
            if (minVal instanceof Number) {
                param.setMin(((Number) minVal).doubleValue());
            } else if (minVal instanceof String) {
                try { param.setMin(Double.parseDouble((String) minVal)); } catch (NumberFormatException ignored) {}
            }

            Object maxVal = details.get("max");
            if (maxVal instanceof Number) {
                param.setMax(((Number) maxVal).doubleValue());
            } else if (maxVal instanceof String) {
                try { param.setMax(Double.parseDouble((String) maxVal)); } catch (NumberFormatException ignored) {}
            }

            Object distName = details.get("distributionName");
            if (distName instanceof String) param.setDistributionName((String) distName);

            Object distDetails = details.get("distributionDetails");
            if (distDetails instanceof String) param.setDistributionDetails((String) distDetails);

            scenario.getParameters().add(param);
        }
    }
}
