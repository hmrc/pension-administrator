Pension Administrator
==================================

Back-end microservice to support the registration, variation, invitation, association and de registration of pension administrator

API
---

| *Task* | *Supported Methods* | *Description* |
|--------|----|----|
| ```/register-with-id/individual                                       ```  | POST   | Returns the Business Partner Record for an individual based on the NINO/UTR from ETMP [More...](docs/register-with-id-ind.md) |
| ```/register-with-id/organisation                                     ```  | POST    |   Returns the Business Partner Record for an organisation from ETMP based on the UTR[More...](docs/register-with-id-org.md) |
| ```/register-with-no-id/organisation                                     ```  | POST   |  Registers an organisation on ETMP who does not have a UTR. Typically this will be a non- UK organisation [More...](docs/register-with-no-id-org.md) |
| ```/register-with-no-id/individual                                              ```  | POST    | Registers an individual on ETMP who does not have a UTR/NINO. Typically this will be a non- UK individual [More...](docs/register-with-id-ind.md) |
| ```/psa-subscription-details                                              ```  | GET    | Get PSA Subscription Details [More...](docs/psa-subscription-details.md) |
| ```/register-psa                                              ```  | POST    | Subscribe a pension scheme administrator [More...](docs/register-psa.md) |
| ```/remove-psa                                              ```  | POST    | Remove a PSA from the scheme [More...](docs/remove-psa.md) |
| ```/deregister-psa/:psaId                                              ```  | DELETE    | De Register a PSA [More...](docs/deregister-psa.md) |
| ```/can-deregister/:id                                              ```  | GET    | Can de register a PSA [More...](docs/can-deregister.md) |
| ```/psa-variation/:psaId                                              ```  | POST    | Update PSA Subscription Details [More...](docs/psa-variation.md) |
| ```/get-minimal-psa                                              ```  | GET    | Get PSA minimal Details [More...](docs/get-minimal-psa.md) |
| ```/accept-invitation                                              ```  | POST    | Accept an invitation to administer a scheme [More...](docs/accept-invitation.md) |
| ```/invite                                              ```  | GET    | Send an invite to a PSA for administering a scheme [More...](docs/invite.md) |
| ```/email-response/:journeyType/*id                                              ```  | GET    | Sends an audit event with the correct response returned from an email service |
| ```/get-name                                              ```  | GET    | Get PSA Name |
| ```/get-email                                              ```  | GET    | Get PSA Email |
| ```/journey-cache/manage-pensions/:id               ```  | GET    | Returns the data from Manage Pensions Cache 
| ```/journey-cache/manage-pensions/:id               ```  | POST    | Save the data to Manage Pensions Cache
| ```/journey-cache/manage-pensions/:id               ```  | DELETE    | Delete the data from Manage Pensions Cache
| ```/journey-cache/psa-data/:id               ```  | GET   | Returns the data from Psa Data Cache 
| ```/journey-cache/psa-data/:id               ```  | POST   | Saves the data to Psa Data Cache 
| ```/journey-cache/psa-data/:id               ```  | DELETE   | Delete the data from Psa Data Cache
| ```/invitation/get-for-scheme                                              ```  | GET    | Get data for Scheme from Invitation Cache |
| ```/invitation/get-for-invitee                                              ```  | GET    | Get data for invitee PSA Id from Invitation Cache |
| ```/invitation/get                                              ```  | GET    | Get all the data from Invitation Cache based on invitee PSA Id and Pstr|
| ```/invitation/add                                              ```  | GET    | Add the data to invitation Cache |
| ```/invitation                                              ```  | DELETE    | Remove the data from Invitation Cache based on invitee PSA Id and Pstr |


