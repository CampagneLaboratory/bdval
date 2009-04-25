/**
Generate whole-chip pathway info and gene2probe files for a set of endpoints. The
whole-chip approach puts all the probesets into the same 'pathway', so that feature
aggregation such as PCA is applied to the entire chip.
*/
myFileDirectory="/home/maqcii/dev-maqcii/"
normDataDirectory=myFileDirectory +"norm-data/"
pathwayPcaDirectory=myFileDirectory +"pathways/pca-whole-chip/"

generateGene2Probes = {   endpointName ->

  tmmFile = new File(normDataDirectory + endpointName+".tmm")
  gene2probeFile =new File(pathwayPcaDirectory+endpointName+"-gene2probes-whole-chip.txt")
  pathwayInfoFile =new File(pathwayPcaDirectory+endpointName+"-whole-chip-pathway.txt")
  gene2probeWriter= new PrintWriter(gene2probeFile)
  pathwayInfoWriter= new PrintWriter(pathwayInfoFile)
  readProbeId = { it ->
    tokens= it.split()
    probeId=tokens[0]

    if("ID_REF"!=probeId)   probeIds.add(probeId);

  }
  probeIds=new HashSet<String>()

  tmmFile.eachLine( readProbeId )

  probeIds.each( { probeId -> gene2probeWriter.write(probeId+"\t"+probeId+"\n" )})

  pathwayInfoWriter.write("whole-chip\t")
  probeIds.each( { probeId -> pathwayInfoWriter .write(probeId+"\t" )})
  pathwayInfoWriter.write("\n")

  pathwayInfoWriter.close();
  gene2probeWriter.close();
}


endpoints=["HamnerWithControl", "Iconix", "NIEHS", "MDACC_PCR","MDACC_ERPOS",
"Cologne_OS_MO","Cologne_EFS_MO", "UAMS_EFS_MO", "UAMS_OS_MO", "UAMS_CPR1",
 "UAMS_CPS1", "Cologne_NEP_R", "Cologne_NEP_S"]

endpoints.each(generateGene2Probes)


