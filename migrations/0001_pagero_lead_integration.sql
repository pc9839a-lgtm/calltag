-- PageRo -> CallTag lead integration storage
-- Apply with: wrangler d1 migrations apply <DATABASE_NAME>

CREATE TABLE IF NOT EXISTS pagero_leads (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    event_id TEXT NOT NULL UNIQUE,
    workspace_key TEXT NOT NULL,
    site_id TEXT NOT NULL,
    customer_name TEXT NOT NULL,
    primary_phone TEXT NOT NULL,
    normalized_phone TEXT NOT NULL,
    email TEXT NOT NULL DEFAULT '',
    inquiry_content TEXT NOT NULL DEFAULT '',
    source_url TEXT NOT NULL DEFAULT '',
    campaign TEXT NOT NULL DEFAULT '',
    metadata_json TEXT NOT NULL DEFAULT '{}',
    submitted_at INTEGER NOT NULL,
    status TEXT NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'DELIVERED', 'IMPORTED', 'REJECTED')),
    delivered_at INTEGER,
    imported_at INTEGER,
    import_result TEXT NOT NULL DEFAULT '',
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_pagero_leads_workspace_pending
    ON pagero_leads(workspace_key, status, id);

CREATE INDEX IF NOT EXISTS idx_pagero_leads_workspace_phone
    ON pagero_leads(workspace_key, normalized_phone, submitted_at DESC);

CREATE INDEX IF NOT EXISTS idx_pagero_leads_site_created
    ON pagero_leads(site_id, created_at DESC);
