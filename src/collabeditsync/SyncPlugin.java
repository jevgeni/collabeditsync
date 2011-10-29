package collabeditsync;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.io.IOException;

public class SyncPlugin implements SyncPluginInterface {

    private final CollabEditResource collabEdit = new CollabEditResource("m37ga");

    public String getComponentName() {
        return PLUGIN_NAME;
    }

    public void initComponent() {
        System.out.println("init!");

        try {
            collabEdit.login();
            collabEdit.changeNick("jevgeni");


        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        final Document[] test = new Document[1];
        new Thread(new Runnable() {

            public void run() {
                while(!Thread.currentThread().isInterrupted()) {
                    if(test[0] != null) {

                        try {
                            System.out.println("Waiting for update!");
                            JSONObject json = collabEdit.waitForUpdate(false);
//                            collabEdit.getDataToInsert();
                            JSONObject op = json.getJSONObject("op");
                            System.out.println(json);
                            JSONArray ops = op.getJSONArray("ops");

                            JSONArray delete = null;
                            JSONArray add = null;
                            JSONArray indexFromStart = null;
                            JSONArray indexFromEnd = null;

                            System.out.println("Detecting... ");
                            for (int i = 0; i < ops.size(); i++) {
                                JSONArray element = ops.getJSONArray(i);
                                if (element.getInt(0) == 9) {
                                    System.out.print("delete found ...");
                                    delete = element;
                                } else if (element.getInt(0) == 8) {
                                    System.out.print("insert found ...");
                                    delete = element;
                                } else if (element.getInt(0) == 7) {
                                    if (indexFromStart == null) {
                                        System.out.println("index from start found...");
                                        indexFromStart = element;
                                    } else {
                                        System.out.println("index from end found...");
                                        indexFromEnd = element;
                                    }
                                }
                            }

                            ApplicationManager.getApplication().invokeLater(new Runnable() {
                                public void run() {

                                    ApplicationManager.getApplication().runWriteAction(new Runnable() {
                                        public void run() {
                                            test[0].insertString(1, "x");
                                        }
                                    });

                                }
                            });

                            System.out.println(json);
                        } catch (IOException e) {
                            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                        }
                    } else {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                        }
                    }
                }
            }
        }).start();

        EditorFactory.getInstance().getEventMulticaster().addDocumentListener(new DocumentListener() {
            public void beforeDocumentChange(DocumentEvent documentEvent) {
                System.out.println("before!");
            }

            public void documentChanged(DocumentEvent documentEvent) {
                System.out.println("after!");
                System.out.println(documentEvent);

               if (test[0] == null) {
                   test[0] = documentEvent.getDocument();
                   System.out.println("Detected document: " + test[0]);
               }


//                try {
//                    JSONObject json = collabEdit.waitForUpdate(true);
//                    collabEdit.sendUpdate(json.getString("full_text"), documentEvent.getDocument().getText());
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
            }
        });
    }

    public void disposeComponent() {
        //do nothing
    }

}
