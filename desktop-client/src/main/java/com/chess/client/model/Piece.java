package com.chess.client.model;

/**
 * Chess piece.
 */
public class Piece {

    private final PieceType type;
    private final boolean white;

    public Piece(PieceType type, boolean white) {
        this.type = type;
        this.white = white;
    }

    public PieceType getType() {
        return type;
    }

    public boolean isWhite() {
        return white;
    }

    /**
     * Convert piece to FEN character.
     */
    public char toFENChar() {
        char c = switch (type) {
            case KING -> 'k';
            case QUEEN -> 'q';
            case ROOK -> 'r';
            case BISHOP -> 'b';
            case KNIGHT -> 'n';
            case PAWN -> 'p';
        };
        return white ? Character.toUpperCase(c) : c;
    }

    /**
     * Create piece from FEN character.
     */
    public static Piece fromFENChar(char c) {
        boolean isWhite = Character.isUpperCase(c);
        char lower = Character.toLowerCase(c);

        PieceType type = switch (lower) {
            case 'k' -> PieceType.KING;
            case 'q' -> PieceType.QUEEN;
            case 'r' -> PieceType.ROOK;
            case 'b' -> PieceType.BISHOP;
            case 'n' -> PieceType.KNIGHT;
            case 'p' -> PieceType.PAWN;
            default -> throw new IllegalArgumentException("Invalid FEN piece char: " + c);
        };

        return new Piece(type, isWhite);
    }
}
