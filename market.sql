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
                `short_name` VARCHAR(16) NOT NULL
            ) CHARACTER SET utf8mb4;

            CREATE TABLE item  (
                `symbol_id` INT,
                `date` date,
                `open` float,
                `high` float,
                `low` float,
                `close` float,
                `volume` bigint,
                PRIMARY KEY (`symbol_id`, `date`)
            ) CHARACTER SET utf8mb4;

            CREATE TABLE imported_file (
                `file_name` VARCHAR(255) PRIMARY KEY
            ) CHARACTER SET utf8mb4;
        END IF;

    END //

DELIMITER ;

CALL configureDatabase();
