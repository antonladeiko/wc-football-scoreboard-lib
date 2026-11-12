# Live Football World Cup Scoreboard

An in-memory Java 21 library for tracking active football matches and querying their current scores. It implements the four scoreboard operations required by the Sportradar assignment and adds one extension: criteria-based search over active matches.

## Quick start

Requires **Java 21** and **Maven**.

```java
import com.sportradar.scoreboard.api.MatchCriteria;
import com.sportradar.scoreboard.api.MatchId;
import com.sportradar.scoreboard.api.MatchSnapshot;
import com.sportradar.scoreboard.api.MatchSort;
import com.sportradar.scoreboard.api.Scoreboard;
import com.sportradar.scoreboard.core.DefaultScoreboard;

import java.util.List;

Scoreboard scoreboard = new DefaultScoreboard();

MatchId mexicoCanada = scoreboard.startMatch("Mexico", "Canada");
MatchId spainBrazil = scoreboard.startMatch("Spain", "Brazil");

scoreboard.updateScore(mexicoCanada, 0, 5);
scoreboard.updateScore(spainBrazil, 10, 2);

List<MatchSnapshot> summary = scoreboard.getSummary();
// Spain 10–2 Brazil, then Mexico 0–5 Canada

MatchCriteria criteria = MatchCriteria.builder()
        .minTotalGoals(5)
        .teams("Mexico", "Brazil")
        .sortBy(MatchSort.TOTAL_GOALS_ASC)
        .build();

List<MatchSnapshot> matches = scoreboard.findByCriteria(criteria);
// Mexico 0–5 Canada, then Spain 10–2 Brazil

scoreboard.finishMatch(mexicoCanada);
// Mexico vs Canada no longer appears in the summary.
```

```bash
mvn clean test
mvn clean package
```

## Public API

`Scoreboard` is the public interface; `DefaultScoreboard` is its in-memory implementation.

| Operation | Contract |
| --- | --- |
| `MatchId startMatch(String homeTeam, String awayTeam)` | Starts a match at 0–0 and returns its generated identifier. |
| `void updateScore(MatchId id, int homeScore, int awayScore)` | Replaces **both** current scores; the arguments are not goal increments. |
| `void finishMatch(MatchId id)` | Removes the match from the active scoreboard. |
| `List<MatchSnapshot> getSummary()` | Returns an unmodifiable list of active match snapshots, sorted by total goals descending, then by later start first. |
| `List<MatchSnapshot> findByCriteria(MatchCriteria criteria)` | Returns matching active-match snapshots, filtered by optional criteria and ordered using the selected sort mode. |

### Criteria-based search (additional operation)

`findByCriteria` is the **one additional operation** beyond the assignment's four required operations. It reads one immutable board-state snapshot, applies all specified filters, and returns an unmodifiable list of `MatchSnapshot` values. It does not modify any match.

Use `MatchCriteria.builder()` to configure optional filters:

| Builder method | Meaning |
| --- | --- |
| `bothTeamsScored(true)` | Both teams must have scored at least once. |
| `bothTeamsScored(false)` | At least one team must have scored zero goals (including a 0–0 match). |
| `minTotalGoals(long)` | Inclusive lower bound on the sum of both teams' goals. |
| `maxTotalGoals(long)` | Inclusive upper bound on the sum of both teams' goals. |
| `teams(String...)` | Match either home or away team against **any** supplied name (OR within the team filter). Repeated calls accumulate names. |
| `sortBy(MatchSort)` | Selects result ordering; defaults to `TOTAL_GOALS_DESC`. |

All enabled filter categories combine using **AND**. Unspecified filters impose no restriction; an empty `MatchCriteria` therefore returns all active matches in summary order. Team matching follows `TeamName` equality: it ignores case and leading/trailing whitespace, but not repeated internal whitespace or team aliases. Filtering considers **active matches only**.

Available ordering modes:

| `MatchSort` | Ordering |
| --- | --- |
| `TOTAL_GOALS_DESC` | Total goals descending (default). |
| `TOTAL_GOALS_ASC` | Total goals ascending. |
| `HOME_TEAM_ASC` | Home-team display name ascending, compared case-insensitively. |

