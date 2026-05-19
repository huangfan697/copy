const api = require('../../utils/api')

Page({
  data: {
    todayDate: '',
    todayErrorRate: 0,
    todayTotal: 0,
    streakDays: 0,
    trainCount: 0,
    yesterdayWrong: 0,
    yesterdayNew: 0,
    notes: [],
    collections: [],
    errorRateClass: ''
  },

  onShow() {
    this.loadAll()
  },

  async loadAll() {
    this.setDate()
    this.loadStats()
    this.loadNotes()
    this.loadTrainData()
    this.loadCollections()
  },

  setDate() {
    const now = new Date()
    const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
    const str = `${now.getMonth() + 1}月${now.getDate()}日 ${weekdays[now.getDay()]}`
    this.setData({ todayDate: str })
  },

  async loadStats() {
    try {
      const data = await api.getErrorRate(7)
      const today = data[data.length - 1]
      const rate = today ? Number(today.errorRate) : 0
      const total = today ? today.totalCount : 0

      // 计算连续天数
      let streak = 0
      for (let i = data.length - 1; i >= 0; i--) {
        if (data[i].totalCount > 0) streak++
        else if (i < data.length - 1) break
      }

      this.setData({
        todayErrorRate: rate,
        todayTotal: total,
        streakDays: streak,
        errorRateClass: rate > 50 ? 'high' : ''
      })
    } catch (e) {}
  },

  async loadTrainData() {
    try {
      const info = await api.getTodayTrainInfo()
      this.setData({
        trainCount: info.totalCount,
        yesterdayWrong: info.yesterdayWrong,
        yesterdayNew: info.yesterdayNew
      })
    } catch (e) {
      this.setData({ trainCount: 0 })
    }
  },

  async loadNotes() {
    try {
      const notes = await api.getNotes({ status: 0 })
      notes.forEach(n => {
        try { n.tagsList = n.knowledgeTags ? JSON.parse(n.knowledgeTags) : [] } catch (e) { n.tagsList = [] }
      })
      this.setData({ notes: notes.slice(0, 5) })
    } catch (e) {}
  },

  onStartTrain() {
    if (this.data.trainCount === 0) {
      wx.showToast({ title: '暂无题目', icon: 'none' })
      return
    }
    wx.navigateTo({ url: '/pages/practice/practice?mode=today' })
  },

  onGoUpload() {
    wx.navigateTo({ url: '/pages/upload/upload' })
  },

  onNoteTap(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: `/pages/detail/detail?id=${id}` })
  },

  async loadCollections() {
    try {
      const collections = await api.getCollections()
      // 格式化日期显示
      collections.forEach(c => {
        if (c.collectionDate) {
          const d = new Date(c.collectionDate)
          c.dateDisplay = `${d.getMonth() + 1}月${d.getDate()}日`
        }
      })
      this.setData({ collections: collections.slice(0, 7) })
    } catch (e) {}
  },

  onCollectionTap(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: `/pages/collection/collection?id=${id}` })
  }
})
