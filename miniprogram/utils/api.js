const app = getApp()

function request(url, method = 'GET', data = {}) {
  return new Promise((resolve, reject) => {
    wx.request({
      url: app.globalData.baseUrl + url,
      method,
      data,
      header: {
        'Content-Type': method === 'GET' ? 'application/json' : 'application/json'
      },
      success(res) {
        if (res.data.success) {
          resolve(res.data.data)
        } else {
          wx.showToast({ title: res.data.message || '请求失败', icon: 'none' })
          reject(new Error(res.data.message))
        }
      },
      fail(err) {
        wx.showToast({ title: '网络错误', icon: 'none' })
        reject(err)
      }
    })
  })
}

function uploadFile(url, filePath) {
  return new Promise((resolve, reject) => {
    wx.uploadFile({
      url: app.globalData.baseUrl + url,
      filePath,
      name: 'file',
      formData: { userId: app.globalData.userId },
      success(res) {
        const data = JSON.parse(res.data)
        if (data.success) {
          resolve(data.data)
        } else {
          wx.showToast({ title: data.message || '上传失败', icon: 'none' })
          reject(new Error(data.message))
        }
      },
      fail(err) {
        wx.showToast({ title: '网络错误', icon: 'none' })
        reject(err)
      }
    })
  })
}

module.exports = {
  // 错题笔记
  getNotes: (params) => {
    return request('/api/notes', 'GET', { ...params, userId: app.globalData.userId })
  },
  getNoteDetail: (id) => {
    return request(`/api/notes/${id}`)
  },
  uploadNote: (filePath) => {
    return uploadFile('/api/notes/upload', filePath)
  },
  updateNoteStatus: (id, status) => {
    return request(`/api/notes/${id}/status`, 'PUT', { status })
  },

  // 练习
  generatePractice: (noteId) => {
    return request(`/api/practice/generate/${noteId}?userId=${app.globalData.userId}`, 'POST')
  },
  getPracticeList: (noteId) => {
    return request(`/api/practice/list/${noteId}`, 'GET', { userId: app.globalData.userId })
  },
  submitAnswer: (questionId, userAnswer) => {
    return request('/api/practice/submit', 'POST', { questionId, userAnswer, userId: app.globalData.userId })
  },
  getTodayTrainInfo: () => {
    return request('/api/practice/today', 'GET', { userId: app.globalData.userId })
  },
  getTodayQuestions: () => {
    return request('/api/practice/today/questions', 'GET', { userId: app.globalData.userId })
  },

  // 每日栏目
  getCollections: () => {
    return request('/api/collections', 'GET', { userId: app.globalData.userId })
  },
  getCollectionDetail: (id) => {
    return request(`/api/collections/${id}`, 'GET', { userId: app.globalData.userId })
  },
  getCollectionQuestions: (id) => {
    return request(`/api/collections/${id}/questions`, 'GET', { userId: app.globalData.userId })
  },

  // 统计
  getErrorRate: (days = 7) => {
    return request('/api/stats/error-rate', 'GET', { days, userId: app.globalData.userId })
  }
}
