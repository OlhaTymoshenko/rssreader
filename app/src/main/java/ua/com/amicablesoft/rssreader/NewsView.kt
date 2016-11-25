package ua.com.amicablesoft.rssreader

/**
 * Created by lapa on 25.11.16.
 */
interface NewsView {
    fun showNews(newsFeed:List<News>)
    fun showError(error: Throwable)
}