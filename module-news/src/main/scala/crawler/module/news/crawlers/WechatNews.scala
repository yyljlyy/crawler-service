package crawler.module.news.crawlers

import java.net.URLEncoder
import java.time.Instant

import crawler.module.news.enums.ItemSource
import crawler.module.news.model.{NewsItem, SearchResult}
import crawler.util.Utils
import crawler.util.http.HttpClient
import crawler.util.time.TimeUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

/**
 * 搜狗微信搜索
 * Created by Yang Jing (yangbajing@gmail.com) on 2015-11-10.
 */
class WechatNews(val httpClient: HttpClient) extends NewsCrawler(ItemSource.wechat) {
  private def parseNewsItem(elem: Element) = {
    implicit val duration = 1.second

    try {
      val title = elem.select("h4")
      val footer = elem.select("div.s-p")
      val scriptStr = elem.select("script").last().text()
      val timeStr = """'(\d+?)'""".r.findFirstMatchIn(scriptStr).map(_.matched.replace("'", ""))
      val href = WechatNews.complateWeixinUrl(title.select("a").attr("href").trim)
      val url = Option(WechatNews.find302Location(href, requestHeaders())).getOrElse(href)
      NewsItem(
        title.text().trim,
        url,
        footer.select("a#weixin_account").attr("title"),
        Option(TimeUtils.toLocalDateTime(Instant.ofEpochSecond(timeStr.map(_.toLong).getOrElse(Instant.now().getEpochSecond)))),
        elem.select("p").text())
    } catch {
      case e: Exception =>
        logger.error(elem.toString)
        throw e
    }
  }

  /**
   * 抓取搜索页
    *
    * @param key 搜索关键词
   * @return
   */
  override def fetchItemList(key: String)(implicit ec: ExecutionContext): Future[SearchResult] = {
    fetchPage(WechatNews.searchUrl(key)).map { response =>
      response.getHeaders.entrySet().asScala.foreach { case entry => println(entry.getKey + ": " + entry.getValue.asScala) }

      val now = TimeUtils.now()
      val doc = Jsoup.parse(response.getResponseBody(Utils.CHARSET.name()), "http://weixin.sogou.com")
      println(doc)
      val results = doc.select("div.wx-rb")
      if (!doc.select("#seccodeImage").isEmpty) {
        SearchResult(newsSource, key, now, 0, Nil, Some(doc.select("div.content-box").select("p.p2").text()))
      } else if (results.isEmpty) {
        SearchResult(newsSource, key, now, 0, Nil)
      } else {
        val newsItems = results.asScala.map(parseNewsItem)
        val countText = doc.select("resnum#scd_num").text().replace(",", "").trim
        val count =
          try {
            countText.toInt
          } catch {
            case e: Exception =>
              logger.warn("count < 1: " + countText, e)
              newsItems.size
          }
        SearchResult(newsSource, key, now, count, newsItems)
      }
    }
  }

}

object WechatNews {
  final val WEIXIN_SEARCH_PAGE = "http://weixin.sogou.com"

  def complateWeixinUrl(uri: String) =
    if (uri.startsWith("/")) WEIXIN_SEARCH_PAGE + uri else uri

  def searchUrl(key: String) =
    WEIXIN_SEARCH_PAGE + "/weixin?query=%s&type=2".format(URLEncoder.encode(key, Utils.CHARSET.name()))

  def find302Location(url: String, headers: Seq[(String, String)])(implicit duration: Duration) = {
    val client = HttpClient(false)
    try {
      val resp = Await.result(client.get(url).header(headers: _*).execute(), duration)
      resp.getHeader("Location")
    } catch {
      case e: Exception =>
        try {
          val respose = Await.result(client.get(url).header(headers: _*).execute(), duration + 1.second)
          respose.getHeader("Location")
        } catch {
          case e: Exception =>
            // do nothing
            null
        }
    } finally {
      client.close()
    }
  }
}
