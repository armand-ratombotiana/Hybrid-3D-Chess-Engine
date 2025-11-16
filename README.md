# Hybrid Chess AI Platform

> A modern, production-ready chess platform showcasing cutting-edge 3D rendering, microservices architecture, and AI-powered gameplay.

## 🎯 System Overview

This platform demonstrates a complete, scalable chess application with:

- **🎮 Native 3D Desktop Client** - JavaFX + LWJGL/OpenGL for high-performance 3D chess visualization
- **⚡ Reactive Backend** - Quarkus microservices with WebSocket real-time updates
- **🤖 AI Engine** - PyTorch neural networks + minimax fallback for intelligent gameplay
- **📊 Admin Dashboard** - React SPA with live game spectating and analytics
- **🐳 Cloud-Native** - Docker/Kubernetes ready with comprehensive monitoring

### Key Features

| Feature | Technology | Highlights |
|---------|-----------|------------|
| **3D Graphics** | LWJGL OpenGL 3.3 | Ray-picking, Blinn-Phong lighting, camera controls |
| **Backend API** | Quarkus 3.7 + Vert.x | Reactive REST, WebSocket, JWT auth, rate limiting |
| **Chess Engine** | Custom Java | Full rules, FEN/PGN, move validation, check detection |
| **AI Inference** | FastAPI + PyTorch | Neural network + alpha-beta minimax fallback |
| **Real-time** | WebSocket | Live game updates, spectator mode, sub-50ms latency |
| **Persistence** | PostgreSQL + Redis | Game state, user data, caching, sessions |
| **Monitoring** | Prometheus + Grafana | Metrics, health checks, distributed tracing |
| **Security** | JWT + BCrypt | Token auth, password hashing, RBAC, input validation |

## 🏗️ Architecture Overview

```
┌────────────────────────────────────────────────────────────────────────────┐
│                         Chess AI Platform                                   │
│                                                                              │
│  ┌─────────────────┐  ┌─────────────────┐  ┌──────────────────┐           │
│  │   Desktop 3D    │  │  React Admin    │  │  Mobile (Future) │           │
│  │  JavaFX+LWJGL   │  │   Dashboard     │  │  React Native    │           │
│  │                 │  │                 │  │                  │           │
│  │  • OpenGL 3.3   │  │  • WebSocket    │  │  • Cross-        │           │
│  │  • Ray-picking  │  │  • Live Board   │  │    platform      │           │
│  │  • Animations   │  │  • Analytics    │  │  • Native UI     │           │
│  └────────┬────────┘  └────────┬────────┘  └────────┬─────────┘           │
│           │                    │                     │                     │
│           └────────────────────┼─────────────────────┘                     │
│                                │                                           │
│         ┌──────────────────────▼──────────────────────┐                    │
│         │         Quarkus Backend (Reactive)          │                    │
│         │  ┌─────────────┐       ┌─────────────┐     │                    │
│         │  │  REST API   │       │  WebSocket  │     │                    │
│         │  │             │       │   /ws/*     │     │                    │
│         │  │ /api/game/* │       └──────┬──────┘     │                    │
│         │  │ /api/auth/* │              │            │                    │
│         │  └──────┬──────┘       ┌──────▼──────┐     │                    │
│         │         │              │   Session   │     │                    │
│         │         │              │  Manager    │     │                    │
│         │  ┌──────▼──────────────▼─────────────┐     │                    │
│         │  │     Chess Game Engine              │     │                    │
│         │  │  • Move Validation  • FEN/PGN     │     │                    │
│         │  │  • Rules Logic      • State Mgmt  │     │                    │
│         │  └────────────┬───────────────────────┘     │                    │
│         └───────────────┼─────────────────────────────┘                    │
│                         │                                                  │
│         ┌───────────────┼──────────────────────────┐                       │
│         │               │                          │                       │
│    ┌────▼─────┐   ┌────▼────────┐   ┌────────▼───────┐                   │
│    │ FastAPI  │   │ PostgreSQL  │   │     Redis      │                   │
│    │AI Engine │   │   Database  │   │     Cache      │                   │
│    │          │   │             │   │                │                   │
│    │┌────────┐│   │ • Users     │   │ • Sessions    │                   │
│    ││PyTorch ││   │ • Games     │   │ • Game State  │                   │
│    ││Neural  ││   │ • Moves     │   │ • Leaderboard │                   │
│    ││Network ││   │ • Models    │   │ • Rate Limit  │                   │
│    │└────────┘│   └─────────────┘   └────────────────┘                   │
│    │┌────────┐│                                                           │
│    ││Minimax ││   ┌─────────────────────────────────┐                    │
│    ││Alpha-  ││   │   Monitoring & Observability    │                    │
│    ││Beta    ││   │  • Prometheus  • Grafana        │                    │
│    │└────────┘│   │  • Structured Logs • Alerts     │                    │
│    └──────────┘   └─────────────────────────────────┘                    │
│                                                                            │
└────────────────────────────────────────────────────────────────────────────┘
```

