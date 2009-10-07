package org.bdval.modelconditions;

import java.io.File;
import java.io.PrintWriter;
import java.util.Map;

/**
 * @author Fabien Campagne
 *         Date: Oct 6, 2009
 *         Time: 4:07:13 PM
 */
public class ProcessModelConditionsOptions {
    public String[] resultDirectories;
    public String modelConditionsFilename;
    public String[] modelConditionLines;
    public Map<String, Map<String, String>> modelConditions;
    public PrintWriter output;
}
