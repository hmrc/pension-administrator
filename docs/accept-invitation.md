Accept-invitation
-----------------------
Accepts an invitation

* **URL**

  `/accept-invitation`

* **Method**

  `POST`

* **Example Payload**

```json
{
   "pstr":"test-pstr",
   "inviteePsaId":"A7654321",
   "inviterPsaId":"A1234567",
   "declaration":true,
   "declarationDuties":true
}

```

* **Success Response:**

  * **Code:** 204 No Content <br />

* **Error Response:**

  * **Code:** 400 BAD_REQUEST <br />
    **Content:** `{
                     "code": "INVALID_PAYLOAD",
                     "reason": "Submission has not passed validation. Invalid Payload."
                  }`

  * **Code:** 403 FORBIDDEN <br />
    **Content:** `{
                              "code": "INVALID_INVITEE_PSAID",
                              "reason": "The back end has indicated that there is    Invalid Invitee PSAID."
                          }`
                          
  * **Code:** 403 FORBIDDEN <br />
    **Content:** `{
                                "code": "ACTIVE_RELATIONSHIP_EXISTS",
                                "reason": "TThe back end has indicated that there is activerelationship exists between scheme BP and PSA BP."
                            }`
    
  * **Code:** 404 NOT_FOUND <br />
    **Content:** `{
                              "code": "NOT_FOUND",
                              "reason": "The back end has indicated that there is    no match found."
                          }`
    
  * **Code:** 4XX Upstream4xxResponse <br />

  OR anything else

  * **Code:** 5XX Upstream5xxResponse <br />