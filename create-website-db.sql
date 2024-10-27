-- Generates the schema for the website H2 DB
-- Use command: runscript from 'create-website-db.sql' to generate DB tables
;              
CREATE SEQUENCE "PUBLIC"."BLOG_SEQ" START WITH 1;
CREATE SEQUENCE "PUBLIC"."STATIC_FILE_SEQ" START WITH 1 RESTART WITH 3;
CREATE CACHED TABLE "PUBLIC"."BLOG"(
    "ID" BIGINT NOT NULL,
    "CREATED" TIMESTAMP(6) NOT NULL,
    "FILE_NAME" CHARACTER VARYING(255) NOT NULL,
    "HASH" CHARACTER VARYING(255) NOT NULL,
    "TITLE" CHARACTER VARYING(255) NOT NULL,
    "UPDATED" TIMESTAMP(6)
);
ALTER TABLE "PUBLIC"."BLOG" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_1" PRIMARY KEY("ID");
CREATE CACHED TABLE "PUBLIC"."STATIC_FILE"(
    "ID" BIGINT NOT NULL,
    "HASH" CHARACTER VARYING(255) NOT NULL,
    "MODIFIED" TIMESTAMP(6) NOT NULL,
    "NAME" CHARACTER VARYING(255) NOT NULL,
    "TYPE" ENUM('CSS', 'IMAGE') NOT NULL
);
ALTER TABLE "PUBLIC"."STATIC_FILE" ADD CONSTRAINT "PUBLIC"."CONSTRAINT_B" PRIMARY KEY("ID");
ALTER TABLE "PUBLIC"."BLOG" ADD CONSTRAINT "PUBLIC"."UKRNROU1M94MUCGT39EPCW8OV59" UNIQUE NULLS DISTINCT ("TITLE");
ALTER TABLE "PUBLIC"."BLOG" ADD CONSTRAINT "PUBLIC"."UKSF1PVRAKVQMVSD6601YJ154IQ" UNIQUE NULLS DISTINCT ("FILE_NAME");
ALTER TABLE "PUBLIC"."STATIC_FILE" ADD CONSTRAINT "PUBLIC"."UK46VFLF26WY2GO80AIDYVPIUI3" UNIQUE NULLS DISTINCT ("NAME");