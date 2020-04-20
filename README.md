Pension Administrator
==================================

Back-end microservice to support the registration, variation, invitation, association and de registration of pension administrator

API
---

| *Task* | *Supported Methods* | *Description* |
|--------|----|----|
| ```/register-with-no-id/organisation                                     ```  | POST   | Register with no Id for NON-UK Organisation [More...](docs/register-with-no-id-org.md) |
| ```/register-with-id/individual                                       ```  | POST   | Register with Id for Individual [More...](docs/register-with-no-id-ind.md) |
| ```/register-with-id/organisation                                     ```  | POST    | Register with Id for Organisation [More...](docs/register-with-id-org.md) |
| ```/register-with-no-id/individual                                              ```  | POST    | Register with no Id for NON-UK Individual [More...](docs/register-with-id-ind.md) |
| ```/psa-subscription-details                                              ```  | GET    | Get Psa Subscription Details [More...](docs/psa-subscription-details.md) |
| ```/register-psa                                              ```  | POST    | Register a Pension scheme administrator [More...](docs/register-psa.md) |
| ```/remove-psa                                              ```  | POST    | Remove a Psa [More...](docs/remove-psa.md) |
| ```/deregister-psa/:psaId                                              ```  | DELETE    | De Register a Psa [More...](docs/deregister-psa.md) |
| ```/can-deregister/:id                                              ```  | GET    | Can de register a Psa [More...](docs/can-deregister.md) |
| ```/psa-variation/:psaId                                              ```  | POST    | Update Psa Subscription Details [More...](docs/psa-variation.md) |
| ```/get-minimal-psa                                              ```  | GET    | Get Minimal Psa Details [More...](docs/get-minimal-psa.md) |
| ```/accept-invitation                                              ```  | POST    | Accept Invitation [More...](docs/accept-invitation.md) |
| ```/invite                                              ```  | GET    | Send an invite [More...](docs/invite.md) |
| ```/email-response/:journeyType/*id                                              ```  | GET    | Sends an audit event with the correct response returned from an email service |
| ```/get-name                                              ```  | GET    | Get Psa Name |
| ```/get-email                                              ```  | GET    | Get Psa Email |
| ```/journey-cache/manage-pensions/:id               ```  | GET    | Returns the data from Manage Pensions Cache 
| ```/journey-cache/manage-pensions/:id               ```  | POST    | Save the data to Manage Pensions Cache
| ```/journey-cache/manage-pensions/:id               ```  | DELETE    | Delete the data from Manage Pensions Cache
| ```/journey-cache/psa-data/:id               ```  | GET   | Returns the data from Psa Data Cache 
| ```/journey-cache/psa-data/:id               ```  | POST   | Saves the data to Psa Data Cache 
| ```/journey-cache/psa-data/:id               ```  | DELETE   | Delete the data from Psa Data Cache
| ```/invitation/get-for-scheme                                              ```  | GET    | Get data for selected scheme from Invitation Cache |
| ```/invitation/get-for-invitee                                              ```  | GET    | Get data for selected invitee from Invitation Cache |
| ```/invitation/get                                              ```  | GET    | Get all the data from Invitation Cache |
| ```/invitation/add                                              ```  | GET    | Add the data to invitation Cache |
| ```/invitation                                              ```  | GET    | Get AFT Overview |


