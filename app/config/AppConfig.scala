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

package config

import com.google.inject.Inject
import play.api.{Configuration, Environment, Mode}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig


class AppConfig @Inject()(serviceConfig: ServicesConfig, runModeConfiguration: Configuration, environment: Environment) {
  protected def mode: Mode = environment.mode

  lazy val appName: String = runModeConfiguration.underlying.getString("appName")

  lazy val baseURL: String = serviceConfig.baseUrl("des-hod")
  lazy val baseUrlEmail: String = serviceConfig.baseUrl("email")
  lazy val baseUrlPensionsScheme: String = serviceConfig.baseUrl("pensions-scheme")
  lazy val baseUrlPensionAdministrator: String = serviceConfig.baseUrl("pension-administrator")

  lazy val schemeAdminRegistrationUrl: String = s"$baseURL${runModeConfiguration.underlying.getString("serviceUrls.scheme.administrator.register")}"
  lazy val registerWithoutIdOrganisationUrl: String = s"$baseURL${runModeConfiguration.underlying.getString("serviceUrls.register.without.id.organisation")}"
  lazy val registerWithoutIdIndividualUrl: String = s"$baseURL${runModeConfiguration.underlying.getString("serviceUrls.register.without.id.individual")}"
  lazy val registerWithIdIndividualUrl: String = s"$baseURL${runModeConfiguration.underlying.getString("serviceUrls.register.with.id.individual")}"
  lazy val registerWithIdOrganisationUrl: String = s"$baseURL${runModeConfiguration.underlying.getString("serviceUrls.register.with.id.organisation")}"
  lazy val psaMinimalDetailsUrl: String = s"$baseURL${runModeConfiguration.underlying.getString("serviceUrls.psa.minimal.details")}"
  lazy val psaSubscriptionDetailsUrl: String = s"$baseURL${runModeConfiguration.underlying.getString("serviceUrls.psa.subscription.details")}"
  lazy val removePsaUrl: String = s"$baseURL${runModeConfiguration.underlying.getString("serviceUrls.remove.psa")}"

  lazy val createPsaAssociationUrl: String = s"$baseURL${runModeConfiguration.underlying.getString("serviceUrls.createPsaAssociation")}"
  lazy val desEnvironment: String = runModeConfiguration.getOptional[String]("microservice.services.des-hod.env").getOrElse("local")
  lazy val authorization: String = "Bearer " + runModeConfiguration.getOptional[String]("microservice.services.des-hod.authorizationToken").getOrElse("local")

  lazy val emailUrl: String = s"$baseUrlEmail${runModeConfiguration.underlying.getString("serviceUrls.email")}"
  lazy val checkAssociationUrl: String = s"$baseUrlPensionsScheme${runModeConfiguration.underlying.getString("serviceUrls.checkPsaAssociation")}"

  lazy val invitationExpiryDays: Int = runModeConfiguration.underlying.getInt("invitationExpiryDays")
  lazy val invitationCallbackUrl: String = s"$baseUrlPensionAdministrator${runModeConfiguration.underlying.getString("serviceUrls.invitation.callback")}"
}
