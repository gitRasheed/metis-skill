# Testing

Write tests that make behavior obvious.

## Prefer explicit tests

When practical, prefer tests where each case is written directly rather than hidden inside loops, branching, or layers of fixtures.

Benefits:

- failures point more directly at the broken case
- expected behavior is easier to read in sequence
- the test itself is less likely to contain subtle logic bugs

## Prefer behavior over implementation

Favor tests that verify:

- visible outputs
- state transitions
- contract boundaries
- cross-module behavior

Avoid overfitting tests to private structure unless the implementation detail is itself the contract.

## Use indirection only when it helps

Parameterized or table-driven tests are still useful when:

- many cases truly share the same shape
- duplication would become harder to maintain than the table
- the test runner still produces readable failures

If the indirection hides the story of the test, simplify it.

## Good testing targets

- parsers
- state machines
- business rules
- transformations
- failure handling
- integrations with files, databases, services, or queues

## Lower-priority targets

- trivial passthroughs
- obvious getters and setters
- wrappers with no meaningful branching or transformation
