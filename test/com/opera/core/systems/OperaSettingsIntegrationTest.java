package com.opera.core.systems;

import com.google.common.io.Files;

import com.opera.core.systems.runner.launcher.OperaLauncherRunner;
import com.opera.core.systems.scope.internal.OperaIntervals;
import com.opera.core.systems.testing.Ignore;
import com.opera.core.systems.testing.NoDriver;
import com.opera.core.systems.testing.OperaDriverTestCase;
import com.opera.core.systems.testing.drivers.TestOperaDriver;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.openqa.selenium.Platform;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.net.NetworkUtils;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import static com.opera.core.systems.OperaProduct.CORE;
import static com.opera.core.systems.scope.internal.OperaIntervals.SERVER_DEFAULT_PORT;
import static com.opera.core.systems.scope.internal.OperaIntervals.SERVER_DEFAULT_PORT_IDENTIFIER;
import static com.opera.core.systems.scope.internal.OperaIntervals.SERVER_RANDOM_PORT_IDENTIFIER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.openqa.selenium.Platform.WINDOWS;

/**
 * @author Andreas Tolf Tolfsen <andreastt@opera.com>
 */
@NoDriver
public class OperaSettingsIntegrationTest extends OperaDriverTestCase {

  public static final NetworkUtils NETWORK_UTILS = new NetworkUtils();
  public final long defaultHandshakeTimeout = OperaIntervals.HANDSHAKE_TIMEOUT.getValue();

  public OperaSettings settings;

  private void assertDriverCreated(OperaSettings settings) {
    TestOperaDriver driver = new TestOperaDriver(settings);
    assertNotNull(driver);
    driver.quit();
  }

  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  @Before
  public void beforeEach() {
    OperaIntervals.HANDSHAKE_TIMEOUT.setValue(2000);
    settings = new OperaSettings();
  }

  @After
  public void afterEach() {
    OperaIntervals.HANDSHAKE_TIMEOUT.setValue(defaultHandshakeTimeout);
  }

  @Test
  public void binaryIsCorrectlyLaunched() {
    settings.setBinary(new File(OperaPaths.operaPath()));
    TestOperaDriver driver = new TestOperaDriver(settings);

    assertNotNull(driver);
    assertTrue("Expected Opera to run", driver.isRunning());

    driver.quit();
  }

  @Test(expected = WebDriverException.class)
  public void binaryInvalidThrowsException() {
    settings.setBinary(resources.fakeFile());
    TestOperaDriver driver = new TestOperaDriver(settings);
  }

  @Test(expected = WebDriverException.class)
  public void autostartIsRespected() {
    settings.autostart(false);
    TestOperaDriver driver = new TestOperaDriver(settings);
  }

  @Test
  public void launcherIsUsedWhenSet() throws IOException {
    File newLauncher = tmp.newFile("newLauncher");
    Files.copy(OperaLauncherRunner.launcherDefaultLocation(), newLauncher);
    if (!Platform.getCurrent().is(WINDOWS)) {
      newLauncher.setExecutable(true);
    }

    settings.setLauncher(newLauncher);
    TestOperaDriver driver = new TestOperaDriver(settings);

    assertNotNull(driver);
    assertEquals(newLauncher, driver.getSettings().getLauncher());

    driver.quit();
  }

  @Test
  public void loggingFileReceivesOutput() throws IOException {
    File log = tmp.newFile("operadriver.log");

    settings.logging().setFile(log);
    settings.logging().setLevel(Level.FINER);

    TestOperaDriver driver = new TestOperaDriver(settings);
    driver.quit();

    assertNotSame(0, log.length());
    assertTrue(log.length() > 0);
  }

  @Test
  @Ignore(products = CORE, value = "core does not support -pd")
  public void profileIsRespected() throws IOException {
    File profile = tmp.newFolder();
    settings.setProfile(profile.getPath());
    TestOperaDriver driver = new TestOperaDriver(settings);

    assertNotNull(driver);
    assertEquals(profile, driver.preferences().get("User Prefs", "Opera Directory").getValue());
    assertEquals(profile, driver.getSettings().profile().getDirectory());

    driver.quit();
  }

  @Test
  public void profileCanBeSetUsingString() {
    File profileDirectory = tmp.newFolder("profile");
    settings.setProfile(profileDirectory.getPath());
    assertEquals(profileDirectory.getPath(), settings.profile().getDirectory().getPath());
  }

  @Test
  @Ignore("CORE-44852: Unable to automatically connect debugger to non-loopback address")
  public void hostIsRespectedOnLaunch() {
    String host = NETWORK_UTILS.getIp4NonLoopbackAddressOfThisMachine().getHostAddress();
    settings.setHost(host);
    TestOperaDriver driver = new TestOperaDriver(settings);

    assertNotNull(driver);
    assertEquals(host, driver.getSettings().getHost());
    assertEquals(host, driver.preferences().get("Developer Tools", "Proxy Host").getValue());

    driver.quit();
  }

  @Test
  public void portCanBeSet() {
    settings.setPort(1234);
    TestOperaDriver driver = new TestOperaDriver(settings);
    assertNotNull(driver);
    assertEquals(1234, driver.getSettings().getPort());
    driver.quit();
  }

  @Test
  public void portSetToRandomIdentifier() {
    settings.setPort((int) SERVER_RANDOM_PORT_IDENTIFIER.getValue());
    assertNotSame((int) SERVER_DEFAULT_PORT.getValue(), settings.getPort());
    assertDriverCreated(settings);
  }

  @Test
  public void portSetToDefaultIdentifier() {
    settings.setPort((int) SERVER_DEFAULT_PORT_IDENTIFIER.getValue());
    assertEquals((int) SERVER_DEFAULT_PORT.getValue(), settings.getPort());
    assertDriverCreated(settings);
  }

  @Test
  @Ignore(products = CORE, value = "core does not reset port number if -debugproxy is omitted")
  public void testSettingPort() {
    settings.setPort(-1);
    TestOperaDriver driver = new TestOperaDriver(settings);
    assertNotNull(driver);
    driver.quit();
  }

}