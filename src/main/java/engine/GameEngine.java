package engine;

import block.Block;
import board.Board;
import board.Cell;
import config.GameConfig;
import engine.command.*;
import event.IGameEventListener;
import mystery.MysteryManager;
import piece.Piece;
import piece.state.*;
import player.Player;
import rules.*;
import dice.*;

import java.util.ArrayList;
import java.util.List;


/**
 * Has the complete game flow.
 * This will init the game state, determine first player, run  rounds and turns, execute moves.
 */
public class GameEngine {
    private final Board board;
    private final List<Player> players;
    private final TurnManager turnManager;
    private final RuleEngine ruleEngine;
    private final CaptureHandler captureHandler;
    private final BlockHandler blockHandler;
    private final MysteryManager mysteryManager;
    private final IDice dice;
    private final ICoinToss coinToss;
    private final List<IGameEventListener> listeners;
    private final GameConfig config;

    public GameEngine(Board board,
                      List<Player> players,
                      RuleEngine ruleEngine,
                      CaptureHandler captureHandler,
                      BlockHandler blockHandler,
                      MysteryManager mysteryManager,
                      IDice dice,
                      ICoinToss coinToss) {
        this.board = board;
        this.players = new ArrayList<>(players);
        this.turnManager = new TurnManager(this.players);
        this.ruleEngine = ruleEngine;
        this.captureHandler = captureHandler;
        this.blockHandler = blockHandler;
        this.mysteryManager = mysteryManager;
        this.dice = dice;
        this.coinToss = coinToss;
        this.listeners = new ArrayList<>();
        this.config = GameConfig.getInstance();
    }

    public void addEventListener(IGameEventListener listener) {
        listeners.add(listener);
    }

    // Start game --------------------------------------------------------------------------------------------------

    public void startGame() {
        initializePiecesInBase();
        firePlayerInfoEvents();
        determineFirstPlayer();

        List<Player> finishOrder = new ArrayList<>();

        while (finishOrder.size() < players.size() - 1) {
            playRound(finishOrder);
        }

        addRemainingPlayersToFinishOrder(finishOrder);
        fireFinalPlacements(finishOrder);
    }

    private void initializePiecesInBase() {
        for (Player player : players) {
            for (Piece piece : player.getPieces()) {
                board.getBaseCell(player.getColor()).addPiece(piece);
            }
        }
    }

    private void firePlayerInfoEvents() {
        for (Player player : players) {
            List<String> names = new ArrayList<>();
            for (Piece piece : player.getPieces()) {
                names.add(piece.getFullName());
            }
            firePlayerInfo(player.getColor(), names);
        }
    }

    // Get first player --------------------------------------------------------------------------------------------------

    private void determineFirstPlayer() {
        int highestRoll = -1;
        Player firstPlayer = null;

        for (Player player : players) {
            int roll = dice.roll();
            fireInitialRoll(player.getColor(), roll);
            if (roll > highestRoll) {
                highestRoll = roll;
                firstPlayer = player;
            }
        }

        fireFirstPlayer(firstPlayer.getColor());

        int startIndex = turnManager.getIndexOf(firstPlayer);
        turnManager.setPlayerOrder(startIndex);

        List<String> order = new ArrayList<>();
        for (int i = 0; i < players.size(); i++) {
            order.add(players.get((startIndex + i) % players.size()).getColor());
        }
        fireTurnOrder(order);
    }

    // Round --------------------------------------------------------------------------------------------------

    private void playRound(List<Player> finishOrder) {
        for (int i = 0; i < players.size(); i++) {
            Player player = turnManager.getNextPlayer();
            if (player.hasWon()) continue;

            playTurn(player);

            if (player.hasWon() && !finishOrder.contains(player)) {
                finishOrder.add(player);
                fireGameWon(player.getColor());
                if (finishOrder.size() == players.size()) return;
            }
        }

        // Mystery cell lifecycle
        boolean piecesOnPath = false;
        for (Player player : players) {
            for (Piece piece : player.getPieces()) {
                if (piece.isOnBoard() && !piece.isInHomeStraight()) {
                    piecesOnPath = true;
                    break;
                }
            }
        }

        int prevPos = mysteryManager.getPosition();
        boolean wasActive = mysteryManager.isActive();
        mysteryManager.updateRound(piecesOnPath);

        if (mysteryManager.isActive()
                && (!wasActive || mysteryManager.getPosition() != prevPos)) {
            fireMysteryCellSpawned(mysteryManager.getPosition(),
                    mysteryManager.getRoundsRemaining());
        }

        // Advance piece states (Energized/Sick/Frozen timers)
        for (Player player : players) {
            for (Piece piece : player.getPieces()) {
                piece.updateState();
            }
        }

        turnManager.incrementRound();
        fireRoundEnd(players);
    }

