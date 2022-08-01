package edu.hm.hafner.model;

public class Result {

    private final Metric metric; // LINE, BRANCH, COMPLEXITY, MUTATION, LOC
    private final int idk; // TODO

    private final boolean noResult; // TODO: ersetzen durch etwas sinnvolleres

    public Result(final Metric metric, final int idk) {
        this.metric = metric;
        this.idk = idk;

        noResult = idk < 0;
    }

    public Metric getMetric() {
        return metric;
    }

    public int getIdk() {
        return idk;
    }

    /**
     * TODO
     * Soll zurückliefern, ob eine Metrik (Coverage, Mutation, Complexity, LOC) gemessen wurde
     * @return if the metric was measured
     */
    public boolean hasResult() {
        return idk < 0;
    }

    public Result add(final Result additional) {
        return new Result(additional.getMetric(), idk + additional.getIdk());
    }
}
