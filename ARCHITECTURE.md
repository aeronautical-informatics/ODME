# ODME Architecture Guide

## Overview

ODME (Operational Design Domain Modeling Environment) is a Java/Swing desktop
application for authoring System Entity Structure (SES) models and deriving
scenario sets for AI Learning Assurance (EASA AI Roadmap 2.0).

---

## Package Structure

```
odme/
├── domain/              <- PURE JAVA. No Swing. No mxGraph. Fully testable.
│   ├── model/           <- SESNode, SESTree, PESTree, Scenario, ScenarioStatus
│   ├── operations/      <- Commands (Add/Delete/Prune/Rename/SetVariable)
│   │                       CommandHistory (undo/redo)
│   │                       NodeFactory
│   ├── persistence/     <- SESSerializer (interface), XmlSESSerializer
│   │                       ScenarioStore (interface), JsonScenarioStore
│   ├── validation/      <- SESValidator (interface), SESStructureValidator
│   │                       ValidationResult
│   │                       FieldValidators (variable name/value/bound validation)
│   ├── coverage/        <- ODDCoverageAnalyzer, ODDCoverageReport
│   ├── enumeration/     <- PESEnumerator (interface), ExhaustivePESEnumerator
│   ├── traceability/    <- TraceabilityMatrix, TraceabilityEntry
│   │                       CsvTraceabilityExporter, HtmlTraceabilityExporter
│   ├── audit/           <- AuditLogger (structured event logging)
│   ├── graph/           <- SESGraph (interface), SESGraphNode, SESGraphEdge
│   │                       Graph abstraction decoupling algorithms from mxGraph
│   ├── transform/       <- XmlToPythonTranslator, YamlToPythonTranslator
│   │                       XsdToYamlConverter, XsdParser, XmlTransformRules
│   └── prune/           <- PruneEngine (algorithms against SESGraph interface)
│                           NamingConventions (Dec/Spec/MAsp suffix rules)
│
├── application/         <- Orchestration layer (use-case services)
│   ├── ProjectSession   <- Scoped session replacing static JtreeToGraphVariables
│   └── ProjectService   <- Lifecycle: create/save/load/validate/scenarios
│
├── sampling/            <- Constrained LHS sampling pipeline
│   ├── LatinHypercubeSampler    <- Generates normalized [0,1] LHS samples
│   ├── ConstraintEvaluator      <- Evaluates ODME constraint syntax via mXparser
│   │                               "if(@param > val) then (@other < lim) else true"
│   ├── ScenarioParser           <- SnakeYAML-based .yaml scenario file parser
│   │                               Extracts Parameters (numerical/categorical/
│   │                               distribution) and constraints (Intra/Inter)
│   ├── SamplingManager          <- Full pipeline: YAML → LHS → rejection sampling
│   │                               → scale to real ranges → CSV export
│   │                               Two modes: generateSamples() (incl. distribution)
│   │                               and generateSamplesforDomainModel() (excl. dist.)
│   ├── GenerateSamplesPanel     <- Swing UI: YAML picker, sample count, CSV output
│   │                               Uses BackgroundTaskRunner for non-blocking exec
│   ├── distribution/
│   │   └── DistributionSampling <- normalDistributionSample (5-tier SD approach)
│   │                               uniformDistributionSample([a,b])
│   └── model/
│       ├── Parameter            <- POJO: name, type, min, max, options,
│       │                           distributionName, distributionDetails, constraint
│       └── Scenario             <- POJO: List<Parameter> + List<String> constraints
│
└── [Legacy packages — unchanged, to be gradually wired to domain layer]
    ├── odmeeditor/      <- Swing UI (Main, ODMEEditor, DynamicTree, MenuBar...)
    │   ├── BackgroundTaskRunner  <- SwingWorker async task runner with modal
    │   │                           progress dialog; replaces boilerplate everywhere
    │   ├── Distribution          <- Right-panel table: Node/Variable/Distribution
    │   │                           Name/Details — reads from ODD XSD attributes
    │   ├── IntraEntityConstraint <- Constraint panel for within-entity constraints
    │   ├── InterEntityConstraints<- Constraint panel for cross-entity constraints
    │   │                           Both panels: read-only table + double-click dialog
    │   └── ScenarioGeneration    <- CSV rows → individual XML scenario files
    │                               Column format: "EntityName_VariableName"
    ├── jtreetograph/    <- Graph-tree synchronisation (JtreeToGraphAdd, Prune...)
    ├── core/            <- EditorContext singleton, FileConvertion, utilities
    ├── behaviour/       <- Behaviour tree editor
    ├── contextmenus/    <- Right-click context menus
    ├── controller/      <- MenuController, ToolbarController
    └── module/          <- External tool imports (CAMEO)
```

