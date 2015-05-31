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
            category.setNameDE("Film");
            category.setNameEN("Movie");

            category.addQuestion(createDBpediaQuestion("Welche Filme sind von Tim Burton?",
                    "Tim Burton is the regisseur of which movies?", 30, "Tim_Burton"));

            category.addQuestion(createDBpediaQuestion("Welche Filme sind von Quentin Tarantino?",
                    "Quentin Tarantino is the regisseur of which movies?", 10, "Quentin_Tarantino"));

            category.addQuestion(createDBpediaQuestion("Welche Filme sind von Jim Jarmusch?",
                    "Jim Jarmusch is the regisseur of which movies?", 20, "Jim_Jarmusch"));

            category.addQuestion(createDBpediaQuestion("Welche Filme sind von Robert Aldrich?",
                    "Robert Aldrich is the regisseur of which movies?", 40, "Robert_Aldrich"));

            category.addQuestion(createDBpediaQuestion("Welche Filme sind von Stanley Kramer?",
                    "Stanley Kramer is the regisseur of which movies?", 60, "Stanley_Kramer"));

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
                .setLimit(2) // at most five statements
                .addWhereClause(RDF.type, DBPediaOWL.Film)
                .addPredicateExistsClause(FOAF.name)
                .addWhereClause(DBPediaOWL.director, validResource)
                .addFilterClause(RDFS.label, Locale.GERMAN)
                .addFilterClause(RDFS.label, Locale.ENGLISH);

        // retrieve data from dbpedia
        play.Logger.info(String.format("DBpedia Querso %s", query.toQueryString()));
        Model validModel = DBPediaService.loadStatements(query.toQueryString());

        // get english and german movie names, e.g., for right choices
        List<String> englishValidQuestionNames =
                DBPediaService.getResourceNames(validModel, Locale.ENGLISH);
        List<String> germanValidQuestionNames =
                DBPediaService.getResourceNames(validModel, Locale.GERMAN);

        // alter query to get movies without tim burton
        query.removeWhereClause(DBPediaOWL.director, validResource);
        query.addMinusClause(DBPediaOWL.director, validResource);

        // retrieve data from dbpedia
        Model invalidModel = DBPediaService.loadStatements(query.toQueryString());
        // get english and german movie names, e.g., for wrong choices
        List<String> englishInvalidQuestionNames =
                DBPediaService.getResourceNames(invalidModel, Locale.ENGLISH);
        List<String> germanInvalidQuestionNames =
                DBPediaService.getResourceNames(invalidModel, Locale.GERMAN);

        // valid answers
        addAnswers(question, englishValidQuestionNames, germanValidQuestionNames, true);

        // invalid answers
        addAnswers(question, englishInvalidQuestionNames, germanInvalidQuestionNames, false);

        JeopardyDAO.INSTANCE.persist(question);

        return question;
    }

    private static void addAnswers(Question question, List<String> englishAnswers, List<String> germanAnswers, boolean isCorrect) {

        for(int i = 0; i < englishAnswers.size(); i++) {
            Answer answer = new Answer();
            if(germanAnswers.get(i).isEmpty())
                answer.setTextDE(englishAnswers.get(i));
            else
                answer.setTextDE(germanAnswers.get(i));

            answer.setTextEN(englishAnswers.get(i));

            answer.setCorrectAnswer(isCorrect);

            JeopardyDAO.INSTANCE.persist(answer);

            if(isCorrect) {
                question.addRightAnswer(answer);
            } else {
                question.addWrongAnswer(answer);
            }

            question.getAnswers().add(answer);
        }
    }
}
