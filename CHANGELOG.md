# Changelog

All notable changes to ODME are documented in this file.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).
This project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased] — 2.0.0-SNAPSHOT

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
