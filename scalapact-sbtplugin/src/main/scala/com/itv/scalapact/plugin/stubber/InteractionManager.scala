package com.itv.scalapact.plugin.stubber

import com.itv.scalapactcore.Interaction

import scalaz._
import Scalaz._

object InteractionManager extends InteractionManager

//Use trait for testing or you'll have race conditions!
trait InteractionManager {

  import InteractionMatchers._

  private var interactions = List.empty[Interaction]

  def findMatchingInteraction(request: RequestDetails): Option[Interaction] = {
    interactions.find{ i =>
      matchMethods(i.request.method)(request.method) &&
        matchHeaders(i.request.headers)(request.headers) &&
        matchPaths(i.request.path)(request.path) &&
        matchBodies(i.request.body)(request.body)
    }
  }

  def getInteractions: List[Interaction] = interactions

  def addInteraction(interaction: Interaction): Unit = interactions = interaction :: interactions

  def addInteractions(interactions: List[Interaction]): Unit = interactions.foreach(addInteraction)

  def clearInteractions(): Unit = interactions = List.empty[Interaction]

}

case class PathStructure(path: String, params: Map[String, String])

case class RequestDetails(method: Option[String], headers: Option[Map[String, String]], path: Option[String], body: Option[String])

object InteractionMatchers {

  val matchStatusCodes: Option[Int] => Option[Int] => Boolean = expected => received =>
    generalMatcher(expected, received, (e: Int, r: Int) => e == r)

  val matchMethods: Option[String] => Option[String] => Boolean = expected => received =>
    generalMatcher(expected, received, (e: String, r: String) => e == r)

  val matchHeaders: Option[Map[String, String]] => Option[Map[String, String]] => Boolean = expected => received =>
    generalMatcher(expected, received, (e: Map[String, String], r: Map[String, String]) => e.toSet.subsetOf(r.toSet))

  val matchPaths: Option[String] => Option[String] => Boolean = expected => received =>
    generalMatcher(expected, received, (e: String, r: String) => toPathStructure(e) == toPathStructure(r))

  val matchBodies: Option[String] => Option[String] => Boolean = expected => received =>
    generalMatcher(expected, received, (e: String, r: String) => e == r)

  private def generalMatcher[A](expected: Option[A], received: Option[A], predictate: (A, A) => Boolean): Boolean =
    (expected |@| received) { predictate } match {
      case Some(s) => s
      case None => true
    }

  private lazy val toPathStructure: String => PathStructure = fullPath => {
    if(fullPath.isEmpty) PathStructure("", Map.empty[String, String])
    else {
      fullPath.split('?').toList match {
        case Nil => PathStructure("", Map.empty[String, String]) //should never happen
        case x :: Nil => PathStructure(x, Map.empty[String, String])
        case x :: xs =>

          val params: Map[String, String] = Convertors.pair(xs.mkString.split('&').toList.flatMap(p => p.split('=').toList))

          PathStructure(x, params)
      }
    }
  }
}