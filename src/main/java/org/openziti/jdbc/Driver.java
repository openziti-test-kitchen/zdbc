
package org.openziti.jdbc;

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

public class Driver implements java.sql.Driver {
  private static Driver registeredDriver;
  private static Set<String> zitiConfigs = new HashSet<>();

  public static final String ZITI_JSON = "zitiIdentityFile";
  public static final String ZITI_KEYSTORE = "zitiKeystore";
  public static final String ZITI_KEYSTORE_PASSWORD = "zitiKeystorePassword";
  public static final String ZITI_DRIVER_URL_PATTERN = "zitiDriverUrlPattern";
  public static final String ZITI_DRIVER_CLASSNAME = "zitiDriverClassname";
  public static final String ZITI_DRIVER_FEATURES = "zitiDriverFeatures";

  private static ShimManager mgr = new ShimManager();

  public static enum ZitiFeature {
    seamless, nameService, nioProvider
  }

  static {
    System.out.println("Registering Ziti Driver");
    try {
      register();
    } catch (SQLException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  @Override
  public Connection connect(String url, Properties info) throws SQLException {
    System.out.println("Connecting to " + url);

    // Get out if it's not the right prefix
    if (null == url || !url.startsWith("zdbc")) {
      return null;
    }

    DriverShim shim = mgr.getShim(url).orElseGet(() -> registerShim(info));
    
    parseUrl(url, info);
    shim.configureDriverProperties(info);

    // OK - we have a zdbc request.
    String dbUrl = url.replaceFirst("zdbc", "jdbc");

    setupZiti(shim, info);
    return shim.getDelegate().connect(dbUrl, info);
  }



  @Override
  public boolean acceptsURL(String url) throws SQLException {
    return null != url && url.startsWith("zdbc");
  }

  @Override
  public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
    DriverPropertyInfo i = new DriverPropertyInfo(ZITI_JSON, "Ziti identity file");
    i.description = "Ziti identify file to use for the connection";
    i.required = false;

    DriverPropertyInfo k = new DriverPropertyInfo(ZITI_KEYSTORE, "Keystore containing one or more Ziti identities");
    k.description = "Keystore containing one or more Ziti identities";
    k.required = false;

    DriverPropertyInfo p = new DriverPropertyInfo(ZITI_KEYSTORE_PASSWORD, "Ziti keystore password");
    p.description = "Password for the Ziti keystore";
    p.required = false;


    String dbUrl = url.replaceFirst("zdbc", "jdbc");

    DriverShim shim = mgr.getShim(url).orElse(registerShim(info));

    DriverPropertyInfo[] props = shim.getDelegate().getPropertyInfo(dbUrl, info);
    for (int ii = 0; ii < props.length; ii++) {
      System.out.println("Found driver property " + props[ii].name);
    }
    DriverPropertyInfo[] result = new DriverPropertyInfo[props.length + 3];
    result[0] = i;
    result[1] = k;
    result[2] = p;
    for (int ii = 0; ii < props.length; ii++) {
      System.out.println("Found driver property " + props[ii].name);
      result[ii + 3] = props[ii];
    }

    return result;
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
    return false;
  }

  @Override
  public Logger getParentLogger() throws SQLFeatureNotSupportedException {
    // TODO Auto-generated method stub
    return null;
  }

  public static void register() throws SQLException {
    if (isRegistered()) {
      throw new IllegalStateException(
          "Driver is already registered. It can only be registered once.");
    }
    Driver registeredDriver = new Driver();
    DriverManager.registerDriver(registeredDriver);
    Driver.registeredDriver = registeredDriver;
  }


  /**
   * @return {@code true} if the driver is registered against {@link DriverManager}
   */
  public static boolean isRegistered() {
    return registeredDriver != null;
  }

  private DriverShim registerShim(Properties info) throws ShimException {
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
      return mgr.registerShim(
          info.getProperty(ZITI_DRIVER_URL_PATTERN),
          info.getProperty(ZITI_DRIVER_CLASSNAME),
          features);
    } catch (ReflectiveOperationException e) {
      throw new ShimException("Could not create custom Ziti jdbc driver shim", e);
    }
  }

  private void setupZiti(DriverShim shim, Properties info) {


    if (info.containsKey(ZITI_JSON) || info.containsKey(ZITI_KEYSTORE)) {
      System.out.println("JDBC DRIVER IS CONFIGURING ZITI. Production applications should configure Ziti directly");
    }

    System.out.println("Loaded configs: ");
    zitiConfigs.forEach(c -> System.out.println("\t" + c));

    if (zitiConfigs.contains(info.getProperty(ZITI_JSON)) || zitiConfigs.contains(info.getProperty(ZITI_KEYSTORE))) {
      System.out.println("Ziti config has already been loaded.  Skipping");
      return;
    }


    boolean zitiNeedsInit = Ziti.getContexts().isEmpty();


    if (zitiNeedsInit) {
      // System.setProperty("java.nio.channels.spi.SelectorProvider",
      // "org.openziti.net.nio.ZitiSelectorProvider");
    }

    // TODO: General error handling
    if (info.containsKey(ZITI_JSON)) {
      System.out.println("Loading Ziti from JSON identity file " + info.getProperty(ZITI_JSON) + " initalizing: " + zitiNeedsInit);
      if (zitiNeedsInit) {
        System.out.println("Loaded Contexts: ");
        Ziti.getContexts().forEach(c -> System.out.println("\t" + c));

        Ziti.init(info.getProperty(ZITI_JSON), "".toCharArray(), true);
        System.out.println("Loaded Contexts: ");
        Ziti.getContexts().forEach(c -> System.out.println("\t" + c));
      } else {
        Ziti.newContext(info.getProperty(ZITI_JSON), "".toCharArray());
      }
      zitiConfigs.add(info.getProperty(ZITI_JSON));
    } else if (info.containsKey(ZITI_KEYSTORE)) {

      System.out.println("Loading Ziti from keystore " + info.getProperty(ZITI_KEYSTORE));
      if (zitiNeedsInit) {
        Ziti.init(info.getProperty(ZITI_KEYSTORE), info.getProperty(ZITI_KEYSTORE_PASSWORD).toCharArray(), true);
      } else {
        Ziti.newContext(info.getProperty(ZITI_KEYSTORE), info.getProperty(ZITI_KEYSTORE_PASSWORD).toCharArray());
      }
      System.out.println("Ziti initialized");
      zitiConfigs.add(info.getProperty(ZITI_KEYSTORE));
    }
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
