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

import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt._

object MicroServiceBuild extends Build with MicroService {

  val appName = "pension-administrator"

  override lazy val appDependencies: Seq[ModuleID] = compile ++ test()
  private val scalaTestPlusPlayVersion = "3.1.2"
  private val mockitoAllVersion = "1.10.19"
  private val wireMockVersion = "2.19.0"
  private val scalacheckVersion = "1.14.0"
  private val domainVersion = "5.2.0"
  private val scalacheckGenRegexp = "0.1.1"
  private val bootstrapVersion = "0.31.0"
  private val playJsonVersion = "2.6.10"
  private val scalaTestVersion = "3.0.5"
  private val reactiveMongoVersion = "6.2.0"
  private val jsonSchemeValidatorVersion = "0.1.19"
  private val jsonPathVersion = "2.5.0"

  val compile = Seq(
    "uk.gov.hmrc" %% "play-reactivemongo" % reactiveMongoVersion,
    "com.typesafe.play" %% "play-json" % playJsonVersion,
    "com.typesafe.play" %% "play-json-joda" % playJsonVersion,
      ws,
    "uk.gov.hmrc" %% "bootstrap-play-26" % bootstrapVersion,
    "com.networknt" % "json-schema-validator" % jsonSchemeValidatorVersion,
    "com.josephpconley" %% "play-jsonpath" % jsonPathVersion,
    "uk.gov.hmrc" %% "domain" % domainVersion
  )

  def test(scope: String = "test,it") = Seq(
    "uk.gov.hmrc" %% "hmrctest" % "3.2.0" % scope,
    "org.scalatest" %% "scalatest" % scalaTestVersion % scope,
    "org.pegdown" % "pegdown" % "1.6.0" % scope,
    "org.scalacheck" %% "scalacheck" % scalacheckVersion % scope,
    "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
    "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusPlayVersion % scope,
    "org.mockito" % "mockito-all" % mockitoAllVersion % scope,
    "com.github.tomakehurst" % "wiremock" % wireMockVersion % scope,
    "wolfendale" %% "scalacheck-gen-regexp" % scalacheckGenRegexp % scope
  )

}
