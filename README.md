# Operational Design Domain Modeling Environment (ODME)

## Overview

The **Operational Design Domain Modeling Environment (ODME)** is a desktop application for authoring, validating, and managing **Operational Design Domain (ODD)** models for AI-based systems in safety-critical domains, with a strong focus on aviation use cases.

ODME uses the **System Entity Structure (SES)** formalism to model the operational conditions under which an AI or ML system is intended to function. From a single SES model, users can derive **Pruned Entity Structures (PES)** for concrete scenarios, attach variables and constraints, define behavior information, generate ODD views, and create scenario samples for downstream analysis.

The current codebase contains:

- a Swing desktop editor for domain and scenario modelling
- a behavior modelling editor for scenario-linked behaviors
- an ODD manager and sampling workflow
- execution and Python plugin support
- native packaging scripts for Windows, Linux, and macOS

## Table of Contents

- [Overview](#overview)
- [What ODME Currently Supports](#what-odme-currently-supports)
- [Application Modes and Menus](#application-modes-and-menus)
- [Getting Started](#getting-started)
- [Run ODME on Windows](#run-odme-on-windows)
- [Run ODME from Git Bash](#run-odme-from-git-bash)
- [Run Tests](#run-tests)
- [Example Project: Runway Sign Classifier](#example-project-runway-sign-classifier)
- [Architecture](#architecture)
- [Contributing](#contributing)
- [License](#license)

## What ODME Currently Supports

| Area | Current functionality | Status |
|------|------------------------|--------|
| **Domain Modelling** | SES tree construction with synchronized graph view, node editing, variables, constraints, behaviors, distributions, XML/XSD generation | Stable |
| **Scenario Modelling** | Save scenarios, derive scenario-specific structures, generate scenarios from CSV input | Stable |
| **Behavior Modelling** | Sync behaviors from scenarios, build behavior trees and behavior graphs, decorator conditions, behavior attributes, XML export | Stable for core workflows |
| **Operation Design Domain** | Generate OD view and manage ODD data in the ODD Manager | Stable |
| **Sampling** | YAML-driven constrained sampling, Latin Hypercube Sampling, CSV export | Stable |
| **Execution** | Open the execution window for XML, YAML, and Python-oriented workflows | Available |
| **Python Plugin Support** | Run a Python plugin against the current project context | Available |
| **Import From Cameo** | Import external model data through the Cameo import flow | Available |
| **Help System** | Bundled user manual available from the Help menu | Stable |

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

### Tools

- `Run Python Plugin...`: run a Python script with access to the current project session

### Help

- `User Manual`: open the bundled ODME user manual
- `About`: open the about dialog

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

Important:

- the `dist` folder does **not** exist until you build the native app image
- keep the full `dist\ODME` folder together when launching the packaged app

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

## Example Project: Runway Sign Classifier

ODME ships with a complete example based on:

> K. Dmitriev et al., "Runway Sign Classifier: A DAL C Certifiable Machine Learning System,"  
> *IEEE/AIAA 42nd DASC*, Barcelona, 2023.

The example models an airborne DNN system for airport sign detection and classification, with:

- **23 leaf entities** across environment, sensor, and system architecture branches
- **43 ODD parameters**
- **21 continuous parameters** suitable for LHS-based sampling
- **2,592 possible PES combinations** derived from specialization choices

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

## Contributing

Contributions are welcome. Please see [CONTRIBUTING.md](CONTRIBUTING.md) for development and contribution guidelines.

## License

ODME is released under the **MIT License**.
