"""
Minimax chess engine with alpha-beta pruning.
Fallback engine when neural network is not available.
"""

import chess
import random
from typing import Tuple, List, Optional

class MinimaxEngine:
    """
    Simple minimax engine with alpha-beta pruning.
    """

    # Piece values (centipawns)
    PIECE_VALUES = {
        chess.PAWN: 100,
        chess.KNIGHT: 320,
        chess.BISHOP: 330,
        chess.ROOK: 500,
        chess.QUEEN: 900,
        chess.KING: 20000
    }

    # Position bonuses for pieces (simplified)
    PAWN_TABLE = [
        0,  0,  0,  0,  0,  0,  0,  0,
        50, 50, 50, 50, 50, 50, 50, 50,
        10, 10, 20, 30, 30, 20, 10, 10,
        5,  5, 10, 25, 25, 10,  5,  5,
        0,  0,  0, 20, 20,  0,  0,  0,
        5, -5,-10,  0,  0,-10, -5,  5,
        5, 10, 10,-20,-20, 10, 10,  5,
        0,  0,  0,  0,  0,  0,  0,  0
    ]

    KNIGHT_TABLE = [
        -50,-40,-30,-30,-30,-30,-40,-50,
        -40,-20,  0,  0,  0,  0,-20,-40,
        -30,  0, 10, 15, 15, 10,  0,-30,
        -30,  5, 15, 20, 20, 15,  5,-30,
        -30,  0, 15, 20, 20, 15,  0,-30,
        -30,  5, 10, 15, 15, 10,  5,-30,
        -40,-20,  0,  5,  5,  0,-20,-40,
        -50,-40,-30,-30,-30,-30,-40,-50
    ]

    def __init__(self, depth: int = 3):
        self.depth = depth
        self.nodes_searched = 0
        self.pv: List[str] = []

    def get_best_move(
        self,
        fen: str
    ) -> Tuple[str, float, int, int, List[str]]:
        """
        Get the best move for the current position.

        Returns:
            Tuple of (move_uci, score, depth, nodes, principal_variation)
        """
        board = chess.Board(fen)
        self.nodes_searched = 0
        self.pv = []

        if board.is_game_over():
            raise ValueError("Game is over")

        # Get all legal moves
        legal_moves = list(board.legal_moves)
        if not legal_moves:
            raise ValueError("No legal moves available")

        # Search for best move
        best_move = None
        best_score = float('-inf')
        alpha = float('-inf')
        beta = float('inf')

        for move in legal_moves:
            board.push(move)
            score = -self._minimax(board, self.depth - 1, -beta, -alpha, False)
            board.pop()

            if score > best_score:
                best_score = score
                best_move = move

            alpha = max(alpha, score)

        if best_move is None:
            # Fallback to random move
            best_move = random.choice(legal_moves)
            best_score = 0.0

        return (
            best_move.uci(),
            best_score / 100.0,  # Convert centipawns to pawns
            self.depth,
            self.nodes_searched,
            [best_move.uci()]
        )

    def _minimax(
        self,
        board: chess.Board,
        depth: int,
        alpha: float,
        beta: float,
        maximizing: bool
    ) -> float:
        """
        Minimax search with alpha-beta pruning.
        """
        self.nodes_searched += 1

        # Terminal nodes
        if depth == 0 or board.is_game_over():
            return self._evaluate(board)

        legal_moves = list(board.legal_moves)

        if maximizing:
            max_eval = float('-inf')
            for move in legal_moves:
                board.push(move)
                eval_score = self._minimax(board, depth - 1, alpha, beta, False)
                board.pop()

                max_eval = max(max_eval, eval_score)
                alpha = max(alpha, eval_score)

                if beta <= alpha:
                    break  # Beta cutoff

            return max_eval
        else:
            min_eval = float('inf')
            for move in legal_moves:
                board.push(move)
                eval_score = self._minimax(board, depth - 1, alpha, beta, True)
                board.pop()

                min_eval = min(min_eval, eval_score)
                beta = min(beta, eval_score)

                if beta <= alpha:
                    break  # Alpha cutoff

            return min_eval

    def _evaluate(self, board: chess.Board) -> float:
        """
        Evaluate the board position.

        Returns a score in centipawns from white's perspective.
        """
        if board.is_checkmate():
            return -20000 if board.turn else 20000

        if board.is_stalemate() or board.is_insufficient_material():
            return 0

        score = 0

        # Material and position evaluation
        for square in chess.SQUARES:
            piece = board.piece_at(square)
            if piece is None:
                continue

            value = self.PIECE_VALUES.get(piece.piece_type, 0)

            # Add positional bonus
            if piece.piece_type == chess.PAWN:
                pos_bonus = self.PAWN_TABLE[square if piece.color else 63 - square]
            elif piece.piece_type == chess.KNIGHT:
                pos_bonus = self.KNIGHT_TABLE[square if piece.color else 63 - square]
            else:
                pos_bonus = 0

            total_value = value + pos_bonus

            if piece.color == chess.WHITE:
                score += total_value
            else:
                score -= total_value

        # Mobility bonus
        score += len(list(board.legal_moves)) * 10 if board.turn == chess.WHITE else -len(list(board.legal_moves)) * 10

        return score
