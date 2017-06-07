package org.patch;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import com.github.fge.jsonpatch.diff.JsonDiff;
import org.bson.Document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.apache.commons.lang3.StringUtils.removeEnd;
import static org.apache.commons.lang3.StringUtils.removeStart;

public class MongoPatch {
    public JsonPatch getPatch(String sourceJson, String targetJson) throws IOException, JsonPatchException {

        final ObjectMapper mapper = new ObjectMapper();

        JsonNode source = mapper.readTree(sourceJson.getBytes());
        JsonNode target = mapper.readTree(targetJson.getBytes());

        return JsonDiff.asJsonPatch(source, target);
    }

    public Document getPatchAsDocument(String sourceJson, String targetJson) throws IOException, JsonPatchException {
        final ObjectMapper mapper = new ObjectMapper();
        JsonNode source = mapper.readTree(sourceJson.getBytes());
        JsonNode target = mapper.readTree(targetJson.getBytes());
        final JsonNode patchNode = JsonDiff.asJson(source, target);
        Document document = new Document();
        patchNode.elements().forEachRemaining(jsonNode -> formDocument(jsonNode, document));

        return document;
    }

    private Document formDocument(JsonNode jsonNode, Document document) {

        switch (((String)getValue(jsonNode.get("op"))).replaceAll("\"", "")) {
            case "remove":
                document.append("$unset", new Document(remapPathToMongo(getPath(jsonNode)), ""));
                break;
            case "replace":
                addToSet(jsonNode, document);
                break;
            case "add":
                addToSet(jsonNode, document);

                break;
        }
        return document;
    }

    private void addToSet(JsonNode jsonNode, Document document) {
        Document addSet = getSetDocument(document);
        document.put("$set", addSet.append(remapPathToMongo(getPath(jsonNode)), getValue(jsonNode.get("value"))));
    }

    private Object getValue(JsonNode value) {
        if (value.isArray()) {
            List<Object> values = new ArrayList<>();
            value.elements().forEachRemaining(jsonNode -> {
                if(jsonNode.getNodeType().equals(JsonNodeType.STRING)) {
                    values.add(trimPatchQuotes(String.valueOf(jsonNode)));
                }
                else values.add(jsonNode);
            });
            return values;
        }
        return trimPatchQuotes(String.valueOf(value));
    }




    private Document getSetDocument(Document document) {
        Document setDocument = (Document) document.get("$set");

        return setDocument == null ? new Document() : setDocument;
    }

    private String getPath(JsonNode jsonNode) {


        String value = (String)getValue(jsonNode.get("path"));
        value = value.replaceFirst("/", "");
        value = trimPatchQuotes(value);

        return value;


    }

    private String remapPathToMongo(String path) {
        return path.replaceAll("/",".");
    }

    private String trimPatchQuotes(String value) {
        value = removeEnd(value, "\"");
        value = removeStart(value, "\"");
        return value;
    }
}
