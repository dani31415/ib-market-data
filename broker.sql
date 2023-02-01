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

            ALTER TABLE market.`order-old` DROP COLUMN buy_order_at_date;
            ALTER TABLE market.`order-old` DROP COLUMN sell_order_at_date;

            CREATE TABLE `order` LIKE market.`order-old`;
            INSERT INTO `order` SELECT * FROM market.`order-old`;

            ALTER TABLE market.`order-old` ADD COLUMN buy_order_at_date date as (date(buy_order_at));
            ALTER TABLE market.`order-old` ADD COLUMN sell_order_at_date date as (date(sell_order_at));
            ALTER TABLE `order` ADD COLUMN buy_order_at_date date as (date(buy_order_at));
            ALTER TABLE `order` ADD COLUMN sell_order_at_date date as (date(sell_order_at));

            CREATE TABLE `simulation_item` LIKE market.`simulation_item-old`;
            INSERT INTO `simulation_item` SELECT * FROM market.`simulation_item-old`;

            CREATE TABLE `log` LIKE market.`log-old`;
            INSERT INTO `log` SELECT * FROM market.`log-old`;
        END IF;

        -- Version 1 --> 2
        IF @schemaVersion = '1' THEN 
            UPDATE configuration SET value='2' WHERE `key`='schemaVersion';

            CREATE TABLE trade  (
                `id` VARCHAR(64) PRIMARY KEY,
                `order_id` INT NOT NULL,
                `trade_time` DATETIME NOT NULL,
                `side` VARCHAR(1),
                `size` double,
                `price` double,
                `commission` double,
                INDEX (`order_id`, `trade_time`)
            ) CHARACTER SET utf8mb4;

            ALTER TABLE `order` ADD COLUMN bought_quantity FLOAT after quantity;
            ALTER TABLE `order` ADD COLUMN sold_quantity FLOAT after bought_quantity;
        END IF;

        -- Version 2 --> 3
        IF @schemaVersion = '2' THEN 
            UPDATE configuration SET value='3' WHERE `key`='schemaVersion';

            ALTER TABLE `order` ADD COLUMN liquidating BIT DEFAULT 0 NOT NULL AFTER renewal_date;
        END IF;

    END //

DELIMITER ;

CALL configureDatabase();
