package cc.aguesuka.btfind.dht.model;

import cc.aguesuka.btfind.command.ForShell;

import java.util.StringJoiner;

public class DhtNodeRecord {
    private long createTime = System.currentTimeMillis();
    private long lastQueryTime = 0;
    private long lastResponseTime = 0;
    private int queryCount = 0;
    private int responseCount = 0;
    private int recentTimeoutCount = 0;
    private int recentSuccessCount = 0;
    private long lastDelay = 0;
    private long weightedDelay = 0;

    DhtNodeRecord() {

    }


    public long getCreateTime() {
        return createTime;
    }

    public long getLastQueryTime() {
        return lastQueryTime;
    }

    public long getLastResponseTime() {
        return lastResponseTime;
    }

    @ForShell
    public int getQueryCount() {
        return queryCount;
    }

    @ForShell
    public int getResponseCount() {
        return responseCount;
    }

    public int getRecentTimeoutCount() {
        return recentTimeoutCount;
    }

    public int getRecentSuccessCount() {
        return recentSuccessCount;
    }

    @ForShell
    public long getLastDelay() {
        return lastDelay;
    }

    @ForShell
    public long getWeightedDelay() {
        return weightedDelay;
    }

    public void onTimeout() {
        recentTimeoutCount++;
        recentSuccessCount = 0;
    }

    public void onQuery() {
        lastQueryTime = System.currentTimeMillis();
        queryCount++;
    }

    public void onResponse() {
        lastResponseTime = System.currentTimeMillis();
        responseCount++;
        recentTimeoutCount = 0;
        recentSuccessCount++;
        lastDelay = lastResponseTime - lastQueryTime;
        weightedDelay = 0L == weightedDelay ? lastDelay : (weightedDelay + lastDelay) / 2;
    }


    @Override
    public String toString() {
        return new StringJoiner(", ", DhtNodeRecord.class.getSimpleName() + "[", "]")
                .add("createTime=" + createTime)
                .add("lastQueryTime=" + lastQueryTime)
                .add("lastResponseTime=" + lastResponseTime)
                .add("queryCount=" + queryCount)
                .add("responseCount=" + responseCount)
                .add("recentTimeoutCount=" + recentTimeoutCount)
                .add("recentSuccessCount=" + recentSuccessCount)
                .add("lastDelay=" + lastDelay)
                .add("weightedDelay=" + weightedDelay)
                .toString();
    }

}