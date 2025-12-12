-- Track last read timestamp per user and private conversation
CREATE TABLE private_chat_read_states (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    other_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    last_read_at TIMESTAMP NULL,
    CONSTRAINT uq_private_chat_read_state UNIQUE (user_id, other_user_id)
);

CREATE INDEX idx_private_chat_read_states_user_other
    ON private_chat_read_states(user_id, other_user_id);


-- Track last read timestamp per user and community
CREATE TABLE community_chat_read_states (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    community_id UUID NOT NULL REFERENCES communities(id) ON DELETE CASCADE,
    last_read_at TIMESTAMP NULL,
    CONSTRAINT uq_community_chat_read_state UNIQUE (user_id, community_id)
);

CREATE INDEX idx_community_chat_read_states_user_community
    ON community_chat_read_states(user_id, community_id);


-- Track last read timestamp per user and match chat
CREATE TABLE match_chat_read_states (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    match_id UUID NOT NULL REFERENCES matches(id) ON DELETE CASCADE,
    last_read_at TIMESTAMP NULL,
    CONSTRAINT uq_match_chat_read_state UNIQUE (user_id, match_id)
);

CREATE INDEX idx_match_chat_read_states_user_match
    ON match_chat_read_states(user_id, match_id);


