package org.openziti.jdbc;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.openziti.jdbc.Driver.ZitiFeature;
import org.openziti.jdbc.shim.DefaultShim;
import org.openziti.jdbc.shim.Mysql;
import org.openziti.jdbc.shim.Oracle;
import org.openziti.jdbc.shim.Postgresql;

class ShimManager {
  private static Set<DriverShim> shims = new HashSet<>();

  static {
    try {
      shims.add(new Postgresql());
    } catch (ReflectiveOperationException e) {
      System.out.println("Postgres driver not detected, skipping Ziti driver shim");
    }

    try {
      shims.add(new Oracle());
    } catch (ReflectiveOperationException e) {
      System.out.println("Oracle driver not detected, skipping Ziti driver shim");
    }

    try {
      shims.add(new Mysql());
    } catch (ReflectiveOperationException e) {
      System.out.println("Mysql driver not detected, skipping Ziti driver shim");
    }
  }

  protected DriverShim registerShim(String urlPattern, String driverClassName, EnumSet<ZitiFeature> zitiFeatures) throws ReflectiveOperationException {
    DriverShim shim = new DefaultShim(urlPattern, driverClassName, zitiFeatures);
    shims.add(shim);
    return shim;
  }
  
  protected Optional<DriverShim> getShim(String url) {
    String zdbUrl = url.replaceFirst("jdbc", "zdbc");
    Optional<DriverShim> result = shims.stream().filter(s -> s.acceptsURL(url) || s.acceptsURL(zdbUrl)).findFirst();
    System.out.println("Found shim: " + result.isPresent());
    return result;
  }
}
