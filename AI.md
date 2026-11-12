# AI Usage Disclosure

## 1. Summary

AI tools were used during the development process for discussion of product behavior and edge cases, architectural and technical discussions, test generation, and boilerplate code generation. This document separates information supplied by the developer from interpretation of the final code. It does not attribute individual source files or design choices to a particular tool without evidence.

## 2. Tools, environment, and limitations

| Item | Disclosure |
| --- | --- |
| Tools reported by the developer | **ChatGPT-6** and **Claude Sonnet 5.5** |
| Claude modes reported | **Medium / Extra** |
| Product / requirements discussion | AI was assigned the role of a product-owner-style discussion partner to explore possible edge cases and business behavior |
| Technical use | Architecture discussions and technical debate |
| Generated material | Tests and boilerplate code |
| Exact dates, sessions, model settings beyond those above | **not provided** |
| IDE integrations, plugins, or other tools | **not provided** |
| Full proportion of AI-generated versus manually written code | **not provided** |

## 3. Prompt history

Exact verbatim implementation prompts: **not provided**.

The developer described the general approach as configuring AI to act as a knowledgeable Product Owner for discussions of product requirements, edge cases, and unusual scenarios. This is a **description of usage**, not a reconstructed quotation or a prompt transcript.

No unprovided prompts, chat transcripts, or chronological sequence of AI interactions are presented as historical facts.

## 4. Context and artifacts

The available implementation artifacts are the Java source files, JUnit test source files, `pom.xml`, and the Git directory included with the submitted project archive. The stated assignment concerns an in-memory football scoreboard supporting match start, score updates, finish, and ordered summary, with exactly one further public operation to be committed separately.

The source code shows a public `Scoreboard` API, an implementation named `DefaultScoreboard`, immutable match/state values, and `LiveBoardStateStore` for serialized transitions. These are observable characteristics of the supplied code, **not evidence of which person or AI tool authored them**.

The provided Git directory did not contain an accessible commit history during this documentation review, so commits cannot be used to establish the origin or sequence of particular decisions.

## 5. Architectural influence: interpretation, not verified history

The developer confirmed that AI was used for architecture discussions. The following topics are present in the implementation and **may have been discussed**, but their specific origin and whether an AI suggestion was accepted are **not provided**:

- UUID-backed match identity and separate prevention of simultaneous matches involving the same team.
- Team-name normalization with `strip()` and `Locale.ROOT`.
- Immutable `Match` and `LiveBoardState` objects.
- Serialized state transitions using `ReentrantLock`, with publication through `volatile`.
- Newest-first tie resolution through insertion order and stable sorting.
- Summary construction outside the store's write lock.

This list is a reconstruction of design topics from the code, **not** a record of actual AI recommendations or human decisions.

## 6. Developer review, modifications, and rejected suggestions

**To be completed.** Specific code reviewed, manually changed, or rejected by the developer: **not provided**.

Code written entirely by the developer without AI assistance: **not provided**.

No claims are made here that the developer individually reviewed every generated line, rewrote particular classes, or rejected specific AI proposals.

## 7. Verification and remaining unknowns

The codebase contains JUnit tests for lifecycle behavior, input validation, ordering, immutable snapshots, value objects, and selected concurrent scenarios. These are checks **present in the repository**; they do not establish who wrote or executed the tests.

During this documentation review, production Java sources were checked for compilation with Java 21. A successful full Maven test run was **not verified** in the review environment. No benchmarks, formal concurrency proofs, or independent verification of AI-generated output are claimed.

As of the supplied archive, the planned additional `getMatch` operation is not present in the public API. Its implementation, tests, and separate commit are pending.

The disclosure can be extended when actual prompt transcripts, personal review notes, or verifiable Git history become available. Unavailable information remains explicitly marked **not provided** rather than being inferred as fact.
