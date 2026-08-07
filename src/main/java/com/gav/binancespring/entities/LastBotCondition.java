package com.gav.binancespring.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "last_bot_condition")
public class LastBotCondition {
    @Id
    private Integer id;

    @Column(name = "last_max_price")
    private BigDecimal lastMaxPrice;

    @Column(name = "last_min_price")
    private BigDecimal lastMinPrice;

    @Column(name = "up_move_diff_level")
    private BigDecimal upMoveDiffLevel;

    @Column(name = "down_move_diff_level")
    private BigDecimal downMoveDiffLevel;

    @Column(name = "transaction_count")
    private Long transactionCount;
}
