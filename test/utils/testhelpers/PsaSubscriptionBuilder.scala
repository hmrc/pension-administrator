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

package utils.testhelpers

import java.time.LocalDate

import models.{OrganisationOrPartner, _}

object PsaSubscriptionBuilder {

  private val customerId = CustomerIdentification("Individual", Some("NINO"), Some("AA999999A"), true)
  private val individual = IndividualDetailType(Some("Mr"), "abcdefghijkl", Some("abcdefghijkl"), "abcdefjkl", LocalDate.parse("1947-03-29"))
  val organisationOrPartner = OrganisationOrPartner("organization name", None, None, None)

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
  
  val psaSubscriptionUserAnswers = """{
                                        "areYouInUK" : true,
                                        "registrationInfo":{  
                                           "legalStatus":"Individual",
                                           "sapNumber":"",
                                           "noIdentifier":false,
                                           "customerType":"UK",
                                           "idType":"NINO",
                                           "idNumber":"AA999999A"
                                        },
                                        "individualNino":"AA999999A",
                                        "individualDetails":{  
                                           "firstName":"abcdefghijkl",
                                           "middleName":"abcdefghijkl",
                                           "lastName":"abcdefjkl"
                                        },
                                        "individualDateOfBirth":"1947-03-29",
                                        "individualContactAddress":{  
                                           "addressLine1":"Telford1",
                                           "addressLine2":"Telford2",
                                           "addressLine3":"Telford3",
                                           "addressLine4":"Telford3",
                                           "postcode":"TF3 4ER",
                                           "country":"GB"
                                        },
                                        "individualContactDetails":{  
                                           "phone":" ",
                                           "email":"aaa@aa.com"
                                        },
                                        "individualAddressYears":"under_a_year",
                                        "individualPreviousAddress":{  
                                           "addressLine1":"London1",
                                           "addressLine2":"London2",
                                           "addressLine3":"London3",
                                           "addressLine4":"London4",
                                           "postcode":"LN12 4DC",
                                           "country":"GB"
                                        },
                                        "adviserName": "sgfdgssd",
                                        "adviserDetails":{
                                           "email":"aaa@yahoo.com",
                                           "phone":"0044-0987654232"
                                        },
                                        "adviserAddress":{  
                                           "addressLine1":"addline1",
                                           "addressLine2":"addline2",
                                           "addressLine3":"addline3",
                                           "addressLine4":"addline4 ",
                                           "postcode":"56765",
                                           "country":"AD"
                                        }
                                     }""".stripMargin
}


