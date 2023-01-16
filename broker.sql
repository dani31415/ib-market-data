DELIMITER //

DROP PROCEDURE IF EXISTS configureDatabase;

CREATE PROCEDURE configureDatabase()
    BEGIN

        CREATE TABLE IF NOT EXISTS configuration (
            `key` VARCHAR(255) PRIMARY KEY,
            value VARCHAR(2048)
        ) CHARACTER SET utf8mb4;

        SELECT value INTO @schemaVersion FROM configuration where `key`='schemaVersion' LIMIT 1;

        -- Version 0 --> 1
        IF @schemaVersion IS NULL THEN 
            INSERT INTO configuration VALUES ('schemaVersion','1');

            ALTER TABLE market.`order` DROP COLUMN buy_order_at_date;
            ALTER TABLE market.`order` DROP COLUMN sell_order_at_date;

            CREATE TABLE `order` LIKE market.`order`;
            INSERT INTO `order` SELECT * FROM market.`order`;

            ALTER TABLE market.`order` ADD COLUMN buy_order_at_date date as (date(buy_order_at));
            ALTER TABLE market.`order` ADD COLUMN sell_order_at_date date as (date(sell_order_at));
            ALTER TABLE `order` ADD COLUMN buy_order_at_date date as (date(buy_order_at));
            ALTER TABLE `order` ADD COLUMN sell_order_at_date date as (date(sell_order_at));

            CREATE TABLE `simulation_item` LIKE market.`simulation_item`;
            INSERT INTO `simulation_item` SELECT * FROM market.`simulation_item`;

            CREATE TABLE `log` LIKE market.`log`;
            INSERT INTO `log` SELECT * FROM market.`log`;
        END IF;

    END //

DELIMITER ;

CALL configureDatabase();
