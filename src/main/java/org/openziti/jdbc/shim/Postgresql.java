/*
 * Copyright (c) 2018-2022 NetFoundry Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openziti.jdbc.shim;

import java.util.EnumSet;
import java.util.Properties;
import org.openziti.jdbc.BaseZitiDriverShim;
import org.openziti.jdbc.ZitiDriver.ZitiFeature;

public class Postgresql extends BaseZitiDriverShim {
  public Postgresql() throws ReflectiveOperationException {
    super("^(zdbc|jdbc:ziti):postgresql.*", "org.postgresql.Driver", EnumSet.noneOf(ZitiFeature.class));
  }

  @Override
  public void configureDriverProperties(Properties props) {
    props.setProperty("socketFactory", "org.openziti.net.ZitiSocketFactory");
  }
}
