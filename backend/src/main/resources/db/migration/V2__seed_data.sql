-- ---------------------------------------------------------------------------
-- Seed data (synthetic, deterministically generated).
-- 7 customers with distinctive activity patterns, ~10 risk rules, fired
-- risk_assessments where transaction patterns match, and 3 operator logins.
-- Operator credentials (BCrypt below): alice/operator1, bob/operator2, carol/supervisor1
-- ---------------------------------------------------------------------------

-- Operators (passwords: alice/operator1, bob/operator2, carol/supervisor1)
INSERT INTO operators (operator_id, username, password_hash, display_name, role, created_at) VALUES ('46f799f9-ab07-5dfe-985a-ce4e16419dc7', 'alice', '$2b$10$i.fFB0SZP0mT5rCbZTyA.OSrLnhEVyUtcH0zimaQq19h3Gr6KbtpO', 'Alice Ferraro', 'OPERATOR', '2026-08-30 09:00:00');
INSERT INTO operators (operator_id, username, password_hash, display_name, role, created_at) VALUES ('3c7bebef-99e7-572c-a1a7-210101151e27', 'bob', '$2b$10$Rwfli5E8VY0E5hZ496tTuez35OuxtdT21Rql.RDxI..kLM1Hw8kTm', 'Bob Lindqvist', 'OPERATOR', '2026-08-30 09:00:00');
INSERT INTO operators (operator_id, username, password_hash, display_name, role, created_at) VALUES ('4e9fb749-8974-55af-823a-d117d93fd2ae', 'carol', '$2b$10$GPzICoZ/h3oCJwbi1aq9QOwBho9zNpM49ju1T9H0FP/EtJFaJVGja', 'Carol Mensah', 'SUPERVISOR', '2026-08-30 09:00:00');

-- Risk rules
INSERT INTO risk_rules (rule_id, rule_name, applies_to, threshold_logic, weight) VALUES ('daacee21-dcef-5664-854b-8d229493c1a8', 'High-value cross-border payment', 'PAYMENT', 'payment_method IN (''WIRE'',''SWIFT'') AND amount >= 10000 AND receiver_bank_country <> customer.country', 25);
INSERT INTO risk_rules (rule_id, rule_name, applies_to, threshold_logic, weight) VALUES ('8d2d65c3-6697-53d2-bfb1-70e8ffe6495c', 'Payment to high-risk jurisdiction', 'PAYMENT', 'receiver_bank_country IN (high_risk_country_list) -- see policy ''High-Risk Jurisdictions''', 30);
INSERT INTO risk_rules (rule_id, rule_name, applies_to, threshold_logic, weight) VALUES ('ca5b6c7e-ad58-524c-a5d2-ed6bda83b320', 'Structuring: repeated sub-threshold transfers', 'PAYMENT', 'count(payments where 9000 <= amount < 10000 within 72h) >= 3', 20);
INSERT INTO risk_rules (rule_id, rule_name, applies_to, threshold_logic, weight) VALUES ('302df05c-8e9e-51ba-99c9-9d47af3d8ca7', 'Rapid crypto outflow', 'CRYPTO', 'count(outgoing crypto transfers within 24h) >= 3', 25);
INSERT INTO risk_rules (rule_id, rule_name, applies_to, threshold_logic, weight) VALUES ('f398af64-5145-58f2-820c-b6107e54aea5', 'Transfer to known mixing service', 'CRYPTO', 'wallet_address_to IN (known_mixer_address_list)', 35);
INSERT INTO risk_rules (rule_id, rule_name, applies_to, threshold_logic, weight) VALUES ('a2468bf9-4383-50ce-9dd4-3793daf245b5', 'Unregistered/first-time exchange counterparty', 'CRYPTO', 'exchange_name NOT IN (approved_vasp_list) OR exchange_name IS NULL', 10);
INSERT INTO risk_rules (rule_id, rule_name, applies_to, threshold_logic, weight) VALUES ('981996a6-394b-5a44-b6ad-88b20b0165c1', 'Card decline burst', 'CARD', 'count(declined card auths within 60m) >= 3', 15);
INSERT INTO risk_rules (rule_id, rule_name, applies_to, threshold_logic, weight) VALUES ('5432f05a-3ece-5c53-8ae9-41de3af81a2c', 'High-risk merchant category', 'CARD', 'mcc_code IN (''7995'',''6051'',''5993'',''4829'')', 15);
INSERT INTO risk_rules (rule_id, rule_name, applies_to, threshold_logic, weight) VALUES ('34c126a4-59ad-5b66-b7a7-d389072056b9', 'High-value card-not-present transaction', 'CARD', 'card_present = false AND amount >= 2000', 10);
INSERT INTO risk_rules (rule_id, rule_name, applies_to, threshold_logic, weight) VALUES ('2f60808a-ef61-5127-b3d5-a7cdbcc9b8d0', 'Activity velocity spike', 'ALL', '7d transaction volume > 5x trailing 90d weekly average', 20);
INSERT INTO risk_rules (rule_id, rule_name, applies_to, threshold_logic, weight) VALUES ('c8300eeb-e8bf-5b4a-aa16-8747fd374cdc', 'Round-amount pattern', 'ALL', 'count(amount % 1000 == 0 AND amount >= 5000 within 7d) >= 3', 10);

-- Customers
INSERT INTO customers (customer_id, full_name, email, segment, country, created_at) VALUES ('3968f964-895e-53af-880b-db8ad20bd114', 'Marco Deluca', 'marco.deluca@example.com', 'RETAIL', 'IT', '2023-04-11 10:00:00');
INSERT INTO customers (customer_id, full_name, email, segment, country, created_at) VALUES ('e0083363-bfaa-5bbf-8aea-765587c08aa7', 'Yulia Sorokina', 'y.sorokina@example.com', 'PREMIUM', 'DE', '2021-09-02 14:30:00');
INSERT INTO customers (customer_id, full_name, email, segment, country, created_at) VALUES ('f64a8ac6-baf4-5ffd-b7ce-3256ae699248', 'Daniel Osei', 'daniel.osei@example.com', 'RETAIL', 'GB', '2024-02-19 09:15:00');
INSERT INTO customers (customer_id, full_name, email, segment, country, created_at) VALUES ('4000cd5a-a08a-580a-a5ff-3f77dba35e98', 'Sofia Marin', 'sofia.marin@example.com', 'RETAIL', 'ES', '2022-12-05 16:45:00');
INSERT INTO customers (customer_id, full_name, email, segment, country, created_at) VALUES ('b2f29e33-5383-5d0e-b81e-da8e81c7bb82', 'Jonas Weber', 'j.weber@weberlogistik.example.com', 'BUSINESS', 'AT', '2020-06-23 11:00:00');
INSERT INTO customers (customer_id, full_name, email, segment, country, created_at) VALUES ('bd083623-c5ad-5ede-a6c1-0b8a8d86a9bd', 'Amira Haddad', 'amira.haddad@example.com', 'PREMIUM', 'FR', '2021-03-14 13:20:00');
INSERT INTO customers (customer_id, full_name, email, segment, country, created_at) VALUES ('a36afcb8-0fa0-53e7-861e-846a46c448de', 'Chen Wei', 'chen.wei@example.com', 'BUSINESS', 'SG', '2023-08-30 08:05:00');

