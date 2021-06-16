/*
 * Copyright 2021 HM Revenue & Customs
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
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

class AppConfig @Inject()(runModeConfiguration: Configuration, environment: Environment, servicesConfig: ServicesConfig) {

  lazy val appName: String = runModeConfiguration.underlying.getString("appName")

  lazy val baseURL: String = servicesConfig.baseUrl("des-hod")
  lazy val ifURL: String = servicesConfig.baseUrl(serviceName = "if-hod")
  lazy val baseUrlEmail: String = servicesConfig.baseUrl("email")
  lazy val baseUrlPensionsScheme: String = servicesConfig.baseUrl("pensions-scheme")
  lazy val baseUrlPensionAdministrator: String = servicesConfig.baseUrl("pension-administrator")

  lazy val schemeAdminRegistrationUrl: String = s"$baseURL${runModeConfiguration.underlying.getString("serviceUrls.scheme.administrator.register")}"
  lazy val registerWithoutIdOrganisationUrl: String = s"$baseURL${runModeConfiguration.underlying.getString("serviceUrls.register.without.id.organisation")}"
  lazy val registerWithoutIdIndividualUrl: String = s"$baseURL${runModeConfiguration.underlying.getString("serviceUrls.register.without.id.individual")}"
  lazy val registerWithIdIndividualUrl: String = s"$baseURL${runModeConfiguration.underlying.getString("serviceUrls.register.with.id.individual")}"
  lazy val registerWithIdOrganisationUrl: String = s"$baseURL${runModeConfiguration.underlying.getString("serviceUrls.register.with.id.organisation")}"

  lazy val psaMinimalDetailsIFUrl: String = s"$ifURL${runModeConfiguration.underlying.getString("serviceUrls.if.psa.minimal.details")}"
  lazy val removePsaIFUrl: String = s"$ifURL${runModeConfiguration.underlying.getString("serviceUrls.if.remove.psa")}"
  lazy val deregisterPsaIFUrl: String = s"$ifURL${runModeConfiguration.underlying.getString("serviceUrls.if.deregister.psa")}"
  lazy val createPsaAssociationIFUrl: String = s"$ifURL${runModeConfiguration.underlying.getString("serviceUrls.if.createPsaAssociation")}"
  lazy val listOfSchemesUrl: String = s"$baseUrlPensionsScheme${runModeConfiguration.underlying.getString("serviceUrls.if.listOfSchemes")}"

  lazy val psaSubscriptionDetailsUrl: String = s"$baseURL${runModeConfiguration.underlying.getString("serviceUrls.psa.subscription.details")}"
  lazy val psaVariationDetailsUrl: String = s"$baseURL${runModeConfiguration.underlying.getString("serviceUrls.psa.variation.details")}"

  lazy val desEnvironment: String = runModeConfiguration.getOptional[String]("microservice.services.des-hod.env").getOrElse("local")
  lazy val authorization: String = "Bearer " + runModeConfiguration.getOptional[String]("microservice.services.des-hod.authorizationToken").getOrElse("local")

  lazy val integrationframeworkEnvironment: String = runModeConfiguration.getOptional[String](
    path = "microservice.services.if-hod.env").getOrElse("local")
  lazy val integrationframeworkAuthorization: String = "Bearer " + runModeConfiguration.getOptional[String](
    path = "microservice.services.if-hod.authorizationToken").getOrElse("local")

  lazy val emailUrl: String = s"$baseUrlEmail${runModeConfiguration.underlying.getString("serviceUrls.email")}"
  lazy val checkAssociationUrl: String = s"$baseUrlPensionsScheme${runModeConfiguration.underlying.getString("serviceUrls.checkPsaAssociation")}"
  lazy val getSchemeDetailsUrl: String = s"$baseUrlPensionsScheme${runModeConfiguration.underlying.getString("serviceUrls.getSchemeDetails")}"

  lazy val invitationExpiryDays: Int = runModeConfiguration.underlying.getInt("invitationExpiryDays")
  lazy val invitationCallbackUrl: String = s"$baseUrlPensionAdministrator${runModeConfiguration.underlying.getString("serviceUrls.invitation.callback")}"

}
