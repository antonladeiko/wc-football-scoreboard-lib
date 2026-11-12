package com.sportradar.scoreboard.core;

import com.sportradar.scoreboard.api.MatchId;
import com.sportradar.scoreboard.api.MatchSnapshot;
import com.sportradar.scoreboard.api.exceptions.MatchNotFoundException;
import com.sportradar.scoreboard.api.exceptions.ScoreboardException;
import com.sportradar.scoreboard.api.exceptions.TeamAlreadyPlayingException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;

final class LiveBoardState {

    private final static LiveBoardState INITIAL = new LiveBoardState(new LinkedHashMap<>());

    private final SequencedMap<MatchId, Match> activeMatches;

    private LiveBoardState(Map<MatchId, Match> matches) {
        this.activeMatches = Collections.unmodifiableSequencedMap(
                new LinkedHashMap<>(matches)
        );
    }

    static LiveBoardState initial() {
        return INITIAL;
    }

     LiveBoardState startMatch(Match match) {
        if (activeMatches.containsKey(match.id())) {
            throw new ScoreboardException(
                    "Match already exists: " + match.id()
            );
        }

        if (isAnyTeamPlaying(match)) {
            throw new TeamAlreadyPlayingException();
        }

        var liveMatches = new LinkedHashMap<>(activeMatches);
        liveMatches.put(match.id(), match);

        return new LiveBoardState(liveMatches);
    }

     LiveBoardState updateScore(MatchId matchId, Score newScore) {
        var match = getMatchById(matchId);

        if (match.score().equals(newScore)) {
            return this;
        }

        var liveMatches = new LinkedHashMap<>(activeMatches);
        liveMatches.put(matchId, match.updateScore(newScore));

        return new LiveBoardState(liveMatches);
    }

     LiveBoardState finishMatch(MatchId matchId) {
        var finishedMatch = getMatchById(matchId);

        var liveMatches = new LinkedHashMap<>(activeMatches);
        liveMatches.remove(finishedMatch.id());

        return new LiveBoardState(liveMatches);
    }

    List<MatchSnapshot> getSummary() {
        return MatchResults.select(
            activeMatches.sequencedValues(),
            match -> true,
            MatchOrdering.summary()
        );
    }

    private boolean isAnyTeamPlaying(Match candidate) {
        return activeMatches.values()
            .stream()
            .anyMatch(match -> match.sharesTeamWith(candidate));
    }

    private Match getMatchById(MatchId matchId) {
        var match = activeMatches.get(matchId);

        if (match == null) {
            throw new MatchNotFoundException(matchId);
        }

        return match;
    }

}
