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
            // Count invited users
            int invitedCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM referrals WHERE referral_id = ?", Integer.class, referralId);
            result.put("invitedCount", invitedCount);
            LOGGER.log(Level.INFO, "Invited count: {0}", invitedCount);

            // Count users who purchased subscriptions
            int purchasedCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM referrals WHERE referral_id = ? AND subscription IS NOT NULL AND `time` IS NOT NULL", Integer.class, referralId);
            result.put("purchasedCount", purchasedCount);
            LOGGER.log(Level.INFO, "Purchased count: {0}", purchasedCount);


            // Fetch subscription details
            String sql = "SELECT subscription, `time` FROM referrals WHERE referral_id = ? AND subscription IS NOT NULL AND `time` IS NOT NULL";
            List<Map<String, Object>> subscriptions = jdbcTemplate.queryForList(sql, referralId);

            Map<String, Integer> subscriptionDetails = new HashMap<>(); // Initialize here
            int totalAmount = 0;

            for (Map<String, Object> row : subscriptions) {
                String subscriptionType = (String) row.get("subscription");
                int time = ((Number) row.get("time")).intValue(); // Safer type casting

                String key = subscriptionType.trim() + " " + time; //Trim for whitespace handling

                LOGGER.log(Level.FINE, "Processing subscription: {0}", key);

                if (SUBSCRIPTIONS.containsKey(key)) {
                    subscriptionDetails.merge(key, 1, Integer::sum); //Efficiently increment counts
                    totalAmount += SUBSCRIPTIONS.get(key);
                } else {
                    LOGGER.log(Level.WARNING, "Unknown subscription key from DB: {0}", key);
                }
            }

            result.put("subscriptionDetails", subscriptionDetails);
            result.put("totalAmount", totalAmount);
            result.put("partnerShare", totalAmount * 0.5);

        } catch (DataAccessException e) {
            LOGGER.log(Level.SEVERE, "Database error in getReferralStats: ", e);
            result.put("error", "Database error. Please try again later.");
        } catch (Exception e) { //Catch other potential exceptions
            LOGGER.log(Level.SEVERE, "Unexpected error in getReferralStats: ", e);
            result.put("error", "An unexpected error occurred. Please try again later.");
        }
        return result;
    }
}