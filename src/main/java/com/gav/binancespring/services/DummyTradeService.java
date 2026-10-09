package com.gav.binancespring.services;

import com.gav.binancespring.dto.CalcTransaction;
import com.gav.binancespring.entities.Balance;
import com.gav.binancespring.entities.LastBotCondition;
import com.gav.binancespring.entities.Transaction;
import com.gav.binancespring.exceptions.OrderUnknownException;
import com.gav.binancespring.repository.BalanceRepository;
import com.gav.binancespring.repository.LastBotConditionRepository;
import com.gav.binancespring.repository.TransactionRepository;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.TreeSet;

@Slf4j
@Service
public class DummyTradeService implements TradeService {

    private final int STATE_ID = 1;
    @Getter
    private final Balance balanceDb;
    private final LastBotCondition lastBotCondition;
    private final CurrentStatusHolder currentStatusHolder;

    private final TransactionRepository transactionRepository;
    private final BalanceRepository balanceRepository;
    private final LastBotConditionRepository lastBotConditionRepository;
    private final OrderStreamService orderStreamService;
    private final BinanceAccountService binanceAccountService;
    private final CalculationService calculationService;

    private final BigDecimal commissionRatio = new BigDecimal("0.001");
    private final BigDecimal volume = new BigDecimal("0.00007");

    private BigDecimal previousBtcBuyPrice = BigDecimal.valueOf(1000_000);
    private BigDecimal previousBtcSellPrice = BigDecimal.ZERO;

    TreeSet<CalcTransaction> calcSales = new TreeSet<>(Collections.reverseOrder());
    TreeSet<CalcTransaction> calcPurchases = new TreeSet<>();
    TreeSet<CalcTransaction> profitCalcSales = new TreeSet<>(Collections.reverseOrder());
    TreeSet<CalcTransaction> profitCalcPurchases = new TreeSet<>(Collections.reverseOrder());
    BigDecimal totalCommission;
    BigDecimal totalSalesVolume;
    BigDecimal totalPurchasesVolume;

    @Autowired
    public DummyTradeService(CurrentStatusHolder currentStatusHolder, TransactionRepository transactionRepository,
                             BalanceRepository balanceRepository,
                             LastBotConditionRepository lastBotConditionRepository,
                             OrderStreamService orderStreamService,
                             BinanceAccountService binanceAccountService,
                             CalculationService calculationService) {
        this.currentStatusHolder = currentStatusHolder;
        this.calculationService = calculationService;
        this.binanceAccountService = binanceAccountService;
        this.transactionRepository = transactionRepository;
        transactionRepository.findIdAndTimeAndPriceAndVolumeByType("SELL")
                .forEach(transaction -> calcSales.add(CalcTransaction.builder()
                        .price(transaction.price())
                        .volume(transaction.volume())
                        .commission(transaction.commission())
                        .build()));
        transactionRepository.findIdAndTimeAndPriceAndVolumeByType("BUY")
                .forEach(transaction -> calcPurchases.add(CalcTransaction.builder()
                        .price(transaction.price())
                        .volume(transaction.volume())
                        .commission(transaction.commission())
                        .build()));
        totalCommission = calculationService.getTotalCommission(calcSales, calcPurchases);
        totalSalesVolume = calculationService.getTotaVolume(calcSales);
        totalPurchasesVolume = calculationService.getTotaVolume(calcPurchases);
        this.balanceRepository = balanceRepository;
        this.lastBotConditionRepository = lastBotConditionRepository;
        this.lastBotCondition = lastBotConditionRepository.findById(STATE_ID).orElse(LastBotCondition.builder()
                .id(STATE_ID)
                .lastMaxPrice(null)
                .lastMinPrice(null)
                .downMoveDiffLevel(null)
                .upMoveDiffLevel(null)
                .transactionCount(0L)
                .profit(null)
                .salesVolume(BigDecimal.ZERO)
                .purchasesVolume(BigDecimal.ZERO)
                .build());
        this.lastBotCondition.setTransactionCount(1L);
//        this.balanceDb = balanceRepository.findById(STATE_ID).orElse(binanceAccountService.fetchWalletBalance());
        this.balanceDb = balanceRepository.findById(STATE_ID).orElse(Balance.builder()
                        .btc(BigDecimal.valueOf(0.0117647))
                        .usdt(BigDecimal.valueOf(1000))
                        .bnb(BigDecimal.ONE)
                .build());
        balanceDb.setId(STATE_ID);
        this.orderStreamService = orderStreamService;
    }

