-- Create private_chat_messages table
CREATE TABLE private_chat_messages (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    sender_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    recipient_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    message TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_private_chat_sender
        FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_private_chat_recipient
        FOREIGN KEY (recipient_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Indexes for private_chat_messages
CREATE INDEX idx_private_chat_sender_id ON private_chat_messages(sender_id);
CREATE INDEX idx_private_chat_recipient_id ON private_chat_messages(recipient_id);
CREATE INDEX idx_private_chat_created_at ON private_chat_messages(created_at DESC);


-- Create community_chat_messages table
CREATE TABLE community_chat_messages (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    community_id UUID NOT NULL REFERENCES communities(id) ON DELETE CASCADE,
    sender_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    message TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_community_chat_community
        FOREIGN KEY (community_id) REFERENCES communities(id) ON DELETE CASCADE,
    CONSTRAINT fk_community_chat_sender
        FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Indexes for community_chat_messages
CREATE INDEX idx_community_chat_community_id ON community_chat_messages(community_id);
CREATE INDEX idx_community_chat_sender_id ON community_chat_messages(sender_id);
CREATE INDEX idx_community_chat_created_at ON community_chat_messages(created_at DESC);


