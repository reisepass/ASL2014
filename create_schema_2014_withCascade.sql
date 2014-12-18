SET statement_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;
CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;
COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';
SET search_path = public, pg_catalog;
SET default_tablespace = '';
SET default_with_oids = false;


CREATE TABLE clients (
    clientid integer NOT NULL
);

ALTER TABLE ONLY clients
    ADD CONSTRAINT client_pkey PRIMARY KEY (clientid);
ALTER TABLE public.clients OWNER TO "ec2-user";

CREATE TABLE queues (
    queueid integer NOT NULL,
    queuename character varying,
    createdby integer,
    creationtime timestamp without time zone DEFAULT now()
);
ALTER TABLE public.queues OWNER TO "ec2-user";

ALTER TABLE ONLY queues
    ADD CONSTRAINT queue_pkey PRIMARY KEY (queueid);

CREATE SEQUENCE messageid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
ALTER TABLE public.messageid_seq OWNER TO "ec2-user";
CREATE TABLE messages (
    messageid integer DEFAULT nextval('messageid_seq'::regclass),
    senderid integer references clients(clientid),
    receiverid integer references clients(clientid),
    queueid integer references queues(queueid) ON DELETE CASCADE,
    context integer,
    priority integer,
    timeofarrival timestamp without time zone DEFAULT now(),
    message character varying(10000)
);
ALTER TABLE public.messages OWNER TO "ec2-user";
CREATE UNIQUE INDEX messageid_idx ON messages USING btree (messageid);
CREATE INDEX timeofarrial_idx ON messages USING btree (timeofarrival);
CREATE INDEX senderid_idx ON messages USING btree (senderid);
CREATE INDEX queueid_idx ON messages USING btree (queueid);
CREATE INDEX receiverid_idx ON messages USING btree (receiverid);

INSERT INTO clients VALUES(0);
INSERT INTO clients VALUES(-1);




REVOKE ALL ON SCHEMA public FROM PUBLIC;
GRANT ALL ON SCHEMA public TO PUBLIC;
ALTER USER "ec2-user" PASSWORD 'ec2-user';


