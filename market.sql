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

            CREATE TABLE Symbols (
                id INT AUTO_INCREMENT PRIMARY KEY,    
                shortName VARCHAR(16) NOT NULL
            ) CHARACTER SET utf8mb4;
        END IF;

    END //

DELIMITER ;

CALL configureDatabase();
