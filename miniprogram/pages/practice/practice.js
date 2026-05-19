const api = require('../../utils/api')

Page({
  data: {
    questions: [],
    currentIndex: 0,
    selectedAnswer: '',
    showResult: false,
    result: null,
    correctCount: 0,
    finished: false,
    progressPercent: 0,
    accuracyRate: 0
  },

  onLoad(options) {
    if (options.mode === 'today') {
      this.loadTodayQuestions()
    } else if (options.mode === 'collection' && options.collectionId) {
      this.loadCollectionQuestions(options.collectionId)
    } else if (options.noteId) {
      this.loadQuestions(options.noteId)
    }
  },

  async loadQuestions(noteId) {
    wx.showLoading({ title: '加载中' })
    try {
      const questions = await api.getPracticeList(noteId)
      // 解析 options JSON
      questions.forEach(q => {
        try {
          q.optionsObj = JSON.parse(q.options)
        } catch (e) {
          q.optionsObj = {}
        }
      })
      this.setData({ questions, progressPercent: questions.length > 0 ? Math.round(100 / questions.length) : 0 })
    } catch (e) {
    } finally {
      wx.hideLoading()
    }
  },

  async loadTodayQuestions() {
    wx.showLoading({ title: '加载中' })
    try {
      const questions = await api.getTodayQuestions()
      questions.forEach(q => {
        try {
          q.optionsObj = JSON.parse(q.options)
        } catch (e) {
          q.optionsObj = {}
        }
      })
      this.setData({ questions, progressPercent: questions.length > 0 ? Math.round(100 / questions.length) : 0 })
    } catch (e) {
    } finally {
      wx.hideLoading()
    }
  },

  async loadCollectionQuestions(collectionId) {
    wx.showLoading({ title: '加载中' })
    try {
      const questions = await api.getCollectionQuestions(collectionId)
      questions.forEach(q => {
        try {
          q.optionsObj = JSON.parse(q.options)
        } catch (e) {
          q.optionsObj = {}
        }
      })
      this.setData({ questions, progressPercent: questions.length > 0 ? Math.round(100 / questions.length) : 0 })
    } catch (e) {
    } finally {
      wx.hideLoading()
    }
  },

  onSelectAnswer(e) {
    this.setData({ selectedAnswer: e.currentTarget.dataset.answer })
  },

  async onSubmit() {
    if (!this.data.selectedAnswer) {
      wx.showToast({ title: '请选择答案', icon: 'none' })
      return
    }

    const question = this.data.questions[this.data.currentIndex]
    try {
      const result = await api.submitAnswer(question.id, this.data.selectedAnswer)
      if (result.correct) {
        this.setData({ correctCount: this.data.correctCount + 1 })
      }

      this.setData({
        showResult: true,
        result
      })
    } catch (e) {
    }
  },

  onNext() {
    const nextIndex = this.data.currentIndex + 1
    if (nextIndex >= this.data.questions.length) {
      const total = this.data.questions.length
      const rate = total > 0 ? Math.round(this.data.correctCount / total * 100) : 0
      this.setData({ finished: true, accuracyRate: rate })
    } else {
      this.setData({
        currentIndex: nextIndex,
        selectedAnswer: '',
        showResult: false,
        result: null
      })
    }
  },

  onBackHome() {
    wx.navigateBack({ delta: 2 })
  }
})