## Architectural Rules

### The Domain Boundary (Enforced)
`odme.domain.*` must have **zero imports** from:
- `javax.swing.*`, `java.awt.*` (Swing)
- `com.mxgraph.*` (mxGraph)
- `odme.odmeeditor.*`, `odme.jtreetograph.*` (legacy UI)

This boundary ensures all domain logic is unit-testable without a display.

### The Strangler Fig Pattern
New features are implemented in `odme.domain.*` and `odme.application.*`.
Legacy code is left unchanged. Over time, UI event handlers are updated to
delegate to `ProjectService` and `CommandHistory` rather than calling
legacy infrastructure directly.

---

## Key Design Decisions

### 1. Explicit Domain Model
Before 2.0, node type was inferred from JTree label suffixes at runtime.
Now `SESNodeType` enum is the single definition; `SESNode` carries its type
explicitly. The type system is discoverable, checkable at compile time, and
documented in one place.

### 2. Command Pattern for Mutations
All model mutations go through `SESCommand` implementations. Benefits:
- Unified undo/redo via `CommandHistory` (not mxUndoManager-only)
- Every mutation produces an `AuditLogger` event (EASA evidence trail)
- Commands are unit-testable without Swing event dispatch thread

### 3. ProjectSession over Global State
`JtreeToGraphVariables` held global mutable state as static fields.
`ProjectSession` scopes that state to one open project. This enables:
- Multiple projects open simultaneously (future)
- Clean testing (construct isolated session per test)
- Clear lifecycle (session created on open, discarded on close)

### 4. Format-Agnostic Persistence Interfaces
`SESSerializer` and `ScenarioStore` are interfaces. `XmlSESSerializer` and
`JsonScenarioStore` are current implementations. Binary `.ssd*` serialization
is replaced by human-readable, diffable, version-controllable files.

### 5. EASA AI Learning Assurance Features
`TraceabilityMatrix`, `ODDCoverageAnalyzer`, and `ExhaustivePESEnumerator`
implement the academic concepts from the companion papers:
- Gupta et al. (2026): ODD coverage metric and scenario enumeration
- Stefani et al. (2025): Automated scenario generation pipeline
- EASA AI Roadmap 2.0: ODD -> scenario -> test -> verdict traceability chain

---

## Data Flow

```
User action (Swing UI)
    |
    v
ProjectService / CommandHistory
    |
    v
SESCommand.execute(ProjectSession)
    |
    +-- Mutates SESTree (domain model)
    +-- Logs to AuditLogger
    +-- Returns result to UI


SESTree (single source of truth)
    |
    +-- JTree adapter (legacy DynamicTree) -- reads from SESTree
    +-- mxGraph adapter (legacy JtreeToGraph*) -- reads from SESTree
```

---

## Running Tests

```bash
# All tests
mvn test

# Coverage report
mvn jacoco:report
open target/site/jacoco/index.html

# Static analysis
mvn spotbugs:check
mvn checkstyle:check
```

---

## File Formats

| Format | Purpose | Location |
|--------|---------|----------|
| `{project}_domain.xml` | SES model (new format) | project directory |
| `{project}Graph.xml` | mxGraph layout (legacy) | project directory |
| `{project}.xml` | JTree structure (legacy) | project directory |
| `xsdfromxml.xsd` | Generated XSD with variable ranges | project directory |
| `scenarios.json` | Scenario catalogue | project directory |
| `*.ssdbeh` | Behaviour data (legacy binary) | scenario directory |
| `*.ssdvar` | Variable data (legacy binary) | scenario directory |
| `*.ssdcon` | Constraint data (legacy binary) | scenario directory |
| `odd/*.ser` | Saved ODD snapshots (Java serialized) | project `odd/` directory |
| `*_LHS_N.csv` | LHS test case exports | user-chosen location |
| Audit log | Structured event log | `~/.odme/logs/audit.log` |
| Application log | Application log | `~/.odme/logs/odme.log` |

---

## ODD Manager & Test Case Generation

The ODD Manager (`ODDManager.java`) reads the generated XSD (`xsdfromxml.xsd`)
to populate a table of all model entities and their variables. Key capabilities:

