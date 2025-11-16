package com.chess.client.model;

/**
 * Chess board square.
 */
public class Square {

    private final int file; // 0-7 (a-h)
    private final int rank; // 0-7 (1-8)
    private Piece piece;

    public Square(int file, int rank) {
        this.file = file;
        this.rank = rank;
        this.piece = null;
    }

    public int getFile() {
        return file;
    }

    public int getRank() {
        return rank;
    }

    public Piece getPiece() {
        return piece;
    }

    public void setPiece(Piece piece) {
        this.piece = piece;
    }

    public boolean isEmpty() {
        return piece == null;
    }

    /**
     * Get algebraic notation (e.g., "e4").
     */
    public String toAlgebraic() {
        char fileChar = (char) ('a' + file);
        char rankChar = (char) ('1' + rank);
        return "" + fileChar + rankChar;
    }
}
