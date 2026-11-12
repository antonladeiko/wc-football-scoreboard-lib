# Live Football World Cup Scoreboard

An in-memory Java library for tracking live football matches and retrieving a scoreboard summary. It exposes a small API for starting matches, setting their current scores, finishing matches, and listing active matches in the order required by the Sportradar assignment.

## Quick start

Requires **Java 21** and **Maven**.

```java
import com.sportradar.scoreboard.api.MatchId;
import com.sportradar.scoreboard.api.MatchSnapshot;
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

### Errors and validation

| Situation | Exception |
| --- | --- |
| A team is already in an active match (as home or away) | `TeamAlreadyPlayingException` |
| Unknown or already finished match in `updateScore` / `finishMatch` | `MatchNotFoundException` |
| Same home and away team, blank team name, or negative score | `IllegalArgumentException` |
| Null team name or match identifier passed to the public operations | `NullPointerException` |

`TeamAlreadyPlayingException` and `MatchNotFoundException` extend `ScoreboardException` (an unchecked exception). Repeating `finishMatch` is not idempotent: the second call throws `MatchNotFoundException`.

## Requirements and implementation choices

### [Assignment] Sportradar requirements

| Requirement | Implementation |
| --- | --- |
| Start a match at 0–0 | `startMatch` |
| Update the current score | `updateScore` |
| Remove finished matches from the active scoreboard | `finishMatch` |
| Summary ordered by total score descending, newest start first on ties | `getSummary` |
| Add exactly one further operation in a separate commit | **Pending:** planned `getMatch`; not part of the current public API |
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

These choices are implementation decisions, **not** additional assignment requirements.

## Assumptions

- The scoreboard is an in-memory, per-instance collection of **active** matches; there is no persistence or recovery after process restart.
- A team cannot participate in two active matches simultaneously. The same teams can play again after a match is finished.
- `updateScore` supplies the authoritative current score, including corrections; it does not require monotonically increasing values.
- A finished match is not available through the active scoreboard. No match history is retained.
- Team names are compared using `strip()` and `toLowerCase(Locale.ROOT)`. Display names retain their original letter casing after stripping outer whitespace. Internal whitespace, aliases, and Unicode normalization are not handled.
- Each `DefaultScoreboard` instance owns its own state. There is no synchronization or coordination between instances.

## Architecture

```text
com.sportradar.scoreboard
├── api
│   ├── Scoreboard            public contract
│   ├── MatchId               UUID-backed identifier
│   ├── MatchSnapshot         read-only result DTO
│   └── exceptions            public domain errors
└── core
    ├── DefaultScoreboard    validates API input, delegates operations
    ├── LiveBoardStateStore   serializes writes, publishes state
    ├── LiveBoardState        immutable set of active matches and transitions
    ├── Match / Score / TeamName  domain values and invariants
    └── MatchResults / MatchOrdering  summary projection and ordering
```

A caller uses `Scoreboard`; `DefaultScoreboard` applies commands to `LiveBoardStateStore`. The store holds a published `LiveBoardState`; that state applies domain transitions and produces summaries using the internal domain objects. The `core` package uses public API value types, but the public interface does not expose internal state classes.

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
| **Summary built after obtaining a snapshot** | Sorting under a lock | Sorting does not hold the store's write lock | Each summary request builds and sorts its own list |
| **Transition-and-publish, not transactions** | General-purpose transaction / rollback mechanism | Enough for the existing pure state transitions | No rollback of external side effects; `executeAtomically` and a `MatchStorage` interface do not exist in this codebase |

Summary ordering deserves one explicit detail: `LiveBoardState` uses a `LinkedHashMap` in insertion order. `MatchResults` traverses values in reverse insertion order and uses Java's stable sort with a comparator for **total goals only**. Equal totals therefore remain newest-first. Updating a match does not reinsert its key and does not alter its start position.

## Concurrency guarantees and boundaries

For a **single** `DefaultScoreboard` instance, each write transition is executed while holding the store's `ReentrantLock` and publishes its resulting immutable board state through a `volatile` field. The team-availability check and insertion happen within the same transition. A summary first captures one published version and then computes its result without holding the write lock.

The included concurrency tests exercise independent concurrent starts, competing starts involving a shared team, and readers observing internally consistent score pairs during updates. These tests cover selected scenarios; they are not a proof for every possible thread interleaving.

The implementation does **not** guarantee that all concurrent readers see the same version, that readers observe every intermediate score, that competing writers execute in arrival order, or that a late-arriving score event is newer than an earlier one. Concurrent updates of the same match follow the order in which their transitions execute; the later executed transition determines the stored score. The lock is not configured for fairness. The guarantees do not extend across separate scoreboard instances.

## Complexity (theoretical, not benchmarked)

Let `n` be the number of active matches.

| Operation | Time | Extra space | Dominant work |
| --- | --- | --- | --- |
| `startMatch` | `O(n)` | `O(n)` | Team scan and map copies |
| `updateScore` (changed score) | `O(n)` | `O(n)` | Map copies |
| `updateScore` (unchanged score) | `O(1)` expected | `O(1)` | Lookup and no-op transition |
| `finishMatch` | `O(n)` | `O(n)` | Map copies |
| `getSummary` | `O(n log n)` | `O(n)` | Sort and snapshot projection |

These estimates describe algorithmic work and exclude lock contention, runtime scheduling, and garbage collection. No performance benchmarks are included.

## Planned additional operation: `getMatch`

**Not implemented in this version.** The intended extension is to return the current `MatchSnapshot` for a supplied `MatchId`, complementing `getSummary` with lookup of one active match. Its final signature, error contract, tests, and separate Git commit will be documented once implemented. No existing example in this README depends on `getMatch`.

## Testing

Tests use JUnit Jupiter and cover:

- Public lifecycle operations, validation, failures, and summary ordering (`DefaultScoreboardTest`).
- Immutable state transitions, including no-op score updates (`LiveBoardStateTest`).
- Sorting, filtering, and source preservation (`MatchResultsTest`).
- Team-name equality, score arithmetic, and value-object validation (`ValueObjectsTest`).
- Selected concurrent start and read/write scenarios (`DefaultScoreboardConcurrencyTest`).

Run with `mvn test`. The production sources were checked for compilation with Java 21 during documentation review; a Maven test run was **not** verified in that review environment.

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
- The additional `getMatch` operation is pending, as is its required separate commit.
