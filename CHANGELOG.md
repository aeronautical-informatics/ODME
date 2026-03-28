# Changelog

All notable changes to ODME are documented in this file.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).
This project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased] — 2.0.0-SNAPSHOT

### Added — Phase 8: Constrained Sampling Pipeline & Scenario Generation

Ported and adapted from [AIgsid/ODME-refactored](https://github.com/AIgsid/ODME-refactored).

#### New package: `odme.sampling`
- `LatinHypercubeSampler` — space-filling normalized LHS sampler (returns `List<double[]>`
  in [0,1]^dimensions; used by SamplingManager)
- `ConstraintEvaluator` — evaluates ODME constraint syntax
  `if(@param > val) then (@other < limit) else true` via mXparser 5.2.1;
  strips special characters from variable names; license confirmed non-commercial
- `ScenarioParser` — SnakeYAML-based parser for `.yaml` scenario files;
  supports numerical (int/double/float), categorical (options list), distribution-typed
  parameters and `HasConstraint` blocks with `IntraConstraint`/`InterConstraint` entries;
  constraint list is reset per `parse()` call (no cross-call leakage)
- `SamplingManager` — full constrained sampling pipeline:
  `generateSamples()` (includes distribution-typed params) and
  `generateSamplesforDomainModel()` (excludes distribution type);
  rejection sampling with `maxAttempts = N × 200`; exports CSV
- `GenerateSamplesPanel` — Swing UI panel: YAML file picker, sample count field,
  CSV output path picker, Generate/Cancel buttons; uses `BackgroundTaskRunner`
- `distribution/DistributionSampling` — `normalDistributionSample(mean, stdDev, n)`
  (5-tier SD-threshold: μ−σ, μ, μ+σ, μ+2σ, μ+3σ) and `uniformDistributionSample(a, b)`
- `model/Parameter` — plain POJO: name, type, min, max, options,
  distributionName, distributionDetails, constraint
- `model/Scenario` — plain POJO: `List<Parameter>` + `List<String>` constraints

#### New files: `odme.odmeeditor`
- `BackgroundTaskRunner` — generic `SwingWorker`-based async task runner;
  shows modal progress dialog with wait cursor; `run(parent, title, msg, task, onSuccess, onError)`
- `Distribution` — 4-column table panel (Node Name, Variable Name, Distribution Name,
  Details); reads distribution metadata from ODD XSD attributes
- `IntraEntityConstraint` — read-only single-column table for within-entity constraints;
  double-click opens viewing dialog
- `InterEntityConstraints` — read-only single-column table for cross-entity constraints;
  same double-click pattern
- `ScenarioGeneration` — converts CSV rows into individual XML scenario files;
  groups columns by entity prefix (`EgoAC_Altitude` → entity `EgoAC`, var `Altitude`);
  handles duplicate folder names; exposes `importScenarioDatasFromCSVFile()` directly

#### Modified files
- `pom.xml` — added `mXparser 5.2.1` and explicit `SnakeYAML 2.2` dependencies
- `MenuBar` — added "Generate Samples" to Scenario Manager menu;
  added "Generate Scenario → From CSV" submenu under "Save Scenario";
  new dialogs use `BackgroundTaskRunner` for non-blocking execution
- `ODMEEditor` — added `Distribution`, `IntraEntityConstraint`, `InterEntityConstraints`
  panels to right-side data view; reset calls in mode-switch listener
- `PanelSplitor` — new overloaded `addSplitor()` accepting Distribution,
  InterEntityConstraints, IntraEntityConstraint alongside existing params
- `ODDManager` — added `currentXsdToYamlTemp()` helper delegating to `xsdToYaml()`

#### Tests
- 104 new tests across 6 test classes — **534 total, 0 failures**
  - `sampling/LatinHypercubeSamplerTest` (9): bounds, strata coverage, edge cases
  - `sampling/ConstraintEvaluatorTest` (10): satisfied/violated, boundary, null/malformed
  - `sampling/distribution/DistributionSamplingTest` (58): tier coverage, bounds, edge cases
  - `sampling/ScenarioParserTest` (10): numerical/categorical/distribution/constraint YAML
  - `sampling/SamplingManagerTest` (7): row count, bounds, categorical, domain-model mode
  - `odmeeditor/ScenarioGenerationTest` (10): XML structure, entity grouping, edge cases

---

### Added — Phase 1: Domain Model Foundation
- `SESNodeType` enum replacing all scattered `endsWith("Dec")`/`"MAsp"`/`"Spec"` magic-string checks
- `SESNode` typed domain object with id, name, type, children, variables, behaviours, constraints, flags
- `SESTree` — single source of truth for the SES model (replaces implicit JTree state)
- `PESTree` — pruned scenario tree linked to source SES for traceability
- `Scenario` with EASA AI Learning Assurance lifecycle state machine
  (DRAFT → IN_REVIEW → APPROVED → DEPRECATED)
- `AuditEntry` immutable record for traceability evidence trail
- `ScenarioStatus` enum covering full EASA review lifecycle
- `SESSerializer` / `ScenarioStore` interfaces for format-agnostic persistence
- `JsonScenarioStore` — backward-compatible JSON persistence with legacy `scenarios.json` reader
- `ValidationResult` — error/warning/info finding collector
- `SESValidator` interface + `SESStructureValidator` (SES well-formedness rules)
- `ProjectSession` — scoped session object replacing `JtreeToGraphVariables` static fields
- `AuditLogger` — structured event logger writing to dedicated rolling audit log
- SLF4J 2.0.9 + Logback 1.4.11 replacing scattered `System.out.println` calls
- `logback.xml` — console + rolling application log + dedicated audit log appenders
- 52 new unit tests

### Added — Phase 2: Command Pattern & Operations
- `SESCommand` interface for reversible domain operations
- `CommandHistory` — unified undo/redo stack (replaces mxUndoManager)
- `AddNodeCommand`, `DeleteNodeCommand`, `PruneToScenarioCommand`,
  `RenameNodeCommand`, `SetNodeVariableCommand`
- `NodeFactory` — ID-generating factory replacing static `JtreeToGraphVariables.nodeNumber`
- `ODDCoverageAnalyzer` + `ODDCoverageReport` — leaf-node coverage metric
  implementing Gupta et al. (2026) methodology
- 25 new unit tests

### Added — Phase 3: XML Persistence & CI Quality Gates
- `XmlSESSerializer` — DOM-based round-trip XML persistence
  (replaces binary `.ssdbeh`/`.ssdvar`/`.ssdcon`/`.ssdflag` serialization)
- JaCoCo 70% line-coverage gate on `odme.domain.*` packages
- SpotBugs 4.8.3.1 + Checkstyle 3.3.1 Maven plugins
- `checkstyle.xml` — naming, complexity <= 15, file-length <= 600 rules
- `checkstyle-suppressions.xml` — all legacy packages suppressed
- `spotbugs-exclude.xml` — all legacy packages excluded
- CI workflow updated with SpotBugs + Checkstyle steps
- 8 new XML round-trip tests

### Added — Phase 4: EASA AI Learning Assurance Features
- `TraceabilityMatrix` — ODD element <-> scenario <-> test case mapping
- `TraceabilityEntry` record for individual trace links
- `CsvTraceabilityExporter` — CSV export for ALM tool import (DOORS, etc.)
- `HtmlTraceabilityExporter` — self-contained HTML evidence report
- `PESEnumerator` interface + `ExhaustivePESEnumerator`
  (automated scenario generation via Cartesian product of specialization choices)
- Coverage-guided `enumerateToCoverage()` for minimal scenario sets
- `ProjectService` — application-layer boundary (create/save/load/validate/scenarios)
- 21 new tests (traceability, enumeration, project service)

### Added — Phase 5: Infrastructure & Documentation
- `CHANGELOG.md` (this file)
- `ARCHITECTURE.md` — package structure, design decisions, data flow
- End-to-end integration test (`FullWorkflowIntegrationTest`)
- Updated `DEVELOPER_GUIDE.md` with domain-driven development guide

### Added — Phase 7: Domain Layer Extraction & Graph Abstraction
- `SESGraph` interface — graph abstraction decoupling domain algorithms from mxGraph
- `SESGraphNode` / `SESGraphEdge` — lightweight value objects for graph traversal
- `XmlToPythonTranslator` — extracted from `Execution.java` (264 lines pure business logic)
- `YamlToPythonTranslator` — extracted from `Execution.java` (85 lines)
- `XsdToYamlConverter` — extracted from `ODDManager.java` (60 lines)
- `XsdParser` — extracted from `ODDManager.java` (80 lines XSD parsing pipeline)
- `XmlTransformRules` — extracted from `JtreeToGraphModify` + `JtreeToGraphGeneral` (350 lines)
- `FieldValidators` — extracted from `Variable.java` (128 lines validation logic)
- `PruneEngine` — pruning algorithms working against `SESGraph` interface
- `NamingConventions` — SES suffix detection/manipulation (Dec/Spec/MAsp rules)
- 99 new tests (221 total, 0 failures)

### Added — Phase 6: Bug Fixes, LHS, Example Project
- `EditorContext.getWorkingDir()` — centralized path construction for SES/PES modes,
  replacing 26 duplicated if/else blocks across 11 files
- `LatinHypercubeSampler` — Latin Hypercube Sampling engine for generating
  coverage-efficient test cases from ODD parameter ranges
- "Generate Test Cases (LHS)" button in ODD Manager — extracts continuous
  parameters from ODD table, prompts for sample count, exports CSV
- **RunwaySignClassifier example** (`examples/RunwaySignClassifier/`) —
  complete ODD model from Dmitriev et al. (DASC 2023) with 43 parameters,
  23 leaf entities, 2,592 PES combinations, and pre-generated LHS samples

### Fixed
- `MenuController.openFunc()` was missing `setNewFileName()` call, causing
  `getSsdFileGraph()` to return wrong path for non-Main projects
- `DynamicTree.openExistingProject()` now refreshes all `.ssd*` file references
  when opening a different project (previously kept stale Main references)
- `ODDManager.xsdToYaml()` NPE when `row[3]` (lower bound) is null —
  now checks for null before calling `isEmpty()`

### Changed
- Version: `1.0-SNAPSHOT` → `2.0.0-SNAPSHOT`
- `pom.xml`: added SLF4J, Logback, AssertJ, jackson-datatype-jsr310 dependencies

### Architecture Notes
- All new code lives under `odme.domain.*` and `odme.application.*`
- No existing source files modified (strangler-fig pattern)
- `odme.domain.*` has zero dependencies on Swing or mxGraph
- Legacy code (`odmeeditor`, `jtreetograph`, `core`, `behaviour`) unchanged

---

## [1.0.0] — 2024 (Academic Prototype)

Initial release used in DLR research publications:
- SES-based ODD modelling with mxGraph visualization
- JTree/mxGraph dual representation synchronized via `jtreetograph` package
- Behaviour, variable, constraint editors with binary `.ssd*` persistence
- CAMEO SysML import module
- Scenario management via `scenarios.json`
- FlatLaf modern Swing look-and-feel
