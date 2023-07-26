import model.*;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

@ServerEndpoint(value = "/quarto/api", encoders = MessageEncoder.class, decoders = MessageDecoder.class)
public class Endpoint {

    private static Game game = new Game();

    @OnOpen
    public void onOpen(Session session) {
        System.out.println("Connection openned successfully with session id: " + session.getId());
    }

    @OnMessage
    public void onMessage(Session session, MessageRequest request) throws IOException, EncodeException {
        switch (request.getConnectionType()) {
            case ENTER_GAME -> handleEnterGameRequest(session);
            case SET_PIECE -> handleSetOpponentPieceRequest(session, request);
            case PLACE_ON_BOARD -> handlePerformPlayRequest(session, request);
        }
    }

    private void handleEnterGameRequest(Session session) throws EncodeException, IOException {
        if (game.getStatus() != GameStatus.PREPARING) return;

        game.addPlayer(session);

        final int numberOfPlayers = game.getPlayers().size();

        final Player assignedPlayer = numberOfPlayers == 1 ? Player.PLAYER_1 : Player.PLAYER_2;

        session.getBasicRemote().sendObject(new MessageResponse(assignedPlayer));

        if (numberOfPlayers < 2) return;

        game.start();

        final MessageResponse response = new MessageResponse(game.getBoard(), game.getBuffer(), game.getTurn());
        sendResponseForAllPlayers(response);
    }

    private void handleSetOpponentPieceRequest(Session session, MessageRequest request) throws EncodeException, IOException {
        if (getRequesterPlayer(session) != game.getTurn()) return;

        game.setRoundPiece(request.getPieceCoordinate());

        game.switchTurn();

        final MessageResponse response = new MessageResponse(game.getTurn(), game.getTurnPieceCoordinate());
        sendResponseForAllPlayers(response);
    }

    private void sendResponseForAllPlayers(MessageResponse response) throws EncodeException, IOException {
        for (Session player : game.getPlayers()) {
            player.getBasicRemote().sendObject(response);
        }
    }

    private void handlePerformPlayRequest(Session session, MessageRequest request) throws EncodeException, IOException {
        final Player requester = getRequesterPlayer(session);
        if (requester != game.getTurn()) return;

        final Coordinate requestCoordinate = request.getPieceCoordinate();

        game.play(requestCoordinate);

        final MessageResponse response = new MessageResponse(game.getBoard(), game.getBuffer(), game.getTurn());
        sendResponseForAllPlayers(response);

        if (gameWon(requestCoordinate)) {
            final Winner winner = game.getTurn() == Player.PLAYER_1 ? Winner.PLAYER_1 : Winner.PLAYER_2;
            sendResponseForAllPlayers(new MessageResponse(winner));
            game = new Game();
            return;
        }

        final boolean stillPlayable = Arrays.stream(game.getBoard()).flatMap(Arrays::stream).anyMatch(Objects::isNull);
        if (stillPlayable) return;

        sendResponseForAllPlayers(new MessageResponse(Winner.DRAW));
        game = new Game();
    }

    private boolean gameWon(Coordinate pieceCoordinate) {
        return rowWins(pieceCoordinate.x()) ||
                columnWins(pieceCoordinate.y()) ||
                pieceCoordinate.x() == pieceCoordinate.y() && mainDiagonalWins() ||
                pieceCoordinate.x() + pieceCoordinate.y() == 3 && secondaryDiagonalWins();
    }

    private boolean mainDiagonalWins() {
        final Piece[] mainDiagonalPieces = new Piece[Game.BOARD_SIZE];
        for (int row = 0; row < Game.BOARD_SIZE; row++) {
            mainDiagonalPieces[row] = game.getBoard()[row][row];
        }
        return checkWinner(mainDiagonalPieces);
    }

    private boolean secondaryDiagonalWins() {
        final Piece[] secondaryDiagonalPieces = new Piece[Game.BOARD_SIZE];
        final int startAtColumn = Game.BOARD_SIZE - 1;
        for (int row = 0; row < Game.BOARD_SIZE; row++) {
            secondaryDiagonalPieces[row] = game.getBoard()[row][startAtColumn - row];
        }
        return checkWinner(secondaryDiagonalPieces);
    }

    private boolean columnWins(int columnIndex) {
        return checkWinner(Arrays.stream(game.getBoard()).map(pieces -> pieces[columnIndex]).toArray(Piece[]::new));
    }

    private boolean rowWins(int rowIndex) {
        return checkWinner(game.getBoard()[rowIndex]);
    }

    private boolean checkWinner(Piece[] pieces) {
        final Piece reference = pieces[0];

        if (reference == null) return false;

        if (Arrays.stream(pieces).allMatch(piece -> piece != null && piece.size() == reference.size())) return true;
        if (Arrays.stream(pieces).allMatch(piece -> piece != null && piece.type() == reference.type())) return true;
        if (Arrays.stream(pieces).allMatch(piece -> piece != null && piece.color() == reference.color())) return true;

        return Arrays.stream(pieces).allMatch(piece -> piece != null && piece.shape() == reference.shape());
    }

    private Player getRequesterPlayer(Session session) {
        return game.getPlayers().indexOf(session) == 0 ? Player.PLAYER_1 : Player.PLAYER_2;
    }

    @OnClose
    public void onClose(Session session) throws EncodeException, IOException {
        if (game.getPlayers().size() == 2) {
            final int playerIndex = game.getPlayers().indexOf(session);
            game.removePlayer(session);
            final Session winnerSession = game.getPlayers().get(0);
            final Winner winnerPlayer = playerIndex == 0 ? Winner.PLAYER_2 : Winner.PLAYER_1;
            winnerSession.getBasicRemote().sendObject(new MessageResponse(winnerPlayer));
        }
        game = new Game();
    }
}
