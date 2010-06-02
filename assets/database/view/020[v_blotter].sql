CREATE VIEW v_blotter AS 
SELECT *
FROM v_all_transactions
WHERE is_template = 0;