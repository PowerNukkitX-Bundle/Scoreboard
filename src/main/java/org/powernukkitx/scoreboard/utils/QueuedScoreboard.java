package org.powernukkitx.scoreboard.utils;

import cn.nukkit.scoreboard.IScoreboardLine;
import cn.nukkit.scoreboard.Scoreboard;
import cn.nukkit.scoreboard.ScoreboardLine;
import cn.nukkit.scoreboard.scorer.FakeScorer;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An improved scoreboard API written for Syodo.
 * @author PleaseInsertNameHere
 */
public class QueuedScoreboard extends Scoreboard {
    protected final Set<IScoreboardLine> addingQueue;
    protected final Set<IScoreboardLine> removalQueue;

    public QueuedScoreboard(String displayName) {
        super("DUMMY", displayName);

        this.lines = new ConcurrentHashMap<>();

        this.addingQueue = new ObjectArraySet<>();
        this.removalQueue = new ObjectArraySet<>();
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
        this.resend();
    }

    @Override
    public void setLines(Collection<IScoreboardLine> lines) {
        this.lines.values().forEach(line -> this.getAllViewers().forEach(viewer -> viewer.removeLine(line)));
        this.lines.clear();
        this.addingQueue.addAll(lines);
        lines.forEach(this::updateScore);
        this.updateCache();
    }

    @Override
    public void setLines(List<String> lines) {
        this.setLines(lines.stream()
                .map(line -> this.buildLine(line, lines.indexOf(line)))
                .collect(Collectors.toList()));
    }

    public QueuedScoreboard addMany(IScoreboardLine... lines) {
        return this.addMany(List.of(lines));
    }

    public QueuedScoreboard addMany(Collection<IScoreboardLine> lines) {
        this.addingQueue.addAll(lines);
        lines.forEach(this::updateScore);
        return this.updateCache();
    }

    public QueuedScoreboard add(String text, int score) {
        return this.add(this.buildLine(text, score));
    }

    public QueuedScoreboard add(IScoreboardLine line) {
        return this.add(line, true);
    }

    protected QueuedScoreboard add(IScoreboardLine line, boolean cache) {
        this.updateScore(line);
        this.addingQueue.add(line);

        return !cache ? this : this.updateCache();
    }

    public QueuedScoreboard remove(int score) {
        return this.remove(score, true);
    }

    protected QueuedScoreboard remove(int score, boolean cache) {
        this.getLineStream(score).forEach(line -> {
            this.getAllViewers().forEach(viewer -> viewer.removeLine(line));
            this.removalQueue.add(line);
        });

        return !cache ? this : this.updateCache();
    }

    public QueuedScoreboard update(int score, String text) {
        synchronized (this) {
            this.remove(score, false);
            this.add(this.buildLine(text, score), false);
        }
        return this.updateCache();
    }

    public ScoreboardLine buildLine(String text, int score) {
        FakeScorer scorer = new FakeScorer(text);
        return new ScoreboardLine(this, scorer, score);
    }

    protected Stream<IScoreboardLine> getLineStream(int score) {
        return this.lines.values().stream().filter(l -> l.getScore() == score);
    }

    public Collection<IScoreboardLine> getLines(int score) {
        return this.getLineStream(score).collect(Collectors.toList());
    }

    public QueuedScoreboard updateCache() {
        this.removalQueue.stream()
                .map(IScoreboardLine::getScorer)
                .toList().forEach(this.lines.keySet()::remove);


        this.addingQueue.forEach(line -> this.lines.put(line.getScorer(), line));

        this.removalQueue.clear();
        this.addingQueue.clear();
        return this;
    }
}
