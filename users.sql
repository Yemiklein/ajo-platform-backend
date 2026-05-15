--
-- PostgreSQL database dump
--

\restrict HOYiyl6aHlVLMG8Vd4hujFoDavxb9LKehe8Q3jRIaEOKKqMe5RjZrdHA8f7VKDf

-- Dumped from database version 16.13 (Homebrew)
-- Dumped by pg_dump version 16.13 (Homebrew)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: ajo_user
--

INSERT INTO public.users (id, created_at, email, enabled, first_name, last_name, password, phone_number, role, updated_at) VALUES (2, '2026-05-10 20:53:36.882705', 'adeyemi2@test.com', true, 'Adeyemi', 'Test', '$2a$10$2.24b61TybjfZ0jWU8bbbuVLmoDTr4Kp2K3mXfg1mKKb4uypX8VJq', '08012345679', 'USER', '2026-05-10 20:53:36.882946');
INSERT INTO public.users (id, created_at, email, enabled, first_name, last_name, password, phone_number, role, updated_at) VALUES (3, '2026-05-10 20:55:45.703987', 'adeyemi22@test.com', true, 'Yemi', 'Raji', '$2a$10$dAWEYfT0HASJsNzxUAFb/eEZeZK5YB8N3RZMrLhf/uk6HvEejCyAK', '08012345689', 'USER', '2026-05-10 20:55:45.704005');
INSERT INTO public.users (id, created_at, email, enabled, first_name, last_name, password, phone_number, role, updated_at) VALUES (4, '2026-05-10 21:46:17.516744', 'ajoke@test.com', true, 'Ajoke', 'Soyombo', '$2a$10$9iRZqgWafc0W082NOsKWTed9gEWZ9pXuwxyeP6Lkw2v5SPG3xHDqy', '08012345680', 'USER', '2026-05-10 21:46:17.516772');
INSERT INTO public.users (id, created_at, email, enabled, first_name, last_name, password, phone_number, role, updated_at) VALUES (5, '2026-05-13 23:53:51.074534', 'alabitest@gmail.com', true, 'Alabi', 'Yellowi', '$2a$10$tAO.r/DCB.t8CGPW1JripuotEijUBdpjIMcVXvDK2cwY/ETJw/VR6', '091386098188', 'USER', '2026-05-13 23:53:51.074695');
INSERT INTO public.users (id, created_at, email, enabled, first_name, last_name, password, phone_number, role, updated_at) VALUES (6, '2026-05-14 22:39:11.976445', 'klein@test.com', true, 'Adeyemi', 'Klein', '$2a$10$GVo6.HBaE8Wzrxa1o9ZCNunxRqafIUbxVX.fs/0UxMUWyx3wgUg2C', '070344342999', 'USER', '2026-05-14 22:39:11.976461');
INSERT INTO public.users (id, created_at, email, enabled, first_name, last_name, password, phone_number, role, updated_at) VALUES (1, '2026-05-10 19:34:17.029274', 'adeyemi@test.com', true, 'Adeyemi', 'Test', '$2a$10$WaTgjOL/Z1sXAnPlaMS26uXKcOnyeJw9d1sx7aOBWux0CPevhxTDW', '08012345678', 'ADMIN', '2026-05-10 19:34:17.029325');


--
-- Name: users_id_seq; Type: SEQUENCE SET; Schema: public; Owner: ajo_user
--

SELECT pg_catalog.setval('public.users_id_seq', 6, true);


--
-- PostgreSQL database dump complete
--

\unrestrict HOYiyl6aHlVLMG8Vd4hujFoDavxb9LKehe8Q3jRIaEOKKqMe5RjZrdHA8f7VKDf

