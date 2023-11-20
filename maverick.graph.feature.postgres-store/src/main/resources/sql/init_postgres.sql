CREATE TABLE book (
    id SERIAL PRIMARY KEY,
    title TEXT NOT NULL,
    author TEXT NOT NULL
);


CREATE TABLE fragments
(
    id SERIAL PRIMARY KEY,
    iri TEXT NOT NULL,
    values jsonb,
    relations jsonb
);