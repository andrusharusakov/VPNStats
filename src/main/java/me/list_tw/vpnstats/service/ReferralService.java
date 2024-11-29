package me.list_tw.vpnstats.service;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


@Service
public class ReferralService {

    private static final Logger LOGGER = Logger.getLogger(ReferralService.class.getName());

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final Map<String, Integer> SUBSCRIPTIONS = new HashMap<>() {{
        put("VPN Lite 30", 145);
        put("VPN Lite 180", 695);
        put("VPN Lite 365", 1195);
        put("VPN Pro 30", 245);
        put("VPN Pro 180", 895);
        put("VPN Pro 365", 1745);
    }};

    public Map<String, Object> getReferralStats(long referralId) {
        Map<String, Object> result = new HashMap<>();
        try {
            int invitedCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM referrals WHERE referral_id = ?", Integer.class, referralId);
            result.put("invitedCount", invitedCount);

            int purchasedCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM referrals WHERE referral_id = ? AND subscription IS NOT NULL AND time IS NOT NULL", Integer.class, referralId);
            result.put("purchasedCount", purchasedCount);

            String sql = "SELECT subscription, time FROM referrals WHERE referral_id = ? AND subscription IS NOT NULL AND time IS NOT NULL";
            List<Map<String, Object>> subscriptionsList = jdbcTemplate.queryForList(sql, referralId);

            int totalAmount = 0;
            Map<String, Integer> subscriptionDetails = new HashMap<>(SUBSCRIPTIONS); //Defensive copy
            for (Map<String, Object> row : subscriptionsList) {
                String subscriptionType = (String) row.get("subscription");
                int time = (int) row.get("time");
                String key = subscriptionType + " " + time;

                if (SUBSCRIPTIONS.containsKey(key)) {
                    subscriptionDetails.put(key, subscriptionDetails.getOrDefault(key, 0) + 1);
                    totalAmount += SUBSCRIPTIONS.get(key);
                } else {
                    LOGGER.log(Level.WARNING, "Unknown subscription type: {0} {1}", new Object[]{subscriptionType, time});
                }
            }

            result.put("subscriptionDetails", subscriptionDetails);
            result.put("totalAmount", totalAmount);
            result.put("partnerShare", totalAmount * 0.5);

        } catch (DataAccessException e) {
            LOGGER.log(Level.SEVERE, "Error fetching referral stats: ", e);
            result.put("error", "Error fetching referral stats. Please try again later.");
        }
        return result;
    }
}