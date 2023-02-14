import play.sbt.PlayImport._
import sbt._

object AppDependencies {

  val appName = "pension-administrator"
  private val playVersion = "7.13.0"
  val compile: Seq[ModuleID] = Seq(
    ws,
    "com.fasterxml.jackson.module"  %% "jackson-module-scala"       % "2.14.2",
    "uk.gov.hmrc.mongo"             %% "hmrc-mongo-play-28"         % "0.74.0",
    "com.typesafe.play"             %% "play-json"                  % "2.9.4",
    "com.typesafe.play"             %% "play-json-joda"             % "2.9.4",
    "uk.gov.hmrc"                   %% "bootstrap-backend-play-28"  % playVersion,
    "com.networknt"                 % "json-schema-validator"       % "1.0.76",
    "uk.gov.hmrc"                   %% "domain"                     % "8.1.0-play-28",
    ehcache
  )

  def test(scope: String = "test, it"): Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-28"     % playVersion    % Test,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-28"    % "0.74.0"    % Test,
    "de.flapdoodle.embed"     %  "de.flapdoodle.embed.mongo"  % "3.5.3"     % Test,
    "org.scalatest"           %% "scalatest"                  % "3.2.15"    % Test,
    "org.scalatestplus"       %% "scalacheck-1-17"            % "3.2.15.0"  % Test,
    "org.scalatestplus"       %% "mockito-4-6"                % "3.2.15.0"  % Test,
    "org.scalatestplus.play"  %% "scalatestplus-play"         % "5.1.0"     % Test,
    "com.vladsch.flexmark"    %  "flexmark-all"               % "0.64.0"    % scope,
    "org.pegdown"             %  "pegdown"                    % "1.6.0"     % scope,
    "com.github.tomakehurst"  %  "wiremock-jre8"              % "2.35.0"    % scope,
    "io.github.wolfendale"    %% "scalacheck-gen-regexp"      % "1.1.0"     % Test
  )
}
