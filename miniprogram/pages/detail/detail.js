const api = require('../../utils/api')

Page({
  data: {
    note: null,
    tags: []
  },

  onLoad(options) {
    this.loadDetail(options.id)
  },

  async loadDetail(id) {
    wx.showLoading({ title: '加载中' })
    try {
      const note = await api.getNoteDetail(id)
      let tags = []
      if (note.knowledgeTags) {
        try {
          tags = JSON.parse(note.knowledgeTags)
        } catch (e) {}
      }
      this.setData({ note, tags })
    } catch (e) {
    } finally {
      wx.hideLoading()
    }
  },

  onGeneratePractice() {
    wx.showLoading({ title: '生成题目中...' })
    api.generatePractice(this.data.note.id)
      .then(() => {
        wx.hideLoading()
        wx.navigateTo({ url: `/pages/practice/practice?noteId=${this.data.note.id}` })
      })
      .catch(() => {
        wx.hideLoading()
      })
  },

  onMarkMastered() {
    api.updateNoteStatus(this.data.note.id, 1)
      .then(() => {
        wx.showToast({ title: '已标记为掌握', icon: 'success' })
        this.setData({ 'note.status': 1 })
      })
  },

  onPreviewImage() {
    wx.previewImage({
      current: this.data.note.imageUrl,
      urls: [this.data.note.imageUrl]
    })
  }
})
