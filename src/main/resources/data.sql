CREATE TABLE users (id_user INT AUTO_INCREMENT PRIMARY KEY,
                    username VARCHAR(50) NOT NULL,
                    email VARCHAR(100) NOT NULL UNIQUE,
                    password VARCHAR(255) NOT NULL);

CREATE TABLE connections (id_connection INT AUTO_INCREMENT PRIMARY KEY,
                          user_id INT NOT NULL,
                          friend_id INT NOT NULL

                          CONSTRAINT fk_user_connection FOREIGN KEY (user_id) REFERENCES users(id_user),
                          CONSTRAINT fk_friend_connection FOREIGN KEY (friend_id) REFERENCES users(id_user));

CREATE TABLE bank_accounts (id_bank_account INT AUTO_INCREMENT PRIMARY KEY,
                            user_id INT NOT NULL,
                            iban VARCHAR(34),
                            bic VARCHAR(11),
                            bank_name VARCHAR(100),

                            CONSTRAINT fk_bank_user FOREIGN KEY (user_id) REFERENCES users(id_user));

CREATE TABLE transactions (id_transaction INT AUTO_INCREMENT PRIMARY KEY,
                           sender_id INT NOT NULL,
                           receiver_id INT NOT NULL,
                           description VARCHAR(255),
                           amount DECIMAL(10, 2) NOT NULL,
                           fee DECIMAL(10, 2),
                           date_transaction TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                           CONSTRAINT fk_sender FOREIGN KEY (sender_id) REFERENCES users(id_user),
                           CONSTRAINT fk_receiver FOREIGN KEY (receiver_id) REFERENCES users(id_user));