import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { Chessboard } from 'react-chessboard'
import { Chess } from 'chess.js'
import { useWebSocket } from '../hooks/useWebSocket'

interface Move {
  from: string
  to: string
  piece: string
  captured?: string
}

interface GameUpdate {
  type: string
  gameId: string
  move?: Move
  fen?: string
  gameState?: string
}

export default function Spectator() {
  const { gameId } = useParams<{ gameId: string }>()
  const [game, setGame] = useState(new Chess())
  const [moveHistory, setMoveHistory] = useState<string[]>([])
  const [gameState, setGameState] = useState('IN_PROGRESS')

  const { lastMessage, sendMessage } = useWebSocket(
    `ws://localhost:8080/ws/game/${gameId}`
  )

  useEffect(() => {
    if (lastMessage) {
      try {
        const update: GameUpdate = JSON.parse(lastMessage)

        if (update.type === 'MOVE' && update.fen) {
          // Update board with new FEN
          const newGame = new Chess(update.fen)
          setGame(newGame)

          // Add move to history
          if (update.move) {
            setMoveHistory(prev => [
              ...prev,
              `${update.move!.from}→${update.move!.to}`
            ])
          }

          if (update.gameState) {
            setGameState(update.gameState)
          }
        }
      } catch (error) {
        console.error('Failed to parse WebSocket message:', error)
      }
    }
  }, [lastMessage])

  return (
    <div className="container mx-auto p-8">
      <div className="max-w-6xl mx-auto">
        <h1 className="text-3xl font-bold mb-6">Spectating Game: {gameId}</h1>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Chess Board */}
          <div className="lg:col-span-2">
            <div className="bg-gray-800 rounded-lg p-4">
              <Chessboard
                position={game.fen()}
                boardWidth={600}
                arePiecesDraggable={false}
              />
            </div>

            <div className="mt-4 bg-gray-800 rounded-lg p-4">
              <div className="flex justify-between items-center">
                <span className="text-lg">
                  Turn: <strong>{game.turn() === 'w' ? 'White' : 'Black'}</strong>
                </span>
                <span className="text-lg">
                  Status: <strong>{gameState}</strong>
                </span>
              </div>
            </div>
          </div>

          {/* Side Panel */}
          <div className="space-y-4">
            {/* Move History */}
            <div className="bg-gray-800 rounded-lg p-4">
              <h3 className="text-xl font-bold mb-4">Move History</h3>
              <div className="max-h-96 overflow-y-auto">
                {moveHistory.length === 0 ? (
                  <p className="text-gray-400">No moves yet</p>
                ) : (
                  <div className="space-y-1">
                    {moveHistory.map((move, idx) => (
                      <div
                        key={idx}
                        className="flex items-center gap-2 text-sm"
                      >
                        <span className="text-gray-400">{Math.floor(idx / 2) + 1}.</span>
                        <span>{move}</span>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>

            {/* Game Info */}
            <div className="bg-gray-800 rounded-lg p-4">
              <h3 className="text-xl font-bold mb-4">Game Info</h3>
              <div className="space-y-2 text-sm">
                <div className="flex justify-between">
                  <span className="text-gray-400">Game ID:</span>
                  <span className="font-mono">{gameId?.slice(0, 8)}...</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-400">Moves:</span>
                  <span>{moveHistory.length}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-400">Check:</span>
                  <span>{game.isCheck() ? 'Yes' : 'No'}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-400">Checkmate:</span>
                  <span>{game.isCheckmate() ? 'Yes' : 'No'}</span>
                </div>
              </div>
            </div>

            {/* FEN */}
            <div className="bg-gray-800 rounded-lg p-4">
              <h3 className="text-xl font-bold mb-2">FEN</h3>
              <code className="text-xs break-all block bg-gray-900 p-2 rounded">
                {game.fen()}
              </code>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
