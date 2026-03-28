CREATE DATABASE IF NOT EXISTS godleiblog CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'godleiblog'@'localhost' IDENTIFIED BY 'GodLeiBlog123!';
CREATE USER IF NOT EXISTS 'godleiblog'@'127.0.0.1' IDENTIFIED BY 'GodLeiBlog123!';
GRANT ALL PRIVILEGES ON godleiblog.* TO 'godleiblog'@'localhost';
GRANT ALL PRIVILEGES ON godleiblog.* TO 'godleiblog'@'127.0.0.1';
FLUSH PRIVILEGES;
