package org.openziti.jdbc.shim;

import static org.openziti.jdbc.ZitiDriver.ZitiFeature.nioProvider;
import java.util.EnumSet;
import org.openziti.jdbc.BaseZitiDriverShim;

public class Oracle extends BaseZitiDriverShim {

  public Oracle() throws ReflectiveOperationException {
    super("^zdbc:oracle:thin.*", 
        "oracle.jdbc.OracleDriver", 
        EnumSet.of(nioProvider));
  }
}
