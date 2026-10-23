## ODME — coverage maps and workflow improvements

This release includes the current desktop application, with:

- Parameter Coverage Map: view all numeric parameters together using shared
  bandwidth and adequacy settings, or inspect individual parameters with advanced
  kernels, custom regions, scores and CSV/SVG exports.
- Automatic and Cartesian scenario generation from specialization choices.
- Behaviour graph and behaviour-tree XML loading, export/merge improvements and
  clearer behaviour rendering and scenario selection.
- Background project loading and YAML export of edited ODD values.
- Updated README, coverage help and bundled manual.

### Download and run

Windows: extract the Windows ZIP and run `ODME/ODME.exe`. Keep the entire folder
together; it includes the Java runtime. Save your projects before replacing an
older application folder.

Linux and macOS: use the platform archives or installers attached below.
Assets may be unsigned when signing credentials are not configured.

### Coverage quick start

Open a project with saved scenarios, then **Scenario Manager > Scenarios List >
Estimate Coverage > Parameter Coverage Map > All parameters**. Keep the defaults
and click **Calculate all parameters**. The **?** buttons explain the settings.

Coverage maps measure numeric parameter-wise marginal support. Categorical
coverage, conditional/invalid-range exclusion, saved map configurations and
explicit dataset partition/version selection are not implemented.

### Validation

Local validation ran 588 tests with Python configured: 587 passed. The remaining
`ConstraintEvaluatorTest.evaluate_plainBooleanExpression_returnsTrue` failure was
also reproduced on the prior upstream baseline `51feee31`.