-- Transactions
INSERT INTO transactions (transaction_id, customer_id, activity_type, amount, currency, status, created_at) VALUES
('75f99706-ee90-5c79-898d-87f963c36b92', '3968f964-895e-53af-880b-db8ad20bd114', 'CARD', 64.23, 'EUR', 'COMPLETED', '2026-06-03 18:40:00'),
('91d7bbc4-03f2-5569-816b-6ce3ceaa983b', '3968f964-895e-53af-880b-db8ad20bd114', 'CARD', 52.1, 'EUR', 'COMPLETED', '2026-06-09 01:53:00'),
('7fd50abe-396a-52b1-86ec-623a8c3ad00a', '3968f964-895e-53af-880b-db8ad20bd114', 'CARD', 34.55, 'EUR', 'COMPLETED', '2026-06-13 21:06:00'),
('15ce9899-b7fb-5d27-b82b-11e4ab515356', '3968f964-895e-53af-880b-db8ad20bd114', 'CARD', 18.9, 'EUR', 'COMPLETED', '2026-06-16 04:19:00'),
('7ec53582-03fc-59c0-a8e7-4de18ed90946', '3968f964-895e-53af-880b-db8ad20bd114', 'CARD', 25.99, 'EUR', 'COMPLETED', '2026-06-20 23:32:00'),
('aa16a358-3600-51b6-83e1-e930d9590574', '3968f964-895e-53af-880b-db8ad20bd114', 'CARD', 45.5, 'EUR', 'COMPLETED', '2026-06-26 05:45:00'),
('c538102d-5187-54fc-aab2-c0635da9b714', '3968f964-895e-53af-880b-db8ad20bd114', 'CARD', 71.12, 'EUR', 'COMPLETED', '2026-06-28 00:58:00'),
('03e04e0c-7281-5213-be12-76e3df73a4c6', '3968f964-895e-53af-880b-db8ad20bd114', 'CARD', 89.9, 'EUR', 'COMPLETED', '2026-07-02 20:11:00'),
('46964971-0848-577e-865e-f66e194f8285', '3968f964-895e-53af-880b-db8ad20bd114', 'CARD', 12.99, 'EUR', 'COMPLETED', '2026-07-08 03:24:00'),
('963d235a-386c-588d-801e-ffb7f7546ff8', '3968f964-895e-53af-880b-db8ad20bd114', 'CARD', 60.3, 'EUR', 'COMPLETED', '2026-07-09 22:37:00'),
('4c61ec7f-9d52-565c-8bec-d140ce4af7f0', '3968f964-895e-53af-880b-db8ad20bd114', 'CARD', 58.77, 'EUR', 'COMPLETED', '2026-07-15 04:50:00'),
('b6b88f50-c4f6-50b9-a4bc-2679316a4f63', '3968f964-895e-53af-880b-db8ad20bd114', 'CARD', 4.5, 'EUR', 'COMPLETED', '2026-07-20 00:03:00'),
('00a67f59-59f5-548f-bd52-1d81b165d26e', '3968f964-895e-53af-880b-db8ad20bd114', 'CARD', 149.8, 'EUR', 'COMPLETED', '2026-07-21 19:16:00'),
('20baa5bf-22b2-502a-997a-6d651869d864', '3968f964-895e-53af-880b-db8ad20bd114', 'CARD', 62.44, 'EUR', 'COMPLETED', '2026-07-27 02:29:00'),
('9ed57414-edd6-5cac-9d9e-c8157d80131b', '3968f964-895e-53af-880b-db8ad20bd114', 'CARD', 12.75, 'EUR', 'COMPLETED', '2026-07-31 20:42:00'),
('ccba3181-8cae-5cc2-99c2-871ae8193c15', '3968f964-895e-53af-880b-db8ad20bd114', 'CARD', 79.95, 'EUR', 'COMPLETED', '2026-08-03 03:55:00'),
('07c48798-989c-5961-9718-3236a557e0e8', '3968f964-895e-53af-880b-db8ad20bd114', 'CARD', 68.21, 'EUR', 'COMPLETED', '2026-08-07 23:08:00'),
('a3f75cf7-aafd-5245-92b4-d9a3825de248', '3968f964-895e-53af-880b-db8ad20bd114', 'CARD', 55.55, 'EUR', 'COMPLETED', '2026-08-13 06:21:00'),
('e7801011-e9ca-5420-abdf-31161613c084', '3968f964-895e-53af-880b-db8ad20bd114', 'CARD', 33.9, 'EUR', 'COMPLETED', '2026-08-15 01:34:00'),
('58dcd101-dd5d-5932-88b4-455e62232056', '3968f964-895e-53af-880b-db8ad20bd114', 'CARD', 61.04, 'EUR', 'COMPLETED', '2026-08-19 19:47:00'),
('1c70b2e4-8c08-5948-8a74-43aace96751e', '3968f964-895e-53af-880b-db8ad20bd114', 'PAYMENT', 950.00, 'EUR', 'COMPLETED', '2026-07-01 08:30:00'),
('eb06d316-4ed7-5b4f-941b-ae8173a4822b', '3968f964-895e-53af-880b-db8ad20bd114', 'PAYMENT', 950.00, 'EUR', 'COMPLETED', '2026-08-01 08:30:00'),
('0a4b1bcf-eb4e-52cc-8019-21f04d2b2c16', 'e0083363-bfaa-5bbf-8aea-765587c08aa7', 'PAYMENT', 48200.00, 'EUR', 'COMPLETED', '2026-07-06 10:12:00'),
('3b75bcce-f282-5d1a-805a-fd0770a21dae', 'e0083363-bfaa-5bbf-8aea-765587c08aa7', 'PAYMENT', 22750.00, 'EUR', 'COMPLETED', '2026-07-14 15:47:00'),
('1a23d5ef-2e4b-5b85-90af-5af726e309ef', 'e0083363-bfaa-5bbf-8aea-765587c08aa7', 'PAYMENT', 31000.00, 'EUR', 'COMPLETED', '2026-07-29 09:03:00'),
('0985d91e-b7f5-5559-88d5-235af17031f5', 'e0083363-bfaa-5bbf-8aea-765587c08aa7', 'PAYMENT', 18500.00, 'EUR', 'PENDING', '2026-08-08 11:26:00'),
('30c7c3e9-66ce-5169-a317-069daf2681cb', 'e0083363-bfaa-5bbf-8aea-765587c08aa7', 'PAYMENT', 9900.00, 'EUR', 'COMPLETED', '2026-08-18 09:05:00'),
('ae485cea-4937-54ef-be4e-31710935d8b4', 'e0083363-bfaa-5bbf-8aea-765587c08aa7', 'PAYMENT', 9850.00, 'EUR', 'COMPLETED', '2026-08-18 17:40:00'),
('31512945-3def-52a3-a925-8162af833d39', 'e0083363-bfaa-5bbf-8aea-765587c08aa7', 'PAYMENT', 9975.00, 'EUR', 'COMPLETED', '2026-08-19 10:55:00'),
('ba147ca4-a0d2-5004-a03e-68274209f1ed', 'e0083363-bfaa-5bbf-8aea-765587c08aa7', 'CARD', 425, 'EUR', 'COMPLETED', '2026-06-20 12:00:00'),
('10663847-5e09-5356-a5a4-080040e520d7', 'e0083363-bfaa-5bbf-8aea-765587c08aa7', 'CARD', 689, 'EUR', 'COMPLETED', '2026-06-29 12:00:00'),
('e5a561fe-5eb6-5026-a991-3601c92a92eb', 'e0083363-bfaa-5bbf-8aea-765587c08aa7', 'CARD', 890, 'EUR', 'COMPLETED', '2026-07-08 12:00:00'),
('caf63953-07c3-5853-964b-37d3693ab61c', 'f64a8ac6-baf4-5ffd-b7ce-3256ae699248', 'CRYPTO', 0.42, 'BTC', 'COMPLETED', '2026-07-21 20:15:00'),
('93b0ade1-ec88-5604-9da2-7ab09b17168d', 'f64a8ac6-baf4-5ffd-b7ce-3256ae699248', 'CRYPTO', 6.10, 'ETH', 'COMPLETED', '2026-07-25 21:40:00'),
('001ed70f-7c4b-5f3f-9a5b-137732432c41', 'f64a8ac6-baf4-5ffd-b7ce-3256ae699248', 'CRYPTO', 0.35, 'BTC', 'COMPLETED', '2026-08-02 19:05:00'),
('d5f6ccaa-a536-527d-840a-e10360e646e6', 'f64a8ac6-baf4-5ffd-b7ce-3256ae699248', 'CRYPTO', 0.40, 'BTC', 'COMPLETED', '2026-08-20 02:10:00'),
('4dfbdfe5-f67a-5eaf-8a76-9c0b3e4169f0', 'f64a8ac6-baf4-5ffd-b7ce-3256ae699248', 'CRYPTO', 3.20, 'ETH', 'COMPLETED', '2026-08-20 02:55:00'),
('d0ffa37b-5716-5d71-8d34-7263838539e7', 'f64a8ac6-baf4-5ffd-b7ce-3256ae699248', 'CRYPTO', 2.75, 'ETH', 'COMPLETED', '2026-08-20 11:30:00'),
('44bb5a2c-a9d0-54aa-8f15-1db13a03b657', 'f64a8ac6-baf4-5ffd-b7ce-3256ae699248', 'CRYPTO', 0.31, 'BTC', 'COMPLETED', '2026-08-20 22:05:00'),
('23073ddd-8a96-5e58-b50f-0dd873b97637', 'f64a8ac6-baf4-5ffd-b7ce-3256ae699248', 'CRYPTO', 1.90, 'ETH', 'FAILED', '2026-08-21 00:45:00'),
('0d83c18f-618a-550b-bbc6-7f2de279f93a', 'f64a8ac6-baf4-5ffd-b7ce-3256ae699248', 'CARD', 43.2, 'GBP', 'COMPLETED', '2026-08-05 13:00:00'),
('dbaa73b3-5486-5468-8f93-49709d08fbe4', 'f64a8ac6-baf4-5ffd-b7ce-3256ae699248', 'CARD', 8.9, 'GBP', 'COMPLETED', '2026-08-08 13:00:00'),
('292fb736-3f8d-5dab-abca-9fecd6e78a3b', 'f64a8ac6-baf4-5ffd-b7ce-3256ae699248', 'CARD', 23.4, 'GBP', 'COMPLETED', '2026-08-11 13:00:00'),
('f25ef868-6a5a-570c-85c0-456df19810fd', '4000cd5a-a08a-580a-a5ff-3f77dba35e98', 'CARD', 1.00, 'EUR', 'FAILED', '2026-08-24 03:12:00'),
('875af0f0-65ba-58c2-b3b4-a5a7544fb31b', '4000cd5a-a08a-580a-a5ff-3f77dba35e98', 'CARD', 49.99, 'EUR', 'FAILED', '2026-08-24 03:21:00'),
('bb4e7a7a-bd20-552a-9bcc-14a20efe4869', '4000cd5a-a08a-580a-a5ff-3f77dba35e98', 'CARD', 120.00, 'EUR', 'FAILED', '2026-08-24 03:34:00'),
('c37de595-1bc8-5e3a-889e-37b93be9093c', '4000cd5a-a08a-580a-a5ff-3f77dba35e98', 'CARD', 300.00, 'EUR', 'FAILED', '2026-08-24 03:50:00'),
('b58e5bc6-30fe-584e-bd2e-043da4c3680e', '4000cd5a-a08a-580a-a5ff-3f77dba35e98', 'CARD', 2450.00, 'EUR', 'COMPLETED', '2026-08-22 23:55:00'),
('ae6afa1e-d910-5381-a0a6-b82fe7747e99', '4000cd5a-a08a-580a-a5ff-3f77dba35e98', 'CARD', 3200.00, 'EUR', 'COMPLETED', '2026-08-23 00:40:00'),
('cfe709cd-9417-5cdc-b56b-f0245ce466ec', '4000cd5a-a08a-580a-a5ff-3f77dba35e98', 'CARD', 500.00, 'EUR', 'COMPLETED', '2026-08-10 22:30:00');
INSERT INTO transactions (transaction_id, customer_id, activity_type, amount, currency, status, created_at) VALUES
('89842227-b44b-5613-83d8-9fa9168d7f97', '4000cd5a-a08a-580a-a5ff-3f77dba35e98', 'CARD', 750.00, 'EUR', 'COMPLETED', '2026-08-16 23:15:00'),
('6895090f-c604-5bb6-9bf2-a958a6f6d027', '4000cd5a-a08a-580a-a5ff-3f77dba35e98', 'CARD', 1000.00, 'EUR', 'COMPLETED', '2026-08-23 01:05:00'),
('33e1b516-29e6-5170-8bf8-b91eaf71d792', '4000cd5a-a08a-580a-a5ff-3f77dba35e98', 'CARD', 3200.00, 'EUR', 'REVERSED', '2026-08-26 10:00:00'),
('f119fd41-c3b4-5917-b057-a715af8e95bc', '4000cd5a-a08a-580a-a5ff-3f77dba35e98', 'CARD', 52.3, 'EUR', 'COMPLETED', '2026-08-04 18:00:00'),
('41588cff-19d4-591f-ba81-10bbeaaeacbc', '4000cd5a-a08a-580a-a5ff-3f77dba35e98', 'CARD', 44.1, 'EUR', 'COMPLETED', '2026-08-10 18:00:00'),
('d6145393-0bb6-5fe2-a900-20d5cc0843da', '4000cd5a-a08a-580a-a5ff-3f77dba35e98', 'CARD', 61.8, 'EUR', 'COMPLETED', '2026-08-16 18:00:00'),
('b503e3b0-e183-544e-a6a6-2a677db6442f', 'b2f29e33-5383-5d0e-b81e-da8e81c7bb82', 'PAYMENT', 4890.00, 'EUR', 'COMPLETED', '2026-06-10 09:00:00'),
('9f5eb347-06d1-5ea3-97b0-27023b411811', 'b2f29e33-5383-5d0e-b81e-da8e81c7bb82', 'PAYMENT', 7420.00, 'EUR', 'COMPLETED', '2026-07-10 09:00:00'),
('480eb08c-8931-560b-99f8-168d4ba5e83c', 'b2f29e33-5383-5d0e-b81e-da8e81c7bb82', 'PAYMENT', 6980.00, 'EUR', 'COMPLETED', '2026-07-22 09:30:00'),
('fbf31277-5330-5e53-8d68-529a1359185f', 'b2f29e33-5383-5d0e-b81e-da8e81c7bb82', 'PAYMENT', 54000.00, 'EUR', 'COMPLETED', '2026-08-05 14:20:00'),
('a9308aed-8d8e-5b2d-aab6-97c13e515b5a', 'b2f29e33-5383-5d0e-b81e-da8e81c7bb82', 'PAYMENT', 7420.00, 'EUR', 'COMPLETED', '2026-08-10 09:00:00'),
('acb194d9-16b0-58ce-8c96-fb669f3e2e12', 'b2f29e33-5383-5d0e-b81e-da8e81c7bb82', 'PAYMENT', 2150.00, 'EUR', 'PENDING', '2026-08-25 09:00:00'),
('69a4f276-17f8-535a-886e-5b317301d810', 'b2f29e33-5383-5d0e-b81e-da8e81c7bb82', 'CARD', 98.02, 'EUR', 'COMPLETED', '2026-07-02 11:00:00'),
('36ef817f-2f25-5fa4-807e-0cb9c11e0cad', 'b2f29e33-5383-5d0e-b81e-da8e81c7bb82', 'CARD', 412.3, 'EUR', 'COMPLETED', '2026-07-13 11:00:00'),
('8488220a-b44b-5069-990f-230fae2c2432', 'b2f29e33-5383-5d0e-b81e-da8e81c7bb82', 'CARD', 512, 'EUR', 'COMPLETED', '2026-07-24 11:00:00'),
('13070b70-722e-5525-8b15-d1a3b3a284c8', 'bd083623-c5ad-5ede-a6c1-0b8a8d86a9bd', 'PAYMENT', 120.00, 'EUR', 'COMPLETED', '2026-06-15 10:00:00'),
('8b351da4-9e66-5925-9902-8792425a6547', 'bd083623-c5ad-5ede-a6c1-0b8a8d86a9bd', 'CARD', 64.50, 'EUR', 'COMPLETED', '2026-07-08 12:30:00'),
('bcb33b14-bc1c-5726-897e-8099b45c5a4b', 'bd083623-c5ad-5ede-a6c1-0b8a8d86a9bd', 'PAYMENT', 5000.00, 'EUR', 'COMPLETED', '2026-08-22 10:00:00'),
('072a3f90-3dc9-54ff-a979-28cbb18afa3e', 'bd083623-c5ad-5ede-a6c1-0b8a8d86a9bd', 'PAYMENT', 8000.00, 'EUR', 'COMPLETED', '2026-08-22 16:30:00'),
('c2a17fcb-dc9d-51af-9585-8271c989a327', 'bd083623-c5ad-5ede-a6c1-0b8a8d86a9bd', 'PAYMENT', 5000.00, 'EUR', 'COMPLETED', '2026-08-23 09:45:00'),
('1cdee8c1-b189-5193-bff9-ac96be8913f1', 'bd083623-c5ad-5ede-a6c1-0b8a8d86a9bd', 'PAYMENT', 10000.00, 'EUR', 'COMPLETED', '2026-08-24 11:15:00'),
('db4d3453-2e43-5be5-8d75-5d798954ac94', 'bd083623-c5ad-5ede-a6c1-0b8a8d86a9bd', 'PAYMENT', 6000.00, 'EUR', 'COMPLETED', '2026-08-25 14:50:00'),
('c299ac42-5dcc-5993-9a64-f21762ca4582', 'bd083623-c5ad-5ede-a6c1-0b8a8d86a9bd', 'PAYMENT', 5000.00, 'EUR', 'COMPLETED', '2026-08-26 09:20:00'),
('11528d77-59d7-5093-9e92-4e1b907cfb61', 'a36afcb8-0fa0-53e7-861e-846a46c448de', 'CRYPTO', 0.85, 'ETH', 'COMPLETED', '2026-06-07 21:00:00'),
('69bab9b9-35b6-5765-a11f-aed7366b5658', 'a36afcb8-0fa0-53e7-861e-846a46c448de', 'CRYPTO', 0.85, 'ETH', 'COMPLETED', '2026-06-21 21:00:00'),
('b5341388-9161-5599-892b-73b7d057966c', 'a36afcb8-0fa0-53e7-861e-846a46c448de', 'CRYPTO', 0.85, 'ETH', 'COMPLETED', '2026-07-05 21:00:00'),
('2b086c86-ad4f-5b3a-84c8-70f9df10de1f', 'a36afcb8-0fa0-53e7-861e-846a46c448de', 'CRYPTO', 0.85, 'ETH', 'COMPLETED', '2026-07-19 21:00:00'),
('bab98675-60a0-5c9b-bd74-e0a52308b8bb', 'a36afcb8-0fa0-53e7-861e-846a46c448de', 'CRYPTO', 0.85, 'ETH', 'COMPLETED', '2026-08-02 21:00:00'),
('992722cc-ed0f-59d3-8157-d354546d1009', 'a36afcb8-0fa0-53e7-861e-846a46c448de', 'CRYPTO', 0.85, 'ETH', 'COMPLETED', '2026-08-16 21:00:00'),
('ea974031-b76f-52c7-9c07-46c47dd5aba7', 'a36afcb8-0fa0-53e7-861e-846a46c448de', 'CARD', 18.7, 'SGD', 'COMPLETED', '2026-07-14 08:30:00'),
('bfb55e60-acdc-5968-a261-0b7907be06e7', 'a36afcb8-0fa0-53e7-861e-846a46c448de', 'CARD', 89.3, 'SGD', 'COMPLETED', '2026-07-26 08:30:00'),
('647cf2c8-1eda-5937-af14-69b59fafc12d', 'a36afcb8-0fa0-53e7-861e-846a46c448de', 'CARD', 875, 'SGD', 'COMPLETED', '2026-08-07 08:30:00');

