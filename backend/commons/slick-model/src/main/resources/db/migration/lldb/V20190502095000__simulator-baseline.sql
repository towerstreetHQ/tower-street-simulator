--
-- PostgreSQL database dump
--

-- Dumped from database version 10.7 (Ubuntu 10.7-0ubuntu0.18.04.1)
-- Dumped by pg_dump version 10.7 (Ubuntu 10.7-0ubuntu0.18.04.1)

-- Started on 2019-05-02 09:47:07 CEST

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET client_min_messages = warning;
SET row_security = off;

--
-- TOC entry 9 (class 2615 OID 398963)
-- Name: attacksimulator; Type: SCHEMA; Schema: -; Owner: towerstreet
--

CREATE SCHEMA attacksimulator;


ALTER SCHEMA attacksimulator OWNER TO towerstreet;

--
-- TOC entry 5 (class 2615 OID 400342)
-- Name: scoring; Type: SCHEMA; Schema: -; Owner: towerstreet
--

CREATE SCHEMA scoring;


ALTER SCHEMA scoring OWNER TO towerstreet;

--
-- TOC entry 751 (class 1247 OID 400957)
-- Name: simulation_config_description; Type: TYPE; Schema: attacksimulator; Owner: towerstreet
--

CREATE TYPE attacksimulator.simulation_config_description AS (
	"position" integer,
	task_key character varying,
	test_case_key character varying
);


ALTER TYPE attacksimulator.simulation_config_description OWNER TO towerstreet;

--
-- TOC entry 754 (class 1247 OID 400961)
-- Name: simulation_scoring_config_description; Type: TYPE; Schema: scoring; Owner: towerstreet
--

CREATE TYPE scoring.simulation_scoring_config_description AS (
	task_key character varying,
	scoring_key character varying
);


ALTER TYPE scoring.simulation_scoring_config_description OWNER TO towerstreet;

--
-- TOC entry 273 (class 1255 OID 400958)
-- Name: add_simulation_config(character varying[], attacksimulator.simulation_config_description[]); Type: FUNCTION; Schema: attacksimulator; Owner: towerstreet
--

CREATE FUNCTION attacksimulator.add_simulation_config(assessment_keys character varying[], tasks_description attacksimulator.simulation_config_description[]) RETURNS void
    LANGUAGE plpgsql
    AS $$
BEGIN
  WITH tasks AS (
    SELECT * FROM unnest(tasks_description) AS q (position, task_key, test_case_key)
  ),
  templates AS (
    -- Prepare IDs of newest templates
    SELECT DISTINCT ON(ast.id) st.id
    FROM attacksimulator.simulation_template AS st
    JOIN public.assessment_type AS ast ON ast.id = st.assessment_type_id
    WHERE ast.assessment_key = ANY (assessment_keys)
    ORDER BY ast.id, st.version DESC
  )
    -- Insert config
    INSERT INTO attacksimulator.simulation_template_config
    SELECT templates.id, t.id, tasks.position, tc.id
    FROM tasks AS tasks
    JOIN attacksimulator.task AS t ON tasks.task_key = t.task_key
    JOIN attacksimulator.test_case AS tc ON tasks.test_case_key = tc.test_case_key
    CROSS JOIN templates AS templates
    ORDER BY tasks.position
  ;
END;
$$;


ALTER FUNCTION attacksimulator.add_simulation_config(assessment_keys character varying[], tasks_description attacksimulator.simulation_config_description[]) OWNER TO towerstreet;

--
-- TOC entry 259 (class 1255 OID 400953)
-- Name: create_new_templates(character varying[]); Type: FUNCTION; Schema: attacksimulator; Owner: towerstreet
--

CREATE FUNCTION attacksimulator.create_new_templates(VARIADIC assessment_keys character varying[]) RETURNS void
    LANGUAGE plpgsql
    AS $$
BEGIN
  INSERT INTO attacksimulator.simulation_template(version, assessment_type_id, description)
  SELECT now(), id, assessment_key
  FROM public.assessment_type
  WHERE assessment_key = ANY (assessment_keys);
END;
$$;


ALTER FUNCTION attacksimulator.create_new_templates(VARIADIC assessment_keys character varying[]) OWNER TO towerstreet;

--
-- TOC entry 260 (class 1255 OID 400954)
-- Name: update_simulation_templates(character varying[]); Type: FUNCTION; Schema: attacksimulator; Owner: towerstreet
--

CREATE FUNCTION attacksimulator.update_simulation_templates(VARIADIC assessment_keys character varying[]) RETURNS void
    LANGUAGE plpgsql
    AS $$
BEGIN
  WITH templates AS (
    SELECT DISTINCT ON(ast.id) st.id, st.assessment_type_id
    FROM attacksimulator.simulation_template AS st
    JOIN public.assessment_type AS ast ON ast.id = st.assessment_type_id
    WHERE ast.assessment_key = ANY (assessment_keys)
    ORDER BY ast.id, st.version DESC
  )
    UPDATE attacksimulator.simulation AS s
    SET template_id = st_new.id
    FROM attacksimulator.simulation_template AS st
    JOIN public.assessment_type AS ast ON ast.id = st.assessment_type_id
    JOIN templates AS st_new ON st.assessment_type_id = st_new.assessment_type_id
    WHERE st.id = s.template_id
;
END;
$$;


ALTER FUNCTION attacksimulator.update_simulation_templates(VARIADIC assessment_keys character varying[]) OWNER TO towerstreet;

--
-- TOC entry 276 (class 1255 OID 401002)
-- Name: create_ops_user(character varying); Type: FUNCTION; Schema: public; Owner: towerstreet
--

CREATE FUNCTION public.create_ops_user(username_par character varying) RETURNS integer
    LANGUAGE plpgsql
    AS $$
DECLARE
  ops_user_id_var int;
BEGIN
  SELECT * FROM create_ops_user(username_par, VARIADIC ARRAY[] :: varchar[])
  INTO ops_user_id_var;
  RETURN ops_user_id_var;
END;
$$;

--
-- TOC entry 274 (class 1255 OID 400962)
-- Name: add_simulation_scoring_config(character varying[], scoring.simulation_scoring_config_description[]); Type: FUNCTION; Schema: scoring; Owner: towerstreet
--

CREATE FUNCTION scoring.add_simulation_scoring_config(assessment_keys character varying[], tasks_description scoring.simulation_scoring_config_description[]) RETURNS void
    LANGUAGE plpgsql
    AS $$
BEGIN
  WITH tasks AS (
    SELECT * FROM unnest(tasks_description) AS q (task_key, scoring_key)
  ),
  templates AS (
    -- Prepare IDs of newest templates
    SELECT DISTINCT ON(ast.id) st.id
    FROM attacksimulator.simulation_template AS st
    JOIN public.assessment_type AS ast ON ast.id = st.assessment_type_id
    WHERE ast.assessment_key = ANY (assessment_keys)
    ORDER BY ast.id, st.version DESC
  )
    -- Insert config
    INSERT INTO scoring.simulation_scoring_config
    SELECT sd.id, templates.id, t.id
    FROM tasks AS tasks
    JOIN attacksimulator.task AS t ON tasks.task_key = t.task_key
    JOIN scoring.scoring_definition AS sd ON tasks.scoring_key = sd.scoring_key
    CROSS JOIN templates AS templates
  ;
END;
$$;


ALTER FUNCTION scoring.add_simulation_scoring_config(assessment_keys character varying[], tasks_description scoring.simulation_scoring_config_description[]) OWNER TO towerstreet;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- TOC entry 209 (class 1259 OID 399014)
-- Name: simulation; Type: TABLE; Schema: attacksimulator; Owner: towerstreet
--

CREATE TABLE attacksimulator.simulation (
    id integer NOT NULL,
    user_id integer NOT NULL,
    token uuid NOT NULL,
    description character varying(50) DEFAULT NULL::character varying,
    customer_assessment_id integer NOT NULL,
    template_id integer NOT NULL,
    CONSTRAINT simulation_token_check CHECK (((token)::text <> ''::text))
);


ALTER TABLE attacksimulator.simulation OWNER TO towerstreet;

--
-- TOC entry 211 (class 1259 OID 399027)
-- Name: simulation_outcome; Type: TABLE; Schema: attacksimulator; Owner: towerstreet
--

CREATE TABLE attacksimulator.simulation_outcome (
    id integer NOT NULL,
    simulation_id integer NOT NULL,
    token uuid NOT NULL,
    created_at timestamp without time zone NOT NULL,
    finished_at timestamp without time zone,
    last_ping_at timestamp without time zone NOT NULL,
    CONSTRAINT simulation_outcome_token_check CHECK (((token)::text <> ''::text))
);


ALTER TABLE attacksimulator.simulation_outcome OWNER TO towerstreet;

--
-- TOC entry 215 (class 1259 OID 399042)
-- Name: user; Type: TABLE; Schema: attacksimulator; Owner: towerstreet
--

CREATE TABLE attacksimulator."user" (
    id integer NOT NULL,
    customer_id integer NOT NULL,
    user_name character varying NOT NULL,
    campaign_visitor_id integer,
    CONSTRAINT user_user_name_check CHECK (((user_name)::text <> ''::text))
);


ALTER TABLE attacksimulator."user" OWNER TO towerstreet;

--
-- TOC entry 238 (class 1259 OID 400835)
-- Name: campaign_visitor; Type: TABLE; Schema: public; Owner: towerstreet
--

CREATE TABLE public.campaign_visitor (
    id integer NOT NULL,
    customer_id integer NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    inet_addr inet,
    user_agent character varying,
    access_token character varying
);


ALTER TABLE public.campaign_visitor OWNER TO towerstreet;

--
-- TOC entry 233 (class 1259 OID 400359)
-- Name: scoring_outcome; Type: TABLE; Schema: scoring; Owner: towerstreet
--

CREATE TABLE scoring.scoring_outcome (
    id integer NOT NULL,
    customer_assessment_id integer NOT NULL,
    simulation_outcome_id integer,
    queued_at timestamp without time zone NOT NULL,
    finished_at timestamp without time zone,
    retries integer
);


ALTER TABLE scoring.scoring_outcome OWNER TO towerstreet;

--
-- TOC entry 243 (class 1259 OID 400996)
-- Name: campaign_visitor_simulation; Type: VIEW; Schema: attacksimulator; Owner: towerstreet
--

CREATE VIEW attacksimulator.campaign_visitor_simulation AS
 SELECT cv.id AS visitor_id,
    cv.customer_id,
    sim.id AS simulation_id,
    sim.token AS simulation_token,
    sim.template_id,
    sim_o.id AS simulation_outcome_id,
    sim_o.created_at AS simulation_outcome_created_at,
    sim_o.finished_at AS simulation_outcome_finished_at,
    sim_o.last_ping_at AS simulation_outcome_last_ping_at,
    so.id AS scoring_outcome_id,
    so.queued_at AS scoring_queued_at,
    so.finished_at AS scoring_finished_at,
    sim.user_id AS simulation_user_id
   FROM ((((attacksimulator.simulation sim
     JOIN attacksimulator."user" sim_u ON ((sim_u.id = sim.user_id)))
     JOIN public.campaign_visitor cv ON ((sim_u.campaign_visitor_id = cv.id)))
     LEFT JOIN attacksimulator.simulation_outcome sim_o ON ((sim.id = sim_o.simulation_id)))
     LEFT JOIN scoring.scoring_outcome so ON ((so.simulation_outcome_id = sim_o.id)))
  ORDER BY sim_o.finished_at DESC;


ALTER TABLE attacksimulator.campaign_visitor_simulation OWNER TO towerstreet;

--
-- TOC entry 199 (class 1259 OID 398975)
-- Name: client_request; Type: TABLE; Schema: attacksimulator; Owner: towerstreet
--

CREATE TABLE attacksimulator.client_request (
    id integer NOT NULL,
    inet_addr inet NOT NULL,
    user_agent character varying,
    requested_at timestamp without time zone NOT NULL,
    resource character varying NOT NULL,
    response_status character varying NOT NULL,
    simulation_id integer,
    simulation_outcome_id integer,
    received_data_id integer,
    token uuid
);


ALTER TABLE attacksimulator.client_request OWNER TO towerstreet;

--
-- TOC entry 200 (class 1259 OID 398981)
-- Name: client_request_id_seq; Type: SEQUENCE; Schema: attacksimulator; Owner: towerstreet
--

CREATE SEQUENCE attacksimulator.client_request_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE attacksimulator.client_request_id_seq OWNER TO towerstreet;

--
-- TOC entry 3286 (class 0 OID 0)
-- Dependencies: 200
-- Name: client_request_id_seq; Type: SEQUENCE OWNED BY; Schema: attacksimulator; Owner: towerstreet
--

ALTER SEQUENCE attacksimulator.client_request_id_seq OWNED BY attacksimulator.client_request.id;


--
-- TOC entry 201 (class 1259 OID 398983)
-- Name: network_segment; Type: TABLE; Schema: attacksimulator; Owner: towerstreet
--

CREATE TABLE attacksimulator.network_segment (
    id integer NOT NULL,
    simulation_outcome_id integer NOT NULL,
    task_id integer NOT NULL,
    client_ip inet NOT NULL,
    subnet_ip inet NOT NULL,
    subnet_prefix integer NOT NULL,
    created_at timestamp without time zone NOT NULL
);


ALTER TABLE attacksimulator.network_segment OWNER TO towerstreet;

--
-- TOC entry 202 (class 1259 OID 398989)
-- Name: network_segment_id_seq; Type: SEQUENCE; Schema: attacksimulator; Owner: towerstreet
--

CREATE SEQUENCE attacksimulator.network_segment_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE attacksimulator.network_segment_id_seq OWNER TO towerstreet;

--
-- TOC entry 3287 (class 0 OID 0)
-- Dependencies: 202
-- Name: network_segment_id_seq; Type: SEQUENCE OWNED BY; Schema: attacksimulator; Owner: towerstreet
--

ALTER SEQUENCE attacksimulator.network_segment_id_seq OWNED BY attacksimulator.network_segment.id;


--
-- TOC entry 203 (class 1259 OID 398991)
-- Name: outcome_task_result; Type: TABLE; Schema: attacksimulator; Owner: towerstreet
--

CREATE TABLE attacksimulator.outcome_task_result (
    id integer NOT NULL,
    simulation_outcome_id integer NOT NULL,
    task_id integer NOT NULL,
    is_success boolean NOT NULL,
    message character varying,
    task_result json,
    duration interval,
    url_test_id integer
);


ALTER TABLE attacksimulator.outcome_task_result OWNER TO towerstreet;

--
-- TOC entry 204 (class 1259 OID 398997)
-- Name: outcome_task_result_id_seq; Type: SEQUENCE; Schema: attacksimulator; Owner: towerstreet
--

CREATE SEQUENCE attacksimulator.outcome_task_result_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE attacksimulator.outcome_task_result_id_seq OWNER TO towerstreet;

--
-- TOC entry 3288 (class 0 OID 0)
-- Dependencies: 204
-- Name: outcome_task_result_id_seq; Type: SEQUENCE OWNED BY; Schema: attacksimulator; Owner: towerstreet
--

ALTER SEQUENCE attacksimulator.outcome_task_result_id_seq OWNED BY attacksimulator.outcome_task_result.id;


--
-- TOC entry 205 (class 1259 OID 398999)
-- Name: received_data; Type: TABLE; Schema: attacksimulator; Owner: towerstreet
--

CREATE TABLE attacksimulator.received_data (
    id integer NOT NULL,
    simulation_outcome_id integer NOT NULL,
    task_id integer NOT NULL,
    content_type character varying(30),
    file_name character varying(30) NOT NULL,
    created_at timestamp without time zone NOT NULL,
    size bigint DEFAULT 0 NOT NULL
);


ALTER TABLE attacksimulator.received_data OWNER TO towerstreet;

--
-- TOC entry 206 (class 1259 OID 399006)
-- Name: received_data_id_seq; Type: SEQUENCE; Schema: attacksimulator; Owner: towerstreet
--

CREATE SEQUENCE attacksimulator.received_data_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE attacksimulator.received_data_id_seq OWNER TO towerstreet;

--
-- TOC entry 3289 (class 0 OID 0)
-- Dependencies: 206
-- Name: received_data_id_seq; Type: SEQUENCE OWNED BY; Schema: attacksimulator; Owner: towerstreet
--

ALTER SEQUENCE attacksimulator.received_data_id_seq OWNED BY attacksimulator.received_data.id;


--
-- TOC entry 207 (class 1259 OID 399008)
-- Name: runner; Type: TABLE; Schema: attacksimulator; Owner: towerstreet
--

CREATE TABLE attacksimulator.runner (
    id integer NOT NULL,
    runner_name character varying(50) NOT NULL,
    CONSTRAINT runner_runner_name_check CHECK (((runner_name)::text <> ''::text))
);


ALTER TABLE attacksimulator.runner OWNER TO towerstreet;

--
-- TOC entry 208 (class 1259 OID 399012)
-- Name: runner_id_seq; Type: SEQUENCE; Schema: attacksimulator; Owner: towerstreet
--

CREATE SEQUENCE attacksimulator.runner_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE attacksimulator.runner_id_seq OWNER TO towerstreet;

--
-- TOC entry 3290 (class 0 OID 0)
-- Dependencies: 208
-- Name: runner_id_seq; Type: SEQUENCE OWNED BY; Schema: attacksimulator; Owner: towerstreet
--

ALTER SEQUENCE attacksimulator.runner_id_seq OWNED BY attacksimulator.runner.id;


--
-- TOC entry 210 (class 1259 OID 399025)
-- Name: simulation_id_seq; Type: SEQUENCE; Schema: attacksimulator; Owner: towerstreet
--

CREATE SEQUENCE attacksimulator.simulation_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE attacksimulator.simulation_id_seq OWNER TO towerstreet;

--
-- TOC entry 3291 (class 0 OID 0)
-- Dependencies: 210
-- Name: simulation_id_seq; Type: SEQUENCE OWNED BY; Schema: attacksimulator; Owner: towerstreet
--

ALTER SEQUENCE attacksimulator.simulation_id_seq OWNED BY attacksimulator.simulation.id;


--
-- TOC entry 212 (class 1259 OID 399031)
-- Name: simulation_outcome_id_seq; Type: SEQUENCE; Schema: attacksimulator; Owner: towerstreet
--

CREATE SEQUENCE attacksimulator.simulation_outcome_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE attacksimulator.simulation_outcome_id_seq OWNER TO towerstreet;

--
-- TOC entry 3292 (class 0 OID 0)
-- Dependencies: 212
-- Name: simulation_outcome_id_seq; Type: SEQUENCE OWNED BY; Schema: attacksimulator; Owner: towerstreet
--

ALTER SEQUENCE attacksimulator.simulation_outcome_id_seq OWNED BY attacksimulator.simulation_outcome.id;


--
-- TOC entry 213 (class 1259 OID 399033)
-- Name: task; Type: TABLE; Schema: attacksimulator; Owner: towerstreet
--

CREATE TABLE attacksimulator.task (
    id integer NOT NULL,
    runner_id integer NOT NULL,
    task_key character varying(32) NOT NULL,
    parameters json,
    CONSTRAINT task_task_key_check CHECK (((task_key)::text <> ''::text))
);


ALTER TABLE attacksimulator.task OWNER TO towerstreet;

--
-- TOC entry 220 (class 1259 OID 399977)
-- Name: url_test; Type: TABLE; Schema: attacksimulator; Owner: towerstreet
--

CREATE TABLE attacksimulator.url_test (
    id integer NOT NULL,
    client_request integer,
    task_id integer NOT NULL,
    size bigint DEFAULT 0 NOT NULL,
    duration interval,
    created_at timestamp without time zone NOT NULL,
    status integer
);


ALTER TABLE attacksimulator.url_test OWNER TO towerstreet;

--
-- TOC entry 246 (class 1259 OID 401024)
-- Name: simulation_results; Type: VIEW; Schema: attacksimulator; Owner: towerstreet
--

CREATE VIEW attacksimulator.simulation_results AS
 SELECT tr.id,
    s.token AS simulation_token,
    s.description AS simulation_description,
    o.token AS outcome_token,
    o.created_at AS outcome_started,
    o.finished_at AS outcome_finished,
    t.task_key,
    tr.is_success,
    tr.message,
    tr.task_result,
    tr.duration,
    url.created_at AS url_check_at,
    url.size AS url_check_response_size,
    url.duration AS url_check_duration
   FROM ((((attacksimulator.outcome_task_result tr
     JOIN attacksimulator.task t ON ((tr.task_id = t.id)))
     JOIN attacksimulator.simulation_outcome o ON ((tr.simulation_outcome_id = o.id)))
     JOIN attacksimulator.simulation s ON ((o.simulation_id = s.id)))
     LEFT JOIN attacksimulator.url_test url ON ((tr.url_test_id = url.id)));


ALTER TABLE attacksimulator.simulation_results OWNER TO towerstreet;

--
-- TOC entry 226 (class 1259 OID 400177)
-- Name: simulation_template; Type: TABLE; Schema: attacksimulator; Owner: towerstreet
--

CREATE TABLE attacksimulator.simulation_template (
    id integer NOT NULL,
    version timestamp without time zone NOT NULL,
    assessment_type_id integer NOT NULL,
    description character varying(50)
);


ALTER TABLE attacksimulator.simulation_template OWNER TO towerstreet;

--
-- TOC entry 227 (class 1259 OID 400190)
-- Name: simulation_template_config; Type: TABLE; Schema: attacksimulator; Owner: towerstreet
--

CREATE TABLE attacksimulator.simulation_template_config (
    template_id integer NOT NULL,
    task_id integer NOT NULL,
    "position" integer NOT NULL,
    test_case_id integer
);


ALTER TABLE attacksimulator.simulation_template_config OWNER TO towerstreet;

--
-- TOC entry 225 (class 1259 OID 400175)
-- Name: simulation_template_id_seq; Type: SEQUENCE; Schema: attacksimulator; Owner: towerstreet
--

CREATE SEQUENCE attacksimulator.simulation_template_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE attacksimulator.simulation_template_id_seq OWNER TO towerstreet;

--
-- TOC entry 3293 (class 0 OID 0)
-- Dependencies: 225
-- Name: simulation_template_id_seq; Type: SEQUENCE OWNED BY; Schema: attacksimulator; Owner: towerstreet
--

ALTER SEQUENCE attacksimulator.simulation_template_id_seq OWNED BY attacksimulator.simulation_template.id;


--
-- TOC entry 229 (class 1259 OID 400215)
-- Name: test_case; Type: TABLE; Schema: attacksimulator; Owner: towerstreet
--

CREATE TABLE attacksimulator.test_case (
    id integer NOT NULL,
    test_case_key character varying(30) NOT NULL,
    label_in_simulation character varying NOT NULL,
    description character varying NOT NULL,
    CONSTRAINT test_case_test_case_key_check CHECK (((test_case_key)::text <> ''::text))
);


ALTER TABLE attacksimulator.test_case OWNER TO towerstreet;

--
-- TOC entry 248 (class 1259 OID 401034)
-- Name: task_for_simulation; Type: VIEW; Schema: attacksimulator; Owner: towerstreet
--

CREATE VIEW attacksimulator.task_for_simulation AS
 SELECT t.id,
    c.template_id,
    c."position",
    t.task_key,
    r.runner_name,
    tc.label_in_simulation,
    t.parameters
   FROM (((attacksimulator.task t
     JOIN attacksimulator.runner r ON ((t.runner_id = r.id)))
     JOIN attacksimulator.simulation_template_config c ON ((t.id = c.task_id)))
     LEFT JOIN attacksimulator.test_case tc ON ((tc.id = c.test_case_id)));


ALTER TABLE attacksimulator.task_for_simulation OWNER TO towerstreet;

--
-- TOC entry 214 (class 1259 OID 399040)
-- Name: task_id_seq; Type: SEQUENCE; Schema: attacksimulator; Owner: towerstreet
--

CREATE SEQUENCE attacksimulator.task_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE attacksimulator.task_id_seq OWNER TO towerstreet;

--
-- TOC entry 3294 (class 0 OID 0)
-- Dependencies: 214
-- Name: task_id_seq; Type: SEQUENCE OWNED BY; Schema: attacksimulator; Owner: towerstreet
--

ALTER SEQUENCE attacksimulator.task_id_seq OWNED BY attacksimulator.task.id;


--
-- TOC entry 228 (class 1259 OID 400213)
-- Name: test_case_id_seq; Type: SEQUENCE; Schema: attacksimulator; Owner: towerstreet
--

CREATE SEQUENCE attacksimulator.test_case_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE attacksimulator.test_case_id_seq OWNER TO towerstreet;