    // Turn controlling --------------------------------------------------------------------------------------------------

    // Runs single player turn with bonus rolls
    private void playTurn(Player player) {
        boolean keepRolling = true;

        while (keepRolling) {
            keepRolling = false;

            int diceValue = dice.roll();
            fireDiceRolled(player.getColor(), diceValue);

            for (Piece piece : player.getPieces()) {
                piece.notifyDiceRoll(diceValue);
            }

            handleFrozenTeleports(player);

            if (diceValue == config.getDiceSides()) {
                player.incrementConsecutiveSixes();
                if (ruleEngine.isThirdConsecutiveSix(player.getConsecutiveSixes())) {
                    handleTripleSixIfBlock(player);
                    player.resetConsecutiveSixes();
                    return;
                }
                keepRolling = true;
            } else {
                player.resetConsecutiveSixes();
            }

            boolean captured = false;
            List<Piece> validMoves = ruleEngine.getValidMoves(player, diceValue);

            if (diceValue == config.getDiceSides()
                    && !player.getPiecesInBase().isEmpty()
                    && player.shouldMoveFromBase(diceValue, board, ruleEngine)) {

                captured = executeBaseEntry(player, validMoves, diceValue);

            } else {
                validMoves.removeIf(Piece::isInBase);

                if (!validMoves.isEmpty()) {
                    Piece chosen = player.selectMove(
                            validMoves, diceValue, board, ruleEngine);

                    if (chosen != null) {
                        if (!chosen.isInBlock()
                                && !chosen.isInHomeStraight()
                                && !chosen.isInBase()) {

                            int blockPos = blockHandler
                                    .getFirstOpponentBlockPosition(chosen, diceValue);

                            if (blockPos != -1 && validMoves.size() > 1) {
                                for (Piece alt : validMoves) {
                                    if (alt == chosen) continue;
                                    if (alt.isInBase() || alt.isAtHome()) continue;

                                    if (alt.isInHomeStraight()) {
                                        chosen = alt;
                                        break;
                                    }

                                    int altBlockPos = blockHandler
                                            .getFirstOpponentBlockPosition(alt, diceValue);

                                    if (altBlockPos == -1) {
                                        chosen = alt;
                                        break;
                                    }
                                }
                            }
                        }

                        captured = executeMove(player, chosen, diceValue);
                    }
                }
            }

            if (captured) {
                keepRolling = true;
            }
        }
    }

    // Enter the base --------------------------------------------------------------------------------------------------

    private boolean executeBaseEntry(Player player,
                                     List<Piece> validMoves, int diceValue) {
        List<Piece> basePieces = new ArrayList<>();
        for (Piece p : validMoves) {
            if (p.isInBase()) basePieces.add(p);
        }
        if (basePieces.isEmpty()) return false;

        Piece piece = player.selectMove(basePieces, diceValue, board, ruleEngine);
        if (piece == null) piece = basePieces.getFirst();

        EnterBoardCommand cmd = new EnterBoardCommand(
                piece,
                board.getStartingCell(player.getColor()),
                board.getBaseCell(player.getColor()),
                coinToss
        );
        cmd.execute();

        int boardCount = player.getPiecesOnBoard().size();
        int baseCount = player.getPiecesInBase().size();
        firePieceEnteredBoard(player.getColor(), piece.getFullName(),
                boardCount, baseCount);

        boolean captured = resolveSinglePieceLanding(player, piece);

        if (!piece.isInBase()
                && !piece.isAtHome()
                && !piece.isInHomeStraight()
                && mysteryManager.isOnMysteryCell(piece.getPosition())) {
            if (handleMysteryLanding(player, piece)) {
                captured = true;
            }
        }

        return captured;
    }


    // Enter the Home Straight ----------------------------------------------------------------------------------

