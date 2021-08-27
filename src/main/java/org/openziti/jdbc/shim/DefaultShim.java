package org.openziti.jdbc.shim;

import java.sql.Driver;
import java.util.EnumSet;
import java.util.Objects;
import java.util.regex.Pattern;
import org.openziti.jdbc.Driver.ZitiFeature;
import org.openziti.jdbc.DriverShim;

public class DefaultShim implements DriverShim {

  protected final Driver delegate;
  protected final Pattern urlPattern;
  protected final String driverClass;
  protected final EnumSet<ZitiFeature> zitiFeatures;

  public DefaultShim(String urlPattern, String driverClassName, EnumSet<ZitiFeature> zitiFeatures) throws ReflectiveOperationException {
    this.urlPattern = Pattern.compile(urlPattern);
    this.driverClass = driverClassName;
    this.zitiFeatures = zitiFeatures;
    delegate = (Driver) Class.forName(driverClassName, false, getClass().getClassLoader()).getDeclaredConstructor().newInstance();

  }

  @Override
  public boolean acceptsURL(String url) {
    System.out.println("Checking to see if " + getClass().getName() + " accepts " + url);
    boolean result = urlPattern.matcher(url).matches(); 
    System.out.println("It " + ((result)?"does":"does not"));
    return result;
  }

  @Override
  public Driver getDelegate() {
    return delegate;
  }

  @Override
  public EnumSet<ZitiFeature> getZitiFeatures() {
    return zitiFeatures;
  }

  @Override
  public int hashCode() {
    return Objects.hash(urlPattern.pattern());
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    DefaultShim other = (DefaultShim) obj;
    return Objects.equals(urlPattern.pattern(), other.urlPattern.pattern());
  }
}
