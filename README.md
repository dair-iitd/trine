# TRINE
This repository hosts the code for "Inferring Temporal Knowledge for Near-Periodic Recurrent Events, IJCAI 2018"

## Usage

The paper has three components: schedule extractor, instance extractor and a temporal inference engine (PGM). The extractors are stand-alone, while the PGM module requires the output of the extractors.

### Schedule Extractor

Refer to the comments in `extractor.py`. 

### Instance Extractor

The following programs have to be run in order:
1. Run `edu.iitd.nlp.ee.freebase.FreebaseEventExtractor` to get a list of events from FreeBase
2. Run the programs in the `edu.iitd.nlp.ee.corpus` package to create an index of paragraphs that contain recurrent events. Choose the program based on the corpus (ClueWeb or NYT Corpus).
3. Run `edu.iitd.nlp.ee.core.InstanceCandidateOccurrenceDatePairExtractor` to extract instance, candidate-occurrence-date pairs
4. To train a occurrence date classifier use `edu.iitd.nlp.ee.classify.LingpipeClassifier`

### PGM

The instructions are compiled as a Jupyter Notebook (`TRINE_tutorial.ipynb`)

## References
	Inferring Temporal Knowledge for Near-Periodic Recurrent Events. Dinesh Raghu, Surag Nair, Mausam. 2018. IJCAI.

