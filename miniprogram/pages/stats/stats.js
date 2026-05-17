const api = require('../../utils/api')

Page({
  data: {
    days: 7,
    chartData: [],
    avgErrorRate: 0,
    totalQuestions: 0,
    totalCorrect: 0
  },

  onShow() {
    this.loadStats()
  },

  async loadStats() {
    wx.showLoading({ title: '加载中' })
    try {
      const data = await api.getErrorRate(this.data.days)
      // 转换为 canvas 折线图数据
      const chartData = data.map(d => ({
        label: `${d.date.slice(5)}`, // MM-DD
        value: Number(d.errorRate),
        total: d.totalCount,
        correct: d.correctCount
      }))

      // 计算汇总
      let totalQ = 0, totalC = 0
      data.forEach(d => {
        totalQ += d.totalCount
        totalC += d.correctCount
      })

      this.setData({
        chartData,
        totalQuestions: totalQ,
        totalCorrect: totalC,
        avgErrorRate: totalQ > 0 ? Math.round((totalQ - totalC) / totalQ * 100) : 0
      })

      this.drawChart(chartData)
    } catch (e) {
    } finally {
      wx.hideLoading()
    }
  },

  onDaysChange(e) {
    this.setData({ days: Number(e.currentTarget.dataset.days) })
    this.loadStats()
  },

  drawChart(data) {
    const query = wx.createSelectorQuery()
    query.select('#chartCanvas')
      .fields({ node: true, size: true })
      .exec((res) => {
        if (!res[0] || !res[0].node) return

        const canvas = res[0].node
        const ctx = canvas.getContext('2d')
        const dpr = wx.getWindowInfo().pixelRatio
        canvas.width = res[0].width * dpr
        canvas.height = res[0].height * dpr
        ctx.scale(dpr, dpr)

        const width = res[0].width
        const height = res[0].height
        const padding = { top: 30, bottom: 40, left: 40, right: 20 }
        const chartW = width - padding.left - padding.right
        const chartH = height - padding.top - padding.bottom

        ctx.clearRect(0, 0, width, height)

        if (data.length === 0) return

        const maxVal = Math.max(...data.map(d => d.value), 100)
        const stepX = chartW / (data.length - 1 || 1)

        // 绘制网格线
        ctx.strokeStyle = '#E5E7EB'
        ctx.lineWidth = 1
        for (let i = 0; i <= 4; i++) {
          const y = padding.top + chartH - (chartH * i / 4)
          ctx.beginPath()
          ctx.moveTo(padding.left, y)
          ctx.lineTo(padding.left + chartW, y)
          ctx.stroke()

          ctx.fillStyle = '#9CA3AF'
          ctx.font = '11px sans-serif'
          ctx.textAlign = 'right'
          ctx.fillText(Math.round(maxVal * i / 4) + '%', padding.left - 5, y + 4)
        }

        // 绘制折线
        ctx.strokeStyle = '#4F46E5'
        ctx.lineWidth = 2
        ctx.beginPath()
        data.forEach((d, i) => {
          const x = padding.left + i * stepX
          const y = padding.top + chartH - (d.value / maxVal * chartH)
          if (i === 0) ctx.moveTo(x, y)
          else ctx.lineTo(x, y)
        })
        ctx.stroke()

        // 绘制数据点
        data.forEach((d, i) => {
          const x = padding.left + i * stepX
          const y = padding.top + chartH - (d.value / maxVal * chartH)

          ctx.fillStyle = '#4F46E5'
          ctx.beginPath()
          ctx.arc(x, y, 4, 0, Math.PI * 2)
          ctx.fill()

          ctx.fillStyle = '#6B7280'
          ctx.font = '10px sans-serif'
          ctx.textAlign = 'center'

          if (data.length <= 15) {
            ctx.fillText(d.label, x, height - 10)
          } else if (i % Math.ceil(data.length / 7) === 0) {
            ctx.fillText(d.label, x, height - 10)
          }
        })
      })
  }
})
