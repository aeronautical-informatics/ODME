# ODME Developer Guide

This guide is for developers working directly on the **ODME repository** (not forks).

It explains how to set up the project, follow safe workflows, and ensure the CI/CD pipeline continues to run smoothly.

---

## Getting Started

### 1. Clone the Repository
git clone https://github.com/aeronautical-informatics/ODME.git

cd ODME

### 2. Build the Project
We use Maven:

mvn clean install

This creates a runnable JAR file inside the target/ folder.

### 3. Run the Application
```bash
java -jar target/SESEditor-2.0.0-SNAPSHOT.jar
```

If the JAR is missing, build it first:
```bash
mvn clean package -DskipTests
```

## Working with the Repository
Always Sync Before Starting Work

git checkout main

git pull origin main

## Create a Feature Branch
git checkout -b your_branch

Never commit directly to main. Work in a separate branch(your own issue specific branch).

## Commit Changes
git add . OR specific files that you want to add

git commit -m "Describe your changes clearly"

## Merge Latest Main Before Pushing
git fetch origin

git merge origin/main


# Very Important: Fix any conflicts locally before pushing.
after analyzing and fixing of any new update related issues

Push Your Branch

git push origin Your_branch


Then create a Pull Request (PR) on GitHub.

CI/CD will run automatically on your PR

## Protecting the CI/CD Pipeline

Some files are critical for the project’s automated builds and workflows:

.github/workflows/* (CI/CD pipelines)

pom.xml (Maven build setup)

.gitignore

## Rules:

Do not overwrite or delete these files.

If your change affects them, explain the reason clearly in your PR.

Test your build locally with mvn clean install before pushing.

## Best Practices

Use clear, meaningful commit messages.

Test everything locally before pushing.

Keep pull requests small and focused.

Discuss large changes with the team before implementation.

Avoid committing build artifacts (e.g., target/ directory).

Don’t touch pipeline files unless discussed.

## More Info

Project overview → see README.md.

Developer workflow → see this guide (DEVELOPER_GUIDE.md).

For questions, coordinate with the maintainer or open an issue.

Welcome to the ODME developer team!
Let’s keep the project stable, collaborative, and easy to maintain.

---

## Domain-Driven Development (v2.0+)

### Adding a new operation

All model mutations go through the Command pattern:

1. Create a class in `odme.domain.operations` implementing `SESCommand`
2. Implement `execute(ProjectSession)`, `undo(ProjectSession)`, `describe()`
3. Add `AuditLogger` call in `execute()`
4. Write a unit test (no Swing needed)
5. Wire the Swing UI to call `projectService.getCommandHistory().execute(yourCommand)`

Example:
```java
public class MyCommand implements SESCommand {
    @Override
    public void execute(ProjectSession session) throws SESCommandException {
        // mutate session.getSESModel()
        AuditLogger.sesNodeAdded(...);
    }

    @Override
    public void undo(ProjectSession session) throws SESCommandException {
        // reverse the mutation
    }

    @Override
    public String describe() { return "My operation on ..."; }
}
```

### Running quality checks

```bash
mvn test                  # unit tests + JaCoCo coverage
mvn jacoco:report         # open target/site/jacoco/index.html
mvn spotbugs:check        # static analysis (new domain code)
mvn checkstyle:check      # code style (new domain code)
```

### Domain package rules

- `odme.domain.*` must never import from `javax.swing`, `java.awt`, or `com.mxgraph`
- All new business logic goes in `odme.domain.*` or `odme.application.*`
- Legacy packages (`odmeeditor`, `jtreetograph`, etc.) are not modified — they are gradually wired to delegate to the domain layer

### Logging

Use SLF4J:
```java
private static final Logger log = LoggerFactory.getLogger(MyClass.class);
log.info("Something happened: {}", value);
log.debug("Detail: {}", detail);
```

Never use `System.out.println`. New code failing this check will be caught by Checkstyle.

### Generating the traceability report

```java
ProjectService service = ...; // inject or obtain
TraceabilityMatrix matrix = new TraceabilityMatrix(ses, scenarios, pesTrees);
new HtmlTraceabilityExporter().export(matrix, Path.of("evidence/traceability.html"));
new CsvTraceabilityExporter().export(matrix, Path.of("evidence/traceability.csv"));
```

### Working with examples

Example projects live in `examples/`. To use one:

```bash
cp -r examples/RunwaySignClassifier .
# Launch ODME → File → Open → select RunwaySignClassifier
```

To create a new example:
1. Build the SES tree in ODME with entities, specializations, and aspects
2. Attach variables with `type`, `min`, `max` values to leaf entities
3. Save the project, then copy the project directory to `examples/`
4. Add a `README.md` documenting the SES structure and variable catalog
5. Test: Generate OD → verify the ODD table → Generate Test Cases (LHS)

### Testing the ODD Manager and LHS

The LHS feature requires variables with numeric ranges (min < max) in the ODD table.
Fixed-value parameters (min == max) and string-type variables are automatically excluded.
To verify:
1. Open a project with parameterized variables
2. **Tools → Generate OD** — check the ODD table shows Lower/Upper Bound columns
3. Click **Generate Test Cases (LHS)** — should list found parameters and prompt for N
4. Enter a sample count and save the CSV

---

## Working with the Sampling Pipeline (`odme.sampling`)

The sampling pipeline is fully headless (no Swing) and can be used programmatically
or via **Scenario Manager → Generate Samples** in the UI.

### YAML scenario file format

```yaml
Scenario:
  EgoAC:                      # entity name
    Speed:                    # variable name → parameter "EgoAC_Speed"
      type: double
      min: 0
      max: 300
    WeatherMode:              # categorical parameter
      options: [Clear, Rain, Fog]
    Noise:                    # distribution-typed parameter
      type: distribution
      distributionName: normalDistribution
      distributionDetails: mean=0.5___stdDev=0.1
    HasConstraint:
      IntraConstraint: if(@EgoACSpeed > 200) then (@EgoACAltitude > 500) else true
      InterConstraint: if(@EgoAC@speed > 50) then (@Traffic@count < 10) else true
```

### Generating samples programmatically

```java
SamplingManager manager = new SamplingManager();

// Full pipeline (includes distribution-typed params)
manager.generateSamples("path/to/scenario.yaml", 100, "output/samples.csv");

// Domain-model mode (excludes distribution-typed params)
manager.generateSamplesforDomainModel("path/to/scenario.yaml", 100, "output/samples.csv");
```

### Constraint syntax

ODME constraint expressions follow the pattern:

```
if(@paramName > value) then (@otherParam < limit) else true
```

- Variables are prefixed with `@` in the source YAML
- `@` and other special chars are stripped before mXparser evaluation
- `true` → `1`, `false` → `0`
- Malformed expressions (no `if/then/else`) always return `false` (safe default)

### Generating XML scenarios from a CSV

```java
// Direct call — no EditorContext required
String result = ScenarioGeneration.importScenarioDatasFromCSVFile(
        "path/to/samples.csv",
        "path/to/output/ScenarioSet1");
```

CSV header convention: `EntityName_VariableName` (e.g. `EgoAC_Speed`).
Each data row produces one `Scenario_N.xml` file.

### Running quality checks

```bash
mvn test                  # unit tests + JaCoCo coverage
mvn jacoco:report         # open target/site/jacoco/index.html
mvn spotbugs:check        # static analysis (new domain code)
mvn checkstyle:check      # code style (new domain code)
```
