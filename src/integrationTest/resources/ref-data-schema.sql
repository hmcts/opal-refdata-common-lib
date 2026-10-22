CREATE TABLE dummy_prisons (
    prison_id BIGINT PRIMARY KEY,
    name VARCHAR(35) NOT NULL,
    address_line_1 VARCHAR(35) NOT NULL,
    address_line_2 VARCHAR(35),
    address_line_3 VARCHAR(35),
    address_line_4 VARCHAR(35),
    address_line_5 VARCHAR(35),
    postcode VARCHAR(8)
);
