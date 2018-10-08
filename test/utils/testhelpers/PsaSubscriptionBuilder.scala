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

package utils.testhelpers

import java.time.LocalDate

import models._
import models.PsaSubscription._
import play.api.libs.json.JsResultException

object PsaSubscriptionBuilder {

  private val customerId = CustomerIdentification("Individual", Some("NINO"), Some("AA999999A"), true)
  private val individual = IndividualDetailType(Some("Mr"), "abcdefghijkl", Some("abcdefghijkl"), "abcdefjkl", LocalDate.parse("1947-03-29"))

  private val address = CorrespondenceAddress("Telford1", "Telford2",Some("Telford3"), Some("Telford3"), "GB", Some("TF3 4ER"))
  private val director1Address = CorrespondenceAddress("addressline1", "addressline2",Some("addressline3"), Some("addressline4"), "GB", Some("B5 9EX"))
  private val director1PrevAddress = CorrespondenceAddress("line1", "line2",Some("line3"), Some("line4"), "AD", Some("567253"))
  private val director1Contact = PsaContactDetails("0044-09876542312", Some("abc@hmrc.gsi.gov.uk"))

  private val director2Address = CorrespondenceAddress("fgfdgdfgfd", "dfgfdgdfg",Some("fdrtetegfdgdg"), Some("dfgfdgdfg"), "AD", Some("56546"))
  private val director2PrevAddress = CorrespondenceAddress("werrertqe", "ereretfdg",Some("asafafg"), Some("fgdgdasdf"), "AD", Some("23424"))
  private val director2Contact = PsaContactDetails("0044-09876542334", Some("aaa@gmail.com"))

  private val contactDetails = PsaContactDetails(" ", Some("aaa@aa.com"))
  private val previousAddress = CorrespondenceAddress("London1", "London2", Some("London3"), Some("London4"), "GB", Some("LN12 4DC"))

  private val psaAddress = CorrespondenceAddress("addline1", "addline2", Some("addline3"), Some("addline4 "), "AD", Some("56765"))
  private val psaContactDetails = PsaContactDetails("0044-0987654232", Some("aaa@yahoo.com"))
  private val pensionsAdvisor = PensionAdvisor("sgfdgssd", psaAddress, Some(psaContactDetails))

  private val director1 = DirectorOrPartner("Director", Some("Mr"), "abcdef", Some("dfgdsfff"), "dfgfdgfdg", new org.joda.time.LocalDate("1950-03-29"),
    Some("AA999999A"), Some("1234567892"), true, Some(director1PrevAddress), Some(CorrespondenceDetails(director1Address, Some(director1Contact))))
  private val director2 = DirectorOrPartner("Director", Some("Mr"), "sdfdff", Some("sdfdsfsdf"), "dfdsfsf", new org.joda.time.LocalDate("1950-07-29"),
    Some("AA999999A"), Some("7897700000"), true, Some(director2PrevAddress), Some(CorrespondenceDetails(director2Address, Some(director2Contact))))

  val psaSubscription = PsaSubscription(false, customerId, None, Some(individual), address, contactDetails,
  true, Some(previousAddress), Some(Seq(director1, director2)), Some(pensionsAdvisor))

}