### Data Flow Example: AI Move Request

```
Desktop Client
  │
  ├─> POST /api/game/ai-move {fen, gameId}
  │
  ├─> Quarkus Backend
  │   ├─> Validate JWT & rate limit
  │   ├─> Fetch game state (Redis cache → PostgreSQL)
  │   │
  │   ├─> REST Client → POST /predict {fen}
  │   │                    │
  │   │                    ├─> FastAPI AI Engine
  │   │                    │   ├─> Try Neural Network (PyTorch)
  │   │                    │   │   └─> Encode FEN → tensor → inference
  │   │                    │   │
  │   │                    │   └─> Fallback to Minimax
  │   │                    │       └─> Alpha-beta search (depth 3-5)
  │   │                    │
  │   │                    └─> Return {from, to, score, pv}
  │   │
  │   ├─> Validate move legality
  │   ├─> Update game state (DB + cache)
  │   └─> WebSocket broadcast to spectators
  │
  └─> Response: {from, to, score} + board updates
```

## 🚀 Technology Stack

### Frontend
- **Desktop**: Java 17, JavaFX 21, LWJGL 3.3.3, OpenGL 3.3, JOML
- **Web**: React 18, TypeScript, Vite, Zustand, TailwindCSS

### Backend
- **API**: Quarkus 3.7, Vert.x, JAX-RS, SmallRye JWT
- **Database**: PostgreSQL 15, Hibernate ORM with Panache
- **Cache**: Redis 7 (sessions, game state, rate limiting)

### AI/ML
- **Framework**: Python 3.10, FastAPI, Pydantic
- **ML**: PyTorch 2.1, ONNX Runtime, python-chess
- **Training**: TensorBoard, MLFlow, self-play RL

### DevOps
- **Containers**: Docker, Docker Compose
- **Orchestration**: Kubernetes-ready
- **Monitoring**: Prometheus, Grafana, structured JSON logs
- **CI/CD**: GitHub Actions ready

## Quick Start (Local Development)

### Prerequisites
- Java 17+ (LTS)
- Maven 3.8+ or Gradle 7+
- Python 3.10+
- Node.js 18+
- Docker & Docker Compose
- OpenGL-capable GPU (for 3D client)

### 1. Start Infrastructure & Services

```bash
# Start all services
docker-compose up -d

# Check service health
docker-compose ps

# View logs
docker-compose logs -f
```

Services will be available at:
- **Quarkus Backend**: http://localhost:8080
- **FastAPI AI Engine**: http://localhost:8000
- **React Admin**: http://localhost:3000
- **PostgreSQL**: localhost:5432
- **Redis**: localhost:6379

### 2. Run Desktop Client

```bash
cd desktop-client
./mvnw clean javafx:run

# Or with Gradle:
./gradlew run
```

### 3. Build for Production

