package crawler.news.routes

import java.util.concurrent.TimeUnit

import akka.http.scaladsl.model.StatusCodes
import com.typesafe.scalalogging.StrictLogging
import crawler.SystemUtils
import crawler.news.enums.{NewsSource, SearchMethod}
import crawler.news.service.NewsService
import org.json4s.Extraction
import org.json4s.jackson.JsonMethods

import scala.concurrent.TimeoutException
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}

/**
 * 新闻路由
 * Created by Yang Jing (yangbajing@gmail.com) on 2015-11-03.
 */
object NewsRoute extends StrictLogging {

  import akka.http.scaladsl.server.Directives._
  import crawler.news.JsonSupport._

  val newsService = new NewsService(SystemUtils.httpClient)

  def apply(pathname: String) =
    path(pathname) {
      get {
        parameters(
          'key.as[String],
          'source.as[String] ? NewsSource.BAIDU.toString,
          'method.as[String] ? SearchMethod.F.toString,
          'duration.as[Int] ? 30) { (key, source, method, duration) =>

          val sources = source.split(',').collect { case s if NewsSource.values.exists(_.toString == s) =>
            NewsSource.withName(s)
          }
          val dura = Duration(duration, TimeUnit.SECONDS)

          onComplete(newsService.fetchNews(key, sources, SearchMethod.withName(method), dura)) {
            case Success(result) =>
              complete(result)

            case Failure(e) =>
              logger.error(e.getLocalizedMessage, e)
              val status = e match {
                case _: TimeoutException => StatusCodes.RequestTimeout
                case _ => StatusCodes.InternalServerError
              }
              complete(status, e.toString)
          }
        }
      }
    }

}
