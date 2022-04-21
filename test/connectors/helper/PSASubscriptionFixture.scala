/*
 * Copyright 2022 HM Revenue & Customs
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

package connectors.helper

import play.api.libs.json.{JsValue, Json}

object PSASubscriptionFixture {

  val registerPSAValidPayload: JsValue = Json.parse(
    """
      |{
      |  "customerType":"UK",
      |  "legalStatus":"Individual",
      |  "idType":"NINO",
      |  "idNumber":"AA999999A",
      |  "sapNumber":"0123456789",
      |  "noIdentifier":false,
      |  "individualDetail":{
      |    "title":"Mr",
      |    "firstName":"abcdefghijklmnopqrs",
      |    "middleName":"abcdefghijklmnopq",
      |    "lastName": "TestLastName",
      |    "dateOfBirth":"2000-12-12"
      |  },
      |  "pensionSchemeAdministratoridentifierStatus":{
      |    "isExistingPensionSchemaAdministrator":false,
      |    "existingPensionSchemaAdministratorReference":"01234567"
      |  },
      |  "correspondenceAddressDetail":{
      |    "addressType":"UK",
      |    "line1":"worthing 1",
      |    "line2":"worthing 2",
      |    "postalCode":"LN12 4DC",
      |    "countryCode":"GB"
      |  },
      |  "correspondenceContactDetail":{
      |    "telephone":"0044-09876542312",
      |    "mobileNumber":"0044-09876542312",
      |    "fax":"0044-09876542312",
      |    "email":"abc@hmrc.gsi.gov.uk"
      |  },
      |  "previousAddressDetail":{
      |    "isPreviousAddressLast12Month":true,
      |    "previousAddressDetail":{
      |      "addressType":"UK",
      |      "line1":"abcdefg",
      |      "line2":"abcdef",
      |      "line3":"abcd",
      |      "line4":"abcdefghijklmno",
      |      "postalCode":"B5 9EX",
      |      "countryCode":"GB"
      |    }
      |  },
      |  "numberOfDirectorOrPartners":{
      |    "isMorethanTenDirectors":true,
      |    "isMorethanTenPartners":true
      |  },
      |  "directorOrPartnerDetail":[
      |    {
      |      "sequenceId":"77",
      |      "entityType":"Director",
      |      "title":"Mr",
      |      "firstName":"abcdefghijklmnopqrs",
      |      "middleName":"abcdefg",
      |      "lastName":"abcdef",
      |      "dateOfBirth":"2018-12-12",
      |      "referenceOrNino":"AA999999A",
      |      "noNinoReason":"abcdefghijklmnopqrs",
      |      "utr":"0123456789",
      |      "noUtrReason":"abcdef",
      |      "correspondenceCommonDetail":{
      |        "addressDetail":{
      |          "addressType":"UK",
      |          "line1":"abcdefg",
      |          "line2":"abcdef",
      |          "line3":"abcd",
      |          "line4":"abcdefghijklmno",
      |          "postalCode":"B5 9EX",
      |          "countryCode":"GB"
      |        },
      |        "contactDetail":{
      |          "telephone":"0044-09876542312",
      |          "mobileNumber":"0044-09876542312",
      |          "fax":"0044-09876542312",
      |          "email":"abc@hmrc.gsi.gov.uk"
      |        }
      |      },
      |      "previousAddressDetail":{
      |        "isPreviousAddressLast12Month":false,
      |        "previousAddressDetail":{
      |          "addressType":"NON-UK",
      |          "line1":"abcdefg",
      |          "line2":"abcdef",
      |          "line3":"abcd",
      |          "line4":"abcdefghijklmno",
      |          "countryCode":"AD"
      |        }
      |      }
      |    }
      |  ],
      |  "declaration":{
      |    "box1":true,
      |    "box2":false,
      |    "box3":true,
      |    "box4":true,
      |    "box7":false
      |  }
      |}
      |""".stripMargin
  )

  val registerPSAInValidPayload: JsValue = Json.parse(
    """
      |{
      |  "customerType":"UK",
      |  "legalStatus":"Individual",
      |  "idType":"NINO",
      |  "idNumber":"AA999999A",
      |  "sapNumber":"0123456789",
      |  "noIdentifier":false,
      |  "individualDetail":{
      |    "title":"Mr",
      |    "firstName":"abcdefghijklmnopqrs",
      |    "middleName":"abcdefghijklmnopq",
      |    "lastName": "TestLastName",
      |    "dateOfBirth":"sdfasdfasdf"
      |  },
      |  "pensionSchemeAdministratoridentifierStatus":{
      |    "isExistingPensionSchemaAdministrator":false,
      |    "existingPensionSchemaAdministratorReference":"01234567"
      |  },
      |  "correspondenceAddressDetail":{
      |    "addressType":"UK",
      |    "line1":"worthing 1",
      |    "line2":"worthing 2",
      |    "postalCode":"LN12 4DC",
      |    "countryCode":"GB"
      |  },
      |  "correspondenceContactDetail":{
      |    "telephone":"0044-09876542312",
      |    "mobileNumber":"0044-09876542312",
      |    "fax":"0044-09876542312",
      |    "email":"abc@hmrc.gsi.gov.uk"
      |  },
      |  "previousAddressDetail":{
      |    "isPreviousAddressLast12Month":true,
      |    "previousAddressDetail":{
      |      "addressType":"UK",
      |      "line1":"abcdefg",
      |      "line2":"abcdef",
      |      "line3":"abcd",
      |      "line4":"abcdefghijklmno",
      |      "postalCode":"B5 9EX",
      |      "countryCode":"GB"
      |    }
      |  },
      |  "numberOfDirectorOrPartners":{
      |    "isMorethanTenDirectors":true,
      |    "isMorethanTenPartners":true
      |  },
      |  "directorOrPartnerDetail":[
      |    {
      |      "sequenceId":"77",
      |      "entityType":"Director",
      |      "title":"Mr",
      |      "firstName":"abcdefghijklmnopqrs",
      |      "middleName":"abcdefg",
      |      "lastName":"abcdef",
      |      "dateOfBirth":"2018-12-12",
      |      "referenceOrNino":"AA999999A",
      |      "noNinoReason":"abcdefghijklmnopqrs",
      |      "utr":"0123456789",
      |      "noUtrReason":"abcdef",
      |      "correspondenceCommonDetail":{
      |        "addressDetail":{
      |          "addressType":"UK",
      |          "line1":"abcdefg",
      |          "line2":"abcdef",
      |          "line3":"abcd",
      |          "line4":"abcdefghijklmno",
      |          "postalCode":"B5 9EX",
      |          "countryCode":"GB"
      |        },
      |        "contactDetail":{
      |          "telephone":"0044-09876542312",
      |          "mobileNumber":"0044-09876542312",
      |          "fax":"0044-09876542312",
      |          "email":"abc@hmrc.gsi.gov.uk"
      |        }
      |      },
      |      "previousAddressDetail":{
      |        "isPreviousAddressLast12Month":false,
      |        "previousAddressDetail":{
      |          "addressType":"NON-UK",
      |          "line1":"abcdefg",
      |          "line2":"abcdef",
      |          "line3":"abcd",
      |          "line4":"abcdefghijklmno",
      |          "countryCode":"AD"
      |        }
      |      }
      |    }
      |  ],
      |  "declaration":{
      |    "box1":true,
      |    "box2":false,
      |    "box3":true,
      |    "box4":true,
      |    "box7":false
      |  }
      |}
      |""".stripMargin
  )

  val registerPSAInValidPayloadWithMultipleErrors: JsValue = Json.parse(
    """
      |{
      |  "customerType":"UK",
      |  "legalStatus":"Individual",
      |  "idType":"NINO",
      |  "idNumber":"AA999999A",
      |  "sapNumber":"0123456789",
      |  "noIdentifier":false,
      |  "individualDetail":{
      |    "title":"Mr",
      |    "firstName":"abcdefghijklmnopqrs",
      |    "middleName":"abcdefghijklmnopq",
      |    "dateOfBirth":"sdfsadfasdfsadf"
      |  },
      |  "pensionSchemeAdministratoridentifierStatus":{
      |    "isExistingPensionSchemaAdministrator":false,
      |    "existingPensionSchemaAdministratorReference":"01234567"
      |  },
      |  "correspondenceAddressDetail":{
      |    "addressType":"UK",
      |    "line1":"worthing 1",
      |    "line2":"worthing 2",
      |    "postalCode":"LN12 4DC",
      |    "countryCode":"GB"
      |  },
      |  "correspondenceContactDetail":{
      |    "telephone":"0044-09876542312",
      |    "mobileNumber":"0044-09876542312",
      |    "fax":"0044-09876542312",
      |    "email":"abc@hmrc.gsi.gov.uk"
      |  },
      |  "previousAddressDetail":{
      |    "isPreviousAddressLast12Month":true,
      |    "previousAddressDetail":{
      |      "addressType":"UK",
      |      "line1":"abcdefg",
      |      "line2":"abcdef",
      |      "line3":"abcd",
      |      "line4":"abcdefghijklmno",
      |      "postalCode":"B5 9EX",
      |      "countryCode":"GB"
      |    }
      |  },
      |  "numberOfDirectorOrPartners":{
      |    "isMorethanTenDirectors":true,
      |    "isMorethanTenPartners":true
      |  },
      |  "directorOrPartnerDetail":[
      |    {
      |      "sequenceId":"77",
      |      "entityType":"Director",
      |      "title":"Mr",
      |      "firstName":"abcdefghijklmnopqrs",
      |      "middleName":"abcdefg",
      |      "lastName":"abcdef",
      |      "dateOfBirth":"2018-12-12",
      |      "referenceOrNino":"AA999999A",
      |      "noNinoReason":"abcdefghijklmnopqrs",
      |      "utr":"0123456789",
      |      "noUtrReason":"abcdef",
      |      "correspondenceCommonDetail":{
      |        "addressDetail":{
      |          "addressType":"UK",
      |          "line1":"abcdefg",
      |          "line2":"abcdef",
      |          "line3":"abcd",
      |          "line4":"abcdefghijklmno",
      |          "postalCode":"B5 9EX",
      |          "countryCode":"GB"
      |        },
      |        "contactDetail":{
      |          "telephone":"0044-09876542312",
      |          "mobileNumber":"0044-09876542312",
      |          "fax":"0044-09876542312",
      |          "email":"abc@hmrc.gsi.gov.uk"
      |        }
      |      },
      |      "previousAddressDetail":{
      |        "isPreviousAddressLast12Month":false,
      |        "previousAddressDetail":{
      |          "addressType":"NON-UK",
      |          "line1":"abcdefg",
      |          "line2":"abcdef",
      |          "line3":"abcd",
      |          "line4":"abcdefghijklmno",
      |          "countryCode":"AD"
      |        }
      |      }
      |    }
      |  ],
      |  "declaration":{
      |    "box1":true,
      |    "box2":false,
      |    "box3":true,
      |    "box4":true,
      |    "box7":false
      |  }
      |}
      |""".stripMargin)

   val psaVariation = Json.parse(
     """
       |{
       |  "customerIdentificationDetails": {
       |    "legalStatus": "Individual",
       |    "idType": "NINO",
       |    "idNumber": "QQ123456C",
       |    "noIdentifier": true
       |  },
       |  "organisationDetails": {
       |    "name": "ABC Ltd",
       |    "crnNumber": "12345678",
       |    "vatRegistrationNumber": "145899025",
       |    "payeReference": "123A"
       |  },
       |  "individualDetails": {
       |    "title": "Mr",
       |    "firstName": "John",
       |    "middleName": "A",
       |    "lastName": "Smith",
       |    "dateOfBirth": "1960-02-29"
       |  },
       |  "correspondenceAddressDetails": {
       |    "changeFlag": true,
       |    "nonUKAddress": true,
       |    "line1": "24/456",
       |    "line2": "ABC Towers",
       |    "line3": "DEF Colony ",
       |    "line4": "Mumbai",
       |    "postalCode": "516369",
       |    "countryCode": "IN"
       |  },
       |  "correspondenceContactDetails": {
       |    "changeFlag": true,
       |    "telephone": "0121234567",
       |    "mobileNumber": "07888654234",
       |    "fax": "01211289653",
       |    "email": "aaa@aa.com"
       |  },
       |  "previousAddressDetails": {
       |    "changeFlag": true,
       |    "isPreviousAddressLast12Month": true,
       |    "previousAddressDetails": {
       |      "nonUKAddress": true,
       |      "line1": "54-123/A",
       |      "line2": "Mega Colony",
       |      "line3": "RK Puram",
       |      "line4": "Bangalore",
       |      "postalCode": "5000001",
       |      "countryCode": "IN"
       |    }
       |  },
       |  "numberOfDirectorOrPartners": {
       |    "changeFlag": true,
       |    "isMoreThanTenDirectors": true,
       |    "isMoreThanTenPartners": true
       |  },
       |  "changeOfDirectorOrPartnerDetails": true,
       |  "directorOrPartnerDetails": [
       |    {
       |      "sequenceId": "123",
       |      "entityType": "Director",
       |      "title": "Mr",
       |      "firstName": "James",
       |      "middleName": "S",
       |      "lastName": "Little",
       |      "dateOfBirth": "1970-02-28",
       |      "nino": "AA123456D",
       |      "noNinoReason": "jsjsj",
       |      "utr": "1234567890",
       |      "noUtrReason": "  ",
       |      "correspondenceCommonDetails": {
       |        "addressDetails": {
       |          "nonUKAddress": true,
       |          "line1": "Plaza 2",
       |          "line2": "Iron Masters Way",
       |          "line3": "Telford",
       |          "line4": "Shropshire",
       |          "postalCode": "TF3 4NT",
       |          "countryCode": "IN"
       |        },
       |        "contactDetails": {
       |          "telephone": "01952222222 ",
       |          "mobileNumber": "07222345678 ",
       |          "fax": "01952333333 ",
       |          "email": "bb@bb.com"
       |        }
       |      },
       |      "previousAddressDetails": {
       |        "isPreviousAddressLast12Month": true,
       |        "previousAddressDetails": {
       |          "nonUKAddress": true,
       |          "line1": "Matheson House",
       |          "line2": "R301/01",
       |          "line3": "Telford",
       |          "line4": "Shropshire",
       |          "postalCode": "TF3 4ER",
       |          "countryCode": "US"
       |        }
       |      }
       |    }
       |  ],
       |  "declarationDetails": {
       |    "changeFlag": true,
       |    "box1": true,
       |    "box2": true,
       |    "box3": true,
       |    "box4": true,
       |    "box5": true,
       |    "box6": true,
       |    "box7": true,
       |    "pensionAdvisorDetails": {
       |      "name": "  ",
       |      "addressDetails": {
       |        "nonUKAddress": false,
       |        "line1": "123 Kenilworth Road",
       |        "line2": "Kenilworth",
       |        "line3": "Coventry",
       |        "line4": "Warwickshire",
       |        "postalCode": "CV3 4RT",
       |        "countryCode": "GB"
       |      },
       |      "contactDetails": {
       |        "telephone": "0121234567",
       |        "mobileNumber": "07345673453",
       |        "fax": "0121222777",
       |        "email": "cc@cc.com"
       |      }
       |    }
       |  }
       |}
       |""".stripMargin)


  val psaVariationInvalid = Json.parse(
    """
      |{
      |  "customerIdentificationDetails": {
      |    "legalStatus": "Individual",
      |    "idType": "NINO",
      |    "idNumber": "QQ123456C",
      |    "noIdentifier": true
      |  },
      |  "organisationDetails": {
      |    "name": "ABC Ltd",
      |    "crnNumber": "12345678",
      |    "vatRegistrationNumber": "145899025",
      |    "payeReference": "123A"
      |  },
      |  "individualDetails": {
      |    "title": "Mr",
      |    "firstName": "John",
      |    "middleName": "A",
      |    "lastName": "Smith",
      |    "dateOfBirth": "sdfsadfasdfsadf"
      |  },
      |  "correspondenceAddressDetails": {
      |    "changeFlag": true,
      |    "nonUKAddress": true,
      |    "line1": "24/456",
      |    "line2": "ABC Towers",
      |    "line3": "DEF Colony ",
      |    "line4": "Mumbai",
      |    "postalCode": "516369",
      |    "countryCode": "IN"
      |  },
      |  "correspondenceContactDetails": {
      |    "changeFlag": true,
      |    "telephone": "0121234567",
      |    "mobileNumber": "07888654234",
      |    "fax": "01211289653",
      |    "email": "aaa@aa.com"
      |  },
      |  "previousAddressDetails": {
      |    "changeFlag": true,
      |    "isPreviousAddressLast12Month": true,
      |    "previousAddressDetails": {
      |      "nonUKAddress": true,
      |      "line1": "54-123/A",
      |      "line2": "Mega Colony",
      |      "line3": "RK Puram",
      |      "line4": "Bangalore",
      |      "postalCode": "5000001",
      |      "countryCode": "IN"
      |    }
      |  },
      |  "numberOfDirectorOrPartners": {
      |    "changeFlag": true,
      |    "isMoreThanTenDirectors": true,
      |    "isMoreThanTenPartners": true
      |  },
      |  "changeOfDirectorOrPartnerDetails": true,
      |  "directorOrPartnerDetails": [
      |    {
      |      "sequenceId": "123",
      |      "entityType": "Director",
      |      "title": "Mr",
      |      "firstName": "James",
      |      "middleName": "S",
      |      "lastName": "Little",
      |      "dateOfBirth": "1970-02-28",
      |      "nino": "AA123456D",
      |      "noNinoReason": "jsjsj",
      |      "utr": "1234567890",
      |      "noUtrReason": "  ",
      |      "correspondenceCommonDetails": {
      |        "addressDetails": {
      |          "nonUKAddress": true,
      |          "line1": "Plaza 2",
      |          "line2": "Iron Masters Way",
      |          "line3": "Telford",
      |          "line4": "Shropshire",
      |          "postalCode": "TF3 4NT",
      |          "countryCode": "IN"
      |        },
      |        "contactDetails": {
      |          "telephone": "01952222222 ",
      |          "mobileNumber": "07222345678 ",
      |          "fax": "01952333333 ",
      |          "email": "bb@bb.com"
      |        }
      |      },
      |      "previousAddressDetails": {
      |        "isPreviousAddressLast12Month": true,
      |        "previousAddressDetails": {
      |          "nonUKAddress": true,
      |          "line1": "Matheson House",
      |          "line2": "R301/01",
      |          "line3": "Telford",
      |          "line4": "Shropshire",
      |          "postalCode": "TF3 4ER",
      |          "countryCode": "US"
      |        }
      |      }
      |    }
      |  ],
      |  "declarationDetails": {
      |    "changeFlag": true,
      |    "box1": true,
      |    "box2": true,
      |    "box3": true,
      |    "box4": true,
      |    "box5": true,
      |    "box6": true,
      |    "box7": true,
      |    "pensionAdvisorDetails": {
      |      "name": "  ",
      |      "addressDetails": {
      |        "nonUKAddress": false,
      |        "line1": "123 Kenilworth Road",
      |        "line2": "Kenilworth",
      |        "line3": "Coventry",
      |        "line4": "Warwickshire",
      |        "postalCode": "CV3 4RT",
      |        "countryCode": "GB"
      |      },
      |      "contactDetails": {
      |        "telephone": "0121234567",
      |        "mobileNumber": "07345673453",
      |        "fax": "0121222777",
      |        "email": "cc@cc.com"
      |      }
      |    }
      |  }
      |}
      |""".stripMargin)
}