    private void executeHomeStraightEntry(Player player, Piece piece,
                                          int effective, Cell fromCell,
                                          int fromPos, int stepsOverApproach) {

        int stepsToHomeFromApproach = config.getHomePathLength() + 1;
        if (stepsOverApproach > stepsToHomeFromApproach) {
            return;
        }

        if (piece.isInBlock()) {
            Block block = blockHandler.findBlockAt(fromCell);
            if (block != null) {
                blockHandler.breakBlock(piece, block);
            }
        }

        int homeStraightIndex = stepsOverApproach - 1;

        fromCell.removePiece(piece);

        if (homeStraightIndex >= config.getHomePathLength()) {
            piece.moveToHome();
            board.getHomeCell(piece.getColor()).addPiece(piece);
        } else {
            piece.moveToHomeStraight(homeStraightIndex);
            board.getHomeStraightCell(
                    piece.getColor(), homeStraightIndex).addPiece(piece);
        }

        firePieceMoved(player.getColor(), piece.getFullName(),
                fromPos, piece.getPosition(), effective, piece.getDirection());
    }

    // Normal movement ---------------------------------------------------------------------------------------------

    private boolean executeMove(Player player, Piece piece, int diceValue) {
        if (piece.isInHomeStraight()) {
            executeHomeStraightMove(player, piece, diceValue);
            return false;
        }

        int fromPos = piece.getPosition();
        Cell fromCell = board.getCellAt(fromPos);
        int effective = piece.getEffectiveMovement(diceValue);


        if (piece.isInBlock()) {
            return executeBlockMove(player, piece, diceValue);
        }

        if (ruleEngine.canPassApproach(piece, diceValue)) {
            int stepsToApproach = blockHandler.distanceFromApproach(piece);
            int stepsOverApproach = effective - stepsToApproach;

            if (piece.getPosition() == board.getApproachPosition(piece.getColor())) {
                stepsOverApproach = effective;
            }

            if (stepsOverApproach > 0) {
                boolean canEnter = ruleEngine.canEnterHomeStraight(piece);

                if ("COUNTERCLOCKWISE".equals(piece.getDirection())) {
                    if (!ruleEngine.canEnterHomeStraightCCW(piece)) {
                        piece.setHasPassedApproachOnce(true);
                        canEnter = false;
                    }
                }

                if (canEnter) {
                    executeHomeStraightEntry(
                            player, piece, effective,
                            fromCell, fromPos, stepsOverApproach);
                    return false;
                }
            } else {
                if ("COUNTERCLOCKWISE".equals(piece.getDirection())
                        && !piece.getHasPassedApproachOnce()) {
                    piece.setHasPassedApproachOnce(true);
                }
            }
        }

        // Standard path -----------------------------------------------------------------
        int destination = ruleEngine.calculateDestination(piece, diceValue);

        int blockPos = blockHandler.getFirstOpponentBlockPosition(piece, diceValue);
        if (blockPos != -1) {
            return handleBlockedMove(
                    player, piece, diceValue, fromPos, blockPos, fromCell);
        }

        Piece capturedPiece = captureHandler.getCapturedPieceAt(
                destination, player.getColor());

        Block attackingBlock = piece.isInBlock()
                ? blockHandler.findBlockAt(fromCell) : null;
        Block defendingBlock = blockHandler.findBlockAt(
                board.getCellAt(destination));

        Cell toCell = board.getCellAt(destination);
        new MoveCommand(piece, fromCell, toCell, effective).execute();
        firePieceMoved(player.getColor(), piece.getFullName(),
                fromPos, destination, effective, piece.getDirection());

        boolean captured = false;
        if (capturedPiece != null) {
            Player capturedPlayer = getPlayerByColor(capturedPiece.getColor());
            new CaptureCommand(piece, capturedPiece, captureHandler).execute();

            int boardCount = capturedPlayer != null
                    ? capturedPlayer.getPiecesOnBoard().size() : 0;
            int baseCount = capturedPlayer != null
                    ? capturedPlayer.getPiecesInBase().size() : 0;

            firePieceCaptured(player.getColor(), piece.getFullName(),
                    destination,
                    capturedPiece.getColor(), capturedPiece.getFullName(),
                    boardCount, baseCount);
            captured = true;
        }

        if (!captured && attackingBlock != null && defendingBlock != null) {
            if (blockHandler.canBlockCaptureBlock(attackingBlock, defendingBlock)) {
                blockHandler.handleBlockCapture(attackingBlock, defendingBlock);
                captured = true;
            }
        }

        if (!captured) {
            checkAndFormBlock(piece, toCell);
        }

        if (mysteryManager.isOnMysteryCell(destination)) {
            if (handleMysteryLanding(player, piece)) {
                captured = true;
            }
        }

        return captured;
    }

