package com.codeborne.selenide;

import org.junit.Test;
import org.openqa.selenium.WebDriver;

import static com.codeborne.selenide.WebDriverRunner.FIREFOX;
import static com.codeborne.selenide.WebDriverRunner.cleanupWebDriverExceptionMessage;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;

public class WebDriverRunnerTest {

  static WebDriver driver = mock(WebDriver.class);

  @Test
  public void allowsToSpecifyCustomWebDriverConfiguration() {
    WebDriverRunner.closeWebDriver();
    Configuration.browser = "com.codeborne.selenide.WebDriverRunnerTest$CustomWebDriverProvider";

    try {
      assertSame(driver, WebDriverRunner.getWebDriver());
    } finally {
      WebDriverRunner.closeWebDriver();
      Configuration.browser = System.getProperty("browser", FIREFOX);
    }
  }

  @Test
  public void cleansWebDriverExceptionMessage() {
    String webdriverException = "org.openqa.selenium.NoSuchElementException: The element could not be found (WARNING: The server did not provide any stacktrace information)\n" +
        "Command duration or timeout: 21 milliseconds\n" +
        "For documentation on this error, please visit: http://seleniumhq.org/exceptions/no_such_element.html\n" +
        "Build info: version: '2.29.1', revision: 'dfb1306b85be4934d23c123122e06e602a15e446', time: '2013-01-22 12:58:05'\n" +
        "System info: os.name: 'Linux', os.arch: 'amd64', os.version: '3.5.0-23-generic', java.version: '1.7.0_10'\n" +
        "Session ID: 610138404f5c180a4f3153785e66c528\n" +
        "Driver info: org.openqa.selenium.chrome.ChromeDriver\n" +
        "Capabilities [{platform=LINUX, chrome.chromedriverVersion=26.0.1383.0, acceptSslCerts=false, javascriptEnabled=true, browserName=chrome, rotatable=false, locationContextEnabled=false, version=24.0.1312.56, cssSelectorsEnabled=true, databaseEnabled=false, handlesAlerts=true, browserConnectionEnabled=false, webStorageEnabled=true, nativeEvents=true, applicationCacheEnabled=false, takesScreenshot=true}]";
    String expectedException = "NoSuchElementException: The element could not be found";
    assertEquals(expectedException, cleanupWebDriverExceptionMessage(webdriverException));
  }

  public static class CustomWebDriverProvider implements WebDriverProvider {
    @Override
    public WebDriver createDriver() {
      return driver;
    }
  }
}
