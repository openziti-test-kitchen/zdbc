
package org.openziti.jdbc;

import static org.openziti.jdbc.ZitiDriver.ZitiFeature.nioProvider;
import static org.openziti.jdbc.ZitiDriver.ZitiFeature.seamless;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.openziti.Ziti;

public class ZitiDriver implements java.sql.Driver {
  private static final Logger log = Logger.getLogger(ZitiDriver.class.getName());

  private static ZitiDriver registeredDriver;
  private static Set<String> zitiConfigs = new HashSet<>();
  private static Set<BaseZitiDriverShim> configuredShims = new HashSet<>();

  public static final String ZITI_JSON = "zitiIdentityFile";
  public static final String ZITI_KEYSTORE = "zitiKeystore";
  public static final String ZITI_KEYSTORE_PASSWORD = "zitiKeystorePassword";
  public static final String ZITI_DRIVER_URL_PATTERN = "zitiDriverUrlPattern";
  public static final String ZITI_DRIVER_CLASSNAME = "zitiDriverClassname";
  public static final String ZITI_DRIVER_FEATURES = "zitiDriverFeatures";

  public static enum ZitiFeature {
    seamless, nioProvider
  }

  static {
    try {
      register();
    } catch (SQLException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  @Override
  public Connection connect(String url, Properties info) throws SQLException {
    if (!acceptsURL(url)) {
      return null;
    }

    log.fine("Ziti driver is attempting to connect to " + url);

    // Throws a SQLException if a shim could not be found or configured
    BaseZitiDriverShim shim = ZitiShimManager.getShim(url).orElseGet(() -> registerShim(info));

    parseUrl(url, info);
    shim.configureDriverProperties(info);

    setupZiti(shim, info);

    // Replace zdbc with jdbc so shim drivers know how to connect
    String dbUrl = url.replaceFirst("zdbc", "jdbc");

    return shim.getDelegate().connect(dbUrl, info);
  }

  @Override
  public boolean acceptsURL(String url) throws SQLException {
    return null != url && url.startsWith("zdbc");
  }

  @Override
  public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {

    DriverPropertyInfo identityFile = new DriverPropertyInfo(ZITI_JSON, "/path/to/identity.json");
    identityFile.description = "Ziti identify file to use for the connection";
    identityFile.required = false;

    DriverPropertyInfo keystoreFile = new DriverPropertyInfo(ZITI_KEYSTORE, "/path/to/keystore");
    keystoreFile.description = "Keystore containing one or more Ziti identities";
    keystoreFile.required = false;

    DriverPropertyInfo keystorePassword = new DriverPropertyInfo(ZITI_KEYSTORE_PASSWORD, "secretPassword");
    keystorePassword.description = "Password for the Ziti keystore";
    keystorePassword.required = false;

    try {
      // Throws a SQL Exception if a shim could not be found or created
      BaseZitiDriverShim shim = ZitiShimManager.getShim(url).orElseGet(() -> registerShim(info));

      String dbUrl = url.replaceFirst("zdbc", "jdbc");

      DriverPropertyInfo[] props = shim.getDelegate().getPropertyInfo(dbUrl, info);

      DriverPropertyInfo[] result = new DriverPropertyInfo[props.length + 3];
      result[0] = identityFile;
      result[1] = keystoreFile;
      result[2] = keystorePassword;
      System.arraycopy(props, 0, result, 3, props.length);

      return result;
    } catch (ShimException e) {
      // No shim could be found, and no shim could be registered. Return the basic properties to define a
      // shim

      log.fine("Ziti driver could not find a shim for url: " + url + ".  Returning basic properties.");
      
      DriverPropertyInfo driverUrlPattern = new DriverPropertyInfo(ZITI_DRIVER_URL_PATTERN, "^zdbc:<database>.*");
      identityFile.description = "Regular expression used to identify the jdbc URL for this driver";
      identityFile.required = true;

      DriverPropertyInfo driverClassName = new DriverPropertyInfo(ZITI_DRIVER_CLASSNAME, "org.jdbc.Driver");
      keystoreFile.description = "The JDBC driver to delegate to";
      keystoreFile.required = true;

      DriverPropertyInfo zitiFeatures = new DriverPropertyInfo(ZITI_DRIVER_FEATURES, "seamless,nioProvider");
      keystorePassword.description = "Ziti driver features required for the JDBC driver";
      keystorePassword.required = true;

      DriverPropertyInfo[] result = new DriverPropertyInfo[6];
      result[0] = identityFile;
      result[1] = keystoreFile;
      result[2] = keystorePassword;
      result[3] = driverUrlPattern;
      result[4] = driverClassName;
      result[5] = zitiFeatures;
      
      return result;
    }
  }

  @Override
  public int getMajorVersion() {
    return 1;
  }

  @Override
  public int getMinorVersion() {
    return 0;
  }

  @Override
  public boolean jdbcCompliant() {
    // The JDBC driver provided by the shim may be JDBC compliant, but this driver cannot know that for
    // sure.
    // It is forced to return false to be compliant with the spec.
    return false;
  }

  @Override
  public Logger getParentLogger() throws SQLFeatureNotSupportedException {
    // TODO Auto-generated method stub
    return null;
  }

  public static void register() throws SQLException {
    log.fine("Registering Ziti JDBC Driver");

    if (isRegistered()) {
      throw new IllegalStateException(
          "Driver is already registered. It can only be registered once.");
    }
    ZitiDriver registeredDriver = new ZitiDriver();
    DriverManager.registerDriver(registeredDriver);
    ZitiDriver.registeredDriver = registeredDriver;
  }


  /**
   * @return {@code true} if the driver is registered against {@link DriverManager}
   */
  public static boolean isRegistered() {
    return registeredDriver != null;
  }

  private BaseZitiDriverShim registerShim(Properties info) throws ShimException {
    if (!info.containsKey(ZITI_DRIVER_URL_PATTERN) || !info.containsKey(ZITI_DRIVER_CLASSNAME)) {
      throw new ShimException("No Ziti driver shim available");
    }

    final EnumSet<ZitiFeature> features;
    if (info.containsKey(ZITI_DRIVER_FEATURES)) {
      features = Arrays
          .stream(info.getProperty(ZITI_DRIVER_FEATURES).split(","))
          .map(s -> ZitiFeature.valueOf(s))
          .collect(Collectors.toCollection(() -> EnumSet.noneOf(ZitiFeature.class)));
    } else {
      features = EnumSet.noneOf(ZitiFeature.class);
    }

    try {
      return ZitiShimManager.registerShim(
          info.getProperty(ZITI_DRIVER_URL_PATTERN),
          info.getProperty(ZITI_DRIVER_CLASSNAME),
          features);
    } catch (ReflectiveOperationException e) {
      throw new ShimException("Could not create custom Ziti jdbc driver shim", e);
    }
  }

  private synchronized void setupZiti(BaseZitiDriverShim shim, Properties info) {

    if (zitiConfigs.contains(info.getProperty(ZITI_JSON)) || zitiConfigs.contains(info.getProperty(ZITI_KEYSTORE))) {
      log.finest("Ziti has already been configured, skipping setup");
      return;
    }

    if (info.containsKey(ZITI_JSON) || info.containsKey(ZITI_KEYSTORE)) {
      log.info("Ziti JDBC wrapper is configuring Ziti identities. Production applications should manage Ziti identities directly");
    }


    // Check to see if NIO feature is required
    if (!configuredShims.contains(shim) && requiresFeature(shim, nioProvider)) {
      log.info("Ziti JDBC wrapper is setting the system property 'java.nio.channels.spi.SelectorProvider' to 'org.openziti.net.nio.ZitiSelectorProvider'");
      System.setProperty("java.nio.channels.spi.SelectorProvider", "org.openziti.net.nio.ZitiSelectorProvider");
    }

    if (info.containsKey(ZITI_JSON)) {
      log.finer(() -> String.format("Found identity file %s in connection properties.", info.getProperty(ZITI_JSON)));

      Ziti.init(info.getProperty(ZITI_JSON), "".toCharArray(), requiresFeature(shim, seamless));
      log.finer(() -> {
        StringBuilder sb = new StringBuilder("Current Ziti contexts: ");
        Ziti.getContexts().forEach(c -> sb.append("\t").append(c).append("\n"));
        return sb.toString();
      });

      zitiConfigs.add(info.getProperty(ZITI_JSON));
    } else if (info.containsKey(ZITI_KEYSTORE)) {

      log.finer(() -> String.format("Found keystore file %s in connection properties.", info.getProperty(ZITI_KEYSTORE)));
      Ziti.init(info.getProperty(ZITI_KEYSTORE), info.getProperty(ZITI_KEYSTORE_PASSWORD).toCharArray(), requiresFeature(shim, seamless));
      zitiConfigs.add(info.getProperty(ZITI_KEYSTORE));
    }
//
//    if(!configuredShims.contains(shim) && requiresFeature(shim, seamless) && !Ziti.isSeamless()) {
//      throw new ShimException(String.format("Ziti JDBC configuration error. JDBC driver %s requires ziti seamless mode, but it is not enabled",
//          shim.getDelegate().getClass().getName()));
//    }
//
    log.fine("Ziti initialized");
    configuredShims.add(shim);
  }

  protected static boolean requiresFeature(BaseZitiDriverShim shim, ZitiFeature feature) {
    return null != shim.getZitiFeatures() && shim.getZitiFeatures().contains(feature);
  }

  protected void parseUrl(String url, Properties info) throws SQLException {
    int qPos = url.indexOf('?');
    if (qPos == -1) {
      // No arguments on the URL
      return;
    }

    String urlArgs = url.substring(qPos + 1);

    // parse the args part of the url
    String[] args = urlArgs.split("&");
    for (String token : args) {
      if (token.isEmpty()) {
        continue;
      }
      int pos = token.indexOf('=');
      if (pos == -1) {
        info.setProperty(token, "");
      } else {
        try {
          info.setProperty(token.substring(0, pos), URLDecoder.decode(token.substring(pos + 1), Charset.defaultCharset().name()));
        } catch (UnsupportedEncodingException e) {
          throw new SQLException(e);
        }
      }
    }
  }

  /** Little wrapper that lets us wrap exceptions found during shim init in lambdas */
  private static class ShimException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    ShimException(String message) {
      super(message);
    }

    ShimException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
