package model;

import lombok.Getter;

import javax.websocket.Session;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Getter
public class Game {
    public static final int BOARD_SIZE = 4;

    private Piece[][] board;
    private Piece[][] buffer;
    private final List<Session> players;
    private Coordinate turnPieceCoordinate;
    private GameStatus status;
    private Player turn;

    public Game() {
        this.players = new ArrayList<>();
        this.status = GameStatus.PREPARING;
    }

    public void start() {
        this.board = new Piece[BOARD_SIZE][BOARD_SIZE];
        this.buffer = new Piece[BOARD_SIZE][BOARD_SIZE];
        this.status = GameStatus.IN_PROGRESS;

        initializeBuffer();
        raffleTurn();
    }

    private void raffleTurn() {
        final int raffledPlayerIndex = new Random().nextInt(2);
        this.turn = raffledPlayerIndex == 0 ? Player.PLAYER_1 : Player.PLAYER_2;
    }

    public void switchTurn() {
        this.turn = turn == Player.PLAYER_1 ? Player.PLAYER_2 : Player.PLAYER_1;
    }

    public void play(Coordinate boardCoordinate) {
        board[boardCoordinate.x()][boardCoordinate.y()] = buffer[turnPieceCoordinate.x()][turnPieceCoordinate.y()];
        buffer[turnPieceCoordinate.x()][turnPieceCoordinate.y()] = null;
        turnPieceCoordinate = null;
    }

    public void setRoundPiece(Coordinate bufferCoordinate) {
        this.turnPieceCoordinate = bufferCoordinate;
    }

    public void addPlayer(Session player) {
        this.players.add(player);
    }

    public void removePlayer(Session player) {
        this.players.remove(player);
    }

    private void initializeBuffer() {
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int column = 0; column < BOARD_SIZE; column++) {
                final Size size = column < BOARD_SIZE / 2 ? Size.BIG : Size.SMALL;
                final Color color = row % 2 == 0 ? Color.RED : Color.BLUE;
                final Shape shape = row < BOARD_SIZE / 2 ? Shape.ROUND : Shape.SQUARE;
                final Type type = column % 2 == 0 ? Type.FULL : Type.HOLLOW;

                buffer[row][column] = new Piece(size, color, shape, type);
            }
        }
    }
}
