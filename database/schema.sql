CREATE TABLE IF NOT EXISTS game_sessions (
    session_id CHAR(36) NOT NULL,
    session_name VARCHAR(120) NOT NULL,
    random_seed BIGINT NOT NULL,
    status VARCHAR(24) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    started_at DATETIME(6) NULL,
    completed_at DATETIME(6) NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (session_id),
    INDEX idx_game_sessions_status_updated (status, updated_at),
    INDEX idx_game_sessions_created (created_at)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS game_results (
    session_id CHAR(36) NOT NULL,
    winner VARCHAR(40) NOT NULL,
    placements_json JSON NOT NULL,
    final_round INT NOT NULL,
    completed_at DATETIME(6) NOT NULL,
    PRIMARY KEY (session_id),
    CONSTRAINT fk_game_results_session
        FOREIGN KEY (session_id) REFERENCES game_sessions (session_id)
        ON DELETE CASCADE,
    INDEX idx_game_results_completed (completed_at)
) ENGINE = InnoDB;
