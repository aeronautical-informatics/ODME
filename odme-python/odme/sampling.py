"""Latin Hypercube Sampling for ODD parameter spaces.

Generates space-filling test cases from ODME parameters, supporting
both continuous ranges (LHS) and categorical specializations (stratified
combinatorial). Zero external dependencies — uses only the Python stdlib.

Example::

    from odme import load_project
    from odme.sampling import LatinHypercubeSampler

    project = load_project("export.json")
    sampler = LatinHypercubeSampler(project.parameters)
    test_cases = sampler.sample(n=100, seed=42)
"""
from __future__ import annotations

import math
import random
from dataclasses import dataclass, field
from typing import Dict, List, Optional, Tuple

from .model import Parameter, SESNode, SESTree, TestCase


# ─── Continuous-only LHS ────────────────────────────────────────────────────

class LatinHypercubeSampler:
    """Latin Hypercube Sampler for continuous ODD parameters.

    Divides each parameter range into *n* equal strata and assigns one
    sample per stratum per dimension, with random pairing across dimensions.
    """

    def __init__(self, parameters: List[Parameter]):
        self.parameters = [p for p in parameters
                           if p.data_type in ("double", "float", "int")]

    def sample(self, n: int, seed: Optional[int] = None) -> List[TestCase]:
        """Generate *n* LHS test cases.

        Args:
            n: Number of samples (= number of strata per dimension).
            seed: Random seed for reproducibility.

        Returns:
            List of TestCase objects with sampled parameter values.
        """
        rng = random.Random(seed)
        dims = len(self.parameters)
        if dims == 0:
            return []

        # For each dimension, create a random permutation of strata
        permutations = []
        for _ in range(dims):
            perm = list(range(n))
            rng.shuffle(perm)
            permutations.append(perm)

        test_cases: List[TestCase] = []
        for i in range(n):
            values: Dict[str, float] = {}
            for d, param in enumerate(self.parameters):
                stratum = permutations[d][i]
                # Uniform random within the stratum
                lo = param.min + (param.max - param.min) * stratum / n
                hi = param.min + (param.max - param.min) * (stratum + 1) / n
                val = rng.uniform(lo, hi)
                if param.data_type == "int":
                    val = round(val)
                values[param.qualified_name] = val
            test_cases.append(TestCase(id=i + 1, values=values))
        return test_cases


# ─── Categorical specialization handling ────────────────────────────────────

@dataclass
class Specialization:
    """A specialization node with its leaf choices."""
    node_name: str
    choices: List[str]


def extract_specializations(tree: SESTree) -> List[Specialization]:
    """Walk the SES tree and extract all specialization dimensions.

    Each SPECIALIZATION node becomes one categorical dimension;
    its direct children are the choices.
    """
    if not tree.root:
        return []

    specs: List[Specialization] = []
    _walk_specializations(tree.root, specs)
    return specs


def _walk_specializations(node: SESNode, out: List[Specialization]):
    if node.type == "SPECIALIZATION":
        choices = [c.name for c in node.children]
        if choices:
            out.append(Specialization(node.name, choices))
    for child in node.children:
        _walk_specializations(child, out)


# ─── Combined sampler: categorical × continuous ─────────────────────────────

@dataclass
class ODDSample:
    """A single sample point in the full ODD space."""
    id: int
    choices: Dict[str, str]         # spec_name -> chosen leaf
    continuous: Dict[str, float]    # qualified_name -> value

    def to_test_case(self) -> TestCase:
        """Flatten into a TestCase with all values merged."""
        values: Dict[str, float] = dict(self.continuous)
        # Encode categorical choices as 1.0 for the chosen leaf
        for spec_name, choice in self.choices.items():
            values[f"_cat_.{spec_name}"] = hash(choice) % 10000
        return TestCase(id=self.id, values=values)


class FullODDSampler:
    """Combined sampler: stratified categorical × LHS continuous.

    Strategy:
      1. Select *k* PES configurations by stratified sampling over the
         categorical dimensions (specializations).
      2. For each PES configuration, run LHS over the continuous
         parameters that belong to the selected leaves.
      3. Total samples = k × n_per_pes.

    This ensures coverage of both the structural (categorical) and
    parametric (continuous) dimensions of the ODD.
    """

    def __init__(self, tree: SESTree, parameters: List[Parameter]):
        self.tree = tree
        self.all_params = parameters
        self.specs = extract_specializations(tree)
        self.continuous = [p for p in parameters
                           if p.data_type in ("double", "float", "int")]

    def _params_for_choices(self, choices: Dict[str, str]) -> List[Parameter]:
        """Return continuous parameters belonging to chosen leaves."""
        chosen_set = set(choices.values())
        return [p for p in self.continuous if p.parent_node in chosen_set]

    def sample_pes_configs(self, k: int,
                           seed: Optional[int] = None) -> List[Dict[str, str]]:
        """Generate *k* stratified PES configurations.

        Uses a Latin-square-like approach over the categorical dimensions
        to ensure coverage across all specialization choices.
        """
        rng = random.Random(seed)
        if not self.specs:
            return [{}] * min(k, 1)

        configs: List[Dict[str, str]] = []
        for i in range(k):
            cfg: Dict[str, str] = {}
            for spec in self.specs:
                # Stratified: cycle through choices, with random perturbation
                idx = (i + rng.randint(0, len(spec.choices) - 1)) % len(spec.choices)
                cfg[spec.node_name] = spec.choices[idx]
            configs.append(cfg)
        return configs

    def sample(self, n_pes: int = 20, n_per_pes: int = 10,
               seed: Optional[int] = None) -> List[ODDSample]:
        """Generate samples across the full ODD.

        Args:
            n_pes: Number of PES configurations to sample.
            n_per_pes: Number of LHS samples per PES configuration.
            seed: Random seed for reproducibility.

        Returns:
            List of ODDSample objects (total: n_pes × n_per_pes).
        """
        rng = random.Random(seed)
        configs = self.sample_pes_configs(n_pes, seed=seed)

        samples: List[ODDSample] = []
        sample_id = 1

        for cfg in configs:
            # Get continuous params for this PES config
            params = self._params_for_choices(cfg)
            if not params:
                params = self.continuous  # fallback to all

            # LHS within these params
            lhs = LatinHypercubeSampler(params)
            tcs = lhs.sample(n_per_pes, seed=rng.randint(0, 2**31))

            for tc in tcs:
                samples.append(ODDSample(
                    id=sample_id,
                    choices=dict(cfg),
                    continuous=tc.values,
                ))
                sample_id += 1

        return samples

    def total_pes_combinations(self) -> int:
        """Calculate total number of possible PES configurations."""
        if not self.specs:
            return 1
        total = 1
        for spec in self.specs:
            total *= len(spec.choices)
        return total
