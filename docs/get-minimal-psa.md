Get-Minimal-Psa
-----------------------
Returns the minimal psa details.

* **URL**

  `/get-minimal-psa`

* **Method**

  `GET`
  
*  **Request Header**
 
  `psaId`

* **Success Response:**

  * **Code:** 200 <br />

* **Example Success Response**

```json
{
   "email":"test@email.com",
   "isPsaSuspended":true,
   "individualDetails":{
      "firstName":"testFirst",
      "middleName":"testMiddle",
      "lastName":"testLast"
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