import play.core.PlayVersion
import play.sbt.PlayImport.{ehcache, _}
import sbt._

object AppDependencies {

  val appName = "pension-administrator"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc"       %% "simple-reactivemongo"       % "8.0.0-play-28",
    "com.typesafe.play" %% "play-json"                  % "2.9.2",
    "com.typesafe.play" %% "play-json-joda"             % "2.9.2",
    "uk.gov.hmrc"       %% "bootstrap-backend-play-28"  % "5.14.0",
    "com.networknt"     %  "json-schema-validator"      % "1.0.3",
    "uk.gov.hmrc"       %% "domain"                     % "6.2.0-play-28",
    ehcache
  )

  def test(scope: String = "test, it"): Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% "reactivemongo-test"         % "5.0.0-play-28"     % Test,
    "com.vladsch.flexmark"    % "flexmark-all"                % "0.36.8"            % "test, it",
    "org.mockito"             % "mockito-core"                % "4.0.0"             % "test",
    "org.mockito"             %% "mockito-scala"              % "1.16.42"           % "test",
    "org.mockito"             %% "mockito-scala-scalatest"    % "1.16.42"           % "test",
    "org.scalatestplus"       %% "scalatestplus-scalacheck"   % "3.1.0.0-RC2"       % "test",
    "org.pegdown"             %  "pegdown"                    % "1.6.0"             % scope,
    "org.scalacheck"          %% "scalacheck"                 % "1.14.0"            % scope,
 //   "com.typesafe.play"       %% "play-test"              % PlayVersion.current % scope,
 //   "org.scalatestplus.play"  %% "scalatestplus-play"     % "4.0.2"             % scope,
    "com.github.tomakehurst"  %  "wiremock-jre8"              % "2.31.0"            % scope,
    "wolfendale"              %% "scalacheck-gen-regexp"      % "0.1.1"             % scope
  )
}
