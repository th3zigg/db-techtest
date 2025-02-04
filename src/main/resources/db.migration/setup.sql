create sequence if not exists SEQ_DATA_HEADER;

drop table if exists DATA_HEADER;

create table DATA_HEADER
(
    DATA_HEADER_ID      NUMBER NOT NULL,
    NAME                VARCHAR2(30 CHAR) NOT NULL,
    BLOCKTYPE           VARCHAR2(11 CHAR) NOT NULL,
    CREATED_TIMESTAMP   TIMESTAMP (6) NOT NULL,
    CONSTRAINT PK_DATA_HEADER PRIMARY KEY (DATA_HEADER_ID),
    CONSTRAINT UK_DATA_HEADER UNIQUE (NAME)
);

create sequence if not exists SEQ_DATA_STORE;

drop table if exists DATA_STORE;

create table DATA_STORE
(
    DATA_STORE_ID           NUMBER NOT NULL,
    DATA_HEADER_ID          NUMBER NOT NULL,
    DATA_BODY               VARCHAR2(1000 CHAR) NOT NULL,
    DATA_BODY_CHECKSUM      VARCHAR2(1000 CHAR) NOT NULL,
    CREATED_TIMESTAMP       TIMESTAMP (6) NOT NULL,
    CONSTRAINT PK_DATA_STORE PRIMARY KEY (DATA_STORE_ID),
    CONSTRAINT FK_DS_DH FOREIGN KEY (DATA_HEADER_ID) REFERENCES DATA_HEADER (DATA_HEADER_ID)
);