Psa-subscription-details
-----------------------
Returns the Psa Subscription details for a given PsaId.

* **URL**

  `/psa-subscription-details`

* **Method**

  `GET`
  
*  **Request Header**
 
  `psaId`

* **Success Response:**

  * **Code:** 200 <br />

* **Example Success Response**

```json
{
   "isSuspended":false,
   "customerIdentification":{
      "legalStatus":"Individual",
      "typeOfId":"NINO",
      "number":"AA999999A",
      "isOverseasCustomer":true
   },
   "individual":{
      "title":"Mr",
      "firstName":"abcdefghijkl",
      "middleName":"abcdefghijkl",
      "lastName":"abcdefjkl",
      "dateOfBirth":"1947-03-29"
   },
   "address":{
      "addressLine1":"Telford1",
      "addressLine2":"Telford2",
      "addressLine3":"Telford3",
      "addressLine4":"Telford3",
      "countryCode":"GB",
      "postalCode":"TF3 4ER"
   },
   "contact":{
      "telephone":" ",
      "email":"aaa@aa.com"
   },
   "isSameAddressForLast12Months":true,
   "previousAddress":{
      "addressLine1":"London1",
      "addressLine2":"London2",
      "addressLine3":"London3",
      "addressLine4":"London4",
      "countryCode":"GB",
      "postalCode":"LN12 4DC"
   },
   "directorsOrPartners":[
      {
         "isDirectorOrPartner":"Director",
         "title":"Mr",
         "firstName":"abcdef",
         "middleName":"dfgdsfff",
         "lastName":"dfgfdgfdg",
         "dateOfBirth":"1950-03-29",
         "nino":"AA999999A",
         "utr":"1234567892",
         "isSameAddressForLast12Months":true,
         "previousAddress":{
            "addressLine1":"line1",
            "addressLine2":"line2",
            "addressLine3":"line3",
            "addressLine4":"line4",
            "countryCode":"AD",
            "postalCode":"567253"
         },
         "correspondenceDetails":{
            "address":{
               "addressLine1":"addressline1",
               "addressLine2":"addressline2",
               "addressLine3":"addressline3",
               "addressLine4":"addressline4",
               "countryCode":"GB",
               "postalCode":"B5 9EX"
            },
            "contactDetails":{
               "telephone":"0044-09876542312",
               "email":"abc@hmrc.gsi.gov.uk"
            }
         }
      },
      {
         "isDirectorOrPartner":"Director",
         "title":"Mr",
         "firstName":"sdfdff",
         "middleName":"sdfdsfsdf",
         "lastName":"dfdsfsf",
         "dateOfBirth":"1950-07-29",
         "nino":"AA999999A",
         "utr":"7897700000",
         "isSameAddressForLast12Months":true,
         "previousAddress":{
            "addressLine1":"werrertqe",
            "addressLine2":"ereretfdg",
            "addressLine3":"asafafg",
            "addressLine4":"fgdgdasdf",
            "countryCode":"AD",
            "postalCode":"23424"
         },
         "correspondenceDetails":{
            "address":{
               "addressLine1":"fgfdgdfgfd",
               "addressLine2":"dfgfdgdfg",
               "addressLine3":"fdrtetegfdgdg",
               "addressLine4":"dfgfdgdfg",
               "countryCode":"AD",
               "postalCode":"56546"
            },
            "contactDetails":{
               "telephone":"0044-09876542334",
               "email":"aaa@gmail.com"
            }
         }
      }
   ],
   "pensionAdvisor":{
      "name":"sgfdgssd",
      "address":{
         "addressLine1":"addline1",
         "addressLine2":"addline2",
         "addressLine3":"addline3",
         "addressLine4":"addline4 ",
         "countryCode":"AD",
         "postalCode":"56765"
      },
      "contactDetails":{
         "telephone":"0044-0987654232",
         "email":"aaa@yahoo.com"
      }
   }
}

```

* **Error Response:**

  * **Code:** 400 BAD_REQUEST <br />
    **Content:** `{
                     "code": "INVALID_PSAID",
                     "reason": "Invalid parameter PSAID."
                  }`

  * **Code:** 4XX Upstream4xxResponse <br />

  OR anything else

  * **Code:** 5XX Upstream5xxResponse <br />