CREATE SCHEMA IF NOT EXISTS ${application.db.schema};

ALTER DATABASE ${application.db.name} SET search_path TO ${application.db.schema},public;

CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA ${application.db.schema};