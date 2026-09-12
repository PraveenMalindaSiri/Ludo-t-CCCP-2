package block;

import piece.Piece;

import java.util.List;

public interface IMovable {
    void move(int steps);

    int getPosition();

    String getDirection();

    List<Piece> getPieces();
}
