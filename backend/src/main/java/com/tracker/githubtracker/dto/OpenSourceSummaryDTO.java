package com.tracker.githubtracker.dto;

import java.util.List;

/** Final shape returned by /open-source-contributions */
public class OpenSourceSummaryDTO {

    private int totalCount;
    private int openCount;
    private int mergedCount;
    private int closedCount;
    private List<OpenSourceContributionDTO> contributions;

    public OpenSourceSummaryDTO() {}

    public OpenSourceSummaryDTO(int totalCount, int openCount, int mergedCount, int closedCount,
                                 List<OpenSourceContributionDTO> contributions) {
        this.totalCount = totalCount;
        this.openCount = openCount;
        this.mergedCount = mergedCount;
        this.closedCount = closedCount;
        this.contributions = contributions;
    }

    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
    public int getOpenCount() { return openCount; }
    public void setOpenCount(int openCount) { this.openCount = openCount; }
    public int getMergedCount() { return mergedCount; }
    public void setMergedCount(int mergedCount) { this.mergedCount = mergedCount; }
    public int getClosedCount() { return closedCount; }
    public void setClosedCount(int closedCount) { this.closedCount = closedCount; }
    public List<OpenSourceContributionDTO> getContributions() { return contributions; }
    public void setContributions(List<OpenSourceContributionDTO> contributions) { this.contributions = contributions; }
}
