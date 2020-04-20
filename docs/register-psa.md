Register-Psa
-----------------------
Register a pension scheme administrator

* **URL**

  `/register-psa`

* **Method**

  `POST`

* **Example Payload**

```json
{
   "individualDetails":{
      "firstName":"Stephen",
      "lastName":"Wood",
      "dateOfBirth":"1990-01-01"
   },
   "individualAddress":{
      "addressLine1":"100 SuttonStreet",
      "addressLine2":"Wokingham",
      "addressLine3":"Surrey",
      "addressLine4":"London",
      "postalCode":"DH14EJ",
      "countryCode":"GB"
   },
   "registrationInfo":{
      "legalStatus":"Individual",
      "sapNumber":"1234567890",
      "noIdentifier":false,
      "customerType":"UK",
      "idType":"NINO",
      "idNumber":"CS700100A"
   },
   "individualDetailsCorrect":true,
   "individualAddressYears":"over_a_year",
   "individualContactDetails":{
      "email":"test@test.com",
      "phone":"3247234"
   },
   "declaration":true,
   "declarationWorkingKnowledge":"workingKnowledge",
   "declarationFitAndProper":true,
   "existingPSA":{
      "isExistingPSA":false
   }
}

```

* **Success Response:**

  * **Code:** 200 <br />

* **Example Success Response**

```json
{
   "processingDate":"2020-04-20",
   "formBundle":"1121313",
   "psaId":"A21999999"
}

```

* **Error Response:**

  * **Code:** 400 BAD_REQUEST <br />
    **Content:** `{
                     "code": "INVALID_PAYLOAD",
                     "reason": "Submission has not passed validation. Invalid Payload."
                  }`

  * **Code:** 409 CONFLICT <br />
    **Content:** `{
                              "code": "DUPLICATE_SUBMISSION",
                              "reason": "Duplicate submissionacknowledgement reference from remote endpoint returned"
                          }`
    
  * **Code:** 4XX Upstream4xxResponse <br />

  OR anything else

  * **Code:** 5XX Upstream5xxResponse <br />