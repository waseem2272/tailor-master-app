package com.example.tailormaster.controller;

import com.example.tailormaster.entity.ledger.CustomerPaymentLedger;
import com.example.tailormaster.service.dashboard.DashboardService;
import com.example.tailormaster.util.ThymeleafUtil;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Controller
@AllArgsConstructor
@RequestMapping("/dashboard")
public class DashboardPageController {

    private static final Logger logger =
            LogManager.getLogger(DashboardPageController.class);

    private final DashboardService dashboardService;

    @GetMapping("/revenue-details")
    public String getRevenueDetails(Model model) {
        logger.info("Requested current-month revenue details.");

        try {
            List<CustomerPaymentLedger> entries =
                    dashboardService.getRevenueEntriesThisMonth();

            BigDecimal totalRevenue = entries.stream()
                    .map(CustomerPaymentLedger::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            model.addAttribute("revenueEntries", entries);
            model.addAttribute("totalRevenue", totalRevenue);
            model.addAttribute("thymeleafUtil", new ThymeleafUtil());

            return "dashboard/revenue-details";

        } catch (Exception e) {
            logger.error("Error fetching revenue details: {}", e.getMessage(), e);
            throw e;
        }
    }
}