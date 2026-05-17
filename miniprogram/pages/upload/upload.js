const api = require('../../utils/api')

Page({
  data: {
    imageUrl: '',
    uploading: false
  },

  onChooseImage() {
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      sourceType: ['album', 'camera'],
      success: (res) => {
        const tempPath = res.tempFiles[0].tempFilePath
        this.setData({ imageUrl: tempPath })
      }
    })
  },

  async onUpload() {
    if (!this.data.imageUrl) {
      wx.showToast({ title: '请先选择图片', icon: 'none' })
      return
    }

    this.setData({ uploading: true })
    wx.showLoading({ title: 'AI 解析中...' })

    try {
      await api.uploadNote(this.data.imageUrl)
      wx.hideLoading()
      wx.showToast({ title: '上传成功', icon: 'success' })
      setTimeout(() => {
        wx.navigateBack()
      }, 1500)
    } catch (e) {
      wx.hideLoading()
      this.setData({ uploading: false })
    }
  }
})
