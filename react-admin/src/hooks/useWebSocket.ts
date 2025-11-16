import { useEffect, useState, useRef } from 'react'

export function useWebSocket(url: string) {
  const [lastMessage, setLastMessage] = useState<string | null>(null)
  const [readyState, setReadyState] = useState<number>(WebSocket.CONNECTING)
  const ws = useRef<WebSocket | null>(null)

  useEffect(() => {
    // Create WebSocket connection
    ws.current = new WebSocket(url)

    ws.current.onopen = () => {
      console.log('WebSocket connected:', url)
      setReadyState(WebSocket.OPEN)
    }

    ws.current.onmessage = (event) => {
      setLastMessage(event.data)
    }

    ws.current.onerror = (error) => {
      console.error('WebSocket error:', error)
    }

    ws.current.onclose = () => {
      console.log('WebSocket disconnected')
      setReadyState(WebSocket.CLOSED)

      // Reconnect after 3 seconds
      setTimeout(() => {
        if (ws.current?.readyState === WebSocket.CLOSED) {
          console.log('Attempting to reconnect...')
          // Trigger re-render to create new connection
          setReadyState(WebSocket.CONNECTING)
        }
      }, 3000)
    }

    // Cleanup on unmount
    return () => {
      if (ws.current) {
        ws.current.close()
      }
    }
  }, [url])

  const sendMessage = (message: string) => {
    if (ws.current?.readyState === WebSocket.OPEN) {
      ws.current.send(message)
    } else {
      console.warn('WebSocket is not open. Message not sent.')
    }
  }

  return { lastMessage, readyState, sendMessage }
}
