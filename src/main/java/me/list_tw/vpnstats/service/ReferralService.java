package me.list_tw.vpnstats.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

@Service
public class ReferralService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Список всех подписок и их стоимости
    private static final Map<String, Integer> SUBSCRIPTIONS = Map.of(
            "VPN Lite 30", 145,
            "VPN Lite 180", 695,
            "VPN Lite 365", 1195,
            "VPN Pro 30", 245,
            "VPN Pro 180", 895,
            "VPN Pro 365", 1745
    );

    public Map<String, Object> getReferralStats(long referralId) {
        // Подсчёт количества пользователей, перешедших по реферальной ссылке, но не купивших подписку
        String countInvitedQuery = "SELECT COUNT(*) FROM referrals WHERE referral_id = ? AND subscription IS NULL";
        int invitedCount = jdbcTemplate.queryForObject(countInvitedQuery, Integer.class, referralId);

        // Подсчёт количества пользователей, которые купили подписку
        String countPurchasedQuery = "SELECT COUNT(*) FROM referrals WHERE referral_id = ? AND subscription IS NOT NULL";
        int purchasedCount = jdbcTemplate.queryForObject(countPurchasedQuery, Integer.class, referralId);

        // Подсчёт количества и стоимости подписок
        String countSubscriptionsQuery = "SELECT subscription, time FROM referrals WHERE referral_id = ? AND subscription IS NOT NULL";
        var subscriptionsCount = jdbcTemplate.query(countSubscriptionsQuery, (rs, rowNum) -> {
            String subscription = rs.getString("subscription");
            int time = rs.getInt("time");
            return subscription + " " + time;
        }, referralId);

        // Инициализация данных для отображения
        int totalAmount = 0;
        var subscriptionDetails = Map.of(
                "VPN Lite 30", 0,
                "VPN Lite 180", 0,
                "VPN Lite 365", 0,
                "VPN Pro 30", 0,
                "VPN Pro 180", 0,
                "VPN Pro 365", 0
        );

        for (String sub : subscriptionsCount) {
            if (SUBSCRIPTIONS.containsKey(sub)) {
                subscriptionDetails.put(sub, subscriptionDetails.get(sub) + 1);
                totalAmount += SUBSCRIPTIONS.get(sub);
            }
        }

        return Map.of(
                "invitedCount", invitedCount,
                "purchasedCount", purchasedCount,
                "subscriptionDetails", subscriptionDetails,
                "totalAmount", totalAmount,
                "partnerShare", totalAmount * 0.5
        );
    }
}