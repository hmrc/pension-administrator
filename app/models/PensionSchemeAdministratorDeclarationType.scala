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

package models

import play.api.libs.functional.syntax._
import play.api.libs.json
import play.api.libs.json.{JsPath, Json, Reads}


case class PensionSchemeAdministratorDeclarationType(box1: Boolean, box2: Boolean, box3: Boolean, box4: Boolean,
                                                     box5: Option[Boolean], box6: Option[Boolean], box7: Boolean,
                                                     pensionAdvisorDetail: Option[PensionAdvisorDetail])

object PensionSchemeAdministratorDeclarationType {
  implicit val formats = Json.format[PensionSchemeAdministratorDeclarationType]

  val apiReads: Reads[PensionSchemeAdministratorDeclarationType] = (
    (JsPath \ "declaration").read[Boolean] and
      (JsPath \ "declarationFitAndProper").read[Boolean] and
      (JsPath \ "declarationWorkingKnowledge").read[String] and
      json.Reads.optionWithNull(PensionAdvisorDetail.apiReads)
    ) ((declarationSectionOneToFour, declarationSectionSeven, workingKnowledge, adviserDetail) => {
    val declarationOutput = PensionSchemeAdministratorDeclarationType(declarationSectionOneToFour, declarationSectionOneToFour,
      declarationSectionOneToFour, declarationSectionOneToFour, None, None, declarationSectionSeven, None)

    if (workingKnowledge == "workingKnowledge") {
      declarationOutput.copy(box5 = Some(true))
    }
    else {
      declarationOutput.copy(box6 = Some(true), pensionAdvisorDetail = adviserDetail.flatten)
    }
  })
}