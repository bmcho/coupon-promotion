package com.bmcho.pointservicebatch.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DailyPointSummary {
    private Long userId;
    private Long earnAmount;
    private Long useAmount;
    private Long cancelAmount;

    @Builder
    public DailyPointSummary(Long userId, Long earnAmount, Long useAmount, Long cancelAmount) {
        this.userId = userId;
        this.earnAmount = earnAmount == null ? 0L : earnAmount;
        this.useAmount = useAmount == null ? 0L : useAmount;
        this.cancelAmount = cancelAmount == null ? 0L : cancelAmount;
    }

    public void addEarnAmount(Long amount) {
        this.earnAmount += amount;
    }

    public void addUseAmount(Long amount) {
        this.useAmount += amount;
    }

    public void addCancelAmount(Long amount) {
        this.cancelAmount += amount;
    }
}