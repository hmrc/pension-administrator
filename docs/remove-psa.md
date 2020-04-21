Remove-psa
-----------------------
 Remove PSA from the scheme.

* **URL**

  `/remove-psa`

* **Method**

  `POST`

* **Example Payload**

```json
{
   "psaId":"A7654321",
   "pstr":"123456789AB",
   "removalDate":"2018-02-01"
}

```

* **Success Response:**

  * **Code:** 204 No Content<br />

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
                          
  * **Code:** 403 FORBIDDEN <br />
    **Content:** `{
                            "code": "ACTIVE_RELATIONSHIP_EXISTS",
                            "reason": "The back end has indicated that PSA has an activerelationship with scheme"
                        }`
                        
  * **Code:** 403 FORBIDDEN <br />
    **Content:** `{
                            "code": "NO_OTHER_ASSOCIATED_PSA",
                            "reason": "The back end has indicated that Scheme does not have any other associated PSA"
                        }`
    
  * **Code:** 4XX Upstream4xxResponse <br />

  OR anything else

  * **Code:** 5XX Upstream5xxResponse <br />