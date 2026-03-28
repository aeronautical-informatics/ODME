# Contributing to ODME

All contributions — bug reports, bug fixes, documentation improvements,
enhancements, and ideas — are welcome.

---

## How to Contribute

### 1. Fork and branch

```bash
git clone https://github.com/umutdurak/ODME.git
cd ODME
git checkout -b feature/my-improvement
```

Never commit directly to `main`. Work on a dedicated branch.

### 2. Build locally

```bash
mvn clean install          # compile + test + package
```

Make sure the build is **green** before opening a PR.

### 3. Run quality checks

```bash
mvn test                   # 500+ unit tests + JaCoCo coverage
mvn spotbugs:check         # static analysis
mvn checkstyle:check       # code style
```

### 4. Open a Pull Request

Push your branch and open a PR against `main`. CI will run automatically.
Describe *what* changed and *why* in the PR description.

---

## Architecture Constraints

| Rule | Details |
|------|---------|
| **Domain boundary** | `odme.domain.*` must never import `javax.swing`, `java.awt`, or `com.mxgraph` |
| **New business logic** | Goes in `odme.domain.*` or `odme.application.*`, not in legacy UI packages |
| **New tests required** | All new domain/sampling/application code must include unit tests |
| **No `System.out.println`** | Use SLF4J (`LoggerFactory.getLogger(...)`) — Checkstyle enforces this for new files |
| **Sampling package** | `odme.sampling.*` must remain headless (no Swing) — UI lives only in `GenerateSamplesPanel` |

See [`ARCHITECTURE.md`](ARCHITECTURE.md) for the full package structure and design decisions.
See [`DEVELOPER_GUIDE.md`](DEVELOPER_GUIDE.md) for step-by-step development workflows.

---

## Reporting Issues

Please open a GitHub Issue with:
- Steps to reproduce
- Expected vs. actual behaviour
- Java version and OS

---

## Code of Conduct

See [`CODE_OF_CONDUCT.md`](CODE_OF_CONDUCT.md).
