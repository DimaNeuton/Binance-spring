package com.gav.binancespring;

import com.gav.binancespring.entities.Balance;
import com.gav.binancespring.entities.Transaction;
import com.gav.binancespring.repository.BalanceRepository;
import com.gav.binancespring.dto.DaysNumber;

import com.gav.binancespring.repository.TransactionRepository;
import com.gav.binancespring.services.BinanceAccountService;
import com.gav.binancespring.services.OrderStreamService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@SessionAttributes("daysNumber")
public class HomeController {

    private final TransactionRepository transactionRepository;
    private final BalanceRepository balanceRepository;
    private final BinanceAccountService binanceAccountService;
    private final OrderStreamService orderStreamService;

    @Autowired
    public HomeController(TransactionRepository transactionRepository,
                          BalanceRepository balanceRepository,
                          BinanceAccountService binanceAccountService,
                          OrderStreamService orderStreamService) {
        this.transactionRepository = transactionRepository;
        this.balanceRepository = balanceRepository;
        this.binanceAccountService = binanceAccountService;
        this.orderStreamService = orderStreamService;
    }

    @GetMapping("/")
    public String goHome(Model model) {
        Balance balanceDB = balanceRepository.findById(1).orElse(null);
        Balance balanceReal = binanceAccountService.fetchWalletBalance();
        model.addAttribute("balanceDB", balanceDB);
        model.addAttribute("balanceReal", balanceReal);
        return "home";
    }

    @GetMapping("/history")
    public String showHistory(@ModelAttribute DaysNumber daysNumber, Model model) {
        LocalDateTime fromDate = LocalDateTime.now().minusDays(daysNumber.getNumber());
        List<Transaction> history = transactionRepository.findByTimeAfter(fromDate);
        model.addAttribute("history", history);
        return "history";
    }

    @PostMapping("/")
    public String processHistory(@Valid @ModelAttribute DaysNumber daysNumber, Errors errors) {
        if (errors.hasErrors()) {
            return "home";
        }

        return "redirect:/history";
    }

    @PostMapping("/order")
    public String placeOrder(@RequestParam String side,
                             @RequestParam BigDecimal quantity,
                             RedirectAttributes redirectAttributes) {
        try {
            orderStreamService.placeMarketOrder(side, quantity);
            redirectAttributes.addFlashAttribute(
                    "orderMessage",
                    "Ордер " + side + " на " + quantity.toPlainString() + " BTC отправлен. Ответ — в логе приложения.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("orderError", e.getMessage());
        }
        return "redirect:/";
    }

    @ModelAttribute(name = "daysNumber")
    public DaysNumber daysNumber() {
        return new DaysNumber();
    }

    @ModelAttribute
    public void addApplicationCondition(Model model) {
        model.addAttribute(
                "appCondition",
                null
        );
    }
}
