package org.openziti.jdbc;

import java.util.EnumSet;
import java.util.Properties;
import org.openziti.jdbc.Driver.ZitiFeature;

public interface DriverShim {
  boolean acceptsURL(String url);
  java.sql.Driver getDelegate();
  EnumSet<ZitiFeature> getZitiFeatures();
  
  default void configureDriverProperties(Properties props) { /*NOOP */ }
}
