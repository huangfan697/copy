App({
  globalData: {
    // baseUrl: 'http://192.168.0.102:8080',
    baseUrl: 'https://book.wangjuxing.cn',
    userId: 1,
    token: ''
  },

  onLaunch() {
    console.log('错题集小程序启动')
    // 尝试从缓存恢复 userId
    const userId = wx.getStorageSync('userId')
    if (userId) {
      this.globalData.userId = userId
    } else {
      this.login()
    }
  },

  login() {
    wx.login({
      success: res => {
        if (!res.code) {
          console.error('wx.login 未返回 code')
          return
        }
        wx.request({
          url: this.globalData.baseUrl + '/api/auth/login',
          method: 'POST',
          data: { code: res.code },
          success: res => {
            if (res.data.success) {
              this.globalData.userId = res.data.data.userId
              wx.setStorageSync('userId', res.data.data.userId)
              console.log('登录成功, userId:', res.data.data.userId)
            } else {
              console.error('登录失败:', res.data.message)
            }
          },
          fail: err => {
            console.error('登录请求失败:', err)
          }
        })
      }
    })
  }
})
