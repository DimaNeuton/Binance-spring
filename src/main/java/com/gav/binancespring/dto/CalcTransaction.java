package com.gav.binancespring.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record CalcTransaction (
        Long id,
        BigDecimal price,
        BigDecimal volume,
        BigDecimal commission
)  implements Comparable<CalcTransaction> {

    @Override
    public int compareTo(CalcTransaction other) {
        int cmp = this.price.compareTo(other.price);
        if (cmp != 0) return cmp;
        return Long.compare(this.id, other.id);
    }
}