--
-- TOC entry 3295 (class 0 OID 0)
-- Dependencies: 228
-- Name: test_case_id_seq; Type: SEQUENCE OWNED BY; Schema: attacksimulator; Owner: towerstreet
--

ALTER SEQUENCE attacksimulator.test_case_id_seq OWNED BY attacksimulator.test_case.id;


--
-- TOC entry 219 (class 1259 OID 399975)
-- Name: url_test_id_seq; Type: SEQUENCE; Schema: attacksimulator; Owner: towerstreet
--

CREATE SEQUENCE attacksimulator.url_test_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE attacksimulator.url_test_id_seq OWNER TO towerstreet;

--
-- TOC entry 3296 (class 0 OID 0)
-- Dependencies: 219
-- Name: url_test_id_seq; Type: SEQUENCE OWNED BY; Schema: attacksimulator; Owner: towerstreet
--

ALTER SEQUENCE attacksimulator.url_test_id_seq OWNED BY attacksimulator.url_test.id;


--
-- TOC entry 216 (class 1259 OID 399049)
-- Name: user_id_seq; Type: SEQUENCE; Schema: attacksimulator; Owner: towerstreet
--

CREATE SEQUENCE attacksimulator.user_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE attacksimulator.user_id_seq OWNER TO towerstreet;

--
-- TOC entry 3297 (class 0 OID 0)
-- Dependencies: 216
-- Name: user_id_seq; Type: SEQUENCE OWNED BY; Schema: attacksimulator; Owner: towerstreet
--

ALTER SEQUENCE attacksimulator.user_id_seq OWNED BY attacksimulator."user".id;


--
-- TOC entry 222 (class 1259 OID 400095)
-- Name: assessment_type; Type: TABLE; Schema: public; Owner: towerstreet
--

CREATE TABLE public.assessment_type (
    id integer NOT NULL,
    assessment_key character varying(30) NOT NULL,
    description character varying,
    CONSTRAINT assessment_type_assessment_key_check CHECK (((assessment_key)::text <> ''::text))
);


ALTER TABLE public.assessment_type OWNER TO towerstreet;

--
-- TOC entry 221 (class 1259 OID 400093)
-- Name: assessment_type_id_seq; Type: SEQUENCE; Schema: public; Owner: towerstreet
--

CREATE SEQUENCE public.assessment_type_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.assessment_type_id_seq OWNER TO towerstreet;

--
-- TOC entry 3298 (class 0 OID 0)
-- Dependencies: 221
-- Name: assessment_type_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: towerstreet
--

ALTER SEQUENCE public.assessment_type_id_seq OWNED BY public.assessment_type.id;


--
-- TOC entry 237 (class 1259 OID 400833)
-- Name: campaign_visitor_id_seq; Type: SEQUENCE; Schema: public; Owner: towerstreet
--

CREATE SEQUENCE public.campaign_visitor_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.campaign_visitor_id_seq OWNER TO towerstreet;

--
-- TOC entry 3299 (class 0 OID 0)
-- Dependencies: 237
-- Name: campaign_visitor_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: towerstreet
--

ALTER SEQUENCE public.campaign_visitor_id_seq OWNED BY public.campaign_visitor.id;


--
-- TOC entry 218 (class 1259 OID 399628)
-- Name: customer; Type: TABLE; Schema: public; Owner: towerstreet
--

CREATE TABLE public.customer (
    id integer NOT NULL,
    company_name character varying NOT NULL,
    normalized_name character varying NOT NULL,
    is_campaign boolean DEFAULT false NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    CONSTRAINT company_company_name_check CHECK (((company_name)::text <> ''::text)),
    CONSTRAINT company_short_name_check CHECK (((normalized_name)::text <> ''::text))
);


ALTER TABLE public.customer OWNER TO towerstreet;

--
-- TOC entry 217 (class 1259 OID 399626)
-- Name: company_id_seq; Type: SEQUENCE; Schema: public; Owner: towerstreet
--

CREATE SEQUENCE public.company_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.company_id_seq OWNER TO towerstreet;

--
-- TOC entry 3300 (class 0 OID 0)
-- Dependencies: 217
-- Name: company_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: towerstreet
--

ALTER SEQUENCE public.company_id_seq OWNED BY public.customer.id;


--
-- TOC entry 224 (class 1259 OID 400109)
-- Name: customer_assessment; Type: TABLE; Schema: public; Owner: towerstreet
--

CREATE TABLE public.customer_assessment (
    id integer NOT NULL,
    assessment_type_id integer NOT NULL,
    customer_id integer NOT NULL,
    created_at timestamp without time zone NOT NULL,
    closed_at timestamp without time zone
);


ALTER TABLE public.customer_assessment OWNER TO towerstreet;

--
-- TOC entry 223 (class 1259 OID 400107)
-- Name: customer_assessment_id_seq; Type: SEQUENCE; Schema: public; Owner: towerstreet
--

CREATE SEQUENCE public.customer_assessment_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.customer_assessment_id_seq OWNER TO towerstreet;

--
-- TOC entry 3301 (class 0 OID 0)
-- Dependencies: 223
-- Name: customer_assessment_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: towerstreet
--

ALTER SEQUENCE public.customer_assessment_id_seq OWNED BY public.customer_assessment.id;


--
-- TOC entry 239 (class 1259 OID 400885)
-- Name: campaign_score_histogram; Type: TABLE; Schema: scoring; Owner: towerstreet
--

CREATE TABLE scoring.campaign_score_histogram (
    customer_id integer NOT NULL,
    score integer NOT NULL,
    value integer DEFAULT 1 NOT NULL
);


ALTER TABLE scoring.campaign_score_histogram OWNER TO towerstreet;

--
-- TOC entry 231 (class 1259 OID 400345)
-- Name: scoring_definition; Type: TABLE; Schema: scoring; Owner: towerstreet
--

CREATE TABLE scoring.scoring_definition (
    id integer NOT NULL,
    scoring_key character varying(30) NOT NULL,
    scoring_type character varying(30) NOT NULL,
    label character varying NOT NULL,
    parameters json,
    description character varying NOT NULL,
    "position" integer NOT NULL,
    category character varying(20) NOT NULL,
    CONSTRAINT scoring_definition_scoring_key_check CHECK (((scoring_key)::text <> ''::text))
);


ALTER TABLE scoring.scoring_definition OWNER TO towerstreet;

--
-- TOC entry 230 (class 1259 OID 400343)
-- Name: scoring_definition_id_seq; Type: SEQUENCE; Schema: scoring; Owner: towerstreet
--

CREATE SEQUENCE scoring.scoring_definition_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE scoring.scoring_definition_id_seq OWNER TO towerstreet;

--
-- TOC entry 3302 (class 0 OID 0)
-- Dependencies: 230
-- Name: scoring_definition_id_seq; Type: SEQUENCE OWNED BY; Schema: scoring; Owner: towerstreet
--

ALTER SEQUENCE scoring.scoring_definition_id_seq OWNED BY scoring.scoring_definition.id;


--
-- TOC entry 236 (class 1259 OID 400396)
-- Name: simulation_scoring_config; Type: TABLE; Schema: scoring; Owner: towerstreet
--

CREATE TABLE scoring.simulation_scoring_config (
    scoring_definition_id integer NOT NULL,
    simulation_template_id integer NOT NULL,
    task_id integer NOT NULL
);


ALTER TABLE scoring.simulation_scoring_config OWNER TO towerstreet;

--
-- TOC entry 244 (class 1259 OID 401014)
-- Name: scoring_definition_with_template; Type: VIEW; Schema: scoring; Owner: towerstreet
--

CREATE VIEW scoring.scoring_definition_with_template AS
 SELECT DISTINCT ON (ssc.scoring_definition_id, ssc.simulation_template_id, sd."position") ssc.scoring_definition_id,
    ssc.simulation_template_id,
    sd.label,
    sd.description,
    sd."position",
    sd.category,
    sd.scoring_type
   FROM (scoring.simulation_scoring_config ssc
     JOIN scoring.scoring_definition sd ON ((ssc.scoring_definition_id = sd.id)));


ALTER TABLE scoring.scoring_definition_with_template OWNER TO towerstreet;

--
-- TOC entry 232 (class 1259 OID 400357)
-- Name: scoring_outcome_id_seq; Type: SEQUENCE; Schema: scoring; Owner: towerstreet
--

CREATE SEQUENCE scoring.scoring_outcome_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE scoring.scoring_outcome_id_seq OWNER TO towerstreet;

--
-- TOC entry 3303 (class 0 OID 0)
-- Dependencies: 232
-- Name: scoring_outcome_id_seq; Type: SEQUENCE OWNED BY; Schema: scoring; Owner: towerstreet
--

ALTER SEQUENCE scoring.scoring_outcome_id_seq OWNED BY scoring.scoring_outcome.id;


--
-- TOC entry 235 (class 1259 OID 400377)
-- Name: scoring_result; Type: TABLE; Schema: scoring; Owner: towerstreet
--

CREATE TABLE scoring.scoring_result (
    id integer NOT NULL,
    scoring_definition_id integer NOT NULL,
    scoring_outcome_id integer NOT NULL,
    created_at timestamp without time zone NOT NULL,
    result character varying(30) NOT NULL,
    result_parameters json
);


ALTER TABLE scoring.scoring_result OWNER TO towerstreet;

--
-- TOC entry 234 (class 1259 OID 400375)
-- Name: scoring_result_id_seq; Type: SEQUENCE; Schema: scoring; Owner: towerstreet
--

CREATE SEQUENCE scoring.scoring_result_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE scoring.scoring_result_id_seq OWNER TO towerstreet;

--
-- TOC entry 3304 (class 0 OID 0)
-- Dependencies: 234
-- Name: scoring_result_id_seq; Type: SEQUENCE OWNED BY; Schema: scoring; Owner: towerstreet
--

ALTER SEQUENCE scoring.scoring_result_id_seq OWNED BY scoring.scoring_result.id;


--
-- TOC entry 245 (class 1259 OID 401019)
-- Name: simulation_outcome_for_scoring; Type: VIEW; Schema: scoring; Owner: towerstreet
--

CREATE VIEW scoring.simulation_outcome_for_scoring AS
 SELECT o.id AS simulation_outcome_id,
    o.created_at AS outcome_created_at,
    o.finished_at AS outcome_finished_at,
    s.template_id,
    so.id AS scoring_outcome_id,
    so.queued_at AS scoring_queued_at,
    so.finished_at AS scoring_finished_at,
    s.customer_assessment_id,
    so.retries
   FROM ((attacksimulator.simulation_outcome o
     JOIN attacksimulator.simulation s ON ((o.simulation_id = s.id)))
     LEFT JOIN scoring.scoring_outcome so ON ((o.id = so.simulation_outcome_id)));


ALTER TABLE scoring.simulation_outcome_for_scoring OWNER TO towerstreet;

--
-- TOC entry 247 (class 1259 OID 401029)
-- Name: simulation_scoring_result; Type: VIEW; Schema: scoring; Owner: towerstreet
--

CREATE VIEW scoring.simulation_scoring_result AS
 SELECT sr.id,
    sd.id AS scoring_definition_id,
    sim_o.id AS simulation_outcome_id,
    sd.scoring_key AS scoring_definition_key,
    sd.label AS scoring_definition_label,
    sr.created_at,
    sr.result,
    sr.result_parameters,
    sim_o.token AS simulation_outcome_token,
    sim_o.finished_at AS simulation_outcome_finished_at,
    so.finished_at AS scoring_outcome_finished_at
   FROM (((scoring.scoring_result sr
     JOIN scoring.scoring_definition sd ON ((sr.scoring_definition_id = sd.id)))
     JOIN scoring.scoring_outcome so ON ((sr.scoring_outcome_id = so.id)))
     JOIN attacksimulator.simulation_outcome sim_o ON ((sim_o.id = so.simulation_outcome_id)));


ALTER TABLE scoring.simulation_scoring_result OWNER TO towerstreet;

--
-- TOC entry 2975 (class 2604 OID 399215)
-- Name: client_request id; Type: DEFAULT; Schema: attacksimulator; Owner: towerstreet
--

ALTER TABLE ONLY attacksimulator.client_request ALTER COLUMN id SET DEFAULT nextval('attacksimulator.client_request_id_seq'::regclass);


--
-- TOC entry 2976 (class 2604 OID 399216)
-- Name: network_segment id; Type: DEFAULT; Schema: attacksimulator; Owner: towerstreet
--

ALTER TABLE ONLY attacksimulator.network_segment ALTER COLUMN id SET DEFAULT nextval('attacksimulator.network_segment_id_seq'::regclass);


--
-- TOC entry 2977 (class 2604 OID 399217)
-- Name: outcome_task_result id; Type: DEFAULT; Schema: attacksimulator; Owner: towerstreet
--

ALTER TABLE ONLY attacksimulator.outcome_task_result ALTER COLUMN id SET DEFAULT nextval('attacksimulator.outcome_task_result_id_seq'::regclass);


--
-- TOC entry 2979 (class 2604 OID 399218)
-- Name: received_data id; Type: DEFAULT; Schema: attacksimulator; Owner: towerstreet
--

ALTER TABLE ONLY attacksimulator.received_data ALTER COLUMN id SET DEFAULT nextval('attacksimulator.received_data_id_seq'::regclass);


--
-- TOC entry 2980 (class 2604 OID 399219)
-- Name: runner id; Type: DEFAULT; Schema: attacksimulator; Owner: towerstreet
--

ALTER TABLE ONLY attacksimulator.runner ALTER COLUMN id SET DEFAULT nextval('attacksimulator.runner_id_seq'::regclass);


--
-- TOC entry 2983 (class 2604 OID 399220)
-- Name: simulation id; Type: DEFAULT; Schema: attacksimulator; Owner: towerstreet
--

ALTER TABLE ONLY attacksimulator.simulation ALTER COLUMN id SET DEFAULT nextval('attacksimulator.simulation_id_seq'::regclass);


--
-- TOC entry 2985 (class 2604 OID 399221)
-- Name: simulation_outcome id; Type: DEFAULT; Schema: attacksimulator; Owner: towerstreet
--

ALTER TABLE ONLY attacksimulator.simulation_outcome ALTER COLUMN id SET DEFAULT nextval('attacksimulator.simulation_outcome_id_seq'::regclass);


--
-- TOC entry 3001 (class 2604 OID 400180)
-- Name: simulation_template id; Type: DEFAULT; Schema: attacksimulator; Owner: towerstreet
--

ALTER TABLE ONLY attacksimulator.simulation_template ALTER COLUMN id SET DEFAULT nextval('attacksimulator.simulation_template_id_seq'::regclass);


--
-- TOC entry 2987 (class 2604 OID 399222)
-- Name: task id; Type: DEFAULT; Schema: attacksimulator; Owner: towerstreet
--

ALTER TABLE ONLY attacksimulator.task ALTER COLUMN id SET DEFAULT nextval('attacksimulator.task_id_seq'::regclass);


--
-- TOC entry 3002 (class 2604 OID 400218)
-- Name: test_case id; Type: DEFAULT; Schema: attacksimulator; Owner: towerstreet
--

ALTER TABLE ONLY attacksimulator.test_case ALTER COLUMN id SET DEFAULT nextval('attacksimulator.test_case_id_seq'::regclass);


--
-- TOC entry 2996 (class 2604 OID 399980)
-- Name: url_test id; Type: DEFAULT; Schema: attacksimulator; Owner: towerstreet
--

ALTER TABLE ONLY attacksimulator.url_test ALTER COLUMN id SET DEFAULT nextval('attacksimulator.url_test_id_seq'::regclass);


--
-- TOC entry 2989 (class 2604 OID 399223)
-- Name: user id; Type: DEFAULT; Schema: attacksimulator; Owner: towerstreet
--

ALTER TABLE ONLY attacksimulator."user" ALTER COLUMN id SET DEFAULT nextval('attacksimulator.user_id_seq'::regclass);


--
-- TOC entry 2998 (class 2604 OID 400098)
-- Name: assessment_type id; Type: DEFAULT; Schema: public; Owner: towerstreet
--

ALTER TABLE ONLY public.assessment_type ALTER COLUMN id SET DEFAULT nextval('public.assessment_type_id_seq'::regclass);


--
-- TOC entry 3008 (class 2604 OID 400838)
-- Name: campaign_visitor id; Type: DEFAULT; Schema: public; Owner: towerstreet
--

ALTER TABLE ONLY public.campaign_visitor ALTER COLUMN id SET DEFAULT nextval('public.campaign_visitor_id_seq'::regclass);


--
-- TOC entry 2991 (class 2604 OID 399631)
-- Name: customer id; Type: DEFAULT; Schema: public; Owner: towerstreet
--

ALTER TABLE ONLY public.customer ALTER COLUMN id SET DEFAULT nextval('public.company_id_seq'::regclass);


--
-- TOC entry 3000 (class 2604 OID 400112)
-- Name: customer_assessment id; Type: DEFAULT; Schema: public; Owner: towerstreet
--

ALTER TABLE ONLY public.customer_assessment ALTER COLUMN id SET DEFAULT nextval('public.customer_assessment_id_seq'::regclass);


--
-- TOC entry 3004 (class 2604 OID 400348)
-- Name: scoring_definition id; Type: DEFAULT; Schema: scoring; Owner: towerstreet
--

ALTER TABLE ONLY scoring.scoring_definition ALTER COLUMN id SET DEFAULT nextval('scoring.scoring_definition_id_seq'::regclass);


--
-- TOC entry 3006 (class 2604 OID 400362)
-- Name: scoring_outcome id; Type: DEFAULT; Schema: scoring; Owner: towerstreet
--

ALTER TABLE ONLY scoring.scoring_outcome ALTER COLUMN id SET DEFAULT nextval('scoring.scoring_outcome_id_seq'::regclass);


--
-- TOC entry 3007 (class 2604 OID 400380)
-- Name: scoring_result id; Type: DEFAULT; Schema: scoring; Owner: towerstreet
--

ALTER TABLE ONLY scoring.scoring_result ALTER COLUMN id SET DEFAULT nextval('scoring.scoring_result_id_seq'::regclass);


--
-- TOC entry 3239 (class 0 OID 398975)
-- Dependencies: 199
-- Data for Name: client_request; Type: TABLE DATA; Schema: attacksimulator; Owner: towerstreet
--



--
-- TOC entry 3241 (class 0 OID 398983)
-- Dependencies: 201
-- Data for Name: network_segment; Type: TABLE DATA; Schema: attacksimulator; Owner: towerstreet
--



--
-- TOC entry 3243 (class 0 OID 398991)
-- Dependencies: 203
-- Data for Name: outcome_task_result; Type: TABLE DATA; Schema: attacksimulator; Owner: towerstreet
--



--
-- TOC entry 3245 (class 0 OID 398999)
-- Dependencies: 205
-- Data for Name: received_data; Type: TABLE DATA; Schema: attacksimulator; Owner: towerstreet
--



--
-- TOC entry 3247 (class 0 OID 399008)
-- Dependencies: 207
-- Data for Name: runner; Type: TABLE DATA; Schema: attacksimulator; Owner: towerstreet
--

INSERT INTO attacksimulator.runner VALUES (1, 'CIS74Test');
INSERT INTO attacksimulator.runner VALUES (2, 'CIS77WannaCryTest');
INSERT INTO attacksimulator.runner VALUES (3, 'CIS77EicarTest');
INSERT INTO attacksimulator.runner VALUES (4, 'CIS133FileExfiltrationTest');
INSERT INTO attacksimulator.runner VALUES (5, 'CIS141LocalNetworkScanTest');
INSERT INTO attacksimulator.runner VALUES (6, 'CIS142ArbitraryNetworkScanTest');
INSERT INTO attacksimulator.runner VALUES (7, 'EnvironmentTest');


--
-- TOC entry 3249 (class 0 OID 399014)
-- Dependencies: 209
-- Data for Name: simulation; Type: TABLE DATA; Schema: attacksimulator; Owner: towerstreet
--

INSERT INTO attacksimulator.simulation VALUES (1, 1, '00000000-0000-0000-0000-000000000000', 'test simulation', 1, 4);
INSERT INTO attacksimulator.simulation VALUES (2, 1, '4bf91c4f-381d-4c2d-9f21-74ec24d67145', 'CIS 7.4 connects to cnn.com', 1, 5);
INSERT INTO attacksimulator.simulation VALUES (3, 1, '3266e140-727d-4910-8f46-a585ee3cd31a', 'CIS 7.4 connects to asdgkjaiu984nldg83lsd.cx', 1, 6);
INSERT INTO attacksimulator.simulation VALUES (4, 1, '63855f63-4da7-4662-8b6e-a41ec8e14fbe', 'CIS 7.4 connects to kjg9834kagfsdixs.com', 1, 7);
INSERT INTO attacksimulator.simulation VALUES (5, 1, '88bdc3b4-bbc5-43fb-a1d2-c3e317f3c068', 'CIS 7.7 WannaCry', 1, 8);
INSERT INTO attacksimulator.simulation VALUES (6, 1, 'cf62c8ac-b559-493b-b63c-8ef817283445', 'CIS 7.9 eicar .com', 1, 9);
INSERT INTO attacksimulator.simulation VALUES (7, 1, 'ee0190ae-dbb1-4c24-b53f-25009c7abace', 'CIS 7.9 eicar .zip', 1, 10);
INSERT INTO attacksimulator.simulation VALUES (8, 1, 'fc0e2647-d0cc-44eb-be7d-4f413f501f84', 'CIS 13.3 exfiltrate CSV file', 1, 11);
INSERT INTO attacksimulator.simulation VALUES (9, 1, '8d2ad532-4842-4ea9-992b-7a042fb3b13b', 'CIS 13.3 exfiltrate XLSX file', 1, 12);
INSERT INTO attacksimulator.simulation VALUES (10, 1, '0bb50f52-89c8-499e-a498-39e91c4e7ec7', 'CIS 13.3 exfiltrate DOCX file', 1, 13);
INSERT INTO attacksimulator.simulation VALUES (11, 1, '9779d9de-e09f-45ee-ad06-2d36833b06a1', 'CIS 14.1 port scan local network', 1, 14);
INSERT INTO attacksimulator.simulation VALUES (12, 1, 'be9829d2-17c8-44c3-aa9b-d13951b1d449', 'CIS 14.2 port scan arbitrary networks', 1, 15);
INSERT INTO attacksimulator.simulation VALUES (13, 1, '92773881-720f-47d2-bb24-84808def2f23', 'Complete simulation scenario', 1, 16);
INSERT INTO attacksimulator.simulation VALUES (14, 1, 'cd56decc-1b14-496b-be42-6bcb965e554d', 'CIS 7.4 Attack simulator url checking list', 1, 17);
INSERT INTO attacksimulator.simulation VALUES (15, 1, '76ef5939-2010-4cf3-bb00-5dd4468c1b9f', 'CIS 7.4 baseline tests', 1, 18);
INSERT INTO attacksimulator.simulation VALUES (16, 1, '3eada3ac-9c25-49ed-9c34-9f5fbec278d6', 'Browser detection test', 1, 23);
INSERT INTO attacksimulator.simulation VALUES (17, 2, '180d20a2-34eb-4a67-93c8-e862db1f51c8', 'TS Simulator campaign', 2, 25);
INSERT INTO attacksimulator.simulation VALUES (18, 1, 'f8c4bcc2-8d5e-4dea-8a89-e28f17213eff', 'Full assessment', 1, 26);


--
-- TOC entry 3251 (class 0 OID 399027)
-- Dependencies: 211
-- Data for Name: simulation_outcome; Type: TABLE DATA; Schema: attacksimulator; Owner: towerstreet
--



--
-- TOC entry 3266 (class 0 OID 400177)
-- Dependencies: 226
-- Data for Name: simulation_template; Type: TABLE DATA; Schema: attacksimulator; Owner: towerstreet
--