    // handle Block moves
    private boolean executeBlockMove(Player player, Piece piece, int diceValue) {
        Cell pieceCell = board.getCellAt(piece.getPosition());
        Block attackingBlock = blockHandler.findBlockAt(pieceCell);

        if (attackingBlock == null) {
            piece.setInBlock(false);
            return false;
        }

        int oldPos = piece.getPosition();
        boolean captured = false;

        int destination = blockHandler.calculateBlockDestination(attackingBlock, diceValue);
        int firstBlockInPath = blockHandler.getFirstOpponentBlockPositionForBlock(attackingBlock, diceValue);

        Piece capturedByBlock = null;
        List<Piece> capturedDefendersByBlock = new ArrayList<>();

        if (firstBlockInPath == -1) {
            capturedByBlock = captureHandler.getCapturedPieceAt(destination, player.getColor());
        }

        if (firstBlockInPath != -1) {
            if (firstBlockInPath == destination) {
                Block defendingBlock = blockHandler.findBlockAt(board.getCellAt(destination));

                if (defendingBlock != null
                        && blockHandler.canBlockCaptureBlock(attackingBlock, defendingBlock)) {

                    capturedDefendersByBlock = new ArrayList<>(defendingBlock.getPieces());
                    blockHandler.handleBlockCapture(attackingBlock, defendingBlock);
                    captured = true;

                } else {
                    String blockingColor = board.getCellAt(destination).hasPieces()
                            ? board.getCellAt(destination).getPieces().getFirst().getColor() : "";
                    String blockingName = board.getCellAt(destination).hasPieces()
                            ? board.getCellAt(destination).getPieces().getFirst().getFullName() : "";

                    firePieceBlocked(player.getColor(), piece.getFullName(),
                            oldPos, destination, blockingColor, blockingName);
                    fireNoOtherPieces(player.getColor());
                    return false;
                }
            } else {
                String blockingColor = board.getCellAt(firstBlockInPath).hasPieces()
                        ? board.getCellAt(firstBlockInPath).getPieces().getFirst().getColor() : "";
                String blockingName = board.getCellAt(firstBlockInPath).hasPieces()
                        ? board.getCellAt(firstBlockInPath).getPieces().getFirst().getFullName() : "";

                firePieceBlocked(player.getColor(), piece.getFullName(),
                        oldPos, firstBlockInPath, blockingColor, blockingName);
                fireNoOtherPieces(player.getColor());
                return false;
            }
        }

        int blockMove = blockHandler.getBlockMovementAmount(attackingBlock, diceValue);
        blockHandler.moveBlock(attackingBlock, diceValue);

        int landedPos = piece.getPosition();

        firePieceMoved(player.getColor(), piece.getFullName(),
                oldPos, landedPos, blockMove, piece.getDirection());

        if (!capturedDefendersByBlock.isEmpty()) {
            for (Piece defender : capturedDefendersByBlock) {
                Player capturedPlayer = getPlayerByColor(defender.getColor());

                int boardCount = capturedPlayer != null
                        ? capturedPlayer.getPiecesOnBoard().size() : 0;
                int baseCount = capturedPlayer != null
                        ? capturedPlayer.getPiecesInBase().size() : 0;

                firePieceCaptured(player.getColor(), piece.getFullName(),
                        landedPos,
                        defender.getColor(), defender.getFullName(),
                        boardCount, baseCount);
            }
        }

        if (capturedByBlock != null) {
            Player capturedPlayer = getPlayerByColor(capturedByBlock.getColor());

            new CaptureCommand(piece, capturedByBlock, captureHandler).execute();

            for (Piece blockPiece : attackingBlock.getPieces()) {
                if (blockPiece != piece) {
                    blockPiece.incrementCaptureCount();
                }
            }

            int boardCount = capturedPlayer != null
                    ? capturedPlayer.getPiecesOnBoard().size() : 0;
            int baseCount = capturedPlayer != null
                    ? capturedPlayer.getPiecesInBase().size() : 0;

            firePieceCaptured(player.getColor(), piece.getFullName(),
                    landedPos,
                    capturedByBlock.getColor(), capturedByBlock.getFullName(),
                    boardCount, baseCount);
            captured = true;
        }

        if (!captured) {
            blockHandler.absorbSameColorPieces(attackingBlock);
        }

        if (mysteryManager.isOnMysteryCell(landedPos)) {
            if (handleMysteryLanding(player, piece)) {
                captured = true;
            }
        }

        return captured;
    }

