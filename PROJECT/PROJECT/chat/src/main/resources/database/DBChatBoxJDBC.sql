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

SHOW CREATE TABLE `User_Group`;

ALTER TABLE `Groups` DROP FOREIGN KEY `groups_ibfk_1`;

ALTER TABLE `Groups`
ADD CONSTRAINT `fk_groups_leader` FOREIGN KEY (leader_id) REFERENCES Users (id) ON DELETE SET NULL;

ALTER TABLE `User_Group` DROP FOREIGN KEY `user_group_ibfk_1`;

ALTER TABLE `User_Group` DROP FOREIGN KEY `user_group_ibfk_2`;

ALTER TABLE `User_Group`
ADD CONSTRAINT `fk_usergroup_user` FOREIGN KEY (user_id) REFERENCES Users (id) ON DELETE CASCADE;

ALTER TABLE `User_Group`
ADD CONSTRAINT `fk_usergroup_group` FOREIGN KEY (group_id) REFERENCES `Groups` (id) ON DELETE CASCADE;

ALTER TABLE `Messages` DROP FOREIGN KEY `messages_ibfk_1`;

ALTER TABLE `Messages` DROP FOREIGN KEY `messages_ibfk_2`;

ALTER TABLE `Messages`
ADD CONSTRAINT `fk_messages_user` FOREIGN KEY (user_id) REFERENCES Users (id) ON DELETE SET NULL;

ALTER TABLE `Messages`
ADD CONSTRAINT `fk_messages_group` FOREIGN KEY (group_id) REFERENCES `Groups` (id) ON DELETE CASCADE;