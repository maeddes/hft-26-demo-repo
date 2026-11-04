// A minimal REST API with Hono on Node.js.
// Node 24 runs TypeScript directly by stripping the types, so there is no build step.
import { serve } from '@hono/node-server'
import { Hono } from 'hono'
import { hostname } from 'node:os'

type Item = { id: number; name: string }

// Node runs all handlers on one thread, so no lock is needed.
const items = new Map<number, Item>()
let nextId = 1

const app = new Hono()

app.get('/health', (c) => c.json({ status: 'ok' }))

// Returns the hostname, which inside a container is the container ID.
app.get('/hello', (c) => c.json({ message: 'hello from hono', hostname: hostname() }))

app.get('/items', (c) => c.json([...items.values()]))

app.post('/items', async (c) => {
  const body = await c.req.json().catch(() => null)
  if (typeof body?.name !== 'string' || body.name === '') {
    return c.json({ error: 'expected {"name": "..."}' }, 400)
  }
  const item = { id: nextId++, name: body.name }
  items.set(item.id, item)
  return c.json(item, 201)
})

app.get('/items/:id', (c) => {
  const item = items.get(Number(c.req.param('id')))
  if (!item) {
    return c.json({ error: 'not found' }, 404)
  }
  return c.json(item)
})

app.delete('/items/:id', (c) => {
  if (!items.delete(Number(c.req.param('id')))) {
    return c.json({ error: 'not found' }, 404)
  }
  return c.body(null, 204)
})

const port = Number(process.env.PORT ?? 8080)
serve({ fetch: app.fetch, port })
console.log(`listening on :${port}`)
