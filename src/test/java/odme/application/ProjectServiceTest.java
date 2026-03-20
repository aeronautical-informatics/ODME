package odme.application;

import odme.domain.model.SESNodeType;
import odme.domain.model.SESNode;
import odme.domain.model.SESTree;
import odme.domain.operations.AddNodeCommand;
import odme.domain.operations.SESCommandException;
import odme.domain.validation.ValidationResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

class ProjectServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void createProject_returnsSessionWithModel() throws IOException {
        ProjectService service = new ProjectService();
        ProjectSession session = service.createProject("TestProject", tempDir);

        assertThat(session).isNotNull();
        assertThat(session.getProjectName()).isEqualTo("TestProject");
        assertThat(service.hasActiveSession()).isTrue();
    }

    @Test
    void validateModel_onEmptyTree_returnsWarning() throws IOException {
        ProjectService service = new ProjectService();
        service.createProject("TestProject", tempDir);

        ValidationResult result = service.validateModel();
        // Empty tree produces a warning but is "valid" (no errors)
        assertThat(result.isValid()).isTrue();
        assertThat(result.getWarnings()).isNotEmpty();
    }

    @Test
    void saveAndLoad_roundTrips() throws IOException {
        ProjectService service = new ProjectService();
        ProjectSession session = service.createProject("MyProject", tempDir);

        // Add a node
        SESTree tree = session.getSESModel().orElseThrow();
        SESNode root = new SESNode("root", "Aircraft", SESNodeType.ENTITY);
        tree.setRoot(root);

        service.saveProject();

        // Load into new service
        ProjectService service2 = new ProjectService();
        ProjectSession loaded = service2.loadProject("MyProject", tempDir);

        assertThat(loaded.getSESModel()).isPresent();
        assertThat(loaded.getSESModel().get().size()).isEqualTo(1);
    }

    @Test
    void commandHistory_executeAndUndo() throws IOException, SESCommandException {
        ProjectService service = new ProjectService();
        ProjectSession session = service.createProject("T", tempDir);

        SESTree tree = session.getSESModel().orElseThrow();
        SESNode root = new SESNode("root", "Aircraft", SESNodeType.ENTITY);
        tree.setRoot(root);

        var history = service.getCommandHistory();
        history.execute(new AddNodeCommand("root", "engine", "Engine", SESNodeType.ENTITY, "user"));
        assertThat(tree.size()).isEqualTo(2);

        history.undo();
        assertThat(tree.size()).isEqualTo(1);
    }

    @Test
    void noActiveSession_validateThrowsException() {
        ProjectService service = new ProjectService();
        assertThatThrownBy(service::validateModel)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("No active project session");
    }
}
