/*
 * Copyright 2018 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package utils

import org.slf4j.event.Level
import org.slf4j.{Logger, Marker}
import play.api.LoggerLike

//noinspection NotImplementedCode
//scalastyle:off number.of.methods
class StubLogger(name: String = "Application") extends LoggerLike {

  case class LogEntry(level: Level, msg: String)

  private var logEntries: Seq[LogEntry] = Seq.empty

  def getLogEntries: Seq[LogEntry] = logEntries

  def reset(): Unit = logEntries = Seq.empty

  override val logger: Logger = new Logger {
    override def getName: String = name

    override def isTraceEnabled: Boolean = true

    override def trace(msg: String): Unit = logEntries = logEntries :+ LogEntry(Level.TRACE, msg)

    override def trace(format: String, arg: scala.Any): Unit = ???

    override def trace(format: String, arg1: scala.Any, arg2: scala.Any): Unit = ???

    override def trace(format: String, arguments: AnyRef*): Unit = ???

    override def trace(msg: String, t: Throwable): Unit = ???

    override def isTraceEnabled(marker: Marker): Boolean = false

    override def trace(marker: Marker, msg: String): Unit = ???

    override def trace(marker: Marker, format: String, arg: scala.Any): Unit = ???

    override def trace(marker: Marker, format: String, arg1: scala.Any, arg2: scala.Any): Unit = ???

    override def trace(marker: Marker, format: String, argArray: AnyRef*): Unit = ???

    override def trace(marker: Marker, msg: String, t: Throwable): Unit = ???

    override def isDebugEnabled: Boolean = true

    override def debug(msg: String): Unit = logEntries = logEntries :+ LogEntry(Level.DEBUG, msg)

    override def debug(format: String, arg: scala.Any): Unit = ???

    override def debug(format: String, arg1: scala.Any, arg2: scala.Any): Unit = ???

    override def debug(format: String, arguments: AnyRef*): Unit = ???

    override def debug(msg: String, t: Throwable): Unit = ???

    override def isDebugEnabled(marker: Marker): Boolean = false

    override def debug(marker: Marker, msg: String): Unit = ???

    override def debug(marker: Marker, format: String, arg: scala.Any): Unit = ???

    override def debug(marker: Marker, format: String, arg1: scala.Any, arg2: scala.Any): Unit = ???

    override def debug(marker: Marker, format: String, arguments: AnyRef*): Unit = ???

    override def debug(marker: Marker, msg: String, t: Throwable): Unit = ???

    override def isInfoEnabled: Boolean = true

    override def info(msg: String): Unit = logEntries = logEntries :+ LogEntry(Level.INFO, msg)

    override def info(format: String, arg: scala.Any): Unit = ???

    override def info(format: String, arg1: scala.Any, arg2: scala.Any): Unit = ???

    override def info(format: String, arguments: AnyRef*): Unit = ???

    override def info(msg: String, t: Throwable): Unit = ???

    override def isInfoEnabled(marker: Marker): Boolean = false

    override def info(marker: Marker, msg: String): Unit = ???

    override def info(marker: Marker, format: String, arg: scala.Any): Unit = ???

    override def info(marker: Marker, format: String, arg1: scala.Any, arg2: scala.Any): Unit = ???

    override def info(marker: Marker, format: String, arguments: AnyRef*): Unit = ???

    override def info(marker: Marker, msg: String, t: Throwable): Unit = ???

    override def isWarnEnabled: Boolean = true

    override def warn(msg: String): Unit = logEntries = logEntries :+ LogEntry(Level.WARN, msg)

    override def warn(format: String, arg: scala.Any): Unit = ???

    override def warn(format: String, arguments: AnyRef*): Unit = ???

    override def warn(format: String, arg1: scala.Any, arg2: scala.Any): Unit = ???

    override def warn(msg: String, t: Throwable): Unit = ???

    override def isWarnEnabled(marker: Marker): Boolean = false

    override def warn(marker: Marker, msg: String): Unit = ???

    override def warn(marker: Marker, format: String, arg: scala.Any): Unit = ???

    override def warn(marker: Marker, format: String, arg1: scala.Any, arg2: scala.Any): Unit = ???

    override def warn(marker: Marker, format: String, arguments: AnyRef*): Unit = ???

    override def warn(marker: Marker, msg: String, t: Throwable): Unit = ???

    override def isErrorEnabled: Boolean = true

    override def error(msg: String): Unit = logEntries = logEntries :+ LogEntry(Level.ERROR, msg)

    override def error(format: String, arg: scala.Any): Unit = ???

    override def error(format: String, arg1: scala.Any, arg2: scala.Any): Unit = ???

    override def error(format: String, arguments: AnyRef*): Unit = ???

    override def error(msg: String, t: Throwable): Unit = ???

    override def isErrorEnabled(marker: Marker): Boolean = false

    override def error(marker: Marker, msg: String): Unit = ???

    override def error(marker: Marker, format: String, arg: scala.Any): Unit = ???

    override def error(marker: Marker, format: String, arg1: scala.Any, arg2: scala.Any): Unit = ???

    override def error(marker: Marker, format: String, arguments: AnyRef*): Unit = ???

    override def error(marker: Marker, msg: String, t: Throwable): Unit = ???
  }

}
