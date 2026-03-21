package odme.domain.transform;

import odme.domain.model.SESNodeType;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates the XML tag transformation rules used when converting
 * internal graph XML output into the canonical SES XML format.
 *
 * <p>Extracted from {@code JtreeToGraphModify} and {@code JtreeToGraphGeneral}
 * so that the regex/string manipulation logic is testable without file I/O,
 * Swing, or mxGraph dependencies.</p>
 */
public class XmlTransformRules {

    // -------------------------------------------------------------------------
    // From JtreeToGraphModify.modifyXmlOutput / modifyXmlOutputSES
    // -------------------------------------------------------------------------

    /**
     * Applies the standard XML output transformation rules to a list of lines.
     * <ul>
     *   <li>Lines ending with {@code start>} are dropped.</li>
     *   <li>Self-closing tags ({@code <name/>}) are expanded to open/close pairs.</li>
     *   <li>All other lines pass through unchanged.</li>
     * </ul>
     *
     * @param inputLines the raw XML lines
     * @return the transformed XML lines
     */
    public List<String> applyModifyXmlOutputRules(List<String> inputLines) {
        List<String> output = new ArrayList<>();
        for (String line : inputLines) {
            if (line.endsWith("start>")) {
                continue;
            } else if (line.endsWith("/>")) {
                String result = line.replaceAll("[</>]", "");
                result = result.replaceAll("\\s+", "");
                output.add("<" + result + ">");
                output.add("</" + result + ">");
            } else {
                output.add(line);
            }
        }
        return output;
    }

    /**
     * Applies the "fix for same name node" transformation rules.
     * Self-closing tags are expanded unless they end with one of the
     * recognised suffixes: {@code RefNode}, {@code Var}, {@code Behaviour}, {@code Con}.
     *
     * @param inputLines the raw XML lines
     * @return the transformed XML lines
     */
    public List<String> applyModifyXmlOutputFixForSameNameNode(List<String> inputLines) {
        List<String> output = new ArrayList<>();
        for (String line : inputLines) {
            if (line.endsWith("/>")) {
                String result = line.replaceAll("[</>]", "");
                if (result.endsWith("RefNode")
                        || result.endsWith("Var")
                        || result.endsWith("Behaviour")
                        || result.endsWith("Con")) {
                    output.add(line);
                } else {
                    result = result.replaceAll("\\s+", "");
                    output.add("<" + result + ">");
                    output.add("</" + result + ">");
                }
            } else {
                output.add(line);
            }
        }
        return output;
    }

    // -------------------------------------------------------------------------
    // From JtreeToGraphGeneral.xmlOutputForXSD
    // -------------------------------------------------------------------------

    /**
     * Transforms internal graph XML into the canonical SES/XSD XML format.
     * <p>This applies the tag renaming conventions:
     * <ul>
     *   <li>Tags ending in {@code Dec} become {@code <aspect name="...">}</li>
     *   <li>Tags ending in {@code MAsp} become {@code <multiAspect name="...">}</li>
     *   <li>Tags ending in {@code Spec} become {@code <specialization name="...">}</li>
     *   <li>Tags ending in {@code Var} become {@code <var>} with attributes</li>
     *   <li>Tags ending in {@code Behaviour} become {@code <behaviour>} elements</li>
     *   <li>Tags ending in {@code RefNode} become self-closing ref elements</li>
     *   <li>Tags ending in {@code Seq} are skipped</li>
     *   <li>Other tags become {@code <entity name="...">}</li>
     *   <li>The first entity gets the full XML namespace declaration</li>
     * </ul>
     *
     * @param inputLines the raw XML lines from the graph export
     * @return the transformed XSD-compatible XML lines
     */
    public List<String> applyXsdTransformRules(List<String> inputLines) {
        List<String> output = new ArrayList<>();
        boolean isFirstEntity = true;

        for (String line : inputLines) {
            if (line.startsWith("<?")) {
                output.add("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>");
            } else if (line.startsWith("</")) {
                String result = line.replaceAll("[</>]", "");
                String mod;
                if (result.endsWith("Dec")) {
                    mod = "</aspect>";
                } else if (result.endsWith("MAsp")) {
                    mod = "</multiAspect>";
                } else if (result.endsWith("Spec")) {
                    mod = "</specialization>";
                } else {
                    if (result.endsWith("Seq")) {
                        continue;
                    }
                    mod = "</entity>";
                }
                output.add(mod);
            } else if (line.startsWith("<")) {
                if (line.endsWith("/>")) {
                    processSelfClosingTag(line, output);
                } else {
                    processOpenTag(line, output, isFirstEntity);
                    // After the first entity open tag, flip the flag
                    String result = line.replaceAll("[</>]", "");
                    if (!result.endsWith("Dec") && !result.endsWith("MAsp")
                            && !result.endsWith("Spec") && !result.endsWith("Seq")) {
                        isFirstEntity = false;
                    }
                }
            }
        }
        return output;
    }