-- Card activity details
INSERT INTO card_activity (transaction_id, card_pan, card_type, merchant_name, mcc_code, card_present, authorization_code, decline_reason) VALUES
('75f99706-ee90-5c79-898d-87f963c36b92', '****4821', 'Debit', 'Esselunga Milano', '5411', TRUE, 'A443358', NULL),
('91d7bbc4-03f2-5569-816b-6ce3ceaa983b', '****4821', 'Debit', 'Eni Station 4412', '5541', TRUE, 'A728085', NULL),
('7fd50abe-396a-52b1-86ec-623a8c3ad00a', '****4821', 'Debit', 'Trattoria da Gino', '5812', TRUE, 'A596418', NULL),
('15ce9899-b7fb-5d27-b82b-11e4ab515356', '****4821', 'Debit', 'Farmacia Centrale', '5912', TRUE, 'A547012', NULL),
('7ec53582-03fc-59c0-a8e7-4de18ed90946', '****4821', 'Debit', 'Amazon Marketplace', '5942', FALSE, 'A527363', NULL),
('aa16a358-3600-51b6-83e1-e930d9590574', '****4821', 'Debit', 'Trenitalia', '4112', TRUE, 'A555553', NULL),
('c538102d-5187-54fc-aab2-c0635da9b714', '****4821', 'Debit', 'Esselunga Milano', '5411', TRUE, 'A329829', NULL),
('03e04e0c-7281-5213-be12-76e3df73a4c6', '****4821', 'Debit', 'Decathlon Milano', '5941', TRUE, 'A168504', NULL),
('46964971-0848-577e-865e-f66e194f8285', '****4821', 'Debit', 'Netflix.com', '4899', FALSE, 'A996892', NULL),
('963d235a-386c-588d-801e-ffb7f7546ff8', '****4821', 'Debit', 'Eni Station 4412', '5541', TRUE, 'A170368', NULL),
('4c61ec7f-9d52-565c-8bec-d140ce4af7f0', '****4821', 'Debit', 'Esselunga Milano', '5411', TRUE, 'A479942', NULL),
('b6b88f50-c4f6-50b9-a4bc-2679316a4f63', '****4821', 'Debit', 'Bar Centrale', '5812', TRUE, 'A291326', NULL),
('00a67f59-59f5-548f-bd52-1d81b165d26e', '****4821', 'Debit', 'Ikea Corsico', '5712', TRUE, 'A264847', NULL),
('20baa5bf-22b2-502a-997a-6d651869d864', '****4821', 'Debit', 'Esselunga Milano', '5411', TRUE, 'A623324', NULL),
('9ed57414-edd6-5cac-9d9e-c8157d80131b', '****4821', 'Debit', 'Autogrill A4', '5812', TRUE, 'A221373', NULL),
('ccba3181-8cae-5cc2-99c2-871ae8193c15', '****4821', 'Debit', 'Zara Milano', '5651', TRUE, 'A164977', NULL),
('07c48798-989c-5961-9718-3236a557e0e8', '****4821', 'Debit', 'Esselunga Milano', '5411', TRUE, 'A711972', NULL),
('a3f75cf7-aafd-5245-92b4-d9a3825de248', '****4821', 'Debit', 'Eni Station 4412', '5541', TRUE, 'A556569', NULL),
('e7801011-e9ca-5420-abdf-31161613c084', '****4821', 'Debit', 'Libreria Feltrinelli', '5942', TRUE, 'A747190', NULL),
('58dcd101-dd5d-5932-88b4-455e62232056', '****4821', 'Debit', 'Esselunga Milano', '5411', TRUE, 'A497896', NULL),
('ba147ca4-a0d2-5004-a03e-68274209f1ed', '****9034', 'Credit', 'KaDeWe Berlin', '5311', TRUE, 'A433436', NULL),
('10663847-5e09-5356-a5a4-080040e520d7', '****9034', 'Credit', 'Lufthansa', '3010', TRUE, 'A295783', NULL),
('e5a561fe-5eb6-5026-a991-3601c92a92eb', '****9034', 'Credit', 'Hotel Adlon', '7011', TRUE, 'A126563', NULL),
('0d83c18f-618a-550b-bbc6-7f2de279f93a', '****7710', 'Debit', 'Tesco London', '5411', TRUE, 'A458055', NULL),
('dbaa73b3-5486-5468-8f93-49709d08fbe4', '****7710', 'Debit', 'TfL Travel', '4111', TRUE, 'A816260', NULL),
('292fb736-3f8d-5dab-abca-9fecd6e78a3b', '****7710', 'Debit', 'Deliveroo', '5812', FALSE, 'A111881', NULL),
('f25ef868-6a5a-570c-85c0-456df19810fd', '****2287', 'Credit', 'AliExpress', '5999', FALSE, NULL, 'Insufficient funds'),
('875af0f0-65ba-58c2-b3b4-a5a7544fb31b', '****2287', 'Credit', 'Steam Games', '5816', FALSE, NULL, 'Suspected fraud - issuer rule'),
('bb4e7a7a-bd20-552a-9bcc-14a20efe4869', '****2287', 'Credit', 'eDreams', '4722', FALSE, NULL, 'Suspected fraud - issuer rule'),
('c37de595-1bc8-5e3a-889e-37b93be9093c', '****2287', 'Credit', 'Fnac.es', '5732', FALSE, NULL, 'Card blocked'),
('b58e5bc6-30fe-584e-bd2e-043da4c3680e', '****2287', 'Credit', 'MediaMarkt Online', '5732', FALSE, 'A908081', NULL),
('ae6afa1e-d910-5381-a0a6-b82fe7747e99', '****2287', 'Credit', 'Apple Store Online', '5732', FALSE, 'A640272', NULL),
('cfe709cd-9417-5cdc-b56b-f0245ce466ec', '****2287', 'Credit', 'BetWinner Casino', '7995', FALSE, 'A839427', NULL),
('89842227-b44b-5613-83d8-9fa9168d7f97', '****2287', 'Credit', 'BetWinner Casino', '7995', FALSE, 'A815048', NULL),
('6895090f-c604-5bb6-9bf2-a958a6f6d027', '****2287', 'Credit', 'BetWinner Casino', '7995', FALSE, 'A229763', NULL),
('33e1b516-29e6-5170-8bf8-b91eaf71d792', '****2287', 'Credit', 'Apple Store Online', '5732', FALSE, 'A784638', NULL),
('f119fd41-c3b4-5917-b057-a715af8e95bc', '****2287', 'Credit', 'Mercadona', '5411', TRUE, 'A421551', NULL),
('41588cff-19d4-591f-ba81-10bbeaaeacbc', '****2287', 'Credit', 'Repsol', '5541', TRUE, 'A691778', NULL),
('d6145393-0bb6-5fe2-a900-20d5cc0843da', '****2287', 'Credit', 'Mercadona', '5411', TRUE, 'A823644', NULL),
('69a4f276-17f8-535a-886e-5b317301d810', '****5142', 'Credit', 'OMV Tankstelle', '5541', TRUE, 'A867337', NULL),
('36ef817f-2f25-5fa4-807e-0cb9c11e0cad', '****5142', 'Credit', 'Metro Cash&Carry', '5300', TRUE, 'A836623', NULL),
('8488220a-b44b-5069-990f-230fae2c2432', '****5142', 'Credit', 'Austrian Airlines', '3075', TRUE, 'A153470', NULL),
('8b351da4-9e66-5925-9902-8792425a6547', '****3319', 'Debit', 'Carrefour Paris', '5411', TRUE, 'A103592', NULL),
('ea974031-b76f-52c7-9c07-46c47dd5aba7', '****6650', 'Credit', 'Grab Singapore', '4121', FALSE, 'A950649', NULL),
('bfb55e60-acdc-5968-a261-0b7907be06e7', '****6650', 'Credit', 'NTUC FairPrice', '5411', TRUE, 'A680827', NULL),
('647cf2c8-1eda-5937-af14-69b59fafc12d', '****6650', 'Credit', 'Singapore Airlines', '3016', TRUE, 'A931925', NULL);

