package odme.domain.prune;

import odme.domain.model.SESNodeType;

/**
 * Centralises the suffix-based naming conventions used throughout the SES
 * model for detecting and manipulating node types.
 *
 * <p>These rules are scattered across {@code JtreeToGraphPrune},
 * {@code JtreeToGraphGeneral}, and {@code JtreeToGraphAdd} in the legacy
 * code. This class provides a single authoritative source.</p>
 */
public final class NamingConventions {

    /** Suffix for aspect (decomposition) nodes. */
    public static final String ASPECT_SUFFIX = "Dec";

    /** Suffix for specialization nodes. */
    public static final String SPECIALIZATION_SUFFIX = "Spec";

    /** Suffix for multi-aspect nodes. */
    public static final String MULTI_ASPECT_SUFFIX = "MAsp";

    /** Suffix used for variable marker nodes in the graph. */
    public static final String VARIABLE_SUFFIX = "Var";

    /** Suffix used for behaviour marker nodes in the graph. */
    public static final String BEHAVIOUR_SUFFIX = "Behaviour";

    /** Suffix used for reference (uniformity) nodes. */
    public static final String REF_NODE_SUFFIX = "RefNode";

    /** Suffix used for constraint nodes. */
    public static final String CONSTRAINT_SUFFIX = "Con";

    /** Suffix used for sequence nodes. */
    public static final String SEQUENCE_SUFFIX = "Seq";

    private NamingConventions() {
        // utility class
    }

    // -------------------------------------------------------------------------
    // Type detection
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} if the node name represents an entity (no
     * recognised structural suffix).
     *
     * @param name the node name
     * @return {@code true} for entity nodes
     */
    public static boolean isEntity(String name) {
        return classify(name) == SESNodeType.ENTITY;
    }

    /**
     * Returns {@code true} if the node name ends with the aspect suffix
     * ({@value #ASPECT_SUFFIX}).
     *
     * @param name the node name
     * @return {@code true} for aspect nodes
     */
    public static boolean isAspect(String name) {
        return name != null && name.endsWith(ASPECT_SUFFIX);
    }

    /**
     * Returns {@code true} if the node name ends with the specialization
     * suffix ({@value #SPECIALIZATION_SUFFIX}).
     *
     * @param name the node name
     * @return {@code true} for specialization nodes
     */
    public static boolean isSpecialization(String name) {
        return name != null && name.endsWith(SPECIALIZATION_SUFFIX);
    }

    /**
     * Returns {@code true} if the node name ends with the multi-aspect
     * suffix ({@value #MULTI_ASPECT_SUFFIX}).
     *
     * @param name the node name
     * @return {@code true} for multi-aspect nodes
     */
    public static boolean isMultiAspect(String name) {
        return name != null && name.endsWith(MULTI_ASPECT_SUFFIX);
    }

    // -------------------------------------------------------------------------
    // Suffix manipulation
    // -------------------------------------------------------------------------

    /**
     * Appends the correct suffix for the given {@link SESNodeType} to a base name,
     * unless the name already has that suffix.
     *
     * @param baseName the base node name
     * @param type     the target node type
     * @return the name with the appropriate suffix
     */
    public static String addSuffix(String baseName, SESNodeType type) {
        if (baseName == null) return null;
        String suffix = type.getLabelSuffix();
        if (suffix.isEmpty() || baseName.endsWith(suffix)) {
            return baseName;
        }
        return baseName + suffix;
    }

    /**
     * Removes any recognised structural suffix from a node name.
     *
     * @param name the full node name (e.g. "vehicleDec")
     * @return the base name without suffix (e.g. "vehicle")
     */
    public static String removeSuffix(String name) {
        if (name == null) return null;
        // Check longest suffixes first to avoid partial matches
        for (String suffix : new String[]{MULTI_ASPECT_SUFFIX, SPECIALIZATION_SUFFIX, ASPECT_SUFFIX}) {
            if (name.endsWith(suffix)) {
                return name.substring(0, name.length() - suffix.length());
            }
        }
        return name;
    }

    /**
     * Returns the base name of a node by stripping any recognised structural suffix.
     * Alias for {@link #removeSuffix(String)}.
     *
     * @param name the full node name
     * @return the base name
     */
    public static String getBaseName(String name) {
        return removeSuffix(name);
    }

    /**
     * Ensures that a node name has the correct suffix matching its current
     * type. If the current name has a different structural suffix, the old
     * suffix is preserved (this mirrors the renaming logic in the legacy
     * code where the suffix is forced to match the current cell type).
     *
     * @param currentName the existing node name (determines the required suffix)
     * @param newName     the proposed new name
     * @return the new name with the correct suffix ensured
     */
    public static String ensureSuffix(String currentName, String newName) {
        if (currentName == null || newName == null) return newName;

        if (currentName.endsWith(ASPECT_SUFFIX) && !newName.endsWith(ASPECT_SUFFIX)) {
            return newName + ASPECT_SUFFIX;
        }
        if (currentName.endsWith(SPECIALIZATION_SUFFIX) && !newName.endsWith(SPECIALIZATION_SUFFIX)) {
            return newName + SPECIALIZATION_SUFFIX;
        }
        if (currentName.endsWith(MULTI_ASPECT_SUFFIX) && !newName.endsWith(MULTI_ASPECT_SUFFIX)) {
            return newName + MULTI_ASPECT_SUFFIX;
        }
        return newName;
    }

    /**
     * Classifies a node name into its {@link SESNodeType} based on suffix.
     * Delegates to {@link SESNodeType#fromLabel(String)}.
     *
     * @param name the node name
     * @return the detected type
     */
    public static SESNodeType classify(String name) {
        return SESNodeType.fromLabel(name);
    }
}
