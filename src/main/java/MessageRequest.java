import lombok.Getter;
import model.Coordinate;

@Getter
public class MessageRequest {
    private Coordinate pieceCoordinate;
    private final ConnectionType connectionType;

    public MessageRequest(ConnectionType connectionType) {
        this.connectionType = connectionType;
    }

    public MessageRequest(Coordinate pieceCoordinate, ConnectionType connectionType) {
        this.pieceCoordinate = pieceCoordinate;
        this.connectionType = connectionType;
    }
}
