# Quick Start Guide

This guide will help you get the Chess AI Platform up and running quickly.

## Prerequisites Check

```bash
# Check Java
java -version  # Should be 17+

# Check Maven
mvn -version   # Should be 3.8+

# Check Python
python --version  # Should be 3.10+

# Check Node.js
node --version  # Should be 18+

# Check Docker
docker --version
docker-compose --version
```

## Option 1: Quick Start with Docker (Recommended)

```bash
# Clone/navigate to project
cd chess-game

# Start all services
docker-compose up --build

# Wait for services to be healthy (check logs)
docker-compose logs -f

# Access:
# - Backend API: http://localhost:8080
# - AI Engine: http://localhost:8000
# - React Admin: http://localhost:3000
# - Prometheus: http://localhost:9090
# - Grafana: http://localhost:3001

# Run desktop client (in separate terminal)
cd desktop-client
./mvnw javafx:run
```

## Option 2: Manual Setup (Development)

### 1. Start Infrastructure

```bash
# Start only database and redis
docker-compose up -d postgres redis

# Wait for services
sleep 10
```

### 2. Start Backend

```bash
cd quarkus-backend

# Development mode (hot reload)
./mvnw quarkus:dev

# Or build and run
./mvnw package
java -jar target/quarkus-app/quarkus-run.jar
```

### 3. Start AI Engine

```bash
cd ai-engine

# Create virtual environment
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate

# Install dependencies
pip install -r requirements.txt

# Run FastAPI
uvicorn app.main:app --reload --port 8000
```

### 4. Start React Admin

```bash
cd react-admin

# Install dependencies
npm install

# Run development server
npm run dev
```

### 5. Run Desktop Client

```bash
cd desktop-client

# Run with Maven
./mvnw javafx:run

# Or build fat JAR
./mvnw package
java -jar target/chess-desktop-client-1.0.0.jar
```

## Initial Setup

### 1. Create Database

The database will be automatically initialized on first run. If you need to manually create it:

```bash
# Connect to PostgreSQL
docker exec -it chess-postgres psql -U chess_user -d chess_db

# Run migrations (if needed)
# Tables will be auto-created by Hibernate
```

### 2. Create Admin User

```bash
# Option 1: Use API
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "email": "admin@chess.com",
    "password": "admin123",
    "role": "ADMIN"
  }'

# Option 2: Direct database insert
docker exec -it chess-postgres psql -U chess_user -d chess_db -c \
  "INSERT INTO users (id, username, email, password_hash, role) VALUES \
   (gen_random_uuid(), 'admin', 'admin@chess.com', '\$2a\$12\$...', 'ADMIN');"
```

### 3. Train Initial AI Model (Optional)

```bash
# Trigger training via API
curl -X POST http://localhost:8000/train \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer your-admin-token" \
  -d '{
    "epochs": 10,
    "batch_size": 32,
    "model_version": "v1.0.0"
  }'

# Or run training script directly
cd ai-engine
python scripts/train_model.py --epochs 10 --output models/v1.0.0
```

## Verify Installation

### 1. Check Backend Health

```bash
curl http://localhost:8080/q/health/live
# Should return: {"status":"UP"}

curl http://localhost:8080/q/health/ready
# Should return: {"status":"UP"} with database and redis checks
```

### 2. Check AI Engine

```bash
curl http://localhost:8000/health
# Should return: {"status":"healthy"}

# Test prediction
curl -X POST http://localhost:8000/predict \
  -H "Content-Type: application/json" \
  -d '{
    "fen": "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
    "playerColor": "white"
  }'
```

### 3. Test Full Flow

```bash
# 1. Create game
GAME_RESPONSE=$(curl -X POST http://localhost:8080/api/game/create \
  -H "Content-Type: application/json" \
  -d '{"mode":"AI","timeControl":{"minutes":10,"increment":5}}')

GAME_ID=$(echo $GAME_RESPONSE | jq -r '.gameId')
echo "Game ID: $GAME_ID"

# 2. Request AI move
curl -X POST http://localhost:8080/api/game/ai-move \
  -H "Content-Type: application/json" \
  -d "{
    \"gameId\": \"$GAME_ID\",
    \"fen\": \"rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1\",
    \"playerColor\": \"black\"
  }"
```