**Tie-breaking:** in all three modes, equal comparator values retain newest-started-first order. This is implemented by iterating matches in reverse insertion order before stable sorting; it is not a separate explicit comparator key.

`MatchCriteria.Builder` rejects negative goal bounds, a minimum greater than the maximum, blank team names, null team-name entries/arrays, and a null sort mode. `teams()` strips outer whitespace and removes exact duplicate strings; case variants can remain distinct in `criteria.teams()` but match equivalently during search. The built criteria's team set is unmodifiable, and later changes to the builder do not change already built criteria. A null `MatchCriteria` passed to `findByCriteria` is rejected.

### Errors and validation

| Situation | Exception |
| --- | --- |
| A team is already in an active match (as home or away) | `TeamAlreadyPlayingException` |
| Unknown or already finished match in `updateScore` / `finishMatch` | `MatchNotFoundException` |
| Same home and away team, blank team name, or negative score | `IllegalArgumentException` |
| Null team name or match identifier passed to lifecycle operations; null search criteria | `NullPointerException` |
| Invalid `MatchCriteria` bounds or blank team names | `IllegalArgumentException` |
| Null team-name array/entry or null sort in `MatchCriteria.Builder` | `NullPointerException` |

`TeamAlreadyPlayingException` and `MatchNotFoundException` extend `ScoreboardException` (an unchecked exception). Repeating `finishMatch` is not idempotent: the second call throws `MatchNotFoundException`.

## Requirements and implementation choices

### [Assignment] Sportradar requirements

| Requirement | Implementation |
| --- | --- |
| Start a match at 0–0 | `startMatch` |
| Update the current score | `updateScore` |
| Remove finished matches from the active scoreboard | `finishMatch` |
| Summary ordered by total score descending, newest start first on ties | `getSummary` |
| Add exactly one further operation in a separate commit | `findByCriteria(MatchCriteria)` is the additional operation. Its placement in a separate commit must be verified from Git history. |
| Supply `README.md` and `AI.md` | These documents |

### [Decision] Additional engineering choices

| Choice | Rationale |
| --- | --- |
| UUID-backed `MatchId` | Identifies an individual match independently of its teams. |
| Case-insensitive team comparison after trimming outer whitespace | Avoids treating trivial casing / outer whitespace differences as distinct teams. |
| One active match per team | Enforced during `startMatch`, independently of whether the team is home or away. |
| Immutable match and board-state versions | Lets readers work with a consistent published state. |
| `ReentrantLock` for state transitions | Serializes validation and mutation of the logical board state. |
| `volatile` state publication | Provides visibility of completed state versions to readers. |
| Ordered map instead of timestamps | Preserves match-start order without depending on wall-clock time. |
| Immutable query criteria and composable predicates | Keeps search configuration separate from filtering and sorting over an immutable snapshot. |
| One criteria-based search operation | Extends read access without introducing additional public commands or changing `getSummary`. |

These choices are implementation decisions, **not** additional assignment requirements.

## Assumptions

- The scoreboard is an in-memory, per-instance collection of **active** matches; there is no persistence or recovery after process restart.
- A team cannot participate in two active matches simultaneously. The same teams can play again after a match is finished.
- `updateScore` supplies the authoritative current score, including corrections; it does not require monotonically increasing values.
- A finished match is not available through the active scoreboard. No match history is retained.
- Team names are compared using `strip()` and `toLowerCase(Locale.ROOT)`. Display names retain their original letter casing after stripping outer whitespace. Internal whitespace, aliases, and Unicode normalization are not handled.
- Each `DefaultScoreboard` instance owns its own state. There is no synchronization or coordination between instances.
- `findByCriteria` searches only active matches. An empty result can mean either no active matches or no matches satisfying the criteria.
- Multiple team names use OR semantics; different types of criteria use AND semantics. `bothTeamsScored(false)` includes 0–0 results.

## Architecture

