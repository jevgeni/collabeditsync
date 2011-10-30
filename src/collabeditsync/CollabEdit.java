package collabeditsync;

import name.fraser.neil.plaintext.diff_match_patch;
import net.sf.json.*;

import java.io.IOException;
import java.util.LinkedList;

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
            command = waitForModificationCommand();
        } while (isMyOwnCommand(command));

        return command;
    }

    Command waitForModificationCommand() {
        try {
            JSONObject response;
            do {
                response = resource.waitForUpdate(false);
                System.out.println("Received: " + response);
            } while (!response.has("op"));

            // TODO: cover with tests cuid/parent_Hash_result_Hash
            JSONObject op = response.getJSONObject("op");
            Integer cuid = op.getInt("cuid");
            String parentHash = op.getString("parent_hash");
            String resultHash = op.getString("result_hash");
            JSONArray ops = op.getJSONArray("ops");

            String deleteText = null;
            String addText = null;
            int startOffset = 0;

            for (int i = 0; i < ops.size(); i++) {
                JSONArray element = ops.getJSONArray(i);
                if (element.getInt(0) == 9) {
                    deleteText = element.getString(1);
                } else if (element.getInt(0) == 8) {
                    addText = element.getString(1);
                } else if (element.getInt(0) == 7) {
                    if (i == 0) startOffset = element.getInt(1);
                }
            }

            Command.Delete delete = deleteText != null ? new Command.Delete(startOffset, deleteText) : null;
            Command.Insert insert = addText != null ? new Command.Insert(startOffset, addText) : null;
            return new Command(cuid, parentHash, resultHash, delete, insert);
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

    private void fullUpdate(String newText) {
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
            return resource.waitForUpdate(true).getString("full_text");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (UnsuccessfulResponseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean isMyOwnCommand(Command command) {
        return command.cuid.equals(getCuid());
    }

    private Integer getCuid() {
        return resource.cuid;
    }

    public void setCuid(int cuid) {
        resource.cuid = cuid;
    }
}
