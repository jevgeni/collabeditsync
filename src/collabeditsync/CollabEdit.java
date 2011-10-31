package collabeditsync;

import name.fraser.neil.plaintext.diff_match_patch;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static collabeditsync.Command.Operation.*;
import static collabeditsync.Command.Operation.DELETE;
import static name.fraser.neil.plaintext.diff_match_patch.*;

public class CollabEdit {
    CollabEditResource resource = new CollabEditResource("m37ga");

    public void init() {
        try {
            resource.login();
            resource.changeNick("jevgeni");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Command waitForExternalModificationCommand() {
        Command command;
        do {
            command = waitForModificationCommands();
        } while (isMyOwnCommand(command));

        return command;
    }

    Command waitForModificationCommands() {
        try {
            JSONObject response;
            do {
                response = resource.waitForUpdate(false);
                System.out.println("Received: " + response);
            } while (!response.containsKey("op"));


            String operation = (String) response.get("op");
            JSONObject op = (JSONObject) JSONValue.parse(operation);

            Long cuid = (Long) op.get("cuid");
            String parentHash = (String) op.get("parent_hash");
            String resultHash = (String) op.get("result_hash");
            JSONArray ops = (JSONArray) op.get("ops");

            int offset = 0;
            int cummulativeOffset = 0;

            List<Command.Diff> diffs = new ArrayList<Command.Diff>();
            for (int i = 0; i < ops.size(); i++) {
                JSONArray element = (JSONArray) ops.get(i);
                long operationCode = (Long) element.get(0);
                if (operationCode == 9) {
                    String value = (String) element.get(1);
                    diffs.add(new Command.Diff(DELETE, offset, value));
                } else if (operationCode == 8) {
                    String value = (String) element.get(1);
                    diffs.add(new Command.Diff(INSERT, offset, value));
                    cummulativeOffset += value.length();
                } else if (operationCode == 7) {
                    offset += (Long)element.get(1);
                    offset += cummulativeOffset;
                    cummulativeOffset = 0;
                }
            }

            return new Command(cuid, parentHash, resultHash, diffs);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (UnsuccessfulResponseException e) {
            e.printStackTrace();
        }

        return null;
    }


    public void diffUpdate(CharSequence textBefore, CharSequence textAfter) throws UnsuccessfulResponseException, IOException {
        diff_match_patch dmp = new diff_match_patch();
        LinkedList<Diff> diffs = dmp.diff_main(textBefore.toString(), textAfter.toString());
        dmp.diff_cleanupSemantic(diffs);
        System.out.println(diffs);

        CharSequence delete = "";
        CharSequence insert = "";
        int myOffset = 0;
        for(int i = 0; i < diffs.size(); i++) {
            Diff diff = diffs.get(i);
            switch (diff.operation) {
                case EQUAL:
                    if (delete.length() > 0 || insert.length() > 0) {
                        textBefore = tryPartialUpdate(myOffset, delete, insert, textBefore);
                        myOffset += insert.length();
                        delete = "";
                        insert = "";
                    }
                    myOffset += diff.text.length();
                    break;
                case DELETE:
                    delete = diff.text;
                    break;
                case INSERT:
                    insert = diff.text;
                    break;
            }
        }

        if (delete.length() > 0 || insert.length() > 0) {
            tryPartialUpdate(myOffset, delete, insert, textBefore);
        }
    }

    private CharSequence tryPartialUpdate(int myOffset, CharSequence delete, CharSequence insert, CharSequence textBefore) throws IOException, UnsuccessfulResponseException {
        StringBuilder builder = new StringBuilder();
        builder.append(textBefore.subSequence(0, myOffset));
        builder.append(insert);
        builder.append(textBefore.subSequence(myOffset + delete.length(), textBefore.length()));
        textBefore = builder.toString();

        resource.sendUpdate(myOffset, delete, insert, textBefore);

        return textBefore;
    }

    public void update(String oldText, String newText) {
        if (oldText.equals(newText)) return;
        try {
            diffUpdate(oldText, newText);
        } catch (UnsuccessfulResponseException e) {
            e.printStackTrace();
            fullUpdate(newText);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void fullUpdate(String newText) {
        System.out.println("full sync");
        try {
            resource.sendUpdate(getFullText(), newText);
        } catch (Exception e1) {
            System.err.println("Cannot perform full sync :(");
            e1.printStackTrace();
        }
    }

    public String getFullText() {
        try {
            return (String) resource.waitForUpdate(true).get("full_text");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (UnsuccessfulResponseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean isMyOwnCommand(Command command) {
        return command.cuid == getCuid();
    }

    private Long getCuid() {
        return resource.cuid;
    }

    public void setCuid(long cuid) {
        resource.cuid = cuid;
    }
}