```text
com.sportradar.scoreboard
├── api
│   ├── Scoreboard            public contract
│   ├── MatchId               UUID-backed identifier
│   ├── MatchSnapshot         read-only result DTO
│   ├── MatchCriteria         immutable search configuration + builder
│   ├── MatchSort             search ordering modes
│   └── exceptions            public domain errors
└── core
    ├── DefaultScoreboard    validates API input, delegates operations
    ├── LiveBoardStateStore   serializes writes, publishes state
    ├── LiveBoardState        immutable set of active matches and transitions
    ├── Match / Score / TeamName  domain values and invariants
    ├── MatchCriteriaMatcher  compiles search filters into a predicate
    ├── MatchOrdering         summary and search comparators
    └── MatchResults          filtering, ordering, DTO projection
```

A caller uses `Scoreboard`; `DefaultScoreboard` applies commands to `LiveBoardStateStore`. The store holds a published `LiveBoardState`, which applies domain transitions and exposes a read-only ordered collection of active matches. `DefaultScoreboard` captures that collection from one state snapshot; `MatchResults` computes summaries and search results outside the write lock. For search, `MatchCriteriaMatcher` compiles criteria into a predicate and `MatchOrdering` supplies the selected comparator. The `core` package uses public API value types, but the public interface does not expose internal state classes.

## Design decisions and trade-offs

| Decision | Alternatives | Why this design | Cost / limitation |
| --- | --- | --- | --- |
| **UUID identity**, with team uniqueness checked separately | Composite key derived from teams | Supports repeated fixtures after a previous match finishes; identity does not depend on presentation names | Callers must retain a `MatchId` to update or finish a match |
| **`TeamName` normalization** using `strip()` and `Locale.ROOT` lowercasing | Exact string equality; comprehensive canonicalization | Small, deterministic rule for common input variations | Does not handle aliases, repeated internal spaces, or all Unicode equivalences |
| **Immutable `Match`, `Score`, and board state** | Mutable shared match objects | Old snapshots cannot be modified by subsequent state transitions | Each changing write allocates new objects and copies the map |
| **Insertion order** for start ordering | Timestamps; explicit start sequence | No clock ties or extra sequence counter | Correct tie ordering relies on ordered-map traversal and stable sorting |
| **`LiveBoardStateStore.update()` under one `ReentrantLock`** | CAS loop; concurrent map; read/write lock | Keeps validation and state transitions serialized with straightforward control flow | Writers contend on one lock; transition work takes place while it is held |
| **Whole-state publication via `volatile`** | In-place mutation with reader locks | Readers can access one consistent version without taking the write lock | Concurrent reads may observe a preceding version; snapshots are not live views |
| **Linear team scan** | Separate occupied-team index | Avoids synchronizing an additional index with every new immutable version | Team-conflict checks scan active matches |
| **Summary and search computed after obtaining a snapshot** | Filtering/sorting under a lock | Queries do not hold the store's write lock | Each query filters, sorts, and materializes its own list |
| **`MatchCriteria` builder and predicate composition** | Multiple dedicated query methods; a mutable query DTO | One extra API operation supports several optional filters with AND semantics | More public types and validation rules; predicate composition is an implementation detail |
| **Team filter with OR semantics** | Require both selected teams; separate lookup per team | Supports finding all active matches involving any named team | Does not express a both-teams fixture query or arbitrary boolean expressions |
| **Transition-and-publish, not transactions** | General-purpose transaction / rollback mechanism | Enough for the existing pure state transitions | No rollback of external side effects; `executeAtomically` and a `MatchStorage` interface do not exist in this codebase |

Ordering deserves one explicit detail: `LiveBoardState` uses a `LinkedHashMap` in insertion order. `MatchResults` traverses values in reverse insertion order and uses Java's stable sort with a comparator for **total goals only**. Equal totals therefore remain newest-first. Updating a match does not reinsert its key and does not alter its start position.

## Concurrency guarantees and boundaries

For a **single** `DefaultScoreboard` instance, each write transition is executed while holding the store's `ReentrantLock` and publishes its resulting immutable board state through a `volatile` field. The team-availability check and insertion happen within the same transition. Both `getSummary` and `findByCriteria` capture one published version and compute their results without holding the write lock.

