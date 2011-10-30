package collabeditsync;

import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

public class SyncPluginTest {

    private SyncPlugin syncPlugin = new SyncPlugin();

    @Before
    public void setUp() throws Exception {
        syncPlugin.setInitialDocumentText("");
        syncPlugin.edit = mock(CollabEdit.class);
    }

//    @Test
//    public void textModifiedAndEventAboutItIsReceived() throws Exception {
//        when(syncPlugin.edit.waitForModificationCommands()).thenReturn(Command.class);
//        // ++ hashes
//        // update, get, skip
//        syncPlugin.documentUpdated("1");
//        syncPlugin.applyUpcomingCommand();
//
//        verify(command.apply(), never())
////        Command command = syncPlugin.edit.waitForModificationCommands();
////        no command.apply();
//    }

    // detect change
    // update, update, get+skip, get+modify, get+skip : stack?
    // get+modify, no-update
}
