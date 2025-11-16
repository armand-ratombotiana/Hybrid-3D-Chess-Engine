package com.chess.client.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Chess board model.
 * Manages board state, pieces, and game rules.
 */
public class Board {

    private final Square[][] squares;
    private final List<String> moveHistory;
    private boolean whiteTurn;
    private boolean whiteKingMoved;
    private boolean blackKingMoved;
    private boolean whiteRookKingsideMoved;
    private boolean whiteRookQueensideMoved;
    private boolean blackRookKingsideMoved;
    private boolean blackRookQueensideMoved;
    private Square enPassantSquare;

    public Board() {
        this.squares = new Square[8][8];
        this.moveHistory = new ArrayList<>();
        this.whiteTurn = true;

        // Initialize squares
        for (int file = 0; file < 8; file++) {
            for (int rank = 0; rank < 8; rank++) {
                squares[file][rank] = new Square(file, rank);
            }
        }
    }

    /**
     * Setup starting chess position.
     */
    public void setupStartingPosition() {
        reset();

        // White pieces
        setPiece(0, 0, new Piece(PieceType.ROOK, true));
        setPiece(1, 0, new Piece(PieceType.KNIGHT, true));
        setPiece(2, 0, new Piece(PieceType.BISHOP, true));
        setPiece(3, 0, new Piece(PieceType.QUEEN, true));
        setPiece(4, 0, new Piece(PieceType.KING, true));
        setPiece(5, 0, new Piece(PieceType.BISHOP, true));
        setPiece(6, 0, new Piece(PieceType.KNIGHT, true));
        setPiece(7, 0, new Piece(PieceType.ROOK, true));

        for (int file = 0; file < 8; file++) {
            setPiece(file, 1, new Piece(PieceType.PAWN, true));
        }

        // Black pieces
        setPiece(0, 7, new Piece(PieceType.ROOK, false));
        setPiece(1, 7, new Piece(PieceType.KNIGHT, false));
        setPiece(2, 7, new Piece(PieceType.BISHOP, false));
        setPiece(3, 7, new Piece(PieceType.QUEEN, false));
        setPiece(4, 7, new Piece(PieceType.KING, false));
        setPiece(5, 7, new Piece(PieceType.BISHOP, false));
        setPiece(6, 7, new Piece(PieceType.KNIGHT, false));
        setPiece(7, 7, new Piece(PieceType.ROOK, false));

        for (int file = 0; file < 8; file++) {
            setPiece(file, 6, new Piece(PieceType.PAWN, false));
        }
    }

    /**
     * Reset board to empty state.
     */
    public void reset() {
        for (int file = 0; file < 8; file++) {
            for (int rank = 0; rank < 8; rank++) {
                squares[file][rank].setPiece(null);
            }
        }

        moveHistory.clear();
        whiteTurn = true;
        whiteKingMoved = false;
        blackKingMoved = false;
        whiteRookKingsideMoved = false;
        whiteRookQueensideMoved = false;
        blackRookKingsideMoved = false;
        blackRookQueensideMoved = false;
        enPassantSquare = null;
    }

    /**
     * Make a move on the board.
     */
    public boolean makeMove(int fromFile, int fromRank, int toFile, int toRank) {
        Square from = getSquare(fromFile, fromRank);
        Square to = getSquare(toFile, toRank);

        if (from == null || to == null || from.getPiece() == null) {
            return false;
        }

        Piece piece = from.getPiece();

        // Validate it's the correct player's turn
        if (piece.isWhite() != whiteTurn) {
            return false;
        }

        // TODO: Implement full move validation (legal moves, check, etc.)

        // Make the move
        to.setPiece(piece);
        from.setPiece(null);

        // Record move
        String moveNotation = squareToAlgebraic(fromFile, fromRank) +
                              squareToAlgebraic(toFile, toRank);
        moveHistory.add(moveNotation);

        // Switch turns
        whiteTurn = !whiteTurn;

        return true;
    }

