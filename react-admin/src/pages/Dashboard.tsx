import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../services/api'

interface Game {
  gameId: string
  status: string
  whitePlayer?: string
  blackPlayer?: string
  createdAt: string
}

export default function Dashboard() {
  const [games, setGames] = useState<Game[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    loadGames()
  }, [])

  const loadGames = async () => {
    try {
      const response = await api.get('/api/game/list?status=IN_PROGRESS')
      setGames(response.data)
    } catch (error) {
      console.error('Failed to load games:', error)
    } finally {
      setLoading(false)
    }
  }

  const createGame = async () => {
    try {
      await api.post('/api/game/create', {
        mode: 'ONLINE',
        timeControl: { minutes: 10, increment: 5 }
      })
      loadGames()
    } catch (error) {
      console.error('Failed to create game:', error)
    }
  }

  return (
    <div className="container mx-auto p-8">
      <div className="max-w-6xl mx-auto">
        <div className="flex justify-between items-center mb-8">
          <h1 className="text-4xl font-bold">Chess Platform Dashboard</h1>
          <button
            onClick={createGame}
            className="bg-green-600 hover:bg-green-700 px-6 py-3 rounded-lg font-semibold"
          >
            Create New Game
          </button>
        </div>

        {/* Stats */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-8">
          <div className="bg-gray-800 rounded-lg p-6">
            <div className="text-gray-400 text-sm">Active Games</div>
            <div className="text-3xl font-bold mt-2">{games.length}</div>
          </div>
          <div className="bg-gray-800 rounded-lg p-6">
            <div className="text-gray-400 text-sm">Total Players</div>
            <div className="text-3xl font-bold mt-2">--</div>
          </div>
          <div className="bg-gray-800 rounded-lg p-6">
            <div className="text-gray-400 text-sm">AI Model</div>
            <div className="text-xl font-bold mt-2">v1.0.0</div>
          </div>
          <div className="bg-gray-800 rounded-lg p-6">
            <div className="text-gray-400 text-sm">Server Status</div>
            <div className="text-xl font-bold mt-2 text-green-500">Online</div>
          </div>
        </div>

        {/* Active Games */}
        <div className="bg-gray-800 rounded-lg p-6">
          <h2 className="text-2xl font-bold mb-4">Active Games</h2>

          {loading ? (
            <div className="text-center py-8">Loading...</div>
          ) : games.length === 0 ? (
            <div className="text-center py-8 text-gray-400">
              No active games. Create one to get started!
            </div>
          ) : (
            <div className="space-y-3">
              {games.map(game => (
                <Link
                  key={game.gameId}
                  to={`/spectator/${game.gameId}`}
                  className="block bg-gray-700 hover:bg-gray-600 rounded-lg p-4 transition"
                >
                  <div className="flex justify-between items-center">
                    <div>
                      <div className="font-semibold">
                        {game.whitePlayer || 'White'} vs {game.blackPlayer || 'Black'}
                      </div>
                      <div className="text-sm text-gray-400">
                        Game ID: {game.gameId.slice(0, 8)}...
                      </div>
                    </div>
                    <div>
                      <span className="px-3 py-1 bg-green-600 rounded-full text-sm">
                        {game.status}
                      </span>
                    </div>
                  </div>
                </Link>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
