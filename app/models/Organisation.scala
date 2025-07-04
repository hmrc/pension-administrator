/*
 * Copyright 2025 HM Revenue & Customs
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

package models

import play.api.libs.json.*
import utils.EnumUtils

object OrganisationTypeEnum extends Enumeration {
  type OrganisationType = Value
  val CorporateBody: Value = Value("Corporate Body")
  val NotSpecified: Value = Value("Not Specified")
  val LLP: Value = Value("LLP")
  val Partnership: Value = Value("Partnership")
  val UnincorporatedBody: Value = Value("Unincorporated Body")

  private val enumUtils = new EnumUtils(OrganisationTypeEnum)

  implicit def enumFormats: Format[OrganisationType] = enumUtils.enumFormat()

}

case class Organisation(organisationName: String, organisationType: OrganisationTypeEnum.OrganisationType)

object Organisation {
  implicit val formats: OFormat[Organisation] = Json.format[Organisation]
}
