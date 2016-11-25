package ua.com.amicablesoft.rssreader

import rx.Subscription

/**
 * Created by lapa on 25.11.16.
 */
class NewsPresenter(val view: NewsView, val repository: NewsRepository) {
    private var subscription: Subscription? = null

    fun onStart() {
        subscription = repository.fetchNews(false)
                .subscribe(
                        { news ->
                            view.showNews(news)
                        },
                        { error ->
                            view.showError(error)
                        }
                )
    }

    fun onStop() {
        subscription?.unsubscribe()
    }

    fun forceReload() {
        subscription?.unsubscribe()
        subscription = repository.fetchNews(true)
            .subscribe(
                { news ->
                    view.showNews(news)
                },
                { error ->
                    view.showError(error)
                }
            )
    }
}