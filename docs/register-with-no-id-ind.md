Register-with-no-id
-----------------------
Register with no Id for NON-UK Individual. This is for business matching.

* **URL**

  `/register-with-no-id/individual`

* **Method**

  `POST`

* **Example Payload**

```json
{
   "firstName":"John",
   "lastName":"Smith",
   "dateOfBirth":"1990-04-03",
   "address":{
      "addressLine1":"31 Myers Street",
      "addressLine2":"Haddonfield",
      "addressLine3":"Illinois",
      "addressLine4":"USA",
      "country":"US"
   }
}
```

* **Success Response:**

  * **Code:** 200 <br />

* **Example Success Response**

```json
{
   "safeId":"XE0001234567890",
   "sapNumber":"1234567890"
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