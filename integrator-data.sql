--
-- PostgreSQL database dump
--

\restrict ueynZfe5ZcG3cjPydlorXZsTNpjpAyMneDnpH053ZPtFL2FP7UKuebF8mCGiIW0

-- Dumped from database version 18.3 (Debian 18.3-1.pgdg13+1)
-- Dumped by pg_dump version 18.3 (Debian 18.3-1.pgdg13+1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Data for Name: routes; Type: TABLE DATA; Schema: route; Owner: route_svc
--

INSERT INTO route.routes (id, name, description, path_pattern, http_method, target_url, transform_type, field_mapping_config, snippet_id, enabled, created_at, updated_at, rate_limit_enabled, rate_limit_replenish_rate, rate_limit_burst_capacity, rate_limit_requested_tokens) VALUES ('485b2ff8-e3c4-41d5-939e-845d25f9b926', 'route_1780246357827', 'Primary route for automated route-service tests', '/orders/1780200446010', 'POST', 'https://jsonplaceholder.typicode.com/posts/1', 'NONE', NULL, NULL, true, '2026-05-31 22:22:37.960975+05:30', '2026-05-31 22:22:37.960975+05:30', false, NULL, NULL, NULL);
INSERT INTO route.routes (id, name, description, path_pattern, http_method, target_url, transform_type, field_mapping_config, snippet_id, enabled, created_at, updated_at, rate_limit_enabled, rate_limit_replenish_rate, rate_limit_burst_capacity, rate_limit_requested_tokens) VALUES ('fe1668a6-c1e5-486b-b03e-5db1823da495', 'route_1780246383944', 'Primary route for automated route-service tests', '/orders/**', 'GET', 'https://jsonplaceholder.typicode.com/posts/3', 'NONE', NULL, NULL, true, '2026-05-31 22:23:03.977872+05:30', '2026-05-31 22:23:03.977872+05:30', false, NULL, NULL, NULL);
INSERT INTO route.routes (id, name, description, path_pattern, http_method, target_url, transform_type, field_mapping_config, snippet_id, enabled, created_at, updated_at, rate_limit_enabled, rate_limit_replenish_rate, rate_limit_burst_capacity, rate_limit_requested_tokens) VALUES ('1f958a98-48bc-467b-8c12-11b3c35fe669', 'route_1780418437613', 'Primary route for automated route-service tests', '/orders/1780418437613', 'POST', 'https://example.com/orders/1780418437613', 'NONE', NULL, NULL, true, '2026-06-02 22:10:37.765203+05:30', '2026-06-02 22:10:37.765203+05:30', false, NULL, NULL, NULL);
INSERT INTO route.routes (id, name, description, path_pattern, http_method, target_url, transform_type, field_mapping_config, snippet_id, enabled, created_at, updated_at, rate_limit_enabled, rate_limit_replenish_rate, rate_limit_burst_capacity, rate_limit_requested_tokens) VALUES ('231d6d2d-948a-4179-8d6a-2ee1377ef113', 'route_1780418462023_updated', 'Updated route fordd tests', '/orders/1780418462023/v2', 'GET', 'http://localhost:9000/orders/45', 'NONE', NULL, NULL, true, '2026-06-02 22:11:02.099864+05:30', '2026-06-02 22:23:01.718719+05:30', false, NULL, NULL, NULL);
INSERT INTO route.routes (id, name, description, path_pattern, http_method, target_url, transform_type, field_mapping_config, snippet_id, enabled, created_at, updated_at, rate_limit_enabled, rate_limit_replenish_rate, rate_limit_burst_capacity, rate_limit_requested_tokens) VALUES ('dfbc133f-ba18-40ae-9c2b-f95ba53034f2', 'route_1780449299700', 'Primary route for automated route-service tests', '/orders/123', 'POST', 'http://localhost:9000/order/1', 'NONE', NULL, NULL, true, '2026-06-03 06:44:59.81502+05:30', '2026-06-09 04:53:29.974246+05:30', true, 60, 60, 1);


--
-- Data for Name: routing_rules; Type: TABLE DATA; Schema: route; Owner: route_svc
--

INSERT INTO route.routing_rules (id, route_id, match_config, override_target_url, priority, enabled, created_at) VALUES ('f8fcd8ac-13eb-4239-bb2a-9c63cff1a805', '231d6d2d-948a-4179-8d6a-2ee1377ef113', '{"matchMode": "ALL", "conditions": [{"key": "X-Client", "type": "HEADER", "equals": "partner-a"}]}', 'https://example.com/rules/1780418462023/updated', 1, false, '2026-06-02 22:11:40.114845+05:30');
INSERT INTO route.routing_rules (id, route_id, match_config, override_target_url, priority, enabled, created_at) VALUES ('87948ec1-6f5e-4ee4-83b2-ace6878e41df', '231d6d2d-948a-4179-8d6a-2ee1377ef113', '{"matchMode": "ALL", "conditions": [{"key": "X-Client", "type": "HEADER", "equals": "partner-b"}]}', 'https://example.com/rules/1780418462023', 0, true, '2026-06-02 22:13:46.762337+05:30');
INSERT INTO route.routing_rules (id, route_id, match_config, override_target_url, priority, enabled, created_at) VALUES ('eb438e61-aeac-4e6d-a2d4-d1e2438c1085', '231d6d2d-948a-4179-8d6a-2ee1377ef113', '{"matchMode": "ALL", "conditions": [{"key": "X-Client", "type": "HEADER", "equals": "partner-c"}]}', 'https://example.com/rules/1780418462023', 0, true, '2026-06-02 22:13:51.817557+05:30');
INSERT INTO route.routing_rules (id, route_id, match_config, override_target_url, priority, enabled, created_at) VALUES ('06a36661-3e21-417b-865e-6b6b5eb5ea62', '231d6d2d-948a-4179-8d6a-2ee1377ef113', '{"matchMode": "ALL", "conditions": [{"key": "X-Client", "type": "HEADER", "equals": "partner-d"}]}', 'https://jsonplaceholder.typicode.com/posts/19', 0, true, '2026-06-02 22:13:58.109101+05:30');
INSERT INTO route.routing_rules (id, route_id, match_config, override_target_url, priority, enabled, created_at) VALUES ('8c9a6599-5f35-42a5-b901-d19dc17abfdc', 'dfbc133f-ba18-40ae-9c2b-f95ba53034f2', '{"matchMode": "ANY", "conditions": [{"key": "$.id", "type": "BODY", "equals": "3"}, {"key": "X-Client", "type": "HEADER", "equals": "partner-d"}]}', 'http://localhost:9000/order/3', 0, true, '2026-06-03 06:56:01.3279+05:30');
INSERT INTO route.routing_rules (id, route_id, match_config, override_target_url, priority, enabled, created_at) VALUES ('2a7550ef-d77d-4f21-be63-310119a76aa8', 'dfbc133f-ba18-40ae-9c2b-f95ba53034f2', '{"matchMode": "ALL", "conditions": [{"key": "$.id", "type": "BODY", "equals": "2"}]}', 'http://localhost:9000/order/2', 2, true, '2026-06-03 06:54:07.542438+05:30');
INSERT INTO route.routing_rules (id, route_id, match_config, override_target_url, priority, enabled, created_at) VALUES ('ac8f8474-183a-4013-a8d4-0a25f63affad', 'dfbc133f-ba18-40ae-9c2b-f95ba53034f2', '{"matchMode": "ALL", "conditions": [{"key": "$.id", "type": "BODY", "equals": "4"}, {"key": "X-Client", "type": "HEADER", "equals": "partner-e"}]}', 'http://localhost:9000/order/error', 1, true, '2026-06-03 07:05:25.011745+05:30');


--
-- PostgreSQL database dump complete
--

\unrestrict ueynZfe5ZcG3cjPydlorXZsTNpjpAyMneDnpH053ZPtFL2FP7UKuebF8mCGiIW0