    @Override
    public void analyse(BigDecimal btcSellPrice, BigDecimal btcBuyPrice, Long tickerTime, BigDecimal bnbPrice) {

        try {
            if (btcSellPrice == null || btcBuyPrice == null || bnbPrice == null) {
                log.info("btcSell - {}\tbtcBuy - {}\tbnb - {}", btcSellPrice, btcBuyPrice, bnbPrice);
                return;
            }

            // Проверка на манипуляцию
            if (btcBuyPrice.compareTo(previousBtcBuyPrice.subtract(BigDecimal.valueOf(100))) < 0 ||
                    btcSellPrice.compareTo(previousBtcSellPrice.add(BigDecimal.valueOf(100))) > 0) {
                previousBtcBuyPrice = btcBuyPrice;
                previousBtcSellPrice = btcSellPrice;
                return;
            }
            previousBtcBuyPrice = btcBuyPrice;
            previousBtcSellPrice = btcSellPrice;

            if (lastBotCondition.getLastMaxPrice() == null || lastBotCondition.getLastMinPrice() == null)
//                trade("SELL", btcSellPrice, tickerTime, bnbPrice);
                tradeOnlyDB("SELL", btcSellPrice, tickerTime, bnbPrice);
            else {
                if (btcSellPrice.compareTo(lastBotCondition.getLastMaxPrice().add(BigDecimal.valueOf(175))) > 0) {
//                    trade("SELL", btcSellPrice, tickerTime, bnbPrice);
                    tradeOnlyDB("SELL", btcSellPrice, tickerTime, bnbPrice);
                }
                if (btcBuyPrice.compareTo(lastBotCondition.getLastMinPrice().subtract(BigDecimal.valueOf(175))) < 0) {
//                    trade("BUY", btcBuyPrice, tickerTime, bnbPrice);
                    tradeOnlyDB("BUY", btcBuyPrice, tickerTime, bnbPrice);
                }
            }
        } catch (Exception e) {
            log.error("analyse error", e);
        }
    }

    @Override
    public void trade(String type, BigDecimal tickerPrice, Long tickerTime, BigDecimal bnbPrice) {
        try {
            var orderResult = orderStreamService.placeMarketOrder(type, volume);

            Long responseTime = orderResult.transactTime();
            BigDecimal responseBtcPrice = orderResult.price().setScale(2, RoundingMode.HALF_UP);

//        commission = ((responseBtcPrice.multiply(volume)).multiply(commissionRatio)).divide(bnbPrice, 8, RoundingMode.HALF_DOWN);

            BigDecimal commission = (responseBtcPrice.multiply(volume)).multiply(commissionRatio).setScale(8, RoundingMode.HALF_DOWN);
            if (type.equals("SELL")) {
                balanceDb.setBtc(balanceDb.getBtc().subtract(volume));
                balanceDb.setUsdt((balanceDb.getUsdt().add(responseBtcPrice.multiply(volume))).subtract(commission).setScale(8, RoundingMode.FLOOR));
            } else {
                balanceDb.setBtc(balanceDb.getBtc().add(volume).subtract(volume.multiply(commissionRatio)).setScale(8, RoundingMode.FLOOR));
                balanceDb.setUsdt(balanceDb.getUsdt().subtract(responseBtcPrice.multiply(volume)).setScale(8, RoundingMode.FLOOR));
            }

//        balance.setBnb(balance.getBnb().subtract(commission));

            Balance balanceReal = binanceAccountService.fetchWalletBalance();

            updateData(type, tickerPrice, tickerTime, responseTime, responseBtcPrice, commission, balanceReal);

            log.info("Test " + type + "\t\tbtcBalance: " + balanceDb.getBtc() + " - " + balanceReal.getBtc() +
                    "\t\tusdtBalance: " + balanceDb.getUsdt() + " - " + balanceReal.getUsdt() +
                    "\t\tcommission: " + commission + " - " + (type.equals("SELL") ? orderResult.commission() :
                    orderResult.commission().multiply(responseBtcPrice).setScale(8, RoundingMode.CEILING)));
//            log.info("last " + type + " " + orderResult.price());
        } catch (OrderUnknownException e) {
            log.warn("Ответ по ордеру потерян. Сверяем через RSET: {}", e.getClientOrderId());
            reconcileUnknownOrder(e.getClientOrderId(), type, tickerPrice, tickerTime);
        } catch (Exception e) {
            log.error("Error in trade " + type, e);
        }
    }

