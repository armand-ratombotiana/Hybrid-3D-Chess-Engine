#!/bin/bash

# Script to generate remaining boilerplate files for Chess AI Platform
# Run this after the initial scaffold is created

set -e

echo "Generating remaining project files..."

# Create directory structure
mkdir -p quarkus-backend/src/main/java/com/chess/backend/{model,dto,service,engine,websocket,security,client,repository}
mkdir -p quarkus-backend/src/main/resources/META-INF/resources
mkdir -p quarkus-backend/src/test/java/com/chess/backend

mkdir -p ai-engine/app/{api,models,engine,utils,training}
mkdir -p ai-engine/{models,training_data,logs}

mkdir -p react-admin/src/{components,hooks,services,store,types,pages}
mkdir -p react-admin/public

mkdir -p monitoring

echo "Directory structure created"

# Generate JWT keys for Quarkus
if [ ! -f quarkus-backend/src/main/resources/META-INF/resources/privateKey.pem ]; then
    echo "Generating RSA key pair for JWT..."
    openssl genrsa -out quarkus-backend/src/main/resources/META-INF/resources/privateKey.pem 2048
    openssl rsa -in quarkus-backend/src/main/resources/META-INF/resources/privateKey.pem \
                -outform PEM -pubout \
                -out quarkus-backend/src/main/resources/META-INF/resources/publicKey.pem
fi

# Generate .gitignore files
cat > .gitignore << 'EOF'
# Compiled class files
*.class
target/
build/

# Log files
*.log
logs/

# Package Files
*.jar
*.war
*.nar
*.ear
*.zip
*.tar.gz
*.rar

# IDE
.idea/
*.iml
.vscode/
.DS_Store

# Maven
.mvn/
!.mvn/wrapper/maven-wrapper.jar

# Gradle
.gradle/
!gradle/wrapper/gradle-wrapper.jar

# Node
node_modules/
dist/
.cache/

# Python
__pycache__/
*.py[cod]
*$py.class
*.so
.Python
venv/
ENV/
.venv

# Models and data
ai-engine/models/*.pth
ai-engine/models/*.onnx
ai-engine/training_data/*.pgn
!ai-engine/models/.gitkeep

# Environment
.env
.env.local

# Docker
.docker/

# Temp
*.tmp
*.swp
EOF

# Generate README for each module
cat > desktop-client/README.md << 'EOF'
# Chess Desktop Client (JavaFX + LWJGL)

## Build

```bash
./mvnw clean package
```

## Run

```bash
./mvnw javafx:run
```

## Create Native Installer

```bash
jpackage --input target/ --name ChessGame --main-jar chess-desktop-client-1.0.0.jar --type msi
```

## LWJGL Integration

This client uses a hybrid approach:
- JavaFX for UI controls and overlays
- LWJGL (OpenGL) for 3D chess board rendering

The OpenGL context renders to an offscreen framebuffer, which is then copied to a JavaFX ImageView for display.

## Controls

- **Left Mouse**: Select and move pieces
- **Right Mouse + Drag**: Rotate camera
- **Scroll Wheel**: Zoom
- **Arrow Keys**: Pan camera

## Configuration

Edit VM options in your IDE or Maven plugin configuration:

```
--add-modules javafx.controls,javafx.fxml
--add-exports javafx.graphics/com.sun.javafx.application=ALL-UNNAMED
-Xmx2G
```
EOF

cat > quarkus-backend/README.md << 'EOF'
# Chess Backend (Quarkus)

## Development Mode

```bash
./mvnw quarkus:dev
```

## Production Build (JVM)

```bash
./mvnw package -Dquarkus.package.type=uber-jar
java -jar target/chess-backend-1.0.0-runner.jar
```

## Native Build (GraalVM)

```bash
./mvnw package -Pnative
./target/chess-backend-1.0.0-runner
```

## Docker Build

```bash
docker build -f src/main/docker/Dockerfile.jvm -t chess/backend:latest .
```

## API Endpoints

- `GET /q/health` - Health check
- `GET /q/metrics` - Prometheus metrics
- `POST /api/auth/login` - User authentication
- `POST /api/game/create` - Create new game
- `POST /api/game/ai-move` - Request AI move
- `WS /ws/game/{gameId}` - WebSocket for live updates
EOF

cat > ai-engine/README.md << 'EOF'
# Chess AI Engine (FastAPI + PyTorch)

## Setup

```bash
python -m venv venv
source venv/bin/activate
pip install -r requirements.txt
```

## Run

```bash
uvicorn app.main:app --reload --port 8000
```

## Train Model

```bash
python -m app.training.train --epochs 50 --batch-size 64
```

## Export to ONNX

```bash
python -m app.models.export_onnx --model models/checkpoint_50.pth --output models/chess_model.onnx
```

## API Endpoints

- `GET /health` - Health check
- `POST /predict` - Get AI move prediction
- `POST /train` - Trigger training job (admin only)
- `GET /models` - List available models
EOF

cat > react-admin/README.md << 'EOF'
# Chess Admin Dashboard (React)

## Setup

```bash
npm install
```

## Development

```bash
npm run dev
```

## Build

```bash
npm run build
```

## Environment Variables

Create `.env.local`:

```
REACT_APP_API_URL=http://localhost:8080
REACT_APP_WS_URL=ws://localhost:8080/ws
```

## Features

- Live game spectating
- Player management
- AI model metrics
- Tournament management
- System health monitoring
EOF

echo "Creating placeholder files..."

# Create .gitkeep files
touch ai-engine/models/.gitkeep
touch ai-engine/training_data/.gitkeep
touch quarkus-backend/src/test/java/com/chess/backend/.gitkeep

echo "✓ Project structure generation complete!"
echo ""
echo "Next steps:"
echo "1. Run: chmod +x generate-remaining-files.sh"
echo "2. Run: ./generate-remaining-files.sh"
echo "3. Follow QUICKSTART.md to start the platform"
echo "4. Implement remaining TODOs in generated files"

# List of files that need to be created manually or by additional scripts
echo ""
echo "Remaining files to create:"
echo "  - Quarkus backend: Model entities, Services, WebSocket handlers"
echo "  - AI Engine: Neural network models, training scripts"
echo "  - React Admin: Components, pages, services"
echo "  - Tests for all modules"
echo ""
echo "See project documentation for implementation details."
