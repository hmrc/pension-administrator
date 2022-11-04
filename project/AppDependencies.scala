import play.sbt.PlayImport._
import sbt._

object AppDependencies {

  val appName = "pension-administrator"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-play-28"         % "0.73.0",
    "uk.gov.hmrc"             %%  "logback-json-logger"       % "5.2.0",
    "com.typesafe.play"       %% "play-json"                  % "2.9.3",
    "com.typesafe.play"       %% "play-json-joda"             % "2.9.3",
    "uk.gov.hmrc"             %% "bootstrap-backend-play-28"  % "7.8.0",
    "com.networknt"            % "json-schema-validator"      % "1.0.73",
    "uk.gov.hmrc"             %% "domain"                     % "8.1.0-play-28",
    ehcache
  )

  def test(scope: String = "test, it"): Seq[ModuleID] = Seq(
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-28"          % "0.73.0"                % Test,
    "de.flapdoodle.embed"      % "de.flapdoodle.embed.mongo"        % "3.5.1"                 % Test,
    "uk.gov.hmrc"             %% "bootstrap-test-play-28"           % "7.8.0"                 % Test,
    "com.vladsch.flexmark"     % "flexmark-all"                     % "0.62.2"                % "test, it",
    "org.scalatestplus"       %% "scalatestplus-scalacheck"         % "3.1.0.0-RC2"           % "test",
    "org.scalatestplus"       %% "mockito-3-4"                      % "3.2.10.0"              % "test",
    "org.pegdown"              % "pegdown"                          % "1.6.0"                 % scope,
    "org.scalacheck"          %% "scalacheck"                       % "1.17.0"                % scope,
    "com.github.tomakehurst"   % "wiremock-jre8"                    % "2.34.0"                % scope,
    "org.scalatest"           %% "scalatest"                        % "3.2.14"                % "test",
    "org.scalatestplus"       %% "scalacheck-1-15"                  % "3.2.11.0"              % "test",
    "org.scalatestplus.play"  %% "scalatestplus-play"               % "5.1.0"                 % "test",
    "io.github.wolfendale"    %% "scalacheck-gen-regexp"            % "1.0.0"                 % "test",
  )
}
