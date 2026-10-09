package com.gav.binancespring.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private LocalDateTime time;
    private String type;
    private BigDecimal price;
    private BigDecimal volume;

    @Column(name = "cumulative_volume")
    private BigDecimal cumulativeVolume;
    private BigDecimal commission;
    private BigDecimal btc;
    private BigDecimal usdt;
    private BigDecimal bnb;
//
//    @Column(name = "profit_taking")
//    private boolean profitTaking;

    //@Formula()
    private BigDecimal profit;

    @Column(name = "price_slippage")
    private BigDecimal priceSlippage;

    @Column(name = "time_slippage")
    private Long timeSlippage;
}
