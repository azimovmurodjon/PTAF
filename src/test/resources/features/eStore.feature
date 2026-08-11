@eStore @LastScenario
Feature: Consumer Deposit with Payment Switch

  Scenario Outline: Consumer Deposit End to End Flow with one product verifying Payment Switch
    Given we navigate to HARNESS_PREPROD_STAGE url
    Given get title of page
    Then we enter value on page TestHarness locator email_flt value "<Email_ID>"
    When we enter value on page TestHarness locator phoneNumber_flt value "<Phone_Number>"
    And we select on page TestHarness locator product_group_flt value "<ProductType>"
    And we select on page TestHarness locator consumer_deposit_product_name value "<Product_Name>"
    Then time out for 2 seconds
    And we click on page TestHarness locator createURL_btn
#    Then we click on page TestHarness locator openURL_btn
    Then we capture screenshot on page consumer_personal_info locator body name "body"
    Then we click TestHarness locator openURL_btn and switch to popup
    Then time out for 15 seconds
    And we close all browsers

    Examples:
      | Email_ID             | Phone_Number | ProductType      | Product_Name                         |
      | sanjayn@fnb-corp.com | 779-902-2577 | Consumer Deposit | 23901 \| Freestyle Checking \| (DDA) |

  Scenario Outline: Consumer Deposit End to End Flow with one product verifying Payment Switch
    Given we navigate to HARNESS_PREPROD_STAGE url
    Given get title of page
    Then we enter value on page TestHarness locator email_flt value "<Email_ID>"
    When we enter value on page TestHarness locator phoneNumber_flt value "<Phone_Number>"
    And we select on page TestHarness locator product_group_flt value "<ProductType>"
    And we select on page TestHarness locator consumer_deposit_product_name value "<Product_Name>"
    Then time out for 2 seconds
    And we click on page TestHarness locator createURL_btn
#    Then we click on page TestHarness locator openURL_btn
    Then we capture screenshot on page consumer_personal_info locator body name "body"
    Then we click TestHarness locator openURL_btn and switch to popup
    Then time out for 15 seconds
#    And we close all browsers

    Examples:
      | Email_ID             | Phone_Number | ProductType      | Product_Name                         |
      | sanjayn@fnb-corp.com | 779-902-2577 | Consumer Deposit | 23901 \| Freestyle Checking \| (DDA) |

  Scenario Outline: Consumer Deposit - Getting Started Page
    When we click on new page consumer_personal_info locator view_and_accept
    Given we enter value on new page consumer_personal_info locator phoneNumber_txt value "<Phone_Number>"
    Then we enter value on new page consumer_personal_info locator dateOfBirth_txt value "<DOB>"
    Then we capture screenshot on new page consumer_personal_info locator body name "body"
    When we click on new page consumer_personal_info locator canvas
#    And we press on new page consumer_personal_info locator body key "End" keyboard
#    When we click on new page consumer_personal_info locator i_accept_btn
#    And we verify on new page consumer_personal_info of locator NextButton is existed
#    Then we click on new page consumer_personal_info locator NextButton
#
    Examples:
      | DOB        |Phone_Number|
      | 01/01/1988 | 779-902-2577|
