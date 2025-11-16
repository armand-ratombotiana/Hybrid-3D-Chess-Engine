# Chess AI Platform Architecture

## System Overview

The Chess AI Platform is a distributed, microservices-based system for playing, analyzing, and spectating chess games with AI opponents.

```
┌─────────────────────────────────────────────────────────────────┐
│                      Chess AI Platform                           │
│                                                                   │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │   Desktop    │  │   React      │  │   Mobile     │          │
│  │   JavaFX     │  │   Admin      │  │   (Future)   │          │
│  │  + LWJGL     │  │              │  │              │          │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘          │
│         │                  │                  │                  │
│         └──────────────────┼──────────────────┘                  │
│                            │                                     │
│         ┌──────────────────▼─────────────────┐                  │
│         │      Quarkus Backend               │                  │
│         │  ┌──────────┐  ┌──────────┐       │                  │
│         │  │ REST API │  │WebSocket │       │                  │
│         │  └────┬─────┘  └────┬─────┘       │                  │
│         │       │             │              │                  │
│         │  ┌────┴─────────────┴─────┐       │                  │
│         │  │   Game Engine + Logic  │       │                  │
│         │  └────────────┬───────────┘       │                  │
│         └───────────────┼───────────────────┘                  │
│                         │                                       │
│         ┌───────────────┼───────────────┐                      │
│         │               │               │                      │
│    ┌────▼────┐    ┌────▼─────┐   ┌────▼─────┐                │
│    │ FastAPI │    │PostgreSQL│   │  Redis   │                │
│    │AI Engine│    │          │   │  Cache   │                │
│    │         │    │          │   │          │                │
│    │┌───────┐│    └──────────┘   └──────────┘                │
│    ││PyTorch││                                                 │
│    ││NN +   ││                                                 │
│    ││Minimax││                                                 │
│    │└───────┘│                                                 │
│    └─────────┘                                                 │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

## Component Details

### 1. Desktop Client (JavaFX + LWJGL)

**Technology Stack:**
- Java 17 (LTS)
- JavaFX 21 (UI framework)
- LWJGL 3.3.3 (OpenGL bindings)
- JOML (Math library)
- Jackson (JSON)
- Tyrus (WebSocket client)

**Responsibilities:**
- 3D chess board rendering with OpenGL
- Camera controls (orbit, pan, zoom)
- Piece selection and movement via ray-picking
- Local two-player mode
- Online multiplayer via WebSocket
- AI opponent integration

**Architecture Pattern:** MVC
- `ChessApplication`: Main entry point
- `OpenGLRenderer`: Offscreen OpenGL rendering
- `MainController`: JavaFX UI controller
- `GameClient`: Network communication
- `Board`, `Piece`, `Square`: Game model

**Rendering Pipeline:**
1. GLFW creates hidden window with OpenGL context
2. Render scene to framebuffer texture
3. Read pixels from framebuffer
4. Copy to JavaFX `WritableImage`
5. Display in `ImageView`

**Performance:**
- Target 60 FPS
- VAO/VBO for static geometry
- Instancing for repeated pieces
- Frustum culling
- Mipmapping for textures

### 2. Quarkus Backend

**Technology Stack:**
- Quarkus 3.7 (Reactive)
- Vert.x (Async I/O)
- Hibernate ORM with Panache
- PostgreSQL (Persistence)
- Redis (Caching)
- SmallRye JWT (Security)
- Micrometer (Metrics)

**Responsibilities:**
- User authentication and authorization
- Game lifecycle management
- Move validation and game rules
- WebSocket real-time updates
- AI service integration
- Leaderboard and statistics

**Key Endpoints:**
- `POST /api/auth/login` - Authentication
- `POST /api/auth/register` - User registration
- `POST /api/game/create` - Create game
- `POST /api/game/move` - Make move
- `POST /api/game/ai-move` - Request AI move
- `GET /api/game/list` - List games
- `WS /ws/game/{gameId}` - Live game updates

**Database Schema:**
```sql
users:
  - id (UUID, PK)
  - username (VARCHAR, UNIQUE)
  - email (VARCHAR, UNIQUE)
  - password_hash (VARCHAR)
  - role (ENUM: PLAYER, ADMIN)
  - created_at (TIMESTAMP)

