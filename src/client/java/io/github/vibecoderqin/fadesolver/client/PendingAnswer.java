package io.github.vibecoderqin.fadesolver.client;

public final class PendingAnswer {
    private final String answer;
    private final long gameId;
    private final double delaySeconds;
    private int ticksRemaining;

    public PendingAnswer(String answer, long gameId, double delaySeconds) {
        this.answer = answer;
        this.gameId = gameId;
        this.delaySeconds = delaySeconds;
        this.ticksRemaining = (int) Math.ceil(delaySeconds * 20.0D);
    }

    public boolean tick() {
        if (ticksRemaining > 0) {
            ticksRemaining--;
        }
        return ticksRemaining == 0;
    }

    public String answer() {
        return answer;
    }

    public long gameId() {
        return gameId;
    }

    public double delaySeconds() {
        return delaySeconds;
    }
}