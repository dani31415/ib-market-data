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

        -- Version 3 --> 4
        IF @schemaVersion = '3' THEN
            UPDATE configuration SET value='4' WHERE `key`='schemaVersion';

            ALTER TABLE `order` ADD COLUMN `optimization` TEXT;
        END IF;

        -- Version 4 --> 5
        IF @schemaVersion = '4' THEN
            UPDATE configuration SET value='5' WHERE `key`='schemaVersion';

            ALTER TABLE `order` ADD COLUMN sell_desired_price FLOAT after sell_position_price;
            ALTER TABLE `order` ADD COLUMN buy_desired_price FLOAT after buy_position_price;
            ALTER TABLE `trade` ADD COLUMN ib_order_ref VARCHAR(64) after order_id;
            ALTER TABLE `trade` ADD INDEX (ib_order_ref);
        END IF;

        -- Version 5 --> 6
        IF @schemaVersion = '5' THEN
            UPDATE configuration SET value='6' WHERE `key`='schemaVersion';

            ALTER TABLE `order` ADD COLUMN `minute` int after `date`;
            ALTER TABLE `order` ADD INDEX (`date`, `minute`);

            CREATE TABLE replay  (
                `order_id` INT PRIMARY KEY,
                `symbol_id` INT NOT NULL,
                `minute` INT,
                `period` INT,
                `variant` VARCHAR(100),
                `iteration` INT,
                `epoch` INT,
                `friends` TEXT
            ) CHARACTER SET utf8mb4;
        END IF;

        -- Version 6 --> 7
        IF @schemaVersion = '6' THEN
            UPDATE configuration SET value='7' WHERE `key`='schemaVersion';

            ALTER TABLE `simulation_item` CHANGE `open_price` `purchase` float;
            ALTER TABLE `simulation_item` CHANGE `gain` `gains` float;
            ALTER TABLE `simulation_item` CHANGE `symbol_id` `symbol_id` int;
            ALTER TABLE `simulation_item` CHANGE `ib_conid` `ib_conid` varchar(20);
            ALTER TABLE `simulation_item` CHANGE `group_guid` `group_guid` varchar(36);
            ALTER TABLE `simulation_item` ADD COLUMN `minute` int after `period`;
            ALTER TABLE `simulation_item` ADD COLUMN `simulation_name` varchar(100) not null after `model_name`;
            -- ALTER TABLE `simulation_item` DROP `group_guid` `purchase` float
        END IF;

        IF @schemaVersion = '7' THEN
            UPDATE configuration SET value='8' WHERE `key`='schemaVersion';

            ALTER TABLE `simulation_item` ADD COLUMN `early` float after `gains`;
        END IF;

        IF @schemaVersion = '8' THEN
            UPDATE configuration SET value='9' WHERE `key`='schemaVersion';

            ALTER TABLE `order` ADD COLUMN purchase_expires DATETIME AFTER renewal_date;
        END IF;

        IF @schemaVersion = '9' THEN
            UPDATE configuration SET value='10' WHERE `key`='schemaVersion';

            ALTER TABLE `order` ADD COLUMN model_last_price float AFTER last_price;
        END IF;

        IF @schemaVersion = '10' THEN
            UPDATE configuration SET value='11' WHERE `key`='schemaVersion';

            ALTER TABLE `simulation_item` ADD COLUMN `liquidated` BIT after `early`;
        END IF;

        IF @schemaVersion = '11' THEN
            UPDATE configuration SET value='12' WHERE `key`='schemaVersion';

            CREATE TABLE ib_order  (
                `id` VARCHAR(64) PRIMARY KEY,
                `order_id` INT,
                `active` BIT NOT NULL,
                `price` FLOAT NOT NULL,
                `quantity` FLOAT NOT NULL,
                `order_ref` VARCHAR(64),
                `side` VARCHAR(1) NOT NULL,
                `status` VARCHAR(64),
                `created_at` DATETIME,
                `updated_at` DATETIME,
                `closed_at` DATETIME
            ) CHARACTER SET utf8mb4;

            CREATE TABLE ib_order_change  (
                `id` INT AUTO_INCREMENT PRIMARY KEY,
                `ib_order_id` VARCHAR(64) NOT NULL,
                `price` FLOAT NOT NULL,
                `quantity` FLOAT NOT NULL,
                `created_at` DATETIME,
                INDEX (`ib_order_id`)
            ) CHARACTER SET utf8mb4;

            ALTER TABLE `ib_order` ADD INDEX (active);

        END IF;

        IF @schemaVersion = '12' THEN
            UPDATE configuration SET value='13' WHERE `key`='schemaVersion';

            ALTER TABLE `simulation_item` ADD COLUMN `sell_prices` TEXT after `liquidated`;
        END IF;

    END //

DELIMITER ;

CALL configureDatabase();
