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
        String countPurchasedQuery = "SELECT COUNT(*) FROM referrals WHERE referral_id = ? AND subscription IS NOT NULL";
        int purchasedCount = jdbcTemplate.queryForObject(countPurchasedQuery, Integer.class, referralId);

        // Подсчёт всех подписок, которые были куплены
        String countSubscriptionsQuery = "SELECT subscription FROM referrals WHERE referral_id = ? AND subscription IS NOT NULL";
        List<String> subscriptions = jdbcTemplate.queryForList(countSubscriptionsQuery, String.class, referralId);

        // Инициализация данных для отображения
        int totalAmount = 0;
        Map<String, Integer> subscriptionDetails = new HashMap<>();
        for (String subscriptionKey : SUBSCRIPTIONS.keySet()) {
            subscriptionDetails.put(subscriptionKey, 0);  // Инициализируем все подписки как 0
        }

        // Подсчёт количества каждой подписки и добавление стоимости
        for (String subscription : subscriptions) {
            String normalizedSubscription = subscription.trim();  // Убираем лишние пробелы
            if (SUBSCRIPTIONS.containsKey(normalizedSubscription)) {
                // Увеличиваем количество этой подписки
                subscriptionDetails.put(normalizedSubscription, subscriptionDetails.get(normalizedSubscription) + 1);
                // Добавляем стоимость этой подписки к общей сумме
                totalAmount += SUBSCRIPTIONS.get(normalizedSubscription);
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