-- Payment activity details
INSERT INTO payment_activity (transaction_id, payment_method, sender_account, receiver_account, receiver_bank_country) VALUES
('1c70b2e4-8c08-5948-8a74-43aace96751e', 'SEPA', 'IT60X0542811101000000123456', 'IT12A0306909606100000064122', 'IT'),
('eb06d316-4ed7-5b4f-941b-ae8173a4822b', 'SEPA', 'IT60X0542811101000000123456', 'IT12A0306909606100000064122', 'IT'),
('0a4b1bcf-eb4e-52cc-8019-21f04d2b2c16', 'SWIFT', 'DE89370400440532013000', 'AE070331234567890123456', 'AE'),
('3b75bcce-f282-5d1a-805a-fd0770a21dae', 'SWIFT', 'DE89370400440532013000', 'TR330006100519786457841326', 'TR'),
('1a23d5ef-2e4b-5b85-90af-5af726e309ef', 'SWIFT', 'DE89370400440532013000', 'AE070331234567890123456', 'AE'),
('0985d91e-b7f5-5559-88d5-235af17031f5', 'SWIFT', 'DE89370400440532013000', 'HK000123456789012345678', 'HK'),
('30c7c3e9-66ce-5169-a317-069daf2681cb', 'WIRE', 'DE89370400440532013000', 'GE29NB0000000101904917', 'GE'),
('ae485cea-4937-54ef-be4e-31710935d8b4', 'WIRE', 'DE89370400440532013000', 'GE29NB0000000101904917', 'GE'),
('31512945-3def-52a3-a925-8162af833d39', 'WIRE', 'DE89370400440532013000', 'GE29NB0000000101904917', 'GE'),
('b503e3b0-e183-544e-a6a6-2a677db6442f', 'SEPA', 'AT611904300234573201', 'DE02120300000000202051', 'DE'),
('9f5eb347-06d1-5ea3-97b0-27023b411811', 'SEPA', 'AT611904300234573201', 'DE02120300000000202051', 'DE'),
('480eb08c-8931-560b-99f8-168d4ba5e83c', 'SEPA', 'AT611904300234573201', 'CZ6508000000192000145399', 'CZ'),
('fbf31277-5330-5e53-8d68-529a1359185f', 'SWIFT', 'AT611904300234573201', 'US64SVBKUS6S3300958879', 'US'),
('a9308aed-8d8e-5b2d-aab6-97c13e515b5a', 'SEPA', 'AT611904300234573201', 'DE02120300000000202051', 'DE'),
('acb194d9-16b0-58ce-8c96-fb669f3e2e12', 'SEPA', 'AT611904300234573201', 'AT483200000012345864', 'AT'),
('13070b70-722e-5525-8b15-d1a3b3a284c8', 'SEPA', 'FR1420041010050500013M02606', 'FR7630006000011234567890189', 'FR'),
('bcb33b14-bc1c-5726-897e-8099b45c5a4b', 'P2P', 'FR1420041010050500013M02606', 'FR7630004000031234567890143', 'FR'),
('072a3f90-3dc9-54ff-a979-28cbb18afa3e', 'P2P', 'FR1420041010050500013M02606', 'BE68539007547034', 'BE'),
('c2a17fcb-dc9d-51af-9585-8271c989a327', 'P2P', 'FR1420041010050500013M02606', 'FR7630004000031234567890143', 'FR'),
('1cdee8c1-b189-5193-bff9-ac96be8913f1', 'P2P', 'FR1420041010050500013M02606', 'LU280019400644750000', 'LU'),
('db4d3453-2e43-5be5-8d75-5d798954ac94', 'P2P', 'FR1420041010050500013M02606', 'FR7630004000031234567890143', 'FR'),
('c299ac42-5dcc-5993-9a64-f21762ca4582', 'P2P', 'FR1420041010050500013M02606', 'BE68539007547034', 'BE');