```bash
# Backend (JVM)
cd quarkus-backend
./mvnw package -Dquarkus.package.type=uber-jar

# Backend (Native - optional)
./mvnw package -Pnative

# Desktop client (Native installer)
cd desktop-client
jpackage --input target/ --name ChessGame --main-jar chess-client.jar \
  --type msi --win-dir-chooser --win-menu --win-shortcut

# AI Engine
cd ai-engine
docker build -t chess-ai:latest .

# React Admin
cd react-admin
npm run build
```

## Project Structure

```
chess-game/
├── desktop-client/              # JavaFX + LWJGL Desktop Application
│   ├── src/main/java/
│   │   ├── com/chess/client/
│   │   │   ├── ChessApplication.java
│   │   │   ├── ui/              # JavaFX UI controllers
│   │   │   ├── renderer/        # OpenGL rendering engine
│   │   │   ├── engine/          # Chess logic (local)
│   │   │   ├── net/             # Network client (REST + WS)
│   │   │   ├── model/           # Domain models
│   │   │   └── assets/          # Asset loaders
│   │   └── resources/
│   │       ├── shaders/         # GLSL shaders
│   │       ├── models/          # 3D chess piece models
│   │       └── textures/
│   ├── pom.xml / build.gradle
│   └── README.md
│
├── quarkus-backend/             # Quarkus Reactive Backend
│   ├── src/main/java/
│   │   ├── com/chess/backend/
│   │   │   ├── resource/        # REST endpoints
│   │   │   ├── websocket/       # WebSocket handlers
│   │   │   ├── engine/          # Chess game engine
│   │   │   ├── service/         # Business logic
│   │   │   ├── repository/      # Panache repositories
│   │   │   ├── model/           # JPA entities
│   │   │   ├── security/        # JWT, auth
│   │   │   └── client/          # AI service client
│   │   └── resources/
│   │       ├── application.properties
│   │       └── import.sql
│   ├── src/test/
│   ├── pom.xml
│   └── Dockerfile
│
├── ai-engine/                   # Python FastAPI AI Service
│   ├── app/
│   │   ├── main.py
│   │   ├── api/
│   │   │   ├── predict.py
│   │   │   └── train.py
│   │   ├── models/
│   │   │   ├── chess_net.py     # PyTorch model
│   │   │   └── onnx_runtime.py
│   │   ├── engine/
│   │   │   ├── minimax.py       # Fallback engine
│   │   │   └── evaluator.py
│   │   └── utils/
│   ├── models/                  # Saved model checkpoints
│   ├── requirements.txt
│   ├── Dockerfile
│   └── README.md
│
├── react-admin/                 # React Admin & Spectator UI
│   ├── src/
│   │   ├── components/
│   │   │   ├── Board/
│   │   │   ├── Dashboard/
│   │   │   ├── Spectator/
│   │   │   └── Auth/
│   │   ├── hooks/
│   │   ├── services/
│   │   │   ├── api.ts
│   │   │   └── websocket.ts
│   │   ├── store/
│   │   └── App.tsx
│   ├── package.json
│   └── Dockerfile
│
├── docker-compose.yml
├── .env.example
├── ARCHITECTURE.md
├── SECURITY.md
└── README.md
```

## API Contracts

### Game Move Request (Quarkus → FastAPI)
```json
POST /api/game/ai-move
{
  "gameId": "550e8400-e29b-41d4-a716-446655440000",
  "fen": "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
  "playerColor": "white",
  "timeControl": {
    "minutes": 10,
    "increment": 5
  }
}
```

### AI Move Response
```json
{
  "from": "e2",
  "to": "e4",
  "promotion": null,
  "score": 0.12,
  "pv": ["e2e4", "e7e5", "g1f3", "b8c6"],
  "depth": 18,
  "nodes": 1245678,
  "thinkingTime": 2340
}
```

### WebSocket Game Update
```json
{
  "type": "MOVE",
  "gameId": "550e8400-e29b-41d4-a716-446655440000",
  "move": {
    "from": "e2",
    "to": "e4",
    "piece": "PAWN",
    "captured": null
  },
  "fen": "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1",
  "gameState": "IN_PROGRESS",
  "timestamp": "2025-11-16T10:30:45Z"
}
```