games:
  - id (UUID, PK)
  - white_player_id (UUID, FK -> users)
  - black_player_id (UUID, FK -> users)
  - status (ENUM: IN_PROGRESS, WHITE_WIN, BLACK_WIN, DRAW)
  - current_fen (TEXT)
  - mode (ENUM: LOCAL, AI, ONLINE)
  - created_at (TIMESTAMP)
  - updated_at (TIMESTAMP)

moves:
  - id (BIGSERIAL, PK)
  - game_id (UUID, FK -> games)
  - move_number (INT)
  - from_square (VARCHAR(2))
  - to_square (VARCHAR(2))
  - piece_type (VARCHAR(10))
  - captured_piece (VARCHAR(10), NULL)
  - promotion (VARCHAR(10), NULL)
  - fen_after (TEXT)
  - timestamp (TIMESTAMP)
```

**Caching Strategy:**
- Redis for ephemeral game state (TTL 1 hour)
- Cache key: `game:{gameId}:state`
- Invalidate on move
- Leaderboard cached with 5min TTL

**Security:**
- JWT tokens (15min expiry, refresh 7 days)
- BCrypt password hashing (cost 12)
- RBAC (Role-Based Access Control)
- Rate limiting (100 req/min per user)
- Input validation via Bean Validation

### 3. AI Engine (FastAPI + PyTorch)

**Technology Stack:**
- Python 3.10
- FastAPI (API framework)
- PyTorch 2.1 (Neural networks)
- python-chess (Chess logic)
- ONNX Runtime (Optimized inference)
- TensorBoard (Training metrics)

**Responsibilities:**
- Move prediction via neural network
- Fallback to minimax engine
- Model training and versioning
- Self-play data generation
- ONNX export for production

**Neural Network Architecture:**
- Input: 8×8×12 tensor (piece positions)
- Convolutional layers (AlphaZero-inspired)
- Residual blocks (10 blocks, 256 filters)
- Policy head: 4096 outputs (all possible moves)
- Value head: [-1, 1] position evaluation

**Training Pipeline:**
1. Self-play game generation
2. Position-outcome pairs collected
3. Supervised learning on dataset
4. Reinforcement learning via policy gradient
5. Model checkpointing every N epochs
6. ONNX export for production

**Inference Optimization:**
- Batched requests (dynamic batching)
- ONNX Runtime (2-3x faster than PyTorch)
- GPU support (CUDA)
- Model quantization (INT8)
- Result caching for identical positions

**Fallback Minimax Engine:**
- Alpha-beta pruning
- Depth 3-5 (configurable)
- Piece-square tables for position evaluation
- Move ordering for better pruning
- ~1000-10000 nodes/sec

### 4. React Admin Dashboard

**Technology Stack:**
- React 18
- TypeScript
- Vite (Build tool)
- Zustand (State management)
- TailwindCSS (Styling)
- react-chessboard (Board component)
- Axios (HTTP client)

**Features:**
- User authentication
- Live game spectating
- Active games dashboard
- Player leaderboard
- AI model metrics
- Admin controls

**WebSocket Integration:**
- Auto-reconnect on disconnect
- Heartbeat every 30s
- Message queue for missed updates
- Optimistic UI updates

### 5. Infrastructure

**PostgreSQL:**
- Version: 15
- Connection pooling: HikariCP (max 20)
- Indexes: username, email, game.status, game.created_at
- Full-text search on game metadata
- Backup: Daily automated backups

**Redis:**
- Version: 7
- Persistence: AOF (Append-Only File)
- Max memory: 2GB with LRU eviction
- Use cases: sessions, game state cache, rate limiting

**Monitoring:**
- Prometheus metrics collection
- Grafana dashboards
- Health checks (liveness, readiness)
- Structured JSON logging
- Distributed tracing (Jaeger)

**Docker Compose Services:**
- `postgres`: Database
- `redis`: Cache
- `backend`: Quarkus app
- `ai-engine`: FastAPI app
- `react-admin`: Nginx static server
- `prometheus`: Metrics
- `grafana`: Visualization

## Data Flow

### Game Creation
```
Desktop Client
  └─> POST /api/game/create
       └─> Quarkus validates request
            └─> Create Game entity (Panache)
                 └─> Save to PostgreSQL
                      └─> Cache in Redis
                           └─> Return gameId
