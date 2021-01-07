import play.core.PlayVersion
import play.sbt.PlayImport.{ehcache, _}
import sbt._

object AppDependencies {

  val appName = "pension-administrator"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc"       %% "simple-reactivemongo"       % "7.31.0-play-26",
    "com.typesafe.play" %% "play-json"                  % "2.6.10",
    "com.typesafe.play" %% "play-json-joda"             % "2.6.10",
    "uk.gov.hmrc"       %% "bootstrap-backend-play-26"  % "3.2.0",
    "com.networknt"     %  "json-schema-validator"      % "1.0.3",
    "uk.gov.hmrc"       %% "domain"                     % "5.10.0-play-26",
    ehcache
  )

  def test(scope: String = "test, it"): Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% "reactivemongo-test"     % "4.22.0-play-26"    % Test,
    "uk.gov.hmrc"             %% "hmrctest"               % "3.10.0-play-26"     % scope,
    "org.scalatest"           %% "scalatest"              % "3.0.8"             % scope,
    "org.pegdown"             %  "pegdown"                % "1.6.0"             % scope,
    "org.scalacheck"          %% "scalacheck"             % "1.14.0"            % scope,
    "com.typesafe.play"       %% "play-test"              % PlayVersion.current % scope,
    "org.scalatestplus.play"  %% "scalatestplus-play"     % "3.1.2"             % scope,
    "org.mockito"             %  "mockito-all"            % "1.10.19"           % scope,
    "com.github.tomakehurst"  %  "wiremock"               % "2.27.2"            % scope,
    "wolfendale"              %% "scalacheck-gen-regexp"  % "0.1.1"             % scope
  )

  // Fixes a transitive dependency clash between wiremock and scalatestplus-play
  val overrides: Seq[ModuleID] = {
    val jettyFromWiremockVersion = "9.2.24.v20180105"
    Seq(
      "org.eclipse.jetty"           % "jetty-client"       % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-continuation" % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-http"         % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-io"           % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-security"     % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-server"       % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-servlet"      % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-servlets"     % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-util"         % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-webapp"       % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-xml"          % jettyFromWiremockVersion,
      "org.eclipse.jetty.websocket" % "websocket-api"      % jettyFromWiremockVersion,
      "org.eclipse.jetty.websocket" % "websocket-client"   % jettyFromWiremockVersion,
      "org.eclipse.jetty.websocket" % "websocket-common"   % jettyFromWiremockVersion
    )
  }

}
