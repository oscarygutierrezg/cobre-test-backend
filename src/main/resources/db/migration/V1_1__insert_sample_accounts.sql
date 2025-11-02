-- Insert initial accounts for CBMM challenge
-- ACC987654321: Initial Balance $0.00 USD (Destination account)
-- ACC123456789: Initial Balance $200,000.00 MXN (Origin account)
INSERT INTO cbmm.account (account_id, account_number, currency, balance, status, created_at, updated_at, version)
VALUES
    (gen_random_uuid(), 'ACC987654321', 'USD', 0.00, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
    (gen_random_uuid(), 'ACC123456789', 'MXN', 200000.00, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