```

### AI Move Request
```
Desktop Client
  └─> POST /api/game/ai-move {fen, gameId}
       └─> Quarkus backend
            └─> REST Client → POST /predict {fen}
                 └─> FastAPI AI Engine
                      ├─> Try Neural Network
                      │    └─> Encode FEN to tensor
                      │         └─> Model inference
                      │              └─> Return move + score
                      │
                      └─> Fallback to Minimax
                           └─> Alpha-beta search
                                └─> Return move + score
            └─> Validate move legality
                 └─> Update game state
                      └─> Save to DB + cache
                           └─> WebSocket broadcast
                                └─> Return move to client
```

### Live Spectator
```
React Client
  └─> Connect WS /ws/game/{gameId}
       └─> Authenticate via JWT
            └─> Subscribe to game updates
                 └─> On move:
                      ├─> Backend emits {type: MOVE, fen, ...}
                      └─> Client receives message
                           └─> Update chessboard component
                                └─> Append to move history
```

## Scalability Considerations

### Horizontal Scaling

**Backend:**
- Stateless design (session in JWT)
- Load balancer (Nginx/HAProxy)
- Multiple Quarkus instances
- Sticky sessions for WebSocket (optional)

**AI Engine:**
- Queue-based architecture (Celery + RabbitMQ)
- Worker pool for inference
- Separate training nodes (GPU)
- Result caching (Redis)

**Database:**
- Read replicas for queries
- Write to master
- Connection pooling
- Partitioning by date (moves table)

### Vertical Scaling

**Backend:**
- Increase JVM heap (Xmx)
- Thread pool tuning
- Native image (GraalVM) for lower memory

**AI Engine:**
- GPU for inference (CUDA)
- Larger batch sizes
- Model parallelism

### Caching Strategy

- **L1**: In-memory (Caffeine cache)
- **L2**: Redis (distributed)
- **CDN**: Static assets (Cloudflare)

### Performance Targets

| Metric | Target |
|--------|--------|
| API Latency (p99) | < 200ms |
| AI Move Latency | < 2s |
| WebSocket Latency | < 50ms |
| Database Query | < 50ms |
| Throughput | 1000 req/sec |

## Security Architecture

### Authentication Flow

```
1. User → POST /api/auth/login {username, password}
2. Backend validates credentials (BCrypt)
3. Generate JWT (RSA-256 signed)
4. Return {token, refresh_token}
5. Client stores in localStorage
6. All subsequent requests: Authorization: Bearer <token>
```

### JWT Claims
```json
{
  "sub": "user-uuid",
  "upn": "username",
  "role": "PLAYER",
  "iat": 1234567890,
  "exp": 1234568790,
  "iss": "chess-platform"
}
```

### Security Measures

- **Transport**: HTTPS/TLS in production
- **Passwords**: BCrypt (cost 12)
- **CSRF**: SameSite cookies + CSRF tokens
- **XSS**: Content Security Policy headers
- **SQL Injection**: Parameterized queries (JPA)
- **Rate Limiting**: 100 req/min per IP
- **WebSocket Auth**: JWT in connection params

## Deployment

### Development
```bash
docker-compose up
```

### Production (Kubernetes)

```yaml
Deployments:
  - quarkus-backend (replicas: 3)
  - ai-engine (replicas: 2, GPU node)
  - react-admin (replicas: 2)

Services:
  - backend-svc (ClusterIP)
  - ai-engine-svc (ClusterIP)
  - admin-svc (LoadBalancer)

Ingress:
  - TLS termination
  - Path routing
  - Rate limiting

Persistence:
  - PostgreSQL StatefulSet
  - Redis StatefulSet
  - PVC for models
```

### CI/CD Pipeline

```
GitHub Push
  └─> GitHub Actions
       ├─> Run tests
       ├─> Build Docker images
       ├─> Push to registry
       └─> Deploy to K8s (ArgoCD)
```

## Future Enhancements

1. **Mobile App** (React Native)
2. **Tournament System** with brackets
3. **Puzzle Training** mode
4. **Opening Book** integration
5. **Endgame Tablebase** (Syzygy)
6. **Analysis Board** with stockfish
7. **Social Features** (friends, chat)
8. **Streaming** (Twitch integration)
9. **Monetization** (premium subscriptions)
10. **Blockchain** (NFT pieces, play-to-earn)

## References

- [Quarkus Documentation](https://quarkus.io)
- [LWJGL Documentation](https://www.lwjgl.org)
- [AlphaZero Paper](https://arxiv.org/abs/1712.01815)
- [Chess Programming Wiki](https://www.chessprogramming.org)
