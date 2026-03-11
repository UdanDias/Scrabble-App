package lk.kelaniya.uok.scrabble.scrabbleapp.util;

public class EloCalculator {

    public static final double DEFAULT_RATING = 1200.0;
    public static final int K = 12;
    public static final int K_PROVISIONAL = 24;        // 2x faster for new players
    public static final int PROVISIONAL_THRESHOLD = 3;
    public static final double ABSENCE_PENALTY = 20.0;// games until "established"

    private EloCalculator() {}

    /**
     * Standard game between two players.
     * @param ratingA       current rating of player A
     * @param ratingB       current rating of player B
     * @param scoreA        1.0 = A won, 0.0 = A lost, 0.5 = tie
     * @param scoreDiff     absolute score difference (e.g. 450-380 → 70)
     * @param gamesPlayedA  total games played by A (for provisional check)
     * @param gamesPlayedB  total games played by B (for provisional check)
     * @return double[]{newRatingA, newRatingB}
     */
    public static double[] calculate(double ratingA, double ratingB,
                                     double scoreA, int scoreDiff,
                                     int gamesPlayedA, int gamesPlayedB) {
        double scoreB = 1.0 - scoreA;

        double expectedA = expected(ratingA, ratingB);
        double expectedB = 1.0 - expectedA;

        double ratingDiff = ratingA - ratingB;
        double multiplier = marginMultiplier(scoreDiff, ratingDiff);

        // ── Use higher K for provisional players ──────────────────────────────
        double kA = isProvisional(gamesPlayedA) ? K_PROVISIONAL : K;
        double kB = isProvisional(gamesPlayedB) ? K_PROVISIONAL : K;

        double newA = ratingA + kA * multiplier * (scoreA - expectedA);
        double newB = ratingB + kB * multiplier * (scoreB - expectedB);

        return new double[]{newA, newB};
    }

    /**
     * Bye game — win vs same-rated ghost.
     * @param rating        current rating
     * @param gamesPlayed   total games played (for provisional check)
     */
    public static double calculateBye(double rating, int gamesPlayed) {
        double k = isProvisional(gamesPlayed) ? K_PROVISIONAL : K;
        return rating + k * (1.0 - 0.5);
    }

    // ── Provisional check ─────────────────────────────────────────────────────
    public static boolean isProvisional(int gamesPlayed) {
        return gamesPlayed < PROVISIONAL_THRESHOLD;
    }

    // ── Expected score (standard Elo) ─────────────────────────────────────────
    private static double expected(double ratingA, double ratingB) {
        return 1.0 / (1.0 + Math.pow(10.0, (ratingB - ratingA) / 400.0));
    }

    // ── Margin multiplier (log scale, rating-diff adjusted) ───────────────────
    private static double marginMultiplier(int scoreDiff, double ratingDiff) {
        int margin = Math.abs(scoreDiff);
        if (margin <= 1) return 1.0;
        return Math.log(margin + 1) * (2.2 / ((ratingDiff * 0.001) + 2.2));
    }
}