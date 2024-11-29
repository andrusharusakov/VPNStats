package me.list_tw.vpnstats.service;

import org.springframework.dao.EmptyResultDataAccessException;
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

    private static final Map<String, Integer> SUBSCRIPTIONS = Map.of(
            "VPN Lite 30", 145,
            "VPN Lite 180", 695,
            "VPN Lite 365", 1195,
            "VPN Pro 30", 245,
            "VPN Pro 180", 895,
            "VPN Pro 365", 1745
    );


    public Map<String, Object> getReferralStats(long referralId) {
        Map<String, Object> result = new HashMap<>();
        result.put("invitedCount", 0);
        result.put("purchasedCount", 0);
        result.put("subscriptionDetails", new HashMap<String, Integer>());
        result.put("totalAmount", 0);
        result.put("partnerShare", 0);

        try {
            // Используем один запрос для эффективности
            String sql = """
                    SELECT
                        COUNT(*) AS invited_count,
                        SUM(CASE WHEN subscription IS NOT NULL THEN 1 ELSE 0 END) AS purchased_count,
                        subscription,
                        COUNT(*) OVER (PARTITION BY subscription) AS subscription_count
                    FROM referrals
                    WHERE referral_id = ?
                    GROUP BY subscription
                    """;

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, referralId);

            if (!rows.isEmpty()) {
                Map<String, Integer> subscriptionDetails = new HashMap<>();
                int totalAmount = 0;
                int invitedCount = (int) rows.get(0).get("invited_count");
                int purchasedCount = (int) rows.get(0).get("purchased_count");


                for (Map<String, Object> row : rows) {
                    String subscription = (String) row.get("subscription");
                    if (subscription != null && SUBSCRIPTIONS.containsKey(subscription)) {
                        int count = (int) row.get("subscription_count");
                        subscriptionDetails.put(subscription, count);
                        totalAmount += count * SUBSCRIPTIONS.get(subscription);
                    }
                }

                result.put("invitedCount", invitedCount);
                result.put("purchasedCount", purchasedCount);
                result.put("subscriptionDetails", subscriptionDetails);
                result.put("totalAmount", totalAmount);
                result.put("partnerShare", (int) Math.round(totalAmount * 0.5));
            }
        } catch (EmptyResultDataAccessException e) {
            // Обработка случая, когда нет записей для данного referralId
            System.out.println("No data found for referralId: " + referralId);
        } catch (Exception e) {
            // Обработка других исключений
            System.err.println("Error fetching referral stats: " + e.getMessage());
            e.printStackTrace(); // Для детальной отладки
        }

        return result;
    }
}