-- Crypto activity details
INSERT INTO crypto_activity (transaction_id, blockchain, wallet_address_from, wallet_address_to, tx_hash, exchange_name) VALUES
('caf63953-07c3-5853-964b-37d3693ab61c', 'BTC', 'exchange-hot-wallet', 'bc1q8d371f8592ba55e69da45308eeb202', '0xb0af2dc292fe57b89fedc1d3a04c3a17fcf384bc', 'Kraken'),
('93b0ade1-ec88-5604-9da2-7ab09b17168d', 'ETH', 'exchange-hot-wallet', '0xc14e15fd62325d97ad7beb7c4d30e1cef366e087', '0x083c6c58afb05f83823a82ddc4e3a6b280809a7f', 'Kraken'),
('001ed70f-7c4b-5f3f-9a5b-137732432c41', 'BTC', 'exchange-hot-wallet', 'bc1q8d371f8592ba55e69da45308eeb202', '0xae0302261e635bd4b7a37a77a7b141eed39bf6c3', 'Binance'),
('d5f6ccaa-a536-527d-840a-e10360e646e6', 'BTC', 'bc1q8d371f8592ba55e69da45308eeb202', 'bc1qd562161d8b8f59858c238160ef15de', '0x01434338173b5de38372ee571239f9850e275f0e', NULL),
('4dfbdfe5-f67a-5eaf-8a76-9c0b3e4169f0', 'ETH', '0xc14e15fd62325d97ad7beb7c4d30e1cef366e087', '0xe67641025b565d24a75dc5d56b5b28ce2620d835', '0xf10224f7575c537693cb3cde1057d6ceddddcbb3', NULL),
('d0ffa37b-5716-5d71-8d34-7263838539e7', 'ETH', '0xc14e15fd62325d97ad7beb7c4d30e1cef366e087', '0xff721d28dcb85b89b9ce59d81b3ccaa88105c14b', '0x9df00484a50b5d5d83c1ffc42e6c7084b6b5ce6e', NULL),
('44bb5a2c-a9d0-54aa-8f15-1db13a03b657', 'BTC', 'bc1q8d371f8592ba55e69da45308eeb202', 'bc1qb8ea008cdcea5ebfac3ed285145091', '0x249f5e6791f25436852e12686747c6a0cc8c0821', NULL),
('23073ddd-8a96-5e58-b50f-0dd873b97637', 'ETH', '0xc14e15fd62325d97ad7beb7c4d30e1cef366e087', '0xe67641025b565d24a75dc5d56b5b28ce2620d835', '0x6e8babd7bc8a5dc6a76c9c1211220d5e6a3ad730', NULL),
('11528d77-59d7-5093-9e92-4e1b907cfb61', 'ETH', 'exchange-hot-wallet', '0xa565c2e154fc5111a85af66df63867fd7eab84e5', '0x852bbc4c726b54bca44bad765a3438e9981ec363', 'Coinbase'),
('69bab9b9-35b6-5765-a11f-aed7366b5658', 'ETH', 'exchange-hot-wallet', '0xa565c2e154fc5111a85af66df63867fd7eab84e5', '0x0285c9ad916a5cd8b560ed15b05b9b22fed35541', 'Coinbase'),
('b5341388-9161-5599-892b-73b7d057966c', 'ETH', 'exchange-hot-wallet', '0xa565c2e154fc5111a85af66df63867fd7eab84e5', '0xbf60e240934551f49b3d19e8deb8dc04476faa45', 'Coinbase'),
('2b086c86-ad4f-5b3a-84c8-70f9df10de1f', 'ETH', 'exchange-hot-wallet', '0xa565c2e154fc5111a85af66df63867fd7eab84e5', '0xa47f10a625f853778b67a17238e59eee004e6157', 'Coinbase'),
('bab98675-60a0-5c9b-bd74-e0a52308b8bb', 'ETH', 'exchange-hot-wallet', '0xa565c2e154fc5111a85af66df63867fd7eab84e5', '0x5b4a4291446d5128ad145f162a98cec47cd584cb', 'Coinbase'),
('992722cc-ed0f-59d3-8157-d354546d1009', 'ETH', 'exchange-hot-wallet', '0xa565c2e154fc5111a85af66df63867fd7eab84e5', '0x9ee9b9ca223b55a3bd7cfe65b8901271c4439f9b', 'Coinbase');

