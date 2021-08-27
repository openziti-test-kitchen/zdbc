package org.openziti.jdbc.shim;

import java.util.EnumSet;
import org.openziti.jdbc.Driver.ZitiFeature;

public class Mysql extends DefaultShim {

  public Mysql() throws ReflectiveOperationException {
    super("^zdbc:mysql.*", "com.mysql.cj.jdbc.Driver", EnumSet.of(ZitiFeature.seamless));
  }
}
