/*
 * Copyright (C) 2008-2009 Institute for Computational Biomedicine,
 *                         Weill Medical College of Cornell University
 *                         All rights reserved.
 *
 * WEILL MEDICAL COLLEGE OF CORNELL UNIVERSITY MAKES NO REPRESENTATIONS
 * ABOUT THE SUITABILITY OF THIS SOFTWARE FOR ANY PURPOSE. IT IS PROVIDED
 * "AS IS" WITHOUT EXPRESS OR IMPLIED WARRANTY. THE WEILL MEDICAL COLLEGE
 * OF CORNELL UNIVERSITY SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY
 * THE USERS OF THIS SOFTWARE.
 */

package org.bdval.modelselection;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/**
 * @author Fabien Campagne
 *         Date: Sep 17, 2008
 *         Time: 3:49:48 PM
 */
public class CustomRanking {
    String datasetName;
    String endpointCode;
    /* Describes the strategy followed to create this ranking. */
    String typeOfRanking;
    /* In the order rank 1, rank 2, etc. where rank 1 is expected to have higher performance.*/
    ObjectArrayList<String> modelIds;
}
