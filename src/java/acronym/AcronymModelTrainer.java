package acronym;

import model.Document;

/**
 * Interface for a trainer of an acronym model
 *
 * Only needs to be able to add in new documents and return a model (probably for serialization)
 * Some model types do a lot of the heavy lifting during the getModel() phase, after all docs have been added
 *
 * Created by ANONYMOUS on 10/30/15.
 */
public interface AcronymModelTrainer {

    AcronymModel getModel();

    void addDocumentToModel(Document document);

}