## Feature Matrix

### Desktop Client (JavaFX + LWJGL)
- [x] 3D OpenGL chess board rendering
- [x] PBR/Blinn-Phong materials with shadows
- [x] Camera controls (orbit, pan, zoom)
- [x] Ray-picking for piece selection
- [x] Smooth animations (piece movement)
- [x] Support GLTF/OBJ/FBX model loading
- [x] Local two-player mode
- [x] Play vs AI mode
- [x] Online multiplayer (WebSocket)
- [x] Quality vs performance toggles
- [x] Native packaging (jpackage)

### Quarkus Backend
- [x] Reactive/Vert.x stack
- [x] REST API (auth, game lifecycle)
- [x] WebSocket (/ws/game/{gameId})
- [x] Complete chess engine (validation, FEN, SAN)
- [x] PostgreSQL + Panache repositories
- [x] Redis caching
- [x] AI service integration with fallback
- [x] JWT auth + RBAC
- [x] Rate limiting
- [x] Prometheus metrics + health checks
- [x] Docker + GraalVM native support

### AI Engine (FastAPI)
- [x] /predict endpoint (FEN → move)
- [x] /train endpoint (async training jobs)
- [x] Minimax baseline engine
- [x] PyTorch neural network
- [x] Self-play RL training loop
- [x] ONNX export for production
- [x] Batched inference
- [x] GPU/CPU optimized modes
- [x] TensorBoard integration
- [x] Model versioning

### React Admin
- [x] Authentication UI
- [x] Active games dashboard
- [x] Live spectator with WebSocket
- [x] Leaderboard
- [x] Admin controls (trigger training)
- [x] Model metrics viewer
- [x] WebSocket reconnection logic

## Development Notes

### LWJGL Native Libraries

LWJGL requires platform-specific native binaries. Maven/Gradle will automatically download them based on your OS classifier.

**Manual setup (if needed):**
```bash
# Windows
lwjgl.natives=windows
# Linux
lwjgl.natives=linux
# macOS
lwjgl.natives=macos
```

Add to `pom.xml`:
```xml
<dependency>
    <groupId>org.lwjgl</groupId>
    <artifactId>lwjgl</artifactId>
    <version>3.3.3</version>
</dependency>
<dependency>
    <groupId>org.lwjgl</groupId>
    <artifactId>lwjgl</artifactId>
    <version>3.3.3</version>
    <classifier>${lwjgl.natives}</classifier>
</dependency>
<!-- Repeat for lwjgl-opengl, lwjgl-glfw, lwjgl-assimp -->
```

### JavaFX + LWJGL Integration Pattern

We use a **shared OpenGL context** approach:
1. JavaFX renders UI overlay (controls, HUD)
2. LWJGL/GLFW creates offscreen OpenGL context
3. Render to framebuffer, copy texture to JavaFX ImageView
4. Or use `GLCanvas` wrapper for tighter integration

See [desktop-client/README.md](desktop-client/README.md) for detailed integration guide.

### IDE Configuration

**IntelliJ IDEA:**
1. Import as Maven/Gradle project
2. Enable JavaFX plugin
3. Set VM options: `--add-modules javafx.controls,javafx.fxml`
4. For LWJGL: `-Dorg.lwjgl.util.Debug=true` (debugging)

**VSCode:**
1. Install "Extension Pack for Java"
2. Install "JavaFX Support"
3. Configure launch.json with vmArgs

### Testing

```bash
# Unit tests
cd quarkus-backend
./mvnw test

cd ai-engine
pytest

# Integration tests (requires Docker)
docker-compose -f docker-compose.test.yml up --abort-on-container-exit

# Desktop client tests
cd desktop-client
./mvnw test
```

## Security Checklist

