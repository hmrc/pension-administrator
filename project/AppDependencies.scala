import play.core.PlayVersion
import play.sbt.PlayImport.{ehcache, _}
import sbt._

object AppDependencies {

  val appName = "pension-administrator"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc"       %% "simple-reactivemongo"       % "8.0.0-play-27",
    "com.typesafe.play" %% "play-json"                  % "2.6.10",
    "com.typesafe.play" %% "play-json-joda"             % "2.6.10",
    "uk.gov.hmrc"       %% "bootstrap-backend-play-27"  % "3.4.0",
    "com.networknt"     %  "json-schema-validator"      % "1.0.3",
    "uk.gov.hmrc"       %% "domain"                     % "5.11.0-play-27",
    ehcache
  )

  def test(scope: String = "test, it"): Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% "reactivemongo-test"     % "5.0.0-play-27"     % Test,
    "org.scalatest"           %% "scalatest"              % "3.0.8"             % scope,
    "org.pegdown"             %  "pegdown"                % "1.6.0"             % scope,
    "org.scalacheck"          %% "scalacheck"             % "1.14.0"            % scope,
    "com.typesafe.play"       %% "play-test"              % PlayVersion.current % scope,
    "org.scalatestplus.play"  %% "scalatestplus-play"     % "4.0.2"             % scope,
    "org.mockito"             %  "mockito-all"            % "1.10.19"           % scope,
    "com.github.tomakehurst"  %  "wiremock-jre8"          % "2.27.2"            % scope,
    "wolfendale"              %% "scalacheck-gen-regexp"  % "0.1.1"             % scope
  )
}
