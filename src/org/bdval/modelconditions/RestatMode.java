package org.bdval.modelconditions;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;

/**
 * @author Fabien Campagne
 *         Date: Oct 6, 2009
 *         Time: 5:34:23 PM
 */
public class RestatMode extends ProcessModelConditionsMode {
    public void defineOptions(final JSAP jsap) throws JSAPException {
    }

    @Override
    public void interpretArguments(JSAP jsap, JSAPResult jsapResult, ProcessModelConditionsOptions options) {
       super.interpretArguments(jsap, jsapResult, options); 
    }

    @Override
    public void processOneModelId(ProcessModelConditionsOptions options, String modelId) {
        System.out.println("will process model id:" + modelId); 
    }
}
