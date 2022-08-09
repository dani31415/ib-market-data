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

    END //

DELIMITER ;

CALL configureDatabase();