    // Home straight movement --------------------------------------------------------------------------------------

    private void executeHomeStraightMove(Player player, Piece piece, int diceValue) {
        int fromIndex = piece.getHomeStraightIndex();
        int fromCellPos = piece.getPosition();
        int newIndex = ruleEngine.calculateHomeStraightDestination(piece, diceValue);

        board.getHomeStraightCell(piece.getColor(), fromIndex).removePiece(piece);

        if (newIndex >= config.getHomePathLength()) {
            piece.moveToHome();
            board.getHomeCell(piece.getColor()).addPiece(piece);
        } else {
            piece.moveToHomeStraight(newIndex);
            board.getHomeStraightCell(piece.getColor(), newIndex).addPiece(piece);
        }

        firePieceMoved(player.getColor(), piece.getFullName(),
                fromCellPos, piece.getPosition(),
                piece.getEffectiveMovement(diceValue), piece.getDirection());
    }

    // Block movement --------------------------------------------------------------------------------------------------

    private boolean handleBlockedMove(Player player, Piece piece, int diceValue,
                                      int fromPos, int destination, Cell fromCell) {
        Cell blockCell = board.getCellAt(destination);
        String blockingColor = blockCell.hasPieces()
                ? blockCell.getPieces().getFirst().getColor() : "";
        String blockingName = blockCell.hasPieces()
                ? blockCell.getPieces().getFirst().getFullName() : "";

        firePieceBlocked(player.getColor(), piece.getFullName(),
                fromPos, destination, blockingColor, blockingName);

        int maxMove = blockHandler.getMaxMoveBeforeBlock(piece, diceValue);

        if (maxMove < 0 || maxMove == fromPos) {
            fireNoOtherPieces(player.getColor());
            return false;
        }

        int steps = calculateStepsBetween(piece, fromPos, maxMove);
        Cell beforeBlock = board.getCellAt(maxMove);

        new MoveCommand(piece, fromCell, beforeBlock, steps).execute();

        fireMovedBeforeBlock(player.getColor(), piece.getFullName(), maxMove);

        boolean captured = resolveSinglePieceLanding(player, piece);

        if (!piece.isInBase()
                && !piece.isAtHome()
                && !piece.isInHomeStraight()
                && mysteryManager.isOnMysteryCell(piece.getPosition())) {
            if (handleMysteryLanding(player, piece)) {
                captured = true;
            }
        }

        return captured;
    }

    // Frozen tp --------------------------------------------------------------------------------------------------

    private void handleFrozenTeleports(Player player) {
        for (Piece piece : player.getPieces()) {
            IPieceState state = piece.getState();
            if (!(state instanceof FrozenState frozen)) continue;
            if (!frozen.shouldTeleportToBase()) continue;

            if (!piece.isInBase() && !piece.isAtHome()
                    && !piece.isInHomeStraight()) {

                Cell currentCell = board.getCellAt(piece.getPosition());

                if (piece.isInBlock()) {
                    Block block = blockHandler.findBlockAt(currentCell);
                    if (block != null) {
                        blockHandler.breakBlock(piece, block);
                    }
                }

                currentCell.removePiece(piece);
            }

            piece.capture();
            board.getBaseCell(piece.getColor()).addPiece(piece);
            frozen.resetTeleportFlag();

            fireTeleportEffect(player.getColor(), piece.getFullName(),
                    "is movement-restricted and has rolled three consecutively."
                            + " Teleporting piece " + piece.getFullName()
                            + " to base.");
        }
    }

    // Mystery cell --------------------------------------------------------------------------------------------------

