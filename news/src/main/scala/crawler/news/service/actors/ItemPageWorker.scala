package crawler.news.service.actors

import akka.actor.Props
import crawler.news.commands.{ItemPageResult, StartFetchItemPage}
import crawler.news.crawlers.NewsCrawler
import crawler.news.enums.NewsSource
import crawler.news.model.NewsItem
import crawler.util.actors.MetricActor

import scala.util.{Failure, Success}

/**
 * 详情页面
 * Created by Yang Jing (yangbajing@gmail.com) on 2015-11-06.
 */
class ItemPageWorker(source: NewsSource.Value, newsItem: NewsItem) extends MetricActor {

  import context.dispatcher

  override val metricReceive: Receive = {
    case StartFetchItemPage =>
      val doSender = sender()

      NewsCrawler.getCrawler(source) match {
        case Some(crawler) =>
          crawler.fetchNewsItem(newsItem.url).onComplete {
            case Success(pageItem) =>
              doSender ! ItemPageResult(newsItem.copy(content = pageItem.content))

            case Failure(e) =>
              doSender ! ItemPageResult(newsItem.copy(error = Some(e.getLocalizedMessage)))
          }

        case None =>
          doSender ! ItemPageResult(newsItem.copy(error = Some(s"Crawler $source not exists, ${newsItem.url} needed.")))
      }

    /*
    // TODO sleep 5s 模拟抓取新闻详情内容
    TimeUnit.SECONDS.sleep(5)

    val newsItem = newsItem.copy(content = "这是新闻详情内容")
    doSender ! ItemPageResult(newsItem)
    */
  }
}

object ItemPageWorker {
  def props(source: NewsSource.Value, item: NewsItem) = Props(new ItemPageWorker(source, item))
}
