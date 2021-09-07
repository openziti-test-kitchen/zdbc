package org.openziti.jdbc.shim;

import java.util.EnumSet;
import org.openziti.jdbc.BaseZitiDriverShim;
import org.openziti.jdbc.ZitiDriver.ZitiFeature;

public class Oracle extends BaseZitiDriverShim {

  public Oracle() throws ReflectiveOperationException {
    super("^zdbc:oracle:thin.*", 
        "oracle.jdbc.OracleDriver", 
        EnumSet.of(ZitiFeature.nioProvider));
  }
}
