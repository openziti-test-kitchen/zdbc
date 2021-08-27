package org.openziti.jdbc.shim;

import java.util.EnumSet;
import java.util.Properties;
import org.openziti.jdbc.Driver.ZitiFeature;

public class Postgresql extends DefaultShim {
  public Postgresql() throws ReflectiveOperationException {
    super("^zdbc:postgresql.*", "org.postgresql.Driver", EnumSet.of(ZitiFeature.seamless));
  }
  
  @Override
  public void configureDriverProperties(Properties props) {
    props.setProperty("socketFactory", "org.openziti.net.ZitiSocketFactory");
  }
}