INSERT INTO attacksimulator.simulation_template VALUES (1, '2019-05-02 08:46:51.846771', 1, NULL);
INSERT INTO attacksimulator.simulation_template VALUES (2, '2019-05-02 08:46:51.846771', 2, NULL);
INSERT INTO attacksimulator.simulation_template VALUES (3, '2019-05-02 08:46:51.846771', 3, NULL);
INSERT INTO attacksimulator.simulation_template VALUES (4, '1970-01-01 01:00:01', 4, 'test simulation');
INSERT INTO attacksimulator.simulation_template VALUES (5, '1970-01-01 01:00:02', 4, 'CIS 7.4 connects to cnn.com');
INSERT INTO attacksimulator.simulation_template VALUES (6, '1970-01-01 01:00:03', 4, 'CIS 7.4 connects to asdgkjaiu984nldg83lsd.cx');
INSERT INTO attacksimulator.simulation_template VALUES (7, '1970-01-01 01:00:04', 4, 'CIS 7.4 connects to kjg9834kagfsdixs.com');
INSERT INTO attacksimulator.simulation_template VALUES (8, '1970-01-01 01:00:05', 4, 'CIS 7.7 WannaCry');
INSERT INTO attacksimulator.simulation_template VALUES (9, '1970-01-01 01:00:06', 4, 'CIS 7.9 eicar .com');
INSERT INTO attacksimulator.simulation_template VALUES (10, '1970-01-01 01:00:07', 4, 'CIS 7.9 eicar .zip');
INSERT INTO attacksimulator.simulation_template VALUES (11, '1970-01-01 01:00:08', 4, 'CIS 13.3 exfiltrate CSV file');
INSERT INTO attacksimulator.simulation_template VALUES (12, '1970-01-01 01:00:09', 4, 'CIS 13.3 exfiltrate XLSX file');
INSERT INTO attacksimulator.simulation_template VALUES (13, '1970-01-01 01:00:10', 4, 'CIS 13.3 exfiltrate DOCX file');
INSERT INTO attacksimulator.simulation_template VALUES (14, '1970-01-01 01:00:11', 4, 'CIS 14.1 port scan local network');
INSERT INTO attacksimulator.simulation_template VALUES (15, '1970-01-01 01:00:12', 4, 'CIS 14.2 port scan arbitrary networks');
INSERT INTO attacksimulator.simulation_template VALUES (16, '1970-01-01 01:00:13', 4, 'Complete simulation scenario');
INSERT INTO attacksimulator.simulation_template VALUES (17, '1970-01-01 01:00:14', 4, 'CIS 7.4 Attack simulator url checking list');
INSERT INTO attacksimulator.simulation_template VALUES (18, '1970-01-02 00:00:00', 4, 'CIS 7.4 baseline tests');
INSERT INTO attacksimulator.simulation_template VALUES (19, '2019-05-02 08:48:28.139611', 1, NULL);
INSERT INTO attacksimulator.simulation_template VALUES (20, '2019-05-02 08:48:28.139611', 2, NULL);
INSERT INTO attacksimulator.simulation_template VALUES (21, '2019-05-02 08:48:28.139611', 3, NULL);
INSERT INTO attacksimulator.simulation_template VALUES (22, '2019-05-02 08:48:28.250469', 5, 'Ultra-light assessment simulation template');
INSERT INTO attacksimulator.simulation_template VALUES (23, '1970-01-03 00:00:00', 4, 'Browser detection test');
INSERT INTO attacksimulator.simulation_template VALUES (24, '2019-05-02 08:48:29.234613', 6, 'Simulator 2019 03 RSA campaign');
INSERT INTO attacksimulator.simulation_template VALUES (25, '2019-05-02 08:48:29.303475', 6, 'Simulator 2019 03 RSA campaign');
INSERT INTO attacksimulator.simulation_template VALUES (26, '2019-05-02 08:48:29.546484', 1, 'light');
INSERT INTO attacksimulator.simulation_template VALUES (27, '2019-05-02 08:48:29.546484', 2, 'medium');
INSERT INTO attacksimulator.simulation_template VALUES (28, '2019-05-02 08:48:29.546484', 3, 'continuous');
INSERT INTO attacksimulator.simulation_template VALUES (29, '2019-05-02 08:48:29.546484', 5, 'ultra-light');


--
-- TOC entry 3267 (class 0 OID 400190)
-- Dependencies: 227
-- Data for Name: simulation_template_config; Type: TABLE DATA; Schema: attacksimulator; Owner: towerstreet
--

