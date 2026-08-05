package com.gav.binancespring;

import com.gav.binancespring.repository.TradingRepository;
import com.gav.binancespring.dto.DaysNumber;
import com.gav.binancespring.dto.TradeHistoryRow;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttributes;

import java.util.List;

@Controller
@SessionAttributes("daysNumber")
public class HomeController {

    private final TradingRepository tradingRepository;

    @Autowired
    public HomeController(TradingRepository tradingRepository) {
        this.tradingRepository = tradingRepository;
    }

    @GetMapping("/")
    public String goHome() {
        return "home";
    }

    @GetMapping("/history")
    public String showHistory(@ModelAttribute DaysNumber daysNumber, Model model) {
        List<TradeHistoryRow> history = tradingRepository.getTradeHistory(daysNumber.getNumber());
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

//    @ModelAttribute(name = "history")
//    public TradeHistory history() {
//        return new TradeHistory();
//    }

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
