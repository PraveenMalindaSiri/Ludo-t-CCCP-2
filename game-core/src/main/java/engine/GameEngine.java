package engine;

import block.Block;
import board.Board;
import board.Cell;
import config.GameConfig;
import dice.ICoinToss;
import dice.IDice;
import engine.command.BlockMoveCommand;
import engine.command.CommandResult;
import engine.command.EnterBoardCommand;
import engine.command.HomeMoveCommand;
import engine.command.ICommand;
import engine.command.MoveCommand;
import event.GameEventPublisher;
import event.GameSnapshot;
import event.IGameEventListener;
import java.util.ArrayList;
import java.util.List;
import mystery.MysteryManager;
import piece.Piece;
import player.Player;
import rules.BlockHandler;
import rules.CaptureHandler;
import rules.LandingResolver;
import rules.RuleEngine;

/**
 * Coordinates game flow while delegating rules and mutations to the existing pattern participants.
 */
public class GameEngine {
    private final Board board;
    private final List<Player> players;
    private final TurnManager turnManager;
    private final RuleEngine ruleEngine;
    private final CaptureHandler captureHandler;
    private final BlockHandler blockHandler;
    private final MysteryManager mysteryManager;
    private final LandingResolver landingResolver;
    private final IDice dice;
    private final ICoinToss coinToss;
    private final GameEventPublisher events;
    private final GameConfig config;
    private final List<Player> finishOrder;

    private boolean initialized;
    private boolean completed;
    private int turnsInCurrentRound;

    public GameEngine(
            Board board,
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
        this.landingResolver =
                new LandingResolver(board, captureHandler, blockHandler, mysteryManager);
        this.dice = dice;
        this.coinToss = coinToss;
        this.events = new GameEventPublisher();
        this.config = GameConfig.getInstance();
        this.finishOrder = new ArrayList<>();
    }

    public void addEventListener(IGameEventListener listener) {
        events.addListener(listener);
    }

    public void removeEventListener(IGameEventListener listener) {
        events.removeListener(listener);
    }

    /** Runs the original automatic simulation to completion. */
    public void startGame() {
        initializeGame();
        while (advanceOneTurn()) {
            // CLI mode intentionally continues until the game is complete.
        }
    }

    /** Initializes a session without forcing the complete simulation to run. */
    public void initializeGame() {
        if (initialized) return;
        for (Player player : players) {
            for (Piece piece : player.getPieces()) board.initializeInBase(piece);
        }
        firePlayerInfoEvents();
        determineFirstPlayer();
        initialized = true;
    }

