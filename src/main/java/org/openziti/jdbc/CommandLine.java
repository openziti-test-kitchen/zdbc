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

package org.openziti.jdbc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.zip.GZIPOutputStream;

public class CommandLine {

  private static void printUsageAndExit() {
    System.out.println("Usage:  java -jar zdbc.jar -e /path/to/identity.json");
    System.out.println("\t -e  Encode a Ziti identity file for use as zitiIdentity jdbc driver property");
    System.out.println("\t        The file is gzipped and base64 encoded");
    System.exit(1);
  }
  
  private static String encodeIdentity(File input) throws IOException {
    
    FileInputStream fis = new FileInputStream(input);
    byte[] buffer = new byte[1024];
    
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    GZIPOutputStream gzip = new GZIPOutputStream(out);
    
    int len = 0;
    while( -1 != (len = fis.read(buffer))) {
      gzip.write(buffer, 0, len);
    }
    gzip.close();

    String encodedIdentity = Base64.getEncoder().encodeToString(out.toByteArray());
    return encodedIdentity;
  }
  
  public static void main(String[] args) {
    if (args.length != 2) {
      printUsageAndExit();      
    }
    if (!"-e".equals(args[0])) {
      printUsageAndExit(); 
    }
    
    File identity = new File(args[1]);
    if( !identity.exists() || !identity.canRead()) {
      System.out.println("identity file " + identity.getAbsolutePath() + " does not exist or could not be read");
      printUsageAndExit();
    }
    
    try { 
      System.out.println(encodeIdentity(identity));
    } catch (IOException e) {
      System.err.println("Failed to encode " + identity.getAbsolutePath() + "," + e.getMessage());
    }
  }
}
