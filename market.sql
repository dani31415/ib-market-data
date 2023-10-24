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

            CREATE TABLE symbol (
                `id` INT AUTO_INCREMENT PRIMARY KEY,
                `short_name` VARCHAR(16) NOT NULL,
                `ib_conid` VARCHAR(20)
            ) CHARACTER SET utf8mb4;

            CREATE TABLE item  (
                `symbol_id` INT,
                `date` date,
                `open` float,
                `high` float,
                `low` float,
                `close` float,
                `volume` bigint,
                `source` tinyint(2),
                PRIMARY KEY (`symbol_id`, `date`)
            ) CHARACTER SET utf8mb4;

            CREATE TABLE imported_file (
                `file_name` VARCHAR(255) PRIMARY KEY
            ) CHARACTER SET utf8mb4;
        END IF;

        -- Version 1 --> 2
        IF @schemaVersion = '1' THEN 
            UPDATE configuration SET value='2' WHERE `key`='schemaVersion';

            CREATE TABLE symbol_snapshot (
                `update_id` DATETIME,
                `symbol_id` INT,
                `ib_conid` VARCHAR(20),
                `status` TINYINT(4),
                last_price FLOAT,
                bid_price FLOAT,
                bid_size FLOAT,
                ask_price FLOAT,
                ask_size FLOAT,
                PRIMARY KEY (`update_id`, `symbol_id`)
            ) CHARACTER SET utf8mb4;
        END IF;

        -- Version 2 --> 3
        IF @schemaVersion = '2' THEN 
            UPDATE configuration SET value='3' WHERE `key`='schemaVersion';

            CREATE TABLE `order` (
                `id` INT AUTO_INCREMENT PRIMARY KEY,
                `group_guid` VARCHAR(36) NOT NULL,
                `symbol_id` INT NOT NULL,
                `ib_conid` VARCHAR(20) NOT NULL,
                `date` DATE NOT NULL,
                `order` INT NOT NULL,
                `status` VARCHAR(36) NOT NULL,
                last_price FLOAT,
                bid_price FLOAT,
                ask_price FLOAT,
                open_price FLOAT,
                INDEX (`group_guid`, `symbol_id`)
            ) CHARACTER SET utf8mb4;
        END IF;

        -- Version 3 --> 4
        IF @schemaVersion = '3' THEN 
            UPDATE configuration SET value='4' WHERE `key`='schemaVersion';

            ALTER TABLE `order` ADD COLUMN quantity FLOAT;
            ALTER TABLE `order` ADD COLUMN `description` TEXT;
            ALTER TABLE `order` ADD COLUMN renewal_date DATETIME;
            ALTER TABLE `order` ADD COLUMN `created_at` DATETIME;
            ALTER TABLE `order` ADD COLUMN `updated_at` DATETIME;
            ALTER TABLE `order` ADD COLUMN `buy_order_id` VARCHAR(100);
            ALTER TABLE `order` ADD COLUMN `sell_order_id` VARCHAR(100);
        END IF;

        -- Version 4 --> 5
        IF @schemaVersion = '4' THEN 
            UPDATE configuration SET value='5' WHERE `key`='schemaVersion';

            ALTER TABLE `order` ADD COLUMN buy_order_price FLOAT;
            ALTER TABLE `order` ADD COLUMN buy_position_price FLOAT;
            ALTER TABLE `order` ADD COLUMN sell_order_price FLOAT;
            ALTER TABLE `order` ADD COLUMN sell_position_price FLOAT;
        END IF;

        -- Version 5 --> 6
        IF @schemaVersion = '5' THEN 
            UPDATE configuration SET value='6' WHERE `key`='schemaVersion';

            ALTER TABLE `order` ADD COLUMN ask_price_at_buy_order FLOAT;
            ALTER TABLE `order` ADD COLUMN last_price_at_buy_order FLOAT;
            ALTER TABLE `order` ADD COLUMN ask_price_at_buy FLOAT;
            ALTER TABLE `order` ADD COLUMN last_price_at_buy FLOAT;
            ALTER TABLE `order` ADD COLUMN bid_price_at_sell_order FLOAT;
            ALTER TABLE `order` ADD COLUMN last_price_at_sell_order FLOAT;
            ALTER TABLE `order` ADD COLUMN bid_price_at_sell FLOAT;
            ALTER TABLE `order` ADD COLUMN last_price_at_sell FLOAT;
        END IF;

        -- Version 6 --> 7
        IF @schemaVersion = '6' THEN 
            UPDATE configuration SET value='7' WHERE `key`='schemaVersion';

            ALTER TABLE `order` ADD COLUMN buy_order_at DATETIME;
            ALTER TABLE `order` ADD COLUMN buy_at DATETIME;
            ALTER TABLE `order` ADD COLUMN sell_order_at DATETIME;
            ALTER TABLE `order` ADD COLUMN sell_at DATETIME;
            ALTER TABLE `order` ADD COLUMN model_name VARCHAR(100);
        END IF;

        -- Version 7 --> 8
        IF @schemaVersion = '7' THEN 
            UPDATE configuration SET value='8' WHERE `key`='schemaVersion';

            ALTER TABLE `order` ADD COLUMN next_renewal_order_id INT;
            ALTER TABLE `order` ADD COLUMN previous_renewal_order_id INT;
        END IF;

        -- Version 8 --> 9
        IF @schemaVersion = '8' THEN 
            UPDATE configuration SET value='9' WHERE `key`='schemaVersion';

            -- ALTER TABLE `symbol` ADD COLUMN available INT(1) DEFAULT 1;
            ALTER TABLE `order` ADD COLUMN buy_order_at_date date as (date(buy_order_at));
            ALTER TABLE `order` ADD COLUMN sell_order_at_date date as (date(sell_order_at));
        END IF;

        -- Version 9 --> 10
        IF @schemaVersion = '9' THEN 
            UPDATE configuration SET value='10' WHERE `key`='schemaVersion';

            CREATE TABLE `period` (
                `id` INT AUTO_INCREMENT PRIMARY KEY,
                `date` date NOT NULL,
                `updated` INT(1) DEFAULT 0,
                `mean` FLOAT,
                INDEX (`date`)
            ) CHARACTER SET utf8mb4;
        END IF;

        -- Version 10 --> 11
        IF @schemaVersion = '10' THEN 
            UPDATE configuration SET value='11' WHERE `key`='schemaVersion';

            ALTER TABLE `order` ADD COLUMN symbol_src_name VARCHAR(100);
        END IF;

        -- Version 11 --> 12
        IF @schemaVersion = '11' THEN 
            UPDATE configuration SET value='12' WHERE `key`='schemaVersion';

            CREATE TABLE `simulation_item` (
                `id` INT AUTO_INCREMENT PRIMARY KEY,
                `group_guid` VARCHAR(36) NOT NULL,
                `symbol_id` INT NOT NULL,
                `ib_conid` VARCHAR(20) NOT NULL,
                `order` INT NOT NULL,
                `period` INT NOT NULL,
                `open_price` FLOAT NOT NULL,
                `gain` FLOAT NOT NULL,
                `symbol_src_name` VARCHAR(100) NOT NULL,
                `model_name` VARCHAR(100) NOT NULL,
                `created_at` DATETIME NOT NULL,
                INDEX (`period`)
            ) CHARACTER SET utf8mb4;
        END IF;

        -- Version 12 --> 13
        IF @schemaVersion = '12' THEN 
            UPDATE configuration SET value='13' WHERE `key`='schemaVersion';

            ALTER TABLE symbol ADD COLUMN old_names VARCHAR(200);
        END IF;

        -- Version 13 --> 14
        IF @schemaVersion = '13' THEN 
            UPDATE configuration SET value='14' WHERE `key`='schemaVersion';

            ALTER TABLE symbol ADD COLUMN disabled INT(1) NOT NULL DEFAULT 0;
        END IF;

        -- Version 14 --> 15
        IF @schemaVersion = '14' THEN 
            UPDATE configuration SET value='15' WHERE `key`='schemaVersion';

            ALTER TABLE item ADD COLUMN since_pre_open INT DEFAULT NULL;
        END IF;

        -- Version 15 --> 16
        IF @schemaVersion = '15' THEN 
            UPDATE configuration SET value='16' WHERE `key`='schemaVersion';

            CREATE TABLE `log` (
                `id` INT AUTO_INCREMENT PRIMARY KEY,
                `source` VARCHAR(36) NOT NULL,
                `message` TEXT NOT NULL,
                `object_type` VARCHAR(36),
                `object` MEDIUMTEXT,
                `created_at` DATETIME NOT NULL
            ) CHARACTER SET utf8mb4;
        END IF;

        -- Version 16 --> 17
        IF @schemaVersion = '16' THEN 
            UPDATE configuration SET value='17' WHERE `key`='schemaVersion';

            ALTER TABLE period MODIFY COLUMN updated BIT;
            ALTER TABLE symbol MODIFY COLUMN `disabled` BIT;

            CREATE TABLE minute_item  (
                `symbol_id` INT,
                `date` date,
                `minute` int,
                `open` float,
                `high` float,
                `low` float,
                `close` float,
                `volume` bigint,
                `source` tinyint(2),
                PRIMARY KEY (`symbol_id`, `date`, `minute`)
            ) CHARACTER SET utf8mb4;
        END IF;

        -- Version 17 --> 18
        IF @schemaVersion = '17' THEN 
            UPDATE configuration SET value='18' WHERE `key`='schemaVersion';

            ALTER TABLE `symbol` ADD COLUMN `created_at` DATETIME;
            ALTER TABLE `symbol` ADD COLUMN `updated_at` DATETIME;
        END IF;

        -- Version 18 --> 19
        IF @schemaVersion = '18' THEN 
            UPDATE configuration SET value='19' WHERE `key`='schemaVersion';

            ALTER TABLE `item` ADD COLUMN `updated_at` DATETIME;
        END IF;

        -- Version 19 --> 20
        IF @schemaVersion = '19' THEN
            UPDATE configuration SET value='20' WHERE `key`='schemaVersion';

            ALTER TABLE `item` ADD COLUMN `version` tinyint(2) NOT NULL DEFAULT 0 AFTER `date`;
            ALTER TABLE `item` ADD COLUMN `stagging` tinyint(1) NOT NULL DEFAULT 1 AFTER `version`;
            ALTER TABLE item DROP PRIMARY KEY;
            ALTER TABLE item ADD PRIMARY KEY (symbol_id, `date`, `version`);
        END IF;

        -- Version 20 --> 21
        IF @schemaVersion = '20' THEN
            UPDATE configuration SET value='21' WHERE `key`='schemaVersion';

            CREATE TABLE snapshot  (
                `symbol_id` INT,
                `date` date,
                `last` float,
                `volume` bigint,
                `status` TINYINT(4),
                `updated_at` DATETIME,
                PRIMARY KEY (`symbol_id`, `date`, `updated_at`)
            ) CHARACTER SET utf8mb4;
        END IF;

        -- Version 21 --> 22
        IF @schemaVersion = '21' THEN 
            UPDATE configuration SET value='22' WHERE `key`='schemaVersion';

            ALTER TABLE `symbol` ADD COLUMN forbidden BIT DEFAULT 0 AFTER `disabled`;
        END IF;

        -- Version 22 --> 23
        IF @schemaVersion = '22' THEN 
            UPDATE configuration SET value='23' WHERE `key`='schemaVersion';

            -- defaults null
            ALTER TABLE `snapshot` ADD COLUMN created_at DATETIME AFTER `updated_at`;
            -- dfaults datetime
            ALTER TABLE `snapshot` CHANGE created_at created_at DATETIME DEFAULT CURRENT_TIMESTAMP;
            ALTER TABLE `snapshot` CHANGE `updated_at` `datetime` DATETIME;
        END IF;

        -- Version 23 --> 24
        IF @schemaVersion = '23' THEN 
            UPDATE configuration SET value='24' WHERE `key`='schemaVersion';

            -- dfaults back to null
            ALTER TABLE `snapshot` CHANGE created_at created_at DATETIME DEFAULT NULL;
        END IF;

    END //

    -- ALTER TABLE minute_item ADD INDEX (`date`, symbol_id);
    -- ALTER TABLE item ADD INDEX (`date`, symbol_id);

    -- ALTER TABLE item DROP PRIMARY KEY;
    -- ALTER TABLE item ADD PRIMARY KEY (`date`, symbol_id);

DELIMITER ;

CALL configureDatabase();
