package client.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import protocol.GameSnapshotDto;
import protocol.SessionStatus;

/** Test-source-only visual board preview. It is not a production local-engine mode. */
public final class BoardPreviewMain {

    private BoardPreviewMain() {}

    public static void main(String[] args) {
        AppTheme.install();
        SwingUtilities.invokeLater(
                () -> {
                    GamePanel panel = new GamePanel();
                    panel.render(
                            snapshot(),
                            List.of(
                                    "Preview data uses protocol DTOs only.",
                                    "No game rules run in the client."),
                            "Blue moved B1");
                    JFrame frame = new JFrame("LUDO-T Board DTO Preview");
                    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    frame.setContentPane(panel);
                    frame.setSize(1120, 820);
                    frame.setLocationRelativeTo(null);
                    frame.setVisible(true);
                });
    }

    private static GameSnapshotDto snapshot() {
        List<GameSnapshotDto.PlayerDto> players = new ArrayList<>();
        String[] colors = {"Red", "Green", "Yellow", "Blue"};
        for (int colorIndex = 0; colorIndex < colors.length; colorIndex++) {
            String color = colors[colorIndex];
            List<GameSnapshotDto.PieceDto> pieces = new ArrayList<>();
            for (int pieceIndex = 0; pieceIndex < 4; pieceIndex++) {
                String name = color.substring(0, 1) + (pieceIndex + 1);
                String position =
                        pieceIndex == 0
                                ? "CELL_" + startingPosition(color)
                                : pieceIndex == 1
                                        ? "HOME_STRAIGHT_" + colorIndex
                                        : pieceIndex == 2 ? "HOME" : "BASE";
                pieces.add(
                        new GameSnapshotDto.PieceDto(
                                name, color + " piece " + (pieceIndex + 1), position));
            }
            players.add(new GameSnapshotDto.PlayerDto(color, 3, 1, pieces));
        }
        return new GameSnapshotDto(
                UUID.randomUUID(),
                42,
                9,
                SessionStatus.PAUSED,
                players,
                new GameSnapshotDto.MysteryDto(true, 20, 3),
                "Blue",
                2,
                "Blue moved B1");
    }

    private static int startingPosition(String color) {
        return switch (color.toUpperCase()) {
            case "YELLOW" -> 0;
            case "BLUE" -> 13;
            case "RED" -> 26;
            case "GREEN" -> 39;
            default -> throw new IllegalArgumentException("Unknown player color " + color);
        };
    }
}
