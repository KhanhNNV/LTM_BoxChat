CREATE DATABASE IF NOT EXISTS DBChatBoxJDBC;

USE DBChatBoxJDBC;

-- Users Table
CREATE TABLE Users (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(100) NOT NULL,
    fullname VARCHAR(100),
    gmail VARCHAR(100)
);

-- Groups Table
CREATE TABLE Groups (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(100),
    leader_id INT,
    FOREIGN KEY (leader_id) REFERENCES Users (id)
);

-- User_Group (many-to-many)
CREATE TABLE User_Group (
    user_id INT,
    group_id INT,
    PRIMARY KEY (user_id, group_id),
    FOREIGN KEY (user_id) REFERENCES Users (id),
    FOREIGN KEY (group_id) REFERENCES Groups (id)
);

-- Messages Table
CREATE TABLE Messages (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT,
    group_id INT,
    content TEXT NOT NULL,
    send_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES Users (id),
    FOREIGN KEY (group_id) REFERENCES Groups (id)
);