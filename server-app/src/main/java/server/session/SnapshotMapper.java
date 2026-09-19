package server.session;

import event.GameSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import protocol.GameSnapshotDto;
import protocol.SessionStatus;

/** Maps immutable game-core state and immutable session metadata to the shared protocol DTO. */
public final class SnapshotMapper {

    public GameSnapshotDto map(GameSnapshot core, SessionMetadata metadata) {
        Objects.requireNonNull(core, "core");
        Objects.requireNonNull(metadata, "metadata");
        List<GameSnapshotDto.PlayerDto> players = new ArrayList<>();
        for (GameSnapshot.PlayerView player : core.getPlayers()) {
            List<GameSnapshotDto.PieceDto> pieces = new ArrayList<>();
            for (GameSnapshot.PieceView piece : player.getPieces()) {
                pieces.add(
                        new GameSnapshotDto.PieceDto(
                                piece.getPieceId(),
                                piece.getName(),
                                piece.getFullName(),
                                piece.getColor(),
                                GameSnapshotDto.PieceArea.valueOf(piece.getArea().name()),
                                piece.getStandardPosition(),
                                piece.getHomeStraightIndex(),
                                piece.getDirection(),
                                piece.getStateLabel(),
                                piece.getStateRoundsRemaining(),
                                piece.isInBlock(),
                                piece.getBlockId(),
                                piece.getPosition()));
            }
            players.add(
                    new GameSnapshotDto.PlayerDto(
                            player.getColor(),
                            player.getBoardCount(),
                            player.getBaseCount(),
                            player.getHomeCount(),
                            pieces));
        }
        return new GameSnapshotDto(
                metadata.sessionId(),
                metadata.version(),
                core.getRound(),
                metadata.status(),
                players,
                new GameSnapshotDto.MysteryDto(
                        core.isMysteryActive(),
                        core.getMysteryPosition(),
                        core.getMysteryRoundsRemaining()),
                core.getCurrentPlayer(),
                metadata.turn(),
                metadata.lastAction(),
                metadata.turnDelayMillis(),
                metadata.lastDice(),
                metadata.lastResult(),
                core.getFinalPlacements(),
                metadata.queueDepth());
    }

    public record SessionMetadata(
            UUID sessionId,
            long version,
            SessionStatus status,
            long turn,
            long turnDelayMillis,
            String lastAction,
            Integer lastDice,
            String lastResult,
            int queueDepth) {

        public SessionMetadata {
            Objects.requireNonNull(sessionId, "sessionId");
            Objects.requireNonNull(status, "status");
            lastAction = lastAction == null ? "" : lastAction;
            lastResult = lastResult == null ? "" : lastResult;
        }
    }
}
