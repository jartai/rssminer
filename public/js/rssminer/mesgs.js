var _MESGS_ = {
  'm_unread_dislike': ["dislike count", "未读数目"],
  'm_neutral_count': ["neutral count", "猜你可能喜欢文章数"],
  'm_like_count': ["like count", "猜你喜欢文章数"],
  'm_like_title': ["I like it, give me more like this in recommend tab",
                   "我喜欢这篇文章"],
  'm_dislike_title': ["I like it, give me more like this in recommend tab",
                      "不喜欢这篇文章，不要为我推荐类似的文章"],
  'm_no_entries': ["No entries", "这里暂时没有文章"],
  'm_publish': ["Publish:", "发表于："],
  'm_read': ["Read:", "阅读于："],
  // settings
  'm_import': ['Import', '导入'],
  'm_url': ['URL', '直接添加'],
  'm_add': ['Add', '添加'],
  'm_paste_url': ['paste atom/rss url here',
                  '把订阅的RSS的地址粘贴在这里，点击添加'],
  'm_import_grader': ['Import all you google reader subscriptions:',
                      "导入您的google Reader订阅列表"]

};

function _LANG_ (k) {
  var words = {
    recommend: '推荐', newest: '最新', read: '已读',
    voted: '喜欢过', oldest: '最旧', next: '下一页',
    add: '添加订阅', settings: '设置', help: '帮助'
  };
  if(_LANG_ZH_) {
    if(!words[k]) {
      throw k + " not has ch";
    }
    return words[k];
  } else {
    return k;
  }
}