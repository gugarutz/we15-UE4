package data;

import at.ac.tuwien.big.we.dbpedia.api.DBPediaService;
import at.ac.tuwien.big.we.dbpedia.api.SelectQueryBuilder;
import at.ac.tuwien.big.we.dbpedia.vocabulary.DBPedia;
import at.ac.tuwien.big.we.dbpedia.vocabulary.DBPediaOWL;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.sparql.vocabulary.FOAF;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import models.Answer;
import models.Category;
import models.JeopardyDAO;
import models.Question;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * Created by Raimund on 31.05.2015.
 */
public class DBpediaDataInserter {
    public static void fetchQuestions() {
        try {
            Category category = new Category();
            category.setNameDE("Geologie");
            category.setNameEN("Geology");

            category.addQuestion(createDBpediaQuestion("Was ist die Hauptstadt von Ungarn?", "What is the capital of Hungary?", 10, "Hungary"));
            category.addQuestion(createDBpediaQuestion("Was ist die Hauptstadt von Paraguay?", "What is the capital of Paraguay?", 10, "Paraguay"));
            category.addQuestion(createDBpediaQuestion("Was ist die Hauptstadt von Griechenland?", "What is the capital of Greece?", 10, "Greece"));
            category.addQuestion(createDBpediaQuestion("Was ist die Hauptstadt von Libanon?", "What is the capital of Libanon?", 10, "Lebanon"));
            category.addQuestion(createDBpediaQuestion("Was ist die Hauptstadt von Aserbaidschan?", "What is the capital of Azerbaijan?", 10, "Azerbaijan"));

            JeopardyDAO.INSTANCE.persist(category);
        } catch (Exception ex) {
            throw new RuntimeException("Could not fetch questions from DBpedia.", ex);
        }
    }

    private static Question createDBpediaQuestion(String textDE, String textEN, int value, String validResourceString) {
        Question question = new Question();
        question.setTextDE(textDE);
        question.setTextEN(textEN);
        question.setValue(value);

        if (!DBPediaService.isAvailable())
            return null;

        Resource validResource = DBPediaService.loadStatements(DBPedia.createResource(validResourceString));

        // build SPARQL-query
        SelectQueryBuilder query = DBPediaService.createQueryBuilder()
                .setLimit(5) // at most five statements
                .addWhereClause(RDF.type, DBPediaOWL.capital)
                .addPredicateExistsClause(FOAF.name)
                .addWhereClause(DBPediaOWL.Place, validResource)
                .addFilterClause(RDFS.label, Locale.GERMAN)
                .addFilterClause(RDFS.label, Locale.ENGLISH);

        // retrieve data from dbpedia
        play.Logger.info(String.format("DBpedia Querso %s", query.toQueryString()));
        Model validModel = DBPediaService.loadStatements(query.toQueryString());

        // get english and german movie names, e.g., for right choices
        List<String> englishValidCapitalName =
                DBPediaService.getResourceNames(validModel, Locale.ENGLISH);
        List<String> germanValidCapitalName =
                DBPediaService.getResourceNames(validModel, Locale.GERMAN);

        // alter query to get movies without tim burton
        query.removeWhereClause(DBPediaOWL.capitalCountry, validResource);
        query.addMinusClause(DBPediaOWL.capitalCountry, validResource);

        // retrieve data from dbpedia
        Model invalidModel = DBPediaService.loadStatements(query.toQueryString());
        // get english and german movie names, e.g., for wrong choices
        List<String> englishInvalidCapitalName =
                DBPediaService.getResourceNames(invalidModel, Locale.ENGLISH);
        List<String> germanInvalidCapitalName =
                DBPediaService.getResourceNames(invalidModel, Locale.GERMAN);

        // valid answers
        addAnswers(question, englishValidCapitalName, germanValidCapitalName, true);

        // invalid answers
        addAnswers(question, englishInvalidCapitalName, germanInvalidCapitalName, false);

        JeopardyDAO.INSTANCE.persist(question);

        return question;
    }

    private static void addAnswers(Question question, List<String> englishAnswers, List<String> germanAnswers, boolean isCorrect) {
        ArrayList<Answer> answers = new ArrayList<>();

        for(int i = 0; i < englishAnswers.size(); i++) {
            Answer answer = new Answer();
            answer.setTextDE(germanAnswers.get(i));
            answer.setTextEN(englishAnswers.get(i));

            answer.setCorrectAnswer(isCorrect);

            JeopardyDAO.INSTANCE.persist(answer);

            if(isCorrect) {
                question.addRightAnswer(answer);
            } else {
                question.addWrongAnswer(answer);
            }

            answers.add(answer);
        }

        question.setAnswers(answers);
    }
}
