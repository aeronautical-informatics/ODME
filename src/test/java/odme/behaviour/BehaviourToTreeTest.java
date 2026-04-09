package odme.behaviour;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.junit.jupiter.api.Test;

import javax.swing.tree.TreePath;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class BehaviourToTreeTest {

    @Test
    void convertMapToArrayCreatesOneTreeRowPerBehaviourValue() {
        Multimap<TreePath, String> behaviourMap = ArrayListMultimap.create();
        TreePath path = new TreePath(new Object[]{"Events", "Robot"});

        behaviourMap.put(path, "Walk");
        behaviourMap.put(path, "Run");

        String[][] rows = BehaviourToTree.convertMapToArray(behaviourMap.asMap());

        Set<String> flattenedRows = Arrays.stream(rows)
                .map(row -> String.join(">", row))
                .collect(Collectors.toSet());

        assertThat(flattenedRows).containsExactlyInAnyOrder(
                "Events>Robot>Walk",
                "Events>Robot>Run"
        );
    }
}