    public void tradeOnlyDB(String type, BigDecimal tickerPrice, Long tickerTime, BigDecimal bnbPrice) {

        try {
            Long responseTime = Instant.now().toEpochMilli();
            BigDecimal responseBtcPrice = tickerPrice;

            BigDecimal commission = (responseBtcPrice.multiply(volume)).multiply(commissionRatio).setScale(8, RoundingMode.HALF_DOWN);
            if (type.equals("SELL")) {
                balanceDb.setBtc(balanceDb.getBtc().subtract(volume));
                balanceDb.setUsdt((balanceDb.getUsdt().add(responseBtcPrice.multiply(volume))).subtract(commission).setScale(8, RoundingMode.FLOOR));
            } else {
                balanceDb.setBtc(balanceDb.getBtc().add(volume).subtract(volume.multiply(commissionRatio)).setScale(8, RoundingMode.FLOOR));
                balanceDb.setUsdt(balanceDb.getUsdt().subtract(responseBtcPrice.multiply(volume)).setScale(8, RoundingMode.FLOOR));
            }

            Balance balanceReal = balanceDb;

            updateData(type, tickerPrice, tickerTime, responseTime, responseBtcPrice, commission, balanceReal);

        } catch (Exception e) {
            log.error("Error in trade " + type, e);
        }
    }

    @Transactional
    public void updateData(String type, BigDecimal tickerPrice, Long tickerTime, Long responseTime, BigDecimal responseBtcPrice, BigDecimal commission, Balance balanceReal) {

        try {
            lastBotCondition.setLastMaxPrice(responseBtcPrice);
            lastBotCondition.setLastMinPrice(responseBtcPrice);
            lastBotCondition.setTransactionCount(lastBotCondition.getTransactionCount() + 1);

            // считаем профит
            if (type.equals("SELL")) {
                calcSales.add(CalcTransaction.builder()
                        .id(currentStatusHolder.get().totalTransactions() + 1)
                        .price(responseBtcPrice)
                        .volume(volume)
                        .build());
                totalSalesVolume = totalSalesVolume.add(volume);
            } else {
                calcPurchases.add(CalcTransaction.builder()
                        .id(currentStatusHolder.get().totalTransactions() + 1)
                        .price(responseBtcPrice)
                        .volume(volume)
                        .build());
                totalPurchasesVolume = totalPurchasesVolume.add(volume);
            }
            totalCommission = totalCommission.add(commission);

            BigDecimal profit = defineProfit();

            // сохраняем транзакцию
            Transaction savedTransaction = transactionRepository.save(Transaction.builder()
                    .time(Instant.ofEpochMilli(responseTime)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime())
                    .type(type)
                    .price(responseBtcPrice)
                    .volume(volume)
                    .cumulativeVolume(responseBtcPrice.multiply(volume).setScale(8, RoundingMode.CEILING))
                    .commission(commission)
                    .btc(balanceDb.getBtc())
                    .usdt(balanceDb.getUsdt())
                    .bnb(balanceDb.getBnb())
                    .profit(profit)
                    .priceSlippage(type.equals("SELL") ? responseBtcPrice.subtract(tickerPrice) : tickerPrice.subtract(responseBtcPrice))
                    .timeSlippage(responseTime - tickerTime)
                    .build());

            currentStatusHolder.update(
                    profit,
                    (long) (profitCalcPurchases.size() + profitCalcSales.size()),
                    (long) calcPurchases.size() + calcSales.size(),
                    totalPurchasesVolume.add(totalSalesVolume),
                    balanceReal.getBtc(),
                    balanceReal.getUsdt(),
                    balanceReal.getBnb(),
                    balanceDb.getBtc(),
                    balanceDb.getUsdt(),
                    balanceDb.getBnb(),
                    savedTransaction);

            // сохраняем баланс
            balanceRepository.save(balanceDb);

            // сохраняем параметры бота
            lastBotConditionRepository.save(lastBotCondition);
        } catch (Exception e) {
            log.error("updateData Exception: " + e.getMessage());
        }
    }

