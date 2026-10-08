CREATE TABLE messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sender_name VARCHAR(100) NOT NULL,
    sender_email VARCHAR(254) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    processed_at TIMESTAMPTZ,

    CONSTRAINT chk_messages_status
        CHECK (status IN ('PENDING', 'QUALIFIED', 'NOT_A_LEAD', 'FAILED')),
    CONSTRAINT chk_messages_content_length
        CHECK (char_length(content) BETWEEN 1 AND 5000)
);

CREATE INDEX idx_messages_created_at ON messages (created_at DESC);
CREATE INDEX idx_messages_status     ON messages (status);

CREATE TABLE leads (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id UUID NOT NULL UNIQUE REFERENCES messages (id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    urgency VARCHAR(20) NOT NULL,
    summary TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_leads_type
        CHECK (type IN ('demo-request', 'pricing-inquiry', 'partnership', 'support', 'other')),
    CONSTRAINT chk_leads_urgency
        CHECK (urgency IN ('low', 'medium', 'high'))
);

CREATE INDEX idx_leads_urgency ON leads (urgency);
CREATE INDEX idx_leads_type    ON leads (type);