"""
PyTorch neural network for chess move prediction.
"""

import torch
import torch.nn as nn
import torch.nn.functional as F
import chess
import numpy as np
from typing import Tuple

class ChessNet(nn.Module):
    """
    Convolutional neural network for chess position evaluation.

    Input: 8x8x12 tensor (12 piece types across 64 squares)
    Output: Policy (move probabilities) + Value (position evaluation)
    """

    def __init__(self, num_filters=256, num_residual_blocks=10):
        super(ChessNet, self).__init__()

        # Initial convolutional layer
        self.conv_input = nn.Conv2d(12, num_filters, kernel_size=3, padding=1)
        self.bn_input = nn.BatchNorm2d(num_filters)

        # Residual blocks
        self.residual_blocks = nn.ModuleList([
            ResidualBlock(num_filters) for _ in range(num_residual_blocks)
        ])

        # Policy head (move prediction)
        self.conv_policy = nn.Conv2d(num_filters, 2, kernel_size=1)
        self.bn_policy = nn.BatchNorm2d(2)
        self.fc_policy = nn.Linear(2 * 8 * 8, 4096)  # All possible moves

        # Value head (position evaluation)
        self.conv_value = nn.Conv2d(num_filters, 1, kernel_size=1)
        self.bn_value = nn.BatchNorm2d(1)
        self.fc_value1 = nn.Linear(8 * 8, 256)
        self.fc_value2 = nn.Linear(256, 1)

    def forward(self, x):
        # Input convolution
        x = F.relu(self.bn_input(self.conv_input(x)))

        # Residual tower
        for block in self.residual_blocks:
            x = block(x)

        # Policy head
        policy = F.relu(self.bn_policy(self.conv_policy(x)))
        policy = policy.view(-1, 2 * 8 * 8)
        policy = self.fc_policy(policy)

        # Value head
        value = F.relu(self.bn_value(self.conv_value(x)))
        value = value.view(-1, 8 * 8)
        value = F.relu(self.fc_value1(value))
        value = torch.tanh(self.fc_value2(value))

        return policy, value


class ResidualBlock(nn.Module):
    """Residual block with batch normalization."""

    def __init__(self, num_filters):
        super(ResidualBlock, self).__init__()

        self.conv1 = nn.Conv2d(num_filters, num_filters, kernel_size=3, padding=1)
        self.bn1 = nn.BatchNorm2d(num_filters)

        self.conv2 = nn.Conv2d(num_filters, num_filters, kernel_size=3, padding=1)
        self.bn2 = nn.BatchNorm2d(num_filters)

    def forward(self, x):
        residual = x

        out = F.relu(self.bn1(self.conv1(x)))
        out = self.bn2(self.conv2(out))

        out += residual
        out = F.relu(out)

        return out


def encode_board(board: chess.Board) -> np.ndarray:
    """
    Encode chess board to 8x8x12 numpy array.

    12 channels: 6 piece types x 2 colors
    """
    encoded = np.zeros((12, 8, 8), dtype=np.float32)

    piece_to_channel = {
        (chess.PAWN, chess.WHITE): 0,
        (chess.KNIGHT, chess.WHITE): 1,
        (chess.BISHOP, chess.WHITE): 2,
        (chess.ROOK, chess.WHITE): 3,
        (chess.QUEEN, chess.WHITE): 4,
        (chess.KING, chess.WHITE): 5,
        (chess.PAWN, chess.BLACK): 6,
        (chess.KNIGHT, chess.BLACK): 7,
        (chess.BISHOP, chess.BLACK): 8,
        (chess.ROOK, chess.BLACK): 9,
        (chess.QUEEN, chess.BLACK): 10,
        (chess.KING, chess.BLACK): 11,
    }

    for square in chess.SQUARES:
        piece = board.piece_at(square)
        if piece:
            channel = piece_to_channel[(piece.piece_type, piece.color)]
            rank = chess.square_rank(square)
            file = chess.square_file(square)
            encoded[channel, rank, file] = 1.0

    return encoded


def predict_move(model: ChessNet, fen: str, device: str = "cpu") -> Tuple[str, float]:
    """
    Predict the best move for a given position.

    Args:
        model: Trained ChessNet model
        fen: Board position in FEN notation
        device: 'cpu' or 'cuda'

    Returns:
        Tuple of (move_uci, score)
    """
    board = chess.Board(fen)
    encoded = encode_board(board)

    # Convert to tensor
    tensor = torch.from_numpy(encoded).unsqueeze(0).to(device)

    # Get predictions
    with torch.no_grad():
        policy, value = model(tensor)

    # Get legal moves
    legal_moves = list(board.legal_moves)
    if not legal_moves:
        raise ValueError("No legal moves available")

    # Convert policy to move probabilities
    policy_probs = torch.softmax(policy[0], dim=0).cpu().numpy()

    # Find best legal move
    best_move = None
    best_score = float('-inf')

    for move in legal_moves:
        # Simplified: use move index as policy index
        # In practice, you'd need a proper move-to-index mapping
        move_idx = hash(move.uci()) % len(policy_probs)
        score = policy_probs[move_idx]

        if score > best_score:
            best_score = score
            best_move = move

    # Fallback to random if no move found
    if best_move is None:
        import random
        best_move = random.choice(legal_moves)
        best_score = 0.5

    return best_move.uci(), float(value[0].item())


def export_to_onnx(model: ChessNet, output_path: str):
    """Export model to ONNX format for production inference."""
    model.eval()

    dummy_input = torch.randn(1, 12, 8, 8)

    torch.onnx.export(
        model,
        dummy_input,
        output_path,
        export_params=True,
        opset_version=14,
        do_constant_folding=True,
        input_names=['input'],
        output_names=['policy', 'value'],
        dynamic_axes={
            'input': {0: 'batch_size'},
            'policy': {0: 'batch_size'},
            'value': {0: 'batch_size'}
        }
    )

    print(f"Model exported to {output_path}")
