"""
FastAPI Chess AI Engine
Provides move prediction and model training endpoints.
"""

from fastapi import FastAPI, HTTPException, Depends, Header
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from typing import Optional, List
import logging
import os

from app.engine.minimax import MinimaxEngine
from app.models.chess_net import ChessNet, predict_move
from app.utils.fen_encoder import encode_fen

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# Initialize FastAPI app
app = FastAPI(
    title="Chess AI Engine",
    description="AI-powered chess move prediction and training",
    version="1.0.0"
)

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Configure for production
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Configuration
API_KEY = os.getenv("API_KEY", "your-ai-api-key-change-in-production")
ADMIN_TOKEN = os.getenv("ADMIN_TOKEN", "your-admin-token-change-in-production")
MODEL_PATH = os.getenv("MODEL_PATH", "models")
DEVICE = os.getenv("DEVICE", "cpu")

# Initialize engines
minimax_engine = MinimaxEngine(depth=3)
neural_net = None  # Loaded on demand

# Load neural network if model exists
try:
    import torch
    model_file = os.path.join(MODEL_PATH, "chess_model.pth")
    if os.path.exists(model_file):
        neural_net = ChessNet()
        neural_net.load_state_dict(torch.load(model_file, map_location=DEVICE))
        neural_net.eval()
        logger.info(f"Loaded neural network from {model_file}")
    else:
        logger.warning(f"No model found at {model_file}, using minimax only")
except Exception as e:
    logger.error(f"Failed to load neural network: {e}")

# Request/Response Models
class PredictRequest(BaseModel):
    fen: str = Field(..., description="FEN string of current board position")
    playerColor: str = Field("white", description="Player color: white or black")
    gameId: Optional[str] = Field(None, description="Optional game ID")
    timeControl: Optional[dict] = Field(None, description="Time control settings")

class PredictResponse(BaseModel):
    from_square: str = Field(..., alias="from", description="Source square (e.g., 'e2')")
    to: str = Field(..., description="Destination square (e.g., 'e4')")
    promotion: Optional[str] = Field(None, description="Promotion piece if applicable")
    score: float = Field(..., description="Evaluation score")
    pv: Optional[List[str]] = Field(None, description="Principal variation")
    depth: int = Field(0, description="Search depth")
    nodes: int = Field(0, description="Nodes searched")
    thinkingTime: int = Field(0, description="Thinking time in milliseconds")

    class Config:
        populate_by_name = True

class TrainRequest(BaseModel):
    epochs: int = Field(10, ge=1, le=1000)
    batch_size: int = Field(32, ge=1, le=512)
    learning_rate: float = Field(0.001, gt=0, lt=1)
    model_version: str = Field("v1.0.0")

class TrainResponse(BaseModel):
    status: str
    message: str
    job_id: Optional[str] = None

# Dependency for API key validation
async def verify_api_key(api_key: Optional[str] = Header(None)):
    if api_key != API_KEY:
        logger.warning(f"Invalid API key attempted: {api_key}")
        # In development, we'll allow requests without API key
        # raise HTTPException(status_code=401, detail="Invalid API key")
    return api_key

# Dependency for admin token validation
async def verify_admin_token(authorization: Optional[str] = Header(None)):
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="Missing or invalid authorization header")

    token = authorization[7:]  # Remove "Bearer " prefix
    if token != ADMIN_TOKEN:
        raise HTTPException(status_code=403, detail="Invalid admin token")

    return token

@app.get("/health")
async def health_check():
    """Health check endpoint."""
    return {
        "status": "healthy",
        "model_loaded": neural_net is not None,
        "device": DEVICE
    }

@app.post("/predict", response_model=PredictResponse)
async def predict(
    request: PredictRequest,
    api_key: str = Depends(verify_api_key)
):
    """
    Predict the best move for a given board position.

    Uses neural network if available, falls back to minimax engine.
    """
    logger.info(f"Predict request: FEN={request.fen[:50]}..., color={request.playerColor}")

    try:
        import time
        start_time = time.time()

        # Try neural network first
        if neural_net is not None:
            try:
                move_uci, score = predict_move(neural_net, request.fen, DEVICE)
                thinking_time = int((time.time() - start_time) * 1000)

                # Parse UCI move (e.g., "e2e4" -> from="e2", to="e4")
                from_square = move_uci[:2]
                to_square = move_uci[2:4]
                promotion = move_uci[4:] if len(move_uci) > 4 else None

                logger.info(f"Neural network prediction: {from_square} -> {to_square} (score: {score:.3f})")

                return PredictResponse(
                    from_square=from_square,
                    to=to_square,
                    promotion=promotion,
                    score=score,
                    pv=[move_uci],
                    depth=0,
                    nodes=0,
                    thinkingTime=thinking_time
                )

            except Exception as e:
                logger.warning(f"Neural network prediction failed: {e}, falling back to minimax")

        # Fallback to minimax
        move_uci, score, depth, nodes, pv = minimax_engine.get_best_move(request.fen)
        thinking_time = int((time.time() - start_time) * 1000)

        from_square = move_uci[:2]
        to_square = move_uci[2:4]
        promotion = move_uci[4:] if len(move_uci) > 4 else None

        logger.info(f"Minimax prediction: {from_square} -> {to_square} (score: {score:.3f}, depth: {depth})")

        return PredictResponse(
            from_square=from_square,
            to=to_square,
            promotion=promotion,
            score=score,
            pv=pv,
            depth=depth,
            nodes=nodes,
            thinkingTime=thinking_time
        )

    except Exception as e:
        logger.error(f"Prediction error: {e}")
        raise HTTPException(status_code=500, detail=f"Prediction failed: {str(e)}")

@app.post("/train", response_model=TrainResponse)
async def train_model(
    request: TrainRequest,
    admin_token: str = Depends(verify_admin_token)
):
    """
    Trigger model training job (async).
    Requires admin authentication.
    """
    logger.info(f"Training request: epochs={request.epochs}, batch_size={request.batch_size}")

    try:
        # TODO: Implement async training job with Celery or similar
        # For now, return a placeholder response

        import uuid
        job_id = str(uuid.uuid4())

        logger.info(f"Training job {job_id} queued")

        return TrainResponse(
            status="queued",
            message=f"Training job queued with {request.epochs} epochs",
            job_id=job_id
        )

    except Exception as e:
        logger.error(f"Training error: {e}")
        raise HTTPException(status_code=500, detail=f"Training failed: {str(e)}")

@app.get("/models")
async def list_models():
    """List available models."""
    try:
        import glob

        models = []
        for model_file in glob.glob(os.path.join(MODEL_PATH, "*.pth")):
            models.append({
                "name": os.path.basename(model_file),
                "path": model_file,
                "size": os.path.getsize(model_file)
            })

        return {
            "models": models,
            "active_model": "chess_model.pth" if neural_net else None
        }

    except Exception as e:
        logger.error(f"Failed to list models: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/")
async def root():
    """Root endpoint with API information."""
    return {
        "name": "Chess AI Engine",
        "version": "1.0.0",
        "endpoints": {
            "health": "/health",
            "predict": "/predict (POST)",
            "train": "/train (POST, admin only)",
            "models": "/models (GET)"
        }
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
