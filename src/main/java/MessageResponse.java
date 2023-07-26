import lombok.Getter;
import model.Coordinate;
import model.GameStatus;
import model.Piece;
import model.Player;
import model.Winner;

@Getter
public class MessageResponse {
    private Piece[][] board;
    private Piece[][] buffer;
    private Player player;
    private Player turn;
    private Coordinate turnPieceCoordinate;
    private Winner winner;
    private final GameStatus status;

    // After enter game:
    public MessageResponse(Player player) {
        this.player = player;
        this.status = GameStatus.PREPARING;
    }

    // After set opponent's piece:
    public MessageResponse(Player turn, Coordinate turnPieceCoordinate) {
        this.turn = turn;
        this.turnPieceCoordinate = turnPieceCoordinate;
        this.status = GameStatus.IN_PROGRESS;
    }

    // After make move:
    public MessageResponse(Piece[][] board,
                           Piece[][] buffer,
                           Player turn) {
        this.board = board;
        this.buffer = buffer;
        this.turn = turn;
        this.status = GameStatus.IN_PROGRESS;
    }

    // After someone win the game:
    public MessageResponse(Winner winner) {
        this.winner = winner;
        this.status = GameStatus.FINISHED;
    }

}