INSERT INTO attacksimulator.simulation_template_config VALUES (1, 59, 59, 10);
INSERT INTO attacksimulator.simulation_template_config VALUES (1, 60, 60, 10);
INSERT INTO attacksimulator.simulation_template_config VALUES (1, 61, 61, 10);
INSERT INTO attacksimulator.simulation_template_config VALUES (1, 62, 62, 10);
INSERT INTO attacksimulator.simulation_template_config VALUES (2, 59, 59, 10);
INSERT INTO attacksimulator.simulation_template_config VALUES (2, 60, 60, 10);
INSERT INTO attacksimulator.simulation_template_config VALUES (2, 61, 61, 10);
INSERT INTO attacksimulator.simulation_template_config VALUES (2, 62, 62, 10);
INSERT INTO attacksimulator.simulation_template_config VALUES (3, 59, 59, 10);
INSERT INTO attacksimulator.simulation_template_config VALUES (3, 60, 60, 10);
INSERT INTO attacksimulator.simulation_template_config VALUES (3, 61, 61, 10);
INSERT INTO attacksimulator.simulation_template_config VALUES (3, 62, 62, 10);
INSERT INTO attacksimulator.simulation_template_config VALUES (18, 59, 59, 10);
INSERT INTO attacksimulator.simulation_template_config VALUES (18, 60, 60, 10);
INSERT INTO attacksimulator.simulation_template_config VALUES (18, 61, 61, 10);
INSERT INTO attacksimulator.simulation_template_config VALUES (18, 62, 62, 10);
INSERT INTO attacksimulator.simulation_template_config VALUES (1, 63, 63, 9);
INSERT INTO attacksimulator.simulation_template_config VALUES (2, 63, 63, 9);
INSERT INTO attacksimulator.simulation_template_config VALUES (3, 63, 63, 9);
INSERT INTO attacksimulator.simulation_template_config VALUES (20, 13, 2, 9);
INSERT INTO attacksimulator.simulation_template_config VALUES (21, 13, 2, 9);
INSERT INTO attacksimulator.simulation_template_config VALUES (23, 64, 1, 14);
INSERT INTO attacksimulator.simulation_template_config VALUES (19, 64, 1, 14);
INSERT INTO attacksimulator.simulation_template_config VALUES (20, 64, 1, 14);
INSERT INTO attacksimulator.simulation_template_config VALUES (21, 64, 1, 14);
INSERT INTO attacksimulator.simulation_template_config VALUES (22, 64, 1, 14);
INSERT INTO attacksimulator.simulation_template_config VALUES (21, 61, 3, 10);
INSERT INTO attacksimulator.simulation_template_config VALUES (19, 61, 3, 10);
INSERT INTO attacksimulator.simulation_template_config VALUES (20, 61, 3, 10);
INSERT INTO attacksimulator.simulation_template_config VALUES (21, 59, 4, 10);
INSERT INTO attacksimulator.simulation_template_config VALUES (2, 1, 1, 1);
INSERT INTO attacksimulator.simulation_template_config VALUES (4, 1, 1, 1);
INSERT INTO attacksimulator.simulation_template_config VALUES (1, 1, 1, 1);
INSERT INTO attacksimulator.simulation_template_config VALUES (3, 1, 1, 1);
INSERT INTO attacksimulator.simulation_template_config VALUES (3, 2, 2, 1);
INSERT INTO attacksimulator.simulation_template_config VALUES (4, 2, 2, 1);
INSERT INTO attacksimulator.simulation_template_config VALUES (1, 2, 2, 1);
INSERT INTO attacksimulator.simulation_template_config VALUES (2, 2, 2, 1);
INSERT INTO attacksimulator.simulation_template_config VALUES (8, 3, 3, 6);
INSERT INTO attacksimulator.simulation_template_config VALUES (1, 3, 3, 6);
INSERT INTO attacksimulator.simulation_template_config VALUES (16, 3, 3, 6);
INSERT INTO attacksimulator.simulation_template_config VALUES (3, 3, 3, 6);
INSERT INTO attacksimulator.simulation_template_config VALUES (4, 3, 3, 6);
INSERT INTO attacksimulator.simulation_template_config VALUES (2, 3, 3, 6);
INSERT INTO attacksimulator.simulation_template_config VALUES (3, 4, 4, 1);
INSERT INTO attacksimulator.simulation_template_config VALUES (2, 4, 4, 1);
INSERT INTO attacksimulator.simulation_template_config VALUES (1, 4, 4, 1);
INSERT INTO attacksimulator.simulation_template_config VALUES (16, 4, 4, 1);
INSERT INTO attacksimulator.simulation_template_config VALUES (5, 4, 4, 1);
INSERT INTO attacksimulator.simulation_template_config VALUES (3, 5, 5, 1);
INSERT INTO attacksimulator.simulation_template_config VALUES (2, 5, 5, 1);
INSERT INTO attacksimulator.simulation_template_config VALUES (6, 5, 5, 1);
INSERT INTO attacksimulator.simulation_template_config VALUES (1, 5, 5, 1);
INSERT INTO attacksimulator.simulation_template_config VALUES (16, 5, 5, 1);
INSERT INTO attacksimulator.simulation_template_config VALUES (1, 6, 6, 1);
INSERT INTO attacksimulator.simulation_template_config VALUES (3, 6, 6, 1);
INSERT INTO attacksimulator.simulation_template_config VALUES (7, 6, 6, 1);
INSERT INTO attacksimulator.simulation_template_config VALUES (16, 6, 6, 1);
INSERT INTO attacksimulator.simulation_template_config VALUES (2, 6, 6, 1);
INSERT INTO attacksimulator.simulation_template_config VALUES (3, 7, 7, 6);
INSERT INTO attacksimulator.simulation_template_config VALUES (8, 7, 7, 6);
INSERT INTO attacksimulator.simulation_template_config VALUES (1, 7, 7, 6);
INSERT INTO attacksimulator.simulation_template_config VALUES (2, 7, 7, 6);
INSERT INTO attacksimulator.simulation_template_config VALUES (16, 7, 7, 6);
INSERT INTO attacksimulator.simulation_template_config VALUES (1, 8, 8, 7);
INSERT INTO attacksimulator.simulation_template_config VALUES (16, 8, 8, 7);
INSERT INTO attacksimulator.simulation_template_config VALUES (2, 8, 8, 7);
INSERT INTO attacksimulator.simulation_template_config VALUES (3, 8, 8, 7);
INSERT INTO attacksimulator.simulation_template_config VALUES (9, 8, 8, 7);
INSERT INTO attacksimulator.simulation_template_config VALUES (1, 9, 9, 7);
INSERT INTO attacksimulator.simulation_template_config VALUES (10, 9, 9, 7);
INSERT INTO attacksimulator.simulation_template_config VALUES (16, 9, 9, 7);
INSERT INTO attacksimulator.simulation_template_config VALUES (2, 9, 9, 7);
INSERT INTO attacksimulator.simulation_template_config VALUES (3, 9, 9, 7);
INSERT INTO attacksimulator.simulation_template_config VALUES (2, 10, 10, 8);
INSERT INTO attacksimulator.simulation_template_config VALUES (1, 10, 10, 8);
INSERT INTO attacksimulator.simulation_template_config VALUES (16, 10, 10, 8);
INSERT INTO attacksimulator.simulation_template_config VALUES (11, 10, 10, 8);
INSERT INTO attacksimulator.simulation_template_config VALUES (3, 10, 10, 8);
INSERT INTO attacksimulator.simulation_template_config VALUES (16, 11, 11, 8);
INSERT INTO attacksimulator.simulation_template_config VALUES (1, 11, 11, 8);
INSERT INTO attacksimulator.simulation_template_config VALUES (3, 11, 11, 8);
INSERT INTO attacksimulator.simulation_template_config VALUES (12, 11, 11, 8);
INSERT INTO attacksimulator.simulation_template_config VALUES (2, 11, 11, 8);
INSERT INTO attacksimulator.simulation_template_config VALUES (16, 12, 12, 8);
INSERT INTO attacksimulator.simulation_template_config VALUES (1, 12, 12, 8);
INSERT INTO attacksimulator.simulation_template_config VALUES (2, 12, 12, 8);
INSERT INTO attacksimulator.simulation_template_config VALUES (13, 12, 12, 8);
INSERT INTO attacksimulator.simulation_template_config VALUES (3, 12, 12, 8);
INSERT INTO attacksimulator.simulation_template_config VALUES (2, 13, 13, 9);
INSERT INTO attacksimulator.simulation_template_config VALUES (3, 13, 13, 9);
INSERT INTO attacksimulator.simulation_template_config VALUES (16, 13, 13, 9);
INSERT INTO attacksimulator.simulation_template_config VALUES (1, 13, 13, 9);
INSERT INTO attacksimulator.simulation_template_config VALUES (14, 13, 13, 9);
INSERT INTO attacksimulator.simulation_template_config VALUES (16, 14, 14, 9);
INSERT INTO attacksimulator.simulation_template_config VALUES (15, 14, 14, 9);
INSERT INTO attacksimulator.simulation_template_config VALUES (16, 15, 15, 2);
INSERT INTO attacksimulator.simulation_template_config VALUES (2, 15, 15, 2);
INSERT INTO attacksimulator.simulation_template_config VALUES (17, 15, 15, 2);
INSERT INTO attacksimulator.simulation_template_config VALUES (1, 15, 15, 2);
INSERT INTO attacksimulator.simulation_template_config VALUES (3, 15, 15, 2);
INSERT INTO attacksimulator.simulation_template_config VALUES (2, 16, 16, 2);
INSERT INTO attacksimulator.simulation_template_config VALUES (3, 16, 16, 2);
INSERT INTO attacksimulator.simulation_template_config VALUES (16, 16, 16, 2);
INSERT INTO attacksimulator.simulation_template_config VALUES (17, 16, 16, 2);
INSERT INTO attacksimulator.simulation_template_config VALUES (1, 16, 16, 2);
INSERT INTO attacksimulator.simulation_template_config VALUES (3, 17, 17, 2);
INSERT INTO attacksimulator.simulation_template_config VALUES (2, 17, 17, 2);
INSERT INTO attacksimulator.simulation_template_config VALUES (17, 17, 17, 2);
INSERT INTO attacksimulator.simulation_template_config VALUES (16, 17, 17, 2);
INSERT INTO attacksimulator.simulation_template_config VALUES (1, 17, 17, 2);
INSERT INTO attacksimulator.simulation_template_config VALUES (2, 18, 18, 2);
INSERT INTO attacksimulator.simulation_template_config VALUES (16, 18, 18, 2);
INSERT INTO attacksimulator.simulation_template_config VALUES (3, 18, 18, 2);
INSERT INTO attacksimulator.simulation_template_config VALUES (1, 18, 18, 2);
INSERT INTO attacksimulator.simulation_template_config VALUES (17, 18, 18, 2);
INSERT INTO attacksimulator.simulation_template_config VALUES (17, 19, 19, 2);
INSERT INTO attacksimulator.simulation_template_config VALUES (1, 19, 19, 2);
INSERT INTO attacksimulator.simulation_template_config VALUES (20, 59, 4, 10);
INSERT INTO attacksimulator.simulation_template_config VALUES (19, 59, 4, 10);
INSERT INTO attacksimulator.simulation_template_config VALUES (20, 60, 5, 10);
INSERT INTO attacksimulator.simulation_template_config VALUES (21, 60, 5, 10);
INSERT INTO attacksimulator.simulation_template_config VALUES (19, 60, 5, 10);
INSERT INTO attacksimulator.simulation_template_config VALUES (21, 62, 6, 10);
INSERT INTO attacksimulator.simulation_template_config VALUES (19, 62, 6, 10);
INSERT INTO attacksimulator.simulation_template_config VALUES (20, 62, 6, 10);
INSERT INTO attacksimulator.simulation_template_config VALUES (20, 1, 7, 11);
INSERT INTO attacksimulator.simulation_template_config VALUES (21, 1, 7, 11);
INSERT INTO attacksimulator.simulation_template_config VALUES (19, 1, 7, 11);
INSERT INTO attacksimulator.simulation_template_config VALUES (20, 2, 8, 12);
INSERT INTO attacksimulator.simulation_template_config VALUES (19, 2, 8, 12);
INSERT INTO attacksimulator.simulation_template_config VALUES (21, 2, 8, 12);
INSERT INTO attacksimulator.simulation_template_config VALUES (21, 4, 9, 13);
INSERT INTO attacksimulator.simulation_template_config VALUES (20, 4, 9, 13);
INSERT INTO attacksimulator.simulation_template_config VALUES (21, 15, 10, 2);
INSERT INTO attacksimulator.simulation_template_config VALUES (21, 12, 57, 8);
INSERT INTO attacksimulator.simulation_template_config VALUES (20, 12, 57, 8);
INSERT INTO attacksimulator.simulation_template_config VALUES (19, 63, 58, 9);
INSERT INTO attacksimulator.simulation_template_config VALUES (21, 63, 58, 9);
INSERT INTO attacksimulator.simulation_template_config VALUES (20, 63, 58, 9);
INSERT INTO attacksimulator.simulation_template_config VALUES (16, 19, 19, 2);
INSERT INTO attacksimulator.simulation_template_config VALUES (2, 19, 19, 2);
INSERT INTO attacksimulator.simulation_template_config VALUES (3, 19, 19, 2);
INSERT INTO attacksimulator.simulation_template_config VALUES (17, 22, 22, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (2, 22, 22, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (3, 22, 22, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (16, 22, 22, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (1, 22, 22, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (2, 23, 23, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (16, 23, 23, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (1, 23, 23, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (3, 23, 23, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (17, 23, 23, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (16, 24, 24, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (17, 24, 24, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (2, 24, 24, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (1, 24, 24, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (3, 24, 24, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (2, 25, 25, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (3, 25, 25, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (1, 25, 25, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (16, 25, 25, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (17, 25, 25, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (17, 26, 26, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (3, 26, 26, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (1, 26, 26, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (2, 26, 26, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (16, 26, 26, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (17, 27, 27, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (16, 27, 27, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (1, 27, 27, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (2, 27, 27, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (3, 27, 27, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (2, 28, 28, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (3, 28, 28, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (1, 28, 28, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (16, 28, 28, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (17, 28, 28, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (2, 29, 29, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (1, 29, 29, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (3, 29, 29, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (16, 29, 29, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (17, 29, 29, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (3, 30, 30, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (1, 30, 30, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (2, 30, 30, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (17, 30, 30, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (16, 30, 30, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (2, 31, 31, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (16, 31, 31, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (1, 31, 31, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (3, 31, 31, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (17, 31, 31, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (16, 32, 32, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (3, 32, 32, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (17, 32, 32, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (1, 32, 32, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (2, 32, 32, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (16, 33, 33, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (3, 33, 33, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (17, 33, 33, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (1, 33, 33, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (2, 33, 33, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (3, 34, 34, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (2, 34, 34, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (16, 34, 34, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (17, 34, 34, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (1, 34, 34, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (2, 35, 35, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (3, 35, 35, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (16, 35, 35, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (1, 35, 35, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (17, 35, 35, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (3, 36, 36, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (17, 36, 36, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (1, 36, 36, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (2, 36, 36, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (16, 36, 36, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (16, 37, 37, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (1, 37, 37, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (17, 37, 37, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (3, 37, 37, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (2, 37, 37, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (17, 38, 38, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (3, 38, 38, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (1, 38, 38, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (2, 38, 38, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (16, 38, 38, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (2, 39, 39, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (3, 39, 39, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (1, 39, 39, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (16, 39, 39, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (17, 39, 39, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (16, 40, 40, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (17, 40, 40, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (2, 40, 40, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (1, 40, 40, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (3, 40, 40, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (16, 41, 41, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (3, 41, 41, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (2, 41, 41, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (17, 41, 41, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (1, 41, 41, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (16, 42, 42, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (1, 42, 42, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (17, 42, 42, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (2, 42, 42, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (3, 42, 42, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (3, 43, 43, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (16, 43, 43, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (1, 43, 43, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (17, 43, 43, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (2, 43, 43, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (1, 44, 44, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (2, 44, 44, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (16, 44, 44, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (17, 44, 44, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (3, 44, 44, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (17, 45, 45, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (16, 45, 45, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (1, 45, 45, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (2, 45, 45, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (3, 45, 45, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (3, 46, 46, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (16, 46, 46, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (17, 46, 46, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (1, 46, 46, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (2, 46, 46, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (2, 47, 47, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (3, 47, 47, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (1, 47, 47, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (16, 47, 47, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (17, 47, 47, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (2, 48, 48, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (1, 48, 48, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (17, 48, 48, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (3, 48, 48, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (16, 48, 48, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (2, 49, 49, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (3, 49, 49, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (16, 49, 49, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (17, 49, 49, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (1, 49, 49, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (3, 50, 50, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (16, 50, 50, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (17, 50, 50, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (2, 50, 50, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (1, 50, 50, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (2, 51, 51, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (16, 51, 51, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (17, 51, 51, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (1, 51, 51, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (3, 51, 51, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (1, 52, 52, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (16, 52, 52, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (2, 52, 52, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (3, 52, 52, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (17, 52, 52, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (3, 53, 53, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (17, 53, 53, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (2, 53, 53, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (1, 53, 53, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (16, 53, 53, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (3, 54, 54, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (2, 54, 54, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (1, 54, 54, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (17, 54, 54, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (16, 54, 54, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (2, 55, 55, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (17, 55, 55, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (16, 55, 55, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (1, 55, 55, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (3, 55, 55, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (3, 56, 56, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (17, 56, 56, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (2, 56, 56, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (1, 56, 56, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (16, 56, 56, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (16, 57, 57, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (2, 57, 57, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (17, 57, 57, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (3, 57, 57, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (1, 57, 57, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (2, 58, 58, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (1, 58, 58, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (3, 58, 58, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (17, 58, 58, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (16, 58, 58, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (20, 15, 10, 2);
INSERT INTO attacksimulator.simulation_template_config VALUES (19, 15, 10, 2);
INSERT INTO attacksimulator.simulation_template_config VALUES (19, 16, 11, 2);
INSERT INTO attacksimulator.simulation_template_config VALUES (20, 16, 11, 2);
INSERT INTO attacksimulator.simulation_template_config VALUES (21, 16, 11, 2);
INSERT INTO attacksimulator.simulation_template_config VALUES (19, 17, 12, 2);
INSERT INTO attacksimulator.simulation_template_config VALUES (21, 17, 12, 2);
INSERT INTO attacksimulator.simulation_template_config VALUES (20, 17, 12, 2);
INSERT INTO attacksimulator.simulation_template_config VALUES (21, 18, 13, 2);
INSERT INTO attacksimulator.simulation_template_config VALUES (19, 18, 13, 2);
INSERT INTO attacksimulator.simulation_template_config VALUES (20, 18, 13, 2);
INSERT INTO attacksimulator.simulation_template_config VALUES (19, 19, 14, 2);
INSERT INTO attacksimulator.simulation_template_config VALUES (20, 19, 14, 2);
INSERT INTO attacksimulator.simulation_template_config VALUES (21, 19, 14, 2);
INSERT INTO attacksimulator.simulation_template_config VALUES (21, 22, 15, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (19, 22, 15, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (20, 22, 15, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (20, 23, 16, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (19, 23, 16, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (21, 23, 16, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (21, 24, 17, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (20, 24, 17, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (19, 24, 17, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (19, 25, 18, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (20, 25, 18, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (21, 25, 18, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (20, 26, 19, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (21, 26, 19, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (19, 26, 19, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (20, 27, 20, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (21, 27, 20, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (19, 27, 20, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (19, 28, 21, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (21, 28, 21, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (20, 28, 21, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (20, 29, 22, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (19, 29, 22, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (21, 29, 22, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (21, 30, 23, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (20, 30, 23, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (19, 30, 23, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (20, 32, 24, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (21, 32, 24, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (19, 32, 24, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (20, 33, 25, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (21, 33, 25, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (19, 33, 25, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (19, 34, 26, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (20, 34, 26, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (21, 34, 26, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (20, 35, 27, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (21, 35, 27, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (19, 35, 27, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (20, 36, 28, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (21, 36, 28, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (19, 36, 28, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (19, 37, 29, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (20, 37, 29, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (21, 37, 29, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (20, 38, 30, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (21, 38, 30, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (19, 38, 30, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (21, 39, 31, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (19, 39, 31, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (20, 39, 31, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (21, 40, 32, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (20, 40, 32, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (19, 40, 32, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (21, 41, 33, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (19, 41, 33, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (20, 41, 33, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (21, 42, 34, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (20, 42, 34, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (19, 42, 34, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (19, 43, 35, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (20, 43, 35, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (21, 43, 35, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (19, 44, 36, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (20, 44, 36, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (21, 44, 36, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (19, 45, 37, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (21, 45, 37, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (20, 45, 37, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (20, 46, 38, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (19, 46, 38, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (21, 46, 38, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (20, 47, 39, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (19, 47, 39, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (21, 47, 39, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (21, 48, 40, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (19, 48, 40, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (20, 48, 40, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (20, 49, 41, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (19, 49, 41, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (21, 49, 41, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (21, 50, 42, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (19, 50, 42, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (20, 50, 42, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (21, 51, 43, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (19, 51, 43, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (20, 51, 43, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (20, 52, 44, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (19, 52, 44, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (21, 52, 44, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (19, 53, 45, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (20, 53, 45, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (21, 53, 45, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (19, 54, 46, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (20, 54, 46, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (21, 54, 46, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (21, 55, 47, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (20, 55, 47, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (19, 55, 47, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (20, 56, 48, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (19, 56, 48, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (21, 56, 48, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (19, 57, 49, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (20, 57, 49, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (21, 57, 49, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (21, 58, 50, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (20, 58, 50, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (19, 58, 50, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (20, 3, 51, 6);
INSERT INTO attacksimulator.simulation_template_config VALUES (19, 3, 51, 6);
INSERT INTO attacksimulator.simulation_template_config VALUES (21, 3, 51, 6);
INSERT INTO attacksimulator.simulation_template_config VALUES (20, 7, 52, 6);
INSERT INTO attacksimulator.simulation_template_config VALUES (19, 7, 52, 6);
INSERT INTO attacksimulator.simulation_template_config VALUES (21, 7, 52, 6);
INSERT INTO attacksimulator.simulation_template_config VALUES (19, 8, 53, 7);
INSERT INTO attacksimulator.simulation_template_config VALUES (21, 8, 53, 7);
INSERT INTO attacksimulator.simulation_template_config VALUES (20, 8, 53, 7);
INSERT INTO attacksimulator.simulation_template_config VALUES (20, 9, 54, 7);
INSERT INTO attacksimulator.simulation_template_config VALUES (21, 9, 54, 7);
INSERT INTO attacksimulator.simulation_template_config VALUES (19, 9, 54, 7);
INSERT INTO attacksimulator.simulation_template_config VALUES (21, 10, 55, 8);
INSERT INTO attacksimulator.simulation_template_config VALUES (19, 10, 55, 8);
INSERT INTO attacksimulator.simulation_template_config VALUES (20, 10, 55, 8);
INSERT INTO attacksimulator.simulation_template_config VALUES (19, 11, 56, 8);
INSERT INTO attacksimulator.simulation_template_config VALUES (20, 11, 56, 8);
INSERT INTO attacksimulator.simulation_template_config VALUES (21, 11, 56, 8);
INSERT INTO attacksimulator.simulation_template_config VALUES (19, 12, 57, 8);
INSERT INTO attacksimulator.simulation_template_config VALUES (22, 61, 3, 10);
INSERT INTO attacksimulator.simulation_template_config VALUES (22, 59, 4, 10);
INSERT INTO attacksimulator.simulation_template_config VALUES (22, 60, 5, 10);
INSERT INTO attacksimulator.simulation_template_config VALUES (22, 62, 6, 10);
INSERT INTO attacksimulator.simulation_template_config VALUES (22, 1, 7, 11);
INSERT INTO attacksimulator.simulation_template_config VALUES (22, 2, 8, 12);
INSERT INTO attacksimulator.simulation_template_config VALUES (22, 15, 10, 2);
INSERT INTO attacksimulator.simulation_template_config VALUES (22, 16, 11, 2);
INSERT INTO attacksimulator.simulation_template_config VALUES (22, 17, 12, 2);
INSERT INTO attacksimulator.simulation_template_config VALUES (22, 18, 13, 2);
INSERT INTO attacksimulator.simulation_template_config VALUES (22, 19, 14, 2);
INSERT INTO attacksimulator.simulation_template_config VALUES (22, 22, 15, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (22, 23, 16, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (22, 24, 17, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (22, 25, 18, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (22, 26, 19, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (22, 27, 20, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (22, 28, 21, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (22, 29, 22, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (22, 30, 23, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (22, 32, 24, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (22, 33, 25, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (22, 34, 26, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (22, 35, 27, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (22, 36, 28, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (22, 37, 29, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (22, 38, 30, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (22, 39, 31, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (22, 40, 32, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (22, 41, 33, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (22, 42, 34, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (22, 43, 35, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (22, 44, 36, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (22, 45, 37, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (22, 46, 38, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (22, 47, 39, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (22, 48, 40, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (22, 49, 41, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (22, 50, 42, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (22, 51, 43, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (22, 52, 44, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (22, 53, 45, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (22, 54, 46, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (22, 55, 47, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (22, 56, 48, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (22, 57, 49, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (22, 58, 50, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (22, 3, 51, 6);
INSERT INTO attacksimulator.simulation_template_config VALUES (22, 7, 52, 6);
INSERT INTO attacksimulator.simulation_template_config VALUES (22, 8, 53, 7);
INSERT INTO attacksimulator.simulation_template_config VALUES (22, 9, 54, 7);
INSERT INTO attacksimulator.simulation_template_config VALUES (22, 10, 55, 8);
INSERT INTO attacksimulator.simulation_template_config VALUES (22, 11, 56, 8);
INSERT INTO attacksimulator.simulation_template_config VALUES (22, 12, 57, 8);
INSERT INTO attacksimulator.simulation_template_config VALUES (22, 63, 58, 9);
INSERT INTO attacksimulator.simulation_template_config VALUES (22, 13, 9, 9);
INSERT INTO attacksimulator.simulation_template_config VALUES (19, 13, 9, 9);
INSERT INTO attacksimulator.simulation_template_config VALUES (22, 4, 2, 13);
INSERT INTO attacksimulator.simulation_template_config VALUES (19, 4, 2, 13);
INSERT INTO attacksimulator.simulation_template_config VALUES (24, 64, 1, 14);
INSERT INTO attacksimulator.simulation_template_config VALUES (24, 61, 3, 10);
INSERT INTO attacksimulator.simulation_template_config VALUES (24, 59, 4, 10);
INSERT INTO attacksimulator.simulation_template_config VALUES (24, 60, 5, 10);
INSERT INTO attacksimulator.simulation_template_config VALUES (24, 62, 6, 10);
INSERT INTO attacksimulator.simulation_template_config VALUES (24, 1, 7, 11);
INSERT INTO attacksimulator.simulation_template_config VALUES (24, 2, 8, 12);
INSERT INTO attacksimulator.simulation_template_config VALUES (24, 63, 58, 9);
INSERT INTO attacksimulator.simulation_template_config VALUES (24, 15, 10, 2);
INSERT INTO attacksimulator.simulation_template_config VALUES (24, 16, 11, 2);
INSERT INTO attacksimulator.simulation_template_config VALUES (24, 17, 12, 2);
INSERT INTO attacksimulator.simulation_template_config VALUES (24, 18, 13, 2);
INSERT INTO attacksimulator.simulation_template_config VALUES (24, 19, 14, 2);
INSERT INTO attacksimulator.simulation_template_config VALUES (24, 22, 15, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (24, 23, 16, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (24, 24, 17, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (24, 25, 18, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (24, 26, 19, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (24, 27, 20, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (24, 28, 21, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (24, 29, 22, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (24, 30, 23, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (24, 32, 24, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (24, 33, 25, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (24, 34, 26, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (24, 35, 27, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (24, 36, 28, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (24, 37, 29, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (24, 38, 30, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (24, 39, 31, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (24, 40, 32, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (24, 41, 33, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (24, 42, 34, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (24, 43, 35, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (24, 44, 36, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (24, 45, 37, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (24, 46, 38, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (24, 47, 39, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (24, 48, 40, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (24, 49, 41, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (24, 50, 42, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (24, 51, 43, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (24, 52, 44, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (24, 53, 45, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (24, 54, 46, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (24, 55, 47, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (24, 56, 48, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (24, 57, 49, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (24, 58, 50, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (24, 3, 51, 6);
INSERT INTO attacksimulator.simulation_template_config VALUES (24, 7, 52, 6);
INSERT INTO attacksimulator.simulation_template_config VALUES (24, 8, 53, 7);
INSERT INTO attacksimulator.simulation_template_config VALUES (24, 9, 54, 7);
INSERT INTO attacksimulator.simulation_template_config VALUES (24, 10, 55, 8);
INSERT INTO attacksimulator.simulation_template_config VALUES (24, 11, 56, 8);
INSERT INTO attacksimulator.simulation_template_config VALUES (24, 12, 57, 8);
INSERT INTO attacksimulator.simulation_template_config VALUES (24, 13, 9, 9);
INSERT INTO attacksimulator.simulation_template_config VALUES (24, 4, 2, 13);
INSERT INTO attacksimulator.simulation_template_config VALUES (25, 64, 1, 14);
INSERT INTO attacksimulator.simulation_template_config VALUES (25, 62, 2, 10);
INSERT INTO attacksimulator.simulation_template_config VALUES (25, 65, 3, 11);
INSERT INTO attacksimulator.simulation_template_config VALUES (25, 66, 4, 11);
INSERT INTO attacksimulator.simulation_template_config VALUES (25, 67, 5, 11);
INSERT INTO attacksimulator.simulation_template_config VALUES (25, 1, 6, 11);
INSERT INTO attacksimulator.simulation_template_config VALUES (25, 2, 7, 12);
INSERT INTO attacksimulator.simulation_template_config VALUES (25, 68, 8, 12);
INSERT INTO attacksimulator.simulation_template_config VALUES (25, 69, 9, 12);
INSERT INTO attacksimulator.simulation_template_config VALUES (25, 70, 10, 12);
INSERT INTO attacksimulator.simulation_template_config VALUES (25, 4, 11, 13);
INSERT INTO attacksimulator.simulation_template_config VALUES (25, 22, 12, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (25, 23, 13, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (25, 24, 14, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (25, 25, 15, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (25, 26, 16, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (25, 27, 17, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (25, 28, 18, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (25, 29, 19, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (25, 30, 20, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (25, 32, 21, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (25, 33, 22, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (25, 34, 23, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (25, 35, 24, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (25, 36, 25, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (25, 37, 26, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (25, 38, 27, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (25, 39, 28, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (25, 40, 29, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (25, 41, 30, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (25, 42, 31, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (25, 43, 32, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (25, 44, 33, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (25, 45, 34, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (25, 46, 35, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (25, 47, 36, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (25, 48, 37, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (25, 50, 39, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (25, 51, 40, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (25, 52, 41, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (25, 53, 42, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (25, 54, 43, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (25, 55, 44, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (25, 56, 45, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (25, 57, 46, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (25, 58, 47, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (25, 8, 48, 7);
INSERT INTO attacksimulator.simulation_template_config VALUES (25, 9, 49, 7);
INSERT INTO attacksimulator.simulation_template_config VALUES (25, 71, 53, 8);
INSERT INTO attacksimulator.simulation_template_config VALUES (25, 72, 54, 8);
INSERT INTO attacksimulator.simulation_template_config VALUES (25, 73, 55, 8);
INSERT INTO attacksimulator.simulation_template_config VALUES (25, 74, 56, 8);
INSERT INTO attacksimulator.simulation_template_config VALUES (25, 77, 59, 8);
INSERT INTO attacksimulator.simulation_template_config VALUES (25, 78, 60, 8);
INSERT INTO attacksimulator.simulation_template_config VALUES (25, 83, 65, 8);
INSERT INTO attacksimulator.simulation_template_config VALUES (27, 64, 1, 14);
INSERT INTO attacksimulator.simulation_template_config VALUES (28, 64, 1, 14);
INSERT INTO attacksimulator.simulation_template_config VALUES (29, 64, 1, 14);
INSERT INTO attacksimulator.simulation_template_config VALUES (26, 64, 1, 14);
INSERT INTO attacksimulator.simulation_template_config VALUES (26, 4, 2, 13);
INSERT INTO attacksimulator.simulation_template_config VALUES (27, 4, 2, 13);
INSERT INTO attacksimulator.simulation_template_config VALUES (28, 4, 2, 13);
INSERT INTO attacksimulator.simulation_template_config VALUES (29, 4, 2, 13);
INSERT INTO attacksimulator.simulation_template_config VALUES (27, 61, 3, 10);
INSERT INTO attacksimulator.simulation_template_config VALUES (29, 61, 3, 10);
INSERT INTO attacksimulator.simulation_template_config VALUES (26, 61, 3, 10);
INSERT INTO attacksimulator.simulation_template_config VALUES (28, 61, 3, 10);
INSERT INTO attacksimulator.simulation_template_config VALUES (27, 59, 4, 10);
INSERT INTO attacksimulator.simulation_template_config VALUES (26, 59, 4, 10);
INSERT INTO attacksimulator.simulation_template_config VALUES (29, 59, 4, 10);
INSERT INTO attacksimulator.simulation_template_config VALUES (28, 59, 4, 10);
INSERT INTO attacksimulator.simulation_template_config VALUES (29, 60, 5, 10);
INSERT INTO attacksimulator.simulation_template_config VALUES (28, 60, 5, 10);
INSERT INTO attacksimulator.simulation_template_config VALUES (26, 60, 5, 10);
INSERT INTO attacksimulator.simulation_template_config VALUES (27, 60, 5, 10);
INSERT INTO attacksimulator.simulation_template_config VALUES (27, 62, 6, 10);
INSERT INTO attacksimulator.simulation_template_config VALUES (26, 62, 6, 10);
INSERT INTO attacksimulator.simulation_template_config VALUES (29, 62, 6, 10);
INSERT INTO attacksimulator.simulation_template_config VALUES (28, 62, 6, 10);
INSERT INTO attacksimulator.simulation_template_config VALUES (29, 1, 7, 11);
INSERT INTO attacksimulator.simulation_template_config VALUES (28, 1, 7, 11);
INSERT INTO attacksimulator.simulation_template_config VALUES (27, 1, 7, 11);
INSERT INTO attacksimulator.simulation_template_config VALUES (26, 1, 7, 11);
INSERT INTO attacksimulator.simulation_template_config VALUES (28, 2, 8, 12);
INSERT INTO attacksimulator.simulation_template_config VALUES (26, 2, 8, 12);
INSERT INTO attacksimulator.simulation_template_config VALUES (27, 2, 8, 12);
INSERT INTO attacksimulator.simulation_template_config VALUES (29, 2, 8, 12);
INSERT INTO attacksimulator.simulation_template_config VALUES (28, 13, 9, 9);
INSERT INTO attacksimulator.simulation_template_config VALUES (27, 13, 9, 9);
INSERT INTO attacksimulator.simulation_template_config VALUES (29, 13, 9, 9);
INSERT INTO attacksimulator.simulation_template_config VALUES (26, 13, 9, 9);
INSERT INTO attacksimulator.simulation_template_config VALUES (28, 15, 10, 2);
INSERT INTO attacksimulator.simulation_template_config VALUES (26, 15, 10, 2);
INSERT INTO attacksimulator.simulation_template_config VALUES (29, 15, 10, 2);
INSERT INTO attacksimulator.simulation_template_config VALUES (27, 15, 10, 2);
INSERT INTO attacksimulator.simulation_template_config VALUES (28, 16, 11, 2);
INSERT INTO attacksimulator.simulation_template_config VALUES (26, 16, 11, 2);
INSERT INTO attacksimulator.simulation_template_config VALUES (29, 16, 11, 2);
INSERT INTO attacksimulator.simulation_template_config VALUES (27, 16, 11, 2);
INSERT INTO attacksimulator.simulation_template_config VALUES (29, 17, 12, 2);
INSERT INTO attacksimulator.simulation_template_config VALUES (28, 17, 12, 2);
INSERT INTO attacksimulator.simulation_template_config VALUES (26, 17, 12, 2);
INSERT INTO attacksimulator.simulation_template_config VALUES (27, 17, 12, 2);
INSERT INTO attacksimulator.simulation_template_config VALUES (28, 18, 13, 2);
INSERT INTO attacksimulator.simulation_template_config VALUES (27, 18, 13, 2);
INSERT INTO attacksimulator.simulation_template_config VALUES (26, 18, 13, 2);
INSERT INTO attacksimulator.simulation_template_config VALUES (29, 18, 13, 2);
INSERT INTO attacksimulator.simulation_template_config VALUES (29, 19, 14, 2);
INSERT INTO attacksimulator.simulation_template_config VALUES (28, 19, 14, 2);
INSERT INTO attacksimulator.simulation_template_config VALUES (26, 19, 14, 2);
INSERT INTO attacksimulator.simulation_template_config VALUES (27, 19, 14, 2);
INSERT INTO attacksimulator.simulation_template_config VALUES (27, 22, 15, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (29, 22, 15, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (28, 22, 15, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (26, 22, 15, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (29, 23, 16, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (26, 23, 16, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (28, 23, 16, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (27, 23, 16, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (26, 24, 17, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (27, 24, 17, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (29, 24, 17, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (28, 24, 17, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (28, 25, 18, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (29, 25, 18, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (27, 25, 18, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (26, 25, 18, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (26, 26, 19, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (29, 26, 19, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (28, 26, 19, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (27, 26, 19, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (27, 27, 20, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (26, 27, 20, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (28, 27, 20, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (29, 27, 20, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (27, 28, 21, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (28, 28, 21, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (29, 28, 21, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (26, 28, 21, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (26, 29, 22, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (29, 29, 22, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (28, 29, 22, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (27, 29, 22, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (26, 30, 23, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (29, 30, 23, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (27, 30, 23, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (28, 30, 23, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (29, 32, 24, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (27, 32, 24, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (26, 32, 24, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (28, 32, 24, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (26, 33, 25, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (28, 33, 25, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (27, 33, 25, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (29, 33, 25, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (26, 34, 26, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (27, 34, 26, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (28, 34, 26, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (29, 34, 26, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (26, 35, 27, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (29, 35, 27, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (28, 35, 27, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (27, 35, 27, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (27, 36, 28, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (26, 36, 28, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (29, 36, 28, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (28, 36, 28, 3);
INSERT INTO attacksimulator.simulation_template_config VALUES (26, 37, 29, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (29, 37, 29, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (27, 37, 29, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (28, 37, 29, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (29, 38, 30, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (27, 38, 30, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (26, 38, 30, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (28, 38, 30, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (27, 39, 31, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (26, 39, 31, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (28, 39, 31, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (29, 39, 31, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (29, 40, 32, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (28, 40, 32, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (26, 40, 32, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (27, 40, 32, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (29, 41, 33, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (26, 41, 33, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (28, 41, 33, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (27, 41, 33, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (27, 42, 34, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (28, 42, 34, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (29, 42, 34, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (26, 42, 34, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (29, 43, 35, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (27, 43, 35, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (28, 43, 35, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (26, 43, 35, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (26, 44, 36, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (29, 44, 36, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (27, 44, 36, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (28, 44, 36, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (28, 45, 37, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (29, 45, 37, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (26, 45, 37, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (27, 45, 37, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (29, 46, 38, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (27, 46, 38, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (28, 46, 38, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (26, 46, 38, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (27, 47, 39, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (28, 47, 39, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (26, 47, 39, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (29, 47, 39, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (29, 48, 40, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (27, 48, 40, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (26, 48, 40, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (28, 48, 40, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (26, 49, 41, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (28, 49, 41, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (27, 49, 41, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (29, 49, 41, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (28, 50, 42, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (26, 50, 42, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (29, 50, 42, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (27, 50, 42, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (28, 51, 43, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (27, 51, 43, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (26, 51, 43, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (29, 51, 43, 4);
INSERT INTO attacksimulator.simulation_template_config VALUES (27, 52, 44, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (28, 52, 44, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (26, 52, 44, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (29, 52, 44, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (29, 53, 45, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (26, 53, 45, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (28, 53, 45, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (27, 53, 45, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (26, 54, 46, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (27, 54, 46, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (28, 54, 46, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (29, 54, 46, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (26, 55, 47, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (29, 55, 47, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (28, 55, 47, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (27, 55, 47, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (28, 56, 48, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (27, 56, 48, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (29, 56, 48, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (26, 56, 48, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (28, 57, 49, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (26, 57, 49, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (27, 57, 49, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (29, 57, 49, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (26, 58, 50, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (29, 58, 50, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (27, 58, 50, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (28, 58, 50, 5);
INSERT INTO attacksimulator.simulation_template_config VALUES (29, 3, 51, 6);
INSERT INTO attacksimulator.simulation_template_config VALUES (26, 3, 51, 6);
INSERT INTO attacksimulator.simulation_template_config VALUES (28, 3, 51, 6);
INSERT INTO attacksimulator.simulation_template_config VALUES (27, 3, 51, 6);
INSERT INTO attacksimulator.simulation_template_config VALUES (29, 7, 52, 6);
INSERT INTO attacksimulator.simulation_template_config VALUES (28, 7, 52, 6);
INSERT INTO attacksimulator.simulation_template_config VALUES (26, 7, 52, 6);
INSERT INTO attacksimulator.simulation_template_config VALUES (27, 7, 52, 6);
INSERT INTO attacksimulator.simulation_template_config VALUES (26, 8, 53, 7);
INSERT INTO attacksimulator.simulation_template_config VALUES (27, 8, 53, 7);
INSERT INTO attacksimulator.simulation_template_config VALUES (28, 8, 53, 7);
INSERT INTO attacksimulator.simulation_template_config VALUES (29, 8, 53, 7);
INSERT INTO attacksimulator.simulation_template_config VALUES (28, 9, 54, 7);
INSERT INTO attacksimulator.simulation_template_config VALUES (27, 9, 54, 7);
INSERT INTO attacksimulator.simulation_template_config VALUES (26, 9, 54, 7);
INSERT INTO attacksimulator.simulation_template_config VALUES (29, 9, 54, 7);
INSERT INTO attacksimulator.simulation_template_config VALUES (27, 71, 55, 8);
INSERT INTO attacksimulator.simulation_template_config VALUES (26, 71, 55, 8);
INSERT INTO attacksimulator.simulation_template_config VALUES (28, 71, 55, 8);
INSERT INTO attacksimulator.simulation_template_config VALUES (29, 71, 55, 8);
INSERT INTO attacksimulator.simulation_template_config VALUES (29, 72, 56, 8);
INSERT INTO attacksimulator.simulation_template_config VALUES (26, 72, 56, 8);
INSERT INTO attacksimulator.simulation_template_config VALUES (28, 72, 56, 8);
INSERT INTO attacksimulator.simulation_template_config VALUES (27, 72, 56, 8);
INSERT INTO attacksimulator.simulation_template_config VALUES (26, 73, 57, 8);
INSERT INTO attacksimulator.simulation_template_config VALUES (28, 73, 57, 8);
INSERT INTO attacksimulator.simulation_template_config VALUES (29, 73, 57, 8);
INSERT INTO attacksimulator.simulation_template_config VALUES (27, 73, 57, 8);
INSERT INTO attacksimulator.simulation_template_config VALUES (26, 74, 58, 8);
INSERT INTO attacksimulator.simulation_template_config VALUES (28, 74, 58, 8);
INSERT INTO attacksimulator.simulation_template_config VALUES (29, 74, 58, 8);
INSERT INTO attacksimulator.simulation_template_config VALUES (27, 74, 58, 8);
INSERT INTO attacksimulator.simulation_template_config VALUES (29, 77, 59, 8);
INSERT INTO attacksimulator.simulation_template_config VALUES (26, 77, 59, 8);
INSERT INTO attacksimulator.simulation_template_config VALUES (28, 77, 59, 8);
INSERT INTO attacksimulator.simulation_template_config VALUES (27, 77, 59, 8);
INSERT INTO attacksimulator.simulation_template_config VALUES (29, 78, 60, 8);
INSERT INTO attacksimulator.simulation_template_config VALUES (28, 78, 60, 8);
INSERT INTO attacksimulator.simulation_template_config VALUES (27, 78, 60, 8);
INSERT INTO attacksimulator.simulation_template_config VALUES (26, 78, 60, 8);
INSERT INTO attacksimulator.simulation_template_config VALUES (27, 83, 61, 8);
INSERT INTO attacksimulator.simulation_template_config VALUES (28, 83, 61, 8);
INSERT INTO attacksimulator.simulation_template_config VALUES (26, 83, 61, 8);
INSERT INTO attacksimulator.simulation_template_config VALUES (29, 83, 61, 8);
INSERT INTO attacksimulator.simulation_template_config VALUES (26, 63, 62, 9);
INSERT INTO attacksimulator.simulation_template_config VALUES (29, 63, 62, 9);
INSERT INTO attacksimulator.simulation_template_config VALUES (28, 63, 62, 9);
INSERT INTO attacksimulator.simulation_template_config VALUES (27, 63, 62, 9);


--
-- TOC entry 3253 (class 0 OID 399033)
-- Dependencies: 213
-- Data for Name: task; Type: TABLE DATA; Schema: attacksimulator; Owner: towerstreet
--

INSERT INTO attacksimulator.task VALUES (1, 1, 'cis-7.4-streaming-content', '{ "url": "https://www.youtube.com" }');
INSERT INTO attacksimulator.task VALUES (2, 1, 'cis-7.4-gaming-content', '{ "url": "https://www.bet365.com" }');
INSERT INTO attacksimulator.task VALUES (3, 2, 'cis-7.7-first-occurance', '{  "first": "iuqerfsodp9ifjaposd",  "second": "fjhgosurijfaewrwergwea" }');
INSERT INTO attacksimulator.task VALUES (4, 1, 'cis-7.4-cnn.com', '{ "url": "https://edition.cnn.com/" }');
INSERT INTO attacksimulator.task VALUES (5, 1, 'cis-7.4-asdgkjaiu984nldg83lsd.cx', '{ "url": "https://www.asdgkjaiu984nldg83lsd.cx/" }');
INSERT INTO attacksimulator.task VALUES (6, 1, 'cis-7.4-kjg9834kagfsdixs.com', '{ "url": "https://www.kjg9834kagfsdixs.com/" }');
INSERT INTO attacksimulator.task VALUES (7, 2, 'cis-7.7-second-occurance', '{ "first": "ifferfsodp9ifjaposd", "second": "fjhgosurijfaewrwergwea" }');
INSERT INTO attacksimulator.task VALUES (14, 6, 'cis-14.2-arbitrary-scan', '{"subnets": []}');
INSERT INTO attacksimulator.task VALUES (8, 3, 'cis-7.9-eicar-com', '{ "protocol": "http", "filename": "eicar-com" }');
INSERT INTO attacksimulator.task VALUES (9, 3, 'cis-7.9-eicar-zip', '{ "protocol": "http", "filename": "eicar-com-zip" }');
INSERT INTO attacksimulator.task VALUES (10, 4, 'cis-13.3-csv', '{ "protocol": "http", "filename": "cc-records", "uploadName": "exfiltrate-cc-client.csv", "testFileKey":"cis-13.3-csv-empty", "records": 0, "recordType": "pci" }');
INSERT INTO attacksimulator.task VALUES (71, 4, 'cis-13.3-csv-empty', '{ "protocol": "http", "filename": "cc-records-empty", "uploadName": "exfiltrate-cc-client-empty.csv", "isTestFile": true }');
INSERT INTO attacksimulator.task VALUES (12, 4, 'cis-13.3-docx', '{ "protocol": "http", "filename": "ssn-records-word", "uploadName": "exfiltrate-ssn.docx", "testFileKey":"cis-13.3-docx-empty", "records": 0, "recordType": "ssn" }');
INSERT INTO attacksimulator.task VALUES (22, 1, 'cis-7.4-social-facebook.com', '{ "url": "https://facebook.com/" }');
INSERT INTO attacksimulator.task VALUES (23, 1, 'cis-7.4-social-twitter.com', '{ "url": "https://twitter.com/" }');
INSERT INTO attacksimulator.task VALUES (24, 1, 'cis-7.4-social-youtube.com', '{ "url": "https://youtube.com/" }');
INSERT INTO attacksimulator.task VALUES (25, 1, 'cis-7.4-social-instagram.com', '{ "url": "http://www.instagram.com/" }');
INSERT INTO attacksimulator.task VALUES (26, 1, 'cis-7.4-social-linkedin.com', '{ "url": "http://www.linkedin.com/" }');
INSERT INTO attacksimulator.task VALUES (27, 1, 'cis-7.4-social-reddit.com', '{ "url": "http://www.reddit.com/" }');
INSERT INTO attacksimulator.task VALUES (29, 1, 'cis-7.4-social-tumblr.com', '{ "url": "http://www.tumblr.com/" }');
INSERT INTO attacksimulator.task VALUES (30, 1, 'cis-7.4-social-pinterest.com', '{ "url": "http://www.pinterest.com/" }');
INSERT INTO attacksimulator.task VALUES (31, 1, 'cis-7.4-social-plus.google.com', '{ "url": "http://www.plus.google.com" }');
INSERT INTO attacksimulator.task VALUES (32, 1, 'cis-7.4-social-flickr.com', '{ "url": "http://www.flickr.com/" }');
INSERT INTO attacksimulator.task VALUES (34, 1, 'cis-7.4-social-ask.fm', '{ "url": "http://www.ask.fm/" }');
INSERT INTO attacksimulator.task VALUES (35, 1, 'cis-7.4-social-livejournal.com', '{ "url": "http://www.livejournal.com/" }');
INSERT INTO attacksimulator.task VALUES (37, 1, 'cis-7.4-url-Bit.ly', '{ "url": "http://Bit.ly/" }');
INSERT INTO attacksimulator.task VALUES (38, 1, 'cis-7.4-url-Goo.gl', '{ "url": "http://Goo.gl/" }');
INSERT INTO attacksimulator.task VALUES (39, 1, 'cis-7.4-url-TinyURL.com', '{ "url": "http://TinyURL.com/" }');
INSERT INTO attacksimulator.task VALUES (40, 1, 'cis-7.4-url-Ow.ly', '{ "url": "http://Ow.ly/" }');
INSERT INTO attacksimulator.task VALUES (41, 1, 'cis-7.4-url-Rebrand.ly', '{ "url": "http://Rebrand.ly/" }');
INSERT INTO attacksimulator.task VALUES (42, 1, 'cis-7.4-url-Is.gd', '{ "url": "http://Is.gd/" }');
INSERT INTO attacksimulator.task VALUES (43, 1, 'cis-7.4-url-Buff.ly', '{ "url": "http://Buff.ly/" }');
INSERT INTO attacksimulator.task VALUES (44, 1, 'cis-7.4-url-Shorte.st', '{ "url": "http://Shorte.st/" }');
INSERT INTO attacksimulator.task VALUES (45, 1, 'cis-7.4-url-Adf.ly', '{ "url": "http://Adf.ly/" }');
INSERT INTO attacksimulator.task VALUES (47, 1, 'cis-7.4-url-Soo.gd', '{ "url": "http://Soo.gd/" }');
INSERT INTO attacksimulator.task VALUES (48, 1, 'cis-7.4-url-Tiny.cc', '{ "url": "http://Tiny.cc/" }');
INSERT INTO attacksimulator.task VALUES (49, 1, 'cis-7.4-url-Bit.do', '{ "url": "http://Bit.do/" }');
INSERT INTO attacksimulator.task VALUES (51, 1, 'cis-7.4-url-is.gd', '{ "url": "http://is.gd/" }');
INSERT INTO attacksimulator.task VALUES (52, 1, 'cis-7.4-cloud-pastebin.com', '{ "url": "http://pastebin.com/" }');
INSERT INTO attacksimulator.task VALUES (54, 1, 'cis-7.4-cloud-drive.google.com', '{ "url": "http://drive.google.com/" }');
INSERT INTO attacksimulator.task VALUES (55, 1, 'cis-7.4-cloud-box.com', '{ "url": "http://box.com/" }');
INSERT INTO attacksimulator.task VALUES (56, 1, 'cis-7.4-cloud-mega.co.nz', '{ "url": "http://mega.co.nz/" }');
INSERT INTO attacksimulator.task VALUES (57, 1, 'cis-7.4-cloud-www.adrive.com', '{ "url": "http://www.adrive.com/" }');
INSERT INTO attacksimulator.task VALUES (58, 1, 'cis-7.4-cloud-onedrive.live.com', '{ "url": "http://onedrive.live.com/" }');
INSERT INTO attacksimulator.task VALUES (33, 1, 'cis-7.4-social-meetup.com', '{"url": "http://www.meetup.com", "imagePath": "mu_static/en-US/logo--script.004ada05.svg"}');
INSERT INTO attacksimulator.task VALUES (36, 1, 'cis-7.4-social-myspace.com', '{"url": "http://x.myspacecdn.com", "imagePath": "new/common/images/ProgressIndicator-White.gif"}');
INSERT INTO attacksimulator.task VALUES (46, 1, 'cis-7.4-url-T2m.io', '{"url": "http://t2mio.com", "imagePath": "assets/images/zesle_logo.png"}');
INSERT INTO attacksimulator.task VALUES (50, 1, 'cis-7.4-url-Link.TL', '{"url": "http://Link.TL/", "imagePath": "storage/images/logo/site.png"}');
INSERT INTO attacksimulator.task VALUES (53, 1, 'cis-7.4-cloud-dropbox.com', '{"url": "https://cfl.dropboxstatic.com", "imagePath": "static/images/logo_catalog/glyph_m1-vflCrXgzt.svg"}');
INSERT INTO attacksimulator.task VALUES (59, 1, 'cis-7.4-maxtimeout', '{"url": "", "imagePath": "measure/maxtimeout", "includeInUrlTesting": false}');
INSERT INTO attacksimulator.task VALUES (60, 1, 'cis-7.4-resettimeout', '{"url": "", "imagePath": "measure/resettimeout", "includeInUrlTesting": false}');
INSERT INTO attacksimulator.task VALUES (61, 1, 'cis-7.4-baseline', '{"url": "", "includeInUrlTesting": false}');
INSERT INTO attacksimulator.task VALUES (62, 1, 'cis-7.4-uncategorized', '{"url": "http://54.175.228.126" }');
INSERT INTO attacksimulator.task VALUES (15, 1, 'cis-7.4-tor-46.233.0.70', '{"url": "http://46.233.0.70/", "hasImage": false, "hasWebServer": false}');
INSERT INTO attacksimulator.task VALUES (13, 5, 'cis-14.1-scan', '{ "portScanTimeout": 3000, "ports": [80, 443, 445, 65534] , "portScanDelay": 55000}');
INSERT INTO attacksimulator.task VALUES (63, 5, 'cis-14.1-scan-second', '{ "portScanTimeout": 3000, "ports": [80, 443, 445, 65534] , "portScanDelay": 55000}');
INSERT INTO attacksimulator.task VALUES (16, 1, 'cis-7.4-tor-204.17.56.42', '{"url": "http://204.17.56.42/", "hasImage": false, "hasWebServer": true}');
INSERT INTO attacksimulator.task VALUES (17, 1, 'cis-7.4-tor-93.157.1.22', '{"url": "http://93.157.1.22/", "hasImage": false, "hasWebServer": true}');
INSERT INTO attacksimulator.task VALUES (18, 1, 'cis-7.4-tor-103.234.220.195', '{"url": "http://103.234.220.195/", "hasImage": false, "hasWebServer": true}');
INSERT INTO attacksimulator.task VALUES (19, 1, 'cis-7.4-tor-103.236.201.27', '{"url": "http://103.236.201.27/", "hasImage": false, "hasWebServer": true}');
INSERT INTO attacksimulator.task VALUES (64, 7, 'browser', NULL);
INSERT INTO attacksimulator.task VALUES (28, 1, 'cis-7.4-social-vk.com', '{ "url": "https://vk.com/", "timeout":60000 }');
INSERT INTO attacksimulator.task VALUES (65, 1, 'cis-7.4-streaming-content-spotif', '{ "url": "http://www.spotify.com" }');
INSERT INTO attacksimulator.task VALUES (66, 1, 'cis-7.4-streaming-content-hulu', '{ "url": "http://www.hulu.com" }');
INSERT INTO attacksimulator.task VALUES (67, 1, 'cis-7.4-streaming-content-netfli', '{ "url": "http://www.netflix.com" }');
INSERT INTO attacksimulator.task VALUES (68, 1, 'cis-7.4-gaming-content-fanduel', '{ "url": "http://www.fanduel.com" }');
INSERT INTO attacksimulator.task VALUES (69, 1, 'cis-7.4-gaming-content-fulltilt', '{ "url": "http://www.fulltilt.com" }');
INSERT INTO attacksimulator.task VALUES (70, 1, 'cis-7.4-gaming-content-wsop', '{ "url": "http://www.wsop.com" }');
INSERT INTO attacksimulator.task VALUES (74, 4, 'cis-13.3-csv-10K', '{ "protocol": "http", "filename": "cc-records-10K", "uploadName": "exfiltrate-cc-client-10K.csv", "testFileKey":"cis-13.3-csv-empty", "records": 10000, "recordType": "pci" }');
INSERT INTO attacksimulator.task VALUES (83, 4, 'cis-13.3-csv-100K', '{ "protocol": "http", "filename": "cc-records-100K", "uploadName": "exfiltrate-cc-client-100K.csv", "testFileKey":"cis-13.3-csv-empty", "records": 100000, "recordType": "pci" }');
INSERT INTO attacksimulator.task VALUES (78, 4, 'cis-13.3-docx-100K', '{ "protocol": "http", "filename": "ssn-records-100K-word", "uploadName": "exfiltrate-ssn-100K.docx", "testFileKey":"cis-13.3-docx-empty", "records": 100000, "recordType": "ssn" }');
INSERT INTO attacksimulator.task VALUES (76, 4, 'cis-13.3-docx-10K', '{ "protocol": "http", "filename": "ssn-records-10K-word", "uploadName": "exfiltrate-ssn-10K.docx", "testFileKey":"cis-13.3-docx-empty", "records": 10000, "recordType": "ssn" }');
INSERT INTO attacksimulator.task VALUES (80, 4, 'cis-13.3-docx-1M', '{ "protocol": "http", "filename": "ssn-records-1M-word", "uploadName": "exfiltrate-ssn-1M.docx", "testFileKey":"cis-13.3-docx-empty", "records": 1000000, "recordType": "ssn" }');
INSERT INTO attacksimulator.task VALUES (82, 4, 'cis-13.3-docx-5M', '{ "protocol": "http", "filename": "ssn-records-5M-word", "uploadName": "exfiltrate-ssn-5M.docx", "testFileKey":"cis-13.3-docx-empty", "records": 5000000, "recordType": "ssn" }');
INSERT INTO attacksimulator.task VALUES (73, 4, 'cis-13.3-docx-empty', '{ "protocol": "http", "filename": "ssn-records-empty-word", "uploadName": "exfiltrate-ssn-empty.docx", "isTestFile": true }');
INSERT INTO attacksimulator.task VALUES (11, 4, 'cis-13.3-xlsx', '{ "protocol": "http", "filename": "ssn-records-excel", "uploadName": "exfiltrate-ssn.xlsx", "testFileKey":"cis-13.3-xlsx-empty", "records": 0, "recordType": "ssn" }');
INSERT INTO attacksimulator.task VALUES (77, 4, 'cis-13.3-xlsx-100K', '{ "protocol": "http", "filename": "ssn-records-100K-excel", "uploadName": "exfiltrate-ssn-100K.xlsx", "testFileKey":"cis-13.3-xlsx-empty", "records": 100000, "recordType": "ssn" }');
INSERT INTO attacksimulator.task VALUES (75, 4, 'cis-13.3-xlsx-10K', '{ "protocol": "http", "filename": "ssn-records-10K-excel", "uploadName": "exfiltrate-ssn-10K.xlsx", "testFileKey":"cis-13.3-xlsx-empty", "records": 10000, "recordType": "ssn" }');
INSERT INTO attacksimulator.task VALUES (79, 4, 'cis-13.3-xlsx-1M', '{ "protocol": "http", "filename": "ssn-records-1M-excel", "uploadName": "exfiltrate-ssn-1M.xlsx", "testFileKey":"cis-13.3-xlsx-empty", "records": 1000000, "recordType": "ssn" }');
INSERT INTO attacksimulator.task VALUES (81, 4, 'cis-13.3-xlsx-5M', '{ "protocol": "http", "filename": "ssn-records-5M-excel", "uploadName": "exfiltrate-ssn-5M.xlsx", "testFileKey":"cis-13.3-xlsx-empty", "records": 5000000, "recordType": "ssn" }');
INSERT INTO attacksimulator.task VALUES (72, 4, 'cis-13.3-xlsx-empty', '{ "protocol": "http", "filename": "ssn-records-empty-excel", "uploadName": "exfiltrate-ssn-empty.xlsx", "isTestFile": true }');


--
-- TOC entry 3269 (class 0 OID 400215)
-- Dependencies: 229
-- Data for Name: test_case; Type: TABLE DATA; Schema: attacksimulator; Owner: towerstreet
--

INSERT INTO attacksimulator.test_case VALUES (5, 'cis-7.4-cloud', 'Accessing cloud storage providers', 'The purpose of this simulation is to validate if the network filtering policy is in place and that it limits the system''s ability to connect to cloud storage providers not approved by the organization.

This is achieved by attempting to fetch a non malicious artifact via HTTP GET requests.

All domains and sites are periodically tested in our testing environment to ensure safety and validity.');
INSERT INTO attacksimulator.test_case VALUES (6, 'cis-7.7-dns-filter', 'Testing DNS filtering', 'The purpose of this simulation is to validate if DNS filtering services are in place to help block access to known malicious domains. 

Our simulation attempts to resolve and connect to well known domains related to WannaCry ransomware by initiating HTTP GET request from the web browser. The browser will initialize DNS resolution to translate the domain name to IP address before initiating a HTTP GET request. The domains should not resolve or the DNS server should redirect users to a safe landing domain.');
INSERT INTO attacksimulator.test_case VALUES (7, 'cis-7.9-eicar', 'Testing malware protection', 'The purpose of this simulation is to verify the function of an endpoint security solution.

Our simulation attempts to download an Eicar file (a benign antivirus test file published at eicar.com) in form of a raw but also compressed executable file. Endpoint protection products, next generation firewalls or filtering application proxy servers should detect and block the download of this file. The file is downloaded only into memory, as it is not possible to store it on the disk without manual user interaction.');
INSERT INTO attacksimulator.test_case VALUES (1, 'cis-7.4-general', 'Accessing web pages of multiple categories', '');
INSERT INTO attacksimulator.test_case VALUES (10, 'cis-7.4-baseline', 'Measuring connection baseline', 'The purpose of this simulation is to determinate a baseline of connection parameters like latency and request timeout to support evaluation of other tests.

This is achieved by attempting to fetch an object via HTTP GET requests from our attack-simulator web site.');
INSERT INTO attacksimulator.test_case VALUES (11, 'cis-7.4-streaming', 'Accessing streaming sites', 'The purpose of this simulation is to validate if the network filtering policy is in place and that it limits the system''s ability to connect to websites not approved by the organization.

This is achieved by attempting to fetch a non malicious artifact via HTTP GET requests from internet sites of various content categories ranging from social media, news, entertainment, streaming sites, to known phishing and malware domains. URLs located on malware or phishing domains only point to non-malicious and non-executable content, such as images.

All domains and sites are periodically tested in our testing environment to ensure safety and validity.');
INSERT INTO attacksimulator.test_case VALUES (12, 'cis-7.4-gaming', 'Accessing gaming sites', 'The purpose of this simulation is to validate if the network filtering policy is in place and that it limits the system''s ability to connect to websites not approved by the organization.

This is achieved by attempting to fetch a non malicious artifact via HTTP GET requests from internet sites of various content categories ranging from social media, news, entertainment, streaming sites, to known phishing and malware domains. URLs located on malware or phishing domains only point to non-malicious and non-executable content, such as images.

All domains and sites are periodically tested in our testing environment to ensure safety and validity.');
INSERT INTO attacksimulator.test_case VALUES (13, 'cis-7.4-news', 'Accessing news sites', 'The purpose of this simulation is to validate if the network filtering policy is in place and that it limits the system''s ability to connect to websites not approved by the organization.

This is achieved by attempting to fetch a non malicious artifact via HTTP GET requests from internet sites of various content categories ranging from social media, news, entertainment, streaming sites, to known phishing and malware domains. URLs located on malware or phishing domains only point to non-malicious and non-executable content, such as images.

All domains and sites are periodically tested in our testing environment to ensure safety and validity. ');
INSERT INTO attacksimulator.test_case VALUES (2, 'cis-7.4-tor', 'Accessing Tor IP addresses', 'The purpose of this simulation is to validate if the network filtering policy is in place and that it limits the system''s ability to connect to websites not approved by the organization.

This is achieved by attempting to fetch a non malicious artifact via HTTP GET requests from internet sites of various content categories ranging from social media, news, entertainment, streaming sites, to known phishing and malware domains. URLs located on malware or phishing domains only point to non-malicious and non-executable content, such as images.

All domains and sites are periodically tested in our testing environment to ensure safety and validity.');
INSERT INTO attacksimulator.test_case VALUES (3, 'cis-7.4-social', 'Accessing social network sites', 'The purpose of this simulation is to validate if the network filtering policy is in place and that it limits the system''s ability to connect to websites not approved by the organization.

This is achieved by attempting to fetch a non malicious artifact via HTTP GET requests from internet sites of various content categories ranging from social media, news, entertainment, streaming sites, to known phishing and malware domains. URLs located on malware or phishing domains only point to non-malicious and non-executable content, such as images.

All domains and sites are periodically tested in our testing environment to ensure safety and validity.');
INSERT INTO attacksimulator.test_case VALUES (4, 'cis-7.4-url', 'Accessing URL shorteners', 'The purpose of this simulation is to validate if the network filtering policy is in place and that it limits the system''s ability to connect to url shortener services (e.g. bit.ly) not approved by the organization.

This is achieved by attempting to fetch a non malicious artifact via HTTP GET requests.

All domains and sites are periodically tested in our testing environment to ensure safety and validity.');
INSERT INTO attacksimulator.test_case VALUES (8, 'cis-13.3-exfiltration', 'Testing data loss prevention', 'Purpose of this simulation is to check if data loss prevention system (DLP) is in place, via simulating an exfiltration transfer of artificially generated PII data in various formats (csv, xlsx, docx) from the browser via HTTP POST method to our testing server. This automatically simulates the same actions, when an user uploads file containing PII to the internet.');
INSERT INTO attacksimulator.test_case VALUES (9, 'cis-14.1-port-scan', 'Simulating lateral movement on local network', 'Our simulation determines the local IP address of the endpoint on which it is run and tries to connect via HTTP GET method to ports (80/HTTP, 443/HTTPS, 445/SMB, 5900/VNC, 4000/nomachine) within the local LAN segment. This helps to determine if network is segmented based on sensitivity which increases the difficulty for attacker to attack services within the network. It also shows if private VLANS are used and the blocking of workstation to workstation communications is in place. The former significantly reduces the possibilities of wide malware spread within the network.');
INSERT INTO attacksimulator.test_case VALUES (14, 'browser', 'Detecting browser data', 'The purpose of this simulation is to determinate basic information about browser and user machine.

This is achieved by accessing public information provided by internet browser.');


--
-- TOC entry 3260 (class 0 OID 399977)
-- Dependencies: 220
-- Data for Name: url_test; Type: TABLE DATA; Schema: attacksimulator; Owner: towerstreet
--



--
-- TOC entry 3255 (class 0 OID 399042)
-- Dependencies: 215
-- Data for Name: user; Type: TABLE DATA; Schema: attacksimulator; Owner: towerstreet
--

INSERT INTO attacksimulator."user" VALUES (1, 1, 'road_runner', NULL);
INSERT INTO attacksimulator."user" VALUES (2, 2, 'campaign', 1);


--
-- TOC entry 3262 (class 0 OID 400095)
-- Dependencies: 222
-- Data for Name: assessment_type; Type: TABLE DATA; Schema: public; Owner: towerstreet
--

INSERT INTO public.assessment_type VALUES (1, 'light', 'Light Assessment Level');
INSERT INTO public.assessment_type VALUES (2, 'medium', 'Medium Assessment Level');
INSERT INTO public.assessment_type VALUES (3, 'continuous', 'Continuous Assessment Level');
INSERT INTO public.assessment_type VALUES (4, 'test-examples', 'Virtual assessment type for test scenarios');
INSERT INTO public.assessment_type VALUES (5, 'ultra-light', 'Ultra Light Assessment Level');
INSERT INTO public.assessment_type VALUES (6, 'simulator-2019-03', 'Simulator 2019 03 RSA campaign');


--
-- TOC entry 3278 (class 0 OID 400835)
-- Dependencies: 238
-- Data for Name: campaign_visitor; Type: TABLE DATA; Schema: public; Owner: towerstreet
--

INSERT INTO public.campaign_visitor VALUES (1, 2, '2019-05-02 09:31:21.941588', NULL, NULL, NULL);


--
-- TOC entry 3258 (class 0 OID 399628)
-- Dependencies: 218
-- Data for Name: customer; Type: TABLE DATA; Schema: public; Owner: towerstreet
--

INSERT INTO public.customer VALUES (1, 'ACME Inc.', 'acme', false, true);
INSERT INTO public.customer VALUES (2, 'simulator-2019-03', 'simulator-2019-03', true, true);


--
-- TOC entry 3264 (class 0 OID 400109)
-- Dependencies: 224
-- Data for Name: customer_assessment; Type: TABLE DATA; Schema: public; Owner: towerstreet
--

INSERT INTO public.customer_assessment VALUES (1, 1, 1, '2019-05-02 08:46:51.724278', NULL);
INSERT INTO public.customer_assessment VALUES (2, 6, 2, '2019-05-02 08:48:29.234613', NULL);


--
-- TOC entry 3279 (class 0 OID 400885)
-- Dependencies: 239
-- Data for Name: campaign_score_histogram; Type: TABLE DATA; Schema: scoring; Owner: towerstreet
--



--
-- TOC entry 3271 (class 0 OID 400345)
-- Dependencies: 231
-- Data for Name: scoring_definition; Type: TABLE DATA; Schema: scoring; Owner: towerstreet
--

INSERT INTO scoring.scoring_definition VALUES (8, 'cis-7.7', 'boolean-result', '7.7 Use of DNS Filtering Services', '{"successWhen": false}', '', 3, 'cis');
INSERT INTO scoring.scoring_definition VALUES (6, 'cis-9.4', 'firewall-port-scan-result', '9.4 Apply Host-based Firewalls or Port Filtering', '{"requiredRuns": 2}', '', 5, 'cis');
INSERT INTO scoring.scoring_definition VALUES (10, 'cis-12.3', 'url-test-result', '12.3 Deny Communications with Known Malicious IP Addresses', '{"successWhen": false, "requiredHits": 3}', '', 6, 'cis');
INSERT INTO scoring.scoring_definition VALUES (4, 'cis-14.1', 'open-port-scan-result', '14.1 Segment the Network Based on Sensitivity', '{"ports": ["80", "443"]}', 'We are using test for detecting local network range by detecting broadcast address. Then we run a lateral port scan on ports 445/SMB, 5900/VNC, 4000/nomachine. We are unable to scan all ports due to limitations of browsers.', 9, 'cis');
INSERT INTO scoring.scoring_definition VALUES (5, 'cis-14.3', 'open-port-scan-result', '14.3 Disable Workstation to Workstation Communication', NULL, '', 10, 'cis');
INSERT INTO scoring.scoring_definition VALUES (11, 'score-stream-url', 'url-test-result', 'Streaming url test', '{"successWhen": false, "requiredHits": 1}', '', 1, 'score');
INSERT INTO scoring.scoring_definition VALUES (12, 'score-gaming-url', 'url-test-result', 'Gaming url test', '{"successWhen": false, "requiredHits": 1}', '', 2, 'score');
INSERT INTO scoring.scoring_definition VALUES (13, 'score-social-url', 'url-test-result', 'Social url test', '{"successWhen": false, "requiredHits": 1}', '', 3, 'score');
INSERT INTO scoring.scoring_definition VALUES (14, 'score-sortheners-url', 'url-test-result', 'Url sortheners url test', '{"successWhen": false, "requiredHits": 1}', '', 4, 'score');
INSERT INTO scoring.scoring_definition VALUES (15, 'score-cloud-url', 'url-test-result', 'Cloud access url test', '{"successWhen": false, "requiredHits": 1}', '', 5, 'score');
INSERT INTO scoring.scoring_definition VALUES (16, 'score-malware-url', 'boolean-result', 'Malware defences test', '{"successWhen": false, "requiredHits": 1}', '', 6, 'score');
INSERT INTO scoring.scoring_definition VALUES (17, 'score-cc-exf-url', 'exfiltration-result', 'CC exfitrantion tests', '{"successWhen": false, "requiredHits": 1}', '', 7, 'score');
INSERT INTO scoring.scoring_definition VALUES (18, 'score-ssn-exf-xlsx-url', 'exfiltration-result', 'SSN exfitration test xlsx', '{"successWhen": false, "requiredHits": 1}', '', 8, 'score');
INSERT INTO scoring.scoring_definition VALUES (19, 'score-ssn-exf-docx-url', 'exfiltration-result', 'SSN exfitration tests docx', '{"successWhen": false, "requiredHits": 1}', '', 9, 'score');
INSERT INTO scoring.scoring_definition VALUES (20, 'score-malware-zip', 'boolean-result', 'Malware defences test', '{"successWhen": false, "requiredHits": 1}', '', 10, 'score');
INSERT INTO scoring.scoring_definition VALUES (1, 'cis-7.4', 'url-test-result', '7.4 Maintain and Enforce Network-Based URL Filters', '{"successWhen": false, "requiredHits": 3}', 'Validates if the network filtering policy is in place and that it limits the system''s ability to connect to websites not approved by the organization.
This is achieved by attempting to fetch a non malicious artifact via HTTP GET requests from internet sites of various content categories ranging from social media, news, entertainment, streaming sites, to known phishing and malware domains. URLs located on malware or phishing domains only point to non-malicious and non-executable content, such as images.
All domains and sites are periodically tested in our testing environment to ensure safety and validity. ', 1, 'cis');
INSERT INTO scoring.scoring_definition VALUES (2, 'cis-7.5', 'url-test-result', '7.5 Uncategorized Site Should Be Blocked by Default', '{"successWhen": false}', 'Checks if company allows user to access sites with unknown category or IP addresses directly. Malware can use such domains to spread tonew locations.
This is achieved by attempting to fetch a non malicious artifact via HTTP GET requests from towerstreet server.', 2, 'cis');
INSERT INTO scoring.scoring_definition VALUES (3, 'cis-13.3', 'exfiltration-result', '13.3 Monitor and Block Unauthorized Network Traffic', '{"successWhen": false, "requiredHits": 1}', 'Checks if data loss prevention system (DLP) is in place, via simulating an exfiltration transfer of artificially generated PII data in various formats (csv, xlsx, docx) from the browser via HTTP POST method to our testing server. This automatically simulates the same actions, when an user uploads file containing PII to the internet.', 7, 'cis');
INSERT INTO scoring.scoring_definition VALUES (7, 'cis-8.1', 'boolean-result', '8.1 Utilize Centrally Managed Anti-malware Software', '{"successWhen": false, "requiredHits": 1}', 'Verifies the function of an endpoint security solution.
Attempts to download an Eicar file (a benign antivirus test file published at eicar.com) in form of a raw but also compressed executable file. Endpoint protection products, next generation firewalls or filtering application proxy servers should detect and block the download of this file. The file is downloaded only into memory, as it is not possible to store it on the disk without manual user interaction.', 4, 'cis');
INSERT INTO scoring.scoring_definition VALUES (9, 'cis-13.4', 'url-test-result', '13.4 Only Allow Access to Authorized Cloud Storage or Email Providers', '{"successWhen": false, "requiredHits": 1}', 'Validates if the network filtering policy is in place and that it limits the system''s ability to connect to cloud storage providers not approved by the organization.
This is achieved by attempting to fetch a non malicious artifact via HTTP GET requests.
All domains and sites are periodically tested in our testing environment to ensure safety and validity. ', 8, 'cis');


--
-- TOC entry 3273 (class 0 OID 400359)
-- Dependencies: 233
-- Data for Name: scoring_outcome; Type: TABLE DATA; Schema: scoring; Owner: towerstreet
--



--
-- TOC entry 3275 (class 0 OID 400377)
-- Dependencies: 235
-- Data for Name: scoring_result; Type: TABLE DATA; Schema: scoring; Owner: towerstreet
--



--
-- TOC entry 3276 (class 0 OID 400396)
-- Dependencies: 236
-- Data for Name: simulation_scoring_config; Type: TABLE DATA; Schema: scoring; Owner: towerstreet
--

INSERT INTO scoring.simulation_scoring_config VALUES (1, 19, 58);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 19, 57);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 19, 56);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 19, 55);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 19, 54);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 19, 53);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 19, 52);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 19, 51);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 19, 50);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 19, 49);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 19, 48);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 19, 47);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 19, 46);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 19, 45);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 19, 44);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 19, 43);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 19, 42);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 19, 41);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 19, 40);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 19, 39);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 19, 38);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 19, 37);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 19, 36);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 19, 35);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 19, 34);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 19, 33);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 19, 32);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 19, 30);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 19, 29);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 19, 28);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 19, 27);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 19, 26);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 19, 25);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 19, 24);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 19, 23);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 19, 22);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 19, 4);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 19, 2);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 19, 1);
INSERT INTO scoring.simulation_scoring_config VALUES (2, 19, 62);
INSERT INTO scoring.simulation_scoring_config VALUES (3, 19, 12);
INSERT INTO scoring.simulation_scoring_config VALUES (3, 19, 11);
INSERT INTO scoring.simulation_scoring_config VALUES (3, 19, 10);
INSERT INTO scoring.simulation_scoring_config VALUES (4, 19, 13);
INSERT INTO scoring.simulation_scoring_config VALUES (5, 19, 13);
INSERT INTO scoring.simulation_scoring_config VALUES (6, 19, 63);
INSERT INTO scoring.simulation_scoring_config VALUES (6, 19, 13);
INSERT INTO scoring.simulation_scoring_config VALUES (7, 19, 9);
INSERT INTO scoring.simulation_scoring_config VALUES (7, 19, 8);
INSERT INTO scoring.simulation_scoring_config VALUES (8, 19, 7);
INSERT INTO scoring.simulation_scoring_config VALUES (8, 19, 3);
INSERT INTO scoring.simulation_scoring_config VALUES (9, 19, 58);
INSERT INTO scoring.simulation_scoring_config VALUES (9, 19, 57);
INSERT INTO scoring.simulation_scoring_config VALUES (9, 19, 56);
INSERT INTO scoring.simulation_scoring_config VALUES (9, 19, 55);
INSERT INTO scoring.simulation_scoring_config VALUES (9, 19, 54);
INSERT INTO scoring.simulation_scoring_config VALUES (9, 19, 53);
INSERT INTO scoring.simulation_scoring_config VALUES (9, 19, 52);
INSERT INTO scoring.simulation_scoring_config VALUES (10, 19, 19);
INSERT INTO scoring.simulation_scoring_config VALUES (10, 19, 18);
INSERT INTO scoring.simulation_scoring_config VALUES (10, 19, 17);
INSERT INTO scoring.simulation_scoring_config VALUES (10, 19, 16);
INSERT INTO scoring.simulation_scoring_config VALUES (10, 19, 15);
INSERT INTO scoring.simulation_scoring_config VALUES (10, 19, 61);
INSERT INTO scoring.simulation_scoring_config VALUES (10, 19, 60);
INSERT INTO scoring.simulation_scoring_config VALUES (10, 19, 59);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 20, 58);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 20, 57);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 20, 56);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 20, 55);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 20, 54);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 20, 53);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 20, 52);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 20, 51);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 20, 50);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 20, 49);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 20, 48);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 20, 47);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 20, 46);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 20, 45);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 20, 44);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 20, 43);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 20, 42);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 20, 41);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 20, 40);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 20, 39);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 20, 38);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 20, 37);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 20, 36);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 20, 35);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 20, 34);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 20, 33);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 20, 32);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 20, 30);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 20, 29);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 20, 28);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 20, 27);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 20, 26);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 20, 25);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 20, 24);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 20, 23);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 20, 22);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 20, 4);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 20, 2);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 20, 1);
INSERT INTO scoring.simulation_scoring_config VALUES (2, 20, 62);
INSERT INTO scoring.simulation_scoring_config VALUES (3, 20, 12);
INSERT INTO scoring.simulation_scoring_config VALUES (3, 20, 11);
INSERT INTO scoring.simulation_scoring_config VALUES (3, 20, 10);
INSERT INTO scoring.simulation_scoring_config VALUES (4, 20, 13);
INSERT INTO scoring.simulation_scoring_config VALUES (5, 20, 13);
INSERT INTO scoring.simulation_scoring_config VALUES (6, 20, 63);
INSERT INTO scoring.simulation_scoring_config VALUES (6, 20, 13);
INSERT INTO scoring.simulation_scoring_config VALUES (7, 20, 9);
INSERT INTO scoring.simulation_scoring_config VALUES (7, 20, 8);
INSERT INTO scoring.simulation_scoring_config VALUES (8, 20, 7);
INSERT INTO scoring.simulation_scoring_config VALUES (8, 20, 3);
INSERT INTO scoring.simulation_scoring_config VALUES (9, 20, 58);
INSERT INTO scoring.simulation_scoring_config VALUES (9, 20, 57);
INSERT INTO scoring.simulation_scoring_config VALUES (9, 20, 56);
INSERT INTO scoring.simulation_scoring_config VALUES (9, 20, 55);
INSERT INTO scoring.simulation_scoring_config VALUES (9, 20, 54);
INSERT INTO scoring.simulation_scoring_config VALUES (9, 20, 53);
INSERT INTO scoring.simulation_scoring_config VALUES (9, 20, 52);
INSERT INTO scoring.simulation_scoring_config VALUES (10, 20, 19);
INSERT INTO scoring.simulation_scoring_config VALUES (10, 20, 18);
INSERT INTO scoring.simulation_scoring_config VALUES (10, 20, 17);
INSERT INTO scoring.simulation_scoring_config VALUES (10, 20, 16);
INSERT INTO scoring.simulation_scoring_config VALUES (10, 20, 15);
INSERT INTO scoring.simulation_scoring_config VALUES (10, 20, 61);
INSERT INTO scoring.simulation_scoring_config VALUES (10, 20, 60);
INSERT INTO scoring.simulation_scoring_config VALUES (10, 20, 59);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 21, 58);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 21, 57);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 21, 56);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 21, 55);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 21, 54);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 21, 53);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 21, 52);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 21, 51);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 21, 50);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 21, 49);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 21, 48);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 21, 47);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 21, 46);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 21, 45);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 21, 44);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 21, 43);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 21, 42);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 21, 41);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 21, 40);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 21, 39);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 21, 38);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 21, 37);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 21, 36);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 21, 35);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 21, 34);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 21, 33);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 21, 32);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 21, 30);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 21, 29);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 21, 28);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 21, 27);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 21, 26);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 21, 25);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 21, 24);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 21, 23);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 21, 22);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 21, 4);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 21, 2);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 21, 1);
INSERT INTO scoring.simulation_scoring_config VALUES (2, 21, 62);
INSERT INTO scoring.simulation_scoring_config VALUES (3, 21, 12);
INSERT INTO scoring.simulation_scoring_config VALUES (3, 21, 11);
INSERT INTO scoring.simulation_scoring_config VALUES (3, 21, 10);
INSERT INTO scoring.simulation_scoring_config VALUES (4, 21, 13);
INSERT INTO scoring.simulation_scoring_config VALUES (5, 21, 13);
INSERT INTO scoring.simulation_scoring_config VALUES (6, 21, 63);
INSERT INTO scoring.simulation_scoring_config VALUES (6, 21, 13);
INSERT INTO scoring.simulation_scoring_config VALUES (7, 21, 9);
INSERT INTO scoring.simulation_scoring_config VALUES (7, 21, 8);
INSERT INTO scoring.simulation_scoring_config VALUES (8, 21, 7);
INSERT INTO scoring.simulation_scoring_config VALUES (8, 21, 3);
INSERT INTO scoring.simulation_scoring_config VALUES (9, 21, 58);
INSERT INTO scoring.simulation_scoring_config VALUES (9, 21, 57);
INSERT INTO scoring.simulation_scoring_config VALUES (9, 21, 56);
INSERT INTO scoring.simulation_scoring_config VALUES (9, 21, 55);
INSERT INTO scoring.simulation_scoring_config VALUES (9, 21, 54);
INSERT INTO scoring.simulation_scoring_config VALUES (9, 21, 53);
INSERT INTO scoring.simulation_scoring_config VALUES (9, 21, 52);
INSERT INTO scoring.simulation_scoring_config VALUES (10, 21, 19);
INSERT INTO scoring.simulation_scoring_config VALUES (10, 21, 18);
INSERT INTO scoring.simulation_scoring_config VALUES (10, 21, 17);
INSERT INTO scoring.simulation_scoring_config VALUES (10, 21, 16);
INSERT INTO scoring.simulation_scoring_config VALUES (10, 21, 15);
INSERT INTO scoring.simulation_scoring_config VALUES (10, 21, 61);
INSERT INTO scoring.simulation_scoring_config VALUES (10, 21, 60);
INSERT INTO scoring.simulation_scoring_config VALUES (10, 21, 59);
INSERT INTO scoring.simulation_scoring_config VALUES (4, 19, 63);
INSERT INTO scoring.simulation_scoring_config VALUES (5, 19, 63);
INSERT INTO scoring.simulation_scoring_config VALUES (4, 20, 63);
INSERT INTO scoring.simulation_scoring_config VALUES (5, 20, 63);
INSERT INTO scoring.simulation_scoring_config VALUES (4, 21, 63);
INSERT INTO scoring.simulation_scoring_config VALUES (5, 21, 63);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 22, 58);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 22, 57);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 22, 56);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 22, 55);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 22, 54);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 22, 53);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 22, 52);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 22, 51);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 22, 50);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 22, 49);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 22, 48);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 22, 47);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 22, 46);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 22, 45);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 22, 44);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 22, 43);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 22, 42);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 22, 41);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 22, 40);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 22, 39);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 22, 38);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 22, 37);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 22, 36);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 22, 35);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 22, 34);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 22, 33);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 22, 32);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 22, 30);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 22, 29);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 22, 28);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 22, 27);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 22, 26);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 22, 25);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 22, 24);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 22, 23);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 22, 22);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 22, 4);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 22, 2);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 22, 1);
INSERT INTO scoring.simulation_scoring_config VALUES (2, 22, 62);
INSERT INTO scoring.simulation_scoring_config VALUES (3, 22, 12);
INSERT INTO scoring.simulation_scoring_config VALUES (3, 22, 11);
INSERT INTO scoring.simulation_scoring_config VALUES (3, 22, 10);
INSERT INTO scoring.simulation_scoring_config VALUES (4, 22, 13);
INSERT INTO scoring.simulation_scoring_config VALUES (5, 22, 13);
INSERT INTO scoring.simulation_scoring_config VALUES (6, 22, 63);
INSERT INTO scoring.simulation_scoring_config VALUES (6, 22, 13);
INSERT INTO scoring.simulation_scoring_config VALUES (7, 22, 9);
INSERT INTO scoring.simulation_scoring_config VALUES (7, 22, 8);
INSERT INTO scoring.simulation_scoring_config VALUES (8, 22, 7);
INSERT INTO scoring.simulation_scoring_config VALUES (8, 22, 3);
INSERT INTO scoring.simulation_scoring_config VALUES (9, 22, 58);
INSERT INTO scoring.simulation_scoring_config VALUES (9, 22, 57);
INSERT INTO scoring.simulation_scoring_config VALUES (9, 22, 56);
INSERT INTO scoring.simulation_scoring_config VALUES (9, 22, 55);
INSERT INTO scoring.simulation_scoring_config VALUES (9, 22, 54);
INSERT INTO scoring.simulation_scoring_config VALUES (9, 22, 53);
INSERT INTO scoring.simulation_scoring_config VALUES (9, 22, 52);
INSERT INTO scoring.simulation_scoring_config VALUES (10, 22, 19);
INSERT INTO scoring.simulation_scoring_config VALUES (10, 22, 18);
INSERT INTO scoring.simulation_scoring_config VALUES (10, 22, 17);
INSERT INTO scoring.simulation_scoring_config VALUES (10, 22, 16);
INSERT INTO scoring.simulation_scoring_config VALUES (10, 22, 15);
INSERT INTO scoring.simulation_scoring_config VALUES (10, 22, 61);
INSERT INTO scoring.simulation_scoring_config VALUES (10, 22, 60);
INSERT INTO scoring.simulation_scoring_config VALUES (10, 22, 59);
INSERT INTO scoring.simulation_scoring_config VALUES (4, 22, 63);
INSERT INTO scoring.simulation_scoring_config VALUES (5, 22, 63);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 24, 58);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 24, 57);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 24, 56);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 24, 55);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 24, 54);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 24, 53);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 24, 52);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 24, 51);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 24, 50);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 24, 49);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 24, 48);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 24, 47);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 24, 46);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 24, 45);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 24, 44);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 24, 43);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 24, 42);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 24, 41);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 24, 40);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 24, 39);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 24, 38);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 24, 37);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 24, 36);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 24, 35);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 24, 34);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 24, 33);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 24, 32);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 24, 30);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 24, 29);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 24, 28);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 24, 27);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 24, 26);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 24, 25);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 24, 24);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 24, 23);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 24, 22);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 24, 4);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 24, 2);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 24, 1);
INSERT INTO scoring.simulation_scoring_config VALUES (2, 24, 62);
INSERT INTO scoring.simulation_scoring_config VALUES (3, 24, 12);
INSERT INTO scoring.simulation_scoring_config VALUES (3, 24, 11);
INSERT INTO scoring.simulation_scoring_config VALUES (3, 24, 10);
INSERT INTO scoring.simulation_scoring_config VALUES (4, 24, 13);
INSERT INTO scoring.simulation_scoring_config VALUES (5, 24, 13);
INSERT INTO scoring.simulation_scoring_config VALUES (6, 24, 63);
INSERT INTO scoring.simulation_scoring_config VALUES (6, 24, 13);
INSERT INTO scoring.simulation_scoring_config VALUES (7, 24, 9);
INSERT INTO scoring.simulation_scoring_config VALUES (7, 24, 8);
INSERT INTO scoring.simulation_scoring_config VALUES (8, 24, 7);
INSERT INTO scoring.simulation_scoring_config VALUES (8, 24, 3);
INSERT INTO scoring.simulation_scoring_config VALUES (9, 24, 58);
INSERT INTO scoring.simulation_scoring_config VALUES (9, 24, 57);
INSERT INTO scoring.simulation_scoring_config VALUES (9, 24, 56);
INSERT INTO scoring.simulation_scoring_config VALUES (9, 24, 55);
INSERT INTO scoring.simulation_scoring_config VALUES (9, 24, 54);
INSERT INTO scoring.simulation_scoring_config VALUES (9, 24, 53);
INSERT INTO scoring.simulation_scoring_config VALUES (9, 24, 52);
INSERT INTO scoring.simulation_scoring_config VALUES (10, 24, 19);
INSERT INTO scoring.simulation_scoring_config VALUES (10, 24, 18);
INSERT INTO scoring.simulation_scoring_config VALUES (10, 24, 17);
INSERT INTO scoring.simulation_scoring_config VALUES (10, 24, 16);
INSERT INTO scoring.simulation_scoring_config VALUES (10, 24, 15);
INSERT INTO scoring.simulation_scoring_config VALUES (10, 24, 61);
INSERT INTO scoring.simulation_scoring_config VALUES (10, 24, 60);
INSERT INTO scoring.simulation_scoring_config VALUES (10, 24, 59);
INSERT INTO scoring.simulation_scoring_config VALUES (4, 24, 63);
INSERT INTO scoring.simulation_scoring_config VALUES (5, 24, 63);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 25, 66);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 25, 1);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 25, 2);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 25, 68);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 25, 69);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 25, 70);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 25, 4);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 25, 22);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 25, 23);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 25, 24);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 25, 25);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 25, 26);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 25, 27);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 25, 28);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 25, 29);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 25, 30);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 25, 32);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 25, 33);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 25, 34);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 25, 35);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 25, 36);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 25, 37);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 25, 38);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 25, 39);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 25, 40);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 25, 41);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 25, 42);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 25, 43);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 25, 44);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 25, 45);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 25, 46);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 25, 47);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 25, 48);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 25, 50);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 25, 51);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 25, 52);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 25, 53);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 25, 54);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 25, 55);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 25, 56);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 25, 57);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 25, 58);
INSERT INTO scoring.simulation_scoring_config VALUES (2, 25, 62);
INSERT INTO scoring.simulation_scoring_config VALUES (3, 25, 71);
INSERT INTO scoring.simulation_scoring_config VALUES (3, 25, 72);
INSERT INTO scoring.simulation_scoring_config VALUES (3, 25, 73);
INSERT INTO scoring.simulation_scoring_config VALUES (3, 25, 74);
INSERT INTO scoring.simulation_scoring_config VALUES (3, 25, 77);
INSERT INTO scoring.simulation_scoring_config VALUES (3, 25, 78);
INSERT INTO scoring.simulation_scoring_config VALUES (7, 25, 8);
INSERT INTO scoring.simulation_scoring_config VALUES (9, 25, 52);
INSERT INTO scoring.simulation_scoring_config VALUES (9, 25, 53);
INSERT INTO scoring.simulation_scoring_config VALUES (9, 25, 54);
INSERT INTO scoring.simulation_scoring_config VALUES (9, 25, 55);
INSERT INTO scoring.simulation_scoring_config VALUES (9, 25, 56);
INSERT INTO scoring.simulation_scoring_config VALUES (9, 25, 57);
INSERT INTO scoring.simulation_scoring_config VALUES (9, 25, 58);
INSERT INTO scoring.simulation_scoring_config VALUES (11, 25, 66);
INSERT INTO scoring.simulation_scoring_config VALUES (11, 25, 1);
INSERT INTO scoring.simulation_scoring_config VALUES (12, 25, 2);
INSERT INTO scoring.simulation_scoring_config VALUES (12, 25, 68);
INSERT INTO scoring.simulation_scoring_config VALUES (12, 25, 69);
INSERT INTO scoring.simulation_scoring_config VALUES (12, 25, 70);
INSERT INTO scoring.simulation_scoring_config VALUES (13, 25, 22);
INSERT INTO scoring.simulation_scoring_config VALUES (13, 25, 23);
INSERT INTO scoring.simulation_scoring_config VALUES (13, 25, 24);
INSERT INTO scoring.simulation_scoring_config VALUES (13, 25, 25);
INSERT INTO scoring.simulation_scoring_config VALUES (13, 25, 26);
INSERT INTO scoring.simulation_scoring_config VALUES (13, 25, 27);
INSERT INTO scoring.simulation_scoring_config VALUES (13, 25, 28);
INSERT INTO scoring.simulation_scoring_config VALUES (13, 25, 29);
INSERT INTO scoring.simulation_scoring_config VALUES (13, 25, 30);
INSERT INTO scoring.simulation_scoring_config VALUES (13, 25, 32);
INSERT INTO scoring.simulation_scoring_config VALUES (13, 25, 33);
INSERT INTO scoring.simulation_scoring_config VALUES (13, 25, 34);
INSERT INTO scoring.simulation_scoring_config VALUES (13, 25, 35);
INSERT INTO scoring.simulation_scoring_config VALUES (13, 25, 36);
INSERT INTO scoring.simulation_scoring_config VALUES (14, 25, 37);
INSERT INTO scoring.simulation_scoring_config VALUES (14, 25, 38);
INSERT INTO scoring.simulation_scoring_config VALUES (14, 25, 39);
INSERT INTO scoring.simulation_scoring_config VALUES (14, 25, 40);
INSERT INTO scoring.simulation_scoring_config VALUES (14, 25, 41);
INSERT INTO scoring.simulation_scoring_config VALUES (14, 25, 42);
INSERT INTO scoring.simulation_scoring_config VALUES (14, 25, 43);
INSERT INTO scoring.simulation_scoring_config VALUES (14, 25, 44);
INSERT INTO scoring.simulation_scoring_config VALUES (14, 25, 45);
INSERT INTO scoring.simulation_scoring_config VALUES (14, 25, 46);
INSERT INTO scoring.simulation_scoring_config VALUES (14, 25, 47);
INSERT INTO scoring.simulation_scoring_config VALUES (14, 25, 48);
INSERT INTO scoring.simulation_scoring_config VALUES (14, 25, 50);
INSERT INTO scoring.simulation_scoring_config VALUES (14, 25, 51);
INSERT INTO scoring.simulation_scoring_config VALUES (15, 25, 52);
INSERT INTO scoring.simulation_scoring_config VALUES (15, 25, 53);
INSERT INTO scoring.simulation_scoring_config VALUES (15, 25, 54);
INSERT INTO scoring.simulation_scoring_config VALUES (15, 25, 55);
INSERT INTO scoring.simulation_scoring_config VALUES (15, 25, 56);
INSERT INTO scoring.simulation_scoring_config VALUES (15, 25, 57);
INSERT INTO scoring.simulation_scoring_config VALUES (15, 25, 58);
INSERT INTO scoring.simulation_scoring_config VALUES (16, 25, 8);
INSERT INTO scoring.simulation_scoring_config VALUES (17, 25, 71);
INSERT INTO scoring.simulation_scoring_config VALUES (17, 25, 74);
INSERT INTO scoring.simulation_scoring_config VALUES (18, 25, 72);
INSERT INTO scoring.simulation_scoring_config VALUES (18, 25, 77);
INSERT INTO scoring.simulation_scoring_config VALUES (19, 25, 73);
INSERT INTO scoring.simulation_scoring_config VALUES (19, 25, 78);
INSERT INTO scoring.simulation_scoring_config VALUES (17, 25, 83);
INSERT INTO scoring.simulation_scoring_config VALUES (3, 25, 83);
INSERT INTO scoring.simulation_scoring_config VALUES (20, 25, 9);
INSERT INTO scoring.simulation_scoring_config VALUES (7, 25, 9);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 26, 42);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 26, 57);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 26, 56);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 26, 55);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 26, 54);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 26, 53);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 26, 52);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 26, 51);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 26, 50);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 26, 49);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 26, 48);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 26, 47);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 26, 46);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 26, 45);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 26, 44);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 26, 43);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 26, 58);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 26, 41);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 26, 40);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 26, 39);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 26, 38);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 26, 37);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 26, 36);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 26, 35);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 26, 34);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 26, 33);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 26, 32);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 26, 30);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 26, 29);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 26, 28);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 26, 27);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 26, 26);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 26, 25);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 26, 24);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 26, 23);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 26, 22);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 26, 4);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 26, 2);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 26, 1);
INSERT INTO scoring.simulation_scoring_config VALUES (2, 26, 62);
INSERT INTO scoring.simulation_scoring_config VALUES (3, 26, 71);
INSERT INTO scoring.simulation_scoring_config VALUES (3, 26, 72);
INSERT INTO scoring.simulation_scoring_config VALUES (3, 26, 73);
INSERT INTO scoring.simulation_scoring_config VALUES (3, 26, 74);
INSERT INTO scoring.simulation_scoring_config VALUES (3, 26, 77);
INSERT INTO scoring.simulation_scoring_config VALUES (3, 26, 78);
INSERT INTO scoring.simulation_scoring_config VALUES (3, 26, 83);
INSERT INTO scoring.simulation_scoring_config VALUES (4, 26, 63);
INSERT INTO scoring.simulation_scoring_config VALUES (4, 26, 13);
INSERT INTO scoring.simulation_scoring_config VALUES (5, 26, 13);
INSERT INTO scoring.simulation_scoring_config VALUES (5, 26, 63);
INSERT INTO scoring.simulation_scoring_config VALUES (6, 26, 63);
INSERT INTO scoring.simulation_scoring_config VALUES (6, 26, 13);
INSERT INTO scoring.simulation_scoring_config VALUES (7, 26, 9);
INSERT INTO scoring.simulation_scoring_config VALUES (7, 26, 8);
INSERT INTO scoring.simulation_scoring_config VALUES (8, 26, 7);
INSERT INTO scoring.simulation_scoring_config VALUES (8, 26, 3);
INSERT INTO scoring.simulation_scoring_config VALUES (9, 26, 56);
INSERT INTO scoring.simulation_scoring_config VALUES (9, 26, 55);
INSERT INTO scoring.simulation_scoring_config VALUES (9, 26, 54);
INSERT INTO scoring.simulation_scoring_config VALUES (9, 26, 53);
INSERT INTO scoring.simulation_scoring_config VALUES (9, 26, 52);
INSERT INTO scoring.simulation_scoring_config VALUES (9, 26, 57);
INSERT INTO scoring.simulation_scoring_config VALUES (9, 26, 58);
INSERT INTO scoring.simulation_scoring_config VALUES (10, 26, 17);
INSERT INTO scoring.simulation_scoring_config VALUES (10, 26, 16);
INSERT INTO scoring.simulation_scoring_config VALUES (10, 26, 15);
INSERT INTO scoring.simulation_scoring_config VALUES (10, 26, 61);
INSERT INTO scoring.simulation_scoring_config VALUES (10, 26, 60);
INSERT INTO scoring.simulation_scoring_config VALUES (10, 26, 59);
INSERT INTO scoring.simulation_scoring_config VALUES (10, 26, 19);
INSERT INTO scoring.simulation_scoring_config VALUES (10, 26, 18);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 27, 42);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 27, 57);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 27, 56);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 27, 55);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 27, 54);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 27, 53);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 27, 52);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 27, 51);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 27, 50);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 27, 49);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 27, 48);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 27, 47);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 27, 46);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 27, 45);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 27, 44);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 27, 43);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 27, 58);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 27, 41);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 27, 40);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 27, 39);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 27, 38);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 27, 37);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 27, 36);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 27, 35);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 27, 34);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 27, 33);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 27, 32);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 27, 30);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 27, 29);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 27, 28);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 27, 27);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 27, 26);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 27, 25);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 27, 24);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 27, 23);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 27, 22);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 27, 4);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 27, 2);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 27, 1);
INSERT INTO scoring.simulation_scoring_config VALUES (2, 27, 62);
INSERT INTO scoring.simulation_scoring_config VALUES (3, 27, 71);
INSERT INTO scoring.simulation_scoring_config VALUES (3, 27, 72);
INSERT INTO scoring.simulation_scoring_config VALUES (3, 27, 73);
INSERT INTO scoring.simulation_scoring_config VALUES (3, 27, 74);
INSERT INTO scoring.simulation_scoring_config VALUES (3, 27, 77);
INSERT INTO scoring.simulation_scoring_config VALUES (3, 27, 78);
INSERT INTO scoring.simulation_scoring_config VALUES (3, 27, 83);
INSERT INTO scoring.simulation_scoring_config VALUES (4, 27, 63);
INSERT INTO scoring.simulation_scoring_config VALUES (4, 27, 13);
INSERT INTO scoring.simulation_scoring_config VALUES (5, 27, 13);
INSERT INTO scoring.simulation_scoring_config VALUES (5, 27, 63);
INSERT INTO scoring.simulation_scoring_config VALUES (6, 27, 63);
INSERT INTO scoring.simulation_scoring_config VALUES (6, 27, 13);
INSERT INTO scoring.simulation_scoring_config VALUES (7, 27, 9);
INSERT INTO scoring.simulation_scoring_config VALUES (7, 27, 8);
INSERT INTO scoring.simulation_scoring_config VALUES (8, 27, 7);
INSERT INTO scoring.simulation_scoring_config VALUES (8, 27, 3);
INSERT INTO scoring.simulation_scoring_config VALUES (9, 27, 56);
INSERT INTO scoring.simulation_scoring_config VALUES (9, 27, 55);
INSERT INTO scoring.simulation_scoring_config VALUES (9, 27, 54);
INSERT INTO scoring.simulation_scoring_config VALUES (9, 27, 53);
INSERT INTO scoring.simulation_scoring_config VALUES (9, 27, 52);
INSERT INTO scoring.simulation_scoring_config VALUES (9, 27, 57);
INSERT INTO scoring.simulation_scoring_config VALUES (9, 27, 58);
INSERT INTO scoring.simulation_scoring_config VALUES (10, 27, 17);
INSERT INTO scoring.simulation_scoring_config VALUES (10, 27, 16);
INSERT INTO scoring.simulation_scoring_config VALUES (10, 27, 15);
INSERT INTO scoring.simulation_scoring_config VALUES (10, 27, 61);
INSERT INTO scoring.simulation_scoring_config VALUES (10, 27, 60);
INSERT INTO scoring.simulation_scoring_config VALUES (10, 27, 59);
INSERT INTO scoring.simulation_scoring_config VALUES (10, 27, 19);
INSERT INTO scoring.simulation_scoring_config VALUES (10, 27, 18);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 28, 42);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 28, 57);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 28, 56);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 28, 55);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 28, 54);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 28, 53);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 28, 52);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 28, 51);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 28, 50);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 28, 49);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 28, 48);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 28, 47);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 28, 46);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 28, 45);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 28, 44);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 28, 43);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 28, 58);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 28, 41);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 28, 40);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 28, 39);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 28, 38);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 28, 37);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 28, 36);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 28, 35);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 28, 34);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 28, 33);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 28, 32);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 28, 30);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 28, 29);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 28, 28);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 28, 27);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 28, 26);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 28, 25);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 28, 24);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 28, 23);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 28, 22);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 28, 4);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 28, 2);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 28, 1);
INSERT INTO scoring.simulation_scoring_config VALUES (2, 28, 62);
INSERT INTO scoring.simulation_scoring_config VALUES (3, 28, 71);
INSERT INTO scoring.simulation_scoring_config VALUES (3, 28, 72);
INSERT INTO scoring.simulation_scoring_config VALUES (3, 28, 73);
INSERT INTO scoring.simulation_scoring_config VALUES (3, 28, 74);
INSERT INTO scoring.simulation_scoring_config VALUES (3, 28, 77);
INSERT INTO scoring.simulation_scoring_config VALUES (3, 28, 78);
INSERT INTO scoring.simulation_scoring_config VALUES (3, 28, 83);
INSERT INTO scoring.simulation_scoring_config VALUES (4, 28, 63);
INSERT INTO scoring.simulation_scoring_config VALUES (4, 28, 13);
INSERT INTO scoring.simulation_scoring_config VALUES (5, 28, 13);
INSERT INTO scoring.simulation_scoring_config VALUES (5, 28, 63);
INSERT INTO scoring.simulation_scoring_config VALUES (6, 28, 63);
INSERT INTO scoring.simulation_scoring_config VALUES (6, 28, 13);
INSERT INTO scoring.simulation_scoring_config VALUES (7, 28, 9);
INSERT INTO scoring.simulation_scoring_config VALUES (7, 28, 8);
INSERT INTO scoring.simulation_scoring_config VALUES (8, 28, 7);
INSERT INTO scoring.simulation_scoring_config VALUES (8, 28, 3);
INSERT INTO scoring.simulation_scoring_config VALUES (9, 28, 56);
INSERT INTO scoring.simulation_scoring_config VALUES (9, 28, 55);
INSERT INTO scoring.simulation_scoring_config VALUES (9, 28, 54);
INSERT INTO scoring.simulation_scoring_config VALUES (9, 28, 53);
INSERT INTO scoring.simulation_scoring_config VALUES (9, 28, 52);
INSERT INTO scoring.simulation_scoring_config VALUES (9, 28, 57);
INSERT INTO scoring.simulation_scoring_config VALUES (9, 28, 58);
INSERT INTO scoring.simulation_scoring_config VALUES (10, 28, 17);
INSERT INTO scoring.simulation_scoring_config VALUES (10, 28, 16);
INSERT INTO scoring.simulation_scoring_config VALUES (10, 28, 15);
INSERT INTO scoring.simulation_scoring_config VALUES (10, 28, 61);
INSERT INTO scoring.simulation_scoring_config VALUES (10, 28, 60);
INSERT INTO scoring.simulation_scoring_config VALUES (10, 28, 59);
INSERT INTO scoring.simulation_scoring_config VALUES (10, 28, 19);
INSERT INTO scoring.simulation_scoring_config VALUES (10, 28, 18);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 29, 42);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 29, 57);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 29, 56);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 29, 55);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 29, 54);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 29, 53);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 29, 52);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 29, 51);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 29, 50);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 29, 49);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 29, 48);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 29, 47);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 29, 46);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 29, 45);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 29, 44);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 29, 43);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 29, 58);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 29, 41);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 29, 40);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 29, 39);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 29, 38);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 29, 37);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 29, 36);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 29, 35);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 29, 34);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 29, 33);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 29, 32);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 29, 30);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 29, 29);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 29, 28);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 29, 27);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 29, 26);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 29, 25);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 29, 24);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 29, 23);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 29, 22);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 29, 4);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 29, 2);
INSERT INTO scoring.simulation_scoring_config VALUES (1, 29, 1);
INSERT INTO scoring.simulation_scoring_config VALUES (2, 29, 62);
INSERT INTO scoring.simulation_scoring_config VALUES (3, 29, 71);
INSERT INTO scoring.simulation_scoring_config VALUES (3, 29, 72);
INSERT INTO scoring.simulation_scoring_config VALUES (3, 29, 73);
INSERT INTO scoring.simulation_scoring_config VALUES (3, 29, 74);
INSERT INTO scoring.simulation_scoring_config VALUES (3, 29, 77);
INSERT INTO scoring.simulation_scoring_config VALUES (3, 29, 78);
INSERT INTO scoring.simulation_scoring_config VALUES (3, 29, 83);
INSERT INTO scoring.simulation_scoring_config VALUES (4, 29, 63);
INSERT INTO scoring.simulation_scoring_config VALUES (4, 29, 13);
INSERT INTO scoring.simulation_scoring_config VALUES (5, 29, 13);
INSERT INTO scoring.simulation_scoring_config VALUES (5, 29, 63);
INSERT INTO scoring.simulation_scoring_config VALUES (6, 29, 63);
INSERT INTO scoring.simulation_scoring_config VALUES (6, 29, 13);
INSERT INTO scoring.simulation_scoring_config VALUES (7, 29, 9);
INSERT INTO scoring.simulation_scoring_config VALUES (7, 29, 8);
INSERT INTO scoring.simulation_scoring_config VALUES (8, 29, 7);
INSERT INTO scoring.simulation_scoring_config VALUES (8, 29, 3);
INSERT INTO scoring.simulation_scoring_config VALUES (9, 29, 56);
INSERT INTO scoring.simulation_scoring_config VALUES (9, 29, 55);
INSERT INTO scoring.simulation_scoring_config VALUES (9, 29, 54);
INSERT INTO scoring.simulation_scoring_config VALUES (9, 29, 53);
INSERT INTO scoring.simulation_scoring_config VALUES (9, 29, 52);
INSERT INTO scoring.simulation_scoring_config VALUES (9, 29, 57);
INSERT INTO scoring.simulation_scoring_config VALUES (9, 29, 58);
INSERT INTO scoring.simulation_scoring_config VALUES (10, 29, 17);
INSERT INTO scoring.simulation_scoring_config VALUES (10, 29, 16);
INSERT INTO scoring.simulation_scoring_config VALUES (10, 29, 15);
INSERT INTO scoring.simulation_scoring_config VALUES (10, 29, 61);
INSERT INTO scoring.simulation_scoring_config VALUES (10, 29, 60);
INSERT INTO scoring.simulation_scoring_config VALUES (10, 29, 59);
INSERT INTO scoring.simulation_scoring_config VALUES (10, 29, 19);
INSERT INTO scoring.simulation_scoring_config VALUES (10, 29, 18);


--
-- TOC entry 3305 (class 0 OID 0)
-- Dependencies: 200
-- Name: client_request_id_seq; Type: SEQUENCE SET; Schema: attacksimulator; Owner: towerstreet
--

SELECT pg_catalog.setval('attacksimulator.client_request_id_seq', 1, false);


--
-- TOC entry 3306 (class 0 OID 0)
-- Dependencies: 202
-- Name: network_segment_id_seq; Type: SEQUENCE SET; Schema: attacksimulator; Owner: towerstreet
--

SELECT pg_catalog.setval('attacksimulator.network_segment_id_seq', 1, false);


--
-- TOC entry 3307 (class 0 OID 0)
-- Dependencies: 204
-- Name: outcome_task_result_id_seq; Type: SEQUENCE SET; Schema: attacksimulator; Owner: towerstreet
--

SELECT pg_catalog.setval('attacksimulator.outcome_task_result_id_seq', 1, false);


--
-- TOC entry 3308 (class 0 OID 0)
-- Dependencies: 206
-- Name: received_data_id_seq; Type: SEQUENCE SET; Schema: attacksimulator; Owner: towerstreet
--

SELECT pg_catalog.setval('attacksimulator.received_data_id_seq', 1, false);


--
-- TOC entry 3309 (class 0 OID 0)
-- Dependencies: 208
-- Name: runner_id_seq; Type: SEQUENCE SET; Schema: attacksimulator; Owner: towerstreet
--

SELECT pg_catalog.setval('attacksimulator.runner_id_seq', 7, true);


--
-- TOC entry 3310 (class 0 OID 0)
-- Dependencies: 210
-- Name: simulation_id_seq; Type: SEQUENCE SET; Schema: attacksimulator; Owner: towerstreet
--

SELECT pg_catalog.setval('attacksimulator.simulation_id_seq', 17, true);


--
-- TOC entry 3311 (class 0 OID 0)
-- Dependencies: 212
-- Name: simulation_outcome_id_seq; Type: SEQUENCE SET; Schema: attacksimulator; Owner: towerstreet
--

SELECT pg_catalog.setval('attacksimulator.simulation_outcome_id_seq', 1, false);


--
-- TOC entry 3312 (class 0 OID 0)
-- Dependencies: 225
-- Name: simulation_template_id_seq; Type: SEQUENCE SET; Schema: attacksimulator; Owner: towerstreet
--

SELECT pg_catalog.setval('attacksimulator.simulation_template_id_seq', 29, true);


--
-- TOC entry 3313 (class 0 OID 0)
-- Dependencies: 214
-- Name: task_id_seq; Type: SEQUENCE SET; Schema: attacksimulator; Owner: towerstreet
--

SELECT pg_catalog.setval('attacksimulator.task_id_seq', 83, true);


--
-- TOC entry 3314 (class 0 OID 0)
-- Dependencies: 228
-- Name: test_case_id_seq; Type: SEQUENCE SET; Schema: attacksimulator; Owner: towerstreet
--

SELECT pg_catalog.setval('attacksimulator.test_case_id_seq', 14, true);


--
-- TOC entry 3315 (class 0 OID 0)
-- Dependencies: 219
-- Name: url_test_id_seq; Type: SEQUENCE SET; Schema: attacksimulator; Owner: towerstreet
--

SELECT pg_catalog.setval('attacksimulator.url_test_id_seq', 1, false);


--
-- TOC entry 3316 (class 0 OID 0)
-- Dependencies: 216
-- Name: user_id_seq; Type: SEQUENCE SET; Schema: attacksimulator; Owner: towerstreet
--

SELECT pg_catalog.setval('attacksimulator.user_id_seq', 2, true);


--
-- TOC entry 3317 (class 0 OID 0)
-- Dependencies: 221
-- Name: assessment_type_id_seq; Type: SEQUENCE SET; Schema: public; Owner: towerstreet
--

SELECT pg_catalog.setval('public.assessment_type_id_seq', 6, true);


--
-- TOC entry 3318 (class 0 OID 0)
-- Dependencies: 237
-- Name: campaign_visitor_id_seq; Type: SEQUENCE SET; Schema: public; Owner: towerstreet
--

SELECT pg_catalog.setval('public.campaign_visitor_id_seq', 1, true);


--
-- TOC entry 3319 (class 0 OID 0)
-- Dependencies: 217
-- Name: company_id_seq; Type: SEQUENCE SET; Schema: public; Owner: towerstreet
--

SELECT pg_catalog.setval('public.company_id_seq', 2, true);


--
-- TOC entry 3320 (class 0 OID 0)
-- Dependencies: 223
-- Name: customer_assessment_id_seq; Type: SEQUENCE SET; Schema: public; Owner: towerstreet
--

SELECT pg_catalog.setval('public.customer_assessment_id_seq', 2, true);


--
-- TOC entry 3321 (class 0 OID 0)
-- Dependencies: 230
-- Name: scoring_definition_id_seq; Type: SEQUENCE SET; Schema: scoring; Owner: towerstreet
--

SELECT pg_catalog.setval('scoring.scoring_definition_id_seq', 20, true);


--
-- TOC entry 3322 (class 0 OID 0)
-- Dependencies: 232
-- Name: scoring_outcome_id_seq; Type: SEQUENCE SET; Schema: scoring; Owner: towerstreet
--

SELECT pg_catalog.setval('scoring.scoring_outcome_id_seq', 1, false);


--
-- TOC entry 3323 (class 0 OID 0)
-- Dependencies: 234
-- Name: scoring_result_id_seq; Type: SEQUENCE SET; Schema: scoring; Owner: towerstreet
--

SELECT pg_catalog.setval('scoring.scoring_result_id_seq', 1, false);


--
-- TOC entry 3014 (class 2606 OID 399248)
-- Name: client_request client_request_pkey; Type: CONSTRAINT; Schema: attacksimulator; Owner: towerstreet
--

ALTER TABLE ONLY attacksimulator.client_request
    ADD CONSTRAINT client_request_pkey PRIMARY KEY (id);


--
-- TOC entry 3016 (class 2606 OID 399250)
-- Name: network_segment network_segment_pkey; Type: CONSTRAINT; Schema: attacksimulator; Owner: towerstreet
--

ALTER TABLE ONLY attacksimulator.network_segment
    ADD CONSTRAINT network_segment_pkey PRIMARY KEY (id);


--
-- TOC entry 3018 (class 2606 OID 399252)
-- Name: outcome_task_result outcome_task_result_pkey; Type: CONSTRAINT; Schema: attacksimulator; Owner: towerstreet
--

ALTER TABLE ONLY attacksimulator.outcome_task_result
    ADD CONSTRAINT outcome_task_result_pkey PRIMARY KEY (id);


--
-- TOC entry 3020 (class 2606 OID 399254)
-- Name: received_data received_data_pkey; Type: CONSTRAINT; Schema: attacksimulator; Owner: towerstreet
--

ALTER TABLE ONLY attacksimulator.received_data
    ADD CONSTRAINT received_data_pkey PRIMARY KEY (id);


--
-- TOC entry 3022 (class 2606 OID 399256)
-- Name: runner runner_pkey; Type: CONSTRAINT; Schema: attacksimulator; Owner: towerstreet
--

ALTER TABLE ONLY attacksimulator.runner
    ADD CONSTRAINT runner_pkey PRIMARY KEY (id);


--
-- TOC entry 3024 (class 2606 OID 399258)
-- Name: runner runner_runner_name_key; Type: CONSTRAINT; Schema: attacksimulator; Owner: towerstreet
--

ALTER TABLE ONLY attacksimulator.runner
    ADD CONSTRAINT runner_runner_name_key UNIQUE (runner_name);


--
-- TOC entry 3030 (class 2606 OID 399262)
-- Name: simulation_outcome simulation_outcome_pkey; Type: CONSTRAINT; Schema: attacksimulator; Owner: towerstreet
--

ALTER TABLE ONLY attacksimulator.simulation_outcome
    ADD CONSTRAINT simulation_outcome_pkey PRIMARY KEY (id);


--
-- TOC entry 3032 (class 2606 OID 399264)
-- Name: simulation_outcome simulation_outcome_token_key; Type: CONSTRAINT; Schema: attacksimulator; Owner: towerstreet
--

ALTER TABLE ONLY attacksimulator.simulation_outcome
    ADD CONSTRAINT simulation_outcome_token_key UNIQUE (token);


--
-- TOC entry 3026 (class 2606 OID 399266)
-- Name: simulation simulation_pkey; Type: CONSTRAINT; Schema: attacksimulator; Owner: towerstreet
--

ALTER TABLE ONLY attacksimulator.simulation
    ADD CONSTRAINT simulation_pkey PRIMARY KEY (id);


--
-- TOC entry 3058 (class 2606 OID 400182)
-- Name: simulation_template simulation_template_pkey; Type: CONSTRAINT; Schema: attacksimulator; Owner: towerstreet
--

ALTER TABLE ONLY attacksimulator.simulation_template
    ADD CONSTRAINT simulation_template_pkey PRIMARY KEY (id);


--
-- TOC entry 3060 (class 2606 OID 400184)
-- Name: simulation_template simulation_template_version_assessment_type_id_key; Type: CONSTRAINT; Schema: attacksimulator; Owner: towerstreet
--

ALTER TABLE ONLY attacksimulator.simulation_template
    ADD CONSTRAINT simulation_template_version_assessment_type_id_key UNIQUE (version, assessment_type_id);


--
-- TOC entry 3028 (class 2606 OID 399268)
-- Name: simulation simulation_token_key; Type: CONSTRAINT; Schema: attacksimulator; Owner: towerstreet
--

ALTER TABLE ONLY attacksimulator.simulation
    ADD CONSTRAINT simulation_token_key UNIQUE (token);


--
-- TOC entry 3034 (class 2606 OID 399270)
-- Name: task task_pkey; Type: CONSTRAINT; Schema: attacksimulator; Owner: towerstreet
--

ALTER TABLE ONLY attacksimulator.task
    ADD CONSTRAINT task_pkey PRIMARY KEY (id);


--
-- TOC entry 3036 (class 2606 OID 399272)
-- Name: task task_task_key_key; Type: CONSTRAINT; Schema: attacksimulator; Owner: towerstreet
--

ALTER TABLE ONLY attacksimulator.task
    ADD CONSTRAINT task_task_key_key UNIQUE (task_key);


--
-- TOC entry 3062 (class 2606 OID 400224)
-- Name: test_case test_case_pkey; Type: CONSTRAINT; Schema: attacksimulator; Owner: towerstreet
--

ALTER TABLE ONLY attacksimulator.test_case
    ADD CONSTRAINT test_case_pkey PRIMARY KEY (id);


--
-- TOC entry 3064 (class 2606 OID 400226)
-- Name: test_case test_case_test_case_key_key; Type: CONSTRAINT; Schema: attacksimulator; Owner: towerstreet
--

ALTER TABLE ONLY attacksimulator.test_case
    ADD CONSTRAINT test_case_test_case_key_key UNIQUE (test_case_key);


--
-- TOC entry 3050 (class 2606 OID 399986)
-- Name: url_test url_test_pkey; Type: CONSTRAINT; Schema: attacksimulator; Owner: towerstreet
--

ALTER TABLE ONLY attacksimulator.url_test
    ADD CONSTRAINT url_test_pkey PRIMARY KEY (id);


--
-- TOC entry 3038 (class 2606 OID 399274)
-- Name: user user_company_id_user_name_key; Type: CONSTRAINT; Schema: attacksimulator; Owner: towerstreet
--

ALTER TABLE ONLY attacksimulator."user"
    ADD CONSTRAINT user_company_id_user_name_key UNIQUE (customer_id, user_name);


--
-- TOC entry 3040 (class 2606 OID 399276)
-- Name: user user_pkey; Type: CONSTRAINT; Schema: attacksimulator; Owner: towerstreet
--

ALTER TABLE ONLY attacksimulator."user"
    ADD CONSTRAINT user_pkey PRIMARY KEY (id);


--
-- TOC entry 3052 (class 2606 OID 400106)
-- Name: assessment_type assessment_type_assessment_key_key; Type: CONSTRAINT; Schema: public; Owner: towerstreet
--

ALTER TABLE ONLY public.assessment_type
    ADD CONSTRAINT assessment_type_assessment_key_key UNIQUE (assessment_key);


--
-- TOC entry 3054 (class 2606 OID 400104)
-- Name: assessment_type assessment_type_pkey; Type: CONSTRAINT; Schema: public; Owner: towerstreet
--

ALTER TABLE ONLY public.assessment_type
    ADD CONSTRAINT assessment_type_pkey PRIMARY KEY (id);


--
-- TOC entry 3074 (class 2606 OID 400844)
-- Name: campaign_visitor campaign_visitor_pkey; Type: CONSTRAINT; Schema: public; Owner: towerstreet
--

ALTER TABLE ONLY public.campaign_visitor
    ADD CONSTRAINT campaign_visitor_pkey PRIMARY KEY (id);


--
-- TOC entry 3042 (class 2606 OID 399640)
-- Name: customer company_company_name_key; Type: CONSTRAINT; Schema: public; Owner: towerstreet
--

ALTER TABLE ONLY public.customer
    ADD CONSTRAINT company_company_name_key UNIQUE (company_name);


--
-- TOC entry 3044 (class 2606 OID 399638)
-- Name: customer company_pkey; Type: CONSTRAINT; Schema: public; Owner: towerstreet
--

ALTER TABLE ONLY public.customer
    ADD CONSTRAINT company_pkey PRIMARY KEY (id);


--
-- TOC entry 3046 (class 2606 OID 399642)
-- Name: customer company_short_name_key; Type: CONSTRAINT; Schema: public; Owner: towerstreet
--

ALTER TABLE ONLY public.customer
    ADD CONSTRAINT company_short_name_key UNIQUE (normalized_name);


--
-- TOC entry 3056 (class 2606 OID 400114)
-- Name: customer_assessment customer_assessment_pkey; Type: CONSTRAINT; Schema: public; Owner: towerstreet
--

ALTER TABLE ONLY public.customer_assessment
    ADD CONSTRAINT customer_assessment_pkey PRIMARY KEY (id);


--
-- TOC entry 3076 (class 2606 OID 400890)
-- Name: campaign_score_histogram campaign_score_histogram_pkey; Type: CONSTRAINT; Schema: scoring; Owner: towerstreet
--

ALTER TABLE ONLY scoring.campaign_score_histogram
    ADD CONSTRAINT campaign_score_histogram_pkey PRIMARY KEY (customer_id, score);


--
-- TOC entry 3066 (class 2606 OID 400354)
-- Name: scoring_definition scoring_definition_pkey; Type: CONSTRAINT; Schema: scoring; Owner: towerstreet
--

ALTER TABLE ONLY scoring.scoring_definition
    ADD CONSTRAINT scoring_definition_pkey PRIMARY KEY (id);


--
-- TOC entry 3068 (class 2606 OID 400356)
-- Name: scoring_definition scoring_definition_scoring_key_key; Type: CONSTRAINT; Schema: scoring; Owner: towerstreet
--

ALTER TABLE ONLY scoring.scoring_definition
    ADD CONSTRAINT scoring_definition_scoring_key_key UNIQUE (scoring_key);


--
-- TOC entry 3070 (class 2606 OID 400364)
-- Name: scoring_outcome scoring_outcome_pkey; Type: CONSTRAINT; Schema: scoring; Owner: towerstreet
--

ALTER TABLE ONLY scoring.scoring_outcome
    ADD CONSTRAINT scoring_outcome_pkey PRIMARY KEY (id);


--
-- TOC entry 3072 (class 2606 OID 400385)
-- Name: scoring_result scoring_result_pkey; Type: CONSTRAINT; Schema: scoring; Owner: towerstreet
--

ALTER TABLE ONLY scoring.scoring_result
    ADD CONSTRAINT scoring_result_pkey PRIMARY KEY (id);


--
-- TOC entry 3047 (class 1259 OID 400866)
-- Name: as_url_test_created_at_idx; Type: INDEX; Schema: attacksimulator; Owner: towerstreet
--

CREATE INDEX as_url_test_created_at_idx ON attacksimulator.url_test USING btree (created_at);


--
-- TOC entry 3048 (class 1259 OID 400867)
-- Name: url_test_created_at_idx; Type: INDEX; Schema: attacksimulator; Owner: towerstreet
--

CREATE INDEX url_test_created_at_idx ON attacksimulator.url_test USING btree (task_id, created_at);


--
-- TOC entry 3079 (class 2606 OID 399357)
-- Name: client_request client_request_received_data_id_fkey; Type: FK CONSTRAINT; Schema: attacksimulator; Owner: towerstreet
--

ALTER TABLE ONLY attacksimulator.client_request
    ADD CONSTRAINT client_request_received_data_id_fkey FOREIGN KEY (received_data_id) REFERENCES attacksimulator.received_data(id);


--
-- TOC entry 3078 (class 2606 OID 399362)
-- Name: client_request client_request_simulation_id_fkey; Type: FK CONSTRAINT; Schema: attacksimulator; Owner: towerstreet
--

ALTER TABLE ONLY attacksimulator.client_request
    ADD CONSTRAINT client_request_simulation_id_fkey FOREIGN KEY (simulation_id) REFERENCES attacksimulator.simulation(id);


--
-- TOC entry 3077 (class 2606 OID 399367)
-- Name: client_request client_request_simulation_outcome_id_fkey; Type: FK CONSTRAINT; Schema: attacksimulator; Owner: towerstreet
--

ALTER TABLE ONLY attacksimulator.client_request
    ADD CONSTRAINT client_request_simulation_outcome_id_fkey FOREIGN KEY (simulation_outcome_id) REFERENCES attacksimulator.simulation_outcome(id);


--
-- TOC entry 3081 (class 2606 OID 399372)
-- Name: network_segment network_segment_simulation_outcome_id_fkey; Type: FK CONSTRAINT; Schema: attacksimulator; Owner: towerstreet
--

ALTER TABLE ONLY attacksimulator.network_segment
    ADD CONSTRAINT network_segment_simulation_outcome_id_fkey FOREIGN KEY (simulation_outcome_id) REFERENCES attacksimulator.simulation_outcome(id);


--
-- TOC entry 3080 (class 2606 OID 399377)
-- Name: network_segment network_segment_task_id_fkey; Type: FK CONSTRAINT; Schema: attacksimulator; Owner: towerstreet
--

ALTER TABLE ONLY attacksimulator.network_segment
    ADD CONSTRAINT network_segment_task_id_fkey FOREIGN KEY (task_id) REFERENCES attacksimulator.task(id);


--
-- TOC entry 3084 (class 2606 OID 399382)
-- Name: outcome_task_result outcome_task_result_simulation_outcome_id_fkey; Type: FK CONSTRAINT; Schema: attacksimulator; Owner: towerstreet
--

ALTER TABLE ONLY attacksimulator.outcome_task_result
    ADD CONSTRAINT outcome_task_result_simulation_outcome_id_fkey FOREIGN KEY (simulation_outcome_id) REFERENCES attacksimulator.simulation_outcome(id);


--
-- TOC entry 3083 (class 2606 OID 399387)
-- Name: outcome_task_result outcome_task_result_task_id_fkey; Type: FK CONSTRAINT; Schema: attacksimulator; Owner: towerstreet
--

ALTER TABLE ONLY attacksimulator.outcome_task_result
    ADD CONSTRAINT outcome_task_result_task_id_fkey FOREIGN KEY (task_id) REFERENCES attacksimulator.task(id);


--
-- TOC entry 3082 (class 2606 OID 400414)
-- Name: outcome_task_result outcome_task_result_url_test_id_fkey; Type: FK CONSTRAINT; Schema: attacksimulator; Owner: towerstreet
--

ALTER TABLE ONLY attacksimulator.outcome_task_result
    ADD CONSTRAINT outcome_task_result_url_test_id_fkey FOREIGN KEY (url_test_id) REFERENCES attacksimulator.url_test(id);


--
-- TOC entry 3086 (class 2606 OID 399392)
-- Name: received_data received_data_simulation_outcome_id_fkey; Type: FK CONSTRAINT; Schema: attacksimulator; Owner: towerstreet
--

ALTER TABLE ONLY attacksimulator.received_data
    ADD CONSTRAINT received_data_simulation_outcome_id_fkey FOREIGN KEY (simulation_outcome_id) REFERENCES attacksimulator.simulation_outcome(id);


--
-- TOC entry 3085 (class 2606 OID 399397)
-- Name: received_data received_data_task_id_fkey; Type: FK CONSTRAINT; Schema: attacksimulator; Owner: towerstreet
--

ALTER TABLE ONLY attacksimulator.received_data
    ADD CONSTRAINT received_data_task_id_fkey FOREIGN KEY (task_id) REFERENCES attacksimulator.task(id);


--
-- TOC entry 3088 (class 2606 OID 400151)
-- Name: simulation simulation_customer_assessment_id_fkey; Type: FK CONSTRAINT; Schema: attacksimulator; Owner: towerstreet
--

ALTER TABLE ONLY attacksimulator.simulation
    ADD CONSTRAINT simulation_customer_assessment_id_fkey FOREIGN KEY (customer_assessment_id) REFERENCES public.customer_assessment(id);


--
-- TOC entry 3090 (class 2606 OID 399412)
-- Name: simulation_outcome simulation_outcome_simulation_id_fkey; Type: FK CONSTRAINT; Schema: attacksimulator; Owner: towerstreet
--

ALTER TABLE ONLY attacksimulator.simulation_outcome
    ADD CONSTRAINT simulation_outcome_simulation_id_fkey FOREIGN KEY (simulation_id) REFERENCES attacksimulator.simulation(id);


--
-- TOC entry 3098 (class 2606 OID 400185)
-- Name: simulation_template simulation_template_assessment_type_id_fkey; Type: FK CONSTRAINT; Schema: attacksimulator; Owner: towerstreet
--

ALTER TABLE ONLY attacksimulator.simulation_template
    ADD CONSTRAINT simulation_template_assessment_type_id_fkey FOREIGN KEY (assessment_type_id) REFERENCES public.assessment_type(id);


--
-- TOC entry 3100 (class 2606 OID 400198)
-- Name: simulation_template_config simulation_template_config_task_id_fkey; Type: FK CONSTRAINT; Schema: attacksimulator; Owner: towerstreet
--

ALTER TABLE ONLY attacksimulator.simulation_template_config
    ADD CONSTRAINT simulation_template_config_task_id_fkey FOREIGN KEY (task_id) REFERENCES attacksimulator.task(id);


--
-- TOC entry 3101 (class 2606 OID 400193)
-- Name: simulation_template_config simulation_template_config_template_id_fkey; Type: FK CONSTRAINT; Schema: attacksimulator; Owner: towerstreet
--

ALTER TABLE ONLY attacksimulator.simulation_template_config
    ADD CONSTRAINT simulation_template_config_template_id_fkey FOREIGN KEY (template_id) REFERENCES attacksimulator.simulation_template(id);


--
-- TOC entry 3099 (class 2606 OID 400227)
-- Name: simulation_template_config simulation_template_config_test_case_id_fkey; Type: FK CONSTRAINT; Schema: attacksimulator; Owner: towerstreet
--

ALTER TABLE ONLY attacksimulator.simulation_template_config
    ADD CONSTRAINT simulation_template_config_test_case_id_fkey FOREIGN KEY (test_case_id) REFERENCES attacksimulator.test_case(id);


--
-- TOC entry 3087 (class 2606 OID 400203)
-- Name: simulation simulation_template_id_fkey; Type: FK CONSTRAINT; Schema: attacksimulator; Owner: towerstreet
--

ALTER TABLE ONLY attacksimulator.simulation
    ADD CONSTRAINT simulation_template_id_fkey FOREIGN KEY (template_id) REFERENCES attacksimulator.simulation_template(id);


--
-- TOC entry 3089 (class 2606 OID 399417)
-- Name: simulation simulation_user_id_fkey; Type: FK CONSTRAINT; Schema: attacksimulator; Owner: towerstreet
--

ALTER TABLE ONLY attacksimulator.simulation
    ADD CONSTRAINT simulation_user_id_fkey FOREIGN KEY (user_id) REFERENCES attacksimulator."user"(id);


--
-- TOC entry 3091 (class 2606 OID 399422)
-- Name: task task_runner_id_fkey; Type: FK CONSTRAINT; Schema: attacksimulator; Owner: towerstreet
--

ALTER TABLE ONLY attacksimulator.task
    ADD CONSTRAINT task_runner_id_fkey FOREIGN KEY (runner_id) REFERENCES attacksimulator.runner(id);


--
-- TOC entry 3095 (class 2606 OID 399987)
-- Name: url_test url_test_client_request_fkey; Type: FK CONSTRAINT; Schema: attacksimulator; Owner: towerstreet
--

ALTER TABLE ONLY attacksimulator.url_test
    ADD CONSTRAINT url_test_client_request_fkey FOREIGN KEY (client_request) REFERENCES attacksimulator.client_request(id);


--
-- TOC entry 3094 (class 2606 OID 399992)
-- Name: url_test url_test_task_id_fkey; Type: FK CONSTRAINT; Schema: attacksimulator; Owner: towerstreet
--

ALTER TABLE ONLY attacksimulator.url_test
    ADD CONSTRAINT url_test_task_id_fkey FOREIGN KEY (task_id) REFERENCES attacksimulator.task(id);


--
-- TOC entry 3092 (class 2606 OID 400861)
-- Name: user user_campaign_visitor_id_fkey; Type: FK CONSTRAINT; Schema: attacksimulator; Owner: towerstreet
--

ALTER TABLE ONLY attacksimulator."user"
    ADD CONSTRAINT user_campaign_visitor_id_fkey FOREIGN KEY (campaign_visitor_id) REFERENCES public.campaign_visitor(id);


--
-- TOC entry 3093 (class 2606 OID 400013)
-- Name: user user_company_id_fkey; Type: FK CONSTRAINT; Schema: attacksimulator; Owner: towerstreet
--

ALTER TABLE ONLY attacksimulator."user"
    ADD CONSTRAINT user_company_id_fkey FOREIGN KEY (customer_id) REFERENCES public.customer(id);


--
-- TOC entry 3109 (class 2606 OID 400845)
-- Name: campaign_visitor campaign_visitor_customer_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: towerstreet
--

ALTER TABLE ONLY public.campaign_visitor
    ADD CONSTRAINT campaign_visitor_customer_id_fkey FOREIGN KEY (customer_id) REFERENCES public.customer(id);


--
-- TOC entry 3097 (class 2606 OID 400115)
-- Name: customer_assessment customer_assessment_assessment_type_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: towerstreet
--

ALTER TABLE ONLY public.customer_assessment
    ADD CONSTRAINT customer_assessment_assessment_type_id_fkey FOREIGN KEY (assessment_type_id) REFERENCES public.assessment_type(id);


--
-- TOC entry 3096 (class 2606 OID 400120)
-- Name: customer_assessment customer_assessment_customer_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: towerstreet
--

ALTER TABLE ONLY public.customer_assessment
    ADD CONSTRAINT customer_assessment_customer_id_fkey FOREIGN KEY (customer_id) REFERENCES public.customer(id);


--
-- TOC entry 3110 (class 2606 OID 400891)
-- Name: campaign_score_histogram campaign_score_histogram_customer_id_fkey; Type: FK CONSTRAINT; Schema: scoring; Owner: towerstreet
--

ALTER TABLE ONLY scoring.campaign_score_histogram
    ADD CONSTRAINT campaign_score_histogram_customer_id_fkey FOREIGN KEY (customer_id) REFERENCES public.customer(id);


--
-- TOC entry 3103 (class 2606 OID 400365)
-- Name: scoring_outcome scoring_outcome_customer_assessment_id_fkey; Type: FK CONSTRAINT; Schema: scoring; Owner: towerstreet
--

ALTER TABLE ONLY scoring.scoring_outcome
    ADD CONSTRAINT scoring_outcome_customer_assessment_id_fkey FOREIGN KEY (customer_assessment_id) REFERENCES public.customer_assessment(id);


--
-- TOC entry 3102 (class 2606 OID 400370)
-- Name: scoring_outcome scoring_outcome_simulation_outcome_id_fkey; Type: FK CONSTRAINT; Schema: scoring; Owner: towerstreet
--

ALTER TABLE ONLY scoring.scoring_outcome
    ADD CONSTRAINT scoring_outcome_simulation_outcome_id_fkey FOREIGN KEY (simulation_outcome_id) REFERENCES attacksimulator.simulation_outcome(id);


--
-- TOC entry 3105 (class 2606 OID 400386)
-- Name: scoring_result scoring_result_scoring_definition_id_fkey; Type: FK CONSTRAINT; Schema: scoring; Owner: towerstreet
--

ALTER TABLE ONLY scoring.scoring_result
    ADD CONSTRAINT scoring_result_scoring_definition_id_fkey FOREIGN KEY (scoring_definition_id) REFERENCES scoring.scoring_definition(id);


--
-- TOC entry 3104 (class 2606 OID 400391)
-- Name: scoring_result scoring_result_scoring_outcome_id_fkey; Type: FK CONSTRAINT; Schema: scoring; Owner: towerstreet
--

ALTER TABLE ONLY scoring.scoring_result
    ADD CONSTRAINT scoring_result_scoring_outcome_id_fkey FOREIGN KEY (scoring_outcome_id) REFERENCES scoring.scoring_outcome(id);


--
-- TOC entry 3108 (class 2606 OID 400399)
-- Name: simulation_scoring_config simulation_scoring_config_scoring_definition_id_fkey; Type: FK CONSTRAINT; Schema: scoring; Owner: towerstreet
--

ALTER TABLE ONLY scoring.simulation_scoring_config
    ADD CONSTRAINT simulation_scoring_config_scoring_definition_id_fkey FOREIGN KEY (scoring_definition_id) REFERENCES scoring.scoring_definition(id);


--
-- TOC entry 3107 (class 2606 OID 400404)
-- Name: simulation_scoring_config simulation_scoring_config_simulation_template_id_fkey; Type: FK CONSTRAINT; Schema: scoring; Owner: towerstreet
--

ALTER TABLE ONLY scoring.simulation_scoring_config
    ADD CONSTRAINT simulation_scoring_config_simulation_template_id_fkey FOREIGN KEY (simulation_template_id) REFERENCES attacksimulator.simulation_template(id);


--
-- TOC entry 3106 (class 2606 OID 400409)
-- Name: simulation_scoring_config simulation_scoring_config_task_id_fkey; Type: FK CONSTRAINT; Schema: scoring; Owner: towerstreet
--

ALTER TABLE ONLY scoring.simulation_scoring_config
    ADD CONSTRAINT simulation_scoring_config_task_id_fkey FOREIGN KEY (task_id) REFERENCES attacksimulator.task(id);


-- Completed on 2019-05-02 09:47:08 CEST

--
-- PostgreSQL database dump complete
--

