package component.operator.in1.aggregate;

import common.tuple.RichTuple;
import component.operator.in1.BaseOperator1In;

public abstract class TimeAggregate<IN extends RichTuple, OUT extends RichTuple> extends BaseOperator1In<IN, OUT> {
    protected final int instance;
    protected final int parallelismDegree;
    protected final long WS;
    protected final long WA;
    protected final TimeWindow w;
    protected long latestTimestamp;
    private boolean firstTuple = true;


    public TimeAggregate(String id, int instance, int parallelismDegree, long ws, long wa, TimeWindow w) {
        super(id);
        this.instance = instance;
        this.parallelismDegree = parallelismDegree;
        WS = ws;
        WA = wa;
        this.w = w;
    }

    public long getEarliestWinStartTS(long ts) {
        long winStart = (ts / WA) * WA;
        while (winStart - WA + WS > ts) {
            winStart -= WA;
        }
        return Math.max(0, winStart);
//        long contributingWins = (ts % WA < WS % WA || WS % WA == 0) ? WS_WA_ceil : WS_WA_ceil_minus_1;
//        return (long) Math.max((ts / WA - contributingWins + 1) * WA, 0.0);
    }

    @Override
    public void enable() {
        w.enable();
        super.enable();
    }

    @Override
    public void disable() {
        super.disable();
        w.disable();
    }

    @Override
    public boolean canRun() {
        return w.canRun() && super.canRun();
    }

    protected void checkIncreasingTimestamps(IN t) {
        if (firstTuple) {
            firstTuple = false;
        } else {
            if (t.getTimestamp() < latestTimestamp) {
                throw new RuntimeException("Input tuple's timestamp decreased!");
            }
        }
    }
}
