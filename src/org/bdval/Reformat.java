/*
 * Copyright (C) 2008-2009 Institute for Computational Biomedicine,
 *                         Weill Medical College of Cornell University
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package edu.cornell.med.icb.biomarkers;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import edu.cornell.med.icb.geo.tools.ClassificationTask;
import edu.cornell.med.icb.geo.tools.GeneList;
import edu.cornell.med.icb.geo.tools.MicroarrayTrainEvaluate;
import edu.mssm.crover.tables.InvalidColumnException;
import edu.mssm.crover.tables.Table;
import edu.mssm.crover.tables.TypeMismatchException;
import edu.mssm.crover.tables.writers.InsightfulMinerTableWriter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Reformats the data into a form suitable for data mining. Outputs the data with labels
 * in a tab delimited format.
 *
 * @author Fabien Campagne
 * Date: Mar 31, 2008
 * Time: 2:41:22 PM
 */
public class Reformat extends DAVMode {
    /**
     * Used to log debug and informational messages.
     */
    private static final Log LOG = LogFactory.getLog(Reformat.class);

    /**
     * Define command line options for this mode.
     * @param jsap the JSAP command line parser
     * @throws JSAPException if there is a problem building the options
     */
    @Override
    public void defineOptions(final JSAP jsap) throws JSAPException {
    }

    @Override
    public void process(final DAVOptions options) {
        super.process(options);
        for (final ClassificationTask task : options.classificationTasks) {
            for (final GeneList geneList : options.geneLists) {
                try {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Reformat for " + task);
                    }
                    final Table processedTable =  processTable(geneList, options.inputTable,
                            options, MicroarrayTrainEvaluate.calculateLabelValueGroups(task));

                    addLabelColumn(processedTable,
                            MicroarrayTrainEvaluate.calculateLabelValueGroups(task));
                    InsightfulMinerTableWriter.writeData(processedTable, options.output);
                    options.output.flush();
                } catch (Exception e) {
                    LOG.fatal(e);
                    System.exit(10);
                }
            }
        }
    }

    private void addLabelColumn(final Table processedTable, final List<Set<String>> labelValueGroups) throws InvalidColumnException {
        final HashMap<Set<String>, Integer> groupToCodedLabel = new HashMap<Set<String>, Integer>();
        if (labelValueGroups.size() != 2) {
            throw new IllegalArgumentException("Classification requires exactly two label groups.");
        }
        final Iterator<Set<String>> it = labelValueGroups.iterator();
        final Set<String> labelGroup0 = it.next();  // negative class
        final Set<String> labelGroup1 = it.next();  // positive class

        groupToCodedLabel.put(labelGroup0, -1);
        groupToCodedLabel.put(labelGroup1, 1);
        final int labelColumnIndex = processedTable.addColumn("label", double.class);

        final String[] idRefs = processedTable.getStrings("ID_REF");

        for (final String id : idRefs) {
            try {
                processedTable.appendDoubleValue(labelColumnIndex, recodeLabel(id, labelValueGroups, groupToCodedLabel));
            } catch (TypeMismatchException e) {
                LOG.warn(e);
            } catch (InvalidColumnException e) {
                LOG.warn(e);
            }
        }
    }

    protected int recodeLabel(final Object value, final List<Set<String>> labelValueGroups,
                              final HashMap<Set<String>, Integer> groupToCodedLabel)
            throws InvalidColumnException {
        if (value instanceof String) {
            final String labelValue = (String) value;
            for (final Set<String> labelGroup : labelValueGroups) {
                if (labelGroup.contains(labelValue)) {
                    return groupToCodedLabel.get(labelGroup);
                }
            }
            throw new IllegalArgumentException("Label value " + labelValue
                    + " must match a label group.");
        } else {
            throw new InvalidColumnException("Label must be encoded with a String type.");
        }
    }
}