The provided concurrency tests exercise independent concurrent starts, competing starts involving a shared team, concurrent updates to different matches, and readers observing internally consistent score pairs during updates. No dedicated concurrency test of `findByCriteria` has been confirmed. These tests cover selected scenarios; they are not a proof for every possible thread interleaving.

The implementation does **not** guarantee that all concurrent readers see the same version, that readers observe every intermediate score, that competing writers execute in arrival order, or that a late-arriving score event is newer than an earlier one. Concurrent updates of the same match follow the order in which their transitions execute; the later executed transition determines the stored score. The lock is not configured for fairness. The guarantees do not extend across separate scoreboard instances.

## Complexity (theoretical, not benchmarked)

Let `n` be the number of active matches, `t` the number of team names in the criteria, and `k` the number of matches that pass all filters (`k ≤ n`). The search estimate assumes expected constant-time set membership and accounts for the matching result list; the immutable board snapshot already exists.

| Operation | Time | Extra space | Dominant work |
| --- | --- | --- | --- |
| `startMatch` | `O(n)` | `O(n)` | Team scan and map copies |
| `updateScore` (changed score) | `O(n)` | `O(n)` | Map copies |
| `updateScore` (unchanged score) | `O(1)` expected | `O(1)` | Lookup and no-op transition |
| `finishMatch` | `O(n)` | `O(n)` | Map copies |
| `getSummary` | `O(n log n)` | `O(n)` | Sort and snapshot projection |
| `findByCriteria` | `O(t + n + k log k)` expected | `O(t + k)` | Normalize `t` team names once; scan `n` active matches; sort/project `k` matches |

These estimates describe algorithmic work and exclude lock contention, runtime scheduling, and garbage collection. No performance benchmarks are included.

## Additional operation: `findByCriteria`

Criteria-based search was chosen to make the active scoreboard queryable without changing the four required lifecycle/summary operations. It combines an immutable `MatchCriteria` builder, optional predicates, and three sort modes. Its results are snapshots, not live views, and use the same published board state as `getSummary`.

This is the **only** additional method on `Scoreboard` in the supplied updated source. The assignment's separate-commit requirement is a Git-history property and cannot be established from source code alone.

## Testing

Tests use JUnit Jupiter. The original suite covers the core behavior below; the new search tests discussed during development should be added/merged and verified in the local repository:

- Public lifecycle operations, validation, failures, and summary ordering (`DefaultScoreboardTest`).
- Immutable state transitions, including no-op score updates (`LiveBoardStateTest`).
- Sorting, filtering, and source preservation (`MatchResultsTest`).
- Criteria-based search and its filter/order combinations (`DefaultScoreboardTest`, if the search tests are merged there).
- Criteria builder defaults, validation, and immutability (`MatchCriteriaTest`, if added).
- Team-name equality, score arithmetic, and value-object validation (`ValueObjectsTest`).
- Selected concurrent start and read/write scenarios (`DefaultScoreboardConcurrencyTest`).

Run with `mvn clean verify`. This README update is based on the supplied source excerpts; the latest source tree and complete test suite have not been executed in this review environment.

## Build

```bash
mvn clean test
mvn clean package
```

The build is configured for Java 21, uses Maven Surefire for JUnit tests, and produces a JAR under `target/`. There are no declared production dependencies.

## Known limitations and deliberate omissions

- No persistence, distributed coordination, match history, or event ordering/versioning.
- No team alias resolution, full Unicode normalization, or duplicate internal whitespace normalization.
- Writes copy the active-match map and are serialized by a single lock; no throughput or latency claims are made.
- No occupied-team index or persistent collection implementation.
- No general transaction API and no rollback of external side effects.
- Criteria-based search does not include pagination, historical/finished matches, team aliases, or arbitrary nested boolean expressions.
- No automated performance measurements for criteria-based search.
- The extra-operation commit must be confirmed from the actual Git history; the README cannot certify that requirement by itself.
