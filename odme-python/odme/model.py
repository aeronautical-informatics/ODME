"""Domain model dataclasses mirroring the ODME JSON export schema."""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Dict, List, Optional


@dataclass
class SESNode:
    """A node in the System Entity Structure tree."""

    id: str
    name: str
    type: str  # ENTITY, ASPECT, MULTI_ASPECT, SPECIALIZATION
    label: str
    path: str
    variables: Dict[str, str] = field(default_factory=dict)
    behaviours: List[str] = field(default_factory=list)
    constraints: List[str] = field(default_factory=list)
    flags: Dict[str, str] = field(default_factory=dict)
    children: List[SESNode] = field(default_factory=list)

    @classmethod
    def from_dict(cls, d: dict) -> SESNode:
        children = [cls.from_dict(c) for c in d.get("children") or []]
        return cls(
            id=d["id"],
            name=d["name"],
            type=d["type"],
            label=d.get("label", d["name"]),
            path=d["path"],
            variables=d.get("variables") or {},
            behaviours=d.get("behaviours") or [],
            constraints=d.get("constraints") or [],
            flags=d.get("flags") or {},
            children=children,
        )

    def leaf_nodes(self) -> List[SESNode]:
        """Return all leaf nodes in this subtree."""
        if not self.children:
            return [self]
        result: list[SESNode] = []
        for c in self.children:
            result.extend(c.leaf_nodes())
        return result

    def find_by_name(self, name: str) -> Optional[SESNode]:
        """Find first node with the given name in this subtree."""
        if self.name == name:
            return self
        for c in self.children:
            found = c.find_by_name(name)
            if found:
                return found
        return None

    def find_by_type(self, node_type: str) -> List[SESNode]:
        """Find all nodes with the given type in this subtree."""
        result: list[SESNode] = []
        if self.type == node_type:
            result.append(self)
        for c in self.children:
            result.extend(c.find_by_type(node_type))
        return result

    def subtree(self) -> List[SESNode]:
        """Return all nodes in this subtree in pre-order."""
        result: list[SESNode] = [self]
        for c in self.children:
            result.extend(c.subtree())
        return result


@dataclass
class SESTree:
    """The complete System Entity Structure (ODD model)."""

    id: str
    name: str
    schema_version: str
    root: Optional[SESNode] = None

    @classmethod
    def from_dict(cls, d: dict) -> SESTree:
        root = SESNode.from_dict(d["root"]) if d.get("root") else None
        return cls(
            id=d["id"],
            name=d["name"],
            schema_version=d.get("schemaVersion", "1.0"),
            root=root,
        )


@dataclass
class PESTree:
    """A Pruned Entity Structure — one concrete scenario."""

    id: str
    name: str
    source_ses_id: str
    pruned_node_ids: list[str] = field(default_factory=list)
    root: Optional[SESNode] = None

    @classmethod
    def from_dict(cls, d: dict) -> PESTree:
        root = SESNode.from_dict(d["root"]) if d.get("root") else None
        return cls(
            id=d["id"],
            name=d["name"],
            source_ses_id=d.get("sourceSESId", ""),
            pruned_node_ids=d.get("prunedNodeIds") or [],
            root=root,
        )


@dataclass
class Scenario:
    """A test scenario derived from the ODD."""

    id: str
    name: str
    status: str
    risk: str = ""
    remarks: str = ""
    source_ses_id: Optional[str] = None
    pes_id: Optional[str] = None
    created_at: Optional[str] = None
    created_by: Optional[str] = None
    pes_tree: Optional[PESTree] = None

    @classmethod
    def from_dict(cls, d: dict) -> Scenario:
        pes = PESTree.from_dict(d["pesTree"]) if d.get("pesTree") else None
        return cls(
            id=d["id"],
            name=d["name"],
            status=d["status"],
            risk=d.get("risk", ""),
            remarks=d.get("remarks", ""),
            source_ses_id=d.get("sourceSESId"),
            pes_id=d.get("pesId"),
            created_at=d.get("createdAt"),
            created_by=d.get("createdBy"),
            pes_tree=pes,
        )


@dataclass
class Parameter:
    """An ODD parameter with sampling bounds."""

    name: str
    parent_node: str
    qualified_name: str
    data_type: str
    min: float
    max: float

    @classmethod
    def from_dict(cls, d: dict) -> Parameter:
        return cls(
            name=d["name"],
            parent_node=d["parentNode"],
            qualified_name=d.get("qualifiedName", f"{d['parentNode']}.{d['name']}"),
            data_type=d["dataType"],
            min=d["min"],
            max=d["max"],
        )


@dataclass
class TestCase:
    """A generated test case with sampled parameter values."""

    id: int
    values: dict[str, float]

    @classmethod
    def from_dict(cls, d: dict) -> TestCase:
        return cls(id=d["id"], values=d["values"])


@dataclass
class CoverageReport:
    """ODD coverage analysis results."""

    total_leaf_nodes: int
    covered_leaf_nodes: int
    coverage_percent: float
    scenario_count: int
    uncovered_nodes: list[dict]

    @classmethod
    def from_dict(cls, d: dict) -> CoverageReport:
        return cls(
            total_leaf_nodes=d["totalLeafNodes"],
            covered_leaf_nodes=d["coveredLeafNodes"],
            coverage_percent=d["coveragePercent"],
            scenario_count=d.get("scenarioCount", 0),
            uncovered_nodes=d.get("uncoveredNodes") or [],
        )


@dataclass
class TraceabilityEntry:
    """An ODD element to scenario to test case link."""

    odd_element_id: str
    odd_element_name: str
    odd_element_path: str
    scenario_id: str
    scenario_name: str
    test_case_id: Optional[str] = None
    verdict: Optional[str] = None

    @classmethod
    def from_dict(cls, d: dict) -> TraceabilityEntry:
        return cls(
            odd_element_id=d["oddElementId"],
            odd_element_name=d["oddElementName"],
            odd_element_path=d["oddElementPath"],
            scenario_id=d["scenarioId"],
            scenario_name=d["scenarioName"],
            test_case_id=d.get("testCaseId"),
            verdict=d.get("verdict"),
        )


@dataclass
class ProjectMeta:
    """Project identity and metadata."""

    id: str
    name: str
    directory: str

    @classmethod
    def from_dict(cls, d: dict) -> ProjectMeta:
        return cls(id=d["id"], name=d["name"], directory=d.get("directory", ""))


@dataclass
class Project:
    """The complete ODME project export."""

    version: str
    meta: ProjectMeta
    ses_tree: SESTree
    scenarios: list[Scenario]
    parameters: list[Parameter]
    test_cases: list[TestCase]
    coverage: Optional[CoverageReport]
    traceability: list[TraceabilityEntry]

    @classmethod
    def from_dict(cls, d: dict) -> Project:
        return cls(
            version=d["version"],
            meta=ProjectMeta.from_dict(d["project"]),
            ses_tree=SESTree.from_dict(d["sesTree"]),
            scenarios=[Scenario.from_dict(s) for s in d.get("scenarios") or []],
            parameters=[Parameter.from_dict(p) for p in d.get("parameters") or []],
            test_cases=[TestCase.from_dict(t) for t in d.get("testCases") or []],
            coverage=(
                CoverageReport.from_dict(d["coverage"]) if d.get("coverage") else None
            ),
            traceability=[
                TraceabilityEntry.from_dict(t) for t in d.get("traceability") or []
            ],
        )
