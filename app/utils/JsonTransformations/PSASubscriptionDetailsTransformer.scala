/*
 * Copyright 2020 HM Revenue & Customs
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

package utils.JsonTransformations

import com.google.inject.Inject
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json.{__, _}

trait JsonTransformer{
  val doNothing: Reads[JsObject] = __.json.put(Json.obj())
}


class PSASubscriptionDetailsTransformer @Inject()(addressTransformer: AddressTransformer,
                                                 directorOrPartnerTransformer: DirectorOrPartnerTransformer,
                                                 legalStatusTransformer: LegalStatusTransformer,
                                                 registrationInfoTransformer: RegistrationInfoTransformer,
                                                 payeAndVatTransformer: PayeAndVatTransformer,
                                                 adviserTransformer: AdviserTransformer,
                                                 individualTransformer: IndividualTransformer) extends JsonTransformer {

  lazy val transformToUserAnswers: Reads[JsObject] =
      registrationInfoTransformer.getRegistrationInfo and
      individualTransformer.getNinoOrUtr and
      getCrn and
      getOrganisationOrPartnerDetails.orElse(individualTransformer.getIndividualDetails) and
      payeAndVatTransformer.getPayeAndVat and
      addressTransformer.getCorrespondenceAddress and
      individualTransformer.getContactDetails and
      addressTransformer.getAddressYearsBasedOnLegalStatus and
      addressTransformer.getPreviousAddressBasedOnLegalStatus and
      adviserTransformer.getAdviser and
      directorOrPartnerTransformer.getDirectorsOrPartners and
      getAreYouInUK and adviserTransformer.getWorkingKnowledge reduce


  private val getOrganisationOrPartnerDetails: Reads[JsObject] = {
    legalStatusTransformer.returnPathBasedOnLegalStatus(__, __ \ 'businessName, __ \ 'businessName).flatMap { orgPath =>
      orgPath.json.copyFrom((__ \ 'psaSubscriptionDetails \ 'organisationOrPartnerDetails \ 'name).json.pick)
    }
  }

  private val getCrn = ((__ \ 'companyRegistrationNumber).json.copyFrom((__ \ 'psaSubscriptionDetails \ 'organisationOrPartnerDetails \ 'crnNumber).json.pick)
    orElse doNothing)

  private val getAreYouInUK: Reads[JsObject] = {
    val isNonUK = (__ \ "psaSubscriptionDetails" \ "correspondenceAddressDetails" \ "nonUKAddress")
      .json.pick[JsBoolean].map{v => JsBoolean(!v.as[Boolean])}

    (__ \ 'areYouInUK).json.copyFrom(isNonUK)

  }
}