    private boolean handleMysteryLanding(Player player, Piece piece) {
        if (piece.isInBlock() && !piece.isInHomeStraight() && !piece.isInBase()) {
            Cell oldCell = board.getCellAt(piece.getPosition());
            Block block = blockHandler.findBlockAt(oldCell);
            if (block != null) {
                blockHandler.breakBlock(piece, block);
            }
        }

        // MysteryManager removes the piece from the mystery cell and applies the teleport/effect.
        mysteryManager.handleLanding(piece);

        int effectIndex = mysteryManager.getLastEffectIndex();
        String destination = getEffectDestinationName(effectIndex);

        // Gamma with CCW piece actually ends at Beta, not Gamma.
        if (effectIndex == 2 && mysteryManager.isLastGammaCCWToBeta()) {
            destination = "Beta";
        }

        fireMysteryLanding(player.getColor(), piece.getFullName(), destination);

        switch (effectIndex) {
            case 0: // Alpha
                if (mysteryManager.isLastAlphaEnergized()) {
                    fireTeleportEffect(player.getColor(), piece.getFullName(),
                            "feels energized, and movement speed doubles.");
                } else {
                    fireTeleportEffect(player.getColor(), piece.getFullName(),
                            "feels sick, and movement speed halves.");
                }
                break;

            case 1: // Beta
                fireTeleportEffect(player.getColor(), piece.getFullName(),
                        "attends briefing and cannot move for four rounds.");
                break;

            case 2: // Gamma
                if (mysteryManager.isLastGammaCCWToBeta()) {
                    fireDirectionChanged(player.getColor(), piece.getFullName(),
                            "COUNTERCLOCKWISE", "CLOCKWISE");
                } else {
                    fireDirectionChanged(player.getColor(), piece.getFullName(),
                            "CLOCKWISE", "COUNTERCLOCKWISE");
                }
                break;

            default:
                break;
        }

        if (piece.isInBase() || piece.isAtHome() || piece.isInHomeStraight()) {
            return false;
        }

        int newPos = piece.getPosition();
        if (newPos < 0 || newPos >= config.getStandardCellCount()) {
            return false;
        }

        Cell newCell = board.getCellAt(newPos);

        // If teleported onto an existing block, a single piece cannot capture it.
        Block destBlock = blockHandler.findBlockAt(newCell);
        if (destBlock != null && !destBlock.getPieces().isEmpty()) {
            String destColor = destBlock.getPieces().getFirst().getColor();

            if (!destColor.equalsIgnoreCase(player.getColor())) {
                // Cannot land on an opponent block, send the teleported piece back to base.
                newCell.removePiece(piece);
                piece.capture();
                board.getBaseCell(piece.getColor()).addPiece(piece);
                return false;
            }

            // Same-color block: only normal standard-path pieces are allowed to join.
            if (blockHandler.canBeInBlock(piece)) {
                blockHandler.addToBlock(piece, destBlock, newCell);
            } else {
                // Avoid illegal same-color stacking outside the block.
                newCell.removePiece(piece);
                piece.capture();
                board.getBaseCell(piece.getColor()).addPiece(piece);
            }
            return false;
        }

        Piece capturedPiece = findSingleOpponentIgnoringMover(piece, newCell);
        if (capturedPiece != null) {
            Player capturedPlayer = getPlayerByColor(capturedPiece.getColor());
            new CaptureCommand(piece, capturedPiece, captureHandler).execute();

            int boardCount = capturedPlayer != null
                    ? capturedPlayer.getPiecesOnBoard().size() : 0;
            int baseCount = capturedPlayer != null
                    ? capturedPlayer.getPiecesInBase().size() : 0;

            firePieceCaptured(player.getColor(), piece.getFullName(),
                    newPos,
                    capturedPiece.getColor(), capturedPiece.getFullName(),
                    boardCount, baseCount);
            return true;
        }

        // Same-color single piece/block formation after teleport.
        if (newCell.getPieces().size() > 1) {
            boolean hasSameColor = false;

            for (Piece other : newCell.getPieces()) {
                if (other != piece
                        && other.getColor().equalsIgnoreCase(piece.getColor())) {
                    hasSameColor = true;
                    break;
                }
            }

            if (hasSameColor && !blockHandler.canBeInBlock(piece)) {
                newCell.removePiece(piece);
                piece.capture();
                board.getBaseCell(piece.getColor()).addPiece(piece);
                return false;
            }
        }

        checkAndFormBlock(piece, newCell);
        return false;
    }

    private String getEffectDestinationName(int effectIndex) {
        return switch (effectIndex) {
            case 0 -> "Alpha";
            case 1 -> "Beta";
            case 2 -> "Gamma";
            case 3 -> "Base";
            case 4 -> "X";
            case 5 -> "Approach";
            default -> "Unknown";
        };
    }

    // Turn controlling --------------------------------------------------------------------------------------------------

