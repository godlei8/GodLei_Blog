const http = require('http')

process.env.MONGODB_URI = 'mongodb://localhost:27017/twikoo'
process.env.TWIKOO_ADMIN_PASS = 'admin123'
process.env.SITE_NAME = 'GodLei Blog'
process.env.SITE_URL = 'http://8.137.187.70'
process.env.TWIKOO_RATE_LIMIT = '0'

const twikoo = require('twikoo-vercel')

const PORT = 3000

const server = http.createServer(async (req, res) => {
  res.setHeader('Access-Control-Allow-Origin', '*')
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization')
  res.setHeader('Access-Control-Allow-Methods', 'POST, OPTIONS')
  res.setHeader('Content-Type', 'application/json')

  if (req.method === 'OPTIONS') {
    res.statusCode = 204
    res.end()
    return
  }

  if (req.method !== 'POST') {
    res.statusCode = 405
    res.end(JSON.stringify({ error: 'Method not allowed' }))
    return
  }

  let body = ''
  req.on('data', chunk => {
    body += chunk.toString()
  })

  req.on('end', async () => {
    console.log('DEBUG: Raw body:', body)
    console.log('DEBUG: Content-Type:', req.headers['content-type'])
    
    try {
      const parsedBody = JSON.parse(body)
      const requestIp = req.headers['x-forwarded-for'] ||
                        req.headers['x-real-ip'] ||
                        req.socket.remoteAddress ||
                        '127.0.0.1'

      const twikooReq = {
        body: parsedBody,
        headers: req.headers,
        ip: typeof requestIp === 'string' ? requestIp.split(',')[0].trim() : requestIp
      }

      const twikooRes = {
        statusCode: 200,
        headers: {},
        body: null,
        status(code) {
          this.statusCode = code
          return this
        },
        setHeader(key, value) {
          this.headers[key] = value
          return this
        },
        json(data) {
          this.body = data
          return this
        },
        end() {}
      }

      await twikoo(twikooReq, twikooRes)

      res.statusCode = twikooRes.statusCode
      Object.entries(twikooRes.headers).forEach(([key, value]) => {
        res.setHeader(key, value)
      })
      res.end(JSON.stringify(twikooRes.body))
    } catch (error) {
      console.error('Twikoo error:', error)
      res.statusCode = 500
      res.end(JSON.stringify({
        error: error.message,
        code: error.code || 'UNKNOWN_ERROR',
        rawBody: body
      }))
    }
  })
})

server.listen(PORT, () => {
  console.log('Twikoo server running at http://localhost:' + PORT)
})
