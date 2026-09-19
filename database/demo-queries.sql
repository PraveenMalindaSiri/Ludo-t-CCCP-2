SELECT
    session_id,
    session_name,
    random_seed,
    status,
    created_at,
    started_at,
    completed_at,
    updated_at
FROM game_sessions
ORDER BY created_at DESC;

SELECT
    session_id,
    winner,
    JSON_EXTRACT(placements_json, '$.placements') AS placements,
    final_round,
    completed_at
FROM game_results
ORDER BY completed_at DESC;

SELECT status, COUNT(*) AS session_count
FROM game_sessions
GROUP BY status
ORDER BY status;
