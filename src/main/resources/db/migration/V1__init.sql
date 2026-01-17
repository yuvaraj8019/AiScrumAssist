CREATE TABLE meetings (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    ceremony_type VARCHAR(50),
    meeting_date TIMESTAMP,
    tool_type VARCHAR(50),
    project_key VARCHAR(255),
    status VARCHAR(50) NOT NULL,
    transcript TEXT,
    summary TEXT,
    audio_filename VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE extracted_items (
    id UUID PRIMARY KEY,
    meeting_id UUID NOT NULL,
    item_type VARCHAR(50) NOT NULL,
    content TEXT,
    created_at TIMESTAMP,
    CONSTRAINT fk_items_meeting FOREIGN KEY (meeting_id) REFERENCES meetings(id)
);

CREATE TABLE tasks (
    id UUID PRIMARY KEY,
    meeting_id UUID NOT NULL,
    tool_type VARCHAR(50),
    external_key_or_id VARCHAR(255),
    title VARCHAR(255),
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP,
    CONSTRAINT fk_tasks_meeting FOREIGN KEY (meeting_id) REFERENCES meetings(id)
);
