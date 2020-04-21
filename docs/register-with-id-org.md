Register-with-id
-----------------------
Register with id for UK Organisation. This is for business matching.

* **URL**

  `/register-with-id/organisation`

* **Method**

  `POST`

* **Example Payload**

```json
{
   "utr":"1100000000",
   "organisationName":"Test Ltd",
   "organisationType":"LLP"
}

```

* **Success Response:**

  * **Code:** 200 <br />

* **Example Success Response**

```json
{
   "safeId":"XE0001234567890",
   "sapNumber":"1234567890",
   "isAnIndividual":false,
   "organisation":{
      "organisationName":"Test Ltd",
      "isAGroup":false,
      "organisationType":"LLP"
   },
   "address":{
      "addressLine1":"100 SuttonStreet",
      "addressLine2":"Wokingham",
      "addressLine3":"Surrey",
      "addressLine4":"London",
      "countryCode":"GB",
      "postalCode":"DH14EJ"
   },
   "contactDetails":{
      "primaryPhoneNumber":"01332752856",
      "secondaryPhoneNumber":"07782565326",
      "faxNumber":"01332754256",
      "emailAddress":"stephen@manncorpone.co.uk"
   }
}

```

* **Error Response:**

  * **Code:** 400 BAD_REQUEST <br />
    **Content:** `{
                     "code": "INVALID_PAYLOAD",
                     "reason": "Submission has not passed validation. Invalid Payload."
                  }`

  * **Code:** 403 FORBIDDEN <br />
    **Content:** `{
                              "code": "INVALID_SUBMISSION",
                              "reason": "Duplicate Submission."
                          }`
    
  * **Code:** 404 NOT_FOUND <br />
    **Content:** `{
                              "code": "NOT_FOUND",
                              "reason": "Resource not found."
                          }`
    
  * **Code:** 4XX Upstream4xxResponse <br />

  OR anything else

  * **Code:** 5XX Upstream5xxResponse <br />