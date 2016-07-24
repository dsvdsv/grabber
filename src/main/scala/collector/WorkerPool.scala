package collector

import java.net.URL

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.routing.{ActorRefRoutee, RoundRobinRoutingLogic, Router}
import akka.stream.actor.{ActorSubscriber, MaxInFlightRequestStrategy, RequestStrategy}
import akka.util.ByteString

/**
  * Created by dsvdsv on 23.07.16.
  */
class WorkerPool extends ActorSubscriber{
  import WorkerPool._
  import akka.stream.actor.ActorSubscriberMessage._

  val queue = Map.empty[URL, ActorRef]

  val router = {
    val routees = Vector.fill(3) {
      ActorRefRoutee(context.actorOf(Props[Worker]))
    }
    Router(RoundRobinRoutingLogic(), routees)
  }

  override protected def requestStrategy = new MaxInFlightRequestStrategy(max = MaxQueueSize) {
    override def inFlightInternally: Int = queue.size
  }

  override def receive = {
    case OnNext(url@URL) =>
      queue += (url -> self)
      router.route(Work(url), self)
  }


  class Worker extends Actor with ActorLogging {
    import WorkerPool._

    import akka.pattern.pipe
    import context.dispatcher

    val http = Http(context.system)

    def receive = {
      case Work(url) =>
        http.singleRequest(HttpRequest(uri = url.toString))
          .pipeTo(self)

       // sender() ! Reply(url)
      case HttpResponse(StatusCodes.OK, headers, entity, _) =>
        log.info("Got response, body: " + entity.dataBytes.runFold(ByteString(""))(_ ++ _))
      case HttpResponse(code, _, _, _) =>
        log.info("Request failed, response code: " + code)
    }

  }
}

object WorkerPool {
  def props = Props(new WorkerPool)

  val MaxQueueSize = 10

  case class Work(url: URL)
  case class Reply(url: URL)
  case class Done(url: URL)
}
