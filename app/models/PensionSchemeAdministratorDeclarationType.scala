/*
 * Copyright 2019 HM Revenue & Customs
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
import play.api.libs.json.{JsPath, Json, Reads, Writes}


case class PensionSchemeAdministratorDeclarationType(box1: Boolean, box2: Boolean, box3: Boolean, box4: Boolean,
                                                     box5: Option[Boolean], box6: Option[Boolean], box7: Boolean,
                                                     pensionAdvisorDetail: Option[PensionAdvisorDetail],
                                                     isChanged: Option[Boolean] = None)

object PensionSchemeAdministratorDeclarationType {
  implicit val formats = Json.format[PensionSchemeAdministratorDeclarationType]

  val apiReads: Reads[PensionSchemeAdministratorDeclarationType] = (
    (JsPath \ "declaration").read[Boolean] and
      (JsPath \ "declarationFitAndProper").read[Boolean] and
      (JsPath \ "declarationWorkingKnowledge").read[String] and
      json.Reads.optionWithNull(PensionAdvisorDetail.apiReads) and
      (JsPath \ "isChanged").readNullable[Boolean]
    ) ((declarationSectionOneToFour, declarationSectionSeven, workingKnowledge, adviserDetail, isChanged) => {
    val declarationOutput = PensionSchemeAdministratorDeclarationType(declarationSectionOneToFour, declarationSectionOneToFour,
      declarationSectionOneToFour, declarationSectionOneToFour, None, None, declarationSectionSeven, None, isChanged)

    if (workingKnowledge == "workingKnowledge") {
      declarationOutput.copy(box5 = Some(true))
    }
    else {
      declarationOutput.copy(box6 = Some(true), pensionAdvisorDetail = adviserDetail.flatten)
    }
  })


  val psaUpdateWrites: Writes[PensionSchemeAdministratorDeclarationType] = (
    (JsPath \ "box1").write[Boolean] and
      (JsPath \ "box2").write[Boolean] and
      (JsPath \ "box3").write[Boolean] and
      (JsPath \ "box4").write[Boolean] and
      (JsPath \ "box5").writeNullable[Boolean] and
      (JsPath \ "box6").writeNullable[Boolean] and
      (JsPath \ "box7").write[Boolean] and
      (JsPath \ "pensionAdvisorDetails").writeNullable(PensionAdvisorDetail.psaUpdateWrites) and
      (JsPath \ "changeFlag").write[Boolean]
    ) (
    padt =>
      (padt.box1, padt.box2, padt.box3, padt.box4, padt.box5, padt.box6, padt.box7, padt.pensionAdvisorDetail, padt.isChanged.fold(false)(identity))
  )


}
