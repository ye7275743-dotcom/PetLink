package com.petlink.infrastructure.file.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class BoundFileAssociationVerifier {
    private final JdbcTemplate jdbcTemplate;

    public BoundFileAssociationVerifier(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Verifies that the formal business image row still points to the exact formal path.
     * SQL is selected only from a fixed whitelist; businessType never becomes SQL text.
     */
    public boolean exists(String businessType, Long businessId, String formalPath) {
        if (businessType == null || businessId == null || businessId <= 0 || formalPath == null || formalPath.isBlank()) {
            return false;
        }
        String sql;
        switch (businessType) {
            case "RESCUE_CLUE":
                sql = "SELECT EXISTS(SELECT 1 FROM rescue_clue_image WHERE clue_id=? AND image_path=?)";
                break;
            case "ANIMAL":
                sql = "SELECT EXISTS(SELECT 1 FROM animal_image WHERE animal_id=? AND image_path=?)";
                break;
            case "FOLLOW_UP":
                sql = "SELECT EXISTS(SELECT 1 FROM follow_up_image WHERE follow_up_id=? AND image_path=?)";
                break;
            default:
                return false;
        }
        Integer exists = jdbcTemplate.queryForObject(sql, Integer.class, businessId, formalPath);
        return exists != null && exists == 1;
    }
}