#
#  Scenario Outline: Consumer Deposit - Primary Applicant Information page
#    Then time out for 4 seconds
#    And we verify on new page consumer_personal_info of locator ConsumerSSN_flt is existed
#    And we verify on new page consumer_personal_info of locator ConsumerSSN_flt is visible
#    Given we enter value on new page consumer_personal_info locator ConsumerSSN_flt value "<SSN>"
#    And we enter value on new page consumer_personal_info locator Street_Address_1 value "<StAddress>"
#    Then time out for 1 seconds
#    And we press on new page consumer_personal_info locator Street_Address_1 key "ArrowDown" keyboard
#    Then time out for 1 seconds
#    And we press on new page consumer_personal_info locator Street_Address_1 key "Enter" keyboard
#    Then time out for 3 seconds
#    Then we capture screenshot on new page consumer_personal_info locator body name "Consumer Deposits"
#    When we click on new page consumer_personal_info locator NextButton
#    Then time out for 15 seconds
#
#    Examples:
#      | SSN       | StAddress                       |
#      | 123456789 | 3122 Carson Avenue, Murrysville |
#
#  Scenario Outline: Consumer Deposit - Citizenship & Identification page
#    Given we click on new page citizenship locator citizenshipLabel
#    And we click on new page citizenship locator idType
#    Then we click on new page citizenship locator idValue
#    Then we enter value on new page citizenship locator idNumber value "<IdNumber>"
#    Then we enter value on new page citizenship locator stateOfIssue value "<State>"
#    Then we click on new page citizenship locator stateOfIssueValue
#    And we enter value on new page citizenship locator issueDate value "01/01/2010"
#    And we enter value on new page citizenship locator expDate value "01/01/2030"
#    Then we capture screenshot on new page proveValidation locator body name "Consumer Deposits"
#    When we click on new page proveValidation locator NextButton
#
#    Examples:
#      | IdNumber | State |
#      | 98488854 | PA    |
#
#  Scenario: Consumer Deposit - Primary Residence Page
#    And we enter value on new page primaryResidence locator yearsOfResidence value "5"
#    When we click on new page primaryResidence locator mailingAddress
#    Then we capture screenshot on new page primaryResidence locator body name "EndToEndFlow"
#    When we click on new page primaryResidence locator NextButton
#
#  Scenario: Consumer Deposit - Employment And Income Page
#    When we click on new page EmploymentIncome locator employmentStatus
#    When we click on new page EmploymentIncome locator employmentType
#    And we enter value on new page EmploymentIncome locator empName value "FNB"
#    And we enter value on new page EmploymentIncome locator jobTitle value "QA Lead"
#    Then we click on new page EmploymentIncome locator OcpCategory
#    Then we click on new page EmploymentIncome locator OcpType
#    And we enter value on new page EmploymentIncome locator AnnualIncome value "100000"
#    Then we capture screenshot on new page EmploymentIncome locator body name "EndToEndFlow"
#    Then we click on new page EmploymentIncome locator NextButton
#    Then time out for 5 seconds
#
#  Scenario: Consumer Deposit - Adding CoAplicant Page
#    Then we click on new page addCoApplicant locator yesAddCoApp
#    Then we click on new page addCoApplicant locator purpose
#    Then we click on new page addCoApplicant locator purposeType
#    Then we click on new page addCoApplicant locator source
#    Then we click on new page addCoApplicant locator sourceType
#    Then we click on new page addCoApplicant locator cashDeposit
#    Then we click on new page addCoApplicant locator wireTransfer
#    Then we click on new page addCoApplicant locator foreignWTransfer
#    Then we click on new page addCoApplicant locator automaticCoverage
#    And we click on new page addCoApplicant locator view_and_accept
#    And we click on new page addCoApplicant locator canvas
#    Then we press on new page addCoApplicant locator body key "End" keyboard
#    Then time out for 2 seconds
#    When we enter value on new page addCoApplicant locator signature value "Suresh"
#    And we click on new page addCoApplicant locator i_accept_btn
#    Then we click on new page addCoApplicant locator NextButton
#    Then time out for 2 seconds
#    Then we click on new page addCoApplicant locator existingFNBCustomer_No
#    Then we click on new page addCoApplicant locator NextButton
#    Then time out for 3 seconds
#
#  Scenario: Consumer Deposit - Coapp Getting Started Page
#    And we enter value on new page coAppGetStarted locator coAppMobilePhone value "412-708-3809"
#    And we enter value on new page coAppGetStarted locator dateOfBirth value "01/01/1992"
#    And we click on new page coAppGetStarted locator view_and_accept
#    And we click on new page coAppGetStarted locator canvas
#    Then we press on new page coAppGetStarted locator body key "End" keyboard
#    And we click on new page coAppGetStarted locator i_accept_btn
#    Then time out for 1 seconds
#    Then we capture screenshot on new page coAppGetStarted locator body name "EndToEndFlow"
#    When we click on new page coAppGetStarted locator NextButton
#    Then time out for 3 seconds
#
#  Scenario Outline: Consumer Deposit - Coapp Applicant Information page
#    And we enter value on new page coAppPersonalInfo locator coAppFirstName value "ConsumerDeposit"
#    And we enter value on new page coAppPersonalInfo locator coAppLastName value "CoApp"
#    Then we enter value on new page coAppPersonalInfo locator coAppSSN value "229156599"
#    Then we enter value on new page coAppPersonalInfo locator coAppEmail value "<CoAppEmail>"
#    Then we enter value on new page coAppPersonalInfo locator coAppStreet_Address1 value "<StAddress1>"
#    Then we enter value on new page coAppPersonalInfo locator coAppStreet_Address2 value "<StAddress2>"
#    Then we enter value on new page coAppPersonalInfo locator coAppStreet_City value "<City>"
#    Then we enter value on new page coAppPersonalInfo locator coAppState value "<State>"
#    And we press on new page coAppPersonalInfo locator body key "Enter" keyboard
#    Then we enter value on new page coAppPersonalInfo locator coAppZIPCode value "<Zip>"
#    Then time out for 1 seconds
#    Then we capture screenshot on new page coAppPersonalInfo locator body name "EndToEndFlow"
#    When we click on new page coAppPersonalInfo locator NextButton
#    Then time out for 5 seconds
#
#    Examples:
#      | CoAppEmail         | StAddress1     | StAddress2 | City       | State | Zip   |
#      | coapp@fnb-corp.com | 30 Isabella St | 6th Floor  | Pittsburgh | PA    | 15212 |
#
#  Scenario Outline: Consumer Deposit - CoappCitizenship & Identification page
#    Then we click on new page coAppCitizenship locator USCitizenYes
#    And we click on new page coAppCitizenship locator idType
#    Then we click on new page coAppCitizenship locator idValue
#    Then we enter value on new page coAppCitizenship locator idNumber value "<IdNumber>"
#    Then we enter value on new page coAppCitizenship locator stateOfIssue value "<State>"
#    Then we click on new page coAppCitizenship locator stateOfIssueValue
#    And we enter value on new page coAppCitizenship locator issueDate value "01/01/2010"
#    And we enter value on new page coAppCitizenship locator expDate value "01/01/2030"
#    Then we capture screenshot on new page coAppCitizenship locator body name "EndToEndFlow"
#    When we click on new page coAppCitizenship locator NextButton
#
#    Examples:
#      | IdNumber | State |
#      | 98488854 | PA    |
#
#  Scenario: Consumer Deposit - Coapp Residence Page
#    Then time out for 5 seconds
#    And we enter value on new page coAppResidenceAddress locator yearsOfResidence value "5"
#    When we click on new page coAppResidenceAddress locator mailingAddress
#    Then we capture screenshot on new page coAppResidenceAddress locator body name "EndToEndFlow"
#    When we click on new page coAppResidenceAddress locator NextButton
#    Then time out for 5 seconds
#
#  Scenario: Consumer Deposit - CoApp Employment Page
#    When we click on new page CoAppEmploymentIncome locator employmentStatus
#    When we click on new page CoAppEmploymentIncome locator employmentType
#    And we enter value on new page CoAppEmploymentIncome locator empName value "FNB"
#    And we enter value on new page CoAppEmploymentIncome locator jobTitle value "QA Lead"
#    Then we click on new page CoAppEmploymentIncome locator OcpCategory
#    Then we click on new page CoAppEmploymentIncome locator OcpType
#    And we enter value on new page CoAppEmploymentIncome locator AnnualIncome value "100000"
#    Then we capture screenshot on new page CoAppEmploymentIncome locator body name "EndToEndFlow"
#    Then we click on new page CoAppEmploymentIncome locator NextButton
#    Then time out for 5 seconds
#
#  Scenario: Consumer Deposit - Bank Funding
#    Then we click on new page proveValidation locator NextButton
#    And we enter value on new page proveValidation locator amountToFund value "50"
#    When we click on new page proveValidation locator Credit_Debit_Card
#
#  Scenario Outline: Pop-up page
#    And we click on pop frame proveValidation locator Credit_debit_Frame
#    Then we click on pop frame proveValidation locator Card_NbrField
#    When we enter value on pop frame proveValidation locator Card_Nbr value "4111111111111111"
#    And we enter value on pop frame proveValidation locator Card_Expiry value "03/30"
#    And we enter value on pop frame proveValidation locator Card_Code value "123"
#    And we enter value on pop frame proveValidation locator Card_FirstName value "<FName>"
#    And we enter value on pop frame proveValidation locator Card_LastName value "<LName>"
#    And we enter value on pop frame proveValidation locator Card_ZipCode value "<Zip>"
#    Then we click on pop frame proveValidation locator ProcessCard
#    Then we click on new page proveValidation locator CardContinueBtn
#    Then time out for 15 seconds
#
#    Examples:
#      | FName  | LName | Zip   |
#      | Naresh | Kumar | 15212 |
#
#  Scenario Outline: Consumer Deposit - Consumer Primary Disclosure Page
#    When we click on new page Acknowledgement locator certSSN
#    When we click on new page Acknowledgement locator backupWithholding1
#    And we click on new page Acknowledgement locator privacyPolicyLink
#    And we click on new page Acknowledgement locator canvas
#    Then time out for 5 seconds
#    Then we press on new page Acknowledgement locator body key "End" keyboard
#    And we click on new page Acknowledgement locator i_accept_btn
#    And we click on new page Acknowledgement locator depositAgreementAndFeeScheduleLink
#    And we click on new page Acknowledgement locator canvas
#    Then time out for 7 seconds
#    Then we press on new page Acknowledgement locator body key "End" keyboard
#    And we click on new page Acknowledgement locator i_accept_btn
#    And we click on new page Acknowledgement locator <checkingLink>
#    And we click on new page Acknowledgement locator canvas
#    Then we press on new page Acknowledgement locator body key "End" keyboard
#    When we enter value on new page Acknowledgement locator signature value "Suresh"
#    And we click on new page Acknowledgement locator i_accept_btn
#    When we click on new page Acknowledgement locator foreignGovtOfficialNo
#    When we click on new page Acknowledgement locator immediateFamilyMemberNo
#    When we click on new page Acknowledgement locator deplomatNo
#    Then we click on new page Acknowledgement locator NextButton
#    Then time out for 10 seconds
#
#    Examples:
#      | checkingLink          |
#      | freestyleCheckingLink |
#
#  Scenario Outline: Consumer Deposit - Consumer Coapp Disclosure Page
#    When we click on new page Acknowledgement locator certSSN
#    When we click on new page Acknowledgement locator backupWithholding1
#    And we click on new page Acknowledgement locator privacyPolicyLink
#    And we click on new page Acknowledgement locator canvas
#    Then time out for 5 seconds
#    Then we press on new page Acknowledgement locator body key "End" keyboard
#    And we click on new page Acknowledgement locator i_accept_btn
#    And we click on new page Acknowledgement locator depositAgreementAndFeeScheduleLink
#    And we click on new page Acknowledgement locator canvas
#    Then time out for 15 seconds
#    Then we press on new page Acknowledgement locator body key "End" keyboard
#    And we click on new page Acknowledgement locator i_accept_btn
#    And we click on new page Acknowledgement locator <checkingLink>
#    And we click on new page Acknowledgement locator canvas
#    Then we press on new page Acknowledgement locator body key "End" keyboard
#    When we enter value on new page Acknowledgement locator signature value "Suresh"
#    And we click on new page Acknowledgement locator i_accept_btn
#    When we click on new page Acknowledgement locator foreignGovtOfficialNo
#    When we click on new page Acknowledgement locator immediateFamilyMemberNo
#    When we click on new page Acknowledgement locator deplomatNo
#    Then we click on new page Acknowledgement locator NextButton
#    Then time out for 5 seconds
#
#    Examples:
#      | checkingLink          |
#      | freestyleCheckingLink |
#
#  Scenario: Consumer Deposit - Visa® Debit Card Options
#    When we click on new page debitCardOrderCheck locator yesDebitCardAccount
#    When we click on new page debitCardOrderCheck locator yesVisaDebitCoApp
#    Then we click on new page debitCardOrderCheck locator NextButton
#    Then time out for 15 seconds
#    When we click on new page debitCardOrderCheck locator orderChecksYes
#    When we click on new page debitCardOrderCheck locator Submit
#    Then time out for 50 seconds
#
#  Scenario: Consumer Deposit - Navigating to dashboard
#    And we get text and contain on new page dashBoardPage locator trackingCode
#    Then time out for 2 seconds
#    And we contain on new page dashBoardPage of locator headingHappyPath value "You did it! We will take it from here."
#    And we contain on new page dashBoardPage of locator subheadingHappyPath value "Congratulations!"
#    And we contain on new page dashBoardPage of locator sectionHeadingHappyPath value "Personal Accounts"
#    And we contain on new page dashBoardPage of locator summaryHappyPath value "Congratulations! You've got yourself a new account. Below is a summary of products that have been successfully opened."
#    And we verify on new page dashBoardPage of locator dashboardCoApp is visible
#    And we verify on new page dashBoardPage of locator dashboardDebit is visible
#    And we verify on new page dashBoardPage of locator dashboardChecks is existed
#    And we verify on new page dashBoardPage of locator accountOpened is visible
#    And we verify on new page dashBoardPage of locator routingNumber is visible
#    Then we verify on new page dashBoardPage of locator PaymentSwitchGetStarted is visible
#    And we verify on new page dashBoardPage of locator atomicGetStarted is visible
#    And we verify on new page dashBoardPage of locator launchMobileBankingModal is visible
#
#    And we verify on new page dashBoardPage of locator PaymentSwitchGetStarted is visible
#    Then we click on new page dashBoardPage locator PaymentSwitchGetStarted
#    And we capture screenshot on new page PaymentSwitchPopup locator body name "paymentswitchmodal"
#    And we click on new page dashBoardPage locator iosAppText
#    Then time out for 5 seconds
#    And we click on new page dashBoardPage locator androidAppText
#    Then time out for 5 seconds
#    And we click on new page dashBoardPage locator closeMobileModal
#    And we verify on new page dashBoardPage of locator paymentActionComplete is visible
#    Then time out for 5 seconds
#    Then we capture screenshot on new page dashBoardPage locator body name "paymentswitch-postaction"
#
#    And we verify on new page dashBoardPage of locator atomicGetStarted is visible
#    And we click on new page dashBoardPage locator launchAtomic
#    Then we click on atomic frame atomicModal locator start
#    Then we click on atomic frame atomicModal locator selectEmployer
#    Then we click on atomic frame atomicModal locator selectHomeDepot
#    Then we enter value on atomic frame atomicModal locator username value "test-good"
#    Then time out for 2 seconds
#    Then we click on atomic frame atomicModal locator loginContinue
#    Then time out for 2 seconds
#    Then we enter value on atomic frame atomicModal locator password value "password"
#    Then time out for 2 seconds
#    Then we click on atomic frame atomicModal locator loginContinue
#    Then time out for 2 seconds
#    Then we click on atomic frame atomicModal locator submitDDS
#    Then time out for 10 seconds
#    Then we click on atomic frame atomicModal locator closeAtomicModal
#    Then time out for 8 seconds
#    Then we verify on new page dashBoardPage of locator ddsComplete is visible
#    And we capture screenshot on new page proveValidation locator body name "01_DDS_Smoke_DDS_DashboardPage"