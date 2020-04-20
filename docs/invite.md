Invite
-----------------------
Send an invitation

* **URL**

  `/invite`

* **Method**

  `POST`

* **Example Payload**

```json
{
   "pstr":"00000000AA",
   "schemeName":"Benefits Scheme",
   "inviterPsaId":"A1000001",
   "inviteePsaId":"A2000001",
   "inviteeName":"Test Invitee"
}

```

* **Success Response:**

  * **Code:** 204 No Content <br />

* **Error Response:**

  * **Code:** 400 BAD_REQUEST <br />
    **Content:** `Bad Request with no request body returned for invite PSA`

  * **Code:** 403 FORBIDDEN <br />
    **Content:** `The invitation is to a PSA already associated with this scheme`
    
  * **Code:** 404 NOT_FOUND <br />
    **Content:** `The name and PSA Id do not match`
    
  * **Code:** 4XX Upstream4xxResponse <br />

  OR anything else

  * **Code:** 5XX Upstream5xxResponse <br />