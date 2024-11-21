
# digital-platform-reporting-submission-frontend

The Reporting Rules for Digital Platforms is a new international tax policy that has been designed by the Organisation for Economic Cooperation and Development (OECD) with the input of many tax jurisdictions around the world. The policy was designed in response to concerns many tax jurisdictions raised that the ‘Gig Economy’ may not be taxed fairly or transparently enough and presented risks to the revenue to most tax jurisdictions. This policy also sits alongside the EU’s equivalent policy called Directive of Administrative Cooperation 7 or DAC7 which was implement by EU members on January 23.

This frontend microservice allows users to submit reports containing seller data to HMRC in a predefined XML schema format. HMRC validates these reports before exchanging the information with other jurisdictions. Key features include:
Submit the seller information required for reporting obligations.
* Submit the reports containing seller data to HMRC (following a defined schema in XML format), which HMRC will then validate and then exchange with other jurisdictions
* Submit the required seller data to HMRC via a file upload mechanism (following a defined schema in XML format) or complete an assumed reporting submission

## Key Terminologies
Digital Platform Operators:
These can vary significantly in size, employee headcount, number of sellers, products/ services offered to customers and number/value of transactions. These are the stakeholders who have the legal obligation to submit data to HMRC if they fall into scope of the policy.

Third parties:
These can be agents or service providers who will act potentially on behalf of digital platforms who are in scope of the policy. Some digital platforms may submit data themselves, others may use these third parties to act on their behalf.

HMRC:
HMRC (via the International Data Exchange Team - IDET) will be responsible for receiving the data submitted, ensuring that users are supported in meeting their legal obligations to submit data, ensuring the data is acquired and exchanged appropriately as per the policy.

Sellers:
Individuals, businesses or companies who are actively selling products or services to customers - referred to as the “Gig Economy” by the OECD. Although they will not use the service, it will be their data that will be submitted to HMRC by the respective digital platforms.

Running the service
You can use service manage to run all dependent microservices using the command below

    sm2 --start DPRS_ALL
Or you could run this microservice locally using

    sm2 --start DIGITAL_PLATFORM_REPORTING_SUBMISSION_FRONTEND
    sbt run

Stopping the service

You can use stop service and dependent microservices using the command below

    sm2 --stop DPRS_ALL
Or you could stop this microservice locally using

    sm2 --stop DIGITAL_PLATFORM_REPORTING_SUBMISSION_FRONTEND

To run locally:

Navigate to http://localhost:9949/auth-login-stub/gg-sign-in which redirects to auth-login-stub page.

Redirect URL: http://localhost:20007/digital-platform-reporting/submission/which-platform-operator

    Affinity Group: Organisation, Individual or Agent

    Enrolment Key: HMRC-DPRS
    Identifier Name: DPRSID
    Identifier Value: 1 (This could be any number)

**Integration and unit tests**

To run the unit tests:

Run 'sbt test' from directory the project is stored in

To run Integration tests:

    sbt it/test


### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").