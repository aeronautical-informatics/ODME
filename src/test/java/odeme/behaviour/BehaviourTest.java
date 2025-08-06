package odeme.behaviour;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.table.DefaultTableModel;

import static org.junit.jupiter.api.Assertions.*;

public class BehaviourTest {

    private Behaviour behaviour;

    @BeforeEach
    public void setUp() {
        behaviour = new Behaviour();
    }

    @Test
    public void testConstructor_InitializesTableWithRows() {
        assertNotNull(Behaviour.table, "Table should not be null");
        assertNotNull(Behaviour.model, "Model should not be null");
        assertEquals(100, Behaviour.model.getRowCount(), "Initial row count should be 100");
    }

    @Test
    public void testSetNullToAllRows_ClearsAndFillsRows() {
        // Pre-fill model with dummy data
        Behaviour.model.setRowCount(10);
        assertEquals(10, Behaviour.model.getRowCount());

        // Call method
        Behaviour.setNullToAllRows();

        assertEquals(100, Behaviour.model.getRowCount(), "Row count should reset to 100 after clearing");
        assertEquals("", Behaviour.model.getValueAt(0, 0), "First row should contain an empty string");

    }

    @Test
    public void testShowBehavioursInTable_AddsNodeAndBehaviours() {
        String nodeName = "Node1";
        String[] behaviours = {"Walk", "Run", "Jump"};

        behaviour.showBehavioursInTable(nodeName, behaviours);

        DefaultTableModel model = Behaviour.model;

        // 3 data rows + 100 null rows
        assertEquals(103, model.getRowCount());

        assertEquals("Node1", model.getValueAt(0, 0));
        assertEquals("Walk", model.getValueAt(0, 1));

        assertEquals("Node1", model.getValueAt(2, 0));
        assertEquals("Jump", model.getValueAt(2, 1));
    }

   @Test
    public void testShowBehavioursInTable_HandlesNullValues() {
        String[] behaviours = {"Run", null, "Stop"};

        behaviour.showBehavioursInTable("NodeX", behaviours);

        assertEquals("Run", Behaviour.model.getValueAt(0, 1));
        assertEquals("", Behaviour.model.getValueAt(1, 0));  // null value case
        assertEquals("Stop", Behaviour.model.getValueAt(2, 1));
    }
}