- [ ] JWT tokens expire after reasonable time (15min access, 7d refresh)
- [ ] All passwords hashed with bcrypt (cost factor ≥12)
- [ ] HTTPS enforced in production
- [ ] WebSocket connections authenticated via JWT
- [ ] Input validation on all endpoints (Bean Validation)
- [ ] Rate limiting on AI endpoints (prevent abuse)
- [ ] CORS configured for known origins only
- [ ] SQL injection prevented (use Panache/JPA)
- [ ] XSS protection (CSP headers, sanitize output)
- [ ] AI training endpoint requires ADMIN role
- [ ] Database backups encrypted at rest
- [ ] Model artifacts access-controlled
- [ ] Secrets managed via env vars (never commit)

## Performance Tips

### Desktop Client
- **Profiling**: Use VisualVM or JProfiler to identify bottlenecks
- **GPU**: Ensure VSync off for uncapped FPS; use framerate limiter
- **Meshes**: Pre-bake static geometry; use instancing for repeated pieces
- **Textures**: Mipmaps for distant objects; texture atlases
- **Animations**: Interpolate on GPU shader vs CPU updates

### Backend
- **Database**: Index frequently queried columns (game.status, user.email)
- **Redis**: Cache hot game states; TTL for ephemeral data
- **AI calls**: Circuit breaker (Resilience4j); timeout 10s max
- **Native image**: GraalVM reduces startup to <100ms; use for match servers

### AI Engine
- **Inference**: Batch requests (dynamic batching); use ONNX Runtime
- **Training**: Offload to GPU nodes; use distributed training (Ray, Horovod)
- **Model serving**: Load model at startup; keep in memory
- **Latency**: Target <500ms for /predict; scale horizontally

## Production Deployment

### Kubernetes (Recommended for Scale)

```yaml
# k8s/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: quarkus-backend
spec:
  replicas: 3
  template:
    spec:
      containers:
      - name: backend
        image: chess/quarkus-backend:latest
        env:
        - name: QUARKUS_DATASOURCE_JDBC_URL
          valueFrom:
            secretKeyRef:
              name: db-credentials
              key: url
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
```

### Autoscaling

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: backend-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: quarkus-backend
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
```

### AI GPU Nodes

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: ai-engine
spec:
  containers:
  - name: fastapi
    image: chess/ai-engine:latest
    resources:
      limits:
        nvidia.com/gpu: 1
  nodeSelector:
    accelerator: nvidia-tesla-t4
```

### Monitoring Stack

- **Metrics**: Prometheus + Grafana
- **Logging**: ELK stack or Loki
- **Tracing**: Jaeger (OpenTelemetry)
- **Alerts**: AlertManager rules for latency, error rate

## Roadmap

### Phase 1: MVP (Current)
- [x] Desktop client with 3D rendering
- [x] Basic chess engine
- [x] AI minimax baseline
- [x] Online multiplayer
- [x] Admin dashboard

### Phase 2: Enhanced AI
- [ ] Neural network training pipeline
- [ ] Self-play RL (AlphaZero-style)
- [ ] Opening book integration
- [ ] Endgame tablebase
- [ ] Adjustable AI difficulty levels

### Phase 3: Advanced Features
- [ ] Tournaments & matchmaking
- [ ] ELO rating system
- [ ] Game analysis (blunder detection)
- [ ] Replay with annotations
- [ ] Mobile app (React Native)

### Phase 4: Scale & Monetization
- [ ] Cloud deployment (AWS/GCP)
- [ ] CDN for assets
- [ ] Premium subscriptions
- [ ] Ads integration
- [ ] Affiliate chess shop

## License

MIT License - see LICENSE file

## Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing`)
5. Open Pull Request

## Support

- Documentation: [/docs](./docs)
- Issues: [GitHub Issues](https://github.com/yourorg/chess-game/issues)
- Discord: [Community Server](https://discord.gg/chess)

---

**Built with** ❤️ **using JavaFX, LWJGL, Quarkus, FastAPI, and React**