    /**
     * Convert square coordinates to algebraic notation.
     */
    private String squareToAlgebraic(int file, int rank) {
        char fileChar = (char) ('a' + file);
        char rankChar = (char) ('1' + rank);
        return "" + fileChar + rankChar;
    }

    /**
     * Get all squares with pieces.
     */
    public List<Square> getAllSquares() {
        List<Square> allSquares = new ArrayList<>();
        for (int file = 0; file < 8; file++) {
            for (int rank = 0; rank < 8; rank++) {
                allSquares.add(squares[file][rank]);
            }
        }
        return allSquares;
    }

    /**
     * Get FEN (Forsyth-Edwards Notation) representation.
     */
    public String toFEN() {
        StringBuilder fen = new StringBuilder();

        // Piece placement (from rank 7 to 0)
        for (int rank = 7; rank >= 0; rank--) {
            int emptyCount = 0;

            for (int file = 0; file < 8; file++) {
                Piece piece = squares[file][rank].getPiece();

                if (piece == null) {
                    emptyCount++;
                } else {
                    if (emptyCount > 0) {
                        fen.append(emptyCount);
                        emptyCount = 0;
                    }
                    fen.append(piece.toFENChar());
                }
            }

            if (emptyCount > 0) {
                fen.append(emptyCount);
            }

            if (rank > 0) {
                fen.append('/');
            }
        }

        // Active color
        fen.append(whiteTurn ? " w " : " b ");

        // Castling availability
        StringBuilder castling = new StringBuilder();
        if (!whiteKingMoved) {
            if (!whiteRookKingsideMoved) castling.append('K');
            if (!whiteRookQueensideMoved) castling.append('Q');
        }
        if (!blackKingMoved) {
            if (!blackRookKingsideMoved) castling.append('k');
            if (!blackRookQueensideMoved) castling.append('q');
        }
        fen.append(castling.length() > 0 ? castling : "-");

        // En passant target square
        fen.append(" ");
        if (enPassantSquare != null) {
            fen.append(squareToAlgebraic(enPassantSquare.getFile(), enPassantSquare.getRank()));
        } else {
            fen.append("-");
        }

        // Halfmove clock and fullmove number (simplified)
        fen.append(" 0 ");
        fen.append((moveHistory.size() / 2) + 1);

        return fen.toString();
    }

    /**
     * Load position from FEN string.
     */
    public void fromFEN(String fen) {
        reset();

        String[] parts = fen.split(" ");
        if (parts.length < 4) {
            throw new IllegalArgumentException("Invalid FEN string");
        }

        // Parse piece placement
        String[] ranks = parts[0].split("/");
        for (int rank = 0; rank < 8; rank++) {
            int file = 0;
            String rankStr = ranks[7 - rank];

            for (char c : rankStr.toCharArray()) {
                if (Character.isDigit(c)) {
                    file += Character.getNumericValue(c);
                } else {
                    Piece piece = Piece.fromFENChar(c);
                    setPiece(file, rank, piece);
                    file++;
                }
            }
        }

        // Parse active color
        whiteTurn = parts[1].equals("w");

        // TODO: Parse castling rights, en passant, etc.
    }

    public String getMoveHistory() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < moveHistory.size(); i++) {
            if (i % 2 == 0) {
                sb.append((i / 2 + 1)).append(". ");
            }
            sb.append(moveHistory.get(i)).append(" ");
            if (i % 2 == 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    public boolean isCheck() {
        // TODO: Implement check detection
        return false;
    }

    public boolean isCheckmate() {
        // TODO: Implement checkmate detection
        return false;
    }

    public boolean isStalemate() {
        // TODO: Implement stalemate detection
        return false;
    }

    public Square getSquare(int file, int rank) {
        if (file < 0 || file >= 8 || rank < 0 || rank >= 8) {
            return null;
        }
        return squares[file][rank];
    }

    private void setPiece(int file, int rank, Piece piece) {
        squares[file][rank].setPiece(piece);
    }

    public boolean isWhiteTurn() {
        return whiteTurn;
    }

    public void setWhiteTurn(boolean whiteTurn) {
        this.whiteTurn = whiteTurn;
    }
}
