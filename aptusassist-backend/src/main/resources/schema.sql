CREATE TABLE IF NOT EXISTS slots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    date DATE NOT NULL,
    pass_no INT NOT NULL,
    status VARCHAR(50) NOT NULL,
    UNIQUE (date, pass_no)
);