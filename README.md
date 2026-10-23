# Operational Design Domain Modeling Environment (ODME)

## Overview

The **Operational Design Domain Modeling Environment (ODME)** is a desktop application for authoring, validating, and managing **Operational Design Domain (ODD)** models for AI-based systems in safety-critical domains, with a strong focus on aviation use cases.

ODME uses the **System Entity Structure (SES)** formalism to model the operational conditions under which an AI or ML system is intended to function. From a single SES model, users can derive **Pruned Entity Structures (PES)** for concrete scenarios, attach variables and constraints, define behavior information, generate ODD views, and create scenario samples for downstream analysis.

The current codebase contains:

- a Swing desktop editor for domain and scenario modelling
- a behavior modelling editor for scenario-linked behaviors
- an ODD manager and sampling workflow
- structural coverage estimation and parameter coverage heat maps
- execution and Python plugin support
- native packaging scripts for Windows, Linux, and macOS

## Table of Contents

- [Overview](#overview)
- [What ODME Currently Supports](#what-odme-currently-supports)
- [Application Modes and Menus](#application-modes-and-menus)
- [Scenario Generation and Coverage](#scenario-generation-and-coverage)
- [Getting Started](#getting-started)
- [Run ODME on Windows](#run-odme-on-windows)
- [Run ODME from Git Bash](#run-odme-from-git-bash)
- [Run Tests](#run-tests)
- [Example Project: Runway Sign Classifier](#example-project-runway-sign-classifier)
- [Architecture](#architecture)
- [Contributing](#contributing)
- [License](#license)

## What ODME Currently Supports

| Area | Current functionality |
|------|------------------------|
| **Domain Modelling** | SES tree construction with synchronized graph view, node editing, variables, constraints, behaviors, distributions, XML/XSD generation; background project loading |
| **Scenario Modelling** | Save and open scenarios, import CSV rows, generate specialization combinations with constrained LHS or Cartesian variable sampling |
| **Coverage Estimation** | Pruning-option and numeric-bin coverage reports; all-parameter heat-map overview; advanced single-parameter regions, metrics and exports |
| **Behavior Modelling** | Sync scenarios containing behavior metadata; edit behavior graphs, conditions and attributes; open saved graph or behavior-tree XML; export and merge behavior XML |
| **Operation Design Domain** | Generate OD view, manage ODD data and export edited table values to YAML |
| **Sampling** | YAML-driven constrained sampling, Latin Hypercube Sampling, CSV export |
| **Execution** | Execution window for XML, YAML, and Python-oriented workflows |
| **Python Plugin Support** | Run a Python script against the current project session; requires an available Python interpreter |
| **Import From Cameo** | External model import flow |
| **Help System** | Bundled user manual and coverage-map `?` help buttons |

## Application Modes and Menus

The top-level menus in the current application are the clearest way to understand what ODME does. The README below mirrors the actual menu structure in the code.

### File

- `Save`: saves the current project and generated artifacts
- `Save As`: saves the project under a new project name
- `Save as PNG`: exports the current graph canvas as an image
- `Exit`: closes the application

### Domain Modelling

This is the main authoring mode for the SES model.

- `New Project`: create a new ODME project
- `Open`: open an existing project folder
- `Import Template`: import a template project
- `Save as Template`: export the current model as a template
- `Import From Cameo`: start the Cameo import workflow

Typical work in this mode includes:

- building the SES structure in the graph and tree views
- adding entity variables
- adding and deleting behaviors on entity nodes
- editing constraints and metadata
- saving project XML and graph artifacts

Project opening loads and decodes data in the background, then applies it to the
editor. The tree initially expands its root; expand deeper branches as needed.

### Scenario Modelling

This menu is used for scenario creation from the currently open domain model.

- `Save Scenario`: create or save a scenario entry
- `Generate Scenario -> From CSV`: create scenario files from CSV rows

### Behavior Modelling

This menu opens the separate behavior editor flow.

- `Sync Behaviour`: open the scenario-linked behavior workflow

From there, the current code supports:

- selecting behavior leaves from the left tree
- placing them into the behavior graph
- adding selector, sequence, parallel, and decorator nodes
- editing decorator conditions
- attaching behavior attributes to behavior entities
- saving and exporting behavior graph data
- `Open Graph`: load a saved mXGraph XML graph or an exported `BehaviourTree` XML file
- `Merge`: combine exported behavior XML with scenario XML

The Sync Behaviour list includes scenarios with non-blank behavior metadata;
an empty behavior sidecar alone does not make a scenario eligible.

### Operation Design Domain

This menu contains the ODD-specific workflow.

- `Generate OD`: generate the current OD view from the model
- `ODD Manager`: inspect and manage ODD data, ranges, and exports

### Scenario Manager

This menu groups downstream scenario tools.

- `Scenarios List`: open saved scenarios
- `Execution`: open the execution window
- `Feedback Loop`: reserved scenario workflow entry point
- `Generate Samples`: open the constrained sample generation panel

Some scenario-related actions stay disabled until a project is open.
The Scenario List toolbar provides **Automatic Scenario Generation**, **Cartesian
Scenario Generation**, **Estimate Coverage**, deletion actions and refresh.

### Tools

- `Run Python Plugin...`: run a Python script with access to the current project session

### Help

- `User Manual`: open the bundled ODME user manual
- `About`: open the about dialog

## Scenario Generation and Coverage

### Generate scenarios

Open **Scenario Manager > Scenarios List** after opening a domain project.

- **Automatic Scenario Generation** enumerates specialization choices and samples
  variable values using constrained Latin Hypercube Sampling. Configure the
  scenario prefix and variable samples per structural combination.
- **Cartesian Scenario Generation** combines specialization choices with Cartesian
  variable levels. Configure the prefix and levels per sampled variable.
- **Scenario Modelling > Generate Scenario > From CSV** imports sampled CSV rows
  into scenario files.
- **Scenario Manager > Generate Samples** opens the separate YAML-driven,
  constraint-aware sampling panel and exports CSV samples.

Automatic and Cartesian generation currently support specialization pruning and
reject models containing multi-aspect nodes. Cartesian generation can produce
large sets as the number of variables and levels grows.

### Estimate coverage

In **Scenarios List**, click **Estimate Coverage**. At least one saved/generated
scenario is required. The configuration window contains:

- **Coverage Estimate**: configure uniform numeric bins using Bin Size or override
  them with Custom Bins, such as `0-2;2-8;8-10`. The bottom **Calculate Coverage**
  button opens the pruning, parameter-bin and combined coverage report.
- **Parameter Coverage Map**: inspect local sample support across numeric ranges.
  Use the calculation buttons inside this tab to calculate heat maps.

### View all parameters together

Choose **Parameter Coverage Map > All parameters**, keep the defaults and click
**Calculate all parameters**. Each numeric parameter appears as a heat strip with
its range, geometric coverage score, sample count and excluded sample count.

Only two settings are needed:

| Setting | Meaning | Default |
|---------|---------|---------|
| **Bandwidth (% of each parameter range)** | Distance over which a sample contributes heat on either side of its value. For a range of 0–10, 5% means 0.5 units. | 5% |
| **Adequacy target** | Combined local sample support required for full green. Higher values require more nearby support. | 1 |

The overview uses a triangular kernel, five equal regions, geometric weighting
and a cold threshold of 0.25. Triangular support is strongest at a sample value
and fades to zero one bandwidth away. Red indicates low support, yellow
intermediate support and green adequate support. All rows use the same 0–1 scale.
Choose bandwidth and adequacy using the operational similarity and evidence
requirements of your application; the defaults are starting points.

### Inspect a parameter in detail

Open **Advanced: single parameter** for custom edges, triangular/Gaussian/binary
kernels, bandwidth in parameter units, adequacy, cold threshold and geometric,
equal-region or priority scoring. Priority weights must sum to 1. Click
**Calculate map** beside the **?** help button after changing settings.

Custom region edges must be strictly increasing and span the declared range.
For `0, 1, 3, 8, 9, 10`, the 3–8 interval is displayed five times wider than
0–1. Changing region widths does not automatically change bandwidth.

Select a region for mean/minimum coverage, cold fraction and contributing
scenario IDs. Toggle sample markers or the classified display, export region
metrics as **CSV**, export a figure as **SVG**, or **Copy cold-region constraints**
as text for a later sampling workflow. Copying does not execute generation.

Coverage maps read the current project's saved scenario set through the existing
coverage adapter. They measure parameter-wise marginal support; they do not
establish joint coverage of parameter combinations. Categorical/Boolean maps,
invalid-range exclusion, conditional activation, saved map configurations and
explicit dataset partition/version selectors are not implemented. See
[COVERAGE_MAP.md](COVERAGE_MAP.md) for details and validation notes.

## Getting Started

### Prerequisites

- **Java 17 or newer**
- **Maven 3.8 or newer**

If you want to build a native desktop app image:

- use a full JDK that includes `jpackage`

### Recommended first run

If you simply want to try the editor:

1. clone the repository
2. build the project
3. launch the JAR or the packaged app
4. open the bundled example project `examples/RunwaySignClassifier`
5. review `Help -> User Manual`

Clone command:

```bash
git clone https://github.com/aeronautical-informatics/ODME.git
cd ODME
```

## Run ODME on Windows

There are two common ways to run ODME on Windows.

### Option 1: Run from source as a JAR

From PowerShell:

```powershell
cd C:\path\to\ODME
$env:MAVEN_OPTS='-Dmaven.repo.local=C:\path\to\ODME\build\.m2\repository -Dmaven.user.home=C:\path\to\ODME\build\.m2'
mvn clean package -DskipTests
java -jar .\target\SESEditor-2.0.0-SNAPSHOT.jar
```

What this does:

- downloads dependencies
- builds the shaded application JAR
- launches the desktop application directly

### Option 2: Build the Windows app image

From PowerShell:

```powershell
cd C:\path\to\ODME
powershell -ExecutionPolicy Bypass -File .\launcher\build-windows-exe.ps1
```

This creates:

- `dist\ODME\ODME.exe`

Then run:

```powershell
.\dist\ODME\ODME.exe
```

Notes:

- the `dist` folder does **not** exist until you build the native app image
- keep the full `dist\ODME` folder together when launching the packaged app
- the bundle includes a Java runtime; a separate Java installation is needed for
  source builds, but not to run the packaged executable
- close the packaged ODME before rebuilding it: Windows locks application files
  while they are in use

## Run ODME from Git Bash

If you use Git Bash on Windows, the easiest source-based launch is:

```bash
cd /c/path/to/ODME
export MAVEN_OPTS="-Dmaven.repo.local=$PWD/build/.m2/repository -Dmaven.user.home=$PWD/build/.m2"
mvn clean package -DskipTests
java -jar target/SESEditor-2.0.0-SNAPSHOT.jar
```

If Git Bash does not find Maven on your PATH, try:

```bash
mvn.cmd clean package -DskipTests
```

If you want the native Windows app from Git Bash, call the PowerShell packager:

```bash
cd /c/path/to/ODME
powershell.exe -ExecutionPolicy Bypass -File ./launcher/build-windows-exe.ps1
./dist/ODME/ODME.exe
```

## Run Tests

Standard test run:

```bash
mvn test
```

Useful additional checks:

```bash
mvn jacoco:report
mvn spotbugs:check
mvn checkstyle:check
```

Notes:

- `mvn test` runs unit tests and produces JaCoCo data
- `mvn jacoco:report` writes the HTML report to `target/site/jacoco/`
- SpotBugs and Checkstyle are configured in the current Maven build
- Python plugin tests require Python. Set `ODME_PYTHON` to the full interpreter
  path if `python3` or `python` is unavailable on PATH.
- Validation on 2026-10-23 ran 588 tests with Python configured: 587 passed and
  `ConstraintEvaluatorTest.evaluate_plainBooleanExpression_returnsTrue` failed.
  That failure was also reproduced on upstream revision `51feee31`; see
  [COVERAGE_MAP.md](COVERAGE_MAP.md). This is a recorded validation result, not a
  claim that every checkout or environment has the same test outcome.

## Example Project: Runway Sign Classifier

ODME ships with a complete example based on:

> K. Dmitriev et al., "Runway Sign Classifier: A DAL C Certifiable Machine Learning System,"  
> *IEEE/AIAA 42nd DASC*, Barcelona, 2023.

The example models operating conditions for a ground-based runway-sign classifier
using a vehicle-mounted camera, including airport, weather, lighting, sign and
sensor conditions. Refer to the example's own README for its model structure,
parameter ranges and structural combination counts.

To open it after launching ODME:

1. choose `Domain Modelling -> Open`
2. select the folder `examples/RunwaySignClassifier`
3. inspect the graph, variables, scenarios, and ODD workflow

For the example-specific details, see [examples/RunwaySignClassifier/README.md](examples/RunwaySignClassifier/README.md).

## Architecture

ODME follows a mixed architecture:

- a modernized domain and application layer for testable core logic
- a legacy Swing and mxGraph-based desktop UI for interactive editing
- dedicated sampling, export, execution, and plugin flows layered around the editor

At a high level:

```text
odme/
|-- domain/        core SES, scenario, validation, transformation, coverage
|-- application/   project services and plugin runner support
|-- sampling/      constrained sampling and CSV generation pipeline
|-- odmeeditor/    Swing desktop application and windows
|-- jtreetograph/  graph and tree synchronization logic
|-- behaviour/     behavior modelling editor support
```

Additional project documentation:

- [ARCHITECTURE.md](ARCHITECTURE.md)
- [DEVELOPER_GUIDE.md](DEVELOPER_GUIDE.md)
- [launcher/README-packaging.md](launcher/README-packaging.md)
- [COVERAGE_MAP.md](COVERAGE_MAP.md)

## Contributing

Contributions are welcome. Please see [CONTRIBUTING.md](CONTRIBUTING.md) for development and contribution guidelines.

## License

ODME is released under the **MIT License**.
