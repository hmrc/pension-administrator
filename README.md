# Pension Administrator

## Overview

This is the backend repository for Pension Administrator. This service allows a user to register and perform duties as a pension administrator. The pension administrator is the person or organisation responsible for the overall management of a pension scheme. A user registers to become a pension scheme administrator. As a pension scheme administrator they can invite others to and remove others from a scheme. 

This service has a corresponding front-end microservice, namely pension-administrator-frontend.

**Associated Frontend Link:** https://github.com/hmrc/pension-administrator-frontend

**Stubs:** https://github.com/hmrc/pensions-scheme-stubs



## Requirements
This service is written in Scala and Play, so needs at least a [JRE] to run.

**Node version:** 20.18.0

**Java version:** 19

**Scala version:** 2.13.14


## Running the Service
**Service Manager Profile:** PODS_ALL

**Port:** 8205

**Link:** http://localhost:8201/register-as-pension-scheme-administrator/registered-psa-details

In order to run the service, ensure Service Manager is installed (see [MDTP guidance](https://docs.tax.service.gov.uk/mdtp-handbook/documentation/developer-set-up/set-up-service-manager.html) if needed) and launch the relevant configuration by typing into the terminal:
`sm2 --start PODS_ALL`

To run the service locally, enter `sm2 --stop PENSION_ADMINISTRATOR`.

In your terminal, navigate to the relevant directory and enter `sbt run`.

Access the Authority Wizard and login with the relevant enrolment details [here](http://localhost:9949/auth-login-stub/gg-sign-in)


## Enrolments
There are several different options for enrolling through the auth login stub. In order to enrol as a dummy user to access the platform for local development and testing purposes, the following details must be entered on the auth login page.


For access to the **Pension Administrator dashboard** for local development, enter the following information: 

**Redirect url -** http://localhost:8204/manage-pension-schemes/overview 

**GNAP Token -** NO 

**Affinity Group -** Organisation 

**Enrolment Key -** HMRC-PODS-ORG 

**Identifier Name -** PsaID 

**Identifier Value -** A2100005

---

If you wish to access the **Pension Practitioner dashboard** for local development, enter the following information: 

**Redirect URL -** http://localhost:8204/manage-pension-schemes/dashboard 

**GNAP Token -** NO 

**Affinity Group -** Organisation 

**Enrolment Key -** HMRC-PODSPP-ORG 

**Identifier Name -** PspID 

**Identifier Value -** 21000005

---

**Dual enrolment** as both a Pension Administrator and Practitioner is also possible and can be accessed by entering:

**Redirect url -** http://localhost:8204/manage-pension-schemes/overview 

**GNAP Token -** NO 

**Affinity Group -** Organisation 

**Enrolment Key 1 -** HMRC-PODSPP-ORG Identifier 

**Name 1 -** PspID Identifier 

**Value 1 -** 21000005

**Enrolment Key 2 -** HMRC-PODS-ORG 

**Identifier Name 2 -** PsaID 

**Identifier Value 2 -** A2100005

---

To access the **Scheme Registration journey**, enter the following information:

**Redirect URL -** http://localhost:8204/manage-pension-schemes/you-need-to-register 

**GNAP Token -** NO 

**Affinity Group -** Organisation

---


## Compile & Test
**To compile:** Run `sbt compile`

**To test:** Use `sbt test`

**To view test results with coverage:** Run `sbt clean coverage test coverageReport`

For further information on the PODS Test Approach and wider testing including acceptance, accessibility, performance, security and E2E testing, visit the PODS Confluence page [here](https://confluence.tools.tax.service.gov.uk/pages/viewpage.action?spaceKey=PODSP&title=PODS+Test+Approach).

For Journey Tests, visit the [Journey Test Repository](| Journey tests(https://github.com/hmrc/pods-journey-tests).

View the prototype [here](https://pods-event-reporting-prototype.herokuapp.com/).


## Navigation and Dependent Services
The Pension Administrator Frontend integrates with the Manage Pension Schemes (MPS) service and uses various stubs available on [GitHub](https://github.com/hmrc/pensions-scheme-stubs). From the Authority Wizard page you will be redirected to the dashboard. Navigate to the appropriate area by accessing items listed within the service-specific tiles on the dashboard. On the Pension Administrator frontend, an administrator can change their details, stop being an administrator and check for invitations, explore Penalties & Charges, manage and migrate pension schemes.


There are numerous APIs implemented throughout the MPS architecture, and the relevant endpoints are illustrated below. For an overview of all PODS APIs, refer to the [PODS API Documentation](https://confluence.tools.tax.service.gov.uk/display/PODSP/PODS+API+Latest+Version).


## Service-Specific Documentation [To Do]
Include relevant links or details to any additional, service-specific documents (e.g., stubs, testing protocols) when available. 

## API

| *Task*                                                                               | *Supported Methods* | *Description*                                                                                                                                       |
|--------------------------------------------------------------------------------------|---------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------|
| ```/register-with-id/individual                ```                                   | POST                | Returns the Business Partner Record for an individual based on the NINO/UTR from ETMP [More...](docs/register-with-id-ind.md)                       |
| ```/register-with-id/organisation                 ```                                | POST                | Returns the Business Partner Record for an organisation from ETMP based on the UTR[More...](docs/register-with-id-org.md)                           |
| ```/register-with-no-id/organisation                                     ```         | POST                | Registers an organisation on ETMP who does not have a UTR. Typically this will be a non- UK organisation [More...](docs/register-with-no-id-org.md) |
| ```/register-with-no-id/individual                                              ```  | POST                | Registers an individual on ETMP who does not have a UTR/NINO. Typically this will be a non- UK individual [More...](docs/register-with-id-ind.md)   |
| ```/psa-subscription-details                                              ```        | GET                 | Get PSA Subscription Details [More...](docs/psa-subscription-details.md)                                                                            |
| ```/register-psa                                              ```                    | POST                | Subscribe a pension scheme administrator [More...](docs/register-psa.md)                                                                            |
| ```/remove-psa                                              ```                      | POST                | Remove a PSA from the scheme [More...](docs/remove-psa.md)                                                                                          |
| ```/deregister-psa/:psaId                                              ```           | DELE                | De Register a PSA [More...](docs/deregister-psa.md)                                                                                                 |
| ```/can-deregister/:id                                              ```              | GET                 | Can de register a PSA [More...](docs/can-deregister.md)                                                                                             |
| ```/psa-variation/:psaId                                              ```            | POST                | Update PSA Subscription Details [More...](docs/psa-variation.md)                                                                                    |
| ```/get-minimal-psa                                              ```                 | GET                 | Get PSA minimal Details [More...](docs/get-minimal-psa.md)                                                                                          |
| ```/accept-invitation                                              ```               | POST                | Accept an invitation to administer a scheme [More...](docs/accept-invitation.md)                                                                    |
| ```/invite                                              ```                          | GET                 | Send an invite to a PSA for administering a scheme [More...](docs/invite.md)                                                                        |
| ```/email-response/:journeyType/*id                                              ``` | GET                 | Sends an audit event with the correct response returned from an email service                                                                       |
| ```/get-name                                              ```                        | GET                 | Get PSA Name                                                                                                                                        |
| ```/get-email                                              ```                       | GET                 | Get PSA Email                                                                                                                                       |
| ```/journey-cache/manage-pensions/:id        ```                                     | GET                 | Returns the data from Manage Pensions Cache                                                                                                         |
| ```/journey-cache/manage-pensions/:id        ```                                     | POST                | Save the data to Manage Pensions Cache                                                                                                              |
| ```/journey-cache/manage-pensions/:id        ```                                     | DELETE              | Delete the data from Manage Pensions Cache                                                                                                          |
| ```/journey-cache/psa-data/:id               ```                                     | GET                 | Returns the data from Psa Data Cache                                                                                                                |
| ```/journey-cache/psa-data/:id               ```                                     | POST                | Saves the data to Psa Data Cache                                                                                                                    |
| ```/journey-cache/psa-data/:id               ```                                     | DELETE              | Delete the data from Psa Data Cache                                                                                                                 |
| ```/invitation/get-for-scheme                ```                                     | GET                 | Get data for Scheme from Invitation Cache                                                                                                           |
| ```/invitation/get-for-invitee               ```                                     | GET                 | Get data for invitee PSA Id from Invitation Cache                                                                                                   |
| ```/invitation/get                           ```                                     | GET                 | Get all the data from Invitation Cache based on invitee PSA Id and Pstr                                                                             |
| ```/invitation/add                           ```                                     | GET                 | Add the data to invitation Cache                                                                                                                    |
| ```/invitation                              ```                                      | DELETE              | Remove the data from Invitation Cache based on invitee PSA Id and Pstr                                                                              |


## License
This code is open source software Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at:

[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
