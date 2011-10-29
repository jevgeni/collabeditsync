package collabeditsync;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.util.Computable;

import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class SyncPlugin implements SyncPluginInterface {

    private final CollabEdit edit = new CollabEdit();

    private final Lock lock = new ReentrantLock();

    private String oldText = null;

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
                                            synchronized (lock) {
                                                try {
                                                    command.apply(test[0]);
                                                    oldText = getDocumentText(test[0]);
                                                } catch (IndexOutOfBoundsException e) {
                                                    try {
                                                        oldText = edit.getFullText();
                                                        test[0].setText(oldText);
                                                    } catch (IOException e1) {
                                                        e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                                                    } catch (UnsuccessfulResponseException e1) {
                                                        e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                                                    }
                                                }
                                            }
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


        new Thread(new Runnable() {

            public void run() {
                while(true) {
                    if(test[0] != null) {
                        final Document document = test[0];

                        synchronized (lock) {
                            if (oldText == null) {
                                oldText = getDocumentText(document);
                            }
                        }
                        sleep();

                        String newText = getDocumentText(document);
                        synchronized (lock) {
                            edit.update(oldText, newText);
                            oldText = newText;
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
            }

            public void documentChanged(DocumentEvent documentEvent) {
//                System.out.println("after!");
//                System.out.println(documentEvent);

               if (test[0] == null) {
                   test[0] = documentEvent.getDocument();
                   System.out.println("Detected document: " + test[0]);
               }

//                edit.handle(documentEvent);


//                try {
//                    JSONObject json = collabEdit.waitForUpdate(true);
//                    collabEdit.sendPartialModificationCommand(json.getString("full_text"), documentEvent.getDocument().getText());
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
            }
        });
    }

    private String getDocumentText(final Document document) {
        String newText = ApplicationManager.getApplication().runReadAction(new Computable<String>() {

            public String compute() {
                return document.getText();
            }
        });
        System.out.println("----new: " + newText);
        System.out.println("-------------------");
        return newText;
    }

    private void sleep() {
        System.out.print("----Sleeping...");
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(" done sleeping");
    }

    public void disposeComponent() {
        //do nothing
    }

}
