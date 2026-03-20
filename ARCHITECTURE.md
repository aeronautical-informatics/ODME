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
│   ├── coverage/        <- ODDCoverageAnalyzer, ODDCoverageReport
│   ├── enumeration/     <- PESEnumerator (interface), ExhaustivePESEnumerator
│   ├── traceability/    <- TraceabilityMatrix, TraceabilityEntry
│   │                       CsvTraceabilityExporter, HtmlTraceabilityExporter
│   └── audit/           <- AuditLogger (structured event logging)
│
├── application/         <- Orchestration layer (use-case services)
│   ├── ProjectSession   <- Scoped session replacing static JtreeToGraphVariables
│   └── ProjectService   <- Lifecycle: create/save/load/validate/scenarios
│
└── [Legacy packages — unchanged, to be gradually wired to domain layer]
    ├── odmeeditor/      <- Swing UI (Main, ODMEEditor, DynamicTree, MenuBar...)
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
| `scenarios.json` | Scenario catalogue | project directory |
| `*.ssdbeh` | Behaviour data (legacy binary) | scenario directory |
| `*.ssdvar` | Variable data (legacy binary) | scenario directory |
| `*.ssdcon` | Constraint data (legacy binary) | scenario directory |
| Audit log | Structured event log | `~/.odme/logs/audit.log` |
| Application log | Application log | `~/.odme/logs/odme.log` |
