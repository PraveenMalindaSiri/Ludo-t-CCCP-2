CREATE DATABASE IF NOT EXISTS ludot
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

CREATE DATABASE IF NOT EXISTS ludot_test
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

-- Run the following statements as an administrator after replacing the example password.
-- CREATE USER IF NOT EXISTS 'ludot_app'@'localhost' IDENTIFIED BY 'replace-me';
-- GRANT SELECT, INSERT, UPDATE, DELETE ON ludot.* TO 'ludot_app'@'localhost';
-- GRANT SELECT, INSERT, UPDATE, DELETE ON ludot_test.* TO 'ludot_app'@'localhost';
-- FLUSH PRIVILEGES;
