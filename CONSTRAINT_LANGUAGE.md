# Constraint language

ODME supports a numeric and boolean subset of XPath 2.0 for sampling constraints.
Every parameter reference starts with `@`. For example, `@speed > 50 and @altitude <= 200`.
Plain references such as `speed > 50` are invalid.

Supported expressions include

- Numeric literals and `true()` or `false()`.
- Comparisons `=`, `!=`, `<`, `<=`, `>`, and `>=`.
- Boolean operations `and`, `or`, and `not(expression)`.
- Arithmetic `+`, `-`, `*`, `div`, and `mod`.
- Parentheses and nested `if (condition) then expression else expression`. Every conditional requires an `else` branch.

Examples

```text
@count mod 3 = 1
@distance div 2 >= 1
@mode = 2 or not(@count != 4)
if (@sun > @max_sun) then false() else true()
if (@a > 0) then (if (@b > 0) then true() else false()) else true()
```

The sampler translates expressions to mXparser. Complete words are translated outside
parameter references, so names such as `@Ordnance`, `@android_mode`, and `@divider` retain their meaning.
Variables keep the existing exact-name, normalized-name, and unique-suffix resolution rules.
Unknown or ambiguous references and invalid syntax fail validation before sampling starts.
An expression that is undefined for a particular sample rejects that sample.

Existing models can continue using `&&`, `||`, `==`, bare `true` and `false`,
and `/` for division. These forms are accepted by the sampler. XPath 2.0 uses
`and`, `or`, `=`, `true()`, `false()`, and `div` instead.

## Schema assertions

The SES schema exporter preserves each constraint's text in its `xs:assert` attribute,
escaping XML characters such as `<` and `&`. It does not translate legacy syntax
into XPath syntax.

In an XSD 1.1 `xs:assert`, `@name` refers to an attribute of the element carrying the
assertion. ODME resolves `@name` across the model by name. A constraint spanning
entities can therefore evaluate correctly in ODME and still fail in an external
XSD 1.1 processor. The supported subset does not include general XPath navigation,
sequences, quantified expressions, or string operations.
