CREATE DATABASE morocco_immovable;

DROP TABLE IF EXISTS users;
CREATE TABLE users
(
    id            SERIAL              NOT NULL PRIMARY KEY,
    first_name    VARCHAR(120)        NOT NULL,
    last_name     VARCHAR(120)        NOT NULL,
    email         VARCHAR(120) UNIQUE NOT NULL,
    password      VARCHAR(120)        NOT NULL,
    identity_code VARCHAR(20) UNIQUE  NOT NULL,
    phone_number  VARCHAR(20) UNIQUE  NOT NULL,
    age           INT                 NOT NULL,
    city          VARCHAR(120)        NOT NULL,
    active        BOOLEAN             NOT NULL DEFAULT FALSE
);

DROP TABLE IF EXISTS houses;
CREATE TABLE houses
(
    id          SERIAL       NOT NULL PRIMARY KEY,
    description VARCHAR(120) NOT NULL,
    owner       INT          NOT NULL,
    price       FLOAT        NOT NULL,
    type        VARCHAR(120) NOT NULL,
    status      BOOLEAN      NOT NULL DEFAULT FALSE,
    photo       VARCHAR(20)  NOT NULL
);