    /** Advances one player's turn for future GUI/server control. */
    public boolean advanceOneTurn() {
        initializeGame();
        if (completed) return false;

        Player player = turnManager.getNextPlayer();
        if (!player.hasWon()) {
            playTurn(player);
            recordWinnerIfNeeded(player);
        }

        turnsInCurrentRound++;
        if (turnsInCurrentRound >= players.size()) {
            turnsInCurrentRound = 0;
            completeRound();
            if (finishOrder.size() >= players.size() - 1) {
                addRemainingPlayersToFinishOrder();
                publishFinalPlacements();
                completed = true;
            }
        }
        return !completed;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public boolean isCompleted() {
        return completed;
    }

    public int getRoundCount() {
        return turnManager.getRoundCount();
    }

    public GameSnapshot getSnapshot() {
        List<String> placements = new ArrayList<>();
        for (Player player : finishOrder) placements.add(player.getColor());
        return GameSnapshot.from(
                turnManager.getRoundCount(),
                turnManager.getCurrentPlayer().getColor(),
                players,
                mysteryManager,
                placements);
    }

    private void firePlayerInfoEvents() {
        for (Player player : players) {
            List<String> names = new ArrayList<>();
            for (Piece piece : player.getPieces()) names.add(piece.getFullName());
            events.playerInfo(player.getColor(), names);
        }
    }

    private void determineFirstPlayer() {
        int highestRoll = -1;
        Player firstPlayer = null;
        for (Player player : players) {
            int roll = dice.roll();
            events.initialRoll(player.getColor(), roll);
            if (roll > highestRoll) {
                highestRoll = roll;
                firstPlayer = player;
            }
        }
        if (firstPlayer == null) {
            throw new IllegalStateException("A game requires at least one player.");
        }

        events.firstPlayer(firstPlayer.getColor());
        int startIndex = turnManager.getIndexOf(firstPlayer);
        turnManager.setPlayerOrder(startIndex);
        List<String> order = new ArrayList<>();
        for (int i = 0; i < players.size(); i++) {
            order.add(players.get((startIndex + i) % players.size()).getColor());
        }
        events.turnOrder(order);
    }

    private void completeRound() {
        boolean piecesOnPath = false;
        for (Player player : players) {
            for (Piece piece : player.getPieces()) {
                if (piece.isOnBoard() && !piece.isInHomeStraight()) {
                    piecesOnPath = true;
                    break;
                }
            }
            if (piecesOnPath) break;
        }

        int previousPosition = mysteryManager.getPosition();
        boolean wasActive = mysteryManager.isActive();
        mysteryManager.updateRound(piecesOnPath);
        if (mysteryManager.isActive()
                && (!wasActive || mysteryManager.getPosition() != previousPosition)) {
            events.mysteryCellSpawned(
                    mysteryManager.getPosition(), mysteryManager.getRoundsRemaining());
        }

        for (Player player : players) {
            for (Piece piece : player.getPieces()) piece.updateState();
        }
        turnManager.incrementRound();
        events.roundEnd(getSnapshot());
    }

    private void playTurn(Player player) {
        boolean keepRolling = true;
        while (keepRolling) {
            keepRolling = false;
            int diceValue = dice.roll();
            events.diceRolled(player.getColor(), diceValue);

            for (Piece piece : player.getPieces()) piece.notifyDiceRoll(diceValue);
            handleStateRequests(player);

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

            List<Piece> validMoves = ruleEngine.getValidMoves(player, diceValue);
            boolean captured = false;
            if (diceValue == config.getDiceSides()
                    && !player.getPiecesInBase().isEmpty()
                    && player.shouldMoveFromBase(diceValue, board, ruleEngine)) {
                captured = executeBaseEntry(player, validMoves, diceValue);
            } else {
                validMoves.removeIf(Piece::isInBase);
                Piece chosen = player.selectMove(validMoves, diceValue, board, ruleEngine);
                if (chosen != null) captured = executeMove(player, chosen, diceValue);
            }
            if (captured) keepRolling = true;
        }
    }

    private boolean executeBaseEntry(Player player, List<Piece> validMoves, int diceValue) {
        List<Piece> basePieces = new ArrayList<>();
        for (Piece piece : validMoves) if (piece.isInBase()) basePieces.add(piece);
        if (basePieces.isEmpty()) return false;

        Piece piece = player.selectMove(basePieces, diceValue, board, ruleEngine);
        if (piece == null) piece = basePieces.getFirst();
        CommandResult result =
                executeCommand(new EnterBoardCommand(piece, board, coinToss, landingResolver));

        events.pieceEnteredBoard(
                player.getColor(),
                piece.getFullName(),
                player.getPiecesOnBoard().size(),
                player.getPiecesInBase().size());
        publishLandingEvents(player, piece, result);
        return result.hasCaptured();
    }

    private boolean executeMove(Player player, Piece piece, int diceValue) {
        if (piece.isInHomeStraight()) {
            int newIndex = ruleEngine.calculateHomeStraightDestination(piece, diceValue);
            CommandResult result =
                    executeCommand(
                            new HomeMoveCommand(
                                    piece, board, newIndex, piece.getEffectiveMovement(diceValue)));
            publishMovement(player, piece, result);
            return false;
        }
        if (piece.isInBlock()) return executeBlockMove(player, piece, diceValue);

        int effective = piece.getEffectiveMovement(diceValue);
        if (tryEnterHomeStraight(player, piece, diceValue, effective)) return false;

        int fromPosition = piece.getPosition();
        int blockPosition = blockHandler.getFirstOpponentBlockPosition(piece, diceValue);
        if (blockPosition != -1) {
            return executePartialBlockedMove(player, piece, diceValue, fromPosition, blockPosition);
        }

        int destination = ruleEngine.calculateDestination(piece, diceValue);
        CommandResult result =
                executeCommand(
                        new MoveCommand(piece, board, destination, effective, landingResolver));
        publishMovement(player, piece, result);
        publishLandingEvents(player, piece, result);
        return result.hasCaptured();
    }

    private boolean tryEnterHomeStraight(Player player, Piece piece, int diceValue, int effective) {
        if (!ruleEngine.canPassApproach(piece, diceValue)) return false;
        int stepsToApproach = blockHandler.distanceFromApproach(piece);
        int stepsOverApproach = effective - stepsToApproach;
        if (piece.getPosition() == board.getApproachPosition(piece.getColor())) {
            stepsOverApproach = effective;
        }

        if (stepsOverApproach <= 0) {
            if ("COUNTERCLOCKWISE".equals(piece.getDirection())
                    && !piece.getHasPassedApproachOnce()) {
                piece.setHasPassedApproachOnce(true);
            }
            return false;
        }

        boolean canEnter = ruleEngine.canEnterHomeStraight(piece);
        if ("COUNTERCLOCKWISE".equals(piece.getDirection())
                && !ruleEngine.canEnterHomeStraightCCW(piece)) {
            piece.setHasPassedApproachOnce(true);
            canEnter = false;
        }
        if (!canEnter) return false;

        int destinationIndex = stepsOverApproach - 1;
        CommandResult result =
                executeCommand(new HomeMoveCommand(piece, board, destinationIndex, effective));
        publishMovement(player, piece, result);
        return true;
    }

    private boolean executeBlockMove(Player player, Piece chosen, int diceValue) {
        Block block = blockHandler.findBlockAt(board.getCellAt(chosen.getPosition()));
        if (block == null) {
            chosen.setInBlock(false);
            return false;
        }

        CommandResult result =
                executeCommand(
                        new BlockMoveCommand(
                                block,
                                diceValue,
                                board,
                                blockHandler,
                                captureHandler,
                                landingResolver));
        if (result.wasBlocked()) {
            fireBlockedResult(player, chosen, result);
            return false;
        }
        if (!result.wasExecuted()) return false;
        publishMovement(player, chosen, result);
        publishLandingEvents(player, chosen, result);
        return result.hasCaptured();
    }

    private boolean executePartialBlockedMove(
            Player player, Piece piece, int diceValue, int fromPosition, int blockPosition) {
        Cell blockingCell = board.getCellAt(blockPosition);
        String blockingColor =
                blockingCell.hasPieces() ? blockingCell.getPieces().getFirst().getColor() : "";
        String blockingName =
                blockingCell.hasPieces() ? blockingCell.getPieces().getFirst().getFullName() : "";
        events.pieceBlocked(
                player.getColor(),
                piece.getFullName(),
                fromPosition,
                blockPosition,
                blockingColor,
                blockingName);

        int stopPosition = blockHandler.getMaxMoveBeforeBlock(piece, diceValue);
        if (stopPosition < 0 || stopPosition == fromPosition) {
            events.noOtherPieces(player.getColor());
            return false;
        }

        int steps = calculateStepsBetween(piece, fromPosition, stopPosition);
        CommandResult result =
                executeCommand(new MoveCommand(piece, board, stopPosition, steps, landingResolver));
        result.markMovedBeforeBlock();
        events.movedBeforeBlock(player.getColor(), piece.getFullName(), stopPosition);
        publishLandingEvents(player, piece, result);
        return result.hasCaptured();
    }

    private void handleStateRequests(Player player) {
        for (Piece piece : player.getPieces()) {
            if (!piece.shouldTeleportToBase()) continue;
            if (piece.isInBlock() && !piece.isInHomeStraight() && !piece.isInBase()) {
                Block block = blockHandler.findBlockAt(board.getCellAt(piece.getPosition()));
                if (block != null) blockHandler.breakBlock(piece, block);
            }
            piece.markTeleportHandled();
            board.sendToBase(piece);
            events.stateTeleportToBase(player.getColor(), piece.getFullName());
        }
    }

    private void handleTripleSixIfBlock(Player player) {
        List<Piece> movedPieces = blockHandler.handleTripleSixBlockBreak(player);
        for (Piece movedPiece : movedPieces) {
            CommandResult result = new CommandResult(CommandResult.Type.MOVE);
            result.addMovedPiece(movedPiece);
            landingResolver.resolve(movedPiece, result);
            publishLandingEvents(player, movedPiece, result);
        }
    }

    private CommandResult executeCommand(ICommand command) {
        if (command == null) throw new IllegalArgumentException("Command cannot be null.");
        return command.execute();
    }

    private void publishMovement(Player player, Piece piece, CommandResult result) {
        if (!result.wasExecuted()) return;
        events.pieceMoved(
                player.getColor(),
                piece.getFullName(),
                result.getFromPosition(),
                result.getToPosition(),
                result.getMovement(),
                result.getDirection());
    }

    private void publishLandingEvents(Player player, Piece actor, CommandResult result) {
        for (Piece captured : result.getCapturedPieces()) {
            Player capturedPlayer = getPlayerByColor(captured.getColor());
            int boardCount = capturedPlayer != null ? capturedPlayer.getPiecesOnBoard().size() : 0;
            int baseCount = capturedPlayer != null ? capturedPlayer.getPiecesInBase().size() : 0;
            events.pieceCaptured(
                    player.getColor(),
                    actor.getFullName(),
                    result.getCapturePosition(captured),
                    captured.getColor(),
                    captured.getFullName(),
                    boardCount,
                    baseCount);
        }
        if (result.getMysteryOutcome() != null) {
            events.mysteryResolved(
                    player.getColor(), actor.getFullName(), result.getMysteryOutcome());
        }
    }

    private void fireBlockedResult(Player player, Piece piece, CommandResult result) {
        Cell blockingCell = board.getCellAt(result.getBlockedAt());
        String color =
                blockingCell.hasPieces() ? blockingCell.getPieces().getFirst().getColor() : "";
        String name =
                blockingCell.hasPieces() ? blockingCell.getPieces().getFirst().getFullName() : "";
        events.pieceBlocked(
                player.getColor(),
                piece.getFullName(),
                piece.getPosition(),
                result.getBlockedAt(),
                color,
                name);
        events.noOtherPieces(player.getColor());
    }

    private void recordWinnerIfNeeded(Player player) {
        if (player.hasWon() && !finishOrder.contains(player)) {
            finishOrder.add(player);
            events.gameWon(player.getColor());
        }
    }

    private void addRemainingPlayersToFinishOrder() {
        for (Player player : players) {
            if (!finishOrder.contains(player)) finishOrder.add(player);
        }
    }

    private int calculateStepsBetween(Piece piece, int from, int to) {
        int count = config.getStandardCellCount();
        return "CLOCKWISE".equals(piece.getDirection())
                ? (to - from + count) % count
                : (from - to + count) % count;
    }

    private Player getPlayerByColor(String color) {
        for (Player player : players) {
            if (player.getColor().equalsIgnoreCase(color)) return player;
        }
        return null;
    }

    private void publishFinalPlacements() {
        List<String> colors = new ArrayList<>();
        for (Player player : finishOrder) colors.add(player.getColor());
        events.finalPlacements(colors);
    }
}
