@secondPageTest
@LastScenario
Feature: Second Page Test

#  Background: Navigate to URL
#    Given we navigate to tool_qa_url url
#    Then get title of page

  Scenario: Opening New page Same Browser
    Given we navigate to tool_qa_url url
    Then get title of page
    Given we click on page homePage locator alert_frame_and_window
    Then we click on page homePage locator browser_window
    When we click on page homePage locator new_tab_btn
    And we get text on new page homePage locator tab_semple_heading
#    And we close all browsers
    When we capture screenshot on page homePage locator body name "body"
    Then we click on page homePage locator frame_btn
    And we get text on frame homePage locator frame_semple_heading

  Scenario: Verify Nested Frame
    Given we click on page homePage locator header_image
    When we click on page homePage locator alert_frame_and_window
    Then we click on page homePage locator nested_frame_tab
    And we get text on frame homePage locator first_frame_txt
    And we click on second frame homePage locator second_frame_txt
    Then we get text on second frame homePage locator second_frame_txt
    When we capture screenshot on second frame homePage locator body name "body"
#    And we close all browsers

  Scenario: Verify Nested Frame
    Given we click on page homePage locator header_image
    When we click on page homePage locator alert_frame_and_window
    Then we click on page homePage locator nested_frame_tab
    And we get text on frame homePage locator first_frame_txt
    And we click on second frame homePage locator second_frame_txt
    Then we get text on second frame homePage locator second_frame_txt
    When we capture screenshot on second frame homePage locator body name "body"

  Scenario: Test Download Document Method
    Given we click on page homePage locator header_image
    Then we click on page homePage locator elements_tab
    When we click on page homePage locator update_and_download_tab
    When we capture screenshot on page homePage locator body name "body"
    Then we click download on page homePage locator download_btn
    And we wait for some time
    Then we click on page homePage locator upload_btn
    And we select document to upload on page homePage locator upload_btn
    Then we wait for some time