1. **Save/Load ODD** — serialized `.ser` snapshots for ODD versioning
2. **Export XML/YAML** — machine-readable and human-readable ODD exports
3. **Distribution columns** — reads `xs:distributionName` and `xs:distributionDetails`
   XSD attributes into the ODD table; population reflected in the `Distribution` panel
4. **Constraint classification** — heuristic split into intra-entity vs inter-entity;
   inter constraints contain the `"AC@"` marker
5. **Latin Hypercube Sampling** — `odmeeditor.LatinHypercubeSampler` generates test vectors
   directly from ODD table rows:
   - Extracts all variables with numeric `[min, max]` ranges (min < max)
   - Divides each parameter range into N equal strata
   - Assigns one random sample per stratum per parameter
   - Shuffles assignments to minimize inter-parameter correlations
   - Exports to CSV with full traceability headers

### Constrained Sampling Pipeline (`odme.sampling`)

A fully separate pipeline for YAML-driven, constraint-aware sampling:

```
ScenarioParser.parse(yamlFile)
    ↓
Scenario { List<Parameter>, List<String> constraints }
    ↓
SamplingManager.generateSamples(yaml, N, outputCsv)
    ├── LatinHypercubeSampler.generateNormalizedSamples(dims, N)  → [0,1] matrix
    ├── scaleSample() → maps [0,1] → [min, max] or distribution sample
    └── ConstraintEvaluator.evaluate(constraint, sample)  ← rejection gate
        ↓
writeToCsv()  → output CSV (numerical cols + random categorical cols)
```

**Constraint syntax** (ODME format → mXparser):
```
if(@rain_intensity > 5) then (@luminosity < 1000) else true
  ↓ (formatted by ConstraintEvaluator)
if(rainintensity > 5, luminosity < 1000, 1)
```

Special characters (`@`, `_`, `(`, `)`, `-`) are stripped from variable names
before mXparser evaluation.

**Distribution-typed parameters** use `distributionName` + `distributionDetails`
(format: `mean=50___stdDev=10` or `min=0___max=100`) instead of [min,max] LHS.

### Scenario Generation from CSV

`ScenarioGeneration.importScenarioDatasFromCSVFile(csvPath, outputDir)` converts
a sampled CSV into individual XML scenario files:

```
EgoAC_Altitude, EgoAC_Speed, Weather_Condition
500, 120, Rainy
800, 200, Clear
   ↓
Scenario_1.xml  (entity EgoAC with vars Altitude=500, Speed=120; entity Weather…)
Scenario_2.xml
```

Columns without `_` are ignored. Output folder: `GeneratedScenarios/{proj}_Scenarios/{name}/`.
Duplicate folder names get a `1` suffix with a warning in the return message.

### Path Construction

`EditorContext.getWorkingDir()` provides the single source of truth for file
paths in both SES and PES modes:
- **SES mode**: `{fileLocation}/{projName}/`
- **PES mode**: `{fileLocation}/{projName}/{currentScenario}/`

This replaced 26 scattered if/else blocks across 11 files.

---

## File Formats — Updated

| Format | Purpose | Location |
|--------|---------|----------|
| `{project}_domain.xml` | SES model (new format) | project directory |
| `{project}Graph.xml` | mxGraph layout (legacy) | project directory |
| `{project}.xml` | JTree structure (legacy) | project directory |
| `xsdfromxml.xsd` | Generated XSD with variable ranges + distribution attrs | project directory |
| `scenarios.json` | Scenario catalogue | project directory |
| `*.ssdbeh` | Behaviour data (legacy binary) | scenario directory |
| `*.ssdvar` | Variable data (legacy binary) | scenario directory |
| `*.ssdcon` | Constraint data (legacy binary) | scenario directory |
| `odd/*.ser` | Saved ODD snapshots (Java serialized) | project `odd/` directory |
| `*_LHS_N.csv` | LHS test case exports (odmeeditor.LatinHypercubeSampler) | user-chosen |
| `*.yaml` | Scenario definition for constrained sampling pipeline | user-chosen |
| `*_samples.csv` | SamplingManager output (sampling pipeline) | user-chosen |
| `GeneratedScenarios/{proj}…/Scenario_N.xml` | ScenarioGeneration XML files | project directory |
| Audit log | Structured event log | `~/.odme/logs/audit.log` |
| Application log | Application log | `~/.odme/logs/odme.log` |

---

## Example Projects

Example projects live in `examples/` and can be copied to the working directory:

| Example | Domain | Parameters | PES Combinations |
|---------|--------|------------|------------------|
| `RunwaySignClassifier` | Airborne ML sign detection (DAL C) | 43 (21 continuous) | 2,592 |
