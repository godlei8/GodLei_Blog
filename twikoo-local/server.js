const http = require('http')

// 必须在加载 twikoo 之前设置环境变量
process.env.MONGODB_URI = 'mongodb://localhost:27017/twikoo'
process.env.TWIKOO_ADMIN_PASS = 'admin123'
process.env.SITE_NAME = 'GodLei Blog'
process.env.SITE_URL = 'https://blog.godlei.cn'
// 禁用限流（开发环境）
process.env.TWIKOO_RATE_LIMIT = '0'

const twikoo = require('twikoo-vercel')

const PORT = 3000

const server = http.createServer(async (req, res) => {
  // 设置 CORS 头
  res.setHeader('Access-Control-Allow-Origin', '*')
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization')
  res.setHeader('Access-Control-Allow-Methods', 'POST, OPTIONS')
  res.setHeader('Content-Type', 'application/json')

  // 处理预检请求
  if (req.method === 'OPTIONS') {
    res.statusCode = 204
    res.end()
    return
  }

  // 只接受 POST 请求
  if (req.method !== 'POST') {
    res.statusCode = 405
    res.end(JSON.stringify({ error: 'Method not allowed' }))
    return
  }

  try {
    // 解析请求体
    let body = ''
    req.on('data', chunk => {
      body += chunk.toString()
    })

    req.on('end', async () => {
      try {
        const parsedBody = JSON.parse(body)

        // 获取请求 IP
        const requestIp = req.headers['x-forwarded-for'] || req.headers['x-real-ip'] || req.socket.remoteAddress || '127.0.0.1'

        // 创建 req 对象给 Twikoo
        const twikooReq = {
          body: parsedBody,
          headers: req.headers,
          ip: typeof requestIp === 'string' ? requestIp.split(',')[0].trim() : requestIp
        }

        // 创建 res 对象给 Twikoo
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

        // 调用 Twikoo
        await twikoo(twikooReq, twikooRes)

        // 返回响应
        res.statusCode = twikooRes.statusCode
        Object.entries(twikooRes.headers).forEach(([key, value]) => {
          res.setHeader(key, value)
        })
        res.end(JSON.stringify(twikooRes.body))
      } catch (error) {
        console.error('Twikoo error:', error)
        res.statusCode = 500
        res.end(
          JSON.stringify({
            error: error.message,
            code: error.code || 'UNKNOWN_ERROR'
          })
        )
      }
    })
  } catch (error) {
    console.error('Request error:', error)
    res.statusCode = 500
    res.end(
      JSON.stringify({
        error: error.message,
        code: 'REQUEST_ERROR'
      })
    )
  }
})

server.listen(PORT, () => {
  console.log(`Twikoo server running at http://localhost:${PORT}`)
  console.log(`MongoDB: ${process.env.MONGODB_URI}`)
})
