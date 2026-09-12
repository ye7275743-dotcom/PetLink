-- Existing deployments: enrich animal profiles without changing existing records.
-- Run once against the PetLink database before deploying the matching server build.
ALTER TABLE `animal`
    ADD COLUMN `personality` VARCHAR(1000) DEFAULT NULL COMMENT '性格与行为观察' AFTER `health_condition`,
    ADD COLUMN `adoption_requirements` VARCHAR(1000) DEFAULT NULL COMMENT '领养家庭要求' AFTER `personality`;
