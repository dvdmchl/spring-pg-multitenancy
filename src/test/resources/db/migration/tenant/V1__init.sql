CREATE TABLE ${schema}.parent
(
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL
);

CREATE TABLE ${schema}.child
(
    id        BIGSERIAL PRIMARY KEY,
    parent_id BIGINT NOT NULL REFERENCES ${schema}.parent (id)
);