    public BigDecimal defineProfit() {
        try {
            BigDecimal totalReceived = BigDecimal.ZERO;
            BigDecimal totalSpent = BigDecimal.ZERO;
            profitCalcPurchases = new TreeSet<>(Collections.reverseOrder());
            profitCalcSales = new TreeSet<>(Collections.reverseOrder());
            BigDecimal salesVolume;
            BigDecimal purchasesVolume;

            if (calcSales.isEmpty() || calcPurchases.isEmpty()) return BigDecimal.ZERO;
            else {
                if (totalSalesVolume.compareTo(totalPurchasesVolume) < 0) {
//            currentSalesWAP = salesWAP;
//            profitVolume = totalSalesVolume;
                    salesVolume = totalSalesVolume;
                    purchasesVolume = BigDecimal.ZERO;
                    for (CalcTransaction purchase : calcPurchases) {
                        if (purchasesVolume.add(purchase.volume()).compareTo(salesVolume) < 0) {
                            purchasesVolume = purchasesVolume.add(purchase.volume());
                            totalSpent = totalSpent.add((purchase.volume().multiply(purchase.price())).setScale(8, RoundingMode.CEILING));
                            profitCalcPurchases.add(purchase);
//                    currentPurchasesWAP = totalSpent.divide(purchasesVolume, 2, RoundingMode.FLOOR);
                        } else if (purchasesVolume.add(purchase.volume()).compareTo(salesVolume) > 0) {
                            totalSpent = totalSpent.add((purchase.price().multiply(salesVolume.subtract(purchasesVolume))).setScale(8, RoundingMode.CEILING));
                            profitCalcPurchases.add(CalcTransaction.builder()
                                    .id(purchase.id())
                                    .price(purchase.price())
                                    .volume(salesVolume.subtract(purchasesVolume))
                                    .build());
//                    try {
//                        currentPurchasesWAP = totalSpent.divide(salesVolume, 2, RoundingMode.FLOOR);
//                    } catch (ArithmeticException e) {
//                        System.out.println("salesVolume" + salesVolume + "\tПропускаем");
//                    }
                            break;
                        } else {
                            totalSpent = totalSpent.add((purchase.volume().multiply(purchase.price())).setScale(8, RoundingMode.CEILING));
                            profitCalcPurchases.add(purchase);
//                    try {
//                        currentPurchasesWAP = totalSpent.divide(salesVolume, 2, RoundingMode.FLOOR);
//                    } catch (ArithmeticException e) {
//                        System.out.println("salesVolume" + salesVolume + "\tПропускаем");
//                    }
                            break;
                        }
                    }
                    for (CalcTransaction sale : calcSales) {
                        profitCalcSales.add(sale);
                        totalReceived = totalReceived.add((sale.price().multiply(sale.volume())).setScale(8, RoundingMode.CEILING));
                    }
                } else if (totalSalesVolume.compareTo(totalPurchasesVolume) > 0) {
//            currentPurchasesWAP = purchasesWAP;
//            profitVolume = totalPurchasesVolume;
                    salesVolume = BigDecimal.ZERO;
                    purchasesVolume = totalPurchasesVolume;
                    for (CalcTransaction sale : calcSales) {
                        if (salesVolume.add(sale.volume()).compareTo(purchasesVolume) < 0) {
                            salesVolume = salesVolume.add(sale.volume());
                            totalReceived = totalReceived.add((sale.volume().multiply(sale.price())).setScale(8, RoundingMode.CEILING));
                            profitCalcSales.add(sale);
//                    currentSalesWAP = totalReceived.divide(salesVolume, 2, RoundingMode.FLOOR);
                        } else if (salesVolume.add(sale.volume()).compareTo(purchasesVolume) > 0) {
                            totalReceived = totalReceived.add((sale.price().multiply(purchasesVolume.subtract(salesVolume))).setScale(8, RoundingMode.CEILING));
                            profitCalcSales.add(CalcTransaction.builder()
                                    .id(sale.id())
                                    .price(sale.price())
                                    .volume(salesVolume.subtract(purchasesVolume))
                                    .build());
//                    try {
//                        currentSalesWAP = totalReceived.divide(purchasesVolume, 2, RoundingMode.FLOOR);
//                    } catch (ArithmeticException e) {
//                        System.out.println("purchasesVolume" + purchasesVolume + "\tПропускаем");
//                    }
                            break;
                        } else {
                            totalReceived = totalReceived.add((sale.volume().multiply(sale.price())).setScale(8, RoundingMode.CEILING));
                            profitCalcSales.add(sale);
//                    try {
//                        currentSalesWAP = totalReceived.divide(purchasesVolume, 2, RoundingMode.FLOOR);
//                    } catch (ArithmeticException e) {
//                        System.out.println("purchasesVolume" + purchasesVolume + "\tПропускаем");
//                    }
                            break;
                        }
                    }
                    for (CalcTransaction purchase : calcPurchases) {
                        profitCalcPurchases.add(purchase);
                        totalSpent = totalSpent.add((purchase.volume().multiply(purchase.price())).setScale(8, RoundingMode.CEILING));
                    }
                } else {
//            profitVolume = totalPurchasesVolume;
//            currentPurchasesWAP = purchasesWAP;
//            currentSalesWAP = salesWAP;
                    for (CalcTransaction purchase : calcPurchases) {
                        profitCalcPurchases.add(purchase);
                        totalSpent = totalSpent.add((purchase.volume().multiply(purchase.price())).setScale(8, RoundingMode.CEILING));
                    }
                    for (CalcTransaction sale : calcSales) {
                        profitCalcSales.add(sale);
                        totalReceived = totalReceived.add((sale.price().multiply(sale.volume())).setScale(8, RoundingMode.CEILING));
                    }
                }
                BigDecimal profit = (totalReceived.subtract(totalSpent)).subtract(totalCommission).setScale(3, RoundingMode.HALF_UP);
                return profit;
            }
        } catch (Exception ex) {
            log.error("error occurred while processing purchases", ex);
        }
        return null;
    }

