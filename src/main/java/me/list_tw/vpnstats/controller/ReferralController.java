package me.list_tw.vpnstats.controller;

import jakarta.servlet.http.HttpServletRequest;
import me.list_tw.vpnstats.service.ReferralService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ReferralController {

    @Autowired
    private ReferralService referralService;

    @GetMapping("/referral-stats")
    public String getReferralStats(@RequestParam long referralId, Model model,  HttpServletRequest request) {
        Resource resource = new ClassPathResource("/templates/referral-stats.html"); // Подкорректируйте путь при необходимости
        if (resource.exists()) {
            System.out.println("Файл шаблона существует!");
        } else {
            System.out.println("Файл шаблона НЕ существует!");
        }
        System.out.println("Context path " + request.getContextPath());
        var stats = referralService.getReferralStats(referralId);

        // Добавляем все необходимые данные в модель
        model.addAttribute("invitedCount", stats.get("invitedCount"));
        model.addAttribute("purchasedCount", stats.get("purchasedCount"));
        model.addAttribute("subscriptionDetails", stats.get("subscriptionDetails"));
        model.addAttribute("totalAmount", stats.get("totalAmount"));
        model.addAttribute("partnerShare", stats.get("partnerShare"));

        return "referral-stats"; // Имя HTML-шаблона
    }
}