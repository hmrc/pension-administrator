Can-deregister
-----------------------
Returns true if the Psa can be de registered.

* **URL**

  `/can-deregister/:id`

* **Method**

  `GET`
  
* **URL Parameter**

  `psaId`  

* **Success Response:**

  * **Code:** 200 with true/false<br />
  
 
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
                            "code": "ALREADY_DEREGISTERED",
                            "reason": "The back end has indicated that Taxpayer is already deregistered"
                        }`
    
  * **Code:** 4XX Upstream4xxResponse <br />

  OR anything else

  * **Code:** 5XX Upstream5xxResponse <br />