    public TreeSet<CalcTransaction> getProfitTransactions(String type) {
        return type.equals("SELL") ? profitCalcSales : profitCalcPurchases;
    }

    private void reconcileUnknownOrder(String clientOrderId, String type,
                                       BigDecimal tickerPrice, Long tickerTime) {
        try {
            var status = binanceAccountService.queryOrderByClientId(clientOrderId);

            if ("FILLED".equals(status.status())) {
                // Ордер исполнился! Восстанавливаем данные
                BigDecimal executedQty = new BigDecimal(String.valueOf(status.executedQty()));
                BigDecimal quoteQty = new BigDecimal(String.valueOf(status.cummulativeQuoteQty()));

                // Заполняем responseTime/responseBtcPrice и вызываем updateData
                Long responseTime = status.updateTime();
                BigDecimal responseBtcPrice = quoteQty.divide(executedQty, 2, RoundingMode.HALF_DOWN);

                BigDecimal commission = (responseBtcPrice.multiply(volume)).multiply(commissionRatio).setScale(8, RoundingMode.HALF_DOWN);
                if (type.equals("SELL")) {
                    balanceDb.setBtc(balanceDb.getBtc().subtract(volume));
                    balanceDb.setUsdt((balanceDb.getUsdt().add(responseBtcPrice.multiply(volume))).subtract(commission).setScale(8, RoundingMode.FLOOR));
                } else {
                    balanceDb.setBtc(balanceDb.getBtc().add(volume).subtract(volume.multiply(commissionRatio)).setScale(8, RoundingMode.FLOOR));
                    balanceDb.setUsdt(balanceDb.getUsdt().subtract(responseBtcPrice.multiply(volume)).setScale(8, RoundingMode.FLOOR));
                }

                Balance balanceReal = binanceAccountService.fetchWalletBalance();

                // Обновляем баланс, сохраняем транзакцию, профит
                updateData(type, tickerPrice, tickerTime, responseTime, responseBtcPrice, commission, balanceReal);

                log.info("Test " + type + "\t\tbtcBalance: " + balanceDb.getBtc() + " - " + balanceReal.getBtc() +
                        "\t\tusdtBalance: " + balanceDb.getUsdt() + " - " + balanceReal.getUsdt() +
                        "\t\tcommission: " + commission + " - " + status.totalCommission());
                log.info("Восстановлен исполненный ордер: {}", clientOrderId);

            } else if ("NEW".equals(status.status()) || "PARTIALLY_FILLED".equals(status.status())) {
                // Ордер на бирже, но не исполнен полностью — отменяем или ждём
                log.warn("Ордер {} в статусе {}, требуется ручное вмешательство",
                        clientOrderId, status.status());
                // Можно вызвать cancelOrder(clientOrderId)

            } else if ("CANCELED".equals(status.status()) || "EXPIRED".equals(status.status())) {
                // Ордер отменён/истёк — ничего не делаем, просто логируем
                log.info("Ордер {} не исполнился: {}", clientOrderId, status.status());

            } else if ("REJECTED".equals(status.status())) {
                log.error("Ордер {} отклонён биржей", clientOrderId);
            }

        } catch (Exception e) {
            log.error("Не удалось выяснить судьбу ордера {}. Требуется ручная проверка!",
                    clientOrderId, e);
            // Здесь можно отправить алерт (телеграм, email), чтобы вы вручную проверили
        }
    }

}