    private void handleTripleSixIfBlock(Player player) {
        boolean hasBlock = false;

        for (Block block : blockHandler.getActiveBlocks().values()) {
            if (!block.getPieces().isEmpty()
                    && block.getPieces().getFirst().getColor()
                    .equalsIgnoreCase(player.getColor())) {
                hasBlock = true;
                break;
            }
        }

        if (hasBlock) {
            List<Piece> movedPieces = blockHandler.handleTripleSixBlockBreak(player);

            for (Piece movedPiece : movedPieces) {
                resolveSinglePieceLanding(player, movedPiece);

                if (!movedPiece.isInBase()
                        && !movedPiece.isAtHome()
                        && !movedPiece.isInHomeStraight()
                        && mysteryManager.isOnMysteryCell(movedPiece.getPosition())) {
                    handleMysteryLanding(player, movedPiece);
                }
            }
        }
    }

    private void checkAndFormBlock(Piece piece, Cell cell) {
        List<Piece> piecesOnCell = cell.getPieces();
        if (piecesOnCell.size() < 2) return;

        if (!blockHandler.canBeInBlock(piece)) return;

        boolean allSameColor = true;
        for (Piece p : piecesOnCell) {
            if (!p.getColor().equalsIgnoreCase(piece.getColor())) {
                allSameColor = false;
                break;
            }
        }
        if (!allSameColor) return;

        Block existing = blockHandler.findBlockAt(cell);
        if (existing == null) {
            Piece other = null;
            for (Piece p : piecesOnCell) {
                if (p != piece) {
                    other = p;
                    break;
                }
            }
            // createBlock already checks canBeInBlock on both pieces
            if (other != null) {
                blockHandler.createBlock(piece, other, cell);
            }
        } else {
            // addToBlock already checks canBeInBlock
            blockHandler.addToBlock(piece, existing, cell);
        }
    }

    // util --------------------------------------------------------------------------------------------------

    private void addRemainingPlayersToFinishOrder(List<Player> finishOrder) {
        for (Player player : players) {
            if (!finishOrder.contains(player)) {
                finishOrder.add(player);
            }
        }
    }

    private void fireFinalPlacements(List<Player> finishOrder) {
        List<Player> snapshot = new ArrayList<>(finishOrder);

        for (IGameEventListener l : listeners) {
            l.onFinalPlacements(snapshot);
        }
    }

    private boolean resolveSinglePieceLanding(Player player, Piece piece) {
        if (piece.isInBase() || piece.isAtHome() || piece.isInHomeStraight()) {
            return false;
        }

        if (piece.getPosition() < 0
                || piece.getPosition() >= config.getStandardCellCount()) {
            return false;
        }

        Cell cell = board.getCellAt(piece.getPosition());

        Block existingBlock = blockHandler.findBlockAt(cell);
        if (existingBlock != null && !existingBlock.getPieces().isEmpty()) {
            String blockColor = existingBlock.getPieces().getFirst().getColor();

            if (!blockColor.equalsIgnoreCase(piece.getColor())) {
                cell.removePiece(piece);
                piece.capture();
                board.getBaseCell(piece.getColor()).addPiece(piece);
                return false;
            }

            if (blockHandler.canBeInBlock(piece)) {
                blockHandler.addToBlock(piece, existingBlock, cell);
            } else {
                cell.removePiece(piece);
                piece.capture();
                board.getBaseCell(piece.getColor()).addPiece(piece);
            }
            return false;
        }

        Piece capturedPiece = findSingleOpponentIgnoringMover(piece, cell);
        if (capturedPiece != null) {
            Player capturedPlayer = getPlayerByColor(capturedPiece.getColor());

            new CaptureCommand(piece, capturedPiece, captureHandler).execute();

            int boardCount = capturedPlayer != null
                    ? capturedPlayer.getPiecesOnBoard().size() : 0;
            int baseCount = capturedPlayer != null
                    ? capturedPlayer.getPiecesInBase().size() : 0;

            firePieceCaptured(player.getColor(), piece.getFullName(),
                    piece.getPosition(),
                    capturedPiece.getColor(), capturedPiece.getFullName(),
                    boardCount, baseCount);
            return true;
        }

        if (cell.getPieces().size() > 1) {
            boolean hasSameColor = false;
            for (Piece other : cell.getPieces()) {
                if (other != piece
                        && other.getColor().equalsIgnoreCase(piece.getColor())) {
                    hasSameColor = true;
                    break;
                }
            }

            if (hasSameColor && !blockHandler.canBeInBlock(piece)) {
                cell.removePiece(piece);
                piece.capture();
                board.getBaseCell(piece.getColor()).addPiece(piece);
                return false;
            }
        }

        checkAndFormBlock(piece, cell);
        return false;
    }