### 4. Run Desktop Client

```bash
cd desktop-client
./mvnw javafx:run

# Should open a window with 3D chess board
# - Click squares to select pieces
# - Drag to move
# - Use mouse to rotate camera
# - Scroll to zoom
```

## Troubleshooting

### Desktop Client Issues

**OpenGL not available:**
```bash
# Check graphics drivers
# Windows: Update GPU drivers from manufacturer website
# Linux: Install mesa-utils, run `glxinfo | grep OpenGL`
# macOS: Should work out of the box on modern systems
```

**LWJGL native library errors:**
```bash
# Natives are auto-downloaded by Maven
# If issues persist, manually download for your platform:
# https://www.lwjgl.org/customize

# Verify LWJGL natives in .m2 repository:
ls ~/.m2/repository/org/lwjgl/lwjgl/3.3.3/
```

**JavaFX not found:**
```bash
# Ensure Java 17+ with JavaFX support
# Or use OpenJFX:
java --module-path /path/to/javafx-sdk/lib \
     --add-modules javafx.controls,javafx.fxml \
     -jar target/chess-client.jar
```

### Backend Issues

**Database connection failed:**
```bash
# Check PostgreSQL is running
docker ps | grep postgres

# Test connection
pg_isready -h localhost -p 5432

# Check credentials in .env or application.properties
```

**Redis connection failed:**
```bash
# Check Redis is running
docker ps | grep redis

# Test connection
redis-cli -h localhost -p 6379 ping
# Should return: PONG
```

**AI service timeout:**
```bash
# Check AI service logs
docker-compose logs ai-engine

# Increase timeout in application.properties:
quarkus.rest-client.ai-service.read-timeout=60000
```

### AI Engine Issues

**Model not found:**
```bash
# Check models directory
ls ai-engine/models/

# Download pre-trained model (if available)
# Or train a new model (see section above)
```

**CUDA/GPU errors:**
```bash
# If GPU not available, use CPU mode
# In docker-compose.yml or .env:
DEVICE=cpu

# Or in Python:
export DEVICE=cpu
python -m uvicorn app.main:app
```

## Performance Optimization

### Desktop Client

```properties
# In desktop-client VM options:
-Xmx2G                    # Increase heap size
-XX:+UseG1GC              # Use G1 garbage collector
-Dprism.order=sw          # Force software rendering if GPU issues
-Djavafx.animation.fullspeed=true  # Uncapped framerate
```

### Backend

```properties
# In application.properties:
quarkus.thread-pool.max-threads=200
quarkus.datasource.jdbc.max-size=20
quarkus.redis.max-pool-size=50
```

### AI Engine

```python
# Use ONNX Runtime for faster inference
ONNX_RUNTIME=true
BATCH_SIZE=16
MAX_WORKERS=4

# Enable GPU (if available)
DEVICE=cuda
```

## Next Steps

1. Read [ARCHITECTURE.md](ARCHITECTURE.md) for system design details
2. Check [SECURITY.md](SECURITY.md) for security best practices
3. Explore API docs at http://localhost:8080/q/swagger-ui
4. View metrics at http://localhost:9090 (Prometheus)
5. Create dashboards at http://localhost:3001 (Grafana)

## Common Commands

```bash
# Full restart
docker-compose down -v
docker-compose up --build

# View logs
docker-compose logs -f backend
docker-compose logs -f ai-engine

# Rebuild desktop client
cd desktop-client && ./mvnw clean package

# Run tests
cd quarkus-backend && ./mvnw test
cd ai-engine && pytest
cd react-admin && npm test

# Create native executable (Quarkus)
cd quarkus-backend
./mvnw package -Pnative
# Binary will be in target/chess-backend-1.0.0-runner

# Create installer (Desktop)
cd desktop-client
jpackage --input target/ --name ChessGame \
  --main-jar chess-desktop-client-1.0.0.jar \
  --type exe --win-dir-chooser --win-menu --win-shortcut
```

## Support

- Issues: GitHub Issues
- Documentation: `/docs` directory
- Community: Discord server

---

**Ready to play chess!** Start the desktop client and create a game.
