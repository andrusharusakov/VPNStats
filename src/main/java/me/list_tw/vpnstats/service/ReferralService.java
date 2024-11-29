package me.list_tw.vpnstats.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReferralService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Список всех подписок и их стоимости
    private static final Map<String, Integer> SUBSCRIPTIONS = new HashMap<>() {{
        put("VPN Lite 30", 145);
        put("VPN Lite 180", 695);
        put("VPN Lite 365", 1195);
        put("VPN Pro 30", 245);
        put("VPN Pro 180", 895);
        put("VPN Pro 365", 1745);
    }};

    public Map<String, Object> getReferralStats(long referralId) {
        // Подсчёт количества пользователей, перешедших по реферальной ссылке, но не купивших подписку
        String countInvitedQuery = "SELECT COUNT(*) FROM referrals WHERE referral_id = ? AND (subscription IS NULL OR subscription = '')";
        int invitedCount = jdbcTemplate.queryForObject(countInvitedQuery, Integer.class, referralId);

        // Подсчёт количества пользователей, которые купили подписку
        String countPurchasedQuery = "SELECT COUNT(*) FROM referrals WHERE referral_id = ? AND subscription IS NOT NULL";
        int purchasedCount = jdbcTemplate.queryForObject(countPurchasedQuery, Integer.class, referralId);

        // Подсчёт количества и стоимости подписок
        String countSubscriptionsQuery = "SELECT subscription, time FROM referrals WHERE referral_id = ? AND subscription IS NOT NULL";
        List<String> subscriptionsCount = jdbcTemplate.query(countSubscriptionsQuery, (rs, rowNum) -> {
            String subscription = rs.getString("subscription");
            int time = rs.getInt("time");
            return subscription + " " + time;
        }, referralId);

        // Инициализация данных для отображения
        int totalAmount = 0;
        Map<String, Integer> subscriptionDetails = new HashMap<>();
        subscriptionDetails.put("VPN Lite 30", 0);
        subscriptionDetails.put("VPN Lite 180", 0);
        subscriptionDetails.put("VPN Lite 365", 0);
        subscriptionDetails.put("VPN Pro 30", 0);
        subscriptionDetails.put("VPN Pro 180", 0);
        subscriptionDetails.put("VPN Pro 365", 0);

        // Подсчёт количества каждой подписки
        for (String sub : subscriptionsCount) {
            String[] parts = sub.split(" ");
            String subscription = parts[0];
            if (SUBSCRIPTIONS.containsKey(subscription)) {
                subscriptionDetails.put(subscription, subscriptionDetails.get(subscription) + 1);
                totalAmount += SUBSCRIPTIONS.get(subscription);
            }
        }

        // Подготовка результатов для отображения
        Map<String, Object> result = new HashMap<>();
        result.put("invitedCount", invitedCount);
        result.put("purchasedCount", purchasedCount);
        result.put("subscriptionDetails", subscriptionDetails);
        result.put("totalAmount", totalAmount);
        result.put("partnerShare", totalAmount * 0.5);

        return result;
    }
}
