CREATE TABLE users (
    id       uuid         NOT NULL DEFAULT gen_random_uuid(),
    email    varchar(254) NOT NULL,
    name     varchar(20)  NOT NULL,
    password text         NOT NULL,
    is_admin boolean      NOT NULL DEFAULT false,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE TABLE blog_post (
    id          uuid            NOT NULL DEFAULT gen_random_uuid(),
    title       varchar(250)    NOT NULL,
    subtitle    varchar(250)    NOT NULL,
    date        date            NOT NULL,
    body        text            NOT NULL,
    img_url     varchar(250)    NOT NULL,
    author_id   uuid            NOT NULL,
    CONSTRAINT pk_blog_post     PRIMARY KEY (id),
    CONSTRAINT uq_blog_post_title UNIQUE (title),
    CONSTRAINT fk_blog_post_author FOREIGN KEY (author_id)
        REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_blog_post_author_id ON blog_post (author_id);