    /**
     * Processes a self-closing XML tag (ending with {@code />}) and adds
     * the appropriate transformed line(s) to the output.
     */
    private void processSelfClosingTag(String line, List<String> output) {
        String result = line.replaceAll("[</>]", "");

        if (result.endsWith("Var")) {
            String noVarResult = result.replace("Var", "");
            String[] properties = noVarResult.split(",");
            if (properties.length >= 3
                    && (properties[1].equals("string") || properties[1].equals("boolean"))) {
                output.add("<var name=\"" + properties[0] + "\" type=\"" + properties[1]
                        + "\" default=\"" + properties[2] + "\"> </var>");
            } else if (properties.length >= 5) {
                output.add("<var name=\"" + properties[0] + "\" type=\"" + properties[1]
                        + "\" default=\"" + properties[2]
                        + "\" lower=\"" + properties[3] + "\" upper=\"" + properties[4]
                        + "\"> </var>");
            }
        } else if (result.endsWith("Behaviour")) {
            String noBehaviourResult = result.replace("Behaviour", "");
            String[] properties = noBehaviourResult.split(",");
            output.add("<behaviour name=\"" + properties[0] + "\"> </behaviour>");
        } else if (result.endsWith("RefNode")) {
            String noRefResult = result.replace("RefNode", "");
            if (noRefResult.endsWith("Dec")) {
                output.add("<aspect name=\"" + noRefResult + "\" ref=\"" + noRefResult + "\"/>");
            } else if (noRefResult.endsWith("MAsp")) {
                output.add("<multiAspect name=\"" + noRefResult + "\" ref=\"" + noRefResult + "\"/>");
            } else if (noRefResult.endsWith("Spec")) {
                output.add("<specialization name=\"" + noRefResult + "\" ref=\"" + noRefResult + "\"/>");
            } else {
                output.add("<entity name=\"" + noRefResult + "\" ref=\"" + noRefResult + "\"/>");
            }
        }
        // Other self-closing tags are silently ignored (same as original)
    }

    /**
     * Processes an opening XML tag and adds the appropriate transformed line
     * to the output.
     */
    private void processOpenTag(String line, List<String> output, boolean isFirstEntity) {
        String result = line.replaceAll("[</>]", "");
        String mod;

        if (result.endsWith("Dec")) {
            mod = "<aspect name=\"" + result + "\">";
        } else if (result.endsWith("MAsp")) {
            mod = "<multiAspect name=\"" + result + "\">";
        } else if (result.endsWith("Spec")) {
            mod = "<specialization name=\"" + result + "\">";
        } else {
            if (result.endsWith("Seq")) {
                return; // skip sequence nodes
            }
            if (isFirstEntity) {
                mod = "<entity xmlns:vc=\"http://www.w3.org/2007/XMLSchema-versioning\""
                        + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
                        + " xsi:noNamespaceSchemaLocation=\"ses.xsd\" name=\"" + result + "\">";
            } else {
                mod = "<entity name=\"" + result + "\">";
            }
        }
        output.add(mod);
    }

    /**
     * Determines the SES node type suffix from an internal node name.
     * Delegates to {@link SESNodeType#fromLabel(String)}.
     *
     * @param nodeName the internal node name (e.g. "vehicleDec", "engineSpec")
     * @return the corresponding {@link SESNodeType}
     */
    public SESNodeType classifyNode(String nodeName) {
        return SESNodeType.fromLabel(nodeName);
    }
}
