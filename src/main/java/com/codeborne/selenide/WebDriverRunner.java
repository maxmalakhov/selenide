package com.codeborne.selenide;

import org.apache.commons.io.IOUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.internal.Killable;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import static com.codeborne.selenide.Configuration.*;
import static org.apache.commons.io.FileUtils.copyFile;

public class WebDriverRunner {
  public static final String CHROME = "chrome";
  public static final String INTERNET_EXPLORER = "ie";
  public static final String FIREFOX = "firefox";

  /**
   * To use OperaDriver, you need to include extra dependency to your project:
   * <dependency org="org.seleniumhq.selenium" name="selenium-htmlunit-driver" rev="2.31.0" conf="test->default"/>
   */
  public static final String HTMLUNIT = "htmlunit";

  /**
   * To use OperaDriver, you need to include extra dependency to your project:
   * <dependency org="com.github.detro.ghostdriver" name="phantomjsdriver" rev="1.+" conf="test->default"/>
   */
  public static final String PHANTOMJS = "phantomjs";

  /**
   * To use OperaDriver, you need to include extra dependency to your project:
   * <dependency org="com.opera" name="operadriver" rev="0.18" conf="test->default"/>
   */
  public static final String OPERA = "opera";

  private static WebDriver webdriver;

  static {
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        closeWebDriver();
      }
    });
  }

  public static WebDriver getWebDriver() {
    if (webdriver == null) {
      webdriver = createDriver();
    }
    return webdriver;
  }

  public static void closeWebDriver() {
    if (webdriver != null) {
      if (!holdBrowserOpen) {
        try {
          webdriver.close();
        } catch (WebDriverException cannotCloseBrowser) {
          System.err.println("Cannot close browser normally (let's kill it): " + cannotCloseBrowser.toString());
        }
        finally {
          killBrowser();
        }
      }
      webdriver = null;
    }
  }

  static void killBrowser() {
    if (webdriver instanceof Killable) {
      try {
        ((Killable) webdriver).kill();
      } catch (Exception e) {
        System.err.println("Failed to kill browser " + webdriver + ':');
        e.printStackTrace();
      }
    }
  }

  public static boolean ie() {
    return INTERNET_EXPLORER.equalsIgnoreCase(browser);
  }

  public static boolean htmlUnit() {
    return HTMLUNIT.equalsIgnoreCase(browser);
  }

  public static boolean phantomjs() {
    return PHANTOMJS.equalsIgnoreCase(browser);
  }

  public static void clearBrowserCache() {
    if (webdriver != null) {
      webdriver.manage().deleteAllCookies();
    }
  }

  public static String source() {
    return getWebDriver().getPageSource();
  }

  public static String url() {
    return getWebDriver().getCurrentUrl();
  }

  public static String takeScreenShot(String fileName) {
    if (webdriver == null) {
      return null;
    }

    File targetFile = new File(reportsFolder, fileName + ".html");

    try {
      writeToFile(webdriver.getPageSource(), targetFile);
    } catch (Exception e) {
      System.err.println(e);
    }

    if (webdriver instanceof TakesScreenshot) {
      try {
        File scrFile = ((TakesScreenshot) webdriver).getScreenshotAs(OutputType.FILE);
        targetFile = new File(reportsFolder, fileName + ".png");
        copyFile(scrFile, targetFile);
      } catch (Exception e) {
        System.err.println(e);
      }
    }

    return targetFile.getAbsolutePath();
  }

  private static void writeToFile(String content, File targetFile) {
    File reportsFolder = targetFile.getParentFile();
    if (!reportsFolder.exists()) {
      System.err.println("Creating folder for test reports: " + reportsFolder);
      if (!reportsFolder.mkdirs()) {
        System.err.println("Failed to create " + reportsFolder);
      }
    }

    try {
      FileWriter output = new FileWriter(targetFile);
      try {
        IOUtils.write(content, output);
      } finally {
        output.close();
      }
    } catch (IOException e) {
      System.err.println("Failed to write page source to file " + targetFile + ": " + e);
    }
  }

  private static WebDriver createDriver() {
    if (remote != null) {
      return createRemoteDriver(remote, browser);
    } else if (CHROME.equalsIgnoreCase(browser)) {
      ChromeOptions options = new ChromeOptions();
      if (startMaximized) {
        // Due do bug in ChromeDriver we need this workaround
        // http://stackoverflow.com/questions/3189430/how-do-i-maximize-the-browser-window-using-webdriver-selenium-2
        options.addArguments("chrome.switches", "--start-maximized");
      }
      return new ChromeDriver(options);
    } else if (ie()) {
      DesiredCapabilities ieCapabilities = DesiredCapabilities.internetExplorer();
      ieCapabilities.setCapability(InternetExplorerDriver.INTRODUCE_FLAKINESS_BY_IGNORING_SECURITY_DOMAINS, true);
      return maximize(new InternetExplorerDriver(ieCapabilities));
    } else if (htmlUnit()) {
      DesiredCapabilities desiredCapabilities = DesiredCapabilities.htmlUnit();
      desiredCapabilities.setCapability(HtmlUnitDriver.INVALIDSELECTIONERROR, true);
      desiredCapabilities.setCapability(HtmlUnitDriver.INVALIDXPATHERROR, false);
      desiredCapabilities.setJavascriptEnabled(true);
      return new HtmlUnitDriver(desiredCapabilities);
    } else if (FIREFOX.equalsIgnoreCase(browser)) {
      return maximize(new FirefoxDriver());
    } else if (OPERA.equalsIgnoreCase(browser)) {
      return createInstanceOf("com.opera.core.systems.OperaDriver");
    } else if (PHANTOMJS.equals(browser)) {
      DesiredCapabilities capabilities = new DesiredCapabilities();
      capabilities.setJavascriptEnabled(true);
      capabilities.setCapability("takesScreenshot", true);
      return new org.openqa.selenium.phantomjs.PhantomJSDriver(capabilities);
    } else {
      return createInstanceOf(browser);
    }
  }

  private static RemoteWebDriver maximize(RemoteWebDriver driver) {
    if (startMaximized) {
      driver.manage().window().maximize();
    }
    return driver;
  }

  private static WebDriver createInstanceOf(String className) {
    try {
      Class<?> clazz = Class.forName(className);
      if (WebDriverProvider.class.isAssignableFrom(clazz)) {
        return ((WebDriverProvider)clazz.newInstance()).createDriver();
      } else {
        return (WebDriver) Class.forName(className).newInstance();
      }
    }
    catch (Exception invalidClassName) {
      throw new IllegalArgumentException(invalidClassName);
    }
  }

  private static WebDriver createRemoteDriver(String remote, String browser) {
    try {
      DesiredCapabilities capabilities = new DesiredCapabilities();
      capabilities.setBrowserName(browser);
      return new RemoteWebDriver(new URL(remote), capabilities);
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException("Invalid 'remote' parameter: " + remote, e);
    }
  }

  public static <T> T fail(String message) {
    throw new AssertionError(message);
  }

  public static String cleanupWebDriverExceptionMessage(WebDriverException webDriverException) {
    return cleanupWebDriverExceptionMessage(webDriverException.toString());
  }

  static String cleanupWebDriverExceptionMessage(String webDriverExceptionInfo) {
    return webDriverExceptionInfo == null || webDriverExceptionInfo.indexOf('\n') == -1 ?
        webDriverExceptionInfo :
        webDriverExceptionInfo
            .substring(0, webDriverExceptionInfo.indexOf('\n'))
            .replaceFirst("(.*)\\(WARNING: The server did not provide any stacktrace.*", "$1")
            .replaceFirst("org\\.openqa\\.selenium\\.(.*)", "$1")
            .trim();
  }
}
