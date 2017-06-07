package org.patch;

import com.github.fge.jsonpatch.JsonPatchException;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class PatchTest {

    String json = "{\n" +
            "  \"baz\": \"qux\",\n" +
            "  \"foo\": \"bar\"\n" +
            "}";

    String patch = "[op: remove; path: \"/foo\", " +
            "op: add; path: \"/hello\"; value: [\"world\"], " +
            "op: replace; path: \"/baz\"; value: \"boo\"]";

    String result = "{\n" +
            "  \"baz\": \"boo\",\n" +
            "  \"hello\": [\"world\"]\n" +
            "}";

    @Test
    public void testPatch() throws IOException, JsonPatchException {





        MongoPatch mongoPatch = new MongoPatch();

        String actual = String.valueOf(mongoPatch.getPatch(json, result));

        assertEquals(patch, actual);

    }

    @Test
    public void testMongoStatements() throws IOException, JsonPatchException {



        String patch = "[op: remove; path: \"/foo\", " +
                "op: add; path: \"/hello\"; value: [\"world\"], " +
                "op: replace; path: \"/baz\"; value: \"boo\"]";


        Document updateExpected = new Document("$unset",new Document("foo","")).append( "$set",new Document("hello", Arrays.asList("world")).append("baz", "boo"));

        Document update = new MongoPatch().getPatchAsDocument(json,result);

        assertEquals(updateExpected,update);


        try(MongoClient mc = new MongoClient()) {
            MongoCollection<Document> coll = mc.getDatabase("test").getCollection("patch");
            coll.deleteMany(new Document());


            coll.insertOne(new Document("baz", "qux").append("foo", "bar").append("_id", 123));
            coll.updateOne(new Document("_id", 123), update);

            System.out.println(coll.find().limit(1).first());
        }

    }

    @Test
    public void testIntegerTransformaion() throws IOException, JsonPatchException {

        String json1 = "{\"int\":1}";
        String json2 = "{\"int\":2}";

        Document update = new MongoPatch().getPatchAsDocument(json1,json2);

        System.out.println("*********** "+update);

    }

    @Test
    public void testNestedDocumentChange() throws IOException, JsonPatchException {
        String json1 = "{\"field\" : { \"nested-field\" : \"value 1\"}}";
        String json2 = "{\"field\" : { \"nested-field\" : \"value 2\"}}";

        Document update = new MongoPatch().getPatchAsDocument(json1,json2);

        System.out.println("*********** "+update);

    }
}
