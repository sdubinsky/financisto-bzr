INSERT INTO category (_id, title, left, right) VALUES (0, '<NO_CATEGORY>', 1, 2);

INSERT INTO project (_id, title) VALUES (0, '<NO_PROJECT>');

INSERT INTO locations(_id, name, datetime, provider, accuracy, latitude, longitude, is_payee, resolved_address) 
VALUES (0, '<CURRENT_LOCATION>', 0, "?", "?", 0, 0, 0, "?");

INSERT INTO currency (title, name, symbol) VALUES ("Russian Ruble", "RUB", "р.");
INSERT INTO currency (title, name, symbol) VALUES ("American Dollar", "USD", "$");
INSERT INTO currency (title, name, symbol) VALUES ("European Euro", "EUR", "€");
INSERT INTO currency (title, name, symbol) VALUES ("British Pound", "GBP", "£");