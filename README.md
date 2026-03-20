# Operational Design Domain Modeling Environment (ODME)

### Table of Contents

* [Overview](#overview)
* [Key Features](#key-features)
* [Getting Started](#getting-started)
* [Example: Runway Sign Classifier](#example-runway-sign-classifier)
* [Workflow](#workflow)
* [Architecture](#architecture)
* [Contributing](#contributing)
* [License](#license)

## Overview

The **Operational Design Domain Modeling Environment (ODME)** is a desktop application for authoring, validating, and managing Operational Design Domain (ODD) models for AI-based systems in safety-critical domains, with a focus on aviation.

ODME implements the **System Entity Structure (SES)** formalism to model the operational conditions under which an AI/ML system is designed to function. From a single SES model, users can derive **Pruned Entity Structures (PES)** representing concrete test scenarios, attach variables with ranges to define the continuous parameter space, and generate test cases using **Latin Hypercube Sampling (LHS)** for efficient coverage of the ODD.

The tool supports the AI Learning Assurance workflow defined in the **EASA AI Roadmap 2.0** and is designed to produce evidence artifacts for certification of ML-based airborne systems.

## Key Features

| Mode | Description | Status |
|------|-------------|--------|
| **Domain Modeling Editor** | SES tree construction with synchronized graph view, variable/constraint attachment, XSD validation | Stable |
| **Scenario Modelling Editor** | Interactive SES→PES pruning to derive concrete test scenarios | Stable |
| **ODD Manager** | Generate, save, edit, and export Operational Design Domains in XML/YAML/CSV | Stable |
| **LHS Test Case Generation** | Latin Hypercube Sampling over ODD parameter ranges for coverage-efficient test vectors | Stable |
| **Behavior Modelling Editor** | Behavior tree editor for entity functions, XML export | Work in Progress |
| **Scenario Manager** | Scenario catalogue with risk factors and lifecycle states | Work in Progress |
| **CAMEO Import Plugin** | Import SysML operational scenarios from CAMEO into ODME | Work in Progress |
| **Scripting Add-On** | Translate scenarios to executable Python scripts for simulation | Work in Progress |

## Getting Started

### Prerequisites

- **Java 17+** (tested with OpenJDK 17 and 21)
- **Maven 3.8+**

### Build and Run

```bash
git clone https://github.com/umutdurak/ODME.git
cd ODME
mvn clean package -DskipTests
java -jar target/SESEditor-2.0.0-SNAPSHOT.jar
```

### Run Tests

```bash
mvn test                  # Unit tests + JaCoCo coverage
mvn jacoco:report         # HTML coverage report → target/site/jacoco/
mvn spotbugs:check        # Static analysis
mvn checkstyle:check      # Code style checks
```

## Example: Runway Sign Classifier

ODME ships with a complete example based on:

> K. Dmitriev et al., "Runway Sign Classifier: A DAL C Certifiable Machine Learning System,"
> *IEEE/AIAA 42nd DASC*, Barcelona, 2023.

The example models an airborne DNN system for airport sign detection and classification, with:

- **23 leaf entities** across Environment (airport, weather, time-of-day), Sensor (distance, elevation, offset), and SystemArchitecture (Faster R-CNN, YOLOv2, SafetyMonitor)
- **43 ODD parameters** (21 with continuous ranges for LHS sampling)
- **2,592 possible PES combinations** from 7 specialization nodes
- Full variable parameterization sourced from the paper's Table II and established aviation standards (ICAO, WMO, CIE)

To try it:
```bash
cp -r examples/RunwaySignClassifier .
# Launch ODME → File → Open → select RunwaySignClassifier
```

See [`examples/RunwaySignClassifier/README.md`](examples/RunwaySignClassifier/README.md) for the complete variable catalog.

## Workflow

This repository uses GitHub Actions to automate build, testing, and release:

1. **Build and Test**: Compiles with Maven, runs unit tests with JaCoCo coverage
2. **Static Analysis**: SpotBugs + Checkstyle quality gates on `odme.domain.*` packages
3. **Publish Artifact**: Stages the built JAR and uploads it as a package
4. **Automate Release**: Creates a GitHub release based on the uploaded artifact

## Architecture

ODME follows a **strangler-fig architecture** with a clean domain layer:

```
odme/
├── domain/          ← Pure Java, no Swing/mxGraph. Fully unit-testable.
│   ├── model/       ← SESNode, SESTree, PESTree, Scenario
│   ├── operations/  ← Command pattern (Add/Delete/Prune/Rename)
│   ├── persistence/ ← XML serializer, JSON scenario store
│   ├── validation/  ← SES structure validator
│   ├── coverage/    ← ODD coverage analyzer
│   ├── enumeration/ ← PES enumerator (exhaustive + coverage-guided)
│   ├── traceability/← EASA traceability matrix, CSV/HTML exporters
│   └── audit/       ← Structured event logging
├── application/     ← ProjectService, ProjectSession
└── [legacy]         ← Swing UI (odmeeditor, jtreetograph, core, behaviour)
```

See [`ARCHITECTURE.md`](ARCHITECTURE.md) for design decisions and data flow.
See [`DEVELOPER_GUIDE.md`](DEVELOPER_GUIDE.md) for development workflow.

## Contributing

Contributions to ODME are welcome! See [`CONTRIBUTING.md`](CONTRIBUTING.md) for guidelines.

## License

ODME is released under the **MIT License**.
