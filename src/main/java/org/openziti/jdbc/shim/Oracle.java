package org.openziti.jdbc.shim;

import java.util.EnumSet;
import org.openziti.jdbc.Driver.ZitiFeature;

public class Oracle extends DefaultShim {

  public Oracle() throws ReflectiveOperationException {
    super("^zdbc:oracle:thin.*", 
        "oracle.jdbc.OracleDriver", 
        EnumSet.of(ZitiFeature.nioProvider, ZitiFeature.nameService));
  }
}
