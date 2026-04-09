package behaviourtreetograph;

import java.util.ArrayList;
import java.util.List;

public class BehaviorAttributeOverview {
    private final String behaviorName;
    private final List<BehaviorAttribute> attributes = new ArrayList<>();

    public BehaviorAttributeOverview(String behaviorName) {
        this.behaviorName = behaviorName;
    }

    public String getBehaviorName() {
        return behaviorName;
    }

    public void addAttribute(BehaviorAttribute attribute) {
        attributes.add(attribute);
    }

    public List<BehaviorAttribute> getAttributes() {
        return attributes;
    }
}
