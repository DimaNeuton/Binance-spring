package com.gav.binancespring;

import com.gav.binancespring.entities.Balance;
import com.gav.binancespring.entities.Transaction;
import com.gav.binancespring.repository.BalanceRepository;
import com.gav.binancespring.dto.DaysNumber;

import com.gav.binancespring.repository.TransactionRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@SessionAttributes("daysNumber")
public class HomeController {

    private final TransactionRepository transactionRepository;
    private final BalanceRepository balanceRepository;

    @Autowired
    public HomeController(TransactionRepository transactionRepository,
                          BalanceRepository balanceRepository) {
        this.transactionRepository = transactionRepository;
        this.balanceRepository = balanceRepository;
    }

    @GetMapping("/")
    public String goHome(Model model) {
        Balance balance = balanceRepository.findById(1).orElse(null);
        model.addAttribute("balance", balance);
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
