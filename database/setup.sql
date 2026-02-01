-- ============================================
-- Database Setup Script for Distributed System
-- ============================================

-- Use the database (already created by Docker)
USE distributed_db;

-- Create users table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    full_name VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_email (email)
) ENGINE=InnoDB;

-- Insert sample data (100 records for testing)
INSERT INTO users (username, email, full_name) VALUES
('john_doe', 'john.doe@email.com', 'John Doe'),
('jane_smith', 'jane.smith@email.com', 'Jane Smith'),
('bob_wilson', 'bob.wilson@email.com', 'Bob Wilson'),
('alice_johnson', 'alice.johnson@email.com', 'Alice Johnson'),
('charlie_brown', 'charlie.brown@email.com', 'Charlie Brown'),
('diana_ross', 'diana.ross@email.com', 'Diana Ross'),
('edward_norton', 'edward.norton@email.com', 'Edward Norton'),
('fiona_apple', 'fiona.apple@email.com', 'Fiona Apple'),
('george_clooney', 'george.clooney@email.com', 'George Clooney'),
('helen_mirren', 'helen.mirren@email.com', 'Helen Mirren'),
('ivan_petrov', 'ivan.petrov@email.com', 'Ivan Petrov'),
('julia_roberts', 'julia.roberts@email.com', 'Julia Roberts'),
('kevin_spacey', 'kevin.spacey@email.com', 'Kevin Spacey'),
('laura_dern', 'laura.dern@email.com', 'Laura Dern'),
('michael_jordan', 'michael.jordan@email.com', 'Michael Jordan'),
('natalie_portman', 'natalie.portman@email.com', 'Natalie Portman'),
('oscar_wilde', 'oscar.wilde@email.com', 'Oscar Wilde'),
('patricia_arquette', 'patricia.arquette@email.com', 'Patricia Arquette'),
('quentin_tarantino', 'quentin.tarantino@email.com', 'Quentin Tarantino'),
('rachel_mcadams', 'rachel.mcadams@email.com', 'Rachel McAdams');

-- Generate more test data (additional 80 records)
INSERT INTO users (username, email, full_name)
SELECT
    CONCAT('user_', n),
    CONCAT('user_', n, '@email.com'),
    CONCAT('User Number ', n)
FROM (
    SELECT @row := @row + 1 as n
    FROM (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
          UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) t1,
         (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
          UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7) t2,
         (SELECT @row := 20) init
    LIMIT 80
) numbers;

-- Verify data
SELECT COUNT(*) as total_users FROM users;

-- Show sample data
SELECT * FROM users LIMIT 10;
