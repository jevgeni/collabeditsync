package collabeditsync;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;

public class SyncPlugin implements SyncPluginInterface {

    private final CollabEdit edit = new CollabEdit();

    public String getComponentName() {
        return PLUGIN_NAME;
    }

    public void initComponent() {
        System.out.println("init!");
        edit.init(); // TODO: make it implicit

        final Document[] test = new Document[1];
        new Thread(new Runnable() {

            public void run() {
                while(true) {
                    if(test[0] != null) {
                        final Command command = edit.waitForModificationCommand();

                        ApplicationManager.getApplication().invokeLater(new Runnable() {
                                public void run() {

                                    ApplicationManager.getApplication().runWriteAction(new Runnable() {
                                        public void run() {
                                            command.apply(test[0]);
                                        }
                                    });

                                }
                            });

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
