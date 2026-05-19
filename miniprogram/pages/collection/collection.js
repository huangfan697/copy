const api = require('../../utils/api')

Page({
  data: {
    id: null,
    collection: null,
    notes: [],
    questions: [],
    showQuestions: false,
    loading: false
  },

  onLoad(options) {
    if (options.id) {
      this.setData({ id: options.id })
      this.loadDetail()
    }
  },

  async loadDetail() {
    this.setData({ loading: true })
    try {
      const data = await api.getCollectionDetail(this.data.id)
      const notes = data.notes || []
      notes.forEach(n => {
        try { n.tagsList = n.knowledgeTags ? JSON.parse(n.knowledgeTags) : [] } catch (e) { n.tagsList = [] }
      })
      this.setData({
        collection: data.collection,
        notes,
        questions: data.questions || [],
        loading: false
      })
    } catch (e) {
      this.setData({ loading: false })
    }
  },

  onStartTrain() {
    if (this.data.questions.length === 0) {
      wx.showToast({ title: '暂无练习题', icon: 'none' })
      return
    }
    wx.navigateTo({ url: `/pages/practice/practice?mode=collection&collectionId=${this.data.id}` })
  },

  onNoteTap(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: `/pages/detail/detail?id=${id}` })
  }
})
