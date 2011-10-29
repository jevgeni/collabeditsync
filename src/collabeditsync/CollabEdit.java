package collabeditsync;

import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.impl.event.DocumentEventImpl;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import java.io.IOException;

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

    public Command waitForModificationCommand() {
        try {
            JSONObject response;
            do {
                response = resource.waitForUpdate(false);
                System.out.println("Received: " + response);
            } while (!response.has("op"));

            JSONObject op = response.getJSONObject("op");
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
            return new Command(delete, insert);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }


    public void handle(DocumentEvent event) {
        try {
            resource.sendUpdate(event.getOffset(), event.getOldFragment(), event.getNewFragment(), event.getDocument().getCharsSequence());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
