"""
FEN encoding utilities.
"""

import chess
import numpy as np

def encode_fen(fen: str) -> np.ndarray:
    """
    Encode FEN string to numerical representation.

    Returns:
        numpy array suitable for model input
    """
    board = chess.Board(fen)

    # Use the encoding from chess_net
    from app.models.chess_net import encode_board
    return encode_board(board)


def decode_move(move_vector: np.ndarray, board: chess.Board) -> str:
    """
    Decode move vector to UCI notation.

    Args:
        move_vector: Model output vector
        board: Current board state

    Returns:
        Move in UCI notation (e.g., "e2e4")
    """
    legal_moves = list(board.legal_moves)
    if not legal_moves:
        raise ValueError("No legal moves")

    # Simplified: return move with highest probability
    move_idx = np.argmax(move_vector) % len(legal_moves)
    return legal_moves[move_idx].uci()