-- Risk assessments (rules fired on seeded patterns)
INSERT INTO risk_assessments (assessment_id, transaction_id, rule_id, triggered_at, score_contribution) VALUES
('b5a9ece5-4b66-5d74-b910-98c2fce04c27', '0a4b1bcf-eb4e-52cc-8019-21f04d2b2c16', 'daacee21-dcef-5664-854b-8d229493c1a8', '2026-07-06 10:14:00', 25),
('9d0ab87e-f4e5-5c4a-91b4-2b4da079a610', '3b75bcce-f282-5d1a-805a-fd0770a21dae', 'daacee21-dcef-5664-854b-8d229493c1a8', '2026-07-14 15:49:00', 25),
('8fb8fcc0-dc6f-5cb8-a412-ecb1100bbe69', '1a23d5ef-2e4b-5b85-90af-5af726e309ef', 'daacee21-dcef-5664-854b-8d229493c1a8', '2026-07-29 09:05:00', 25),
('646f3d0a-e733-5bb8-95fa-4bcbb832a28d', '1a23d5ef-2e4b-5b85-90af-5af726e309ef', 'c8300eeb-e8bf-5b4a-aa16-8747fd374cdc', '2026-07-29 09:05:00', 10),
('73a55915-0ef2-5a67-991e-54b28f7d24c1', '0985d91e-b7f5-5559-88d5-235af17031f5', 'daacee21-dcef-5664-854b-8d229493c1a8', '2026-08-08 11:28:00', 25),
('ce01eb7e-b083-59f8-a4e9-fa542cecb77e', '30c7c3e9-66ce-5169-a317-069daf2681cb', 'ca5b6c7e-ad58-524c-a5d2-ed6bda83b320', '2026-08-18 09:07:00', 20),
('ec0d2319-1c4d-5814-a6d4-86554afed769', 'ae485cea-4937-54ef-be4e-31710935d8b4', 'ca5b6c7e-ad58-524c-a5d2-ed6bda83b320', '2026-08-18 17:42:00', 20),
('767550c6-3bad-54d0-9514-43a7c837db3b', '31512945-3def-52a3-a925-8162af833d39', 'ca5b6c7e-ad58-524c-a5d2-ed6bda83b320', '2026-08-19 10:57:00', 20),
('cb99b4e1-f4f7-55d2-90d4-41754cb7eb82', 'd5f6ccaa-a536-527d-840a-e10360e646e6', '302df05c-8e9e-51ba-99c9-9d47af3d8ca7', '2026-08-20 02:12:00', 25),
('a2e0b3d5-292d-58ce-b82b-ba254b963fb6', 'd5f6ccaa-a536-527d-840a-e10360e646e6', 'a2468bf9-4383-50ce-9dd4-3793daf245b5', '2026-08-20 02:12:00', 10),
('6e850121-e3bf-5af5-9c0a-310f019b7bca', '4dfbdfe5-f67a-5eaf-8a76-9c0b3e4169f0', '302df05c-8e9e-51ba-99c9-9d47af3d8ca7', '2026-08-20 02:57:00', 25),
('84b98ab5-4288-5f75-9206-52ed8a352316', '4dfbdfe5-f67a-5eaf-8a76-9c0b3e4169f0', 'f398af64-5145-58f2-820c-b6107e54aea5', '2026-08-20 02:57:00', 35),
('d97be14d-3018-5541-950c-9a634870a818', 'd0ffa37b-5716-5d71-8d34-7263838539e7', '302df05c-8e9e-51ba-99c9-9d47af3d8ca7', '2026-08-20 11:32:00', 25),
('ec4120d0-afe4-56e0-8daf-1009c5f60415', 'd0ffa37b-5716-5d71-8d34-7263838539e7', 'a2468bf9-4383-50ce-9dd4-3793daf245b5', '2026-08-20 11:32:00', 10),
('3b5d0912-a1f3-5c36-a707-eaac0eece191', '44bb5a2c-a9d0-54aa-8f15-1db13a03b657', '302df05c-8e9e-51ba-99c9-9d47af3d8ca7', '2026-08-20 22:07:00', 25),
('5efa1b8e-0190-5cb3-a909-dc3f8508eb7c', '23073ddd-8a96-5e58-b50f-0dd873b97637', 'f398af64-5145-58f2-820c-b6107e54aea5', '2026-08-21 00:47:00', 35),
('ad78697c-45ce-521c-942d-f30ce50efdac', 'f25ef868-6a5a-570c-85c0-456df19810fd', '981996a6-394b-5a44-b6ad-88b20b0165c1', '2026-08-24 03:14:00', 15),
('20f4dbd6-f3e7-51ce-ba92-fcabd319d5a1', '875af0f0-65ba-58c2-b3b4-a5a7544fb31b', '981996a6-394b-5a44-b6ad-88b20b0165c1', '2026-08-24 03:23:00', 15),
('b9e73198-6773-5fb5-84ca-c5dc022f5c6c', 'bb4e7a7a-bd20-552a-9bcc-14a20efe4869', '981996a6-394b-5a44-b6ad-88b20b0165c1', '2026-08-24 03:36:00', 15),
('e5da162e-8cae-52a1-82f0-339a6faf2caa', 'c37de595-1bc8-5e3a-889e-37b93be9093c', '981996a6-394b-5a44-b6ad-88b20b0165c1', '2026-08-24 03:52:00', 15),
('f3f44f1a-b54c-5400-9cf8-67e41bb98d6f', 'b58e5bc6-30fe-584e-bd2e-043da4c3680e', '34c126a4-59ad-5b66-b7a7-d389072056b9', '2026-08-22 23:57:00', 10),
('ee7f9b45-9912-5bf2-8a18-640420687b1a', 'ae6afa1e-d910-5381-a0a6-b82fe7747e99', '34c126a4-59ad-5b66-b7a7-d389072056b9', '2026-08-23 00:42:00', 10),
('740e9e86-a721-57fd-ba93-0453bfe2db5c', 'cfe709cd-9417-5cdc-b56b-f0245ce466ec', '5432f05a-3ece-5c53-8ae9-41de3af81a2c', '2026-08-10 22:32:00', 15),
('e7291879-59f4-5a3c-bfde-662972c07522', '89842227-b44b-5613-83d8-9fa9168d7f97', '5432f05a-3ece-5c53-8ae9-41de3af81a2c', '2026-08-16 23:17:00', 15),
('997ce655-1c10-5337-9e18-9731a603a1d4', '6895090f-c604-5bb6-9bf2-a958a6f6d027', '5432f05a-3ece-5c53-8ae9-41de3af81a2c', '2026-08-23 01:07:00', 15),
('48730a26-227b-5774-84f7-af322f5828d9', 'fbf31277-5330-5e53-8d68-529a1359185f', 'daacee21-dcef-5664-854b-8d229493c1a8', '2026-08-05 14:22:00', 25),
('402e32c7-dde5-5b99-9df2-865702a77f6d', 'bcb33b14-bc1c-5726-897e-8099b45c5a4b', '2f60808a-ef61-5127-b3d5-a7cdbcc9b8d0', '2026-08-22 10:02:00', 20),
('9ffda8b3-5a4e-5734-b9a4-970dc0ec959b', 'bcb33b14-bc1c-5726-897e-8099b45c5a4b', 'c8300eeb-e8bf-5b4a-aa16-8747fd374cdc', '2026-08-22 10:02:00', 10),
('1d3dd0d6-3250-5e11-9bf0-822a83c5651d', '072a3f90-3dc9-54ff-a979-28cbb18afa3e', 'c8300eeb-e8bf-5b4a-aa16-8747fd374cdc', '2026-08-22 16:32:00', 10),
('0fec8408-1c41-53f9-bd20-4ec9cf53ca44', 'c2a17fcb-dc9d-51af-9585-8271c989a327', 'c8300eeb-e8bf-5b4a-aa16-8747fd374cdc', '2026-08-23 09:47:00', 10),
('45766522-634b-5412-b6ad-c4570ab4e95a', '1cdee8c1-b189-5193-bff9-ac96be8913f1', '2f60808a-ef61-5127-b3d5-a7cdbcc9b8d0', '2026-08-24 11:17:00', 20),
('01b4cc21-1b01-5dcc-97b5-b53f54ba1ee0', '1cdee8c1-b189-5193-bff9-ac96be8913f1', 'c8300eeb-e8bf-5b4a-aa16-8747fd374cdc', '2026-08-24 11:17:00', 10),
('834f68b4-0cd2-583f-913e-321ec8b7d019', 'db4d3453-2e43-5be5-8d75-5d798954ac94', 'c8300eeb-e8bf-5b4a-aa16-8747fd374cdc', '2026-08-25 14:52:00', 10),
('36f8ef18-3d02-50a0-ac84-fbb94e85ec78', 'c299ac42-5dcc-5993-9a64-f21762ca4582', 'c8300eeb-e8bf-5b4a-aa16-8747fd374cdc', '2026-08-26 09:22:00', 10);

