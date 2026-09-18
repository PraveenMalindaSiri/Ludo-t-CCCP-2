package block;

import java.util.List;
import piece.Piece;

public interface IMovable {
    void move(int steps);

    int getPosition();

    String getDirection();

    List<Piece> getPieces();
}
