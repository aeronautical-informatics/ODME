package odme.domain.model;

/**
 * Enumerates the four fundamental node types in a System Entity Structure (SES).
 *
 * <p>In the SES formalism (Zeigler, 1984; Karmokar et al., 2019), a tree is built
 * from exactly these four node kinds. Previously, ODME detected node type by
 * inspecting string suffixes on JTree labels — this enum is the single authoritative
 * definition replacing those scattered string checks.</p>
 *
 * <p>Suffix conventions preserved for backward compatibility with existing XML files:</p>
 * <ul>
 *   <li>ENTITY      — no suffix (root concept)</li>
 *   <li>ASPECT      — suffix "Dec" (decomposition)</li>
 *   <li>MULTI_ASPECT — suffix "MAsp"</li>
 *   <li>SPECIALIZATION — suffix "Spec" or contains "~"</li>
 * </ul>
 */
public enum SESNodeType {

    /** A real-world object or concept in the system. */
    ENTITY("", "entity"),

    /** A decomposition node — breaks an entity into sub-entities. */
    ASPECT("Dec", "aspect"),

    /** A multi-aspect node — entity has multiple instances of sub-entities. */
    MULTI_ASPECT("MAsp", "multiAspect"),

    /** A specialization node — represents one specialization of an entity. */
    SPECIALIZATION("Spec", "specialization");

    private final String labelSuffix;
    private final String xmlTag;

    SESNodeType(String labelSuffix, String xmlTag) {
        this.labelSuffix = labelSuffix;
        this.xmlTag = xmlTag;
    }

    /** The suffix appended to the node name in the JTree label. */
    public String getLabelSuffix() {
        return labelSuffix;
    }

    /** The XML element name used when serializing this node type. */
    public String getXmlTag() {
        return xmlTag;
    }

    /**
     * Infers the node type from a JTree label string.
     * Replaces scattered {@code if (name.endsWith("Dec"))} checks throughout the codebase.
     *
     * @param label the JTree node label (may be null)
     * @return the detected SESNodeType, defaulting to ENTITY for unrecognised labels
     */
    public static SESNodeType fromLabel(String label) {
        if (label == null || label.isBlank()) {
            return ENTITY;
        }
        if (label.endsWith("MAsp")) {
            return MULTI_ASPECT;
        }
        if (label.endsWith("Dec")) {
            return ASPECT;
        }
        if (label.endsWith("Spec") || label.contains("~")) {
            return SPECIALIZATION;
        }
        return ENTITY;
    }

    /**
     * Infers the node type from an XML tag name (inverse of {@link #getXmlTag()}).
     *
     * @param xmlTag the XML element name
     * @return the matching SESNodeType, defaulting to ENTITY
     */
    public static SESNodeType fromXmlTag(String xmlTag) {
        for (SESNodeType type : values()) {
            if (type.xmlTag.equalsIgnoreCase(xmlTag)) {
                return type;
            }
        }
        return ENTITY;
    }

    /**
     * Builds the full JTree label for this node type given a base name.
     *
     * @param baseName the human-readable entity name (without suffix)
     * @return label string as it appears in the JTree
     */
    public String toLabel(String baseName) {
        return baseName + labelSuffix;
    }
}
