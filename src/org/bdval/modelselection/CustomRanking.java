/*
 * Copyright (C) 2008-2009 Institute for Computational Biomedicine,
 *                         Weill Medical College of Cornell University
 *                         All rights reserved.
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
