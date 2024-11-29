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
        // Подсчёт количества пользователей, перешедших по реферальной ссылке (включая тех, кто не купил подписку)
        String countInvitedQuery = "SELECT COUNT(*) FROM referrals WHERE referral_id = ?";
        int invitedCount = jdbcTemplate.queryForObject(countInvitedQuery, Integer.class, referralId);

        // Подсчёт количества пользователей, которые купили подписку
        String countPurchasedQuery = "SELECT COUNT(*) FROM referrals WHERE referral_id = ? AND subscription_type IS NOT NULL AND subscription_duration IS NOT NULL";
        int purchasedCount = jdbcTemplate.queryForObject(countPurchasedQuery, Integer.class, referralId);

        // Подсчёт всех подписок, которые были куплены - ИЗМЕНЁННЫЙ ЗАПРОС
        String countSubscriptionsQuery = "SELECT subscription_type, subscription_duration FROM referrals WHERE referral_id = ? AND subscription_type IS NOT NULL AND subscription_duration IS NOT NULL";
        List<Map<String, Object>> subscriptionsList = jdbcTemplate.queryForList(countSubscriptionsQuery, referralId);


        // Инициализация данных для отображения
        int totalAmount = 0;
        Map<String, Integer> subscriptionDetails = new HashMap<>();
        for (String subscriptionKey : SUBSCRIPTIONS.keySet()) {
            subscriptionDetails.put(subscriptionKey, 0);  // Инициализируем все подписки как 0
        }

        // Подсчёт количества каждой подписки и добавление стоимости - ИЗМЕНЁННЫЙ ЦИКЛ
        for (Map<String, Object> subscriptionData : subscriptionsList) {
            String subscriptionType = (String) subscriptionData.get("subscription_type");
            int subscriptionDuration = (int) subscriptionData.get("subscription_duration");
            String combinedSubscription = subscriptionType + " " + subscriptionDuration;

            if (SUBSCRIPTIONS.containsKey(combinedSubscription)) {
                subscriptionDetails.put(combinedSubscription, subscriptionDetails.get(combinedSubscription) + 1);
                totalAmount += SUBSCRIPTIONS.get(combinedSubscription);
            }
        }

        // Рассчитываем долю партнёра
        double partnerShare = totalAmount * 0.5;

        // Подготовка результатов для отображения
        Map<String, Object> result = new HashMap<>();
        result.put("invitedCount", invitedCount);
        result.put("purchasedCount", purchasedCount);
        result.put("subscriptionDetails", subscriptionDetails);
        result.put("totalAmount", totalAmount);
        result.put("partnerShare", partnerShare);

        return result;
    }
}