    private Piece findSingleOpponentIgnoringMover(Piece mover, Cell cell) {
        Piece opponent = null;
        int opponentCount = 0;

        for (Piece other : cell.getPieces()) {
            if (other == mover) continue;
            if (!other.isOnBoard() || other.getPosition() != cell.getPosition()) continue;

            if (!other.getColor().equalsIgnoreCase(mover.getColor())) {
                opponent = other;
                opponentCount++;
            }
        }

        return opponentCount == 1 ? opponent : null;
    }

    private int calculateStepsBetween(Piece piece, int fromPos, int toPos) {
        int count = config.getStandardCellCount();
        if ("CLOCKWISE".equals(piece.getDirection())) {
            return (toPos - fromPos + count) % count;
        } else {
            return (fromPos - toPos + count) % count;
        }
    }

    private Player getPlayerByColor(String color) {
        for (Player player : players) {
            if (player.getColor().equalsIgnoreCase(color)) return player;
        }
        return null;
    }

    // Events --------------------------------------------------------------------------------------------------

    private void firePlayerInfo(String color, List<String> names) {
        for (IGameEventListener l : listeners) l.onPlayerInfo(color, names);
    }

    private void fireInitialRoll(String color, int value) {
        for (IGameEventListener l : listeners) l.onInitialRoll(color, value);
    }

    private void fireDiceRolled(String color, int value) {
        for (IGameEventListener l : listeners) l.onDiceRolled(color, value);
    }

    private void fireFirstPlayer(String color) {
        for (IGameEventListener l : listeners) l.onFirstPlayer(color);
    }

    private void fireTurnOrder(List<String> colors) {
        for (IGameEventListener l : listeners) l.onTurnOrder(colors);
    }

    private void firePieceEnteredBoard(String color, String pieceName,
                                       int boardCount, int baseCount) {
        for (IGameEventListener l : listeners)
            l.onPieceEnteredBoard(color, pieceName, boardCount, baseCount);
    }

    private void firePieceMoved(String color, String pieceName,
                                int from, int to, int value, String direction) {
        for (IGameEventListener l : listeners)
            l.onPieceMoved(color, pieceName, from, to, value, direction);
    }

    private void firePieceBlocked(String color, String pieceName,
                                  int from, int to,
                                  String blockingColor, String blockingName) {
        for (IGameEventListener l : listeners)
            l.onPieceBlocked(color, pieceName, from, to,
                    blockingColor, blockingName);
    }

    private void fireNoOtherPieces(String color) {
        for (IGameEventListener l : listeners) l.onNoOtherPieces(color);
    }

    private void fireMovedBeforeBlock(String color, String pieceName,
                                      int stoppedAt) {
        for (IGameEventListener l : listeners)
            l.onMovedBeforeBlock(color, pieceName, stoppedAt);
    }

    private void firePieceCaptured(String capturerColor, String capturerName,
                                   int cell,
                                   String capturedColor, String capturedName,
                                   int boardCount, int baseCount) {
        for (IGameEventListener l : listeners)
            l.onPieceCaptured(capturerColor, capturerName, cell,
                    capturedColor, capturedName, boardCount, baseCount);
    }

    private void fireMysteryLanding(String color, String pieceName,
                                    String destination) {
        for (IGameEventListener l : listeners)
            l.onMysteryLanding(color, pieceName, destination);
    }

    private void fireTeleportEffect(String color, String pieceName,
                                    String effect) {
        for (IGameEventListener l : listeners)
            l.onTeleportEffect(color, pieceName, effect);
    }

    private void fireDirectionChanged(String color, String pieceName,
                                      String oldDir, String newDir) {
        for (IGameEventListener l : listeners)
            l.onDirectionChanged(color, pieceName, oldDir, newDir);
    }

    private void fireMysteryCellSpawned(int position, int duration) {
        for (IGameEventListener l : listeners)
            l.onMysteryCellSpawned(position, duration);
    }

    private void fireRoundEnd(List<Player> players) {
        for (IGameEventListener l : listeners) l.onRoundEnd(players);
    }

    private void fireGameWon(String color) {
        for (IGameEventListener l : listeners) l.onGameWon(color);
    }

}
