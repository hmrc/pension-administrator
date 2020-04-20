Register-with-no-id
-----------------------
Register with no Id for NON-UK Organisation. This is for business matching.

* **URL**

  `/register-with-no-id/organisation`

* **Method**

  `POST`

* **Example Payload**

```json
{
   "organisation":{
      "organisationName":"XYZ Ltd"
   },
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
	"processingDate": "2001-12-17T09:30:47Z",
	"formBundleNumber": "123456789012"
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